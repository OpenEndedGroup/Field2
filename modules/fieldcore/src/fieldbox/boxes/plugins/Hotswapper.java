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
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * based on work from code.google.com/p/hotswap (Apache License 2)
 */
public class Hotswapper {
	private VirtualMachine vm;
	private AttachingConnector connector;

	static public Hotswapper cached;

	/**
	 * this is a one shot operation. Todo: make it compatible with the other uses of this class
	 */
	static public boolean swapClass(Consumer<String> warnings, final Class... cc) {

		Log.log("reload", () -> "Entering reload  ");

//		new Thread() {
//			@Override
//			public void run() {

		Log.log("reload", () -> "Entering reload  thread ");


		Log.log("reload", () -> "L1");

		try {
			Log.log("reload", () -> "L2");
			try {
				Log.log("reload", () -> "L3");
				if (cached == null) {
					cached = new Hotswapper();
					cached.connect("127.0.0.1", "5005", "");
				}
//						h.connect("localhost", "5005", "");
				Log.log("reload", () -> "L4");

				for (Class c : cc) {
					Log.log("reload", () -> "L5:" + c);

					String rr = c.getName()
						.replaceAll("\\.", "/") + ".class";
					Log.log("reload", () -> "L6:" + c);
					try {
						cached.replace(new File(c.getClassLoader()
							.getResource(rr)
							.getPath()), c.getName());
						warnings.accept("reloaded class <i>" + c + "</i>");
					} catch (Throwable t) {
						warnings.accept("reload <b>failed</b> for class <i>" + c + "</i>");
						t.printStackTrace();
					}
					Log.log("reload", () -> "L7:" + c);

				}
			} catch (Exception e1) {
				warnings.accept("exception thrown while trying to reload classes: <i>" + e1.getMessage() + "</i>");
				e1.printStackTrace();
				return false;
			}
		} finally {
			Log.log("reload", () -> "L8");
			try {
//						h.disconnect();
				Log.log("reload", () -> "L9");
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		Log.log("reload", () -> "L10");

		boolean ok = true;
		for (Class c : cc) {
			Log.log("reload", () -> "L11" + c);
			if (c.getEnclosingClass() != null) {
				Log.log("reload", () -> "L12" + c);
				ok &= swapClass(warnings, c.getEnclosingClass());
			}
		}
		return ok;

	}

	static public void doSomething() {
		Hotswapper h = cached = cached != null ? cached : new Hotswapper();
		try {
			try {
				h.connect("localhost", "5005", "");
				Log.log("watching", () -> "here we go");
				List<ReferenceType> found = h.vm.allClasses()
					.stream()
					.filter(x -> {
						try {
							List<String> paths = x.sourcePaths(null);
							if (paths == null) return false;
							for (String s : paths) {
//								if (s.contains("nashorn"))
//								Log.log("watching", () -> s + " ? ");
								if (s.contains("bx[TARGET]")) {
									Log.log("watching", () -> "found it ! " + s);
									return true;
								}
							}
							return false;
						} catch (AbsentInformationException e) {
						}
						return false;
					})
					.collect(Collectors.toList());
				Log.log("watching", () -> "found " + found);
				if (found.size() > 0) {
					for (int i = 0; i < found.size(); i++) {
						System.out.println("watching.fields" + found.get(i).allFields());


						final int finalI = i;
						Log.log("watching.fields", () -> found.get(finalI).allFields());
						try {
							found.get(i)
								.allLineLocations()
								.stream()
								.forEach(x -> {
									BreakpointRequest b = h.vm.eventRequestManager()
										.createBreakpointRequest(x);
									b.setSuspendPolicy(BreakpointRequest.SUSPEND_EVENT_THREAD);
									b.putProperty("for location", x.toString());
									b.setEnabled(true);
								});
						} catch (AbsentInformationException e) {
							Log.log("watching.lines", () -> "not available");
						}

						Log.log("watching.methods", () -> found.get(finalI)
							.allMethods());
						Log.log("watching.default", () -> found.get(finalI)
							.defaultStratum());
						Log.log("watching.strata", () -> found.get(finalI)
							.availableStrata());
					}
				}

				new Thread(() -> {
					try {
						while (true) {
							try {
								System.out.println(" waiting for event queue ");
								EventSet e = h.vm.eventQueue()
									.remove();

								System.out.println(" event set contains :" + e.size());

								e.forEach(x -> {
									System.out.println("EVENT :" + x);

									if (x instanceof BreakpointEvent) {
										System.out.println(" --- frames are ?");
										try {
											System.out.println("     frames :" + ((BreakpointEvent) x).thread()
												.frames());
											System.out.println("     frames :" + ((BreakpointEvent) x).thread()
												.frames()
												.get(0)
												.visibleVariables());
											List<StackFrame> frames = ((BreakpointEvent) x).thread()
												.frames();
											for (StackFrame ff : frames) {
												System.out.println(" -- f :" + ff + " = " + ff.visibleVariables());
												ff
													.visibleVariables()
													.forEach(y -> {
														System.out.println("  " + y);
														System.out.println("   " + y.typeName());
														try {
															System.out.println("   " + y.type());
														} catch (ClassNotLoadedException e1) {
															e1.printStackTrace();
														}
														System.out.println("   " + y.getClass());

														try {
															System.out.println("   " + ((BreakpointEvent) x).thread()
																.frames()
																.get(0)
																.getValue(y));
														} catch (IncompatibleThreadStateException e1) {
															e1.printStackTrace();
														}
													});
											}


										} catch (IncompatibleThreadStateException e1) {
											e1.printStackTrace();
										} catch (AbsentInformationException e1) {
											e1.printStackTrace();
										}
										((BreakpointEvent) x).thread().resume();

									}

								});

								e.resume();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
					finally
					{
						System.out.println(" -- we are exiting the queue thread");
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

		Log.log("reload", () -> "connecting to port");

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

		Log.log("reload", () -> "connector is :" + connector);

		Map args = connector.defaultArguments();
		Connector.Argument arg;
		if (!useSocket) {
			arg = (Connector.Argument) args.get("name");
			arg.setValue(name);
		} else {
			arg = (Connector.Argument) args.get("port");
			arg.setValue(port);
		}

		((Connector.Argument) args.get("hostname")).setValue(host);

		Log.log("reload", () -> "port is :" + args);
		Log.log("reload", () -> "connecting...");


		vm = connector.attach(args);
		Log.log("reload", () -> "connected, checking class redef");

		// query capabilities
		if (!vm.canRedefineClasses()) {
			throw new Exception("JVM doesn't support class replacement");
		}


		Log.log("reload", () -> "supports class redefinition");

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

			ReferenceType refType = classes.get(i);
			Map<ReferenceType, byte[]> map = new HashMap<ReferenceType, byte[]>();
			map.put(refType, classBytes);
			try {
				vm.redefineClasses(map);
			} catch (Throwable t) {
				final int finalI = i;
				Log.log("reload.error", () -> "trouble reloading class " + classes.get(finalI) + t);
			}
		}
		// System.err.println("class replaced!");

	}

	public void disconnect() throws Exception {

		System.err.println(" about to dispose " + vm);

		vm.dispose();
		System.err.println(" disposed " + vm);
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
