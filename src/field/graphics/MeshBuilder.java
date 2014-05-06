package field.graphics;

import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Pair;
import field.utility.Util;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by marc on 3/11/14.
 */
public class MeshBuilder implements MeshAcceptor, Bracketable {

	static public int cacheHits = 0;
	static public int cacheMisses_cursor = 0;
	static public int cacheMisses_externalHash = 0;
	static public int cacheMisses_internalHash = 0;

	public class Bookmark
	{
		protected int vertexCursor = MeshBuilder.this.vertexCursor;
		protected int elementCursor = MeshBuilder.this.elementCursor;

		protected Object hash = computeHash();

		public Object externalHash;

		public int at()
		{
			return MeshBuilder.this.vertexCursor-vertexCursor;
		}

		public boolean stillValid()
		{
			return stillValid(null);
		}

		public boolean stillValid(Object externalHash)
		{
			if (MeshBuilder.this.vertexCursor!= vertexCursor || MeshBuilder.this.elementCursor!=elementCursor)
			{
				cacheMisses_cursor++;
				return false;
			}
			if (!Util.safeEq(this.externalHash, externalHash))
			{
				cacheMisses_externalHash++;
				return false;
			}

			Object h2 = computeHash();
			if (!h2.equals(hash))
			{
				cacheMisses_internalHash++;
				return false;
			}
			cacheHits++;
			return true;
		}

		public Bookmark reset()
		{
			return reset(null);
		}

		public Bookmark reset(Object externalHash)
		{
			vertexCursor = MeshBuilder.this.vertexCursor;
			elementCursor = MeshBuilder.this.elementCursor;
			hash = computeHash();
			this.externalHash = externalHash;
			return this;
		}

		public String toString()
		{
			return "book@"+at()+" :"+vertexCursor+" "+elementCursor;
		}

		public Bookmark invalidate() {
			externalHash = "--invalidated--";
			return this;
		}

		public MeshBuilder getOuter() {
			return MeshBuilder.this;
		}
	}

	private BaseMesh target;
	static public float GROWTH = 1.5f;

	public MeshBuilder(BaseMesh target) {
		this.target = target;
	}

	int openCount = 0;

	// no shrinking yet, but when we do, we have to kill all bookmarks (at least all bookmarks above the trim line, which is impossible to compute right now)
	int shrinkage = 0;

	public MeshBuilder open() {
		if (openCount == 0) {
			doOpen();
		}
		openCount++;
		return this;
	}

	public void openAppend() {
		if (openCount == 0) {
			doOpenAppend();
		}
		openCount++;
	}
	public boolean isOpen() {
		return openCount>0;
	}

	int vertexCursor = 0;
	int elementCursor = 0;

	private void doOpen() {
		vertexCursor = 0;
		elementCursor = 0;
	}

	private void doOpenAppend() {
		vertexCursor = target.getVertexLimit();
		elementCursor = target.getElementLimit();
	}

	public void close() {
		openCount--;
		if (openCount == 0) {
			doClose();
		}
		if (openCount<0)
		{
			throw new IllegalArgumentException("more closes than opens?");
		}
	}

	Map<Integer, float[]> aux = new HashMap<Integer, float[]>();

	private void doClose() {
		target.setVertexLimit(vertexCursor);
		target.setElementLimit(elementCursor);
	}

	public MeshBuilder aux(Map<Integer, Object> add) {
		Iterator<Map.Entry<Integer, Object>> m = add.entrySet().iterator();
		while (m.hasNext()) {
			Map.Entry<Integer, Object> n = m.next();
			float[] fa = toFloatArray(n.getValue());
			if (fa != null) aux.put(n.getKey(), fa);
		}
		return this;
	}

	public MeshBuilder aux(int attribute, float[] add) {
		aux.put(attribute, add);
		ensureExists(attribute, add.length, vertexCursor);
		return this;
	}


	public MeshBuilder aux(int attribute, float add) {
		aux.put(attribute, new float[]{add});
		ensureExists(attribute, 1, vertexCursor);
		return this;
	}

	public MeshBuilder aux(int attribute, float x, float y) {
		aux.put(attribute, new float[]{x,y});
		ensureExists(attribute, 2, vertexCursor);
		return this;
	}

	public MeshBuilder aux(int attribute, float x, float y, float z) {
		aux.put(attribute, new float[]{x,y, z});
		ensureExists(attribute, 3, vertexCursor);
		return this;
	}
	public MeshBuilder aux(int attribute, float x, float y, float z, float w) {
		aux.put(attribute, new float[]{x,y, z,w });
		ensureExists(attribute, 4, vertexCursor);
		return this;
	}
	public MeshBuilder aux(int attribute, Vec2 x) {
		aux.put(attribute, new float[]{x.x,x.y});
		ensureExists(attribute, 2, vertexCursor);
		return this;
	}
	public MeshBuilder aux(int attribute, Vec3 x) {
		aux.put(attribute, new float[]{x.x,x.y,x.z});
		ensureExists(attribute, 3, vertexCursor);
		return this;
	}

	public MeshBuilder aux(int attribute, Vec4 x) {
		aux.put(attribute, new float[]{x.x,x.y,x.z,x.w});
		ensureExists(attribute, 4, vertexCursor);
		return this;
	}

	private float[] toFloatArray(Object value) {
		if (value instanceof float[]) return (float[]) value;

		throw new IllegalArgumentException("cannot interpret " + value + " as float array");
	}

	public Bookmark bookmark()
	{
		return new Bookmark();
	}

	public boolean skipTo(Bookmark from, Bookmark to)
	{
		if (!from.stillValid() || from.getOuter()!=this) return false;
		vertexCursor = to.vertexCursor;
		elementCursor = to.elementCursor;
		return true;
	}

	public boolean skipTo(Bookmark from, Bookmark to, Object externalHash, Consumer<MeshBuilder> updator)
	{
		if (!from.stillValid(externalHash) || from.getOuter()!=this)
		{
			from.reset(externalHash);
			updator.accept(this);
			to.reset(externalHash);

			return false;
		}
		vertexCursor = to.vertexCursor;
		elementCursor = to.elementCursor;
		return true;
	}

	public boolean skipTo(Bookmark from, Bookmark to, Object externalHash, Runnable updator)
	{
		return skipTo(from, to, externalHash, x -> updator.run());
	}

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
	 vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 */
	public MeshBuilder nextElement(int v1, int v2, int v3, int v4)
	{
		IntBuffer dest = ensureElementSize(4, elementCursor);
		if (vertexCursor-1-v1<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start "+v1+" > "+(vertexCursor-1));
		if (vertexCursor-1-v2<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle1 "+v2+" > "+(vertexCursor-1));
		if (vertexCursor-1-v3<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle2 "+v3+" > "+(vertexCursor-1));
		if (vertexCursor-1-v4<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end "+v3+" > "+(vertexCursor-1));
		dest.put(vertexCursor-1-v1);
		dest.put(vertexCursor-1-v2);
		dest.put(vertexCursor-1-v3);
		dest.put(vertexCursor-1-v4);

		elementCursor++;
		return this;
	}

	/**
	 vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 */
	public MeshBuilder nextElement_quad(int v1, int v2, int v3, int v4)
	{
		IntBuffer dest = ensureElementSize(3, elementCursor+1);
		if (vertexCursor-1-v1<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start "+v1+" > "+(vertexCursor-1));
		if (vertexCursor-1-v2<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle1 "+v2+" > "+(vertexCursor-1));
		if (vertexCursor-1-v3<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle2 "+v3+" > "+(vertexCursor-1));
		if (vertexCursor-1-v4<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end "+v3+" > "+(vertexCursor-1));

		dest.put(vertexCursor-1-v1);
		dest.put(vertexCursor-1-v2);
		dest.put(vertexCursor-1-v3);

		dest.put(vertexCursor-1-v1);
		dest.put(vertexCursor-1-v3);
		dest.put(vertexCursor-1-v4);

		elementCursor++;
		elementCursor++;
		return this;
	}

	/**
	 vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 */
	public MeshBuilder nextElement(int v1, int v2, int v3)
	{
		IntBuffer dest = ensureElementSize(3, elementCursor);
		if (vertexCursor-1-v1<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start "+v1+" > "+(vertexCursor-1));
		if (vertexCursor-1-v2<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with middle "+v2+" > "+(vertexCursor-1));
		if (vertexCursor-1-v3<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end "+v3+" > "+(vertexCursor-1));

		dest.put(vertexCursor-1-v1);
		dest.put(vertexCursor-1-v2);
		dest.put(vertexCursor-1-v3);

		elementCursor++;
		return this;
	}

	/**
	 vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 */
	public MeshBuilder nextElement(int v1, int v2)
	{
		IntBuffer dest = ensureElementSize(2, elementCursor);
		if (vertexCursor-1-v1<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start "+v1+" > "+(vertexCursor-1));
		if (vertexCursor-1-v2<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end "+v2+" > "+(vertexCursor-1));

		dest.put(vertexCursor-1-v1);
		dest.put(vertexCursor-1-v2);

		elementCursor++;
		return this;
	}

	/**
	 vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on
	 */
	public MeshBuilder nextLine(int start, int end)
	{
		IntBuffer dest = ensureElementSize(2, elementCursor+Math.abs(start - end));
		if (start<0)throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access an unwritten index with start "+start);
		if (end<0)throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access an unwritten index with end "+start);
		if (end>start)
		{
			if (vertexCursor-1-(start+1)<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start "+start+" > "+(vertexCursor-1));
			if (vertexCursor-1-(end+1)<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end "+start+" > "+(vertexCursor-1));

			for(int a=start;a<end;a++)
			{
				dest.put(vertexCursor-1-a);
				dest.put(vertexCursor-1-(a+1));
				elementCursor++;
			}
		}
		else
		{
			if (vertexCursor-1-(start-1)<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with start "+start+" > "+(vertexCursor-1));
			if (vertexCursor-1-(end-1)<0) throw new IllegalArgumentException(" can't write line into vertexbuffer, trying to access a negative index with end "+start+" > "+(vertexCursor-1));

			for(int a=start;a>end;a--)
			{
				dest.put(vertexCursor-1-a);
				dest.put(vertexCursor-1-(a-1));
				elementCursor++;
			}
		}

		return this;
	}

	// todo, some way of doing properties and aux here
	public MeshBuilder nextContourSet(List<List<Vec3>> contours)
	{
		MeshBuilder_tesselationSupport tess = getTessSupport();
		tess.begin();
		for(List<Vec3> c : contours)
		{
			tess.beginContour();
			for(int i=1;i<c.size();i++)
			{
				tess.line(c.get(i-1), c.get(i), Collections.EMPTY_MAP, Collections.EMPTY_MAP);
			}
			tess.endContour();
		}
		tess.end();
		return this;
	}

	public MeshBuilder nextContour(List<Vec3> contours)
	{
		MeshBuilder_tesselationSupport tess = getTessSupport();
		tess.begin();
		tess.beginContour();
		for(int i=1;i<contours.size();i++)
		{
			tess.line(contours.get(i-1), contours.get(i), Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		}
		tess.endContour();
		tess.end();
		return this;
	}

	public MeshBuilder merge(MeshBuilder source)
	{
		if (target.elements==null && source.target.elements!=null) throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");
		if (target.elements!=null && source.target.elements==null) throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");
		if (target.elements.getDimension()!=source.target.elements.getDimension()) throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");

		FloatBuffer v = ensureSize(0, 3, vertexCursor+source.vertexCursor);
		v.put(source.target.vertex(true));
		IntBuffer e = ensureElementSize(target.elements.getDimension(), elementCursor+source.elementCursor);
		IntBuffer e2 = source.target.elements(true);
		for(int i=0;i<source.elementCursor*target.elements.getDimension();i++)
			e.put(e2.get()+elementCursor+1);


		Set<Integer> auxes = new LinkedHashSet<>();
		auxes.addAll(this.target.buffers().keySet());
		auxes.addAll(source.target.buffers().keySet());
		auxes.remove(0);

		for(Integer ii : auxes)
		{
			ArrayBuffer a = this.target.buffers[ii];
			ArrayBuffer b = source.target.buffers[ii];
			if (a==null && b==null) continue;

			if (a!=null && b!=null && a.getDimension()!=b.getDimension()) throw new IllegalArgumentException(" dimension mismatch in merge "+a.getDimension()+"!="+b.getDimension());
			if (b!=null)
			{
				FloatBuffer left = ensureSize(ii, b.getDimension(), vertexCursor+source.vertexCursor);
				FloatBuffer right = b.floats();
				left.put(right); // fill with blank ?
			}
			if (b==null)
				ensureSize(ii, a.getDimension(), vertexCursor+source.vertexCursor); // fill with blank?
		}

		elementCursor+=source.elementCursor;
		vertexCursor+=source.vertexCursor;

		return this;
	}

	MeshBuilder_tesselationSupport tessSupport = null;
	public MeshBuilder_tesselationSupport getTessSupport()
	{
		return tessSupport == null ? (tessSupport = new MeshBuilder_tesselationSupport(this)) : tessSupport;
//		return new MeshBuilder_tesselationSupport(this);
	}

	/**
	 vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to nextVertex, 1 is the vertex before that and so on

	 equivalent to nextLine(start, 0), e.g. nextLine from 'start' to here
	 */
	public MeshBuilder nextLine(int start)
	{
		return nextLine(start, 0);
	}

	private void writeAux(int vertexCursor) {

		Map<Integer, ArrayBuffer> b = target.buffers();
		Iterator<Map.Entry<Integer, ArrayBuffer>> m = b.entrySet().iterator();

		while (m.hasNext()) {
			Map.Entry<Integer, ArrayBuffer> n = m.next();
			if (n.getKey() == 0) continue;

			float[] z = aux.get(n.getKey());
			if (z == null) {
				z = new float[n.getValue().getDimension()];
//				System.out.println(" missing aux data ?"+n.getKey());
			}
//			System.out.println(" putting aux "+n.getKey()+" -> "+z.length);
			ensureSize(n.getKey(), z.length, vertexCursor).put(z);
		}
	}

	private FloatBuffer ensureSize(int attribute, int dimension, int num) {
		ArrayBuffer a = target.buffer(attribute, dimension);
		if (a == null) {
			a = target.arrayBufferFactory.newArrayBuffer((int) (num * GROWTH + 1), GL15.GL_ARRAY_BUFFER, attribute, dimension, 0);
		}
		if (a.getSize() < (num + 1)) {
			a = a.replaceWithSize((int) ((num+1) * GROWTH + 1));
			target.setBuffer(attribute, a);
		}
		FloatBuffer f = a.floats();
		f.clear();
		f.position(dimension * vertexCursor);
		return f;
	}

	private void ensureExists(int attribute, int dimension, int num) {
		ArrayBuffer a = target.buffer(attribute, dimension);
		if (a == null) {
			a = target.arrayBufferFactory.newArrayBuffer((int) (num * GROWTH + 1), GL15.GL_ARRAY_BUFFER, attribute, dimension, 0);
		}
		if (a.getSize() < (num + 1)) {
			a = a.replaceWithSize((int) ((num+1) * GROWTH + 1));
			target.setBuffer(attribute, a);
		}
	}

	private IntBuffer ensureElementSize(int dimension, int num) {
		ArrayBuffer a = target.elements;
		if (a == null) {
			throw new IllegalArgumentException(" can't add elements to a "+target);
		}
		if (a.getSize() < (num + 1)) {
			a = a.replaceWithSize((int) ((num+1) * GROWTH + 1));
			target.setElements(a);
		}
		IntBuffer f = a.ints();
		f.clear();
		f.position(dimension * elementCursor);
		return f;
	}

	protected Object computeHash()
	{
		// for now, at least
		HashSet<Integer> q = new HashSet<>(target.buffers().keySet());
		q.remove(0);
		return new Pair(shrinkage, q);
	}

}
