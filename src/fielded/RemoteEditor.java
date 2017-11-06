package fielded;

import com.google.common.collect.ArrayListMultimap;
import field.app.RunLoop;
import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.message.MessageQueue;
import field.utility.*;
import fieldbox.FieldBox;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.Initiators;
import fieldbox.execution.Completion;
import fieldbox.execution.CompletionStats;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fieldcef.plugins.TextEditor;
import fielded.boxbrowser.TransientCommands;
import fielded.webserver.RateLimitingQueue;
import fielded.webserver.Server;
import fielded.windowmanager.LinuxWindowTricks;
import org.java_websocket.WebSocket;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;

import static field.graphics.StandardFLineDrawing.filled;
import static field.graphics.StandardFLineDrawing.stroked;
import static fieldbox.boxes.FLineDrawing.*;

/**
 * connects to that WebSocket and does things via those message buses
 */
public class RemoteEditor extends Box {

	static public final Dict.Prop<EditorUtils> editorUtils = new Dict.Prop<EditorUtils>("editorUtils").toCanon()
		.doc("utility class for manipulating the editor at a high level");

	static public final Dict.Prop<Supplier<Map<Pair<String, String>, Runnable>>> hotkeyCommands = new Dict.Prop<>("hotkeyCommands").type()
		.doc("commands injected into the editor as hotkey menuSpecs")
		.toCanon();
	static public final Dict.Prop<RemoteEditor> editor = new Dict.Prop<>("editor").type()
		.doc("the (remote) editor object")
		.toCanon();
	static public final Dict.Prop<Function<Box, Consumer<String>>> outputFactory = new Dict.Prop<>("outputFactory");
	static public final Dict.Prop<Function<Box, Consumer<Pair<Integer, String>>>> outputErrorFactory = new Dict.Prop<>("outputErrorFactory");
	static public final Dict.Prop<Function<Box, BiConsumer<String, JSONObject>>> outputMessageFactory = new Dict.Prop<>("outputMessageFactory");

	static public final Dict.Prop<String> defaultEditorProperty = new Dict.Prop<String>("defaultEditorProperty").type()
		.doc("The property that the editor will switch to. Will default to 'code' if not set.");
	private final Server server;

	private String socketName;

	private final MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue;
	private final Watches watches;
	public List<Consumer<String>> logStack = new ArrayList<>();
	public List<Consumer<String>> errorStack = new ArrayList<>();
	public HashMap<String, String> previousClipboards = new LinkedHashMap<>();
	Commands commandHelper = new Commands();

	RateLimitingQueue<String, Pair<String, String>> rater = new RateLimitingQueue<>(20, 100) {

		Deque<Long> overflowedAt = new LinkedList<>();

		@Override
		protected String groupFor(Pair<String, String> stringRunnablePair) {
			return stringRunnablePair.first;
		}

		String lastSend = "";
		boolean warned = false;
		long prevTick = RunLoop.tick;

		@Override
		protected void send(String key, Collection<Pair<String, String>> value) {

//			if (value.size() == 0) return;
//
//			boolean unequal = false;
//			for (Pair<String, String> v : value)
//
//				if (!Util.safeEq(v.second, lastSend)) {
//					unequal = true;
//					break;
//				}
//
//			if (!unequal && RunLoop.tick - prevTick < 2) {
//				prevTick = RunLoop.tick;
//
//
//				if (!warned) {
////					server.send(socketName, "_messageBuss.publish(" + key + ", 'message repeats')+");
//					warned = true;
//					return;
//				} else return;
//			}
//			prevTick = RunLoop.tick;
//			warned = false;
//			lastSend = value.iterator()
//				.next().second.toString();
//
//			Log.log("remote.trace", () -> " >> " + key + " " + value.size());
//
//			if (value.size() > 1) {
//
//				if (value.size() > 10) {
//					overflowedAt.addLast(RunLoop.tick);
//					if (overflowedAt.size() > 4) overflowedAt.removeFirst();
//				}
//
//				if (overflowedAt.size() > 3 && overflowedAt.getLast() - overflowedAt.getFirst() < 10) {
//					//TODO: tell somebody we've dropped something
//					server.send(socketName, "_messageBus.publish('" + key + "', " + value.iterator()
//						.next().second + ")");
//				} else {
//					String m = "";
//					for (Pair<String, String> v : value) {
//						m = m.concat((m.length() > 0 ? "," : "") + v.second);
//					}
//					server.send(socketName, "[" + m + "].forEach(function(q){ _messageBus.publish('" + key + "', q)})");
//				}
//
//			} else {


//			System.out.println(" message :"+socketName+" -> "+value);

			for(Pair<String, String> v : value)
				server.send(socketName, "_messageBus.publish('" + key + "', " + v.second + ")");
//			}
		}
	};

	ArrayListMultimap<String, Runnable> whenSelected = ArrayListMultimap.create();

	Dict.Prop<String> currentlyEditing;
	Box currentSelection;
	boolean selectionHasChanged = false;

	AtomicInteger current_ln = new AtomicInteger();
	AtomicReference<String> completionHelp = new AtomicReference<>("");


	public RemoteEditor(Server server, String sn, Watches watches, MessageQueue<Quad<Dict.Prop, Box, Object, Object>, String> queue) {
		this.server = server;
		this.socketName = sn; // we now let this vary on request
		this.queue = queue;
		this.watches = watches;
		this.properties.put(editor, this);
		this.properties.put(Boxes.dontSave, true);

		this.properties.putToMap(Boxes.insideRunLoop, "main.__watch_service__", this::update);
		this.properties.put(editorUtils, new EditorUtils(this));

		watches.addWatch(Mouse.isSelected, "selection.changed");
		watches.addWatch(LinuxWindowTricks.lostFocus, "focus.editor");

		queue.register(Predicate.isEqual("selection.changed"), (c) -> {
			Log.log("remote.trace", () -> " selection changed message ");
			selectionHasChanged = true;
		});

		queue.register(Predicate.isEqual("focus.editor"), (c) -> {
			Log.log("remote.trace", () -> " sending focus request ");
			server.send(socketName, "_messageBus.publish('focus', {})");
		});

		server.addHandlerLast(x -> x.equals("log"), (s, socket, address, payload) -> {

			if (logStack.size() > 0) logStack.get(logStack.size() - 1)
				.accept("" + payload);

			return payload;
		});

		server.addHandlerLast(x -> x.equals("error"), (s, socket, address, payload) -> {

			if (logStack.size() > 0) errorStack.get(errorStack.size() - 1)
				.accept("" + payload);

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("focus.window"), () -> socketName, (s, socket, address, payload) -> {
			find(Boxes.window, both()).findFirst()
				.ifPresent(w -> w.requestRaise());
			return payload;
		});

		this.properties.put(outputFactory, x -> newOutput(x, "box.output", (m) -> new JSONStringer().object()
			.key("type")
			.value("success")
			.key("message")
			.value(m)
			.key("append")
			.value(true)
			.endObject()
			.toString()));

		this.properties.put(outputErrorFactory, x -> newOutput(x, "box.error", (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object()
			.key("type")
			.value("error")
			.key("line")
			.value((int) lineerror.first)
			.key("append")
			.value(true)
			.key("message")
			.value(lineerror.second)
			.endObject()
			.toString()));

		this.properties.put(outputMessageFactory, x -> (address, payload) -> {
			if (x != null) payload.put("box", x.properties.get(IO.id));


			rater.add(new Pair<>(address, payload.toString()));
		});


		this.properties.put(Execution.directedOutput, z -> {
			// TODO: multiple properties?
			if (z.first == currentSelection) {
				rater.add(new Pair<String, String>("box.output.directed", new JSONStringer().object()
					.key("box")
					.value(z.first.properties.getOrConstruct(IO.id))
					.key("append")
					.value(z.fourth)
					.key("line")
					.value(z.second)
					.key("message")
					.value(z.third)
					.endObject()
					.toString()));
			} else {
				synchronized (whenSelected) {
					List<Runnable> r = whenSelected.get(z.first.properties.getOrConstruct(IO.id));
					try {
						if (r != null && r.size() > 20)
							whenSelected.replaceValues(z.first.properties.getOrConstruct(IO.id), r.subList(20, r.size()));
					} catch (ConcurrentModificationException e) {
					}
					whenSelected.put(z.first.properties.getOrConstruct(IO.id), () -> {
						rater.add(new Pair<String, String>("box.output.directed", new JSONStringer().object()
							.key("box")
							.value(z.first.properties.getOrConstruct(IO.id))
							.key("append")
							.value(z.fourth)
							.key("line")
							.value(z.second)
							.key("message")
							.value(z.third)
							.endObject()
							.toString()));
					});
				}
			}
		});

		server.addHandlerLast((s, socket, address, payload) -> {
			if (payload instanceof JSONObject)
				TransientCommands.transientCommands.handle(address, (JSONObject) payload, null);
			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("text.updated"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;

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
			Callbacks.call(box.get(), Callbacks.onEdit);

			String disabledRanges = p.getString("disabledRanges");

			box.get().properties.put(new Dict.Prop<List<Pair<Integer, Integer>>>("_" + prop + "_disabled"), parseDisabledRanges(disabledRanges));

			int c = p.getInt("ch");
			int ln = p.getInt("line");

			int v = current_ln.incrementAndGet();
			RunLoop.main.workerPool.submit(() -> {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (current_ln.get() != v) return;

				if (true) return;

				Execution.ExecutionSupport support = getExecution(box.get(), new Dict.Prop<String>(prop)).support(box.get(), new Dict.Prop<String>(prop));

				support.completion(text, ln, c, cc -> {
					if (cc.size() > 0) {

						CompletionStats.stats.autosuggest(cc);

						// Tern just loves decodeURIComponent
						if (cc.size() > 20 && cc.get(0).replacewith.equals("decodeURIComponent"))
							return;

						String also = "";
						if (cc.size() < 20 && cc.size() > 1) {
							for (int i = 1; i < cc.size(); i++) {
								also += "<b>" + cc.get(i).replacewith + "</b>, ";
								if (also.length() > 60) {
									also = also.substring(0, also.length() - 2) + "...";
									break;
								}
							}
							if (also.length() <= 60)
								also = also.substring(0, also.length() - 2);
							also = "<br><i>also</i>  " + also;
							also = "<div style='font-size:80%'>" + also + "</div>";
						}

						String doc = cc.get(0).info.replace("&mdash;", "<br>");
						if (doc.length()>200)
							doc = doc.substring(0, 200)+"...";

						completionHelp.set("<div class='comp-space'>" + cc.get(0).replacewith + "</div> <span class=doc>" + doc + "</span>" + also);


					}
				}, false);
			});

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("clipboard.getNewClipboard"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;
			Log.log("clipboardfix", () -> "sync clipboard");
			JSONObject p = (JSONObject) payload;
			String returnAddress = p.getString("returnAddress");

			FieldBoxWindow w = this.first(Boxes.window, both()).get();

			String current = w.getCurrentClipboard();
			final String finalCurrent = current;
			Log.log("clipboardfix", () -> "clipboard is " + finalCurrent);

			String was = previousClipboards.get(socketName);
			if (was == null || !was.equals(current) && current != null) {
				Log.log("clipboardfix", () -> "sending " + finalCurrent);
				previousClipboards.put(socketName, current);

				current = JSONObject.quote(current);

				s.send(socket, "_messageBus.publish('" + returnAddress + "', " + current + ")");
			} else {
				previousClipboards.put(socketName, current);
				s.send(socket, "_messageBus.publish('" + returnAddress + "', " + "null" + ")");
			}


			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("clipboard.setClipboard"), () -> socketName, (s, socket, address, payload) -> {

			this.socketName = socketName;
			Log.log("clipboardfix", () -> "set clipboard to " + payload);
			JSONObject p = (JSONObject) payload;

			FieldBoxWindow w = this.first(Boxes.window, both())
				.get();

			w.setCurrentClipboard(p.getString("value"));

			String was = previousClipboards.put(socketName, p.getString("value"));

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("store.cookie"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) {
				Log.log("remote.cookie", () -> " remote editor is talking about a box that isn't anywhere <" + p + ">");
				return payload;
			}

			String prop = p.getString("property");

			JSONObject text = p.getJSONObject("cookie");

			if (prop == null) throw new IllegalArgumentException(" missing property <" + p + ">");

			if (text == null) throw new IllegalArgumentException(" missing text <" + p + ">");


			Log.log("remote.cookie", () -> " storing cookie to :" + ("_" + prop + "_cookie"));
			Log.log("remote.cookie", () -> " cookie is :" + text.toString());
			box.get().properties.put(new Dict.Prop<String>("_" + prop + "_cookie"), text.toString());

			IO.persist(new Dict.Prop<String>("_" + prop + "_cookie"));

			boxFeedback(box, new Vec4(0, 0, 0, 0.8f));

			return payload;
		});

		server.addHandlerLast(x -> x.startsWith("execution.fragment"), () -> socketName, (s, socket, address, payload) -> {

			this.socketName = socketName;
			Log.log("remote.trace", () -> " inside execution fragment ");

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

			Log.log("remote.debug", () -> "lineoffset ;" + lineoffset + " " + p.has("lineoffset"));

			String suffix = address.length() < "execution.fragment.".length() ? "" : address.substring("execution.fragment.".length());

			Execution.ExecutionSupport support = getExecution(box.get(), new Dict.Prop<String>(prop)).support(box.get(), new Dict.Prop<String>(prop));
			support.setLineOffsetForFragment(lineoffset, new Dict.Prop<String>(prop));

			List<Pair<Integer, Integer>> dis = parseDisabledRanges(p.getString("disabledRanges"));

			if (dis != null) {
				text = DisabledRangeHelper.rewriteWithDisabledRanges(text, "/* -- start -- ", "-- end -- */", dis, lineoffset);
			}


			support.executeTextFragment(text, suffix, newOutput(box.get(), returnAddress, (m) -> new JSONStringer().object()
				.key("type")
				.value("success")
				.key("message")
				.value(m)
				.endObject()
				.toString()), newOutput(box.get(), returnAddress,
				(Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object()
					.key("type")
					.value("error")
					.key("line")
					.value((int) lineerror.first)
					.key("message")
					.value(lineerror.second)
					.endObject()
					.toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));
			Drawing.dirty(box.get());



			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("execution.all"), () -> socketName, (s, socket, address, payload) -> {

			this.socketName = socketName;
			Log.log("remote.trace", () -> " inside execution all ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));

			if (!box.isPresent()) throw new IllegalArgumentException(" no box called <" + box + ">");

			String prop = p.getString("property");

			if (box.get() != currentSelection)
				System.err.println(" (warning?) remote editor is trying to execute a box we're not editing ?");

			String text = p.getString("text");

			if (text == null) throw new IllegalArgumentException(" can't execute no text ");

			List<Pair<Integer, Integer>> dis = parseDisabledRanges(p.getString("disabledRanges"));

			if (dis != null) {
				text = DisabledRangeHelper.rewriteWithDisabledRanges(text, "/* -- start -- ", "-- end -- */", dis);
			}


			String returnAddress = p.getString("returnAddress");

			Execution.ExecutionSupport support = getExecution(box.get(), new Dict.Prop<String>(prop)).support(box.get(), new Dict.Prop<String>(prop));
			support.executeAll(text, newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object()
					.key("type")
					.value("error")
					.key("line")
					.value((int) lineerror.first)
					.key("message")
					.value(lineerror.second)
					.endObject()
					.toString()),
				newOutput(box.get(), returnAddress, (m) -> new JSONStringer().object()
					.key("type")
					.value("success")
					.key("message")
					.value(m)
					.endObject()
					.toString()));
			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));

			return payload;
		});


		server.addHandlerLast(Predicate.isEqual("execution.begin"), () -> socketName, (s, socket, address, payload) -> {

			this.socketName = socketName;
			Log.log("remote.trace", () -> " inside execution begin ");

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

			Execution.ExecutionSupport support = getExecution(box.get(), new Dict.Prop<String>(prop)).support(box.get(), new Dict.Prop<String>(prop));
			support.begin(newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object()
					.key("type")
					.value("error")
					.key("line")
					.value((int) lineerror.first)
					.key("message")
					.value(lineerror.second)
					.endObject()
					.toString()), newOutput(box.get(), returnAddress,
				(m) -> new JSONStringer().object()
					.key("type")
					.value("success")
					.key("message")
					.value(m)
					.endObject()
					.toString()),
				Collections.singletonMap("_t", Initiators.constant(box.get(), 0.5)), true);

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));
			Drawing.dirty(box.get());

			return payload;
		});
		server.addHandlerLast(Predicate.isEqual("execution.end"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", () -> " inside execution end ");

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

			Execution.ExecutionSupport support = getExecution(box.get(), new Dict.Prop<String>(prop)).support(box.get(), new Dict.Prop<String>(prop));
			support.end(newOutput(box.get(), returnAddress, (Function<Pair<Integer, String>, String>) (lineerror) -> new JSONStringer().object()
				.key("type")
				.value("error")
				.key("line")
				.value((int) lineerror.first)
				.key("message")
				.value(lineerror.second)
				.endObject()
				.toString()), newOutput(box.get(), returnAddress,
				(m) -> new JSONStringer().object()
					.key("type")
					.value("success")
					.key("message")
					.value(m)
					.endObject()
					.toString()));

			boxFeedback(box, new Vec4(0, 0.5f, 0.3f, 0.5f));
			Drawing.dirty(box.get());

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("notify.completion"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;
			JSONObject p = (JSONObject) payload;
			CompletionStats.stats.notify(p.getString("uuid"));
			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("request.completions"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;

			Log.log("remote.trace", () -> " inside request completions ");

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

			Execution.ExecutionSupport support = getExecution(box.get(), new Dict.Prop<String>(prop)).support(box.get(), new Dict.Prop<String>(prop));

			support.completion(text, line, ch, newOutput(box.get(), returnAddress, (responses) -> {

				CompletionStats.stats.autosuggest(responses);

				JSONStringer stringer = new JSONStringer();
				stringer.array();
				for (Completion res : responses) {
					stringer.object();
					stringer.key("start")
						.value(res.start);
					stringer.key("end")
						.value(res.end);
					stringer.key("replaceWith")
						.value(res.replacewith);
					stringer.key("uuid")
						.value(res.uuid);
					stringer.key("info")
						.value(res.info);
					stringer.endObject();
				}
				stringer.endArray();


				return stringer.toString();
			}), true);

			return payload;
		});
		server.addHandlerLast(Predicate.isEqual("request.imports"), () -> socketName, (s, socket, address, payload) -> {

			Log.log("remote.trace", () -> " inside request completions ");

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

			Execution.ExecutionSupport support = getExecution(box.get(), new Dict.Prop<String>(prop)).support(box.get(), new Dict.Prop<String>(prop));
			support.imports(text, line, ch, newOutput(box.get(), returnAddress, (responses) -> {

				JSONStringer stringer = new JSONStringer();
				stringer.array();
				for (Completion res : responses) {
					stringer.object();
					stringer.key("start")
						.value(res.start);
					stringer.key("end")
						.value(res.end);
					stringer.key("replaceWith")
						.value(res.replacewith);
					stringer.key("info")
						.value(res.info);
					stringer.key("header")
						.value(res.header);
					stringer.endObject();
				}
				stringer.endArray();
				return stringer.toString();
			}));


			// todo: feedback in the UI that something is executing.

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("request.commands"), () -> socketName, (s, socket, address, payload) -> {

			this.socketName = socketName;
			Log.log("remote.trace", () -> " inside request commands ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));
			String prop = p.getString("property");
			String text = p.getString("text");
			String returnAddress = p.getString("returnAddress");
			int line = p.getInt("line");
			int ch = p.getInt("ch");

			commandHelper.requestCommands(box, prop, text, x -> s.send(socket, "_messageBus.publish('" + returnAddress + "', " + x + ")"), line, ch);

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("call.commandByName"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;

			JSONObject p = (JSONObject) payload;
			String command = p.getString("command");

			CompletionStats.stats.notify(command);

			find(Commands.commands, both()).flatMap(m -> m.get()
				.entrySet()
				.stream())
				.filter(m -> m.getKey().first.trim()
					.toLowerCase()
					.equals(command.trim()
						.toLowerCase()))
				.findFirst()
				.ifPresent(m -> {
					Runnable r = m.getValue();
					if (r instanceof ExtendedCommand) ((ExtendedCommand) r).begin(
						commandHelper.supportsPrompt(x -> server.send(socketName, "_messageBus.publish('begin.commands', " + x + ")")), null);
					r.run();
				});

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("call.command"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;

			JSONObject p = (JSONObject) payload;
			String command = p.getString("command");

			Runnable r = commandHelper.callTable.get(command);

			String name = commandHelper.callTableName.get(command);
			CompletionStats.stats.notify(name);

			if (r != null) {
				if (r instanceof ExtendedCommand)
					((ExtendedCommand) r).begin(commandHelper.supportsPrompt(x -> server.send(socketName, "_messageBus.publish('begin.commands', " + x + ")")), null);
				r.run();
			}

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("request.hotkeyCommands"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;

			Log.log("remote.trace", () -> " inside request commands ");

			JSONObject p = (JSONObject) payload;

			Optional<Box> box = findBoxByID(p.getString("box"));
			String prop = p.getString("property");
			String text = p.getString("text");
			String returnAddress = p.getString("returnAddress");
			int line = p.getInt("line");
			int ch = p.getInt("ch");

			JSONObject allJSCommands = p.getJSONObject("allJSCommands");


			//todo: handle no box case

			List<Map.Entry<Pair<String, String>, Runnable>> commands = box.get()
				.find(Commands.commands, box.get()
					.both())
				.flatMap(m -> m.get()
					.entrySet()
					.stream())
				.collect(Collectors.toList());

			Map<Pair<String, String>, Runnable> mergeMap = new LinkedHashMap<>();
			for (Object key : allJSCommands.keySet()) {
				mergeMap.put(new Pair<>("" + key, allJSCommands.getJSONArray("" + key)
					.getString(0)), wrapCommandForHotkeys(allJSCommands.getJSONArray("" + key)
					.getString(1)));
			}


			for (Map.Entry<Pair<String, String>, Runnable> currCommand : commands) {
				ExtendedCommand val = wrapCommandForHotkeys("performCommand('" + currCommand.getKey().first + "')");
				currCommand.setValue(val);
			}

			commands.addAll(mergeMap.entrySet());

			Log.log("remote.trace", () -> " commands are :" + commands);

			JSONStringer stringer = new JSONStringer();
			stringer.array();
			commandHelper.callTable.clear();
			for (Map.Entry<Pair<String, String>, Runnable> r : commands) {
				String u = UUID.randomUUID()
					.toString();
				commandHelper.callTable.put(u, r.getValue());
				stringer.object();
				stringer.key("name")
					.value(r.getKey().first);
				stringer.key("info")
					.value(r.getKey().second);
				stringer.key("call")
					.value(u);
				stringer.endObject();
			}


			Log.log("remote.trace", () -> " call table looks like :" + commandHelper.callTable);

			stringer.endArray();

			server.send(socketName, "_messageBus.publish('" + returnAddress + "', " + stringer.toString() + ")");

			return payload;
		});

		server.addHandlerLast(Predicate.isEqual("call.alternative"), () -> socketName, (s, socket, address, payload) -> {
			this.socketName = socketName;

			JSONObject p = (JSONObject) payload;
			String command = p.getString("command");
			String text = p.getString("text");

			ExtendedCommand r = commandHelper.callTable_alternative;

			if (r != null) {
				if (r instanceof ExtendedCommand)
					r.begin(commandHelper.supportsPrompt(x -> server.send(socketName, "_messageBus.publish('begin.commands', " + x + ")")), text);
				r.run();
			}

			return payload;
		});

		selectionHasChanged = true;


	}

	static public void boxFeedback(Optional<Box> box, Vec4 color) {
		boxFeedback(box, color, "__edited__", 0, 60);
	}

	static public void boxFeedback(Optional<Box> box, Vec4 color, String name, int index, int duration) {

		if (box.get().properties.get(frameDrawing) != null) // we only decorate things that are drawn
			box.get().properties.putToMap(frameDrawing, name, expires(boxOrigin((bx) -> {

				FLine f = new FLine();
				f.rect(-5 + index * 10, -5 + Math.min(0, index) * 10, 10, 10);
				f.attributes.put(filled, true);
				f.attributes.put(stroked, false);
				f.attributes.put(StandardFLineDrawing.color, color);
				f.attributes.put(FLineDrawing.layer, "glass2");
				return f;

			}, new Vec2(1, 1)), duration));
		Drawing.dirty(box.get(), "glass2");
	}

	static public void removeBoxFeedback(Optional<Box> box, String name) {
		if (box.get().properties.get(frameDrawing) != null) // we only decorate things that are drawn
			box.get().properties.removeFromMap(frameDrawing, name);
	}

	// Function to gather all javascript files in a directory as a list of strings
	// (i.e. "somefile.js")
	// Arguments: Absolute directory location of .js files
	// Returns: List of file names
	private static List<String> findJSFiles(String dir) {
		File[] files = new File(dir).listFiles();
		List<String> fileStrings = new ArrayList<>();
		for (File file : files) {
			if (!file.isDirectory() && file.toString()
				.endsWith(".js")) {
				String fullPath = file.toString();
				fileStrings.add(fullPath.substring(fullPath.lastIndexOf('/') + 1));
			}
		}

		return fileStrings;
	}

	private List<Pair<Integer, Integer>> parseDisabledRanges(String disabledRanges) {
		if (disabledRanges == null) return null;
		return DisabledRangeHelper.parseDisabledRanges(disabledRanges);
	}

	protected ExtendedCommand wrapCommandForHotkeys(String text) {
		return new ExtendedCommand() {
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
						altWas = "";
						String[] indivKeys = alternativeChosen.trim()
							.toLowerCase()
							.split("-");
						for (String key : indivKeys) {
							altWas += key.substring(0, 1)
								.toUpperCase() + key.substring(1) + "-";
						}
						altWas = altWas.substring(0, altWas.length() - 1);
					}

					@Override
					public void run() {
						if (altWas == null) return;

						//Set up file reading
						File file = new File(System.getProperty("user.home") + "/.field/hotkeys.txt");
						String contents = "";

						//Read properties text file into a string (contents)
						try (BufferedReader in = new BufferedReader(new FileReader(file))) {
							while (in.ready()) contents += in.readLine() + "\n";
						} catch (IOException x) {
							Log.log("hotkeys.error", () -> "Error: Cannot open properties text file in read, file is " + file);
							Log.log("hotkeys.error", () -> x);
						}

						String[] lines = contents.split("\n");
						String next = "";
						int n = 1;
						for (String line : lines) {
							String[] parts = line.split(":");
							if (parts.length != 2) {
								final int finalN = n;
								Log.log("hotkeys.error", () -> "couldn't parse <" + line + "> in file, on line " + finalN);
							} else {
								if (parts[0].trim()
									.equals(altWas)) {
									// filter it out
								} else {
									next += line + "\n";
								}
							}
							n++;
						}

						next += altWas + ":" + text;

						//Write the contents to the output file
						try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
							out.write(next);
						} catch (IOException x) {
							Log.log("hotkeys.error", () -> "Error: Cannot open properties text file in write, file is " + file);
							Log.log("hotkeys.error", () -> x);
						}

						//now that the contents have been written to the output file we update the hotkeys
						StringBuilder propertiesContents = new StringBuilder();
						//Read properties text file into a string (contents)
						try (BufferedReader in = new BufferedReader(new FileReader(file))) {
							int curr;
							while ((curr = in.read()) != -1) {
								propertiesContents.append((char) curr);
							}
							in.close();
						} catch (IOException x) {
							System.err.println("Error: Cannot open properties text file in read");
						}
						for (String line : propertiesContents.toString()
							.split("\n")) {
							String[] splitLine = line.split(":");
							Log.log("hotkeys.debug", () -> " line is :" + splitLine.length + " <" + line + ">");
							if (splitLine.length > 1) {
								sendJavaScript("extraKeys[\"" + splitLine[0].trim() + "\"] = function (cm) {" + splitLine[1].trim() + ";}");
							}
						}

					}
				});
			}
		};
	}

	protected <T> Consumer<T> newOutput(Box inside, String returnAddress, Function<T, String> toJson) {
		Consumer<T> c = x -> {
			String json = toJson.apply(x)
				.trim();

			if (json.endsWith("}"))
				json = json.substring(0, json.length() - 1) + ",box:'" + inside.properties.get(IO.id) + "'}";

			rater.add(new Pair<>(returnAddress, json));
		};

		return c;
	}

	protected Optional<Box> findBoxByID(String uid) {
		return breadthFirst(both()).filter(x -> Util.safeEq(x.properties.get(IO.id), uid))
			.findFirst();
	}

	public Dict.Prop<String> getCurrentlyEditingProperty() {
		return currentlyEditing;
	}

	public void setCurrentlyEditingProperty(Dict.Prop<String> ed) {
		if (!Util.safeEq(currentlyEditing, ed)) {
			this.currentlyEditing = ed;
			changeSelection(currentSelection, currentlyEditing);
		}
	}

	public Box getCurrentlyEditing() {
		return currentSelection;
	}

	public void changeSelection(Box currentSelection, Dict.Prop<String> editingProperty) {

		try {
			Log.log("remote.trace", () -> " publishing selection changed :" + currentSelection + " " + editingProperty);

			if (currentSelection == null || editingProperty == null) {
				server.send(socketName, "_messageBus.publish('selection.changed', {box:null, property:null, text:''})");
			} else {

				String text = currentSelection.properties.get(editingProperty);
				if (text == null) text = "";

				final String finalText = text;
				Log.log("remote.trace", () -> " current text is :" + finalText);


				//todo: pass back two cookies for this box --- one persistant (saved to disk), one just for current things

				JSONObject buildMessage = new JSONObject();
				buildMessage.put("box", currentSelection.properties.get(IO.id));
				buildMessage.put("text", currentSelection.properties.getOr(editingProperty, () -> ""));
				buildMessage.put("property", editingProperty.getName());
				buildMessage.put("name", currentSelection.properties.get(Box.name));


				String cooked = currentSelection.properties.get(new Dict.Prop<String>("_" + editingProperty.getName() + "_cookie"));
				IO.persist(new Dict.Prop<String>("_" + editingProperty.getName() + "_cookie"));

				Log.log("remote.cookie", () -> "cookie ns now :" + cooked);
				buildMessage.put("cookie", new JSONObject(cooked == null ? "{}" : cooked));


				Execution ex = getExecution(currentSelection, editingProperty);
				if (ex != null) {
					Execution.ExecutionSupport support = ex.support(currentSelection, editingProperty);
					if (support != null) {
						String cmln = support.getCodeMirrorLanguageName();
						Log.log("remote.general", () -> "langage :" + cmln);
						buildMessage.put("languageName", cmln);
						support.setFilenameForStacktraces("" + currentSelection + "/" + currentSelection.properties.getOrConstruct(IO.id));
					} else {
					}
				} else {
					// this can happen when we're editing something that isn't 'code'
					String cmln = FieldBox.fieldBox.io.getLanguageForProperty(editingProperty);
					String finalCmln = cmln;
					Log.log("remote.general", () -> "langage :" + finalCmln);
					if (cmln == null || cmln.trim().equals("")) cmln = "javascript";
					buildMessage.put("languageName", cmln);

				}
				Log.log("remote.trace", () -> " message will be sent " + buildMessage.toString());

				Log.log("remote.trace", () -> "\n " + currentSelection.properties.get(new Dict.Prop<JSONObject>("_" + editingProperty.getName() + "_cookie")) + "\n");

				server.send(socketName, "_messageBus.publish('selection.changed', " + buildMessage.toString() + ")");

				List<Runnable> q;
				synchronized (whenSelected) {
					q = whenSelected.removeAll(currentSelection.properties.getOrConstruct(IO.id));
				}
				if (q != null) {
					q.forEach(x -> x.run());
				}
				//todo: check for other editors?
				//watches.addWatch(editingProperty, "edited.property.changed");
			}
			currentlyEditing = editingProperty;
			this.currentSelection = currentSelection;

		}
		catch(Server.ConnectionLost e)
		{
			killRemoteEditor();
		}
	}

	private void killRemoteEditor() {
		this.disconnectFromAll();
	}

	String lastCh = "";
	boolean pinned = false;

	protected boolean update() {

		if (selectionHasChanged && !pinned) {

			selectionHasChanged = false;
			lastCh = completionHelp.get();

			Set<Box> selection = this.breadthFirst(both())
				.filter(x -> x.properties.isTrue(Mouse.isSelected, false))
				.filter(x -> !(x instanceof TextEditor))
				.collect(Collectors.toSet());

			if (selection.size() != 1) {
				changeSelection(null, currentlyEditing);
			} else {
				Box target = selection.iterator()
					.next();

				Dict.Prop objectProp;

				if (currentlyEditing != null && target.properties.has(currentlyEditing)) {
					objectProp = currentlyEditing;
				} else objectProp = target.find(defaultEditorProperty, target.upwards())
					.findFirst()
					.map(x -> (Dict.Prop) new Dict.Prop<String>(x).toCanon())
					.orElseGet(() -> (Dict.Prop) Execution.code);


				Log.log("remoteeditor",
					() -> " looking for a defaultEditorProperty on <" + target + "> <" + target.properties.get(defaultEditorProperty) + ">, got <" + objectProp + ">");

				if (!(target == currentSelection && objectProp.equals(currentlyEditing)))
					changeSelection(target, objectProp);
			}
		}

		String ch = completionHelp.get();
		if (!ch.equals(lastCh)) {
			JSONStringer h = new JSONStringer();
			h.object().key("message").value(ch).endObject();
			server.send(socketName, "_messageBus.publish('extra.help', " + h + ")");
			lastCh = ch;
		}


		return true;
	}

	static public Execution getExecution(Box box, Dict.Prop<String> prop) {

		return box.breadthFirst(box.upwards())
			.filter(x -> x.properties.has(Execution.execution))
			.map(x -> x.properties.get(Execution.execution))
			.filter(x -> x != null)
			.filter(x -> x.support(box, prop) != null)
			.findFirst()
			.orElseGet(() -> null);


	}

	/**
	 * A general purpose Send some JavaScript to a text editor call. This is queued to run inside the animation cycle, and thus is thread-safe.
	 */
	public void sendJavaScript(String javascript) {
		Log.log("javascript.trace", () -> ">>>Sending javascript");
		Log.log("javascript.trace", () -> javascript);
		Log.log("javascript.trace", () -> "<<<Sentjavascript");
		server.send(socketName, javascript);
	}

	/**
	 * A general purpose Send some JavaScript to a text editor call, doesn't queue it inside the animation cycle, this must be called from the main thread, or a blocking ThreadSync thread
	 */
	public void sendJavaScriptNow(String javascript) {
		Log.log("javascript.trace", () -> ">>>Sending javascript");
		Log.log("javascript.trace", () -> javascript);
		Log.log("javascript.trace", () -> "<<<Sentjavascript");
		server.broadcast(javascript);
	}

	/**
	 * A general purpose Send some JavaScript to a text editor call
	 */
	public void sendJavaScript(WebSocket socketName, String javascript) {
		Log.log("javascript.trace", () -> ">>>Sending javascript");
		Log.log("javascript.trace", () -> javascript);
		Log.log("javascript.trace", () -> "<<<Sentjavascript");
		server.send(socketName, javascript);
	}

	public Server getServer() {
		return server;
	}

	public Util.ExceptionlessAutoClosable pushToLogStack(Consumer<String> log, Consumer<String> error) {
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

	public interface ExtendedCommand extends Runnable {
		void begin(SupportsPrompt prompt, String alternativeChosen);
	}

	public interface SupportsPrompt {
		void prompt(String prompt, Map<Pair<String, String>, Runnable> options, ExtendedCommand alternative);
	}

	public void pin() {
		pinned = true;
	}

	public void unpin() {
		pinned = false;
	}

	static {
		IO.persist(defaultEditorProperty);
		IO.persist(new Dict.Prop<String>("_" + defaultEditorProperty.getName() + "_cookie"));

	}

}
