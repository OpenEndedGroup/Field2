package field.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by marc on 3/10/14.
 */
public interface ArrayBuffer {
	boolean clean(int limit);

	public int getSize();

	public int getBinding();

	public int getAttribute();

	public int getDimension();

	default public FloatBuffer floats() {
		return (FloatBuffer) floats(false);
	}

	default public IntBuffer ints() {
		return (IntBuffer) ints(false);
	}

	public FloatBuffer floats(boolean readOnly);

	public IntBuffer ints(boolean readOnly);

	public ArrayBuffer replaceWithSize(int size);

	public void destroy();
}
