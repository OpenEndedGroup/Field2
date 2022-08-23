package fieldbox;

import field.app.RunLoop;
import field.app.ThreadSync2;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.DragToCopy;
import fieldbox.boxes.plugins.Pages;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.plugins.Launch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plugin: Adds Standard Menus and Shortcuts too basic not to have (new box, save etc.)
 */
public class DefaultMenus extends Box {

    static public final Dict.Prop<FunctionOfBox<Box>> newBox = new Dict.Prop<FunctionOfBox<Box>>("newBox").toCanon()
            .doc("`_.newBox()` will create a new box that's a child of this one");

    static public final Dict.Prop<BiFunctionOfBoxAnd<Class, Box>> newBoxOfClass = new Dict.Prop<BiFunctionOfBoxAnd<Class, Box>>("newBoxOfClass").toCanon()
            .doc("`_.newBoxOfClass(c)` create a new box that's a child of this one, with a custom class `c`");

    static public final Dict.Prop<BiFunctionOfBoxAnd<String, Box>> ensureChild = new Dict.Prop<FunctionOfBox<Box>>("ensureChild").toCanon()
            .doc("`_.ensureChild('name')` creates a new box that's a child of this one, if there already isn't one with this `name`");

    static public final Dict.Prop<Boolean> wasNew = new Dict.Prop<Boolean>("wasNew").toCanon()
            .doc("`_.wasNew` is set to indicate that the box mentioned by calls like `_.ensureChild()` was actually created, not merely found");

    static public final Dict.Prop<TriFunctionOfBoxAnd<String, Class, Box>> ensureChildOfClass = new Dict.Prop<BiFunctionOfBoxAnd<Class, Box>>("ensureChildOfClass").toCanon()
            .doc("`_.ensureChildOfClass('name', Something.class)` create a new box that's a peer of this one, with a custom class `Something`, if one called `name` doesn't already exist");

    static public final Dict.Prop<BiFunctionOfBoxAnd<Class, Box>> setClass = new Dict.Prop<FunctionOfBox<Box>>("setClass").toCanon()
            .doc("`_ = _.setClass(Something.class)` sets the class 'this' box to be `Something`. This only does anything if `Something` is a valid subclass of `Box`");

    static public final Dict.Prop<FunctionOfBox<Box>> deleteBox = new Dict.Prop<FunctionOfBox<Box>>("deleteBox").toCanon().type()
            .doc("delete this box");

    // this gets set if we successfully opened something
    static public volatile boolean safeToSave = false;

    private final Box root;
    private String filename;
    boolean saveOnExit = true;

    static boolean weAreExiting = false;

    public DefaultMenus(Box root, String filename) {
        this.root = root;
        this.filename = filename;
        properties.put(MarkingMenus.menuSpecs, (event) -> {
            if (isNothingSelected()) {

                MarkingMenus.MenuSpecification spec = new MarkingMenus.MenuSpecification();

                MarkingMenus.MenuSpecification saveMenu = new MarkingMenus.MenuSpecification();
                saveMenu.items.put(MarkingMenus.Position.E, new MarkingMenus.MenuItem("Save", () -> {
                    save();
                }));

                saveMenu.items.put(MarkingMenus.Position.S, new MarkingMenus.MenuItem(saveOnExit ? "Save on exit (toggle)" : "Don't save on exit (toggle)", () -> {
                    saveOnExit = !saveOnExit;
                }));

                spec.items.put(MarkingMenus.Position.E, new MarkingMenus.MenuItem("Save...", () -> {
                    save();
                }).setSubmenu(saveMenu));
                spec.items.put(MarkingMenus.Position.N, new MarkingMenus.MenuItem("New Box", () -> {
                    Vec2 at = convertCoordinateSystem(event.after);
                    newBox(at, root);
                }));

                return spec;
            }
            return null;
        });

        properties.putToMap(Keyboard.onCharTyped, "__hotkeymenus__", (e, k) -> {
            if (e.properties.isTrue(Window.consumed, false)) return;

            if (k == 'n')
                newBox(convertCoordinateSystem(e.after.mouseState), root);
        });

        properties.put(setClass, (box, clazz) -> {

            if (!Box.class.isAssignableFrom(clazz))
                throw new ClassCastException(" class '" + clazz + "' isn't a Box subclass");

            if (box.getClass().equals(clazz)) // nothing to do
                return box;

            Set<Box> c = new LinkedHashSet<>(box.children());
            Set<Box> p = new LinkedHashSet<>(box.parents());

            try {
                Box newBox = DragToCopy.duplicateBox(box, clazz, Collections.emptyList());

                for (Box cc : c)
                    newBox.connect(cc);
                for (Box pp : p)
                    pp.connect(newBox);

                box.disconnectFromAll();

                return newBox;
            } catch (NullPointerException e) {
                ClassCastException q = new ClassCastException(" class '" + clazz + "' didn't instantiate properly");
                q.initCause(e);
                throw q;
            }
        });

        properties.put(Commands.commands, () -> {

            Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
            m.put(new Pair<>("Save all", "Saves this document"), DefaultMenus.this::save);

            m.put(new Pair<>("Save as...", "Save this document under a new name (this makes all of the boxes here independent from any other files)."),
                    () -> {
                        String altWas = new Launch(root).getSaveFile();
                        try {
                            saveAsNewFile(root, altWas);

                            // change title

                            this.find(Boxes.window, both()).findFirst().ifPresent(window -> {
                                window.setTitle("Field - " + altWas);
                            });

                        } catch (IOException e) {

                            // notify
                            e.printStackTrace();
                        }
                    });
            return m;
        });

        properties.put(newBox, (box) ->

                newBox(box.find(Box.frame, box.both())
                        .findFirst()
                        .map(x -> new Vec2(x.x + x.w + 5, x.y + x.h + 5))
                        .orElseGet(() -> new Vec2(0, 0)), new Box[]{box}));

        properties.put(ensureChild, (box, name) ->

        {
            Optional<Box> f = box.children()
                    .stream()
                    .filter(x -> x.properties.equals(Box.name, name))
                    .findFirst();

            f.map(x -> x.properties.remove(wasNew));

            return f.orElseGet(() -> {
                Box bx = newBox(box.find(Box.frame, box.both())
                        .findFirst()
                        .map(x -> new Vec2(x.x + x.w + 5, x.y + x.h + 5))
                        .orElseGet(() -> new Vec2(0, 0)), box);
                bx.properties.put(Box.name, name);
                return bx;
            });
        });


        properties.put(newBoxOfClass, (box, cz) ->
        {
            return newBoxOfClass(cz, box.find(Box.frame, box.both())
                    .findFirst()
                    .map(x -> new Vec2(x.x + x.w + 5, x.y + x.h + 5))
                    .orElseGet(() -> new Vec2(0, 0)), new Box[]{box});
        });


        properties.put(ensureChildOfClass, (box, name, cz) ->
        {
            Optional<Box> f = box.children()
                    .stream()
                    .filter(x -> x.properties.equals(Box.name, name))
                    .filter(cz::isInstance)
                    .findFirst();

            f.map(x -> x.properties.remove(wasNew));

            return f.orElseGet(() -> {
                Box bx = newBoxOfClass(cz, box.find(Box.frame, box.both())
                        .findFirst()
                        .map(x -> new Vec2(x.x + x.w + 5, x.y + x.h + 5))
                        .orElseGet(() -> new Vec2(0, 0)), box);
                bx.properties.put(Box.name, name);
                return bx;
            });
        });

        RunLoop.main.onExit(() -> {
            weAreExiting = true;
            if (saveOnExit && safeToSave) if (this.breadthFirst(both())
                    .filter(x -> x.properties.get(Box.frame) != null)
                    .findFirst()
                    .isPresent()) save();
        });

        properties.put(deleteBox, (box) -> {
            Callbacks.transition(box, Mouse.isSelected, false, false, Callbacks.onSelect, Callbacks.onDeselect);
            Callbacks.call(box, Callbacks.onDelete);
            box.disconnectFromAll();
            return null;
        });

    }

    private Box newBox(Vec2 at, Box... parents) {

        Box b1 = new Box();
        for (Box p : parents)
            p.connect(b1);
        float w = 50;
        b1.properties.put(frame, new Rect(at.x - w, at.y - w, w * 2, w * 2));
        b1.properties.put(Box.name, "Untitled");
        b1.properties.put(wasNew, true);

        root.find(Pages.get_pages(), root.upwards()).findFirst().get().moveToCurrentPage(b1);

        Drawing.dirty(b1);
        return b1;
    }

    static public Box newBoxOfClass(Class cz, Vec2 at, Box... parents) {
        Box b1 = null;
        try {
            b1 = (Box) cz.getConstructor()
                    .newInstance();
        } catch (NoSuchMethodException e) {
            try {
                b1 = (Box) cz.getConstructor(Box.class)
                        .newInstance(parents[0]);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e1) {
                e1.printStackTrace();
            }

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        if (b1 == null) return null;

        for (Box p : parents)
            p.connect(b1);
        float w = 50;
        b1.properties.put(frame, new Rect(at.x - w, at.y - w, w * 2, w * 2));
        b1.properties.put(Box.name, "Untitled");
        b1.properties.put(wasNew, true);
        Drawing.dirty(b1);
        return b1;
    }

    public Vec2 convertCoordinateSystem(Window.MouseState event) {
        Optional<Drawing> drawing = this.find(Drawing.drawing, both())
                .findFirst();
        return drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(event.x, event.y)))
                .orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));
    }

    static public void save(Box root, String filename) {
        Log.println("io.debug", " saving .... ");
        Map<Box, String> special = new LinkedHashMap<>();
        special.put(root, ">>root<<");

        String path = "";
        String fn = filename;
        if (filename.contains("/")) {

            path = filename.substring(0, filename.lastIndexOf("/"));
            fn = filename.substring(filename.lastIndexOf("/") + 1);
        }

        IO.Document doc = FieldBox.fieldBox.io.compileDocument(path, root, special);

        FieldBox.fieldBox.io.filesTouched.clear();

        boolean error = false;
        try {
            String finalPath = path;
            String finalFn = fn;

            if (weAreExiting) {
                FieldBox.fieldBox.io.writeOutDocument(IO.WORKSPACE + "/" + finalPath + "/" + finalFn, doc);
            } else {
                ThreadSync2.callInMainThreadAndWait(() -> {

                    Drawing.notify("Saving...", root, 100);
                    ThreadSync2.yieldIfPossible();
                    FieldBox.fieldBox.io.writeOutDocument(IO.WORKSPACE + "/" + finalPath + "/" + finalFn, doc);
                    ThreadSync2.yieldIfPossible();
                    //				if (!error)
                    {
                        Log.println("io.debug", " going to notify ...");
                        Drawing.notify("Saved to " + filename, root, 200);
                        Log.println("io.debug", " ... notified ");
                    }

                    return null;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Drawing.notify("Error saving " + e.getMessage(), root, 200);
            error = true;
        }


    }

    private void save() {
        save(root, filename);
    }

    public boolean saveAsNewFile(Box root, String absoluteFilename) throws IOException {

        Map<Box, String> special = new LinkedHashMap<>();


        String filename = new File(absoluteFilename).getName();
        filename = filename + (filename.endsWith(".field2") ? "" : ".field2");
        String tmpWorkspace = new File(absoluteFilename).getParentFile().getAbsolutePath();

        String workspaceWas = FieldBox.fieldBox.io.getDefaultDirectory(); // no sure we reset that...

        FieldBox.fieldBox.io.setDefaultDirectory(tmpWorkspace);


        IO.uniqify(root);

        String path = IO.WORKSPACE + "/" + filename + "/";

        special.put(root, ">>root<<");

        List<Runnable> undo = new LinkedList<>();

        IO.Document doc = FieldBox.fieldBox.io.compileDocument("", root, x -> {
            undo.add(IO.uniqify(x));
            return true;
        }, special);

        Map<String, String> remap = new LinkedHashMap<String, String>();

        doc.externalList.forEach(x -> {
            String nid = Box.newID();
            remap.put(x.id, nid);
            x.id = nid;
        });

        doc.externalList.forEach(x -> {
            x.children = x.children.stream().map(y -> remap.getOrDefault(y, y)).filter(y -> y != null).collect(Collectors.toList());
            x.parents = x.parents.stream().map(y -> remap.getOrDefault(y, y)).filter(y -> y != null).collect(Collectors.toList());
        });


        boolean error = false;
        try {
            FieldBox.fieldBox.io.writeOutDocument(IO.WORKSPACE + "/" + filename, doc);
        } catch (IOException e) {
            e.printStackTrace();
            Drawing.notify("Error saving " + e.getMessage(), root, 200);
            error = true;
        }

        for (Runnable r : undo)
            r.run();


        this.filename = filename;

        return true;
    }

    private boolean isNothingSelected() {

        return !find(Mouse.isSelected, both()).filter(x -> x.booleanValue())
                .findFirst()
                .isPresent();
    }

}
