package fieldbox.boxes.plugins;


import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import field.utility.Log;
import field.utility.Pair;
import fieldagent.Trampoline;
import fieldbox.boxes.Box;
import fielded.RemoteEditor;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plugin that supports reloading classes on the fly by connecting to a debug port in the running VM.
 */
public class Reload extends Box {

	private List<Map.Entry<Class, Trampoline.Record>> toReload;

	ReloadTarget target = new ReloadTarget();

	public Reload(Box root) {
		Log.on(".*reload.*", Log::red);

		new Thread(() -> {
			int lastNotified = 0;
			while (true) {
				try {
					toReload = Trampoline.loadMap.entrySet().stream().filter(x -> x.getValue().modified())
						    .collect(Collectors.toList());

				} catch (ConcurrentModificationException e) {
					// doesn't matter (will happen if classes are loading while we iterate over it)
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (toReload.size()>lastNotified) {
					lastNotified = toReload.size();
					String classList = toReload.stream().map( x-> x.getKey().getName().substring(x.getKey().getName().lastIndexOf(".")+1)).collect(Collectors.joining(", "));
					if (classList.length()>100) classList = classList.substring(0, 100)+"...";
					Log.log("reload", "There are classes that can be reloaded: " + classList);
					Log.log("reload", "Use the reload command to do so");
				}
			}
		}).start();


		properties.put(RemoteEditor.commands, () -> {
			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();


			Log.log("reload", "needing reloading...", toReload);

			List<Map.Entry<Class, Trampoline.Record>> toReload_copy = toReload;

			if (toReload_copy.size() == 0) {
			} else {
				String classList = toReload_copy.stream().map( x-> x.getKey().getName().substring(x.getKey().getName().lastIndexOf(".")+1)).collect(Collectors.joining(", "));
				if (classList.length()>100) classList = classList.substring(0, 100)+"...";
				m.put(new Pair<>("Reload changed classes", "Reloads the class" + (toReload_copy
					    .size() != 1 ? "es" : "") + " that have changed on disk &mdash; <i>" + classList+"</i>"), () -> {
					Hotswapper.swapClass(toReload_copy.stream().map(x ->x .getKey()).collect(Collectors.toList()).toArray(new Class[0]));
					toReload_copy.stream().forEach( x -> Trampoline.loadMap.compute( x.getKey(), (z, r) -> r.update()));
				});
			}
			return m;
		});
	}


	/**
	 * based on work from code.google.com/p/hotswap (Apache License 2)
	 */
	static public class Hotswapper {
		private VirtualMachine vm;
		private AttachingConnector connector;

		static public boolean swapClass(final Class... cc) {

			// new Thread() {
			// @Override
			// public void run() {

			System.out.println(" SWAP SINGLE CLASS ENTRY ");
			Hotswapper h = new Hotswapper();
			try {
				try {
					h.connect("localhost", "5005", "");

					for (Class c : cc) {
						String rr = c.getName().replaceAll("\\.", "/") + ".class";
						System.out.println(" looked up resource : " + rr + " -> " + c.getClassLoader()
							    .getResource(rr) + " " + c);
						h.replace(new File(c.getClassLoader().getResource(rr).getPath()), c.getName());
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					return false;
				}
			} finally {
				try {
					h.disconnect();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println(" SWAP SINGLE CLASS EXIT ");

			for (Class c : cc) {
				System.out.println(" has outer class ? :" + c.getEnclosingClass());
				if (c.getEnclosingClass() != null) {
					System.out.println(" recursive swap :" + c.getEnclosingClass());
					swapClass(c.getEnclosingClass());
				}
			}

			return true;
			// }
			// }.start();

		}

		public void connect(String name) throws Exception {
			connect(null, null, name);
		}

		public void connect(String host, String port) throws Exception {
			connect(host, port, null);
		}

		// either host,port will be set, or name
		private void connect(String host, String port, String name) throws Exception {

			Log.log("reload", "connecting to port");

			// connect to JVM
			boolean useSocket = (port != null);

			VirtualMachineManager manager = Bootstrap.virtualMachineManager();
			List connectors = manager.attachingConnectors();
			connector = null;
			for (int i = 0; i < connectors.size(); i++) {
				AttachingConnector tmp = (AttachingConnector) connectors.get(i);
				if (!useSocket && tmp.transport().name().equals("dt_shmem")) {
					connector = tmp;
					break;
				}
				if (useSocket && tmp.transport().name().equals("dt_socket")) {
					connector = tmp;
					break;
				}
			}
			if (connector == null) {
				throw new IllegalStateException("Cannot find shared memory connector");
			}

			Log.log("reload", "connector is :" + connector);

			Map args = connector.defaultArguments();
			Connector.Argument arg;
			// use name if using dt_shmem
			if (!useSocket) {
				arg = (Connector.Argument) args.get("name");
				arg.setValue(name);
			}
			// use port if using dt_socket
			else {
				arg = (Connector.Argument) args.get("port");
				arg.setValue(port);
			}
			Log.log("reload", "port is :" + args);
			Log.log("reload", "connecting...");

			vm = connector.attach(args);

			Log.log("reload", "connected, checking class redef");

			// query capabilities
			if (!vm.canRedefineClasses()) {
				throw new Exception("JVM doesn't support class replacement");
			}

			Log.log("reload", "supports class redefinition");

			// if (!vm.canAddMethod()) {
			// throw new Exception("JVM doesn't support adding method");
			// }
			// System.err.println("attached!");
		}

		public void replace(File classFile, String className) throws Exception {
			// load class(es)
			byte[] classBytes = loadClassFile(classFile);
			// redefine in JVM
			List<com.sun.jdi.ReferenceType> classes = vm.classesByName(className);

			// if the class isn't loaded on the VM, can't do the replace.
			if (classes == null || classes.size() == 0) return;

			for (int i = 0; i < classes.size(); i++) {

				ReferenceType refType = (ReferenceType) classes.get(i);
				Map<ReferenceType, byte[]> map = new HashMap<ReferenceType, byte[]>();
				map.put(refType, classBytes);
				try {
					vm.redefineClasses((Map<? extends com.sun.jdi.ReferenceType, byte[]>) map);
				}
				catch(Throwable t)
				{
					Log.log("reload.error", "trouble reloading class "+classes.get(i), t);
				}
			}
			// System.err.println("class replaced!");
		}

		public void disconnect() throws Exception {
			vm.dispose();
		}

		private byte[] loadClassFile(File classFile) throws IOException {
			DataInputStream in = new DataInputStream(new FileInputStream(classFile));

			byte[] ret = new byte[(int) classFile.length()];
			in.readFully(ret);
			in.close();

			// System.err.println("class file loaded.");
			return ret;
		}
	}

}
