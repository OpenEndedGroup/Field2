package field.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * An interface to ArrayBuffers.
 * <p>
 * An Array buffer ties two things together: OpenGL named and backed piece of memory with some CPU backed piece of memory. These piece of memory have
 * a number of elements "a size" and each of these elements has a "dimension" number of floats or ints (since a float and an int take up the same
 * amount of space it doesn't matter which).
 * <p>
 * An Array buffer also keeps track of it's dirty state --- what part of the CPU backed piece of memory needs to be reuploaded to OpenGL because it's
 * changed. Right now the caching strategy is quite simple, there's a range at the beginning of the buffer that's potentially dirty because it's been
 * written to
 * <p>
 * Currently we have exactly one implementation --- SimpleArrayBuffer -- but more have been seen in the wild (for example an ArrayBuffer that streams
 * animation data from Memory mapped files from disk).
 */
public interface ArrayBuffer {

	/**
	 * the number of elements of dimension in this buffer
	 */
	public int getSize();

	/**
	 * the OpenGL binding associated with this buffer
	 */
	public int getBinding();

	/**
	 * the Shader attribute associated with this buffer
	 */
	public int getAttribute();

	/**e
	 * the dimensionality of each element (between 1 and 4 inclusive).
	 */
	public int getDimension();

	/**
	 * returns a read/write view onto this buffer as a FloatBuffer (equivalent to floats(false))
	 */
	default public FloatBuffer floats() {
		return (FloatBuffer) floats(false);
	}

	/**
	 * returns a read/write view onto this buffer as an IntBuffer (equivalent to ints(false))
	 */
	default public IntBuffer ints() {
		return (IntBuffer) ints(false);
	}

	/**
	 * returns a read/write view onto this buffer as a FloatBuffer. If this is marked as readOnly then OpenGL won't necessarily get any changes we
	 * make to this buffer.
	 */
	public FloatBuffer floats(boolean readOnly);

	/**
	 * returns a read/write view onto this buffer as an IntBuffer. If this is marked as readOnly then OpenGL won't necessarily get any changes we
	 * make to this buffer.
	 */
	public IntBuffer ints(boolean readOnly);

	/**
	 * Replaces this buffer with a buffer of an identical class, but of a different size. Size here is in elements (that is, floats / ints *
	 * dimension).
	 */
	public ArrayBuffer replaceWithSize(int size);

	/**
	 * Causes the graphics system to ultimately relinquish OpenGL resources associated with this buffer
	 */
	public void destroy();

	/**
	 * Mark the area up to this point as clean --- internal use only (hence the access restriction)
	 */
	boolean clean(int limit);
}
