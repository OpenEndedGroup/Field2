package field.graphics;

import field.linalg.Mat4;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Conversions;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Util;
import fieldlinker.AsMap;
import fieldlinker.AsMap_callable;
import fieldnashorn.annotations.HiddenInAutocomplete;
import org.lwjgl.opengl.GL15;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.BufferUnderflowException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Dynamically create geometry piece by piece in a way that is cacheable and as friendly to contemporary OpenGL as possible.
 */
public class MeshBuilder implements MeshAcceptor, Bracketable, Scene.ContainsPerform, AsMap_callable, AsMap {

    static public int cacheHits = 0;
    static public int cacheMisses_cursor = 0;
    static public int cacheMisses_externalHash = 0;
    static public int cacheMisses_internalHash = 0;
    static public int cacheMisses_tooOld = 0;
    static public float GROWTH = 1.5f;
    public Mat4 localTransform = new Mat4().identity();
    protected Set<String> knownNonProperties;
    volatile int openCount = 0;
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

        target.asMap_set("localTransform", (Supplier<Mat4>) () -> {
            Mat4 mm = new Mat4();
            localTransform.transpose(mm);
            return mm;
        });

    }

    /**
     * opens a MeshBuilder to get it ready for accepting geometry. The first call to open() will clear out all the geometry that was previously in this container. Nested calls to open() without a
     * corresponding call to close() will append. Make sure that there's a matching close() for every open() (i.e. learn how to use try-with-resources and try-finally blocks)
     */
    public MeshBuilder open() {
        if (openCount == 0) {
            doOpen();
            target.attach("__forceClose__" + System.identityHashCode(this), new Scene.Perform() {
                @Override
                public boolean perform(int pass) {
                    if (openCount > 0) {
                        openCount = 0;
                        doClose();
                    }
                    return false;
                }

                @Override
                public int[] getPasses() {
                    return new int[]{-1};
                }
            });
        }
        openCount++;
        return this;
    }

    @Override
    public boolean asMap_isProperty(String p) {
        if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();

        return !knownNonProperties.contains(p) && target.asMap_isProperty(p);
    }

    protected Set<String> computeKnownNonProperties() {
        Set<String> r = new LinkedHashSet<>();
        Method[] m = this.getClass()
                .getMethods();
        for (Method mm : m)
            r.add(mm.getName());
        Field[] f = this.getClass()
                .getFields();
        for (Field ff : f)
            if (!Modifier.isStatic(ff.getModifiers()))
                r.add(ff.getName());

        r.remove("children");
        r.remove("parents");

        return r;
    }

    /**
     * Call this function, wrapped between an open() and close() pair
     */
    public MeshBuilder asMap_call(Object r, Object o1) {

        Object rr = Conversions.convert(o1, Runnable.class);

        if (rr instanceof Runnable) {

            open();
            try {
                ((Runnable) rr).run();
            } finally {
                close();
            }
        } else
            throw new ClassCastException(" can't call " + o1);

        return this;
    }

    @Override
    public Object asMap_get(String s) {
        return target.asMap_get(s);
    }

    @Override
    public Object asMap_set(String s, Object o) {
        return target.asMap_set(s, o);
    }

    @Override
    public Object asMap_new(Object o) {
        return target.asMap_new(o);
    }

    @Override
    public Object asMap_new(Object o, Object o1) {
        return target.asMap_new(o, o1);
    }

    @Override
    public Object asMap_getElement(int i) {
        return target.asMap_getElement(i);
    }

    @Override
    public Object asMap_setElement(int i, Object o) {
        return target.asMap_setElement(i, o);
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

    public int getVertexCursor() {
        return vertexCursor;
    }

    public int getElementCursor() {
        return elementCursor;
    }

    public void setElementCursor(int n) {
        elementCursor = n;
    }

    public void setVertexColor(int n) {
        vertexCursor = n;
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
     * <p>
     * Will throw an exception unless you give it a clean FloatBuffer (not-sliced, allocateDirect etc.)
     */
    public MeshBuilder bindAux(int attribute, int dimension, FloatBuffer storage) throws NoSuchFieldException, IllegalAccessException {
        ArrayBuffer a = ensureExists(attribute, dimension, vertexCursor);

        if (a instanceof SimpleArrayBuffer) {
            ((SimpleArrayBuffer) a).setCustomStorage(storage);
        } else {
            throw new IllegalArgumentException(" can't bind to arraybuffer of class " + (a.getClass()));
        }

        return this;
    }

    private float[] toFloatArray(Object value) {
        if (value instanceof float[]) return (float[]) value;

        throw new IllegalArgumentException("cannot interpret " + value + " as float array");
    }

    /**
     * Creates a new cache bookmark at this point in the construction of the geometry. You can use a pair of bookmarks at some future recreation of this mesh to ask if a group of calls to
     * v, e, and aux can be skipped completely
     */
    public Bookmark bookmark() {
        return new Bookmark();
    }

    /**
     * Creates a new cache bookmark at this point in the construction of the geometry. You can use a pair of bookmarks at some future recreation of this mesh to ask if a group of calls to
     * v, e, and aux can be skipped completely.
     *
     * @param absolute, point to a particular vertex
     */
    public Bookmark bookmark(int absolute) {
        return new Bookmark(absolute);
    }

    /**
     * Can all the calls to v, e and aux, between these two bookmarks be skipped? Returns true if we have skipped forward, false otherwise.
     */
    public boolean skipTo(Bookmark from, Bookmark to) {
        if (!from.stillValid() || from.getOuter() != this) return false;
        vertexCursor = to.vertexCursor;
        elementCursor = to.elementCursor;
        return true;
    }

    /**
     * Can all the calls to v, e and aux, between these two bookmarks be skipped? Returns true if we have skipped forward, false otherwise. Bookmarks can be unskippable because
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
     * Can all the calls to v, e and aux, between these two bookmarks be skipped? Returns true if we have skipped forward, false otherwise.
     * <p>
     * Equivalent to skipTo(from, to, externalHash, (Consumer<MeshBuilder> x -> updator.run())
     */
    public boolean skipTo(Bookmark from, Bookmark to, Object externalHash, Runnable updator) {
        return skipTo(from, to, externalHash, x -> updator.run());
    }


    /**
     * Adds a vertex to this MeshBuilder
     */
    public MeshBuilder v(float x, float y, float z) {
        FloatBuffer dest = ensureSize(0, 3, vertexCursor);
        dest.put(x);
        dest.put(y);
        dest.put(z);

        writeAux(vertexCursor);

        vertexCursor++;
        return this;
    }

    public MeshBuilder raw_v(float[] f) {
        FloatBuffer dest = ensureSize(0, 3, vertexCursor + f.length / 3);
        dest.put(f);
        vertexCursor += f.length / 3;
        return this;
    }

    public MeshBuilder raw_e_line(int[] f) {
        IntBuffer dest = ensureElementSize(2, elementCursor + f.length / 2);
        dest.put(f);
        elementCursor += f.length / 2;
        return this;
    }

    /**
     * Adds a vertex to this MeshBuilder
     */
    public MeshBuilder v(Vec3 a) {
        return v((float) a.x, (float) a.y, (float) a.z);
    }

    /**
     * Adds a vertex to this MeshBuilder
     */
    public MeshBuilder v(Vec2 a) {
        return v((float) a.x, (float) a.y, 0);
    }


    /**
     * Adds a element of type "line with adjacency" to this MeshBuilder. Line with adjancey is an OpenGL element that is a line segment with two other associated vertices (typically the previous
     * and next vertices, but shaders are free to interpret these however they'd like to)
     * <p>
     * (vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to v, 1 is the vertex before that and so on)
     */
    public MeshBuilder e(int v1, int v2, int v3, int v4) {
        IntBuffer dest = ensureElementSize(4, elementCursor);
        if (vertexCursor - 1 - v1 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v2 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with middle1 " + v2 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v3 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with middle2 " + v3 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v4 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));
        dest.put(vertexCursor - 1 - v1);
        dest.put(vertexCursor - 1 - v2);
        dest.put(vertexCursor - 1 - v3);
        dest.put(vertexCursor - 1 - v4);

        elementCursor++;
        return this;
    }

    /**
     * adds a "triangle with adjacency", that is the triangle v1,v2,v3 and the additional verts corresponding to the edges v12, v23, and v31
     */
    public MeshBuilder e(int v1, int v2, int v3, int v12, int v23, int v31) {
        IntBuffer dest = ensureElementSize(6, elementCursor);
        if (vertexCursor - 1 - v1 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v2 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with middle1 " + v2 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v3 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with middle2 " + v3 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v12 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v23 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v31 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));
        dest.put(vertexCursor - 1 - v1);
        dest.put(vertexCursor - 1 - v12);

        dest.put(vertexCursor - 1 - v2);
        dest.put(vertexCursor - 1 - v23);

        dest.put(vertexCursor - 1 - v3);
        dest.put(vertexCursor - 1 - v31);

        elementCursor++;
        return this;
    }

    /**
     * Adds two triangles that mark out the quad v1,v2,v3,v4. Specifically v1,v2,v3 and v1,v3,v4.
     * <p>
     * (vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to v, 1 is the vertex before that and so on)
     */
    public MeshBuilder e_quad(int v1, int v2, int v3, int v4) {
        IntBuffer dest = ensureElementSize(3, elementCursor + 1);
        if (vertexCursor - 1 - v1 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v2 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with middle1 " + v2 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v3 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with middle2 " + v3 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v4 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));

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
     * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to v, 1 is the vertex before that and so on
     */
    public MeshBuilder e(int v1, int v2, int v3) {

        IntBuffer dest = ensureElementSize(3, elementCursor);
        if (vertexCursor - 1 - v1 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v2 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with middle " + v2 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v3 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with end " + v3 + " > " + (vertexCursor - 1));

        dest.put(vertexCursor - 1 - v1);
        dest.put(vertexCursor - 1 - v2);
        dest.put(vertexCursor - 1 - v3);

        elementCursor++;
        return this;
    }

    /**
     * Adds the line segment v1, v2.
     * <p>
     * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to v, 1 is the vertex before that and so on
     */
    public MeshBuilder e(int v1, int v2) {
        IntBuffer dest = ensureElementSize(2, elementCursor);
        if (vertexCursor - 1 - v1 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with start " + v1 + " > " + (vertexCursor - 1));
        if (vertexCursor - 1 - v2 < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access a negative index with end " + v2 + " > " + (vertexCursor - 1));

        dest.put(vertexCursor - 1 - v1);
        dest.put(vertexCursor - 1 - v2);

        elementCursor++;
        return this;
    }

    /**
     * Adds a string of line segments stretching from start all the way through to end.
     * <p>
     * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to v, 1 is the vertex before that and so on
     */
    public MeshBuilder line(int start, int end) {
        IntBuffer dest = ensureElementSize(2, elementCursor + Math.abs(start - end));
        if (start < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access an unwritten index with start " + start);
        if (end < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access an unwritten index with end " + start);
        if (end > start) {
            if (vertexCursor - 1 - (start + 1) < 0)
                throw new IllegalArgumentException(
                        " can't write line into vertexbuffer, trying to access a negative index with start " + start + " > " + (vertexCursor - 1));
            if (vertexCursor - 1 - (end - 1 + 1) < 0)
                throw new IllegalArgumentException(
                        " can't write line into vertexbuffer, trying to access a negative index with end " + end + " > " + (vertexCursor - 1));

            for (int a = start; a < end; a++) {
                dest.put(vertexCursor - 1 - a);
                dest.put(vertexCursor - 1 - (a + 1));
                elementCursor++;
            }
        } else {
            if (vertexCursor - 1 - (start - 1) < 0)
                throw new IllegalArgumentException(
                        " can't write line into vertexbuffer, trying to access a negative index with start " + start + " > " + (vertexCursor - 1));
            if (vertexCursor - 1 - (end + 1 - 1) < 0)
                throw new IllegalArgumentException(
                        " can't write line into vertexbuffer, trying to access a negative index with end " + end + " > " + (vertexCursor - 1));

            for (int a = start - 1; a > end; a--) {
                dest.put(vertexCursor - 1 - a);
                dest.put(vertexCursor - 1 - (a - 1));
                elementCursor++;
            }
        }

        return this;
    }

    /**
     * Adds a string of line segments stretching from start all the way through to end.
     * <p>
     * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to v, 1 is the vertex before that and so on
     */
    public MeshBuilder lineAdj(int start, int end) {
        IntBuffer dest = ensureElementSize(4, elementCursor + Math.abs(start - end));
        if (start < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access an unwritten index with start " + start);
        if (end < 0)
            throw new IllegalArgumentException(
                    " can't write line into vertexbuffer, trying to access an unwritten index with end " + start);
        if (end > start) {
            if (vertexCursor - 1 - (start + 3) < 0)
                throw new IllegalArgumentException(
                        " can't write line into vertexbuffer, trying to access a negative index with start " + start + " > " + (vertexCursor - 1));
            if (vertexCursor - 1 - (end - 1 + 3) < 0)
                throw new IllegalArgumentException(
                        " can't write line into vertexbuffer, trying to access a negative index with end " + end + " > " + (vertexCursor - 1));

            for (int a = start; a < end; a++) {
                dest.put(vertexCursor - 1 - a);
                dest.put(vertexCursor - 1 - (a + 1));
                dest.put(vertexCursor - 1 - (a + 2));
                dest.put(vertexCursor - 1 - (a + 3));
                elementCursor++;
            }
        } else {
            if (vertexCursor - 1 - (start - 3) < 0)
                throw new IllegalArgumentException(
                        " can't write line into vertexbuffer, trying to access a negative index with start " + start + " > " + (vertexCursor - 1));
            if (vertexCursor - 1 - (end + 1 - 3) < 0)
                throw new IllegalArgumentException(
                        " can't write line into vertexbuffer, trying to access a negative index with end " + end + " > " + (vertexCursor - 1));

            for (int a = start - 1; a > end; a--) {
                dest.put(vertexCursor - 1 - a);
                dest.put(vertexCursor - 1 - (a - 1));
                dest.put(vertexCursor - 1 - (a - 2));
                dest.put(vertexCursor - 1 - (a - 3));
                elementCursor++;
            }
        }

        return this;
    }

    /**
     * Adds a string of line segments corresponding to the Vec3's in this List. (note, nothing will happen if line.size()<=1
     */
    public MeshBuilder line(List<Vec3> line) {
        if (line.size() <= 1) return this;

        for (int i = 0; i < line.size(); i++) {
            v(line.get(i));
        }
        line(0, line.size() - 1);
        return this;
    }

    /**
     * Adds a contour, via the tesselator, to a triangle mesh. Like the rest of the public api, toTesselate refers to vertex indices by starting at '0' (the most recently added via v(...)) and the '1' (the one added before that) etc..
     */
    public MeshBuilder e(List<Integer> toTesselate) {


        ArrayList<Integer> abs = new ArrayList<Integer>(toTesselate.size());
        for (int i = 0; i < toTesselate.size(); i++)
            abs.add(-toTesselate.get(i) + vertexCursor);

        MeshBuilder_tesselationSupport tess = getTessSupport();
        tess.begin();
        tess.beginContour();
        for (int i = 1; i < toTesselate.size(); i++) {
            tess.line(abs.get(i - 1), abs.get(i), i == 1 ? auxMapFor(abs.get(i - 1)) : null, auxMapFor(abs.get(i)));
        }

        tess.endContour();
        tess.end();

        return this;
    }

    private Map<Integer, float[]> auxMapFor(int vertex) {

        Map<Integer, ArrayBuffer> b = target.buffers();
        Iterator<Map.Entry<Integer, ArrayBuffer>> m = b.entrySet()
                .iterator();

        Map<Integer, float[]> q = new LinkedHashMap<>();
        while (m.hasNext()) {
            Map.Entry<Integer, ArrayBuffer> n = m.next();
            if (n.getKey() == 0) continue;

            float[] z = aux.get(n.getKey());
            if (z == null) {
                z = new float[n.getValue()
                        .getDimension()];
            }

            FloatBuffer fs = n.getValue()
                    .floats(true);
            fs.position(z.length * (vertex - 1));
            fs.get(z);
            q.put(n.getKey(), z);
        }

        return q;
    }


    /**
     * Adds a set of contours described by a list of lists of Vec3. These lists are tesselated into triangles by the GLU_TESS_WINDING_NONZERO tesselation rule
     */
    public MeshBuilder contours(List<List<Vec3>> contours) {
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
    public MeshBuilder contour(List<Vec3> contours) {
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

    public MeshBuilder addMesh(List<Vec3> vertex, List<int[]> faces) {
        int here = vertexCursor;
        vertex.forEach(x -> v(x));

        if (target.elements == null) {
            return this;
        }

        if (target.elements.getDimension() == 2) {
            int origin = vertexCursor - here;

            for (int[] f : faces) {
                for (int i = 0; i < f.length; i++) {
                    int a = f[i];
                    int b = f[(i + 1) % f.length];

                    b = origin - b;
                    a = origin - a;

                    e(a, b);
                }
            }
            return this;
        }

        if (target.elements.getDimension() == 4) {
            int origin = vertexCursor - here;

            for (int[] f : faces) {
                for (int i = 0; i < f.length; i++) {
                    int a0 = f[(i - 1 + f.length) % f.length];
                    int a = f[i];
                    int b = f[(i + 1) % f.length];
                    int b0 = f[(i + 2) % f.length];

                    b0 = origin - b0;
                    a0 = origin - a0;
                    b = origin - b;
                    a = origin - a;

                    e(a0, a, b, b0);
                }
            }
            return this;
        }

        if (target.elements.getDimension() == 3) {

            int origin = vertexCursor - here;
            for (int[] f : faces) {

                if (f.length == 3) {
                    e(origin - f[0], origin - f[1], origin - f[2]);
                    continue;
                }
                if (f.length == 4) {
                    e(origin - f[0], origin - f[1], origin - f[2]);
                    e(origin - f[0], origin - f[2], origin - f[3]);
                    continue;
                }
                if (f.length < 3)
                    continue;

                List<Integer> ee = new ArrayList<>(f.length);
                for (int ff : f)
                    ee.add(origin - ff);
                e(ee);
            }
            return this;
        }

        throw new IllegalArgumentException(
                " addMesh not supported for element dimension " + target.elements.getDimension());

    }

    /**
     * Merges the geometry contained by "source" into this MeshBuilder. This is a relatively fast operation, consider caching sub-geometry inside individual MeshBuilders (and, of course,
     * protecting this merge operation between two bookmarks).
     */
    public MeshBuilder merge(MeshBuilder source) {
        if (target.elements == null && source.target.elements != null)
            throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");
        if (target.elements != null && source.target.elements == null)
            throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");
        if (target.elements == null && source.target.elements == null) {
        } else if (target.elements.getDimension() != source.target.elements.getDimension())
            throw new IllegalArgumentException(" cannot merge, mismatch in element dimension ");

        // thread shenanigans
        int sc = source.vertexCursor;
        FloatBuffer vv = source.target.vertex(true).asReadOnlyBuffer();
        vv.clear();
        vv.limit(sc * 3);
        FloatBuffer v = ensureSize(0, 3, vertexCursor + sc);
        v.put(vv);
        if (target.elements==null)
        {}
        else {
            IntBuffer e = ensureElementSize(target.elements.getDimension(), elementCursor + source.elementCursor);
            IntBuffer e2 = source.target.elements(true);
            for (int i = 0; i < source.elementCursor * target.elements.getDimension(); i++)
                e.put(e2.get() + vertexCursor);
        }


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
                throw new IllegalArgumentException(
                        " dimension mismatch in merge " + a.getDimension() + "!=" + b.getDimension());
            if (b != null) {
                FloatBuffer left = ensureSize(ii, b.getDimension(), vertexCursor + source.vertexCursor);
                FloatBuffer right = b.floats();
                try {
                    right.limit(sc * b.getDimension());
                    left.put(right); // fill with blank ?
                }
                catch(BufferUnderflowException e)
                {
                    e.printStackTrace();
                }
            }
            if (b == null)
                ensureSize(ii, a.getDimension(), vertexCursor + source.vertexCursor); // fill with blank?
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
     * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to v, 1 is the vertex before that and so on
     * <p>
     * equivalent to line(start, 0), e.g. line from 'start' to here
     */
    public MeshBuilder line(int start) {
        return line(start, 0);
    }

    /**
     * vertex numbers are backwards from the current vertex, so vertex 0 is the most recent call to v, 1 is the vertex before that and so on
     * <p>
     * equivalent to lineAdj(start, 0), e.g. line from 'start' to here
     */
    public MeshBuilder lineAdj(int start) {
        return lineAdj(start, 0);
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

    public FloatBuffer ensureSize(int attribute, int dimension, int num) {
        ArrayBuffer a = target.buffer(attribute, dimension);
        if (a == null) {
            a = target.arrayBufferFactory.newArrayBuffer((int) (num * GROWTH + 1), GL15.GL_ARRAY_BUFFER, attribute,
                    dimension, 0);
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

    public ArrayBuffer ensureExists(int attribute, int dimension, int num) {
        ArrayBuffer a = target.buffer(attribute, dimension);
        if (a == null) {
            a = target.arrayBufferFactory.newArrayBuffer((int) (num * GROWTH + 1), GL15.GL_ARRAY_BUFFER, attribute,
                    dimension, 0);
        }
        if (a.getSize() < (num + 1)) {
            a = a.replaceWithSize((int) ((num + 1) * GROWTH + 1));
            target.setBuffer(attribute, a);
        }
        return a;
    }

    public IntBuffer ensureElementSize(int dimension, int num) {
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

    @Override
    @HiddenInAutocomplete
    public Scene.Perform getPerform() {
        return getTarget();
    }

    @Override
    public boolean asMap_delete(Object o) {
        return false;
    }

    public MeshBuilder transform(Function<Vec3, Object> transformer) {
        FloatBuffer vv = target.vertex(false);
        for (int i = 0; i < vertexCursor; i++) {
            float x = vv.get(3 * i + 0);
            float y = vv.get(3 * i + 1);
            float z = vv.get(3 * i + 2);

            Vec3 v = new Vec3(x, y, z);
            Object ret = transformer.apply(v);
            if (ret instanceof Vec3) {
                vv.put(3 * i + 0, (float) ((Vec3) ret).x);
                vv.put(3 * i + 1, (float) ((Vec3) ret).y);
                vv.put(3 * i + 2, (float) ((Vec3) ret).z);
            } else {
                vv.put(3 * i + 0, (float) v.x);
                vv.put(3 * i + 1, (float) v.y);
                vv.put(3 * i + 2, (float) v.z);
            }
        }
        return this;
    }

    public class Bookmark {
        public Object externalHash;
        protected int vertexCursor = MeshBuilder.this.vertexCursor;
        protected int elementCursor = MeshBuilder.this.elementCursor;
        protected long buildNumber = MeshBuilder.this.buildNumber;
        protected Object hash = computeHash();

        public Bookmark() {
        }

        public Bookmark(int absolute) {
            vertexCursor = absolute;
        }

        public int at() {
            return MeshBuilder.this.vertexCursor - vertexCursor;
        }

        public boolean stillValid() {
            return stillValid(null);
        }

        public boolean stillValid(Object externalHash) {

            if (buildNumber < MeshBuilder.this.buildNumber - 1) {
                Log.log("cache",
                        () -> " buildNumber, build was out of date " + buildNumber + " " + MeshBuilder.this.buildNumber);
                cacheMisses_tooOld++;
                return false;
            }

            this.buildNumber = MeshBuilder.this.buildNumber;

            Log.log("cache",
                    () -> "evaluating cache " + MeshBuilder.this.vertexCursor + " / " + vertexCursor + "  " + MeshBuilder.this.elementCursor + " / " + elementCursor + " " + this.externalHash + "=" + externalHash + " " + computeHash() + "=" + this.hash);

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
            Log.log("cache", () -> "succeeded");
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
