package field.linalg;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.Iterator;

/**
 * Utility class for Mapping arrays of Floats
 */
public class MappedFloatArray {


	private final RandomAccessFile raf;
	private final FloatBuffer fbuffer;
	private final ByteBuffer buffer;
	private final DoubleBuffer dbuffer;
	private final IntBuffer ibuffer;
	int preferredDimension = 3;

	public MappedFloatArray(String filename) throws IOException {
		raf = new RandomAccessFile(filename, "r");

		buffer = raf.getChannel()
			    .map(FileChannel.MapMode.READ_ONLY, 0, raf.length())
			    .order(ByteOrder.nativeOrder());
		fbuffer = buffer.asFloatBuffer();
		dbuffer = buffer.asDoubleBuffer();
		ibuffer = buffer.asIntBuffer();
	}

	public MappedFloatArray setPreferredDimension(int p) {
		preferredDimension = p;
		return this;
	}

	public Vec3 get3f(int index) {
		return new Vec3(fbuffer.get(index), fbuffer.get(index + 1), fbuffer.get(index + 2));
	}

	public int length3f() {
		return fbuffer.limit() / 3;
	}

	public Vec2 get2f(int index) {
		return new Vec2(fbuffer.get(index), fbuffer.get(index + 1));
	}

	public int length2f() {
		return fbuffer.limit() / 2;
	}

	public float get1f(int index) {
		return fbuffer.get(index);
	}

	public int length1f() {
		return fbuffer.limit();
	}

	public int length() {
		switch (preferredDimension) {
			case 1:
				return length1f();
			case 2:
				return length2f();
			case 3:
				return length3f();
			default:
				throw new IllegalArgumentException();
		}
	}

	public Vec3 toVec3(int index) {
		switch (preferredDimension) {
			case 1:
				new Vec3(get1f(index), 0, 0);
			case 2:
				get2f(index).toVec3();
			case 3:
				get3f(index);
			default:
				throw new IllegalArgumentException();
		}
	}

	public int preferredDimension() {
		return preferredDimension;
	}

	public Iterator<Vec3> getVec3s() {
		return new Iterator<Vec3>() {

			int index = 0;

			@Override
			public boolean hasNext() {
				return index < length();
			}

			@Override
			public Vec3 next() {
				return toVec3(index++);
			}
		};
	}

}
