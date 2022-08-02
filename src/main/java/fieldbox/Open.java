package fieldbox;

import field.app.RunLoop;
import field.app.ThreadSync;
import field.app.ThreadSync2;
import field.graphics.*;
import field.graphics.util.onsheetui.Label;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Options;
import field.utility.Rect;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.*;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fieldbox.ui.Compositor;
import fieldbox.ui.FieldBoxWindow;
import fieldbox.ui.ScreenGeometry;
import fieldcef.plugins.*;
import fielded.ServerSupport;
import fielded.boxbrowser.BoxBrowser;
import fielded.boxbrowser.WebApps;
import fielded.plugins.Launch;
import fielded.plugins.MakeNewTextEditor;
import fielded.plugins.Out;
import fieldnashorn.Nashorn;
import jdk.dynalink.linker.GuardingDynamicLinkerExporter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

//import fieldcef.plugins.GlassBrowser;
//import fieldcef.plugins.OutputBox;
//import fieldcef.plugins.TextEditor;
//import fieldcef.plugins.TtapsextEditor_boxBrowser;
//import jdk.dynalink.linker.GuardingDynamicLinkerExporter;
//import jdk.nashorn.api.linker.NashornLinkerExporter;

/**
 * This Opens a document, loading a Window and a standard assortment of plugins into the top of a Box graph and the document into the "bottom" of the Box graph.
 * <p>
 * A Plugin is simply something that's initialized: Constructor(root).connect(root) we can take these from a classname and can optionally initialize them connected to something else
 */
public class Open {

    static public final Dict.Prop<String> fieldFilename = new Dict.Prop<>("fieldFilename").toCanon()
            .type()
            .doc("the name of the field sheet that we are currently in");

    private FieldBoxWindow window;
    private final Boxes boxes;
    private final Drawing drawing;
    private final FLineDrawing frameDrawing;
    private final Mouse mouse;
    private final FrameManipulation frameManipulation;
    private final TextDrawing textDrawing;
    private final FLineInteraction interaction;

    private final MarkingMenus markingMenus;
    private final String filename;
    private final Keyboard keyboard;
    private final Drops drops;
    private PluginList pluginList;
    private Map<String, List<Object>> plugins;
    private Nashorn javascript;
    Box root;

//	private int sizeX = AutoPersist.persist("window_sizeX", () -> 1000, x -> Math.min(1920 * 2, Math.max(100, x)), (x) -> window == null ? x : (int) window.getBounds().w);
//	private int sizeY = AutoPersist.persist("window_sizeY", () -> 800, x -> Math.min(1920 * 2, Math.max(100, x)), (x) -> window == null ? x : (int) window.getBounds().h);
//	private int atX = AutoPersist.persist("window_atX", () -> 0, x -> x, (x) -> window == null ? x : (int) window.getBounds().x);
//	private int atY = AutoPersist.persist("window_atY", () -> 0, x -> x, (x) -> window == null ? x : (int) window.getBounds().y);

    public boolean experimental = Options.dict().isTrue(new Dict.Prop<Boolean>("experimental"), false);

    public Open(String filename) {

        DefaultMenus.safeToSave = false;

        ServiceLoader<GuardingDynamicLinkerExporter> ll = ServiceLoader.load(GuardingDynamicLinkerExporter.class);

        this.filename = filename;
        Log.log("startup", () -> " -- Initializing window -- ");

        try {
            pluginList = new PluginList();
            plugins = pluginList.read(System.getProperty("user.home") + "/.field/plugins.edn", true);

            if (plugins != null) pluginList.interpretClassPathAndOptions(plugins);

            FieldBox.fieldBox.io.setPluginList(pluginList);

        } catch (IOException e) {
            e.printStackTrace();
            pluginList = null;
        }


        Rect bounds = ScreenGeometry.primaryMonitorBounds();
        int inset = 15;
        window = new FieldBoxWindow((int) bounds.x + inset, (int) bounds.y + inset, (int) bounds.w - inset * 2, (int) bounds.h - inset * 2, filename);

        window.scene.attach(-5, this::defaultGLPreambleBackground);
        window.mainLayer()
                .attach(-5, this::defaultGLPreamble);

        boxes = new Boxes();
        root = boxes.root();
        root.properties.put(Boxes.window, window);

        window.getCompositor()
                .newLayer("glass");
        window.getCompositor()
                .getLayer("glass")
                .getScene()
                .attach(-5, this::defaultGLPreambleTransparent);

        window.getCompositor()
                .newLayer("glass2");
        window.getCompositor()
                .getLayer("glass2")
                .getScene()
                .attach(-5, this::defaultGLPreambleTransparent);

        Watches watches = new Watches();
        watches.connect(root);

        drawing = new Drawing();
        // add the default layer to the box graph
        drawing.install(root);
        // add the glass layer to the box graph
        drawing.install(root, "glass");
        drawing.install(root, "glass2");
        // connect drawing to the box graph
        drawing.connect(root);

        textDrawing = new TextDrawing();
        // add the text default layer to the box graph
        textDrawing.install(root);
        // add the text glass layer to the box graph
        textDrawing.install(root, "glass");
        textDrawing.install(root, "glass2");
        // connect text drawing to the box graph
        textDrawing.connect(root);

        frameDrawing = (FLineDrawing) new FLineDrawing(root).connect(root);

        mouse = new Mouse();
        window.addMouseHandler(state -> {
            mouse.dispatch(root, state);
            return true;
        });

        keyboard = new Keyboard();
        window.addKeyboardHandler(state -> {
            keyboard.dispatch(root, state);
            return true;
        });

        drops = new Drops();
        window.addDropHandler(state -> {
            drops.dispatch(root, state);
            return true;
        });


        root.properties.put(fieldFilename, filename);

        new DefaultMenus(root, filename).connect(root);

        // MarkingMenus must come before FrameManipulation, so FrameManipulation can handle selection state modification before MarkingMenus run
        markingMenus = (MarkingMenus) new MarkingMenus(root).connect(root);

        frameManipulation = (FrameManipulation) new FrameManipulation(root).connect(root);

        // Interaction must come before frameManipulation, otherwise all those drags with FLines become marquees on the canvas
        interaction = (FLineInteraction) new FLineInteraction(root).connect(root);

        // here are some examples of plugins
        new Delete(root).connect(root);

        new Topology(root).connect(root);

        new Dispatch(root).connect(root);

        new Chorder(root).connect(root);

        new Meshes(root).connect(root);

        new IsExecuting(root).connect(root);

        new Rename(root).connect(root);

        new DoubleClickToRename(root).connect(root);

        new Scrolling(root).connect(root);

        new GraphicsSupport(root).connect(root);
        new GraphicsSupport_shadertoy(root).connect(root);



        new ComputeShaderSupport(root).connect(root);

        new BlankCanvas(root).connect(root);

        new DragFilesToCanvas(root).connect(root);

//		new Reload(root).connect(root);

        new PluginsPlugin(root).connect(root);

//		new FrameConstraints(root).connect(root);

        new Alignment(root).connect(root);

        new BoxPair(root).connect(root);

        new StatusBar(root).connect(root);

//		new HotkeyMenus(root, null).connect(root);

        new Threading().connect(root);

//		new Typing(root).connect(root);

        if (experimental)
            new MakeNewTextEditor(root).connect(root);

        new RunCommand(root).connect(root);

        new Auto(root).connect(root);

        new FrameChangedHash(root).connect(root);

        new InsertPath().connect(root);

        new Directionality(root).connect(root);

        new Handles(root).connect(root);

        new Create(root).connect(root);

        new DragToCopy(root).connect(root);

        new Pseudo(root).connect(root);

        new Pages(root).connect(root);

        new Taps(root).connect(root);

        new Image2(root).connect(root);

        new Export(root).connect(root);

        new Templates(root).connect(root);

        new Notifications(root).connect(root);

        new KeyboardFocus(root).connect(root);

        new RevealInFinder(root).connect(root);

        new Channels(root).connect(root);

        new MissingStream(root).connect(root);

        new KeyboardShortcuts(root).connect(root);

        new PresentationMode(root).connect(root);

        new Increment(root).connect(root);

        new Out(root).connect(root);

        new Group(root).connect(root);

        new WebApps(root).connect(root);

        new Exec(root).connect(root);

        new AsEditable(root).connect(root);

        new TimeHelper(root).connect(root);

        new Label(root).connect(root);

        new Pads(root).connect(root);

        new Interventions(root).connect(root);

        new Welcome(root).connect(root);

        new Bundle(root).connect(root);

        new Launch(root).connect(root);

        new SimpleTweaks(root).connect(root);


        if (ThreadSync.enabled) new ThreadSyncFeedback(root).connect(root);
        if (ThreadSync2.getEnabled()) new ThreadSync2Feedback(root).connect(root);

        FileBrowser fb = new FileBrowser(root);
        fb.connect(root);

        /* cascade two blurs, a vertical and a horizontal together from the glass layer onto the base layer */
        Compositor.Layer lx = window.getCompositor()
                .newLayer("__main__blurx", 0, 8);
        Compositor.Layer ly = window.getCompositor()
                .newLayer("__main__blury", 1, 8);

        Compositor.Layer composited = window.getCompositor()
                .newLayer("__main__composited", 0);

        composited.getScene()
                .attach(-10, this::defaultGLPreamble);

        window.getCompositor()
                .getMainLayer()
                .blurYInto(0, lx.getScene());
        lx.blurXInto(0, ly.getScene());

        lx.addDependancy(window.getCompositor()
                .getMainLayer());
        ly.addDependancy(lx);

        window.getCompositor()
                .getMainLayer()
                .drawInto(window.scene);

        window.getCompositor()
                .getLayer("glass")
                .compositeWith(ly, composited.getScene());

        composited.addDependancy(window.getCompositor()
                .getLayer("glass"));
        composited.addDependancy(ly);

        window.getCompositor()
                .getLayer("glass")
                .compositeWith(ly, window.scene);

        lx = window.getCompositor()
                .newLayer("__main__gblurx", 0, 8);
        ly = window.getCompositor()
                .newLayer("__main__gblury", 1, 8);


        composited.blurYInto(0, lx.getScene());
        lx.blurXInto(0, ly.getScene());

        lx.addDependancy(composited);
        ly.addDependancy(lx);

        window.getCompositor()
                .getLayer("glass2")
                .compositeWith(ly, window.scene);


		/* reports on how much data we're sending to OpenGL and how much the MeshBuilder caching system is getting us. This is useful for noticing when we're repainting excessively or our
		cache is suddenly blown completely */
        RunLoop.main.getLoop()
                .attach(10, Scene.strobe((i) -> {
                    if (MeshBuilder.cacheHits + MeshBuilder.cacheMisses_internalHash + MeshBuilder.cacheMisses_cursor + MeshBuilder.cacheMisses_externalHash > 0) {
                        Log.println("graphics.stats",
                                " meshbuilder cache h" + MeshBuilder.cacheHits + " | mc" + MeshBuilder.cacheMisses_cursor + " / meh" + MeshBuilder.cacheMisses_externalHash + " / mih"
                                        + MeshBuilder.cacheMisses_internalHash + " / mto" + MeshBuilder.cacheMisses_tooOld + " | tex" + Texture.bytesUploaded);
                        MeshBuilder.cacheHits = 0;
                        MeshBuilder.cacheMisses_cursor = 0;
                        MeshBuilder.cacheMisses_externalHash = 0;
                        MeshBuilder.cacheMisses_internalHash = 0;
                        MeshBuilder.cacheMisses_tooOld = 0;
                        Texture.bytesUploaded = 0;
                    }
                    if (SimpleArrayBuffer.uploadBytes > 0) {
                        Log.println("graphics.stats", " uploaded " + SimpleArrayBuffer.uploadBytes + " bytes to OpenGL");
                        SimpleArrayBuffer.uploadBytes = 0;
                    }
                }, 600));

        //initializes window mgmt for linux
//		if (Main.os == Main.OS.linux) new LinuxWindowTricks(root);
        //initializes window mgmt for osx
//		if (Main.os == Main.OS.mac) new OSXWindowTricks(root);


        // add Javascript runtime as base execution layer
        javascript = new Nashorn();
        Execution execution = new Execution((box, prop) -> (prop.equals(Execution.code) ? javascript.apply(box, prop) : null));
        execution.connect(root);

        new ServerSupport(root);//.openEditor();

        // add a red line time slider to the sheet (this isn't saved with the document, so we'll add it each time)
        TimeSlider ts = new TimeSlider();
        root
                .connect(ts);

        root.properties.put(TimeSlider.time, ts);

        new TextEditor(root).connect(root);

        RunLoop.main.once(() -> {

            new BoxBrowser(root).connect(root);
            new TextEditor_boxBrowser2(root).connect(root);
            new GlassBrowser(root).connect(root);

            // actually open the document that's stored on disk
            doOpen(root, filename);

            Log.log("startup", () -> " -- FieldBox finished initializing, loading plugins ... -- ");

            // initialize the plugins

            if (pluginList != null) pluginList.interpretPlugins(plugins, root);

            Log.log("startup", () -> " -- FieldBox plugins finished, entering animation loop -- ");


//			if (false)
            {
                System.err.println(" booting up text editor ");

//				new OutputBox(root).connect(root);
//				new DocumentationBrowser(root).connect(root);
//				new NotificationBox(root).connect(root);

//			if (Main.os != Main.OS.windows)
                {
                }
            }

            // call loaded on everything above root
            Log.log("startup", () -> "calling .loaded on plugins");
            root
                    .breadthFirst(root
                            .upwards())
                    .filter(x -> x instanceof IO.Loaded)
                    .forEach(x -> ((IO.Loaded) x).loaded());


            new Startup(root);

            // start the runloop
            boxes.start();

            DefaultMenus.safeToSave = true;

        });


        if (Main.os == Main.OS.mac) {
            RunLoop.main.once(() -> {
                Rect b = window.getBounds();
                window.setBounds((int) b.x, (int) b.y, (int) b.w, (int) b.h + 1);
            });
        }

    }

    static public Set<Box> doOpen(Box root, String filename) {
        Map<String, Box> special = new LinkedHashMap<>();
        special.put(">>root<<", root);

        Set<Box> created = new LinkedHashSet<Box>();

//		if (filename.endsWith(".field2"))
        {
            IO.Document doc = FieldBox.fieldBox.io.readDocument(new File(filename).isAbsolute() ? filename : (IO.WORKSPACE + "/" + filename), special, created);
            Log.println("io.debug", "created :" + created);
        }
        Drawing.dirty(root);

        return created;

    }

    public boolean defaultGLPreamble(int pass) {


        GraphicsContext.getContext().stateTracker.viewport.set(new int[]{0, 0, window.getFrameBufferWidth(), window.getFrameBufferHeight()});
        GraphicsContext.getContext().stateTracker.scissor.set(new int[]{0, 0, window.getFrameBufferWidth(), window.getFrameBufferHeight()});
        glClearColor((float) window.background.x, (float) window.background.y, (float) window.background.z, 1);
        glClear(GL11.GL_COLOR_BUFFER_BIT);
        glEnable(GL11.GL_BLEND);
        glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL11.GL_DEPTH_TEST);
        if (Main.os != Main.OS.mac) glEnable(GL13.GL_MULTISAMPLE);

        return true;
    }


    public boolean defaultGLPreambleBackground(int pass) {

        GraphicsContext.getContext().stateTracker.viewport.set(new int[]{0, 0, window.getFrameBufferWidth(), window.getFrameBufferHeight()});
        GraphicsContext.getContext().stateTracker.scissor.set(new int[]{0, 0, window.getFrameBufferWidth(), window.getFrameBufferHeight()});
        glEnable(GL11.GL_BLEND);
        glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL11.GL_DEPTH_TEST);
        if (Main.os != Main.OS.mac) glEnable(GL13.GL_MULTISAMPLE);

        return true;
    }

    public boolean defaultGLPreambleTransparent(int pass) {

        GraphicsContext.getContext().stateTracker.viewport.set(new int[]{0, 0, window.getFrameBufferWidth(), window.getFrameBufferHeight()});
        GraphicsContext.getContext().stateTracker.scissor.set(new int[]{0, 0, window.getFrameBufferWidth(), window.getFrameBufferHeight()});
        glClearColor((float) window.background.x, (float) window.background.y, (float) window.background.z, 0);
        glClear(GL11.GL_COLOR_BUFFER_BIT);
        glEnable(GL11.GL_BLEND);
        glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL11.GL_DEPTH_TEST);
        if (Main.os != Main.OS.mac) glEnable(GL13.GL_MULTISAMPLE);

        return true;
    }
}
