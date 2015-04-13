package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldbox.DefaultMenus;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Mouse;
import fieldbox.boxes.Watches;
import fieldbox.io.IO;
import fielded.RemoteEditor;
import fielded.webserver.Server;
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A small text height box in JS that refers to a box on the canvas, with a factory system for them.
 */
public class Taps extends Box implements IO.Loaded {

	static public final Dict.Prop<BiFunctionOfBoxAnd<String, Box /*Function<Object, Object>*/>> tap = new Dict.Prop<>("tap").type()
																.toCannon(); //doc!
	static public final Dict.Prop<Binding> tapBinding = new Dict.Prop<>("tapBinding").type()
											 .toCannon(); //doc!



	boolean selectionHasChanged = false;
	Box selectionWas = null;
	boolean editorLoaded = false;
	HashMap<String, Runnable> queue = new LinkedHashMap<>();

	public Taps(Box root) {


	}

	public void loaded() {
		properties.put(tap, (x, string) -> {

			Box c = x.first(DefaultMenus.ensureChildOfClass)
				 .get()
				 .apply(x, string, Box.class);// todo, templates?


			// do something with 'c' -> function

			Binding bindingwWas = c.properties.get(tapBinding);
			if (bindingwWas == null) {
				Binding binding = new Binding(c, x, string);
				Optional<RemoteEditor> first = this.first(RemoteEditor.editor, both());
				if (first.isPresent()) {
					binding.server = first.get()
							      .getServer();
					binding.lastSocket = Server.currentWebSocket.get();
				}
				c.properties.put(tapBinding, binding);
			}

			return c;

		});


		this.properties.putToMap(Boxes.insideRunLoop, "main.__activate/deactivate__", (Supplier<Boolean>) this::update);
	}

	protected boolean update() {
		if (selectionHasChanged) {
			selectionHasChanged = false;
			if (selectionWas != null) {
				Binding wasBinding = selectionWas.properties.get(tapBinding);
				if (wasBinding != null) {
					deactivate(wasBinding);
				}
				selectionWas = null;
			}

			if (selection().count() == 1) {
				Box next = selection().findFirst()
						      .get();
				Binding nextBinding = next.properties.get(tapBinding);
				if (nextBinding != null) {
					activate(nextBinding);
				}

				selectionWas = next;
			}
		}

		List<Runnable> r = new ArrayList<>(queue.values());
		queue.clear();
		r.forEach(x -> x.run());


		if (!editorLoaded) {
			Optional<RemoteEditor> first = this.first(RemoteEditor.editor, both());
			Log.log("tap", "is the editor loaded yet ? " + first);

			if (first.isPresent()) {
				editorLoaded = true;
				RemoteEditor editor = first.get();

				editor.getServer()
				      .addHandlerLast(Predicate.isEqual("taps.activeset"), (s, socket, address, payload) -> {

					      Log.log("tap", "tap.activeset recieved ");

					      // the list of the active taps inside:
					      Box e = editor.getCurrentlyEditing();
					      if (e == null) {
						      Log.log("tap", "nothing being edited");
						      return payload;
					      }

					      queue.put("updateTapsFrom", () -> {

						      // clean these out on demand

						      List<Box> collect = e.children()
									   .stream()
									   .filter(x -> x.properties.has(tapBinding))
									   .collect(Collectors.toList());

						      JSONObject p = (JSONObject) payload;
						      JSONArray active = p.getJSONArray("active");
						      Map<String, Vec2> names = new LinkedHashMap<>();
						      for (int i = 0; i < active.length(); i++) {
							      JSONObject o = (JSONObject) active.get(i);
							      String name = o.getString("name");
							      names.put(name, new Vec2(o.getDouble("x"), o.getDouble("y")));
						      }


						      List<Box> toDelete = collect.stream()
										  .filter(x -> !names.containsKey(x.properties.get(tapBinding).canvasId))
										  .collect(Collectors.toList());

						      Log.log("tap", "collected :" + collect + " / " + toDelete);

//					      for (Box b : toDelete) {
//						      Callbacks.delete(b);
//						      b.disconnectFromAll();
//					      }

						      collect.removeAll(toDelete);

						      collect.stream()
							     .forEach(x -> {
								     Binding b = x.properties.get(tapBinding);
								     updatePosition(b, names.get(b.canvasId));
								     b.lastSocket = Server.currentWebSocket.get();
							     });
					      }); return payload;
				      });
// snoop editor changed to set active / unactive.
				Watches watches = first(Watches.watches).orElseThrow(() -> new IllegalArgumentException(" need Watches for server support"));

				watches.addWatch(Mouse.isSelected, (changed) -> {
					selectionHasChanged = true;
				});

			}


		}


		return true;
	}


	private void activate(Binding nextBinding) {
		// todo, callback
	}

	private void deactivate(Binding wasBinding) {
		// todo, callback
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}


	private void updatePosition(Binding binding, Vec2 at) {
		binding.editorPosition = new Rect(at.x, at.y, 5, 5);

		// todo, callback
	}

	/**
	 * The function call will look like this:
	 * <p>
	 * _.tap(_, "type:name")
	 * <p>
	 * this will create on demand a box called 'name' and 'connect' it to this part of the text (and have it parented to this box).
	 * <p>
	 * We can build a factory for taps of type "type" and a menuSpecs item for inserting this string (hidden by a canvas) directly
	 * <p>
	 * we'll arrange things so that _.tap("_, "type:name")(x) executes something inside this box.
	 * <p>
	 * we'll arrange to broadcast the position of all of these widgets on every text change; boxes can use this to change their position, or hide.
	 * <p>
	 * boxes can send javascript to the browser to alter the drawing of the canvas that is this widget.
	 */

	public class Binding {
		public final Box target;
		public final Box inside;
		public String canvasId;
		public WebSocket lastSocket;
		public Server server;
		public boolean active; // are we currently visible in the editor, or are we serialized
		boolean seen;
		Rect editorPosition;

		public Binding(Box target, Box inside, String canvasId) {
			this.target = target;
			this.inside = inside;
			this.canvasId = canvasId;
		}

		public void execute(String javascript) {
			server.send(lastSocket, javascript);
		}

	}


}
