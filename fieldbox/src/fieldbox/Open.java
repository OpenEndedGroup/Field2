package fieldbox;

import field.graphics.MeshBuilder;
import field.graphics.RunLoop;
import field.graphics.Scene;
import field.graphics.SimpleArrayBuffer;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.boxes.TimeSlider;
import fieldbox.boxes.plugins.Delete;
import fieldbox.boxes.plugins.IsExecuting;
import fieldbox.boxes.plugins.Rename;
import fieldbox.boxes.plugins.Topology;
import fieldbox.io.IO;
import fieldbox.boxes.plugins.Chorder;
import fieldbox.ui.Compositor;
import fieldbox.ui.FieldBoxWindow;
import fielded.Execution;
import fielded.scratch.ServerSupport;
import fielded.windowmanager.LinuxWindowTricks;
import fieldnashorn.Nashorn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.IOException;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * This Opens a document, loading a Window and a standard assortment of plugins into the top of a Box graph and the document into the "bottom" of the Box graph.
 *
 * The significant TODO: here is the open Plugin architecture
 */
public class Open {

	private final FieldBoxWindow window;
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
	private Nashorn javascript;

	public Open(String filename)  {
		this.filename = filename;
		window = new FieldBoxWindow(50, 50, 1500, 1000, filename);

		window.scene().connect(-5, this::defaultGLPreamble);
		window.mainLayer().connect(-5, this::defaultGLPreamble);

		boxes = new Boxes();
		boxes.root().properties.put(Boxes.window, window);

		window.getCompositor().newLayer("glass");
		window.getCompositor().getLayer("glass").getScene().connect(-5, this::defaultGLPreambleTransparent);

		Watches watches = new Watches();
		watches.connect(boxes.root());

		drawing = new Drawing();
		drawing.install(boxes.root());
		drawing.install(boxes.root(), "glass");
		drawing.connect(boxes.root());

		textDrawing = new TextDrawing();
		textDrawing.install(boxes.root());
		textDrawing.install(boxes.root(), "glass");
		textDrawing.connect(boxes.root());

		frameDrawing = new FLineDrawing();
		frameDrawing.connect(boxes.root());

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

		interaction = new FLineInteraction();
		interaction.connect(boxes.root());

		// MarkingMenus must come before FrameManipulation, so FrameManipulation can handle selection state modification before MarkingMenus run
		markingMenus = new MarkingMenus(boxes.root());
		markingMenus.connect(boxes.root());

		frameManipulation = new FrameManipulation();
		frameManipulation.connect(boxes.root());

		new Delete(boxes.root()).connect(boxes.root());

		new Topology(boxes.root()).connect(boxes.root());

		new Chorder(boxes.root()).connect(boxes.root());

		new DefaultMenus(boxes.root(), filename).connect(boxes.root());

		new IsExecuting(boxes.root()).connect(boxes.root());

		new Rename(boxes.root()).connect(boxes.root());

		Compositor.Layer lx = window.getCompositor().newLayer("__main__blurx");
		Compositor.Layer ly = window.getCompositor().newLayer("__main__blury", 1);
		window.getCompositor().getMainLayer().blurYInto(5, lx.getScene());
		lx.blurXInto(5, ly.getScene());
//		ly.drawInto(window.scene());
		window.getCompositor().getMainLayer().drawInto(window.scene());
		window.getCompositor().getLayer("glass").compositeWith(ly, window.scene());


		System.err.println(" -- FieldBox finished initializing -- ");

		RunLoop.main.getLoop().connect(10, Scene.strobe((i) -> {

			if (MeshBuilder.cacheHits + MeshBuilder.cacheMisses_internalHash + MeshBuilder.cacheMisses_cursor + MeshBuilder.cacheMisses_externalHash > 0) {
				System.out.println(" meshbuilder cache " + MeshBuilder.cacheHits + " | " + MeshBuilder.cacheMisses_cursor + " / " + MeshBuilder.cacheMisses_externalHash + " / " + MeshBuilder.cacheMisses_internalHash);
				MeshBuilder.cacheHits = 0;
				MeshBuilder.cacheMisses_cursor = 0;
				MeshBuilder.cacheMisses_externalHash = 0;
				MeshBuilder.cacheMisses_internalHash = 0;
			}
			if (SimpleArrayBuffer.uploadBytes > 0) {
				System.out.println(" uploaded " + SimpleArrayBuffer.uploadBytes + " bytes to OpenGL");
				SimpleArrayBuffer.uploadBytes = 0;
			}
		}, 600));


		new LinuxWindowTricks(boxes.root());

		javascript = new Nashorn();
		Execution execution = new Execution(javascript);
		execution.connect(boxes.root());

		try {
			new ServerSupport(boxes).openEditor();
		} catch (IOException e) {
			e.printStackTrace();
		}

		boxes.root().connect(new TimeSlider());

		doOpen();
		boxes.start();

	}

	protected void doOpen()
	{
		Map<String, Box> special = new LinkedHashMap<>();
		special.put(">>root<<", boxes.root());

		Set<Box> created = new LinkedHashSet<Box>();
		IO.Document doc = FieldBox.fieldBox.io.readDocument(FieldBox.fieldBox.io.WORKSPACE + "/"+filename, special, created);
		System.out.println("created :" + created);

		Drawing.dirty(boxes.root());

	}

	protected void testFile()
	{
		Box b1 = new Box();
		boxes.root().connect(b1);
		b1.properties.put(Box.frame, new Rect(40, 70, 200f, 200f));
		b1.properties.put(Box.name, "Big Box - b1");
		b1.properties.put(IO.id, UUID.randomUUID().toString());
		Drawing.dirty(b1);


		Box b2 = null;
		b2 = new Box();
		boxes.root().connect(b2);
		b2.properties.put(Box.frame, new Rect(300f, 50f, 50f, 75f));
		b2.properties.put(Box.name, "A_one day");
		b2.properties.put(IO.id, UUID.randomUUID().toString());
		Drawing.dirty(b2);
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
		glClearColor(2*0x2d/255f,2*0x31/255f,2*0x33/255f, 0);
		glClear(GL11.GL_COLOR_BUFFER_BIT);
		glEnable(GL11.GL_BLEND);
		glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL11.GL_DEPTH_TEST);
		glEnable(GL13.GL_MULTISAMPLE);
		return true;
	}
}
