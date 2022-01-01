package fieldbox.ui;

import field.graphics.FastJPEG;
import field.graphics.JPEGLoader;
import field.graphics.Window;
import field.utility.Log;
import org.lwjgl.glfw.GLFWImage;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;
import static org.lwjgl.glfw.GLFW.glfwSetCursor;

/**
 * Classes for setting cursors
 */
public class Cursors {

	static JPEGLoader j = FastJPEG.j;
	static public final int cursorSize = 32;

	static public long arrowLeft = 0;
	static public long arrowRight = 0;
	static public long arrowUp = 0;
	static public long arrowDown = 0;

	static protected long currentCursor = 0;

	public static long getCurrentCursor() {
		return currentCursor;
	}

	static public void clear(Window window) {
		if (currentCursor != 0) glfwSetCursor(window.getGLFWWindowReference(), currentCursor = 0);
	}

	static public void arrowLeft(Window window) {

		if (arrowLeft == 0) arrowLeft = loadResource("arrowLeft.jpg");

		Log.log("cursors", () -> "setting to arrow");
		if (currentCursor != arrowLeft) glfwSetCursor(window.getGLFWWindowReference(), currentCursor = arrowLeft);
	}

	protected static long loadResource(String res) {
		URL arrowFile = Cursors.class.getClassLoader()
					     .getResource("images/"+res);

		String file = arrowFile.getFile();
		file= new File(file).getAbsolutePath();
		ByteBuffer dest = ByteBuffer.allocateDirect(3 * cursorSize * cursorSize);
		ByteBuffer destRGBA = ByteBuffer.allocateDirect(4 * cursorSize * cursorSize);
		j.decompress(file, dest, cursorSize, cursorSize);

		for (int i = 0; i < cursorSize * cursorSize; i++) {
			byte r = dest.get();
			byte g = dest.get();
			byte b = dest.get();
			destRGBA.put(r);
			destRGBA.put(g);
			destRGBA.put(b);
			destRGBA.put((byte) (255 - (b & 0xff)));
		}


		dest.rewind();destRGBA.rewind();

		GLFWImage g = GLFWImage.calloc();
		g.height(cursorSize);
		g.width(cursorSize);
		g.pixels(destRGBA);

		return glfwCreateCursor(g, 16, 16);
	}

	static public void arrowRight(Window window) {
		if (arrowRight == 0) arrowRight = loadResource("arrowRight.jpg");

		Log.log("cursors", () -> "setting to arrow");
		if (currentCursor != arrowRight) glfwSetCursor(window.getGLFWWindowReference(), currentCursor = arrowRight);
	}

	static public void arrowDown(Window window) {
		if (arrowDown == 0) arrowDown = loadResource("arrowDown.jpg");

		Log.log("cursors", () -> "setting to arrow");
		if (currentCursor != arrowDown) glfwSetCursor(window.getGLFWWindowReference(), currentCursor = arrowDown);
	}

	static public void arrowUp(Window window) {
		if (arrowUp == 0) arrowUp = loadResource("arrowUp.jpg");

		Log.log("cursors", () -> "setting to arrow");
		if (currentCursor != arrowUp) glfwSetCursor(window.getGLFWWindowReference(), currentCursor = arrowUp);
	}


}
