package fieldbox.boxes.plugins;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import field.utility.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * based on work from code.google.com/p/hotswap (Apache License 2)
 */
public class Hotswapper {
	private VirtualMachine vm;
	private AttachingConnector connector;

	/**
	 * this is a one shot operation. Todo: make it compatible with the other uses of this class
	 */
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
					String rr = c.getName()
						     .replaceAll("\\.", "/") + ".class";
					System.out.println(" looked up resource : " + rr + " -> " + c.getClassLoader()
												     .getResource(rr) + " " + c);
					h.replace(new File(c.getClassLoader()
							    .getResource(rr)
							    .getPath()), c.getName());
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

	static public void doSomething() {
		Hotswapper h = new Hotswapper();
		try {
			try {
				h.connect("localhost", "5005", "");

				Log.log("watching", "here we go");
				List<ReferenceType> found = h.vm.allClasses()
								.stream()
								.filter(x -> {
									try {
										List<String> paths = x.sourcePaths(null);
										if (paths == null) return false;
										for (String s : paths) {
											if (s.contains("nashorn")) Log.log("watching", s+" ? ");
											if (s.endsWith("bx[TARGET]")) return true;
										}
										return false;
									} catch (AbsentInformationException e) {
									}
									;
									return false;
								})
								.collect(Collectors.toList());
				Log.log("watching", "found " + found);
				if (found.size() > 0) {
					for(int i=0;i<found.size();i++) {
						System.out.println("watching.fields" + found.get(i)
											    .allFields());


						Log.log("watching.fields", found.get(i)
										.allFields());
						try {
							Log.log("watching.lines", found.get(i)
										       .allLineLocations());


							found.get(i).allLineLocations().stream().forEach(x -> {
								BreakpointRequest b = h.vm.eventRequestManager()
													  .createBreakpointRequest(x);
								b.setSuspendPolicy(BreakpointRequest.SUSPEND_EVENT_THREAD);
								b.putProperty("for location", x.toString());
								b.setEnabled(true);
							});
						} catch (AbsentInformationException e) {
							Log.log("watching.lines", "not available");
						}

						Log.log("watching.methods", found.get(i)
										 .allMethods());
						Log.log("watching.default", found.get(i)
										 .defaultStratum());
						Log.log("watching.strata", found.get(i)
										.availableStrata());
					}
				}

				new Thread(() -> {
					while(true) {
						try {
							EventSet e = h.vm.eventQueue()
									      .remove();
							e.forEach(x -> {
								System.out.println("EVENT :"+x);

								if (x instanceof BreakpointEvent)
								{
									System.out.println(" --- frames are ?");
									try {
										System.out.println("     frames :" + ((BreakpointEvent) x).thread()
																	  .frames());
										System.out.println("     frames :" + ((BreakpointEvent) x).thread()
																	  .frames().get(0).visibleVariables());
										((BreakpointEvent) x).thread()
																	  .frames().get(0).visibleVariables().forEach(y -> {
											System.out.println("  " + y);
											System.out.println("   " + y.typeName());
											try {
												System.out.println("   "+y.type());
											} catch (ClassNotLoadedException e1) {
												e1.printStackTrace();
											}
											System.out.println("   "+y.getClass());

											try {
												System.out.println("   " + ((BreakpointEvent) x).thread()
																		.frames()
																		.get(0)
																		.getValue(y));
											} catch (IncompatibleThreadStateException e1) {
												e1.printStackTrace();
											}
										});
									} catch (IncompatibleThreadStateException e1) {
										e1.printStackTrace();
									} catch (AbsentInformationException e1) {
										e1.printStackTrace();
									}

								}

							});

							e.resume();
						}catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}).start();


			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} finally {
//			try {
//				h.disconnect();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
		}
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
			if (!useSocket && tmp.transport()
					     .name()
					     .equals("dt_shmem")) {
				connector = tmp;
				break;
			}
			if (useSocket && tmp.transport()
					    .name()
					    .equals("dt_socket")) {
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
				vm.redefineClasses((Map<? extends ReferenceType, byte[]>) map);
			} catch (Throwable t) {
				Log.log("reload.error", "trouble reloading class " + classes.get(i), t);
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
