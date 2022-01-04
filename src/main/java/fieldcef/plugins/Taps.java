package fieldcef.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldbox.DefaultMenus;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.Chorder;
import fieldbox.boxes.plugins.Templates;
import fieldbox.io.IO;
import fielded.RemoteEditor;
import fielded.webserver.Server;
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//import fieldcef.plugins.TextEditor;

/**
 * A small text height box in JS that refers to a box on the canvas, with a factory system for them.
 */
public class Taps extends Box implements IO.Loaded {

    static public final Dict.Prop<BiFunctionOfBoxAnd<String, Box /*Function<Object, Object>*/>> tap = new Dict.Prop<>(
            "tap").type()
            .toCanon(); //doc!
    static public final Dict.Prop<Binding> tapBinding = new Dict.Prop<>("tapBinding").type()
            .toCanon(); //doc!

    // used by interventions

        static public final Dict.Prop<Function<Double, Double>> evalInterpolation = new Dict.Prop<Function<Double, Double>>("evalInterpolation").toCanon().type();



    boolean selectionHasChanged = false;
    Box selectionWas = null;
    boolean editorLoaded = false;
    HashMap<String, Runnable> queue = new LinkedHashMap<>();
    HashMap<String, Runnable> onActiveSet = new LinkedHashMap<>();

    public Taps(Box root) {

    }

    public void loaded() {
        properties.put(tap, (x, name) -> {

            String kind = name.substring(0, name.lastIndexOf(":"));

            Box c;

            if (kind.equalsIgnoreCase("PAD"))
                c = Pads.padFactory(x, kind, name);
            else
                c = x.first(Templates.ensureChildTemplated)
                        .get()
                        .apply(x, "tap-" + kind, name);

            if (c == null) // no template or child found, just make a box
            {
                c = x.first(DefaultMenus.newBox).get().apply(x);
                c.properties.put(Box.name, name);
            }

            // do something with 'c' -> function

            Binding bindingwWas = c.properties.get(tapBinding);
            if (bindingwWas == null) {
                Binding binding = new Binding(c, x, name);
                Optional<RemoteEditor> first = this.first(RemoteEditor.editor, both());
                if (first.isPresent()) {
                    binding.server = first.get()
                            .getServer();
                    binding.lastSocket = Server.currentWebSocket.get();
                }
                c.properties.put(tapBinding, binding);
                x.first(Chorder.begin).get().apply(c);
            }

            return c;

        });


        this.properties.putToMap(Boxes.insideRunLoop, "main.__activate/deactivate__", this::update);
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

                next.children.forEach( x-> {
                    Binding nextBinding = x.properties.get(tapBinding);
                    if (nextBinding != null) {
                        activate(nextBinding);
                    }
                });

                selectionWas = next;
            }
        }

        List<Runnable> r = new ArrayList<>(queue.values());
        queue.clear();
        r.forEach(x -> x.run());


        if (!editorLoaded && first(Watches.watches, both()).isPresent()) {
            Optional<RemoteEditor> first = this.first(RemoteEditor.editor, both());
            Log.log("tap", () -> "is the editor loaded yet ? " + first);

            if (first.isPresent()) {
                editorLoaded = true;
                RemoteEditor editor = first.get();

                editor.getServer()
                        .addHandlerLast(Predicate.isEqual("taps.activeset"), (s, socket, address, payload) -> {

//                            Log.log("tap", () -> "tap.activeset recieved ");

                            System.out.println(" tap.activeset recieved ");

                            // the list of the active taps inside:
                            Box e = editor.getCurrentlyEditing();
                            if (e == null) {
                                Log.log("tap", () -> "nothing being edited");
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
                                Map<String, Rect> names = new LinkedHashMap<>();
                                for (int i = 0; i < active.length(); i++) {
                                    JSONObject o = (JSONObject) active.get(i);
                                    String name = o.getString("name");
                                    names.put(name, new Rect(o.getDouble("x"), o.getDouble("y"), o.getDouble("w"),
                                                             o.getDouble("h")));
                                }


                                List<Box> toDelete = collect.stream()
                                        .filter(x -> !names.containsKey(x.properties.get(tapBinding).canvasId))
                                        .filter(x -> !x.properties.isTrue(Pads.isPad, false)) // don't auto delete pads
                                        .collect(Collectors.toList());

                                Log.log("tap", () -> "collected :" + collect + " / " + toDelete);

                                for (Box b : toDelete) {
                                    Callbacks.delete(b);
                                    b.disconnectFromAll();
                                }

                                collect.removeAll(toDelete);

                                collect.stream()
                                        .forEach(x -> {
                                            Binding b = x.properties.get(tapBinding);
                                            if (b!=null) {
                                                updatePosition(b, names.get(b.canvasId));
                                                b.lastSocket = Server.currentWebSocket.get();
                                            }
                                        });
                            });

                            onActiveSet.values().forEach(Runnable::run);
                            onActiveSet.clear();
                            return payload;
                        });

                // snoop editor changed to set active / unactive.
                Watches watches = first(Watches.watches, both()).orElseThrow(
                        () -> new IllegalArgumentException(" need Watches for server support"));

                watches.addWatch(Mouse.isSelected, (changed) -> {
                    selectionHasChanged = true;
                });

            }


        }


        return true;
    }


    private void activate(Binding nextBinding) {
        onActiveSet.put(""+System.identityHashCode(nextBinding), () -> {
            nextBinding.target.first(Chorder.begin).get().apply(nextBinding.target);
        });
    }

    private void deactivate(Binding wasBinding) {
        // todo, callback
    }

    private Stream<Box> selection() {
        return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
    }


    private void updatePosition(Binding binding, Rect at) {
        if (binding.editorPosition == null || !binding.editorPosition.equals(at) && binding.target != null)
            Drawing.dirty(binding.target);


        if (at!=null)
            binding.editorPosition = new Rect(at.x, at.y, at.w, at.h);

        // todo, callback
    }

    /**
     * The function call will look like this:
     * <p>
     * _.tap(_, "type:name")
     * <p>
     * this will create on demand a box called 'name' and 'connect' it to this part of the text (and have it parented to this box). We can build a factory for taps of type "type" and a menuSpecs
     * item for inserting this string (hidden by a canvas) directly we'll arrange things so that _.tap("_, "type:name")(x) executes something inside this box. we'll arrange to broadcast the
     * position of all of these widgets on every text change; boxes can use this to change their position, or hide. boxes can send javascript to the browser to alter the drawing of the canvas that
     * is this widget.
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
            if (lastSocket != null) server.send(lastSocket, javascript);
        }


        public Function<Box, FLine> connective() {
            TextEditor t = inside.find(TextEditor.textEditor, inside.both())
                    .findFirst()
                    .get();

            return (x) -> {

                if (editorPosition == null) return new FLine(); // check current editable


                Rect f0 = t.browser_.properties.get(Box.frame)
                        .duplicate();
                Rect f1 = target.properties.get(Box.frame);

                f0.x += editorPosition.x;
                f0.y += editorPosition.y;


                f0 = f0.inset(5);
                f1 = f1.inset(5);


                FLine f = new FLine();

                f.moveTo(f0.x, f0.y);
                f.lineTo(f1.x, f1.y);
                f.moveTo(f0.x + editorPosition.w, f0.y);
                f.lineTo(f1.x + f1.w, f1.y);
                f.moveTo(f0.x + editorPosition.w, f0.y + editorPosition.h);
                f.lineTo(f1.x + f1.w, f1.y + f1.h);
                f.moveTo(f0.x, f0.y + editorPosition.h);
                f.lineTo(f1.x, f1.y + f1.h);

                f.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, 0.1));

                return f;


            };
        }


    }


}
