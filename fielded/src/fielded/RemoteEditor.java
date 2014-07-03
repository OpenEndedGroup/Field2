package fielded;

import field.graphics.FLine;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.message.MessageQueue;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Quad;
import field.utility.Util;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fielded.webserver.RateLimitingQueue;
import fielded.webserver.Server;
import fielded.windowmanager.LinuxWindowTricks;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * connects to that WebSocket and does things via those message busses
 */
public class RemoteEditor extends Box {

	static public final Dict.Prop<Supplier<Map<Pair<String, String>, Runnable>>> commands = new Dict.Prop<>("commands").type().doc("commands injected into the editor as ctrl-space menu").toCannon();
	static public final Dict.Prop<RemoteEditor> editor = new Dict.Prop<>("editor").type().doc("the (remote) editor object").toCannon();
	static public final Dict.Prop<Function<Box, Consumer<String>>> outputFactory = new Dict.Prop<>("outputFactory");
	static public final Dict.Prop<Function<Box, Consumer<Pair<Integer, String>>>> outputErrorFactory = new Dict.Prop<>("outputErrorFactory");

	static public interface ExtendedCommand extends Runnable
	{
		public void begin(SupportsPrompt prompt, String alternativeChosen);
	}

	static public interface SupportsPrompt
	{
		public void prompt(String prompt, Map<Pair<String, String>, Runnable> options, ExtendedCommand alternative);
	}

	private final Server server;
	private final String socketName;
	private final MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue;
	private final Watches watches;

	LinkedHashMap<String, Runnable> callTable = new LinkedHashMap<>();
	ExtendedCommand callTable_alternative = null;

	public RemoteEditor(Server server, String socketName, Watches watches, MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue) {
		this.server = server;
		this.socketName = socketName;
		this.queue = queue;
		this.watches = watches;
		this.properties.put(editor, this);

		this.properties.putToMap(Boxes.insideRunLoop, "main.__watch_service__", (Supplier<Boolean>) this::update);

		watches.addWatch(Mouse.isSelected, "selection.changed");
		watches.addWatch(LinuxWindowTricks.lostFocus, "focus.editor");

		queue.register(Predicate.isEqual("selection.changed"), (c) -> {
			System.out.println(" selection changed message ");
			selectionHasChanged = true;
		});

		queue.register(Predicate.isEqual("focus.editor"), (c) -> {
			System.out.println(" sending focus request ");
			server.send(socketName, "_messageBus.publish('focus', {})");
		});

		server.addHandlerLast(Predicate.isEqual("focus.window"), () -> socketName, (s, socket, address, payload) -> {
			find(Boxes.window, both()).findFirst().ifPresent(w -> w.requestRaise());
			return payload;
		});

		this.properties.put(outputFactory, x -> newOutput(x, "box.output", (m) -> new JSONStringer().object().key("type").value("success").key("message").value(m).endObject().toString()));
		this.properties.put(outputErrorFactory, x -> newOutput(x, "box.error", (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object().key("type").value("error").key("line").value((int) lineerror.first).key("message").value(lineerror.second).endObject().toString()));

		System.out.println(" set default factories ");


		server.addHandlerLast(Predicate.isEqual("text.updated"), () -> socketName, (s, socket, address, payload) -> {

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent())
				System.err.println(" remote editor is talking about a box that isn't anywhere <" + p + ">");

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

			if (!box.isPresent())
				System.err.println(" remote editor is talking about a box that isn't anywhere <" + p + ">");

			String prop = p.getString("property");

			JSONObject text = p.getJSONObject("cookie");

			if (prop == null) throw new IllegalArgumentException(" missing property <" + p + ">");

			if (text == null) throw new IllegalArgumentException(" missing text <" + p + ">");


			System.out.println(" storing cookie to :" + ("_" + prop + "_cookie"));
			System.out.println(" cookie is :" + text);
			box.get().properties.put(new Dict.Prop<JSONObject>("_" + prop + "_cookie"), text);

			boxFeedback(box, new Vec4(0, 0, 0, 0.8f));

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("execution.fragment"), () -> socketName, (s, socket, address, payload) -> {

			System.out.println(" inside execution fragment ");

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
			support.executeTextFragment(text, newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object().key("type").value("error").key("line").value((int) lineerror.first).key("message").value(lineerror.second).endObject().toString()), newOutput(box.get(), returnAddress, (m) -> new JSONStringer().object().key("type").value("success").key("message").value(m).endObject().toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("execution.all"), () -> socketName, (s, socket, address, payload) -> {

			System.out.println(" inside execution all ");

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
			support.executeAll(text, newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object().key("type").value("error").key("line").value((int) lineerror.first).key("message").value(lineerror.second).endObject().toString()), newOutput(box.get(), returnAddress, (m) -> new JSONStringer().object().key("type").value("success").key("message").value(m).endObject().toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});


		server.addHandlerLast(Predicate.isEqual("execution.begin"), () -> socketName, (s, socket, address, payload) -> {

			System.out.println(" inside execution begin ");

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
			support.begin(newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object().key("type").value("error").key("line").value((int) lineerror.first).key("message").value(lineerror.second).endObject().toString()), newOutput(box.get(), returnAddress, (m) -> new JSONStringer().object().key("type").value("success").key("message").value(m).endObject().toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});
		server.addHandlerLast(Predicate.isEqual("execution.end"), () -> socketName, (s, socket, address, payload) -> {

			System.out.println(" inside execution end ");

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
			support.end(newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object().key("type").value("error").key("line").value((int) lineerror.first).key("message").value(lineerror.second).endObject().toString()), newOutput(box.get(), returnAddress, (m) -> new JSONStringer().object().key("type").value("success").key("message").value(m).endObject().toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("request.completions"), () -> socketName, (s, socket, address, payload) -> {

			System.out.println(" inside request completions ");

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


			// todo: feedback in the UI that something is executing.

			return payload;
		});
		server.addHandlerLast(Predicate.isEqual("request.imports"), () -> socketName, (s, socket, address, payload) -> {

			System.out.println(" inside request completions ");

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

			System.out.println(" inside request commands ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));
			String prop = p.getString("property");
			String text = p.getString("text");
			String returnAddress = p.getString("returnAddress");
			int line = p.getInt("line");
			int ch = p.getInt("ch");

			// now we need to ask everybody if they have any commands to offer based on the above.

			//todo: handle no box case

			List<Map.Entry<Pair<String, String>, Runnable>> commands =
				    (List<Map.Entry<Pair<String, String>, Runnable>>) box.get().find(RemoteEditor.commands, box.get().both()).flatMap(m -> m.get().entrySet().stream()).collect(Collectors.toList());


			System.out.println(" commands are :" + commands);

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


			System.out.println(" call table looks like :" + callTable);

			stringer.endArray();

			server.send(socketName, "_messageBus.publish('" + returnAddress + "', " + stringer.toString() + ")");

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("call.command"), () -> socketName, (s, socket, address, payload) -> {

			JSONObject p = (JSONObject) payload;
			String command = p.getString("command");

			Runnable r = callTable.get(command);

			if (r!=null)
			{
				if (r instanceof ExtendedCommand) ((ExtendedCommand)r).begin(supportsPrompt(server, socketName), null);
				r.run();
			}

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("call.alternative"), () -> socketName, (s, socket, address, payload) -> {

			JSONObject p = (JSONObject) payload;
			String command = p.getString("command");
			String text = p.getString("text");

			ExtendedCommand r = callTable_alternative;

			if (r!=null)
			{
				if (r instanceof ExtendedCommand) ((ExtendedCommand)r).begin(supportsPrompt(server, socketName), text);
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
				String u = UUID.randomUUID().toString();
				callTable_alternative = alternative;
				stringer.value(null);
			}
			stringer.endObject();

			server.send(socketName, "_messageBus.publish('begin.commands', " + stringer.toString() + ")");
		};

	}

	static public void boxFeedback(Optional<Box> box, Vec4 color) {
		box.get().properties.putToMap(FLineDrawing.frameDrawing, "__edited__", FLineDrawing.expires(FLineDrawing.boxOrigin((bx) -> {

			FLine f = new FLine();
			f.rect(-5, -5, 10, 10);
			f.attributes.put(FLineDrawing.filled, true);
			f.attributes.put(FLineDrawing.stroked, false);
			f.attributes.put(FLineDrawing.color, color);
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

			System.out.println(" >> " + key + " " + value.size());

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

			if (json.endsWith("}"))
				json = json.substring(0, json.length() - 1) + ",box:'" + inside.properties.get(IO.id) + "'}";

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

		System.out.println(" publishing selection changed :" + currentSelection + " " + editingProperty);

		if (currentSelection == null || editingProperty == null) {
			server.send(socketName, "_messageBus.publish('selection.changed', {box:null, property:null, text:''})");
		} else {


			String text = currentSelection.properties.get(editingProperty);
			if (text == null) text = "";

			System.out.println(" current text is :" + text);


			//todo: pass back two cookies for this box --- one persistant (saved to disk), one just for current things

			JSONObject buildMessage = new JSONObject();
			buildMessage.put("box", currentSelection.properties.get(IO.id));
			buildMessage.put("text", currentSelection.properties.getOr(currentlyEditing, () -> ""));
			buildMessage.put("property", currentlyEditing.getName());
			buildMessage.put("name", currentSelection.properties.get(Box.name));
			buildMessage.put("cookie", currentSelection.properties.get(new Dict.Prop<JSONObject>("_" + editingProperty.getName() + "_cookie")));

			System.out.println(" message will be sent " + buildMessage.toString());

			System.out.println("\n " + currentSelection.properties.get(new Dict.Prop<JSONObject>("_" + editingProperty.getName() + "_cookie")) + "\n");

			server.send(socketName, "_messageBus.publish('selection.changed', " + buildMessage.toString() + ")");
			//todo: set language? get it from execution?

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
			Set<Box> selection = this.breadthFirst(downwards()).filter(x -> x.properties.isTrue(Mouse.isSelected, false)).collect(Collectors.toSet());
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
}
