package fieldcef.plugins;

import field.app.RunLoop;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Options;
import field.utility.Rect;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.PresentationMode;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fieldcef.browser.Browser;
import fielded.Commands;
import fielded.ServerSupport;
import fieldnashorn.annotations.HiddenInAutocomplete;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;

/**
 * This is a browser, created by default, that embeds the text editor.
 */
public class TextEditor extends Box implements IO.Loaded {

    static public final Dict.Prop<TextEditor> textEditor = new Dict.Prop<>("textEditor").toCanon()
            .type()
            .doc("The TextEditor that is stuck in front of the window, in window coordinates");
    private final Box root;
    @HiddenInAutocomplete
    public Browser browser_;
    @HiddenInAutocomplete
    public String styles;
    String styleSheet = "field-codemirror.css";

    int ezl = (int)(Math.sqrt(Options.dict().getFloat(new Dict.Prop("extraZoomLevel"), 1f)));

    // we'll need to make sure that this is centered on larger screens
    int maxw = 900*ezl;
    int maxh = 1400*ezl;
    int heightLast = -1;
    int tick = 0;
    Commands commandHelper = new Commands();
    long lastTriggerAt = -1;
    int ignoreHide = 0;
    private int maxhOnCreation = 0;
    private String setHeightCode = "";
    private String setWidthCode = "";
    private String initialURL;

    public TextEditor(Box root) {
        this.properties.put(textEditor, this);
        this.root = root;
    }

    @HiddenInAutocomplete
    private static String readFile(String s, boolean append) {
        try (BufferedReader r = new BufferedReader(new FileReader(new File(s)))) {
            String line = "";
            while (r.ready()) {
                line += r.readLine() + "\n";
            }

            if (append) line += "\n//# sourceURL=" + s;
            return line;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    int frameLast = 0;

    @HiddenInAutocomplete
    public void loaded() {
        loaded("http://localhost:" + find(ServerSupport.server, both()).findFirst().get().port + "/init");
    }

    @HiddenInAutocomplete
    public void loaded(String initialURL) {

        this.initialURL = initialURL;
        Log.log("texteditor.debug", () -> "initializing browser");

//		RunLoop.main.delay(() -> {

        FieldBoxWindow window = this.find(Boxes.window, this.both())
                .findFirst()
                .get();

        Drawing drawing = root.first(Drawing.drawing)
                .orElseThrow(() -> new IllegalArgumentException(
                        " can't install text-drawing into something without drawing support"));


        maxh = Math.min(1500, Math.max(600, window.getHeight()-50));

        browser_ = new Browser();

        Vec2 v = drawing.windowSystemToDrawingSystem(new Vec2(window.getWidth() - maxw - 10, 10));
        Vec2 vd = drawing.windowSystemToDrawingSystemDelta(new Vec2(maxw, maxh));

        frameLast = (int) vd.x;
        browser_.properties.put(Box.frame, new Rect(v.x, v.y, vd.x, vd.y));

        maxhOnCreation = maxh;

        browser_.pauseForBoot();

        this.properties.put(FLineDrawing.layer, "glass");
        browser_.properties.put(FLineDrawing.layer, "glass");

        browser_.properties.put(Drawing.windowSpace, new Vec2(1, 0));
        browser_.properties.put(Drawing.windowScale, new Vec2(1, 1));

        browser_.properties.put(Boxes.dontSave, true);
        browser_.properties.put(Box.hidden, true);
        browser_.properties.put(Mouse.isSticky, true);

        browser_.properties.put(Box.undeletable, true);

        browser_.properties.put(FrameManipulation.maxHeight, maxhOnCreation-10);
        browser_.properties.put(FrameManipulation.maxWidth, maxw);

        browser_.connect(root);
        browser_.loaded();

        browser_.properties.put(Box.name, "__texteditor__");

//        executeJavaScript("$(\".CodeMirror\").height(" + (maxh ) + ")");
//        executeJavaScript("$(\".CodeMirror\").width(" + (maxw ) + ")");
        executeJavaScript("$(\".CodeMirror\").width(\"100vw\")");
//        executeJavaScript("$(\".CodeMirror\").height(\"100vh\")");

        this.properties.putToMap(PresentationMode.onEnterPresentationMode, "__hideOnEnter__", () -> hide());

        this.properties.put(Boxes.dontSave, true);
        styles = findAndLoad(styleSheet, false);

        long[] t = {0};
        RunLoop.main.getLoop()
                .attach(x -> {
                    if (t[0] == 0) t[0] = System.currentTimeMillis();
                    if (System.currentTimeMillis() - t[0] > 1000) {
                        boot();

                        // set correct size
                        int h = window.getHeight();

                        Rect r = browser_.properties.get(Box.frame);
                        Callbacks.frameChange(browser_, new Rect(r.x, r.y, r.w, Math.min(maxhOnCreation, r.h)));
                        browser_.properties.get(Box.frame).h = Math.min(maxhOnCreation, r.h);

                        return false;
                    }

                    return true;
                });


        find(Watches.watches, both()).forEach(w -> {

            w.getQueue()
                    .register(x -> x.equals("selection.changed"), c -> {

                        Log.log("shy", () -> "selection is now" + selection().count());

                        if (!pinned) {
                            browser_.executeJavaScript("_messageBus.publish('defocus', {})");
                            browser_.setFocus(false);
                        }


                        if (selection().count() != 1 && !pinned) {
                            browser_.properties.put(Box.hidden, true);
                            Drawing.dirty(this);
                        } else {

                            Optional<PresentationMode> o = find(PresentationMode._presentationMode, both()).findFirst();
                            if (o.isPresent() && o.get().isPresent()) return;

                            executeJavaScript(setHeightCode);
                            executeJavaScript(setWidthCode);

                            browser_.properties.put(Box.hidden, false);
                            Drawing.dirty(this);
                        }

                    });

        });

        first(Boxes.window, both()).ifPresent(x -> x.addKeyboardHandler(event -> {
            Set<Integer> kpressed = Window.KeyboardState.keysPressed(event.before, event.after);
            if ((kpressed.contains(GLFW_KEY_LEFT_SHIFT) || kpressed.contains(
                    GLFW_KEY_RIGHT_SHIFT)) && event.after.keysDown.size() == 1) {
                trigger();
            } else if (event.after.keysDown.size() == 1) {
                lastTriggerAt = 0;
            }

            return true;
        }));

        RunLoop.main.getLoop()
                .attach(x -> {

//                    return true;

                    int maxh = window.getHeight();// - 25 - 10 - 10 - 2;
                    Rect f = browser_.properties.get(Box.frame);
//
                    f = f.duplicate();
//
                    Vec2 d1 = drawing.drawingSystemToWindowSystem(new Vec2(f.x, f.y));
                    Vec2 d2 = drawing.drawingSystemToWindowSystem(new Vec2(f.x + f.w, f.y + f.h));

                    f.x = (float) d1.x;
                    f.y = (float) d1.y;
                    f.w = (float) (d2.x - d1.x);
                    f.h = (float) (d2.y - d1.y);
//
//
//                    if ((int) f.h != heightLast) {
//                        heightLast = (int) f.h;
//                        f = f.duplicate();
//
//                        System.out.println(" height now :" + Math.min(f.h - 100, maxhOnCreation - 40));
//
////							setHeightCode = "$(\"body\").height(" + Math.min(f.h*0.84-10, maxhOnCreation - 40) + ");cm.refresh();";
////							setHeightCode += "$(\".CodeMirror\").height(" + Math.min(f.h*0.84-40, maxhOnCreation - 40) + ");cm.refresh();";
//                        setHeightCode = "$(\"body\").height(" + Math.min(f.h -50,
//                                                                         maxhOnCreation ) + ");cm.refresh();";
//                        setHeightCode += "$(\".CodeMirror\").height(\"100%\");cm.refresh();";
//                        executeJavaScript(setHeightCode);
//                    }
//
//
                    if ((int) f.w != frameLast || (int)f.h !=heightLast) {
                        frameLast = (int) f.w;
                        heightLast = (int) f.h;
                        f = f.duplicate();

//                        setWidthCode = "$(\".totalContainer\").width(" + Math.min(maxw , (int) (f.w -30 )) + ");cm.refresh();";
//                        setWidthCode += "$(\".totalContainer\").height(" + Math.min(maxh , (int) (f.h -20 )) + ");cm.refresh();";
                        setWidthCode += "$(\".CodeMirror\").width(" + Math.min(maxw , (int) (f.w -30 )) + ");cm.refresh();";
                        setWidthCode += "$(\".CodeMirror\").height(" + Math.min(maxh , (int) (f.h -30 )) + ");cm.refresh();";
//                        setWidthCode += "$(\".CodeMirror\").width(\"100%\");cm.refresh();";
//                        setWidthCode += "$(\".CodeMirror\").height(\"100%\");cm.refresh();";
                        executeJavaScript(setWidthCode);
                    }


                    return true;
                });
//		}, 100);
    }

    void updateSize() {
        Rect f = browser_.properties.get(Box.frame);
//        setHeightCode = "$(\"body\").height(" + Math.min(f.h - 20, maxhOnCreation - 40) + ");cm.refresh();";
        setHeightCode += "$(\".CodeMirror\").height(" + Math.min(f.h - 20, maxhOnCreation - 40) + ");cm.refresh();";
        executeJavaScript(setHeightCode);
    }


    @HiddenInAutocomplete
    public void trigger() {
        long now = System.currentTimeMillis();
        if (now - lastTriggerAt < 500) {
            if (!browser_.getFocus()) browser_.executeJavaScript_queued("_messageBus.publish('focus', {})");
            browser_.setFocus(!browser_.getFocus());
        }
        lastTriggerAt = now;
    }

    @HiddenInAutocomplete
    public void boot() {
        browser_.properties.put(browser_.url, initialURL);
        Drawing.dirty(this);
        browser_.finishBooting();


    }

    @HiddenInAutocomplete
    public void show() {
//		System.out.println(" showing because show() called ");

        Optional<PresentationMode> o = find(PresentationMode._presentationMode, both()).findFirst();
        if (o.isPresent() && o.get().isPresent()) return;

        browser_.properties.put(Box.hidden, false);
        browser_.setFocus(true);
        Drawing.dirty(browser_);
    }

    @HiddenInAutocomplete
    public void hide() {
        tick = 0;
        RunLoop.main.getLoop()
                .attach(x -> {
                    if (tick == 5) {
                        browser_.properties.put(Box.hidden, true);
                        Drawing.dirty(this);
                    }
                    tick++;
                    return tick != 5;
                });
        browser_.setFocus(false);
        Drawing.dirty(browser_);
    }

    @HiddenInAutocomplete
    public void runCommands() {
        browser_.executeJavaScript("goCommands()");
        show();
    }

    @HiddenInAutocomplete
    public void center() {
        FieldBoxWindow window = this.find(Boxes.window, both())
                .findFirst()
                .get();
        Rect f = browser_.properties.get(Box.frame);
        f.x = (int) ((window.getWidth() - f.w) / 2);
        f.y = (int) ((window.getHeight() - f.h) / 2);
        if (!browser_.properties.isTrue(Box.hidden, false)) Drawing.dirty(this);
    }

    @HiddenInAutocomplete
    private String findAndLoad(String f, boolean append) {

        String[] roots = {Main.app + "/modules/fieldcore/resources/", Main.app + "/modules/fieldcef_macosx/resources/", Main.app + "/lib/web/"};
        for (String s : roots) {
            if (new File(s + "/" + f).exists()) return readFile(s + "/" + f, append);
        }
        Log.log("glassbrowser_.error", () -> this.getClass() + " Couldnt' find file in playlist :" + f);
        return null;
    }

    @HiddenInAutocomplete
    private Stream<Box> selection() {
        return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false))
                .filter(x -> x != browser_)
                .filter(x -> x.properties.has(Box.name))
                .filter(x -> !x.properties.get(Box.name).equals("__texteditor__"))
                .filter(x -> x != this);
    }


    /**
     * Injects css into the text editor. For example '_.textEditor.injectCSS("body {font-size:20px;}"' will give you a markedly bigger font.
     */
    public void injectCSS(String css) {
        browser_.injectCSS(css);
    }

    /**
     * Executes some javascript directly in the text editor. For larger amounts of TextEditor coding, mark a box as "Bridge to Editor" with the command menu.
     */
    public void executeJavaScript(String js) {
        browser_.executeJavaScript_queued(js);
    }

    /**
     * reloads this text editor. Useful if you are hacking on the CSS or JavaScript that backs the editor
     */
    public void reload() {
        browser_.reload();
    }


    public void setURL(String url) {
        browser_.properties.put(browser_.url, url);
    }

    boolean pinned = false;

    public void pin() {
        pinned = true;
    }

    public void unpin() {
        pinned = false;
    }

    public boolean isPinned() {
        return pinned;
    }
}

