package fielded;

import field.message.MessageQueue;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Ports;
import field.utility.Quad;
import fieldagent.Main;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Watches;
import fieldbox.execution.Execution;
import fielded.plugins.BridgeToTextEditor;
import fielded.webserver.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by marc on 3/26/14.
 */
public class ServerSupport {

	static public int webserverPort = -1;

	static public final Dict.Prop<Server> server = new Dict.Prop<Server>("server").type().toCannon().doc("The internal websocket-capable server");

	static public List<String> playlist = Arrays
		    .asList("messagebus.js", "instantiate.js", "kill.js", "changehooks.js", "status.js", "helpbox.js", "modal.js", "brackets.js", "output.js", "doubleshift.js", "JSHotkeyFunctions.js", "colorPicker.js", "drags.js", "taps.js");


	public ServerSupport(Boxes boxes) {

		new BridgeToTextEditor(boxes.root()).connect(boxes.root());

		Watches watches = boxes.root().first(Watches.watches)
			    .orElseThrow(() -> new IllegalArgumentException(" need Watches for server support"));
		MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue = watches.getQueue();


		Log.log("startup", ()->" server support is initializing ");
		try {

			int a = webserverPort = Ports.nextAvailable(8080);
			int b = Ports.nextAvailable(a+1);
			Server s = new Server(a, b);

			boxes.root().properties.put(server, s);
			s.setFixedResource("/init", readFile(fieldagent.Main.app + "/modules/fieldcore/resources//init.html"));
			s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldcore/resources/");

			s.addHandlerLast(x -> x.equals("alive"), (server, socket, address, payload) -> {
				Log.log("remote.general", ()->" alive :" + payload);
				return payload;
			});

			s.addHandlerLast(x -> x.equals("log"), (server, socket, address, payload) -> {
				Log.log("remote.general", ()->"-\n" + payload + "\n-");
				return payload;
			});

			s.addHandlerLast(x -> x.equals("error"), (server, socket, address, payload) -> {
				Log.log("remote.general", ()->"-e-\n" + payload + "\n-e-");

				return payload;
			});

			s.addHandlerLast(x -> x.equals("initialize"), (server, socket, address, payload) -> {
				s.send(socket, readFile(fieldagent.Main.app + "/modules/fieldcore/resources/include.js"));
				return payload;
			});

			s.addHandlerLast(x -> x.equals("initialize.finished"), (server, socket, address, payload) -> {

				for (String n : playlist) {
					s.send(socket, readFile(fieldagent.Main.app + "/modules/fieldcore/resources/" + n));
				}

				Log.log("remote.trace", ()->" payload is :" + payload);

				String name = payload + "";

				Log.log("remote.trace", ()->" naming socket " + name + " = " + socket);

				s.nameSocket(name, socket);

				Log.log("startup", ()->" initializing remote editor ");

				RemoteEditor ed = new RemoteEditor(s, name, watches, queue);
				ed.connect(boxes.root());
				ed.setCurrentlyEditingProperty(Execution.code);

				//Set up file reading
				File file = new File(System.getProperty("user.home") + "/.field/hotkeys.txt");
				StringBuilder contents = new StringBuilder();

				//Read properties text file into a string (contents)
				try (BufferedReader in = new BufferedReader(new FileReader(file))) {
					int curr;
					while ((curr = in.read()) != -1) {
						contents.append((char) curr);
					}
					in.close();
				} catch (IOException x) {
					System.err.println("Error: Cannot open properties text file in read");
				}

				for (String line : contents.toString().split("\n")) {
					String[] splitLine = line.split(":");
					Log.log("hotkeys.debug",()-> " line is :" + splitLine.length + " <" + line + ">");
					if (splitLine.length > 1) {
						ed.sendJavaScript("extraKeys[\"" + splitLine[0].trim() + "\"] = function (cm) {" + splitLine[1]
							    .trim() + ";}");
					}
				}

				return payload;
			});


		} catch (IOException e) {
		}
	}


	private static String readFile(String s) {
		try (BufferedReader r = new BufferedReader(new FileReader(new File(s)))) {
//			String line = "//# sourceURL="+s+"\n";
			String line= s.endsWith(".js") ? ("//# sourceURL="+s+"\n") : "";
			while (r.ready()) {
				line += r.readLine() + "\n";
			}
			return line;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	static public void openEditor() throws IOException {
		switch (Main.os) {
			case linux:
				try {
					new ProcessBuilder("/usr/bin/google-chrome", "--app=http://localhost:"+ServerSupport.webserverPort+"/init")
						    .redirectOutput(ProcessBuilder.Redirect.to(File.createTempFile("field", "browseroutput")))
						    .redirectError(File.createTempFile("field", "browsererror")).start();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				break;
			case mac:
				try {
					File f = File.createTempFile("fieldchromeuserdir", "field");
					f.delete();
					f.mkdirs();
					System.out.println(" Launching field with tmp dir :" + f);
					//  this almost works, but chrome is ignoring the --harmony flag completely
//					new ProcessBuilder("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", /*"--app=http://localhost:8080/init",*/ "--user-data-dir="+f.getAbsolutePath(), "--enable-experimental-web-platform-features", "--js-flags=\"--harmony\"", "--no-first-run")
					new ProcessBuilder("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", "--app=http://localhost:"+ServerSupport.webserverPort+"/init", "--no-first-run")
						    .redirectOutput(ProcessBuilder.Redirect.to(File.createTempFile("field", "browseroutput")))
						    .redirectError(File.createTempFile("field", "browsererror")).start();

				} catch (Throwable t2) {
					t2.printStackTrace();
				}
				break;
		}

	}

}
