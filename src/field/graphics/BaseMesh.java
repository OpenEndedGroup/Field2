package field.graphics;

import field.linalg.Vec3;
import field.utility.Log;

import java.lang.reflect.Array;
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
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_LINES_ADJACENCY;

/**
 * This is the base class for geometry in the Field graphics system.
 * <p>
 * Geometry includes Triangle meshes, lines lists and point lists (which are all subclasses of this). Further customizations can be made by passing in
 * an ArrayBufferFactory.
 */
public class BaseMesh extends Scene implements Scene.Perform {


	/**
	 * return a view onto the vertex storage for this mesh. Write your vertex data into this FloatBuffer
	 */
	public FloatBuffer vertex(boolean readOnly) {
		return buffer(0, 3).floats(false);
	}

	/**
	 * equivalent to vertex(false)
	 *
	 * @return
	 */
	public FloatBuffer vertex() {
		return vertex(false);
	}

	/**
	 * return a view onto the element storage for this mesh. Write your element data into this IntBuffer
	 */
	public IntBuffer elements(boolean readOnly) {
		return elements.ints(readOnly);
	}


	/**
	 * equivalent to elements(false)
	 */
	public IntBuffer elements() {
		return elements(false);
	}

	/**
	 * return a view onto the aux storage for this mesh. Write your aux data into these FloatBuffer. This data appears in shaders, associated per
	 * vertex.
	 * <p>
	 * For example, this statement in a vertex shader:
	 * <p>
	 * "layout(location=1) in vec4 color"
	 * <p>
	 * means that you can write
	 * <p>
	 * mesh.aux(1, 4).put([1,2,3,4])...
	 * <p>
	 * location=1 gives the attribute (=1) and vec4 gives the dimension. location=0 is taken by the vertex position itself; typically you can have
	 * up to and including location=15.
	 */
	public FloatBuffer aux(int attribute, int dimension) {
		return buffer(attribute, dimension).floats(false);
	}


	public interface ArrayBufferFactory {
		public ArrayBuffer newArrayBuffer(int maxVertex, int binding, int attribute, int dimension, int divisor);
	}


	int maxVertex;
	int limitVertex;

	int maxElement;
	int limitElement;

	ArrayBuffer elements;
	ArrayBuffer[] buffers = new ArrayBuffer[16];
	ArrayBufferFactory arrayBufferFactory = SimpleArrayBuffer::newArrayBuffer;

	/**
	 * limit the number of vertices sent to OpenGL without truncating the declared size of the storage.
	 */
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

	/**
	 * truncate or extend the size of this mesh's vertex storage (and aux storage)
	 */
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

	/**
	 * limit the number of elements sent to OpenGL without truncating the declared size of the storage.
	 */
	public void setElementLimit(int limit) {
		if (elements == null) return;

		if (limit > elements.getSize()) {
			elements = elements.replaceWithSize(limit);
		}

		limitElement = limit;
		maxElement = Math.max(maxElement, limitElement);
	}

	/**
	 * truncate or extend the size of the meshes element storage
	 */
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

	public int getElementDimension() { return elements.getDimension(); };

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
		if (buffers[attribute].getDimension() != dimension)
			throw new IllegalArgumentException(" dimension mismatch. Attribute " + attribute + " was previously declared to be of dimension " + dimension + " not " + buffers[attribute]
				    .getDimension());

		return buffers[attribute];
	}

	public Map<Integer, ArrayBuffer> buffers() {
		Map<Integer, ArrayBuffer> m = new LinkedHashMap<Integer, ArrayBuffer>();
		for (int i = 0; i < buffers.length; i++)
			if (buffers[i] != null) m.put(i, buffers[i]);
		return m;
	}

	@Override
	public boolean perform(int pass) {

		Log.log("graphics.trace", " perform pass :" + this + " / " + pass);

		if (pass == 0) {
			Integer va = GraphicsContext.get(this);
			if (va == null) {
				va = glGenVertexArrays();
				GraphicsContext.put(this, va);
			}

			Log.log("graphics.trace", " va name is " + va);
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
				if (primitiveSize == 0) {
					glDrawArrays(primitiveType, 0, limitVertex);
				} else {
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

	/**
	 * returns a new BaseMesh that draws up to numPoints.
	 * <p>
	 * call vertex() to get a FloatBuffer into which you can put vertex data, aux(...) to get FloatBuffers to put aux data.
	 * <p>
	 * for example:
	 * <p>
	 * list = pointList(2) list.vertex().put([0,0,0]).put([1,1,1])
	 * <p>
	 * will give you a point list that draws two points, one at the origin, one at 1,1,1
	 */
	static public BaseMesh pointList(int numPoints) {
		return standard(numPoints, 0, GL_POINTS, 0);
	}

	/**
	 * returns a new BaseMesh that draws up to numPoints vertices and numElement lines.
	 * <p>
	 * call vertex() to get a FloatBuffer into which you can put vertex data, aux(...) to get FloatBuffers to put aux data and call elements() to
	 * connect vertices together with line segments
	 * <p>
	 * for example:
	 * <p>
	 * lines = lineList(3, 2) lines.vertex().put([0,0,0]).put([1,0,0]).put([1,1,0]) lines.elements().put([0,1]).put([1,2])
	 * <p>
	 * will give an L shaped, two line segment shape.
	 */
	static public BaseMesh lineList(int numPoints, int numElements) {
		return standard(numPoints, numElements, GL_LINES, 2);
	}

	/**
	 * returns a new BaseMesh that draws up to numPoints vertices and numElement lines with adjecency
	 * <p>
	 * call vertex() to get a FloatBuffer into which you can put vertex data, aux(...) to get FloatBuffers to put aux data and call elements() to
	 * connect vertices together with line segments
	 * <p>
	 * for example:
	 * <p>
	 * lines = lineAdjecencyList(3, 2) lines.vertex().put([0,0,0]).put([1,0,0]).put([1,1,0]) lines.elements().put([0, 0, 1, 2]).put([0, 1,2, 2])
	 * <p>
	 * will give an L shaped, two line segment shape.
	 */
	static public BaseMesh lineAdjecencyList(int numPoints, int numElements) {
		return standard(numPoints, numElements, GL_LINES_ADJACENCY, 4);
	}

	/**
	 * returns a new BaseMesh that draws up to numPoints vertices and numElement lines with adjecency
	 * <p>
	 * call vertex() to get a FloatBuffer into which you can put vertex data, aux(...) to get FloatBuffers to put aux data and call elements() to
	 * connect vertices together with line segments
	 * <p>
	 * for example:
	 * <p>
	 * lines = triangleList(3, 2) lines.vertex().put([0,0,0]).put([1,0,0]).put([1,1,0]) lines.elements().put([0, 1, 2])
	 * <p>
	 * will give an triangle.
	 */
	static public BaseMesh triangleList(int numPoints, int numElements) {
		return standard(numPoints, numElements, GL_TRIANGLES, 3);
	}

	/**
	 * destroy this mesh and all OpenGL resources associated with it.
	 */
	public void finalize() {
		GraphicsContext.postQueueInAllContexts(this::destroy);
	}

	protected void destroy() {
		Integer s = GraphicsContext.remove(this);
		if (s == null) return;
		glDeleteVertexArrays(s);
		for (ArrayBuffer b : buffers)
			b.destroy();
		if (elements != null) elements.destroy();
	}

	/**
	 * checks the contents of this mesh for validity and prints out information
	 */
	public void debugContents(String channel)
	{
		Log.log(channel, "debugContents for mesh "+this);
		int vl = getVertexLimit();
		int el = getElementLimit();
		Log.log(channel, "VL :"+vl+" | "+el+" "+this.buffers[0].floats(true)+" / "+this.elements(true));
		for(ArrayBuffer a : this.buffers)
		{
			if (a==null) continue;
			Log.log(channel, "buffer "+a.getAttribute()+" "+a.getBinding()+" "+a.getDimension()+" "+a.getSize());
		}


		if (this.buffers[0]!=null)
		{
			Log.log(channel, "checking elements "+this);
			IntBuffer a = this.elements(true);
			FloatBuffer f = this.buffers[0].floats();

			int st = this.elements.getDimension();

			for(int q=0;q<el*st;q++)
			{
				Log.log(channel, (q/st)+" | "+a.get(q)+" -> "+(a.get(q)<vl ? new Vec3(f.position(3*a.get(q))) : "ILLEGAL"));
				if ((q+1)%st==0)
					Log.log(channel, ".");
			}
		}
	}

}
