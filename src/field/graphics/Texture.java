package field.graphics;

import field.utility.Log;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL42.glTexStorage2D;


/**
 * An OpenGL texture.
 * <p>
 * Handles async double-buffered PBO texture upload by default.
 * <p>
 * Follows the same pattern as FBO --- create a texture by picking one of the growing number of static helpers in TextureSpecification that mask the complexity
 * of OpenGL enums
 */
public class Texture extends BaseScene<Texture.State> implements Scene.Perform {

	public class State extends BaseScene.Modifiable {
		protected int name = -1;

		protected int pboA;
		protected int pboB;
		protected int pbo;

		protected ByteBuffer old;
	}

	public final TextureSpecification specification;

	public Texture(TextureSpecification specification) {
		this.specification = specification;
	}

	int mod = 0;

	AtomicInteger pendingUploads = new AtomicInteger(0);

	/**
	 * schedules an upload from this bytebuffer to this texture during the next drawn. Set stream to true to hint to OpenGL that you mean to keep
	 * on doing this.
	 */
	public void upload(ByteBuffer upload, boolean stream) {
		pendingUploads.incrementAndGet();
		connect(new Transient(() -> {
			pendingUploads.decrementAndGet();
			State s = GraphicsContext.get(this, null);

			Log.log("graphics.trace", "state for texture in upload is "+s);

			if (s == null) return;

			Log.log("graphics.trace", "uploading ");
			glBindBuffer(GL_PIXEL_UNPACK_BUFFER, s.pboA);
			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
			glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);
			glBufferData(GL_PIXEL_UNPACK_BUFFER, specification.elementSize * specification.width * specification.height, stream ? GL15.GL_STREAM_DRAW : GL15.GL_STATIC_DRAW);
			s.old = GL15.glMapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, GL15.GL_WRITE_ONLY, s.old);
			s.old.rewind();
			upload.rewind();
			upload.limit(s.old.limit());
			s.old.put(upload);
			upload.clear();
			upload.rewind();
			s.old.rewind();
			GL15.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER);
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
			Log.log("graphics.trace", "uploaded part 1");
			s.mod++;
		}, -2));
	}

	public int getPendingUploads()
	{
		return pendingUploads.get();
	}

	protected boolean perform0() {

		State s = GraphicsContext.get(this);

		Log.log("graphics.trace", "activating texture :"+specification.unit+" = "+s.name);

		glActiveTexture(GL_TEXTURE0 + specification.unit);
		glBindTexture(specification.target, s.name);

		return true;
	}


	protected State setup() {
		State s = new State();
		s.name = glGenTextures();
		s.pboA = glGenBuffers();
		s.pboB = glGenBuffers();
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
		}

		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);

		glTexStorage2D(specification.target, specification.highQuality ? (int) (Math
			    .floor(Math.log(Math.max(specification.width, specification.height)) / Math
					.log(2)) + 1) : 1, specification.internalFormat, specification.width, specification.height);
		glTexSubImage2D(specification.target, 0, 0, 0, specification.width, specification.height, specification.format, specification.type, specification.pixels);
		if (specification.highQuality) {
			glGenerateMipmap(specification.target);
		}
		return s;
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
		}

		static public TextureSpecification byte3(int unit, int width, int height, ByteBuffer source, boolean mips) {
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB8, width, height, GL_RGB, GL_UNSIGNED_BYTE, 3, source, mips);
		}

		static public TextureSpecification fromJpeg(int unit, String filename, boolean mips) {
			int[] wh = FastJPEG.j.dimensions(filename);
			ByteBuffer data = ByteBuffer.allocateDirect(3 * wh[0] * wh[1]);
			FastJPEG.j.decompress(filename, data, wh[0], wh[1]);
			return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB8, wh[0], wh[1], GL_RGB, GL_UNSIGNED_BYTE, 3, data, mips);
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
	}

	protected int upload(State s) {

		Log.log("graphics.trace", "finishing upload part 2");
		glActiveTexture(GL_TEXTURE0 + specification.unit);
		glBindTexture(specification.target, s.name);
		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, s.pbo);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);
		glTexSubImage2D(specification.target, 0, 0, 0, specification.width, specification.height, specification.format, specification.type, 0);
		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
		if (specification.highQuality) {
			glGenerateMipmap(specification.target);
		}
		long b = System.currentTimeMillis();

		int q = s.pboA;
		s.pboA = s.pboB;
		s.pboB = q;

		return mod;
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1};
	}


	protected void deallocate(State s) {
		glDeleteTextures(s.name);
		glDeleteBuffers(s.pboA);
		glDeleteBuffers(s.pboB);
	}
}
