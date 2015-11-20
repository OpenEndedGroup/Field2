package field.graphics.util;

import field.graphics.FastJPEG;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL30.*;

/*
Class to save out a canvas to a directory of .jpgs
 */
public class Saver {

	private final int numWorkers;

	private final int width;

	private final int height;

	private final ExecutorService pool;

	private final String prefix;

	public Saver(int width, int height, int numWorkers, String prefix) {
		this.width = width;
		this.height = height;
		this.numWorkers = numWorkers;
		this.prefix = prefix;

		pool = Executors.newCachedThreadPool();
	}

	List<FutureTask<ByteBuffer>> workers = new ArrayList<FutureTask<ByteBuffer>>();

	int frameNumber = 0;

	boolean on = false;
	boolean drip = false;

	private String lastFilename;

	public void setOn(boolean on) {
		this.on = on;
		drip = false;
	}

	public void start() {
		setOn(true);
	}

	public void stop() {
		setOn(false);
	}

	public void drip() {
		on = true;
		drip = true;
	}

	public void update() {
		if (!on) return;

		ByteBuffer storage = null;

		if (workers.size() < numWorkers) {

			storage = newStorage();
		} else {
			;//System.out.println("state opf workers: ");
			for (FutureTask t : workers)
				;//System.out.println(t.isDone());

			FutureTask<ByteBuffer> w = workers.remove(0);
			try {
				storage = w.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		getImage(storage);

		lastFilename = prefix + pad(frameNumber++) + ".jpg";
		FutureTask<ByteBuffer> task = new FutureTask<ByteBuffer>(makeWorker(storage, lastFilename));
		pool.execute(task);
		workers.add(task);

		if (drip) on = false;
	}

	private String pad(int i) {
		String s = i + "";
		while (s.length() < 5) s = "0" + s;
		return s;
	}

	private void getImage(ByteBuffer storage) {

		int[] a = {0};
		assert glGetError() == 0;

//		glFinish();
		storage.rewind();
		a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
//		glFinish();
//		GL11.glReadBuffer(GL11.GL_BACK);
		glReadPixels(0, 0, width, height, GL11.GL_RGB, GL_UNSIGNED_BYTE, storage);
//		glFinish();
		glBindFramebuffer(GL_FRAMEBUFFER, a[0]);
		storage.rewind();

		assert glGetError() == 0;
	}

	FastJPEG j2 = new FastJPEG();

	private Callable<ByteBuffer> makeWorker(final ByteBuffer storage, final String filename) {
		return new Callable<ByteBuffer>() {
			public ByteBuffer call() throws Exception {

				j2.compress(filename, storage, width, height);

				return storage;
			}
		};
	}

	private ByteBuffer newStorage() {
		return ByteBuffer.allocateDirect(width * height * 4);
	}


}
