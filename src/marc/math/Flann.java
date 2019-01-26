package marc.math;

import field.linalg.Vec2;
import field.linalg.Vec3;
import field.utility.Log;
import fieldagent.Main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Flann {

    static {
        if (Main.os == Main.OS.mac)
            System.loadLibrary("JNanoFlann");
        else
            System.loadLibrary("jnanoflann");
    }

    private long cloud = -1;
    private boolean is2d = false;
    private LongBuffer cache;
    private FloatBuffer allocated;

    public Flann() {

    }

    // test
    static public void main(String[] a) {

        {
            int num = 1000000;
            FloatBuffer f = ByteBuffer.allocateDirect(4 * num * 3)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            for (int i = 0; i < num; i++) {
                f.put((float) Math.random());
                f.put((float) Math.random());
                f.put((float) Math.random());
            }
            f.rewind();

            Flann flann = new Flann();
            long cloud = flann.build3(f, num);

            Log.log("flann", () -> "got :" + cloud);

            Vec3 v = new Vec3(Math.random(), Math.random(), Math.random());
            int c = flann.closest3(cloud, (float) v.x, (float) v.y, (float) v.z);

            Log.log("flann", () -> "closest is :" + c + "  to " + v + " is " + f.get(c * 3 + 0) + " " + f.get(
                    c * 3 + 1) + " " + f
                    .get(c * 3 + 2) + "  " + new Vec3(3 * c, f));

            Log.log("flann",
                    () -> "distance is " + new Vec3(3 * c, f).distance(v) + " " + new Vec3(3 * c, f).distanceSquared(
                            v));

            LongBuffer out = ByteBuffer.allocateDirect(8 * 10)
                    .order(ByteOrder.nativeOrder())
                    .asLongBuffer();

            flann.closestN3(cloud, (float) v.x, (float) v.y, (float) v.z, out, 10);


            flann.free3(cloud);
        }
        {
            int num = 1000000;
            FloatBuffer f = ByteBuffer.allocateDirect(4 * num * 2)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            for (int i = 0; i < num; i++) {
                f.put((float) Math.random());
                f.put((float) Math.random());
            }
            f.rewind();

            Flann flann = new Flann();
            long cloud = flann.build2(f, num);

            Log.log("flann", () -> "got :" + cloud);

            Vec2 v = new Vec2(Math.random(), Math.random());
            int c = flann.closest2(cloud, (float) v.x, (float) v.y);

            Log.log("flann", () -> "closest is :" + c + "  to " + v + " is " + f.get(c * 2 + 0) + " " + f.get(
                    c * 2 + 1) + " " + f
                    .get(c * 2 + 2) + "  " + new Vec3(2 * c, f));

            Log.log("flann",
                    () -> "distance is " + new Vec2(2 * c, f).distance(v) + " " + new Vec2(2 * c, f).distance(v));

            LongBuffer out = ByteBuffer.allocateDirect(8 * 10)
                    .order(ByteOrder.nativeOrder())
                    .asLongBuffer();

            flann.closestN2(cloud, (float) v.x, (float) v.y, out, 10);


            flann.free2(cloud);
        }
    }

    public <T> void build3d(List<T> d, Function<T, Vec3> t) {
        free();

        FloatBuffer f = (allocated == null || allocated.capacity() != d.size() * 3) ? ByteBuffer.allocateDirect(
                4 * d.size() * 3)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer() : allocated;
        for (T dd : d) {
            Vec3 v = t.apply(dd);
            f.put((float) v.x).put((float) v.y).put((float) v.z);
        }
        f.rewind();
        cloud = build3(f, d.size());
        allocated = f;
        is2d = false;
    }

    public <T> void build128d(List<T> d, Function<T, float[]> t) {
        free();

        FloatBuffer f = (allocated == null || allocated.capacity() != d.size() * 128) ? ByteBuffer.allocateDirect(
                4 * d.size() * 128)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer() : allocated;

        for (T dd : d) {
            float[] v = t.apply(dd);
            if (v.length != 128)
                throw new IllegalArgumentException(v.length + "!=128");
            f.put(v);
        }
        f.rewind();
        cloud = build128(f, d.size());
        allocated = f;
        is2d = false;
    }

    public <T> Flann build2d(List<T> d, Function<T, Vec2> t) {
        free();

        FloatBuffer f = (allocated == null || allocated.capacity() != d.size() * 2) ? ByteBuffer.allocateDirect(
                4 * d.size() * 2)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer() : allocated;
        for (T dd : d) {
            Vec2 v = t.apply(dd);
            f.put((float) v.x).put((float) v.y);
        }
        f.rewind();
        cloud = build2(f, d.size());
        is2d = true;
        allocated = f;

        return this;
    }

    public List<Integer> find3(Vec3 p, int n) {
        if (cache == null || cache.capacity() < n) {
            cache = ByteBuffer.allocateDirect(8 * n).order(ByteOrder.nativeOrder()).asLongBuffer();
        }

        if (cloud == -1 || is2d) throw new IllegalArgumentException();


        closestN3(cloud, (float) p.x, (float) p.y, (float) p.z, cache, n);

        List<Integer> r = new ArrayList<Integer>(n);
        for (int i = 0; i < n; i++) {
            r.add((int) cache.get(i));
        }
        return r;
    }

    public int[] find3(Vec3 p, int n, int[] r) {
        if (cache == null || cache.capacity() < n) {
            cache = ByteBuffer.allocateDirect(8 * n).order(ByteOrder.nativeOrder()).asLongBuffer();
        }

        if (cloud == -1 || is2d) throw new IllegalArgumentException();

        if (r == null || r.length != n)
            r = new int[n];

        closestN3(cloud, (float) p.x, (float) p.y, (float) p.z, cache, n);

        for (int i = 0; i < n; i++) {
            r[i] = ((int) cache.get(i));
        }

        return r;
    }

    public List<Integer> find128(FloatBuffer point, int n) {
        if (cache == null || cache.capacity() < n) {
            cache = ByteBuffer.allocateDirect(8 * n).order(ByteOrder.nativeOrder()).asLongBuffer();
        }

        if (cloud == -1) throw new IllegalArgumentException();

        closestN128(cloud, point, cache, n);

        List<Integer> r = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            r.add((int) cache.get(i));
        }
        return r;
    }

    public List<Integer> find2(Vec2 p, int n) {
        if (cache == null || cache.capacity() < n) {
            cache = ByteBuffer.allocateDirect(8 * n).order(ByteOrder.nativeOrder()).asLongBuffer();
        }
        cache.rewind();
        if (cloud == -1 || !is2d) throw new IllegalArgumentException();

        closestN2(cloud, (float) p.x, (float) p.y, cache, n);

        List<Integer> r = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            r.add((int) cache.get(i));
        }
        return r;
    }

    public int find3(Vec3 p) {

        if (cloud == -1 || is2d) throw new IllegalArgumentException();

        return closest3(cloud, (float) p.x, (float) p.y, (float) p.z);
    }

    public int find2(Vec2 p) {
        if (cloud == -1 || !is2d) throw new IllegalArgumentException();

        int index = closest2(cloud, (float) p.x, (float) p.y);

        return index;
    }

    public void free() {
        if (cloud == -1) return;
        if (is2d)
            free2(cloud);
        else
            free3(cloud);
        cloud = -1;
    }

    native protected long build3(FloatBuffer data, int len);

    native protected int closest3(long flann, float x, float y, float z);

    native protected int closestN3(long flann, float x, float y, float z, LongBuffer buffer, int len);

    native protected void free3(long flann);

    native protected long build128(FloatBuffer data, int len);

    native protected int closest128(long flann, FloatBuffer point);

    native protected int closestN128(long flann, FloatBuffer point, LongBuffer buffer, int len);

    native protected void free128(long flann);

    native protected long build2(FloatBuffer data, int len);

    native protected int closest2(long flann, float x, float y);

    native protected int closestN2(long flann, float x, float y, LongBuffer buffer, int len);

    native protected void free2(long flann);
}
