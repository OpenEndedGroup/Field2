package fielded;

import field.app.RunLoop;
import field.message.MessageQueue;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Ports;
import field.utility.Quad;
import fieldagent.Main;
import fieldbox.boxes.Box;
import fieldbox.boxes.Watches;
import fieldbox.execution.Execution;
import fielded.webserver.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by marc on 3/26/14.
 */
public class ServerSupport {

//	static public int webserverPort = -1;

	static public final Dict.Prop<Server> server = new Dict.Prop<Server>("server").type().toCanon().doc("The internal websocket-capable server");

	static public List<String> playlist = Arrays
		.asList("messagebus.js", "instantiate.js", "kill.js", "changehooks.js", "status.js", "helpbox.js", "modal.js", "brackets.js", "output.js", "doubleshift.js", "JSHotkeyFunctions.js", "colorPicker.js", "drags.js", "taps.js", "interventions.js");

	private RemoteEditor ed;
	private Server s;


	public ServerSupport(Box root) {

//		new BridgeToTextEditor(root).connect(root);

		Watches watches = root.first(Watches.watches)
			.orElseThrow(() -> new IllegalArgumentException(" need Watches for server support"));
		MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue = watches.getQueue();


		Log.log("startup", () -> " server support is initializing ");
		try {

			int a = Ports.nextAvailable(8080);
			int b = Ports.nextAvailable(a + 1);
			s = new Server(a, b);

			root.properties.put(server, s);
//			s.setFixedResource("/init", readFile(fieldagent.Main.app + "/modules/fieldcore/resources//init.html"));
			s.setFixedResource("/init", readFile(Main.app + "/lib/web/init.html"));

//			s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldcore/resources/");
			s.addDocumentRoot(Main.app + "/lib/web/");


			s.addHandlerLast(x -> x.equals("alive"), (server, socket, address, payload) -> {
				Log.log("remote.general", () -> " alive :" + payload);
				return payload;
			});

			s.addHandlerLast(x -> x.equals("log"), (server, socket, address, payload) -> {
				Log.log("remote.general", () -> "-\n" + payload + "\n-");
				return payload;
			});

			s.addHandlerLast(x -> x.equals("error"), (server, socket, address, payload) -> {
				Log.log("remote.general", () -> "-e-\n" + payload + "\n-e-");

				return payload;
			});

			s.addHandlerLast(x -> x.equals("initialize"), (server, socket, address, payload) -> {
//				s.send(socket, readFile(fieldagent.Main.app + "/modules/fieldcore/resources/include.js"));
				s.send(socket, readFile(Main.app + "/lib/web/include.js"));

				return payload;
			});

			s.addHandlerLast(x -> x.equals("initialize.finished"), (server, socket, address, payload) -> {

				for (String n : playlist) {
//					s.send(socket, readFile(fieldagent.Main.app + "/modules/fieldcore/resources/" + n));
					s.send(socket, readFile(fieldagent.Main.app + "/lib/web/" + n));
				}

				Log.log("remote.trace", () -> " payload is :" + payload);

				String name = payload + "";

				Log.log("remote.trace", () -> " naming socket " + name + " = " + socket);

				s.nameSocket(name, socket);

				Log.log("startup", () -> " initializing remote editor ");

				ed = new RemoteEditor(s, name, watches, queue);
				ed.connect(root);
				ed.setCurrentlyEditingProperty(Execution.code);

				return payload;
			});


		} catch (IOException e) {
		}
	}


	public static String readFile(String s) {
		try (BufferedReader r = new BufferedReader(new FileReader(new File(s)))) {
//			String line = "//# sourceURL="+s+"\n";
			String line = s.endsWith(".js") ? ("//# sourceURL=" + s + "\n") : "";
			while (r.ready()) {
				line += r.readLine() + "\n";
			}
			return line;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

//	static public void openEditor() throws IOException {
//		switch (Main.os) {
//			case linux:
//				try {
//					new ProcessBuilder("/usr/bin/google-chrome", "--app=http://localhost:"+ServerSupport.webserverPort+"/init")
//						    .redirectOutput(ProcessBuilder.Redirect.to(File.createTempFile("field", "browseroutput")))
//						    .redirectError(File.createTempFile("field", "browsererror")).start();
//				} catch (Throwable t) {
//					t.printStackTrace();
//				}
//				break;
//			case mac:
//				try {
//					File f = File.createTempFile("fieldchromeuserdir", "field");
//					f.delete();
//					f.mkdirs();
//					System.out.println(" Launching field with tmp dir :" + f);
//					//  this almost works, but chrome is ignoring the --harmony flag completely
////					new ProcessBuilder("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", /*"--app=http://localhost:8080/init",*/ "--user-data-dir="+f.getAbsolutePath(), "--enable-experimental-web-platform-features", "--js-flags=\"--harmony\"", "--no-first-run")
//					new ProcessBuilder("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", "--app=http://localhost:"+ServerSupport.webserverPort+"/init", "--no-first-run")
//						    .redirectOutput(ProcessBuilder.Redirect.to(File.createTempFile("field", "browseroutput")))
//						    .redirectError(File.createTempFile("field", "browsererror")).start();
//
//				} catch (Throwable t2) {
//					t2.printStackTrace();
//				}
//				break;
//		}
//
//	}

	public Server getServer() {
		return s;
	}


	public Future<RemoteEditor> getRemoteEditor() {
		CompletableFuture<RemoteEditor> c = new CompletableFuture<RemoteEditor>();

		if (ed != null)
			c.complete(ed);
		else
			RunLoop.main.getLoop().attach(pass -> {
				if (ed != null) {
					c.complete(ed);
					return false;
				}
				return true;
			});

		return c;

	}

}
