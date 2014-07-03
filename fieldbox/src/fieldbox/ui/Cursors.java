package fieldbox.ui;

import com.badlogic.jglfw.Glfw;
import field.graphics.FastJPEG;
import field.graphics.Window;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Classes for setting cursors
 */
public class Cursors {

	static FastJPEG j = new FastJPEG();
	static public final int cursorSize = 32;

	static public long arrow = 0;


	static public void clear(Window window)	{
//		Glfw.
// (window.getGLFWWindowReference(), 0);
	}

	static public void arrow(Window window)
	{
		if (true) return;
//		System.out.println(" ARROW ");
		if (arrow==0)
		{
			URL arrowFile = Cursors.class.getClassLoader().getResource("arrow.jpg");
			String file = arrowFile.getFile();
			ByteBuffer dest = ByteBuffer.allocateDirect(4*cursorSize*cursorSize);
			j.decompress(file, dest, cursorSize, cursorSize);

			for(int i=0;i<cursorSize*cursorSize*4;i++)
				dest.put((byte)(Math.random()*255));

			dest.rewind();
			arrow = Glfw.glfwCreateCursor(dest, cursorSize, cursorSize, cursorSize, cursorSize);
		}
		Glfw.glfwSetCursor(window.getGLFWWindowReference(), arrow);
	}
}
