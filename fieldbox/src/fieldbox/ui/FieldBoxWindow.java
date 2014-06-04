package fieldbox.ui;

import com.badlogic.jglfw.GlfwCallback;
import com.badlogic.jglfw.GlfwCallbackAdapter;
import field.graphics.GraphicsContext;
import field.graphics.Scene;
import field.graphics.Window;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.badlogic.jglfw.Glfw.glfwCreateCursor;
import static com.badlogic.jglfw.Glfw.glfwShowWindow;

/**
 * Created by marc on 4/14/14.
 */
public class FieldBoxWindow extends Window {

	private Compositor compositor;
	private final long cursor;

	public FieldBoxWindow(int x, int y, int w, int h, String filename) {
		super(x, y, w, h, "Field - "+filename);

		compositor = new Compositor(this);

		ByteBuffer noise = ByteBuffer.allocateDirect(64 * 64 * 4);
		for(int i=0;i<64*64;i++)
		{
			noise.put((byte)(Math.random()*255));
			noise.put((byte)(Math.random()*255));
			noise.put((byte)(Math.random()*255));
			noise.put((byte)(Math.random()*255));
		}
		noise.rewind();
		cursor = glfwCreateCursor(noise, 64, 64, 4, 4);
		System.out.println(" cursor is :" + cursor);
	}

	boolean dirty = true;
	boolean wasDirty = false;

	@Override
	public void loop() {
		wasDirty = dirty;
		dirty = false;
		super.loop();
	}

	int t = 0;

	protected void updateScene() {
		GraphicsContext.enterContext(graphicsContext);
		try {
			if (GraphicsContext.trace) System.out.println("scene is ...\n" + mainScene.debugPrintScene());

			compositor.updateScene();

			mainScene.updateAll();
		} finally {
			GraphicsContext.exitContext(graphicsContext);
		}
	}

	@Override
	protected boolean needsRepainting() {
		return wasDirty;
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
		return new GlfwCallbackDelegate(super.makeCallback())
		{
			@Override
			public void windowRefresh(long l) {
				requestRepaint();
			}

			@Override
			public void drop(long l, String[] strings) {
		System.out.println(" DROP! :"+ Arrays.asList(strings));
			}
		};
	}

	public void requestRepaint() {
		dirty = true;
	}

	public void requestRaise()
	{
		glfwShowWindow(window);
	}

}