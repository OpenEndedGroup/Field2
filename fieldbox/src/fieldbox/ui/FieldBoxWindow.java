package fieldbox.ui;

import com.badlogic.jglfw.GlfwCallback;
import field.graphics.GraphicsContext;
import field.app.RunLoop;
import field.graphics.Scene;
import field.graphics.Window;
import field.utility.Log;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

import static com.badlogic.jglfw.Glfw.glfwCreateCursor;
import static com.badlogic.jglfw.Glfw.glfwShowWindow;

/**
 * Created by marc on 4/14/14.
 */
public class FieldBoxWindow extends Window {

	private Compositor compositor;

	public FieldBoxWindow(int x, int y, int w, int h, String filename) {
		super(x, y, w, h, "Field - " + filename);

		compositor = new Compositor(this);
	}

	int dirty = 1;
	boolean wasDirty = false;

	@Override
	public void loop() {
		wasDirty = dirty > 0;
		dirty = dirty - 1;
		if (dirty < 0) dirty = 0;
		super.loop();
	}

	int t = 0;

	protected void updateScene() {
		GraphicsContext.enterContext(graphicsContext);
		try {
			Log.log("graphics.trace", () -> "scene is ...\n" + scene.debugPrintScene());
			GraphicsContext.getContext().stateTracker.viewport.set(new int[]{0, 0, w * getRetinaScaleFactor(), h * getRetinaScaleFactor()});
			GraphicsContext.getContext().stateTracker.scissor.set(new int[]{0, 0, w * getRetinaScaleFactor(), h * getRetinaScaleFactor()});
			GraphicsContext.getContext().stateTracker.fbo.set(0);
			GraphicsContext.getContext().stateTracker.shader.set(0);
			GraphicsContext.getContext().stateTracker.blendState.set(new int[]{GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA});

			compositor.updateScene();

			scene.updateAll();
		} finally {
			GraphicsContext.exitContext(graphicsContext);
		}
	}

	@Override
	protected boolean needsRepainting() {
		return wasDirty && !disabled;
	}

	public Scene mainLayer() {
		return compositor.getMainLayer().getScene();
	}

	public Compositor getCompositor() {
		return compositor;
	}

	@Override
	protected GlfwCallback makeCallback() {
		GlfwCallback parent = super.makeCallback();
		return new GlfwCallbackDelegate(super.makeCallback()) {
			@Override
			public void windowRefresh(long l) {
				requestRepaint();
			}

			@Override
			public boolean windowClose(long l) {
				RunLoop.main.exit();
				return false;
			}

		};
	}

	public void requestRepaint() {
		dirty = 1;
	}

	public void requestRaise() {
		glfwShowWindow(window);
	}

}
