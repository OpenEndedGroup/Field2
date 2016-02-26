package field.graphics;

import field.graphics.util.BufferUtils;
import field.utility.Log;

import java.nio.*;

import static org.lwjgl.opengl.ARBInstancedArrays.glVertexAttribDivisorARB;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

/**
 * An OpenGL Vertex Buffer backed by a ByteBuffer. Useful for VertexArrays and ElementArrays
 */
public class SimpleArrayBuffer implements ArrayBuffer {

	static public int uploadBytes = 0;
	final int divisor;
	private final FloatBuffer dataAsFloat;
	private final IntBuffer dataAsInt;
	private final int size;
	private final int binding;
	private final int attribute;
	private final int dimension;
	ByteBuffer data;
	int mod = 0;
	private FloatBuffer customStorage = null;
	public SimpleArrayBuffer(int size, int binding, int attribute, int dimension, int divisor) {
		this.size = size;
		this.binding = binding;
		this.attribute = attribute;
		this.dimension = dimension;
		this.divisor = divisor;

		data = ByteBuffer.allocateDirect(4 * size * dimension)
				 .order(ByteOrder.nativeOrder());
		dataAsFloat = data.asFloatBuffer();
		dataAsInt = data.asIntBuffer();
	}

	static public ArrayBuffer newArrayBuffer(int maxVertex, int binding, int attribute, int dimension, int divisor) {
		return new SimpleArrayBuffer(maxVertex, binding, attribute, dimension, divisor);
	}

	public FloatBuffer getCustomStorage() {
		return customStorage;
	}

	public void setCustomStorage(FloatBuffer customStorage) throws NoSuchFieldException, IllegalAccessException {
		this.customStorage = customStorage;
		data = BufferUtils.asByteBuffer(customStorage);
		mod++;
	}

	@Override
	public boolean clean(int limit) {
		State state = GraphicsContext.get(this);
		final State finalState = state;
		Log.log("graphics.trace", ()-> "       clean " + finalState);
		if (state == null) GraphicsContext.put(this, state = setup());
		if (state.mod != mod || state.limit < limit) {
			upload(state, limit);
			state.mod = mod;
			return true;
		}
		return false;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int getBinding() {
		return binding;
	}

	@Override
	public int getAttribute() {
		return attribute;
	}

	@Override
	public int getDimension() {
		return dimension;
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

	public void destroy() {
		State s = GraphicsContext.get(this);
		if (s != null) {
			GraphicsContext.invalidateInThisContext(this);
			glDeleteBuffers(s.name);
		}
	}

	@Override
	public FloatBuffer floats(boolean readOnly) {
		if (!readOnly) mod++;
		return (FloatBuffer) dataAsFloat.rewind()
						.limit(dimension * size);
	}

	@Override
	public IntBuffer ints(boolean readOnly) {
		if (!readOnly) mod++;
		return (IntBuffer) dataAsInt.rewind()
					    .limit(dimension * size);
	}

	@Override
	public ByteBuffer bytes(boolean readOnly) {
		if (!readOnly) mod++;
		return (ByteBuffer) data.rewind().limit(dimension*size*4);
	}

	private State setup() {
		State s = new State();

		s.name = glGenBuffers();

		glBindBuffer(binding, s.name);

		if (divisor != 0) glVertexAttribDivisorARB(attribute, divisor);

		glBufferData(binding, size * 4 * dimension, GL_STATIC_DRAW);

		if (attribute == -1) {

		} else {
			glEnableVertexAttribArray(attribute);
			glVertexAttribPointer(attribute, dimension, GL_FLOAT, false, 0, 0);
		}

		glBindBuffer(binding, 0);

		return s;
	}

	private void upload(State s, int limit) {

		glBindBuffer(binding, s.name);
		data.rewind();
		data.limit(4 * limit * dimension);

		s.limit = limit;

		if (customStorage != null) {
			if (customStorage.limit() < (limit * dimension)) Log.log("graphics.error", ()->"ERROR: not enough data in bound storage, attribute " + attribute);
			else glBufferSubData(binding, 0, customStorage);
		} else {
			glBufferSubData(binding, 0, data);
		}
		uploadBytes += 4 * limit * dimension;

	}

	@Override
	public ArrayBuffer replaceWithSize(int size) {
		SimpleArrayBuffer next = new SimpleArrayBuffer(size, binding, attribute, dimension, divisor);

		int min = Math.min(size, this.size);

		next.data.clear();
		this.data.clear();
		this.data.limit(4 * min * dimension);
		next.data.limit(4 * min * dimension);

		next.data.put(this.data);
		next.data.clear();
		this.data.clear();

		if (customStorage!=null && customStorage.limit()>=size*dimension)
		{
			next.customStorage = customStorage;
			//TODO skip the bit above where we bother to even allocate the non "custom" storage
		}

		return next;
	}

	@Override
	protected void finalize() throws Throwable {
		GraphicsContext.postQueueInAllContexts(() -> this.destroy());
	}

	public class State {
		int name = -1;
		int mod = -1;
		int limit = 0;
	}

}
