package field.graphics;

import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Util;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Consumer;

/**
 * Dynamically create geometry piece by piece in a way that is cacheable and as friendly to contemporary OpenGL as possible.
 */
public class MeshBuilder implements MeshAcceptor, Bracketable {

	static public int cacheHits = 0;
	static public int cacheMisses_cursor = 0;
	static public int cacheMisses_externalHash = 0;
	static public int cacheMisses_internalHash = 0;
	static public int cacheMisses_tooOld = 0;
	static public float GROWTH = 1.5f;
	int openCount = 0;
	long buildNumber = 0;
	// no shrinking yet, but when we do, we have to kill all bookmarks (at least all bookmarks above the trim line, which is impossible to compute right now)
	int shrinkage = 0;
	int vertexCursor = 0;
	int elementCursor = 0;
	Map<Integer, float[]> aux = new HashMap<Integer, float[]>();
	MeshBuilder_tesselationSupport tessSupport = null;
	private BaseMesh target;

	public MeshBuilder(BaseMesh target) {
		this.target = target;
	}

	/**
	 * opens a MeshBuilder to get it ready for accepting geometry. The first call to open() will clear out all the geometry that was previously in this container. Nested calls to open() without a
	 * corresponding call to close() will append. Make sure that there's a matching close() for every open() (i.e. learn how to use try-with-resources and try-finally blocks)
	 */
	public MeshBuilder open() {
		if (openCount == 0) {
			doOpen();
		}
		openCount++;
		return this;
	}

	/**
	 * opens a MeshBuilder to get it ready for accepting additional geometry. Make sure that there's a matching close() for every open() (i.e. learn how to use try-with-resources and try-finally
	 * blocks)
	 */
	public void openAppend() {
		if (openCount == 0) {
			doOpenAppend();
		}
		openCount++;
	}

	/**
	 * is this MeshBuilder open and ready to accept geometry?
	 */
	public boolean isOpen() {
		return openCount > 0;
	}

	private void doOpen() {
		vertexCursor = 0;
		elementCursor = 0;
		buildNumber++;
	}

	private void doOpenAppend() {
		vertexCursor = target.getVertexLimit();
		elementCursor = target.getElementLimit();
	}

	/**
	 * closes a MeshBuilder. There should be a close() for every open() and it's an error to let the graphics system get hold of a MeshBuilder that isn't closed properly. Alas, this is a hard
	 * error to catch since the missing close() by definition doesn't have a known stacktrace. Closing something that isn't open will through an IllegalArgumentException, but again, this isn't
	 * always local to the site of the mismatch error.
	 */
	public void close() {
		openCount--;
		if (openCount == 0) {
			doClose();
		}
		if (openCount < 0) {
			throw new IllegalArgumentException("more closes than opens?");
		}
	}

	private void doClose() {
		target.setVertexLimit(vertexCursor);
		target.setElementLimit(elementCursor);
	}

	/**
	 * equivalent to a number of calls to aux(int attribute, float[] add) simultanously
	 */
	public MeshBuilder aux(Map<Integer, Object> add) {
		Iterator<Map.Entry<Integer, Object>> m = add.entrySet()
							    .iterator();
		while (m.hasNext()) {
			Map.Entry<Integer, Object> n = m.next();
			float[] fa = toFloatArray(n.getValue());
			if (fa != null) aux.put(n.getKey(), fa);
		}
		return this;
	}

	/**
	 * Adds some aux data associated with attribute "attribute" with dimension add.length. "attribute" must be between 1 and 15 inclusive, add must have dimension 1,2,3 or 4.
	 */
	public MeshBuilder aux(int attribute, float[] add) {
		aux.put(attribute, add);
		ensureExists(attribute, add.length, vertexCursor);
		return this;
	}


	/**
	 * Adds some aux data associated with attribute "attribute" with dimension 1. "attribute" must be between 1 and 15 inclusive
	 */
	public MeshBuilder aux(int attribute, float add) {
		aux.put(attribute, new float[]{add});
		ensureExists(attribute, 1, vertexCursor);
		return this;
	}

	/**
	 * Adds some aux data associated with attribute "attribute" with dimension 2. "attribute" must be between 1 and 15 inclusive
	 */
	public MeshBuilder aux(int attribute, float x, float y) {
		aux.put(attribute, new float[]{x, y});
		ensureExists(attribute, 2, vertexCursor);
		return this;
	}

	/**
	 * Adds some aux data associated with attribute "attribute" with dimension 3. "attribute" must be between 1 and 15 inclusive
	 */
	public MeshBuilder aux(int attribute, float x, float y, float z) {
		aux.put(attribute, new float[]{x, y, z});
		ensureExists(attribute, 3, vertexCursor);
		return this;
	}

	/**
	 * Adds some aux data associated with attribute "attribute" with dimension 4. "attribute" must be between 1 and 15 inclusive
	 * <p>
	 * (Note, even though the alphabet goes wxyz, OpenGL, and everybody else, has 4-vectors go xyzw)
	 */
	public MeshBuilder aux(int attribute, float x, float y, float z, float w) {
		aux.put(attribute, new float[]{x, y, z, w});
		ensureExists(attribute, 4, vertexCursor);
		return this;
	}

	/**
	 * Adds some aux data associated with attribute "attribute" with dimension 2. "attribute" must be between 1 and 15 inclusive
	 */
	public MeshBuilder aux(int attribute, Vec2 x) {
		aux.put(attribute, new float[]{(float) x.x, (float) x.y});
		ensureExists(attribute, 2, vertexCursor);
		return this;
	}

	/**
	 * Adds some aux data associated with attribute "attribute" with dimension 3. "attribute" must be between 1 and 15 inclusive
	 */
	public MeshBuilder aux(int attribute, Vec3 x) {
		aux.put(attribute, new float[]{(float) x.x, (float) x.y, (float) x.z});
		ensureExists(attribute, 3, vertexCursor);
		return this;
	}

	/**
	 * Adds some aux data associated with attribute "attribute" with dimension 4. "attribute" must be between 1 and 15 inclusive
	 */
	public MeshBuilder aux(int attribute, Vec4 x) {
		aux.put(attribute, new float[]{(float) x.x, (float) x.y, (float) x.z, (float) x.w});
		ensureExists(attribute, 4, vertexCursor);
		return this;
	}

	/**
	 * binds a FloatBuffer to this aux attribute. Dimension must be 1,2,3 or 4, storage.limit() must equal the number of vertices in this mesh * that dimension. You can call this over and over again to force a re-upload of this FloatBuffer to the GPU
	 */
	public MeshBuilder bindAux(int attribute, int dimension, FloatBuffer storage)
	{
		ArrayBuffer a = ensureExists(attribute, dimension, vertexCursor);

		if (a instanceof SimpleArrayBuffer)
		{
			((SimpleArrayBuffer)a).setCustomStorage(storage);
		}
		else
		{
			throw new IllegalArgumentException(" can't bind to arraybuffer of class "+(a.getClass()));
		}

		return this;
	}

	private float[] toFloatArray(Object value) {
		if (value instanceof float[]) return (float[]) value;

		throw new IllegalArgumentException("cannot interpret " + value + " as float array");
	}

	/**
	 * Creates a new cache bookmark at this point in the construction of the geometry. You can use a pair of bookmarks at some future recreation of this mesh to ask if a group of calls to
	 * nextVertex, nextElement, and aux can be skipped completely
	 */
	public Bookmark bookmark() {
		return new Bookmark();
	}

	/**
	 * Can all the calls to nextVertex, nextElement and aux, between these two bookmarks be skipped? Returns true if we have skipped forward, false otherwise.
	 */
	public boolean skipTo(Bookmark from, Bookmark to) {
		if (!from.stillValid() || from.getOuter() != this) return false;
		vertexCursor = to.vertexCursor;
		elementCursor = to.elementCursor;
		return true;
	}

	/**
	 * Can all the calls to nextVertex, nextElement and aux, between these two bookmarks be skipped? Returns true if we have skipped forward, false otherwise. Bookmarks can be unskippable because
	 * externalHashes have changed, because internalHashes have changed (for example an aux channgel has been added) or because the geometry layout has changed (the number of vertices or elements
	 * added up to this point is different).
	 * <p>
	 * If false, then from is reset with the value of "externalHash", updator is called with updator.accept(this) and then to is reset with the value of externalHash.
	 */
	public boolean skipTo(Bookmark from, Bookmark to, Object externalHash, Consumer<MeshBuilder> updator) {

		if (!from.stillValid(externalHash) || from.getOuter() != this) {
			from.reset(externalHash);
			updator.accept(this);
			to.reset(externalHash);

			return false;
		}
		vertexCursor = to.vertexCursor;
		elementCursor = to.elementCursor;
		return true;
	}

	/**
	 * Can all the calls to nextVertex, nextElement and aux, between these two bookmarks be skipped? Returns true if we have skipped forward, false otherwise.
	 * <p>
	 * Equivalent to skipTo(from, to, externalHash, (Consumer<MeshBuilder> x -> updator.run())
	 */
	public boolean skipTo(Bookmark from, Bookmark to, Object externalHash, Runnable updator) {
		return skipTo(from, to, externalHash, x -> updator.run());
	}

	/**
	 * Adds a vertex to this MeshBuilder
	 */
	public MeshBuilder nextVertex(float x, float y, float z) {
		FloatBuffer dest = ensureSize(0, 3, vertexCursor);
		dest.put(x);
		dest.put(y);
		dest.put(z);

		writeAux(vertexCursor);

		vertexCursor++;
		return this;
	}

	/**
	 * Adds a vertex to this MeshBuilder
	 */
	public MeshBuilder nextVertex(Vec3 a) {
		return nextVertex((float) a.x, (float) a.y, (float) a.z);
	}

	/**
	 * Adds a vertex to this MeshBuilder
	 */
	public MeshBuilder nextVertex(Vec2 a) {
		return nextVertex((float) a.x, (float) a.y, 0);
	}


	/**
	 * Adds a element of type "line with adjacency" to this MeshBuilder. Line with adjancey is an OpenGL element that is a line segment with two other associated vertices (typically the previous
	 * and next vertices, but shaders are free to interpret these however they'd like to)
	 * <p>
	 * (vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on)
	 */
	public MeshBuilder nextElement(int v1, int v2, int v3, int v4) {
		IntBuffer dest = ensureElementSize(4, elementCursor);
		if (vertexCursor - 1 - v1 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v2 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle1 " + v2 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v3 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle2 " + v3 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v4 < 0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));
		dest.put(vertexCursor - 1 - v1);
		dest.put(vertexCursor - 1 - v2);
		dest.put(vertexCursor - 1 - v3);
		dest.put(vertexCursor - 1 - v4);

		elementCursor++;
		return this;
	}

	/**
	 * Adds two triangles that mark out the quad v1,v2,v3,v4. Specifically v1,v2,v3 and v1,v3,v4.
	 * <p>
	 * (vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on)
	 */
	public MeshBuilder nextElement_quad(int v1, int v2, int v3, int v4) {
		IntBuffer dest = ensureElementSize(3, elementCursor + 1);
		if (vertexCursor - 1 - v1 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v2 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle1 " + v2 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v3 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle2 " + v3 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v4 < 0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));

		dest.put(vertexCursor - 1 - v1);
		dest.put(vertexCursor - 1 - v2);
		dest.put(vertexCursor - 1 - v3);

		dest.put(vertexCursor - 1 - v1);
		dest.put(vertexCursor - 1 - v3);
		dest.put(vertexCursor - 1 - v4);

		elementCursor++;
		elementCursor++;
		return this;
	}

	/**
	 * Adds the triangle v1,v2,v3.
	 * <p>
	 * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 */
	public MeshBuilder nextElement(int v1, int v2, int v3) {


		IntBuffer dest = ensureElementSize(3, elementCursor);
		if (vertexCursor - 1 - v1 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v2 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle " + v2 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v3 < 0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));

		dest.put(vertexCursor - 1 - v1);
		dest.put(vertexCursor - 1 - v2);
		dest.put(vertexCursor - 1 - v3);

		elementCursor++;
		return this;
	}

	/**
	 * Adds the line segment v1, v2.
	 * <p>
	 * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 */
	public MeshBuilder nextElement(int v1, int v2) {
		IntBuffer dest = ensureElementSize(2, elementCursor);
		if (vertexCursor - 1 - v1 < 0)
			throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
		if (vertexCursor - 1 - v2 < 0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end " + v2 + " > " + (vertexCursor - 1));

		dest.put(vertexCursor - 1 - v1);
		dest.put(vertexCursor - 1 - v2);

		elementCursor++;
		return this;
	}

	/**
	 * Adds a string of line segments stretching from start all the way through to end.
	 * <p>
	 * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 */
	public MeshBuilder nextLine(int start, int end) {
		IntBuffer dest = ensureElementSize(2, elementCursor + Math.abs(start - end));
		if (start < 0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access an unwritten index with start " + start);
		if (end < 0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access an unwritten index with end " + start);
		if (end > start) {
			if (vertexCursor - 1 - (start + 1) < 0)
				throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start " + start + " > " + (vertexCursor - 1));
			if (vertexCursor - 1 - (end-1 + 1) < 0)
				throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end " + end + " > " + (vertexCursor - 1));

			for (int a = start; a < end; a++) {
				dest.put(vertexCursor - 1 - a);
				dest.put(vertexCursor - 1 - (a + 1));
				elementCursor++;
			}
		} else {
			if (vertexCursor - 1 - (start - 1) < 0)
				throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start " + start + " > " + (vertexCursor - 1));
			if (vertexCursor - 1 - (end+1 - 1) < 0)
				throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end " + end + " > " + (vertexCursor - 1));

			for (int a = start - 1; a > end; a--) {
				dest.put(vertexCursor - 1 - a);
				dest.put(vertexCursor - 1 - (a - 1));
				elementCursor++;
			}
		}

		return this;
	}

	/** Adds a string of line segments corresponding to the Vec3's in this List. (note, nothing will happen if line.size()<=1
	 *
	 */
	public MeshBuilder nextLine(List<Vec3> line)
	{
		if(line.size()<=1) return this;

		for(int i=0;i<line.size();i++)
		{
			nextVertex(line.get(i));
		}
		nextLine(0, line.size()-1);
		return this;
	}

	// todo, some way of doing properties and aux here

	/**
	 * Adds a set of contours described by a list of lists of Vec3. These lists are tesselated into triangles by the GLU_TESS_WINDING_NONZERO tesselation rule
	 */
	public MeshBuilder nextContourSet(List<List<Vec3>> contours) {
		MeshBuilder_tesselationSupport tess = getTessSupport();
		tess.begin();
		for (List<Vec3> c : contours) {
			tess.beginContour();
			for (int i = 1; i < c.size(); i++) {
				tess.line(c.get(i - 1), c.get(i), Collections.EMPTY_MAP, Collections.EMPTY_MAP);
			}
			tess.endContour();
		}
		tess.end();
		return this;
	}

	/**
	 * Adds a contours described by list of Vec3. This list is tesselated into triangles by the GLU_TESS_WINDING_NONZERO tesselation rule
	 */
	public MeshBuilder nextContour(List<Vec3> contours) {
		MeshBuilder_tesselationSupport tess = getTessSupport();
		tess.begin();
		tess.beginContour();
		for (int i = 1; i < contours.size(); i++) {
			tess.line(contours.get(i - 1), contours.get(i), Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		}
		tess.endContour();
		tess.end();
		return this;
	}

	/**
	 * Merges the geometry contained by "source" into this MeshBuilder. This is a relatively fast operation, consider caching sub-geometry inside individual MeshBuilders (and, of course,
	 * protecting this merge operation between two bookmarks).
	 */
	public MeshBuilder merge(MeshBuilder source) {
		if (target.elements == null && source.target.elements != null) throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");
		if (target.elements != null && source.target.elements == null) throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");
		if (target.elements.getDimension() != source.target.elements.getDimension()) throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");

		FloatBuffer v = ensureSize(0, 3, vertexCursor + source.vertexCursor);
		v.put(source.target.vertex(true));
		IntBuffer e = ensureElementSize(target.elements.getDimension(), elementCursor + source.elementCursor);
		IntBuffer e2 = source.target.elements(true);
		for (int i = 0; i < source.elementCursor * target.elements.getDimension(); i++)
			e.put(e2.get() + elementCursor + 1);


		Set<Integer> auxes = new LinkedHashSet<>();
		auxes.addAll(this.target.buffers()
					.keySet());
		auxes.addAll(source.target.buffers()
					  .keySet());
		auxes.remove(0);

		for (Integer ii : auxes) {
			ArrayBuffer a = this.target.buffers[ii];
			ArrayBuffer b = source.target.buffers[ii];
			if (a == null && b == null) continue;

			if (a != null && b != null && a.getDimension() != b.getDimension())
				throw new IllegalArgumentException(" dimension mismatch in merge " + a.getDimension() + "!=" + b.getDimension());
			if (b != null) {
				FloatBuffer left = ensureSize(ii, b.getDimension(), vertexCursor + source.vertexCursor);
				FloatBuffer right = b.floats();
				left.put(right); // fill with blank ?
			}
			if (b == null) ensureSize(ii, a.getDimension(), vertexCursor + source.vertexCursor); // fill with blank?
		}

		elementCursor += source.elementCursor;
		vertexCursor += source.vertexCursor;

		return this;
	}

	/**
	 * returns a utility class that's helpful for acccessing the tesselator
	 *
	 * @return
	 */
	public MeshBuilder_tesselationSupport getTessSupport() {

		return new MeshBuilder_tesselationSupport(this);
		//	return tessSupport == null ? (tessSupport = new MeshBuilder_tesselationSupport(this)) : tessSupport;
	}

	/**
	 * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 * <p>
	 * equivalent to nextLine(start, 0), e.g. nextLine from 'start' to here
	 */
	public MeshBuilder nextLine(int start) {
		return nextLine(start, 0);
	}

	/**
	 * Appends an FLine into this meshbuilder. Note this operation is sensitive to the kind of Mesh that this MeshBuilder is targeting. If you append a line that isn't filled into a MeshBuilder
	 * for a triangle mesh, nothing will happen.
	 * <p>
	 */
	public void append(FLine f) {
		int d = target.getElementDimension();
		if (d == 2) StandardFLineDrawing.dispatchLine(f, null, this, null, Optional.empty(), null);
		else if (d == 3) StandardFLineDrawing.dispatchLine(f, this, null, null, Optional.empty(), null);
		else if (d == 0) StandardFLineDrawing.dispatchLine(f, null, null, this, Optional.empty(), null);
		else if (d == 4) // lines_with_adj
			StandardFLineDrawing.dispatchLine(f, null, this, null, Optional.empty(), null);
		else throw new IllegalArgumentException(" can't draw into a MeshBuilder with dimension <" + d + ">");
	}

	private void writeAux(int vertexCursor) {

		Map<Integer, ArrayBuffer> b = target.buffers();
		Iterator<Map.Entry<Integer, ArrayBuffer>> m = b.entrySet()
							       .iterator();

		while (m.hasNext()) {
			Map.Entry<Integer, ArrayBuffer> n = m.next();
			if (n.getKey() == 0) continue;

			float[] z = aux.get(n.getKey());
			if (z == null) {
				z = new float[n.getValue()
					       .getDimension()];
			}
			ensureSize(n.getKey(), z.length, vertexCursor).put(z);
		}
	}

	private FloatBuffer ensureSize(int attribute, int dimension, int num) {
		ArrayBuffer a = target.buffer(attribute, dimension);
		if (a == null) {
			a = target.arrayBufferFactory.newArrayBuffer((int) (num * GROWTH + 1), GL15.GL_ARRAY_BUFFER, attribute, dimension, 0);
		}
		if (a.getSize() < (num + 1)) {
			a = a.replaceWithSize((int) ((num + 1) * GROWTH + 1));
			target.setBuffer(attribute, a);
		}
		FloatBuffer f = a.floats();
		f.clear();
		f.position(dimension * vertexCursor);
		return f;
	}

	private ArrayBuffer ensureExists(int attribute, int dimension, int num) {
		ArrayBuffer a = target.buffer(attribute, dimension);
		if (a == null) {
			a = target.arrayBufferFactory.newArrayBuffer((int) (num * GROWTH + 1), GL15.GL_ARRAY_BUFFER, attribute, dimension, 0);
		}
		if (a.getSize() < (num + 1)) {
			a = a.replaceWithSize((int) ((num + 1) * GROWTH + 1));
			target.setBuffer(attribute, a);
		}
		return a;
	}

	private IntBuffer ensureElementSize(int dimension, int num) {
		ArrayBuffer a = target.elements;
		if (a == null) {
			throw new IllegalArgumentException(" can't add elements to a " + target);
		}
		if (a.getSize() < (num + 1)) {
			a = a.replaceWithSize((int) ((num + 1) * GROWTH + 1));
			target.setElements(a);
		}
		IntBuffer f = a.ints();
		f.clear();
		f.position(dimension * elementCursor);
		return f;
	}

	protected Object computeHash() {
		// for now, at least
		HashSet<Integer> q = new HashSet<>(target.buffers()
							 .keySet());
		q.remove(0);
		return new Pair(shrinkage, q);
	}

	/**
	 * returns the mesh that this builder is building
	 */
	public BaseMesh getTarget() {
		return target;
	}

	public class Bookmark {
		public Object externalHash;
		protected int vertexCursor = MeshBuilder.this.vertexCursor;
		protected int elementCursor = MeshBuilder.this.elementCursor;
		protected long buildNumber = MeshBuilder.this.buildNumber;
		protected Object hash = computeHash();

		public int at() {
			return MeshBuilder.this.vertexCursor - vertexCursor;
		}

		public boolean stillValid() {
			return stillValid(null);
		}

		public boolean stillValid(Object externalHash) {

			if (buildNumber < MeshBuilder.this.buildNumber - 1) {
				Log.log("cache", () -> " buildNumber, build was out of date " + buildNumber + " " + MeshBuilder.this.buildNumber);
				cacheMisses_tooOld++;
				return false;
			}

			this.buildNumber = MeshBuilder.this.buildNumber;

			Log.log("cache",
				"evaluating cache " + MeshBuilder.this.vertexCursor + " / " + vertexCursor + "  " + MeshBuilder.this.elementCursor + " / " + elementCursor + " " + this.externalHash + "=" + externalHash + " " + computeHash() + "=" + this.hash);

			if (MeshBuilder.this.vertexCursor != vertexCursor || MeshBuilder.this.elementCursor != elementCursor) {
				Log.log("cache",
					() -> " CURSORS vertex cursor was :" + vertexCursor + " / " + MeshBuilder.this.vertexCursor + "  " + elementCursor + " / " + MeshBuilder.this.elementCursor);
				cacheMisses_cursor++;
				return false;
			}
			if (!Util.safeEq(this.externalHash, externalHash)) {
				Log.log("cache", () -> " externalHash " + this.externalHash + " " + externalHash);
				cacheMisses_externalHash++;
				return false;
			}

			Object h2 = computeHash();
			if (!h2.equals(hash)) {
				Log.log("cache", () -> " internalHash " + h2 + " " + hash);
				cacheMisses_internalHash++;
				return false;
			}
			cacheHits++;
			Log.log("cache", "succeeded");
			return true;
		}

		public Bookmark reset() {
			return reset(null);
		}

		public Bookmark reset(Object externalHash) {
			vertexCursor = MeshBuilder.this.vertexCursor;
			elementCursor = MeshBuilder.this.elementCursor;
			hash = computeHash();
			this.externalHash = externalHash;
			return this;
		}

		public String toString() {
			return "book@" + at() + " :" + vertexCursor + " " + elementCursor;
		}

		public Bookmark invalidate() {
			externalHash = "--invalidated--";
			return this;
		}

		public MeshBuilder getOuter() {
			return MeshBuilder.this;
		}
	}
}
