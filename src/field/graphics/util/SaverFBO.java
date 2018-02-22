package field.graphics.util;

import field.graphics.FBO;
import field.graphics.FastJPEG;
import field.utility.IdempotencyMap;
import field.utility.Pair;
import fieldnashorn.annotations.HiddenInAutocomplete;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/*
Class to save out a canvas to a directory of .jpgs
 */
public class SaverFBO {

	private final int numWorkers;

	public final int width;
	public final int height;

	private final ExecutorService pool;

	private final String prefix;
	private FBO fbo;

	public SaverFBO(int width, int height, int numWorkers, String prefix, FBO fbo) {
		this.width = width;
		this.height = height;
		this.numWorkers = numWorkers;
		this.prefix = prefix;
		this.fbo = fbo;

		pool = Executors.newCachedThreadPool();
	}

	List<FutureTask<Pair<ByteBuffer, ByteBuffer>>> workers = new ArrayList<>();

	public int frameNumber = 0;

	@HiddenInAutocomplete
	boolean on = false;
	@HiddenInAutocomplete
	boolean drip = false;

	private String lastFilename;


	@HiddenInAutocomplete
	public void setOn(boolean on) {
		this.on = on;
		drip = false;
	}

	/**
	 * call to start streaming jpgs out to disk
	 */
	public String start() {
		setOn(true);
		return "Saving to '" + prefix + "'";
	}

	/**
	 * call to stop streaming jpgs out to disk
	 */
	public void stop() {
		setOn(false);
	}

	/**
	 * Opens the directory that this is saving to in the Finder (Mac Only)
	 */
	public void open()
	{
		Desktop.getDesktop().browseFileDirectory(new File(prefix).getParentFile());
	}

	/**
	 * call to save exactly one jpg to disk and then stop
	 */

	public void drip() {
		on = true;
		drip = true;
	}

	@HiddenInAutocomplete
	public IdempotencyMap<Consumer<ByteBuffer>> hooks = new IdempotencyMap<Consumer<ByteBuffer>>(Consumer.class);


	@HiddenInAutocomplete
	public void update() {
		if (update(prefix, frameNumber, ".jpg")) frameNumber++;
	}

	@HiddenInAutocomplete
	public boolean update(String prefix, int frameNumber, String suffix) {
		if (!on) {
			runHooks(null);
			return false;
		}

		Pair<ByteBuffer, ByteBuffer> storage = null;

		if (workers.size() < numWorkers) {
			storage = newStorage();
		} else {
			//System.out.println("state opf workers: ");
//			for (FutureTask t : workers)
//				System.out.println(t.isDone());

			FutureTask<Pair<ByteBuffer, ByteBuffer>> w = workers.remove(0);
			try {
				storage = w.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		getImage(storage);

		runHooks(storage);

		lastFilename = prefix + pad(frameNumber) + suffix;
		FutureTask<Pair<ByteBuffer ,ByteBuffer >> task = new FutureTask<>(makeWorker(storage, lastFilename));
		pool.execute(task);
		workers.add(task);

		if (drip) on = false;

		return true;
	}

	private void runHooks(Pair<ByteBuffer ,ByteBuffer > storage) {
		hooks.values().stream().forEach(x -> x.accept(storage.first));
	}

	@HiddenInAutocomplete
	static public String pad(int i) {
		String s = i + "";
		while (s.length() < 6) s = "0" + s;
		return s;
	}

	@HiddenInAutocomplete
	private void getImage(Pair<ByteBuffer, ByteBuffer> storage) {

		int[] a = {0};
		assert glGetError() == 0;

		storage.first.rewind();
		a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
		glBindFramebuffer(GL_FRAMEBUFFER, fbo.getOpenGLFrameBufferNameInCurrentContext());
		glReadPixels(0, 0, width, height, GL11.GL_RGB, GL_UNSIGNED_BYTE, storage.first);
		glBindFramebuffer(GL_FRAMEBUFFER, a[0]);
		storage.first.rewind();

////		glFinish();
//		storage.first.rewind();
//		a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
//		glBindFramebuffer(GL_FRAMEBUFFER, 0);
////		glFinish();
////		GL11.glReadBuffer(GL11.GL_BACK);
//		glReadPixels(0, 0, width, height, GL11.GL_RGB, GL_UNSIGNED_BYTE, storage.first);
////		glFinish();
//		glBindFramebuffer(GL_FRAMEBUFFER, a[0]);
//		storage.first.rewind();

		assert glGetError() == 0;
	}

	FastJPEG j2 = new FastJPEG();

	private Callable<Pair<ByteBuffer, ByteBuffer>> makeWorker(final Pair<ByteBuffer, ByteBuffer> storage, final String filename) {
		return new Callable<Pair<ByteBuffer, ByteBuffer>>() {
			public Pair<ByteBuffer, ByteBuffer> call() throws Exception {

//				for(int y=0;y<height;y++)
//				{
//					storage.first.position(y*width*3);
//					storage.first.limit((y+1)*width*3);
//					storage.second.position((height-y-1)*width*3);
//					storage.second.limit((height-y-1+1)*width*3);
//					storage.second.put(storage.first);
//					storage.second.clear();
//					storage.first.clear();
//				}

				try {
					j2.compress(filename, storage.first, width, height);
				} catch (Throwable t) {
					System.err.println(" -- exception thrown in compress for :" + filename + " " + storage + " " + width + " " + height);
					t.printStackTrace();
				}
				return storage;

			}
		};
	}

	private Pair<ByteBuffer, ByteBuffer> newStorage() {
		return new Pair<>(ByteBuffer.allocateDirect(width * height * 4), ByteBuffer.allocateDirect(width * height * 4));
	}


	@Override
	@HiddenInAutocomplete
	public String toString() {
		return "Screenshotter, saving to '" + prefix + "'";
	}
}
