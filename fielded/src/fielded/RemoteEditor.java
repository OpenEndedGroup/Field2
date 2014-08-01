package fielded;

import field.graphics.FLine;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.message.MessageQueue;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fielded.webserver.RateLimitingQueue;
import fielded.webserver.Server;
import fielded.windowmanager.LinuxWindowTricks;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static fieldbox.boxes.FLineDrawing.*;
import static fieldbox.boxes.StandardFLineDrawing.filled;
import static fieldbox.boxes.StandardFLineDrawing.stroked;

/**
 * connects to that WebSocket and does things via those message busses
 */
public class RemoteEditor extends Box {

	static public final Dict.Prop<Supplier<Map<Pair<String, String>, Runnable>>> commands = new Dict.Prop<>("commands").type()
		    .doc("commands injected into the editor as ctrl-space menu").toCannon();
	static public final Dict.Prop<Supplier<Map<Pair<String, String>, Runnable>>> hotkeyCommands = new Dict.Prop<>("hotkeyCommands").type()
		    .doc("commands injected into the editor as hotkey menu").toCannon();
	static public final Dict.Prop<RemoteEditor> editor = new Dict.Prop<>("editor").type().doc("the (remote) editor object").toCannon();
	static public final Dict.Prop<Function<Box, Consumer<String>>> outputFactory = new Dict.Prop<>("outputFactory");
	static public final Dict.Prop<Function<Box, Consumer<Pair<Integer, String>>>> outputErrorFactory = new Dict.Prop<>("outputErrorFactory");

	static public interface ExtendedCommand extends Runnable {
		public void begin(SupportsPrompt prompt, String alternativeChosen);
	}

	static public interface SupportsPrompt {
		public void prompt(String prompt, Map<Pair<String, String>, Runnable> options, ExtendedCommand alternative);
	}

	private final Server server;
	private final String socketName;
	private final MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue;
	private final Watches watches;
	public final LinkedHashMap<String, String> hotkeyTranslator = new LinkedHashMap<>();

	LinkedHashMap<String, Runnable> callTable = new LinkedHashMap<>();
	ExtendedCommand callTable_alternative = null;

	public List<Consumer<String>> logStack = new ArrayList<>();
	public List<Consumer<String>> errorStack = new ArrayList<>();

	public RemoteEditor(Server server, String socketName, Watches watches, MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue) {
		this.server = server;
		this.socketName = socketName;
		this.queue = queue;
		this.watches = watches;
		this.properties.put(editor, this);

		this.properties.putToMap(Boxes.insideRunLoop, "main.__watch_service__", (Supplier<Boolean>) this::update);

		this.hotkeyTranslator.put("Autocomplete","Autocomplete()");
		this.hotkeyTranslator.put("Commands", "Commands()");
		this.hotkeyTranslator.put("Current Bracket", "Current_Bracket()");
		this.hotkeyTranslator.put("Hotkeys", "Hotkeys()");
		this.hotkeyTranslator.put("Import", "Import()");
		this.hotkeyTranslator.put("Run All", "Run_All()");
		this.hotkeyTranslator.put("Run Begin", "Run_Begin()");
		this.hotkeyTranslator.put("Run End", "Run_End()");
		this.hotkeyTranslator.put("Run Selection", "Run_Selection()");

		watches.addWatch(Mouse.isSelected, "selection.changed");
		watches.addWatch(LinuxWindowTricks.lostFocus, "focus.editor");

		queue.register(Predicate.isEqual("selection.changed"), (c) -> {
			Log.log("remote.trace", " selection changed message ");
			selectionHasChanged = true;
		});

		queue.register(Predicate.isEqual("focus.editor"), (c) -> {
			Log.log("remote.trace", " sending focus request ");
			server.send(socketName, "_messageBus.publish('focus', {})");
		});

		server.addHandlerLast(x -> x.equals("log"), (s, socket, address, payload) -> {

			if (logStack.size() > 0) logStack.get(logStack.size() - 1).accept("" + payload);

			return payload;
		});

		server.addHandlerLast(x -> x.equals("error"), (s, socket, address, payload) -> {

			if (logStack.size() > 0) errorStack.get(errorStack.size() - 1).accept("" + payload);

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("focus.window"), () -> socketName, (s, socket, address, payload) -> {
			find(Boxes.window, both()).findFirst().ifPresent(w -> w.requestRaise());
			return payload;
		});

		this.properties.put(outputFactory, x -> newOutput(x, "box.output", (m) -> new JSONStringer().object().key("type").value("success")
			    .key("message").value(m).endObject().toString()));
		this.properties
			    .put(outputErrorFactory, x -> newOutput(x, "box.error", (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer()
					.object().key("type").value("error").key("line").value((int) lineerror.first).key("message")
					.value(lineerror.second).endObject().toString()));


		server.addHandlerLast(Predicate.isEqual("text.updated"), () -> socketName, (s, socket, address, payload) -> {

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) System.err.println(" remote editor is talking about a box that isn't anywhere <" + p + ">");

			String prop = p.getString("property");

			String text = p.getString("text");

			if (prop == null) throw new IllegalArgumentException(" missing property <" + p + ">");

			if (text == null) throw new IllegalArgumentException(" missing text <" + p + ">");

			box.get().properties.put(new Dict.Prop<String>(prop), text);

			boxFeedback(box, new Vec4(0, 0, 0, 0.2f));

			Drawing.dirty(box.get());

			return payload;
		});


		server.addHandlerLast(Predicate.isEqual("store.cookie"), () -> socketName, (s, socket, address, payload) -> {

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) System.err.println(" remote editor is talking about a box that isn't anywhere <" + p + ">");

			String prop = p.getString("property");

			JSONObject text = p.getJSONObject("cookie");

			if (prop == null) throw new IllegalArgumentException(" missing property <" + p + ">");

			if (text == null) throw new IllegalArgumentException(" missing text <" + p + ">");


			Log.log("remote.cookie", " storing cookie to :" + ("_" + prop + "_cookie"));
			Log.log("remote.cookie", " cookie is :" + text.toString());
			box.get().properties.put(new Dict.Prop<String>("_" + prop + "_cookie"), text.toString());

			IO.persist(new Dict.Prop<String>("_" + prop + "_cookie"));

			boxFeedback(box, new Vec4(0, 0, 0, 0.8f));

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("execution.fragment"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", " inside execution fragment ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) throw new IllegalArgumentException(" no box called <" + box + ">");

			String prop = p.getString("property");

			if (box.get() != currentSelection)
				System.err.println(" (warning?) remote editor is trying to execute a box we're not editing ?");

			String text = p.getString("text");

			if (text == null) throw new IllegalArgumentException(" can't execute no text ");

			String returnAddress = p.getString("returnAddress");

			int lineoffset = p.has("lineoffset") ? p.getInt("lineoffset") : 0;

			Log.log("remote.debug", "lineoffset ;"+lineoffset+" "+p.has("lineoffset"));

			Execution.ExecutionSupport support = getExecution(box.get()).support(box.get(), new Dict.Prop<String>(prop));

			support.setLineOffsetForFragment(lineoffset);
			support.executeTextFragment(text, newOutput(box
				    .get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object()
				    .key("type").value("error").key("line").value((int) lineerror.first).key("message").value(lineerror.second)
				    .endObject().toString()), newOutput(box.get(), returnAddress, (m) -> new JSONStringer().object().key("type")
				    .value("success").key("message").value(m).endObject().toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("execution.all"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", " inside execution all ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) throw new IllegalArgumentException(" no box called <" + box + ">");

			String prop = p.getString("property");

			if (box.get() != currentSelection)
				System.err.println(" (warning?) remote editor is trying to execute a box we're not editing ?");

			String text = p.getString("text");

			if (text == null) throw new IllegalArgumentException(" can't execute no text ");

			String returnAddress = p.getString("returnAddress");

			Execution.ExecutionSupport support = getExecution(box.get()).support(box.get(), new Dict.Prop<String>(prop));
			support.executeAll(text, newOutput(box
				    .get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object()
				    .key("type").value("error").key("line").value((int) lineerror.first).key("message").value(lineerror.second)
				    .endObject().toString()), newOutput(box.get(), returnAddress, (m) -> new JSONStringer().object().key("type")
				    .value("success").key("message").value(m).endObject().toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});


		server.addHandlerLast(Predicate.isEqual("execution.begin"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", " inside execution begin ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) throw new IllegalArgumentException(" no box called <" + box + ">");

			String prop = p.getString("property");

			if (box.get() != currentSelection)
				System.err.println(" (warning?) remote editor is trying to execute a box we're not editing ?");

			String text = p.getString("text");

			if (text == null) throw new IllegalArgumentException(" can't execute no text ");

			box.get().properties.put(currentlyEditing, text);

			String returnAddress = p.getString("returnAddress");

			Execution.ExecutionSupport support = getExecution(box.get()).support(box.get(), new Dict.Prop<String>(prop));
			support.begin(newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer()
				    .object().key("type").value("error").key("line").value((int) lineerror.first).key("message")
				    .value(lineerror.second).endObject().toString()), newOutput(box.get(), returnAddress, (m) -> new JSONStringer()
				    .object().key("type").value("success").key("message").value(m).endObject().toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});
		server.addHandlerLast(Predicate.isEqual("execution.end"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", " inside execution end ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) throw new IllegalArgumentException(" no box called <" + box + ">");

			String prop = p.getString("property");

			if (box.get() != currentSelection)
				System.err.println(" (warning?) remote editor is trying to execute a box we're not editing ?");

			String text = p.getString("text");

			if (text == null) throw new IllegalArgumentException(" can't execute no text ");

			box.get().properties.put(currentlyEditing, text);

			String returnAddress = p.getString("returnAddress");

			Execution.ExecutionSupport support = getExecution(box.get()).support(box.get(), new Dict.Prop<String>(prop));
			support.end(newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer()
				    .object().key("type").value("error").key("line").value((int) lineerror.first).key("message")
				    .value(lineerror.second).endObject().toString()), newOutput(box.get(), returnAddress, (m) -> new JSONStringer()
				    .object().key("type").value("success").key("message").value(m).endObject().toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("request.completions"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", " inside request completions ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) throw new IllegalArgumentException(" no box called <" + box + ">");

			String prop = p.getString("property");

			if (box.get() != currentSelection)
				System.err.println(" (warning?) remote editor is trying to request completions in a box we're not editing ?");

			String text = p.getString("text");

			if (text == null) throw new IllegalArgumentException(" can't execute no text ");

			String returnAddress = p.getString("returnAddress");

			int line = p.getInt("line");
			int ch = p.getInt("ch");

			Execution.ExecutionSupport support = getExecution(box.get()).support(box.get(), new Dict.Prop<String>(prop));
			support.completion(text, line, ch, newOutput(box.get(), returnAddress, (responses) -> {

				JSONStringer stringer = new JSONStringer();
				stringer.array();
				for (Execution.Completion res : responses) {
					stringer.object();
					stringer.key("start").value(res.start);
					stringer.key("end").value(res.end);
					stringer.key("replaceWith").value(res.replacewith);
					stringer.key("info").value(res.info);
					stringer.endObject();
				}
				stringer.endArray();
				return stringer.toString();
			}));

			return payload;
		});
		server.addHandlerLast(Predicate.isEqual("request.imports"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", " inside request completions ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) throw new IllegalArgumentException(" no box called <" + box + ">");

			String prop = p.getString("property");

			if (box.get() != currentSelection)
				System.err.println(" (warning?) remote editor is trying to request completions in a box we're not editing ?");

			String text = p.getString("text");

			if (text == null) throw new IllegalArgumentException(" can't execute no text ");

			String returnAddress = p.getString("returnAddress");

			int line = p.getInt("line");
			int ch = p.getInt("ch");

			Execution.ExecutionSupport support = getExecution(box.get()).support(box.get(), new Dict.Prop<String>(prop));
			support.imports(text, line, ch, newOutput(box.get(), returnAddress, (responses) -> {

				JSONStringer stringer = new JSONStringer();
				stringer.array();
				for (Execution.Completion res : responses) {
					stringer.object();
					stringer.key("start").value(res.start);
					stringer.key("end").value(res.end);
					stringer.key("replaceWith").value(res.replacewith);
					stringer.key("info").value(res.info);
					stringer.key("header").value(res.header);
					stringer.endObject();
				}
				stringer.endArray();
				return stringer.toString();
			}));


			// todo: feedback in the UI that something is executing.

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("request.commands"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", " inside request commands ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));
			String prop = p.getString("property");
			String text = p.getString("text");
			String returnAddress = p.getString("returnAddress");
			int line = p.getInt("line");
			int ch = p.getInt("ch");

			// now we need to ask everybody if they have any commands to offer based on the above.

			//todo: handle no box case

			List<Map.Entry<Pair<String, String>, Runnable>> commands = (List<Map.Entry<Pair<String, String>, Runnable>>) box.get()
				    .find(RemoteEditor.commands, box.get().both()).flatMap(m -> m.get().entrySet().stream())
				    .collect(Collectors.toList());


			Log.log("remote.trace", " commands are :" + commands);

			JSONStringer stringer = new JSONStringer();
			stringer.array();
			callTable.clear();
			for (Map.Entry<Pair<String, String>, Runnable> r : commands) {
				String u = UUID.randomUUID().toString();
				callTable.put(u, r.getValue());
				stringer.object();
				stringer.key("name").value(r.getKey().first);
				stringer.key("info").value(r.getKey().second);
				stringer.key("call").value(u);
				stringer.endObject();
			}


			Log.log("remote.trace", " call table looks like :" + callTable);

			stringer.endArray();

			server.send(socketName, "_messageBus.publish('" + returnAddress + "', " + stringer.toString() + ")");

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("call.command"), () -> socketName, (s, socket, address, payload) -> {

			JSONObject p = (JSONObject) payload;
			String command = p.getString("command");

			Runnable r = callTable.get(command);

			if (r != null) {
				if (r instanceof ExtendedCommand) ((ExtendedCommand) r).begin(supportsPrompt(server, socketName), null);
				r.run();
			}

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("request.hotkeyCommands"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", " inside request commands ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));
			String prop = p.getString("property");
			String text = p.getString("text");
			String returnAddress = p.getString("returnAddress");
			int line = p.getInt("line");
			int ch = p.getInt("ch");
			String JSCommands = p.getJSONObject("allJSCommands").toString();
			HashMap<String, String> JSMap = new HashMap<>();
			HashMap<Pair<String, String>, Runnable> mergemap = new HashMap<>();

			for (String entry : JSCommands.substring(1, JSCommands.length()-1).replace("\"","").split(",") ) {
				String[] splitEntry = entry.split(":");
				JSMap.put(splitEntry[0], splitEntry[1]);
			}

			//todo: handle no box case

			List<Map.Entry<Pair<String, String>, Runnable>> commands = box.get()
				    .find(RemoteEditor.commands, box.get().both()).flatMap(m -> m.get().entrySet().stream())
				    .collect(Collectors.toList());

			for (String key : JSMap.keySet()) {
				mergemap.put(new Pair<>(key, JSMap.get(key)), new ExtendedCommand() {
					@Override
					public void begin(SupportsPrompt prompt, String alternativeChosen) { }

					@Override
					public void run() {	}
				});
			}

			commands.addAll(mergemap.entrySet());

			//Override the existing functionality in the menu to instead prompt for a new hotkey
			//And write the new hotkey to the properties file
			for (Map.Entry<Pair<String, String>, Runnable> currCommand : commands) {
				ExtendedCommand val = new ExtendedCommand() {
					public SupportsPrompt p;

					@Override
					public void begin(SupportsPrompt prompt, String alternativeChosen) {
						this.p = prompt;
					}

					@Override
					public void run() {

						Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

						//Prompt for a new hotkey
						p.prompt("set hotkey to...", m, new ExtendedCommand() {
							String altWas = null;

							@Override
							public void begin(SupportsPrompt prompt, String alternativeChosen) {
								altWas = alternativeChosen;
							}

							@Override
							public void run() {
								if (altWas != null) {

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

									//Find if the hotkey is already associated with a command
									//If so, determine the beginning and end of the command text for replacement with new command
									int commandBegin = -1;
									int commandEnd = -1;
									for (int i = 0; i < contents.length(); ++i){
										StringBuilder readCommand = new StringBuilder();
										char currChar;
										while ((currChar = contents.charAt(i)) != ':') {
											readCommand.append(currChar);
											++i;
										}
										if (readCommand.toString().equals(altWas)) {
											commandBegin = i+2;
											while ((contents.charAt(i)) != '\n') ++i;
											commandEnd = i;
											break;
										}
										while ((contents.charAt(i)) != '\n') ++i;
										readCommand.setLength(0);
									}

									//Replace the old command in contents with the new one or create a new command
									if (commandBegin > -1) {
										contents.delete(commandBegin, commandEnd);
										contents.insert(commandBegin, currCommand.getKey().first);
									} else {
										contents.append(altWas);
										contents.append(": ");
										contents.append(currCommand.getKey().first);
										contents.append("\n");
									}

									//Write the contents to the output file
									try ( BufferedWriter out = new BufferedWriter(new FileWriter(file)) ) {
										out.write(contents.toString());
									} catch (IOException x) {
										System.err.println("Error: Cannot open properties text file in write");
									}
								}
							}
						});
					}
				};
				currCommand.setValue(val);
			}


			Log.log("remote.trace", " commands are :" + commands);

			JSONStringer stringer = new JSONStringer();
			stringer.array();
			callTable.clear();
			for (Map.Entry<Pair<String, String>, Runnable> r : commands) {
				String u = UUID.randomUUID().toString();
				callTable.put(u, r.getValue());
				stringer.object();
				stringer.key("name").value(r.getKey().first);
				stringer.key("info").value(r.getKey().second);
				stringer.key("call").value(u);
				stringer.endObject();
			}


			Log.log("remote.trace", " call table looks like :" + callTable);

			stringer.endArray();

			server.send(socketName, "_messageBus.publish('" + returnAddress + "', " + stringer.toString() + ")");

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("call.alternative"), () -> socketName, (s, socket, address, payload) -> {

			JSONObject p = (JSONObject) payload;
			String command = p.getString("command");
			String text = p.getString("text");

			ExtendedCommand r = callTable_alternative;

			if (r != null) {
				if (r instanceof ExtendedCommand) ((ExtendedCommand) r).begin(supportsPrompt(server, socketName), text);
				r.run();
			}

			return payload;
		});

		selectionHasChanged = true;


	}

	protected SupportsPrompt supportsPrompt(Server server, String socketName) {

		return (prompt, commands1, alternative) -> {
			JSONStringer stringer = new JSONStringer();
			stringer.object();
			stringer.key("prompt");
			stringer.value(prompt);
			stringer.key("commands");
			stringer.array();
			callTable.clear();
			for (Map.Entry<Pair<String, String>, Runnable> r : commands1.entrySet()) {
				String u = UUID.randomUUID().toString();
				callTable.put(u, r.getValue());
				stringer.object();
				stringer.key("name").value(r.getKey().first);
				stringer.key("info").value(r.getKey().second);
				stringer.key("call").value(u);
				stringer.endObject();
			}
			stringer.endArray();

			if (alternative != null) {
				stringer.key("alternative");
				String u = UUID.randomUUID().toString();
				callTable_alternative = alternative;
				stringer.value(u);
			} else {
				callTable_alternative = null;
				stringer.key("alternative");
				callTable_alternative = alternative;
				stringer.value(null);
			}
			stringer.endObject();

			server.send(socketName, "_messageBus.publish('begin.commands', " + stringer.toString() + ")");
		};

	}

	static public void boxFeedback(Optional<Box> box, Vec4 color) {
		box.get().properties.putToMap(frameDrawing, "__edited__", expires(boxOrigin((bx) -> {

			FLine f = new FLine();
			f.rect(-5, -5, 10, 10);
			f.attributes.put(filled, true);
			f.attributes.put(stroked, false);
			f.attributes.put(StandardFLineDrawing.color, color);
			return f;

		}, new Vec2(1, 1)), 60));
		Drawing.dirty(box.get());
	}

	RateLimitingQueue<String, Pair<String, String>> rater = new RateLimitingQueue<String, Pair<String, String>>(20, 100) {
		@Override
		protected String groupFor(Pair<String, String> stringRunnablePair) {
			return stringRunnablePair.first;
		}

		@Override
		protected void send(String key, Collection<Pair<String, String>> value) {

			Log.log("remote.trace", " >> " + key + " " + value.size());

			if (value.size() > 1) {

				if (value.size() < 10) {
					String m = "";
					for (Pair<String, String> v : value) {
						m = m.concat((m.length() > 0 ? "," : "") + v.second);
					}
					server.send(socketName, "[" + m + "].forEach(function(q){ _messageBus.publish('" + key + "', q)})");
				} else
					//TODO: tell somebody we've dropped something
					server.send(socketName, "_messageBus.publish('" + key + "', " + value.iterator().next().second + ")");

			} else {
				server.send(socketName, "_messageBus.publish('" + key + "', " + value.iterator().next().second + ")");
			}
		}
	};

	protected <T> Consumer<T> newOutput(Box inside, String returnAddress, Function<T, String> toJson) {
		Consumer<T> c = x -> {
			String json = toJson.apply(x).trim();

			if (json.endsWith("}")) json = json.substring(0, json.length() - 1) + ",box:'" + inside.properties.get(IO.id) + "'}";

			rater.add(new Pair<>(returnAddress, json));
		};

		return c;
	}


	protected Optional<Box> findBoxByID(String uid) {
		return breadthFirst(downwards()).filter(x -> Util.safeEq(x.properties.get(IO.id), uid)).findFirst();
	}

	Dict.Prop<String> currentlyEditing;
	Box currentSelection;

	public void setCurrentlyEditingProperty(Dict.Prop<String> ed) {
		if (!Util.safeEq(currentlyEditing, ed)) {
			this.currentlyEditing = ed;
			changeSelection(currentSelection, currentlyEditing);
		}
	}

	public Dict.Prop<String> getCurrentlyEditingProperty() {
		return currentlyEditing;
	}

	public Box getCurrentlyEditing() {
		return currentSelection;
	}

	private void changeSelection(Box currentSelection, Dict.Prop<String> editingProperty) {

		Log.log("remote.trace", " publishing selection changed :" + currentSelection + " " + editingProperty);

		if (currentSelection == null || editingProperty == null) {
			server.send(socketName, "_messageBus.publish('selection.changed', {box:null, property:null, text:''})");
		} else {

			String text = currentSelection.properties.get(editingProperty);
			if (text == null) text = "";

			Log.log("remote.trace", " current text is :" + text);


			//todo: pass back two cookies for this box --- one persistant (saved to disk), one just for current things

			JSONObject buildMessage = new JSONObject();
			buildMessage.put("box", currentSelection.properties.get(IO.id));
			buildMessage.put("text", currentSelection.properties.getOr(currentlyEditing, () -> ""));
			buildMessage.put("property", currentlyEditing.getName());
			buildMessage.put("name", currentSelection.properties.get(Box.name));


			String cooked = currentSelection.properties.get(new Dict.Prop<String>("_" + editingProperty.getName() + "_cookie"));
			Log.log("remote.cookie", "cookie ns now :" + cooked);
			buildMessage.put("cookie", new JSONObject(cooked == null ? "{}" : cooked));


			Execution ex = getExecution(currentSelection);
			if (ex != null) {
				Execution.ExecutionSupport support = ex.support(currentSelection, editingProperty);
				String cmln = support.getCodeMirrorLanguageName();
				Log.log("remote.general", "langage :" + cmln);
				buildMessage.put("languageName", cmln);
				if (support != null) support.setFilenameForStacktraces("" + currentSelection);
			}

			Log.log("remote.trace", " message will be sent " + buildMessage.toString());

			Log.log("remote.trace", () -> "\n " + currentSelection.properties
				    .get(new Dict.Prop<JSONObject>("_" + editingProperty.getName() + "_cookie")) + "\n");

			server.send(socketName, "_messageBus.publish('selection.changed', " + buildMessage.toString() + ")");

			//todo: check for other editors?
			//watches.addWatch(editingProperty, "edited.property.changed");
		}
		currentlyEditing = editingProperty;
		this.currentSelection = currentSelection;
	}

	boolean selectionHasChanged = false;


	protected boolean update() {
		if (selectionHasChanged) {
			selectionHasChanged = false;
			Set<Box> selection = this.breadthFirst(downwards()).filter(x -> x.properties.isTrue(Mouse.isSelected, false))
				    .collect(Collectors.toSet());
			if (selection.size() != 1) {
				changeSelection(null, currentlyEditing);
			} else {
				changeSelection(selection.iterator().next(), currentlyEditing);
			}
		}
		return true;
	}


	public Execution getExecution(Box box) {
		return box.first(Execution.execution).orElseThrow(() -> new IllegalArgumentException("no execution found for box " + box));
	}


	/**
	 * A general purpose Send some JavaScript to a text editor call
	 */
	public void sendJavaScript(String javascript) {
		server.send(socketName, javascript);
	}

	public Util.ExceptionlessAutoCloasable pushToLogStack(Consumer<String> log, Consumer<String> error) {
		this.logStack.add(log);
		this.errorStack.add(error);
		return () -> {
			this.logStack.remove(log);
			this.errorStack.remove(error);
		};
	}

	public void popFromLogStack() {
		this.logStack.remove(this.logStack.size() - 1);
		this.errorStack.remove(this.errorStack.size() - 1);
	}

	// Function to gather all javascript files in a directory as a list of strings
	// (i.e. "somefile.js")
	// Arguments: Absolute directory location of .js files
	// Returns: List of file names
	private static List<String> findJSFiles(String dir){
		File[] files = new File(dir).listFiles();
		List<String> fileStrings = new ArrayList<>();
		for (File file : files) {
			if (!file.isDirectory() && file.toString().endsWith(".js")) {
				String fullPath = file.toString();
				fileStrings.add(fullPath.substring(fullPath.lastIndexOf('/')+1));
			}
		}

		return fileStrings;
	}

}
