package fielded.boxbrowser;

import com.google.common.collect.HashBiMap;
import com.google.common.io.Files;
import field.app.RunLoop;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Ports;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.boxes.Callbacks;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.RemoteEditor;
import fielded.plugins.Out;
import fielded.webserver.NanoHTTPD;
import fielded.webserver.Server;

import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Plugin for making web apps in Field
 */
public class WebApps extends Box implements IO.Loaded {
	static public final String EXECUTE = "/execute/";
	static public final String CONTINUE = "/continue/";
	static public final String FILESYSTEM = "/filesystem/";

	static public Dict.Prop<BiFunctionOfBoxAnd<String, String>> newStaticHTML = new Dict.Prop<>("newStaticHTML").type()
														    .toCannon()
														    .doc("`_.newStaticHTML(\"mypage\")` adds an `_.html` property to this box that is served up by the WebApps plugin at http://localhost:8082/mypage");

	static public Dict.Prop<BiConsumer<String, String>> newStaticFile = new Dict.Prop<>("newStaticFile").type()
													    .toCannon()
													    .doc("`_.newStaticFile(\"myjpeg.jpg\", \"/path/to/a/jpeg\")` adds a file to the webserver resolvable at  http://localhost:8082/myjpeg.jpg");

	static public Dict.Prop<String> html = new Dict.Prop<>("html").type()
								      .toCannon()
								      .doc(" a static html resource served by the WebApps plugin, creatable using `_.newStaticHTML(...)`");

	static {
		FieldBox.fieldBox.io.addFilespec("html", ".html", "htmlmixed");
	}

	static public final String preambleA = "<html xmlns='http://www.w3.org/1999/xhtml'> <meta charset='UTF-8'><head>" +
		    "<script src='/field/filesystem/codemirror-5.12/lib/codemirror.js'></script>" +
		    "<link rel='stylesheet' href='/field/filesystem/codemirror-5.12/lib/codemirror.css'>" +
		    "<link rel='stylesheet' href='/field/filesystem/codemirror-5.12/theme/default.css'>" +
		    "<link rel='stylesheet' href='/field/filesystem/field-boxbrowser.css' type='text/css'>" +
		    "<script src='/field/filesystem/codemirror-5.12/mode/javascript/javascript.js'></script>" +
		    "<script src='/field/filesystem/jquery-2.1.0.min.js'></script>" +
		    "<script src='/field/filesystem/field-boxbrowser.js'></script>" +
		    "</head><body><div class='all'>";
	static public final String postambleA = "</div></body>";

	static public String preambleB;

	static {
		try {
			preambleB = Files.toString(new File(fieldagent.Main.app + "modules/fieldcore/resources/init_webapps.html_fragment"), Charset.defaultCharset());
			preambleB += "<body><div class='all'>";
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static public final String postambleB = "</div></body>";

	private final Box root;
	private Server s;

	HashBiMap<String, Box> lookup = HashBiMap.create();
	HashBiMap<String, String> resources = HashBiMap.create();

	public WebApps(Box root) {
		this.root = root;

		properties.put(Commands.commands, () -> {
			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			RemoteEditor ed = this.find(RemoteEditor.editor, both())
					      .findFirst()
					      .get();

			Box box = ed.getCurrentlyEditing();
			Dict.Prop<String> cep = ed.getCurrentlyEditingProperty();

			if (box == null) return m;

			String s = box.properties.get(WebApps.html);
			if (!cep.equals(html) && (box.first(html, box.upwards())
						     .isPresent() || s != null)) m.put(new Pair<>("Edit <i>HTML</i>",
												  "Switch to editing html associated with this box, served at http://localhost:8082/" + lookup.inverse()
																							      .get(box)),
										       () -> {
											       ed.setCurrentlyEditingProperty(html);
										       });

			return m;
		});

		this.properties.put(newStaticHTML, (x, name) -> {

			lookup.put(name, x);

			return x.properties.computeIfAbsent(html, m -> "");
		});

		this.properties.put(newStaticFile, (name, path) -> {

			resources.put(name, path);
		});

	}

	static AtomicInteger uid = new AtomicInteger(0);

	@Override
	public void loaded() {

		try {
			int a = Ports.nextAvailable(8080);
			int b = Ports.nextAvailable(a + 1);
			this.s = new Server(a, b);

			System.err.println(" webapps port is :" + a + " / " + b);

		} catch (IOException e) {
			e.printStackTrace();
		}

		s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldbox/resources/");
		s.addDocumentRoot(fieldagent.Main.app + "/modules/fielded/resources/");


		s.addURIHandler((uri, method, headers, params, files) -> {
			if (uri.startsWith(EXECUTE)) {
				uri = uri.substring(EXECUTE.length());
				String[] pieces = uri.split("/");

				Optional<Box> first = root.breadthFirst(root.both())
							  .filter(x -> x.properties.has(Box.name) && x.properties.get(Box.name)
														 .equals(pieces[0]))
							  .findFirst();

				if (!first.isPresent()) return null;

				Box bx = first.get();

				Object res = Callbacks.call(bx, Callbacks.main, params);

				return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, preambleA + res + postambleA);
			}
			return null;
		});
		s.addURIHandler((uri, method, headers, params, files) -> {
			if (uri.startsWith(CONTINUE)) {
				uri = uri.substring(CONTINUE.length());
				String[] pieces = uri.split("/");

				Optional<Box> first = root.breadthFirst(root.both())
							  .filter(x -> x.properties.has(Box.name) && x.properties.get(Box.name)
														 .equals(pieces[0]))
							  .findFirst();

				if (!first.isPresent()) return null;

				Box bx = first.get();


				String uid = "" + (WebApps.uid.incrementAndGet());
				String pre = preambleB.replaceAll("///WSPORT///", "" + s.getWebsocketPort())
						      .replaceAll("///ID///", uid)
						      .replaceAll("///PORT///", "" + s.getPort());

				// now we wait for it to open a websocket with id 'uid' and grab that socket.


				s.addHandlerLast((server, socket, address, payload) -> {
					System.out.println(" socket opened with :" + server + " " + socket + " " + address + " " + payload + "  on thread :" + Thread.currentThread());

					RunLoop.main.once(() -> {
						if (("" + payload).equals(uid)) {

							System.out.println(" Socket is live, executing code ");

							Consumer<String> executeJS = s -> {
								socket.send(s);
							};

						Consumer<String> append = s -> {
							executeJS.accept("$('.all').append('" + s.replace("'", "\"") + "');");
						};

							LinkedHashMap<String, Object> p2 = new LinkedHashMap<String, Object>();
							p2.putAll(params);
							p2.put("files", files);
							p2.put("execute", executeJS);
							p2.put("append", append);

							Object res = Callbacks.call(bx, Callbacks.main, p2);

							if (res != null) {
								Optional<Out> o = root.find(Out.__out, root.both())
										      .findAny();
								if (o.isPresent()) {
									append.accept(o.get()
										       .convert(res));
								} else {
									append.accept("" + res);
								}
							}

						}
					});
					return payload;
				});

				return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, pre + postambleB);
			}
			return null;
		});

		s.addURIHandler((uri, method, headers, params, files) -> {

			System.out.println(" looking up uri :" + uri);

			if (uri.startsWith("/")) uri = uri.substring(1);

			Box bx = lookup.get(uri);
			if (root.breadthFirstAll(root.allDownwardsFrom())
				.anyMatch(z -> z == bx)) {
				String m = bx.properties.get(html);
				if (m != null) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, m);
			} else lookup.remove(uri);

			return null;
		});

		s.addURIHandler((uri, method, headers, params, files) -> {

			if (uri.startsWith("/")) uri = uri.substring(1);

			String path = resources.get(uri);
			if (path != null) try {
				return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "application/unknown", new BufferedInputStream(new FileInputStream(new File(path))));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return null;
		});

		s.addURIHandler((uri, method, headers, params, files) -> {
			if (uri.startsWith(FILESYSTEM)) {
				uri = uri.substring(FILESYSTEM.length());

				if (new File(uri).exists()) try {
					return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "application/unknown", new BufferedInputStream(new FileInputStream(new File(uri))));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			return null;
		});

	}


}

