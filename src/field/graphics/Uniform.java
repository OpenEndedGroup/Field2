package field.graphics;

import field.linalg.*;
import org.apache.commons.math3.geometry.euclidean.oned.Vector1D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL20.*;

/**
 * A Scene.Perform that allows you to set a uniform value on a shader at an element in the scene graph.
 * <p>
 * Obviously you can .connect these to Shader's to set the value of uniform variables inside shaders, but it's often more useful to .connect these to,
 * say, BaseMeshes to set these values per-mesh rather than per shader. More likely yet is that you have a bunch of uniforms to set at a single point
 * in the graph. See UniformBundle.
 * <p>
 * todo: uniform cache (store both locations (invalidate on reload) and current values (invalidate on shader change) per context per shader)
 */
public class Uniform<T> extends Scene implements Scene.Perform {

	private final String name;
	private Supplier<T> value;

	public Uniform(String name, Supplier<T> value) {
		this.name = name;
		this.value = value;
	}

	public Uniform(String name, T value) {
		this.name = name;
		this.value = () -> value;
	}

	public Uniform<T> setValue(Supplier<T> value) {
		this.value = value;
		return this;
	}

	FloatBuffer matrix3 = ByteBuffer.allocateDirect(4 * 3 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
	FloatBuffer matrix4 = ByteBuffer.allocateDirect(4 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

	boolean transpose = false;
	boolean intOnly = false;

	public Uniform<T> setTranspose(boolean t) {
		this.transpose = t;
		return this;
	}

	public Uniform<T> setIntOnly(boolean t) {
		this.intOnly = t;
		return this;
	}

	@Override
	public boolean perform(int pass) {
		if (pass == -1) update(-1, () -> setUniformNow());
		return true;
	}

	//todo: array names
	private boolean setUniformNow() {
		Integer name = GraphicsContext.stateTracker.shader.get();

		if (name == null) return true;


		int location = glGetUniformLocation(name, this.name);

		if (location != -1) {
			T t = value.get();
			float[] tf = rewriteToFloatArray(t);
			if (tf != null && !intOnly) {
				if (tf.length == 1) glUniform1f(location, tf[0]);
				else if (tf.length == 2) glUniform2f(location, tf[0], tf[1]);
				else if (tf.length == 3) glUniform3f(location, tf[0], tf[1], tf[2]);
				else if (tf.length == 4) glUniform4f(location, tf[0], tf[1], tf[2], tf[3]);
				else throw new IllegalArgumentException(" bad dimension after conversion to float array " + t + " -> " + tf.length);
			} else {
				int[] ti = rewriteToIntArray(t);
				if (ti != null) {
					if (ti.length == 1) glUniform1i(location, ti[0]);
					else if (ti.length == 2) glUniform2i(location, ti[0], ti[1]);
					else if (ti.length == 3) glUniform3i(location, ti[0], ti[1], ti[2]);
					else if (ti.length == 4) glUniform4i(location, ti[0], ti[1], ti[2], ti[3]);
					else
						throw new IllegalArgumentException(" bad dimension after conversion to int array " + t + " -> " + ti.length);
				} else {
					float[][] tm = rewriteToFloatMatrix(t);
					if (tm != null && !intOnly) {
						if (tm.length == 3) {
							matrix3.rewind();
							matrix3.put(tm[0]);
							matrix3.put(tm[1]);
							matrix3.put(tm[2]);
							matrix3.rewind();
							glUniformMatrix3(location, transpose, matrix3);
						} else if (tm.length == 4) {
							matrix4.rewind();
							matrix4.put(tm[0]);
							matrix4.put(tm[1]);
							matrix4.put(tm[2]);
							matrix4.put(tm[3]);
							matrix4.rewind();
							glUniformMatrix4(location, transpose, matrix4);
						} else if (tm.length == 2) {
							matrix4.rewind();
							matrix4.put(tm[0]);
							matrix4.put(tm[1]);
							matrix4.rewind();
							glUniformMatrix2(location, transpose, matrix4);
						} else
							throw new IllegalArgumentException(" bad dimension after conversion to float matrix " + t + " -> " + tm.length);
					} else
						throw new IllegalArgumentException(" cannot convert " + t + " to something that OpenGL can use as a uniform");

				}
			}
		}
		return true;
	}

	static public float[] rewriteToFloatArray(Object t) {

		if (t instanceof float[]) return (float[]) t;
		if (t instanceof Float) return new float[]{((Number) t).floatValue()};

		if (t instanceof Vector1D) return new float[]{(float) ((Vector1D) t).getX()};
		if (t instanceof Vector2D) return new float[]{(float) ((Vector2D) t).getX(), (float) ((Vector2D) t).getY()};
		if (t instanceof Vector3D)
			return new float[]{(float) ((Vector3D) t).getX(), (float) ((Vector3D) t).getY(), (float) ((Vector3D) t).getZ()};

		if (t instanceof Vec2) return new float[]{(float) ((Vec2) t).getX(), (float) ((Vec2) t).getY()};
		if (t instanceof Vec3) return new float[]{(float) ((Vec3) t).getX(), (float) ((Vec3) t).getY(), (float) ((Vec3) t).getZ()};
		if (t instanceof Vec4)
			return new float[]{(float) ((Vec4) t).getX(), (float) ((Vec4) t).getY(), (float) ((Vec4) t).getZ(), (float) ((Vec4) t)
				    .getW()};

		return null;
	}

	static public int[] rewriteToIntArray(Object t) {

		if (t instanceof int[]) return (int[]) t;
		if (t instanceof Number) return new int[]{((Number) t).intValue()};

		return null;
	}

	static public float[][] rewriteToFloatMatrix(Object t) {

		if (t instanceof Mat2) return new float[][]{{((Mat2) t).m00, ((Mat2) t).m10}, {((Mat2) t).m01, ((Mat2) t).m11}};
		if (t instanceof Mat3)
			return new float[][]{{((Mat3) t).m00, ((Mat3) t).m10, ((Mat3) t).m20}, {((Mat3) t).m01, ((Mat3) t).m11, ((Mat3) t).m21}, {((Mat3) t).m02, ((Mat3) t).m12, ((Mat3) t).m22}};
		if (t instanceof Mat4)
			return new float[][]{{((Mat4) t).m00, ((Mat4) t).m10, ((Mat4) t).m20, ((Mat4) t).m30}, {((Mat4) t).m01, ((Mat4) t).m11, ((Mat4) t).m21, ((Mat4) t).m31}, {((Mat4) t).m02, ((Mat4) t).m12, ((Mat4) t).m22, ((Mat4) t).m32}, {((Mat4) t).m03, ((Mat4) t).m13, ((Mat4) t).m23, ((Mat4) t).m33}};

		return null;
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1};
	}

	static public Predicate<Consumer<Integer>> findUniform(String named) {
		return x -> (x instanceof Uniform) && ((Uniform) x).name.equals(named);
	}

}
