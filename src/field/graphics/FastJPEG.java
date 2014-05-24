package field.graphics;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * This is a fast native jpeg loader. Well battle tested, thread-safe. We have versions for linux and os x.
 */
public class FastJPEG {

	static {
		System.loadLibrary("fieldjpegturb");
	}

	static public final FastJPEG j = new FastJPEG();

	public native void decompress(String filename, Buffer dest, int width, int height);

	public native void decompressFlipped(String filename, Buffer dest, int width, int height);

	public native void decompressGrey(String filename, Buffer dest, int width, int height);

	public native void compress(String filename, Buffer dest, int width, int height);

	public native long dimensionsFor(String filename);

	public int[] dimensions(String filename) {
		long dim = dimensionsFor(filename);
		if (dim == 0) return null;
		return new int[]{(int) (dim >> 16), (int) (dim & 0xffff)};
	}

	public Texture.TextureSpecification loadTexture(int unit, boolean mips, String filename) {

		int[] d = dimensions(filename);
		if (d == null) return null;

		ByteBuffer b = ByteBuffer.allocateDirect(3 * d[0] * d[1]);
		decompress(filename, b, d[0], d[1]);
		b.rewind();

		return Texture.TextureSpecification.byte3(unit, d[0], d[1], b, mips);
	}
}
