package fieldbox;

import field.graphics.MeshBuilder;
import field.graphics.RunLoop;
import field.graphics.Scene;
import field.graphics.SimpleArrayBuffer;
import field.utility.AutoPersist;
import field.utility.Log;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.*;
import fieldbox.io.IO;
import fieldbox.ui.Compositor;
import fieldbox.ui.FieldBoxWindow;
import fielded.Execution;
import fielded.ServerSupport;
import fielded.plugins.BridgeToTextEditor;
import fielded.windowmanager.LinuxWindowTricks;
import fielded.windowmanager.OSXWindowTricks;
import fieldnashorn.Nashorn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.IOException;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

/**
 * This Opens a document, loading a Window and a standard assortment of plugins into the top of a Box graph and the document into the "bottom" of the
 * Box graph.
 * <p>
 * A Plugin is simply something that's initialized: Constructor(boxes.root()).connect(boxes.root()) we can take these from a classname and can
 * optionally initialize them connected to something else
 */
public class Open {

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

	private int sizeX = AutoPersist.persist("window_sizeX", () -> 1000, x -> Math.min(2560, Math.max(100, x)), (x) -> window==null ? x : (int)window.getBounds().w);
	private int sizeY = AutoPersist.persist("window_sizeY", () -> 800, x -> Math.min(2560, Math.max(100, x)), (x) -> window==null ? x : (int)window.getBounds().h);

	public Open(String filename) {
		this.filename = filename;
		Log.log("startup", " -- Initializing window -- ");

		try {
			pluginList = new PluginList();
			plugins = pluginList.read(System.getProperty("user.home") + "/.field/plugins.edn", true);

			if (plugins!=null)
				pluginList.interpretClassPathAndOptions(plugins);
		} catch (IOException e) {
			e.printStackTrace();
			pluginList = null;
		}

		window = new FieldBoxWindow(50, 50, sizeX, sizeY, filename);

		window.scene().connect(-5, this::defaultGLPreamble);
		window.mainLayer().connect(-5, this::defaultGLPreamble);

		boxes = new Boxes();
		boxes.root().properties.put(Boxes.window, window);

		window.getCompositor().newLayer("glass");
		window.getCompositor().getLayer("glass").getScene().connect(-5, this::defaultGLPreambleTransparent);

		Watches watches = new Watches();
		watches.connect(boxes.root());

		drawing = new Drawing();
		// add the default layer to the box graph
		drawing.install(boxes.root());
		// add the glass layer to the box graph
		drawing.install(boxes.root(), "glass");
		// connect drawing to the box graph
		drawing.connect(boxes.root());

		textDrawing = new TextDrawing();
		// add the text default layer to the box graph
		textDrawing.install(boxes.root());
		// add the text glass layer to the box graph
		textDrawing.install(boxes.root(), "glass");
		// connect text drawing to the box graph
		textDrawing.connect(boxes.root());

		frameDrawing = (FLineDrawing) new FLineDrawing().connect(boxes.root());

		mouse = new Mouse();
		window.addMouseHandler(state -> {
			mouse.dispatch(boxes.root(), state);
			return true;
		});

		keyboard = new Keyboard();
		window.addKeyboardHandler(state -> {
			keyboard.dispatch(boxes.root(), state);
			return true;
		});

		drops = new Drops();
		window.addDropHandler(state -> {
			drops.dispatch(boxes.root(), state);
			return true;
		});


		// MarkingMenus must come before FrameManipulation, so FrameManipulation can handle selection state modification before MarkingMenus run
		markingMenus = (MarkingMenus) new MarkingMenus(boxes.root()).connect(boxes.root());

		frameManipulation = (FrameManipulation) new FrameManipulation(boxes.root()).connect(boxes.root());

		// Interaction must come before frameManipulation, otherwise all those drags with FLines become marquees on the canvas
		interaction = (FLineInteraction) new FLineInteraction(boxes.root()).connect(boxes.root());

		// here are some examples of plugins
		new Delete(boxes.root()).connect(boxes.root());

		new Topology(boxes.root()).connect(boxes.root());

		new Dispatch(boxes.root()).connect(boxes.root());

		new Chorder(boxes.root()).connect(boxes.root());

		new DefaultMenus(boxes.root(), filename).connect(boxes.root());

		new IsExecuting(boxes.root()).connect(boxes.root());

		new Rename(boxes.root()).connect(boxes.root());

		new Scrolling(boxes.root()).connect(boxes.root());

		new GraphicsSupport(boxes.root()).connect(boxes.root());

		new BlankCanvas(boxes.root()).connect(boxes.root());

		new DragFilesToCanvas(boxes.root()).connect(boxes.root());

		new Reload(boxes.root()).connect(boxes.root());

		new PluginsPlugin(boxes.root()).connect(boxes.root());

		/* cascade two blurs, a vertical and a horizontal together from the glass layer onto the base layer */
		Compositor.Layer lx = window.getCompositor().newLayer("__main__blurx");
		Compositor.Layer ly = window.getCompositor().newLayer("__main__blury", 1);
		window.getCompositor().getMainLayer().blurYInto(5, lx.getScene());
		lx.blurXInto(5, ly.getScene());
		window.getCompositor().getMainLayer().drawInto(window.scene());
		window.getCompositor().getLayer("glass").compositeWith(ly, window.scene());

		/* reports on how much data we're sending to OpenGL and how much the MeshBuilder caching system is getting us. This is useful for noticing when we're repainting excessively or our cache is suddenly blown completely */
		RunLoop.main.getLoop().connect(10, Scene.strobe((i) -> {
			if (MeshBuilder.cacheHits + MeshBuilder.cacheMisses_internalHash + MeshBuilder.cacheMisses_cursor + MeshBuilder.cacheMisses_externalHash > 0) {
				Log.println("graphics.stats", " meshbuilder cache " + MeshBuilder.cacheHits + " | " + MeshBuilder.cacheMisses_cursor + " / " + MeshBuilder.cacheMisses_externalHash + " / " + MeshBuilder.cacheMisses_internalHash);
				MeshBuilder.cacheHits = 0;
				MeshBuilder.cacheMisses_cursor = 0;
				MeshBuilder.cacheMisses_externalHash = 0;
				MeshBuilder.cacheMisses_internalHash = 0;
			}
			if (SimpleArrayBuffer.uploadBytes > 0) {
				Log.println("graphics.stats", " uploaded " + SimpleArrayBuffer.uploadBytes + " bytes to OpenGL");
				SimpleArrayBuffer.uploadBytes = 0;
			}
		}, 600));

		//initializes window mgmt for linux
		if (Main.os == Main.OS.linux) new LinuxWindowTricks(boxes.root());
		//initializes window mgmt for osx
		if (Main.os == Main.OS.mac) new OSXWindowTricks(boxes.root());


		// add Javascript runtime as base execution layer
		javascript = new Nashorn();
		Execution execution = new Execution((box, prop) -> (prop.equals(Execution.code) ? javascript.apply(box, prop) : null));
		execution.connect(boxes.root());

		try {
			new ServerSupport(boxes).openEditor();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// add a red line time slider to the sheet (this isn't saved with the document, so we'll add it each time
		boxes.root().connect(new TimeSlider());

		// actually open the document that's stored on disk
		doOpen();

		Log.log("startup", " -- FieldBox finished initializing, loading plugins ... -- ");

		// initialize the plugins

		if (pluginList!=null)
			pluginList.interpretPlugins(plugins, boxes.root());

		Log.log("startup", " -- FieldBox plugins finished, entering animation loop -- ");

		// start the runloop
		boxes.start();

	}

	protected void doOpen() {
		Map<String, Box> special = new LinkedHashMap<>();
		special.put(">>root<<", boxes.root());

		Set<Box> created = new LinkedHashSet<Box>();
		IO.Document doc = FieldBox.fieldBox.io.readDocument(FieldBox.fieldBox.io.WORKSPACE + "/" + filename, special, created);
		Log.println("io.debug", "created :" + created);

		Drawing.dirty(boxes.root());

	}

	public boolean defaultGLPreamble(int pass) {
		glViewport(0, 0, window.getWidth(), window.getHeight());
		glClearColor(0x2d / 255f, 0x31 / 255f, 0x33 / 255f, 1);
		glClear(GL11.GL_COLOR_BUFFER_BIT);
		glEnable(GL11.GL_BLEND);
		glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL11.GL_DEPTH_TEST);
		glEnable(GL13.GL_MULTISAMPLE);
		return true;
	}

	public boolean defaultGLPreambleTransparent(int pass) {
		glViewport(0, 0, window.getWidth(), window.getHeight());
		glClearColor(2 * 0x2d / 255f, 2 * 0x31 / 255f, 2 * 0x33 / 255f, 0);
		glClear(GL11.GL_COLOR_BUFFER_BIT);
		glEnable(GL11.GL_BLEND);
		glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL11.GL_DEPTH_TEST);
		glEnable(GL13.GL_MULTISAMPLE);
		return true;
	}
}
