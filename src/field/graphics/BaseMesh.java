package field.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL32.GL_LINES_ADJACENCY;

/**
 * Created by marc on 3/10/14.
 */
public class BaseMesh extends Scene implements Scene.Perform {

	public interface ArrayBufferFactory {
		public ArrayBuffer newArrayBuffer(int maxVertex, int binding, int attribute, int dimension, int divisor);
	}

	public BaseMesh() {
	}

	int maxVertex;
	int limitVertex;

	int maxElement;
	int limitElement;

	ArrayBuffer elements;
	ArrayBuffer[] buffers = new ArrayBuffer[16];
	ArrayBufferFactory arrayBufferFactory = SimpleArrayBuffer::newArrayBuffer;

	public int setVertexLimit(int limit) {
		for (int i = 0; i < buffers.length; i++) {
			ArrayBuffer o = buffers[i];
			if (o != null && o.getSize() < limit) {
				buffers[i] = buffers[i].replaceWithSize(limit);
			}
		}
		limitVertex = limit;
		trimVertexLimit();
		maxVertex = Math.max(maxVertex, limitVertex);
		return limitVertex;
	}

	private int setVertexMax(int vertexMax) {
		for (int i = 0; i < buffers.length; i++) {
			ArrayBuffer o = buffers[i];
			if (o != null && o.getSize() != vertexMax) {
				buffers[i] = buffers[i].replaceWithSize(vertexMax);
			}
		}
		limitVertex = maxVertex = vertexMax;
		trimVertexLimit();
		maxVertex = limitVertex;
		return maxVertex;
	}

	public void setElementLimit(int limit) {
		if (elements == null) return;

		if (limit > elements.getSize())
		{
			elements = elements.replaceWithSize(limit);
		}

		limitElement = limit;
		maxElement = Math.max(maxElement, limitElement);
	}

	public void setElementMax(int limit) {
		if (elements == null) return;

		if (limit != elements.getSize()) elements = elements.replaceWithSize(limit);

		limitElement = maxElement = elements.getSize();
	}

	public void trimVertexLimit() {
		int limit = limitVertex;
		for (int i = 0; i < buffers.length; i++) {
			ArrayBuffer o = buffers[i];
			if (o != null) limit = Math.min(limit, o.getSize());
		}
		limitVertex = limit;
	}

	public void trimElementLimit() {
		if (elements == null) return;
		limitElement = Math.min(limitElement, elements.getSize());
	}

	public int getElementLimit() {
		return limitElement;
	}

	public int getVertexLimit() {
		return limitVertex;
	}

	public void setBuffer(int attribute, ArrayBuffer buffer) {
		if (buffer.getSize() < limitVertex) buffers[attribute] = buffer.replaceWithSize(limitVertex);
		buffers[attribute] = buffer;
	}

	public void setElements(ArrayBuffer buffer) {
		if (buffer.getSize() < limitVertex) elements = buffer.replaceWithSize(limitVertex);
		if (buffer.getBinding() != GL_ELEMENT_ARRAY_BUFFER)
			throw new IllegalArgumentException(" can't use this buffer as an elements buffer, it's a GL_ARRAY_BUFFER not a GL_ELEMENT_ARRAY_BUFFER ");
		elements = buffer;
	}

	public ArrayBuffer buffer(int attribute, int dimension) {
		if (buffers[attribute] == null && dimension > 0) {
			buffers[attribute] = arrayBufferFactory.newArrayBuffer(maxVertex, GL_ARRAY_BUFFER, attribute, dimension, 0);
			return buffers[attribute];
		}
		if (buffers[attribute].getDimension() != dimension) throw new IllegalArgumentException(" dimension mismatch. Attribute "+attribute+" was previously declared to be of dimension "+dimension+" not "+buffers[attribute].getDimension());

		return buffers[attribute];
	}

	public Map<Integer, ArrayBuffer> buffers() {
		Map<Integer, ArrayBuffer> m = new LinkedHashMap<Integer, ArrayBuffer>();
		for (int i = 0; i < buffers.length; i++)
			if (buffers[i] != null) m.put(i, buffers[i]);
		return m;
	}

	public FloatBuffer aux(int attribute, int stride) {
		return buffer(attribute, stride).floats(false);
	}

	public FloatBuffer vertex(boolean readOnly) {
		return buffer(0, 3).floats(false);
	}

	public IntBuffer elements(boolean readOnly) {
		return elements.ints(readOnly);
	}

	public IntBuffer elements() {
		return elements(false);
	}


	@Override
	public boolean perform(int pass) {

		if (GraphicsContext.trace)
		{
			System.out.println(" perform pass :" + this + " / " + pass);
		}

		if (pass == 0) {
			Integer va = GraphicsContext.get(this);
			if (va == null) {
				va = glGenVertexArrays();
				GraphicsContext.put(this, va);
			}

			if (GraphicsContext.trace) System.out.println(" va name is " + va);
			glBindVertexArray(va);

			boolean work = false;

			trimElementLimit();
			trimVertexLimit();

			if (elements != null) work |= elements.clean(limitElement);

			LinkedHashSet<Integer> notSeen = new LinkedHashSet<>();
			notSeen.addAll(Arrays.asList(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}));

			for (ArrayBuffer b : buffers)
				if (b != null) {
					work |= b.clean(limitVertex);
					notSeen.remove(b.getAttribute());
				}

			if (work) {
				for (ArrayBuffer b : buffers)
					if (b != null) glEnableVertexAttribArray(b.getAttribute());

				for (Integer i : notSeen)
					glDisableVertexAttribArray(i);
			}

			super.update(0, this::performNow);

			glBindVertexArray(0);

		}
		return true;
	}

	protected boolean performNow() {
		return true;
	}

	@Override
	public int[] getPasses() {
		return new int[]{0};
	}

	static public BaseMesh standard(int numVertex, int numElements, int primitiveType, int primitiveSize) {
		BaseMesh m = new BaseMesh() {
			protected boolean performNow() {
				if (primitiveSize == 0)
				{
					glDrawArrays(primitiveType, 0, limitVertex);
				}
				else
				{
					glDrawElements(primitiveType, limitElement * primitiveSize, GL_UNSIGNED_INT, 0);
				}
				return true;
			}
		};
		m.setVertexMax(numVertex);
		if (primitiveSize > 0) {
			m.setElements(new SimpleArrayBuffer(numElements, GL_ELEMENT_ARRAY_BUFFER, -1, primitiveSize, 0));
			m.setElementMax(numElements);
		}
		return m;
	}

	static public BaseMesh pointList(int numPoints) {
		return standard(numPoints, 0, GL_POINTS, 0);
	}

	static public BaseMesh lineList(int numPoints, int numElements) {
		return standard(numPoints, numElements, GL_LINES, 2);
	}

	static public BaseMesh lineAdjecencyList(int numPoints, int numElements) {
		return standard(numPoints, numElements, GL_LINES_ADJACENCY, 4);
	}

	static public BaseMesh triangleList(int numPoints, int numElements) {
		return standard(numPoints, numElements, GL_TRIANGLES, 3);
	}

	public void finalize() {
		GraphicsContext.postQueueInAllContexts(this::destroy);
	}

	protected void destroy() {
		Integer s = GraphicsContext.remove(this);
		if (s == null) return;
		glDeleteVertexArrays(s);
		for(ArrayBuffer b : buffers)
			b.destroy();
		if (elements!=null)
			elements.destroy();
	}

}
