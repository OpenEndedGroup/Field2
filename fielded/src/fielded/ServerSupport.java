package fielded;

import field.message.MessageQueue;
import field.utility.*;
import field.graphics.RunLoop;
import fieldagent.Main;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Watches;
import fielded.Execution;
import fielded.RemoteEditor;
import fielded.plugins.BridgeToTextEditor;
import fielded.webserver.Server;
import fieldnashorn.Nashorn;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Created by marc on 3/26/14.
 */
public class ServerSupport {

	static public List<String> playlist = Arrays
		    .asList("messagebus.js", "instantiate.js", "JSHotkeyFunctions.js", "changehooks.js", "status.js", "helpbox.js", "modal.js", "brackets.js", "output.js", "doubleshift.js");


	public ServerSupport(Boxes boxes) {

		new BridgeToTextEditor(boxes.root()).connect(boxes.root());


		Watches watches = boxes.root().first(Watches.watches)
			    .orElseThrow(() -> new IllegalArgumentException(" need Watches for server support"));
		MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue = watches.getQueue();


		Log.log("startup", " server support is initializing ");
		try {

			// todo: these need to be random, unallocated ports

			Server s = new Server(8080, 8081);
			s.setFixedResource("/init", readFile(fieldagent.Main.app + "fielded/internal/init.html"));
			s.addDocumentRoot(fieldagent.Main.app + "/fielded/internal/");
			s.addDocumentRoot(fieldagent.Main.app + "/fielded/external/");

			s.addHandlerLast(x -> x.equals("alive"), (server, socket, address, payload) -> {
				Log.log("remote.general", " alive :" + payload);
				return payload;
			});

			s.addHandlerLast(x -> x.equals("log"), (server, socket, address, payload) -> {
				Log.log("remote.general","-\n" + payload + "\n-");
				return payload;
			});

			s.addHandlerLast(x -> x.equals("error"), (server, socket, address, payload) -> {
				Log.log("remote.general","-e-\n" + payload + "\n-e-");

				return payload;
			});

			s.addHandlerLast(x -> x.equals("initialize"), (server, socket, address, payload) -> {
				s.send(socket, readFile(fieldagent.Main.app + "/fielded/internal/include.js"));
				return payload;
			});

			s.addHandlerLast(x -> x.equals("initialize.finished"), (server, socket, address, payload) -> {

				for (String n : playlist) {
					s.send(socket, readFile(fieldagent.Main.app + "/fielded/internal/" + n));
				}

				Log.log("remote.trace", " payload is :" + payload);

				String name = payload + "";

				Log.log("remote.trace"," naming socket " + name + " = " + socket);

				s.nameSocket(name, socket);

				Log.log("startup"," initializing remote editor ");

				RemoteEditor ed = new RemoteEditor(s, name, watches, queue);
				ed.connect(boxes.root());
				ed.setCurrentlyEditingProperty(Execution.code);

				//Set up file reading
				Path properties = FileSystems.getDefault().getPath("fieldbox/resources", "properties.txt");
				File file = new File(properties.toString());
				StringBuilder contents = new StringBuilder();

				//Read properties text file into a string (contents)
				try ( BufferedReader in = new BufferedReader(new FileReader(file) ) ) {
					int curr;
					while((curr = in.read()) != -1) {
						contents.append((char)curr);
					}
					in.close();
				} catch(IOException x) {
					System.err.println("Error: Cannot open properties text file in read");
				}

				for (String line : contents.toString().split("\n") ) {
					String[] splitLine = line.split(": ");
					ed.sendJavaScript("extraKeys[\"" + splitLine[0] + "\"] = function (cm) {" + ed.hotkeyTranslator.get(splitLine[1]) + ";}");
				}

				return payload;
			});


		} catch (IOException e) {
		}
	}


	private static String readFile(String s) {
		try (BufferedReader r = new BufferedReader(new FileReader(new File(s)))) {
			String line = "";
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
					new ProcessBuilder("/usr/bin/google-chrome", "--app=http://localhost:8080/init")
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
					System.out.println(" Launching field with tmp dir :"+f);
					//  this almost works, but chrome is ignoring the --harmony flag completely
//					new ProcessBuilder("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", /*"--app=http://localhost:8080/init",*/ "--user-data-dir="+f.getAbsolutePath(), "--enable-experimental-web-platform-features", "--js-flags=\"--harmony\"", "--no-first-run")
					new ProcessBuilder("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", "--app=http://localhost:8080/init", "--no-first-run")
						    .redirectOutput(ProcessBuilder.Redirect.to(File.createTempFile("field", "browseroutput")))
						    .redirectError(File.createTempFile("field", "browsererror")).start();

				} catch (Throwable t2) {
					t2.printStackTrace();
				}
				break;
		}

	}

}
