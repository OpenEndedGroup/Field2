package field.graphics;

import field.utility.Log;
import org.lwjgl.opengl.ARBTextureStorage;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.*;


/**
 * An OpenGL texture.
 * <p>
 * Handles async double-buffered PBO texture upload by default.
 * <p>
 * Follows the same pattern as FBO --- create a texture by picking one of the growing number of static helpers in TextureSpecification that mask the complexity of OpenGL enums
 */
public class Texture extends BaseScene<Texture.State> implements Scene.Perform, OffersUniform<Integer> {

	// global statistics on how much we're sending to OpennGL
	static public int bytesUploaded = 0;
	public final TextureSpecification specification;
	int mod = 0;
	boolean isDoubleBuffered = true;
	AtomicInteger pendingUploads = new AtomicInteger(0);

	public Texture(TextureSpecification specification) {
		this.specification = specification;
		if (this.specification.forceSingleBuffered) setIsDoubleBuffered(false);
	}

	/**
	 * schedules an upload from this bytebuffer to this texture during the next drawn. Set stream to true to hint to OpenGL that you mean to keep on doing this.
	 */
	public void upload(ByteBuffer upload, boolean stream) {
		pendingUploads.incrementAndGet();
		attach(new Transient(() -> {
			pendingUploads.decrementAndGet();
			State s = GraphicsContext.get(this, null);

			Log.log("graphics.trace", "state for texture in upload is " + s);
			Log.log("texture.trace", "upload, part 1, for texture " + this + " " + s + " " + upload.capacity());

			if (s == null) return;

			s.x0 = 0;
			s.x1 = specification.width;
			s.y0 = 0;
			s.y1 = specification.height;

			Log.log("graphics.trace", "uploading ");
			glBindBuffer(GL_PIXEL_UNPACK_BUFFER, isDoubleBuffered ? (stream ? s.pboA : s.pboB) : s.pbo);
			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
			glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);
			s.old = GL15.glMapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, GL15.GL_WRITE_ONLY, s.old);
			s.old.rewind();
			upload.rewind();
			upload.limit(s.old.limit());
			s.old.put(upload);
			upload.clear();
			upload.rewind();
			s.old.rewind();

			bytesUploaded += s.old.limit();
			GL15.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER);
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
			Log.log("graphics.trace", "uploaded part 1");
			s.mod++;
		}, -2).setOnceOnly());
	}

	/**
	 * schedules an upload from the buffer that was originally used to create this texture to opengl
	 */
	public void upload() {
		pendingUploads.incrementAndGet();
		attach(new Transient(() -> {
			pendingUploads.decrementAndGet();
			State s = GraphicsContext.get(this, null);

			Log.log("graphics.trace", "state for texture in upload is " + s);
			Log.log("texture.trace2", "upload, part 1, for texture " + this + " " + s + " " + specification.pixels+" "+specification.pixels.get(0));

			if (s == null) return;

			glBindTexture(specification.target, s.name);
			glTexSubImage2D(specification.target, 0, 0, 0, specification.width, specification.height, specification.format, specification.type, specification.pixels);
			Log.log("texture.trace2", "upload, part 2? :" + glGetError());
			glBindTexture(specification.target, 0);

			if (specification.highQuality) {
				glGenerateMipmap(specification.target);
			}
		}, -2).setOnceOnly());
	}

	/**
	 * schedules an upload from this bytebuffer to this texture during the next drawn. Set stream to true to hint to OpenGL that you mean to keep on doing this.
	 */
	public void upload(ByteBuffer upload, boolean stream, int x0, int y0, int x1, int y1) {
		pendingUploads.incrementAndGet();
		attach(new Transient(() -> {
			pendingUploads.decrementAndGet();
			State s = GraphicsContext.get(this, null);


			Log.log("graphics.trace", "state for texture in upload is " + s);
			Log.log("texture.trace", "upload, part 1, for texture " + this + " " + s + " " + upload.capacity());

			if (s == null) return;
			s.x0 = x0;
			s.x1 = x1;
			s.y0 = y0;
			s.y1 = y1;
			glBindBuffer(GL_PIXEL_UNPACK_BUFFER, isDoubleBuffered ? (stream ? s.pboA : s.pboB) : s.pbo);
			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
			glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);

			int start = specification.elementSize * (specification.width * y0 + x0);
			int end = specification.elementSize * (specification.width * (y1 - 1) + x1);
			s.old = glMapBufferRange(GL21.GL_PIXEL_UNPACK_BUFFER, start, end - start, GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_INVALIDATE_RANGE_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT, s.old);

			s.old.position(0);
			upload.position(start);
			upload.limit(end);
			s.old.limit(end - start);

			s.old.put(upload);

			upload.clear();
			upload.rewind();
			s.old.clear();
			s.old.rewind();

			bytesUploaded += s.old.limit();

			GL15.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER);
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
			Log.log("graphics.trace", "uploaded part 1");
			s.mod++;
		}, -2).setOnceOnly());
	}

	public int getPendingUploads() {
		return pendingUploads.get();
	}

	protected boolean perform0() {

		State s = GraphicsContext.get(this);

		Log.log("graphics.trace", "activating texture :" + specification.unit + " = " + s.name);

		glActiveTexture(GL_TEXTURE0 + specification.unit);
		glBindTexture(specification.target, s.name);

		return true;
	}

	public int getOpenGLNameInCurrentContext() {
		State s = GraphicsContext.get(this);
		if (s == null) throw new IllegalArgumentException("No state in this context");

		return s.name;
	}

	public int getOpenGLNameInContext(GraphicsContext context) {
		State s = context.lookup(this);
		if (s == null) throw new IllegalArgumentException("No state in this context");

		return s.name;
	}

	protected State setup() {


		State s = new State();
		s.name = glGenTextures();
		s.pboA = glGenBuffers();
		if (isDoubleBuffered) s.pboB = glGenBuffers();
		s.pbo = s.pboA;


		glActiveTexture(GL_TEXTURE0 + specification.unit);
		glBindTexture(specification.target, s.name);


		if (!specification.highQuality) {
			glTexParameteri(specification.target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(specification.target, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(specification.target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(specification.target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(specification.target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		} else {
			glTexParameteri(specification.target, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(specification.target, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

		}


		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);


		ARBTextureStorage.glTexStorage2D(specification.target,
						 specification.highQuality ? (int) (Math.floor(Math.log(Math.max(specification.width, specification.height)) / Math.log(2)) + 1) : 1,
						 specification.internalFormat, specification.width, specification.height);


		glTexSubImage2D(specification.target, 0, 0, 0, specification.width, specification.height, specification.format, specification.type, specification.pixels);
		if (specification.highQuality) {
			glGenerateMipmap(specification.target);
		}


		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, s.pbo);

		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

		glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);

		glBufferData(GL_PIXEL_UNPACK_BUFFER, specification.elementSize * specification.width * specification.height, GL15.GL_STREAM_DRAW);


		if (isDoubleBuffered) {
			glBindBuffer(GL_PIXEL_UNPACK_BUFFER, s.pboB);
			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
			glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);
			glBufferData(GL_PIXEL_UNPACK_BUFFER, specification.elementSize * specification.width * specification.height, GL15.GL_STREAM_DRAW);

		}
		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);


		return s;
	}

	protected int upload(State s) {

		Log.log("graphics.trace", "finishing upload part 2");
		Log.log("texture.trace", "finishing upload part 2" + " " + specification);


		glActiveTexture(GL_TEXTURE0 + specification.unit);
		glBindTexture(specification.target, s.name);
		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, isDoubleBuffered ? s.pboB : s.pbo);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);


		int top = specification.elementSize * s.y0 * specification.width;
		glTexSubImage2D(specification.target, 0, 0, s.y0, specification.width, s.y1 - s.y0 - 1, specification.format, specification.type, top);


		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
		if (specification.highQuality) {
			glGenerateMipmap(specification.target);
		}
		long b = System.currentTimeMillis();

		if (isDoubleBuffered) {
			int q = s.pboA;
			s.pboA = s.pboB;
			s.pboB = q;
		}

		return mod;
	}

	public Texture setIsDoubleBuffered(boolean isDoubleBuffered) {
		this.isDoubleBuffered = isDoubleBuffered;
		return this;
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1};
	}

	protected void deallocate(State s) {
		glDeleteTextures(s.name);
		glDeleteBuffers(s.pboA);
		if (isDoubleBuffered) glDeleteBuffers(s.pboB);
	}

	@Override
	public Integer getUniform() {
		State s = GraphicsContext.get(this);
		if (s == null) return null;
		return s.name;

	}

	static public class TextureSpecification {
		public final int unit;
		public final int target;
		public final int internalFormat;
		public final int width;
		public final int height;
		public final int format;
		public final int type;
		public final int elementSize;
		public final boolean highQuality;
		public final ByteBuffer pixels;
		public final boolean forceSingleBuffered;

		public TextureSpecification(int unit, int target, int internalFormat, int width, int height, int format, int type, int elementSize, ByteBuffer pixels, boolean highQuality) {
			this.unit = unit;
			this.target = target;
			this.internalFormat = internalFormat;
			this.width = width;
			this.height = height;
			this.format = format;
			this.type = type;
			this.elementSize = elementSize;
			this.pixels = pixels;
			this.highQuality = highQuality;
			forceSingleBuffered = false;
		}

		public TextureSpecification(int unit, int target, int internalFormat, int width, int height, int format, int type, int elementSize, ByteBuffer pixels, boolean highQuality, boolean forceNotStreaming) {
			this.unit = unit;
			this.target = target;
			this.internalFormat = internalFormat;
			this.width = width;
			this.height = height;
			this.format = format;
			this.type = type;
			this.elementSize = elementSize;
			this.pixels = pixels;
			this.highQuality = highQuality;
			this.forceSingleBuffered = forceNotStreaming;
		}

		static public TextureSpecification byte3(int unit, int width, int height, ByteBuffer source, boolean mips) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB8, width, height, GL_RGB, GL_UNSIGNED_BYTE, 3, source, mips);
		}

		static public TextureSpecification byte1(int unit, int width, int height, ByteBuffer source, boolean mips) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL_R8, width, height, GL_RED, GL_UNSIGNED_BYTE, 1, source, mips);
		}

		static public TextureSpecification fromJpeg(int unit, String filename, boolean mips) {
			int[] wh = FastJPEG.j.dimensions(filename);
			ByteBuffer data = ByteBuffer.allocateDirect(3 * wh[0] * wh[1]);
			FastJPEG.j.decompress(filename, data, wh[0], wh[1]);
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB8, wh[0], wh[1], GL_RGB, GL_UNSIGNED_BYTE, 3, data, mips);
		}

		static public TextureSpecification from1DRGBAFloatBuffer(int unit, int length, FloatBuffer source) {
			ByteBuffer data = ByteBuffer.allocateDirect(4 * 4 * length)
						    .order(ByteOrder.nativeOrder());
			data.asFloatBuffer()
			    .put(source);
			data.rewind();
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RGBA32F, length, 1, GL_RGBA, GL_FLOAT, 16, data, false);
		}

		static public TextureSpecification from1DFloatBuffer(int unit, int length, FloatBuffer source) {
			ByteBuffer data = ByteBuffer.allocateDirect(4 * length);
			data.asFloatBuffer()
			    .put(source);
			data.rewind();
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_R32F, length, 1, GL_RED, GL_FLOAT, 4, data, false);
		}

		static public TextureSpecification from1DRFloatBuffer(int unit, int length, ByteBuffer data) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_R32F, length, 1, GL_RED, GL_FLOAT, 4, data, false, true);
		}

		static public TextureSpecification from1DRGFloatBuffer(int unit, int length, ByteBuffer data) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RG32F, length, 1, GL_RG, GL_FLOAT, 8, data, false, true);
		}

		static public TextureSpecification from1DRGBFloatBuffer(int unit, int length, ByteBuffer data) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RGB32F, length, 1, GL_RGB, GL_FLOAT, 12, data, false, true);
		}

		static public TextureSpecification from1DRGBAFloatBuffer(int unit, int length, ByteBuffer data) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RGBA32F, length, 1, GL_RGBA, GL_FLOAT, 16, data, false, true);
		}

		static public TextureSpecification byte4(int unit, int width, int height, ByteBuffer source, boolean mips) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGBA8, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 4, source, mips);
		}

		static public TextureSpecification float4(int unit, int width, int height, ByteBuffer source, boolean mips) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT, 16, source, mips);
		}

		static public TextureSpecification float4_1d(int unit, int width, ByteBuffer source, boolean mips) {
			return new TextureSpecification(unit, GL_TEXTURE_1D, GL30.GL_RGBA32F, width, 1, GL_RGBA, GL_FLOAT, 16, source, mips);
		}

		@Override
		public String toString() {
			return "TextureSpecification{" +
				    "unit=" + unit +
				    ", target=" + target +
				    ", internalFormat=" + internalFormat +
				    ", width=" + width +
				    ", height=" + height +
				    ", format=" + format +
				    ", type=" + type +
				    ", elementSize=" + elementSize +
				    ", highQuality=" + highQuality +
				    ", pixels=" + pixels +
				    '}';
		}
	}

	public class State extends BaseScene.Modifiable {
		protected int name = -1;

		protected int pboA;
		protected int pboB;
		protected int pbo;
		protected ByteBuffer old;
		int x0, x1, y0, y1;
	}
}
