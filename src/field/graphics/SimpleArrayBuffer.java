package field.graphics;

import field.utility.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

	private final FloatBuffer dataAsFloat;
	private final IntBuffer dataAsInt;

	public class State {
		int name = -1;
		int mod = -1;
		int limit = 0;
	}

	private final int size;
	private final int binding;
	private final int attribute;
	private final int dimension;
	final int divisor;

	ByteBuffer data;
	int mod = 0;

	public SimpleArrayBuffer(int size, int binding, int attribute, int dimension, int divisor) {
		this.size = size;
		this.binding = binding;
		this.attribute = attribute;
		this.dimension = dimension;
		this.divisor = divisor;

		data = ByteBuffer.allocateDirect(4 * size * dimension).order(ByteOrder.nativeOrder());
		dataAsFloat = data.asFloatBuffer();
		dataAsInt = data.asIntBuffer();
	}

	@Override
	public boolean clean(int limit) {
		State state = GraphicsContext.get(this);
		Log.log("graphics.trace", "       clean " + state);
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
		return (FloatBuffer) dataAsFloat.rewind().limit(dimension * size);
	}

	@Override
	public IntBuffer ints(boolean readOnly) {
		if (!readOnly) mod++;
		return (IntBuffer) dataAsInt.rewind().limit(dimension * size);
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

		glBufferSubData(binding, 0, data);

		uploadBytes+=4*limit*dimension;

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

		return next;
	}

	@Override
	protected void finalize() throws Throwable {
		GraphicsContext.postQueueInAllContexts(() -> this.destroy());
	}

	static public ArrayBuffer newArrayBuffer(int maxVertex, int binding, int attribute, int dimension, int divisor) {
		return new SimpleArrayBuffer(maxVertex, binding, attribute, dimension, divisor);
	}

}
