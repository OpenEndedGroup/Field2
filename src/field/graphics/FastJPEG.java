package field.graphics;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * This is a fast native jpeg loader. Well battle tested, thread-safe. We have versions for linux and os x.
 */
public class FastJPEG implements JPEGLoader {

	static public boolean available;

	static {
		available = false;
		try {
			System.loadLibrary("fieldjpegturb");
			available = true;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (!available) try {
			System.loadLibrary("turbojpegF");
			available = true;
		} catch (Throwable t) {
		}
		if (!available) System.out.println(" Fast jpeg loading is not available. This isn't typically going to be a problem.");
		else
			System.out.println(" Fast jpeg loading is available");
	}

	static public final JPEGLoader j;

	static {
		if (available) j = new FastJPEG();
		else j = new SlowJPEG();
	}

	public native void decompress(String filename, Buffer dest, int width, int height);

	public native void decompress4(String filename, Buffer dest, int width, int height);

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
