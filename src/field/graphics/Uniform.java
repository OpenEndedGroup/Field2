package field.graphics;

import field.linalg.*;
import fieldbox.execution.Errors;
import org.lwjgl.opengl.NVBindlessTexture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL20.*;

/**
 * A Scene.Perform that allows you to set a uniform value on a shader at an element in the internalScene graph.
 * <p>
 * Obviously you can .connect these to Shader's to set the value of uniform variables inside shaders, but it's often more useful to .connect these to, say, BaseMeshes to set these values per-mesh
 * rather than per shader. More likely yet is that you have a bunch of uniforms to set at a single point in the graph. See UniformBundle.
 * <p>
 */
public class Uniform<T> extends Scene implements Scene.Perform {

	private final String name;
	FloatBuffer matrix3 = ByteBuffer.allocateDirect(4 * 3 * 3)
		.order(ByteOrder.nativeOrder())
		.asFloatBuffer();
	FloatBuffer matrix4 = ByteBuffer.allocateDirect(4 * 4 * 4)
		.order(ByteOrder.nativeOrder())
		.asFloatBuffer();
	boolean transpose = false;
	boolean intOnly = false;
	boolean pushed = false;
	private Supplier<T> value;
	private T mostRecentValue;
	private Class lastType;

	public Uniform(String name, Supplier<T> value) {
		this.name = name;
		this.value = value;
		if (value instanceof Errors.ErrorConsumer)
			this.ec = ((Errors.ErrorConsumer) value);
		else
			this.ec = Errors.errors.get();
	}

	public Uniform(String name, T value) {
		this.name = name;
		this.value = () -> value;
		if (value instanceof Errors.ErrorConsumer)
			this.ec = ((Errors.ErrorConsumer) value);
		else
			this.ec = Errors.errors.get();
	}

	Errors.ErrorConsumer ec;

	@Override
	public void setErrorConsumer(Errors.ErrorConsumer c) {
		this.ec = c;
	}

	@Override
	public Errors.ErrorConsumer getErrorConsumer() {
		return this.ec;
	}

	static public float[] rewriteToFloatArray(Object t) {

		if (t instanceof float[]) return (float[]) t;
		if (t instanceof int[]) {
			return null;
		}

		if (t instanceof Float) return new float[]{((Number) t).floatValue()};
		if (t instanceof Double) return new float[]{((Number) t).floatValue()};
		if (t instanceof Boolean) return new float[]{((Boolean) t).booleanValue() ? 1f : 0f};

//		if (t instanceof Vector1D) return new float[]{(float) ((Vector1D) t).getX()};
//		if (t instanceof Vector2D) return new float[]{(float) ((Vector2D) t).getX(), (float) ((Vector2D) t).getY()};
//		if (t instanceof Vector3D) return new float[]{(float) ((Vector3D) t).getX(), (float) ((Vector3D) t).getY(), (float) ((Vector3D) t).getZ()};

		if (t instanceof Vec2) return new float[]{(float) ((Vec2) t).x, (float) ((Vec2) t).y};
		if (t instanceof Vec3)
			return new float[]{(float) ((Vec3) t).x, (float) ((Vec3) t).y, (float) ((Vec3) t).z};
		if (t instanceof Vec4)
			return new float[]{(float) ((Vec4) t).x, (float) ((Vec4) t).y, (float) ((Vec4) t).z, (float) ((Vec4) t).w};

		return null;
	}

	static public int[] rewriteToIntArray(Object t) {

		if (t instanceof int[]) return (int[]) t;
		if (t instanceof Number) return new int[]{((Number) t).intValue()};

		return null;
	}

	static public float[][] rewriteToFloatMatrix(Object t) {

		if (t instanceof Mat2)
			return new float[][]{{(float) ((Mat2) t).m00, (float) ((Mat2) t).m10}, {(float) ((Mat2) t).m01, (float) ((Mat2) t).m11}};
		if (t instanceof Mat3)
			return new float[][]{{(float) ((Mat3) t).m00, (float) ((Mat3) t).m10, (float) ((Mat3) t).m20}, {(float) ((Mat3) t).m01, (float) ((Mat3) t).m11, (float) ((Mat3) t).m21}, {(float) ((Mat3) t).m02, (float) ((Mat3) t).m12, (float) ((Mat3) t).m22}};
		if (t instanceof Mat4)
			return new float[][]{{(float) ((Mat4) t).m00, (float) ((Mat4) t).m10, (float) ((Mat4) t).m20, (float) ((Mat4) t).m30}, {(float) ((Mat4) t).m01, (float) ((Mat4) t).m11, (float) ((Mat4) t).m21, (float) ((Mat4) t).m31}, {(float) ((Mat4) t).m02, (float) ((Mat4) t).m12, (float) ((Mat4) t).m22, (float) ((Mat4) t).m32}, {(float) ((Mat4) t).m03, (float) ((Mat4) t).m13, (float) ((Mat4) t).m23, (float) ((Mat4) t).m33}};

		return null;
	}

	static public boolean isAccepableInstance(Object o) {
		try {
			return o instanceof Number || o instanceof Vec2 || o instanceof Vec3 || o instanceof Vec4 || o instanceof Mat2 || o instanceof Mat3 || o instanceof Mat4 || (o instanceof Supplier || isAccepableInstance(
				((Supplier) o).get()));
		} catch (ClassCastException e) {
			return false;
		}
	}

	static public Predicate<Consumer<Integer>> findUniform(String named) {
		return x -> (x instanceof Uniform) && ((Uniform) x).name.equals(named);
	}

	public Uniform<T> setValue(Supplier<T> value) {
		this.value = value;
		return this;
	}

	/**
	 * gets the value of this uniform most recently sent to the graphics system. Note: if this uniform is attached to the internalScene, but no shader cares about this uniform, this will always return
	 * null
	 */
	public T get() {
		return mostRecentValue;
	}

	/**
	 * gets the value that would be sent to the graphics system right now
	 */
	public T evaluate() {
		return value.get();
	}

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
		if (pass == -1) {
			update(-1, this::setUniformNow);
		} else if (pass == 1) {
			if (pushed) {
				Runnable r = GraphicsContext.getContext().uniformCache.pop(this.name);
				pushed = false;
				if (r != null) {
					r.run();
				}
			}
		}
		return true;
	}

	private boolean setUniformNow() {
		return setUniformNow(true);
	}


	//todo: array names
	private boolean setUniformNow(boolean push) {
		pushed = false;

		Integer name = GraphicsContext.getContext().stateTracker.shader.get();
		GraphicsContext.checkError(() -> "while setting (1):" + name + " / " + this.name);
		T t = value.get();

		if (push) {
			GraphicsContext was = GraphicsContext.getContext();
			Thread wasThread = Thread.currentThread();
			boolean changed = GraphicsContext.getContext().uniformCache.push(this.name, t, () -> {
				GraphicsContext cc = GraphicsContext.getContext();

				if (cc != was) {
					System.err.println(" WARNING: graphics context doesn't match ");
					new Exception().printStackTrace();
				}
				if (cc!=was)
				{
					new Exception().printStackTrace();
				}
				if (Thread.currentThread()!=wasThread)
				{
					new Exception().printStackTrace();
				}

				setUniformNow(false);
			});
			pushed = true;

			// bold
			if (!changed) return true;
		}

		try {
			if (name == null) return true;

			int location = cached_glGetUniformLocation(name, this.name);
			GraphicsContext.checkError(() -> "while setting (2):" + name + " / " + this.name);

			lastType = null;

			if (location != -1) {

				if (t == null) {
					return true;
				}

				mostRecentValue = t;
				float[] tf = rewriteToFloatArray(t);
				GraphicsContext.checkError();
				if (tf != null && !intOnly) {
					GraphicsContext.checkError(() -> "while setting (3):" + name + " / " + this.name);
					if (tf.length == 1) {
						glUniform1f(location, tf[0]);
						lastType = Float.class;
					} else if (tf.length == 2) {
						glUniform2f(location, tf[0], tf[1]);
						lastType = Vec2.class;
					} else if (tf.length == 3) {
						glUniform3f(location, tf[0], tf[1], tf[2]);
						lastType = Vec3.class;
					} else if (tf.length == 4) {
						glUniform4f(location, tf[0], tf[1], tf[2], tf[3]);
						lastType = Vec4.class;
					} else
						throw new IllegalArgumentException(" bad dimension after conversion to float array " + t + " -> " + tf.length);
					GraphicsContext.checkError(() -> "while setting (4):" + name + " / " + this.name);
				} else {

					if (t instanceof Long)
					{
						NVBindlessTexture.glUniformHandleui64NV(location, (Long)t);
						GraphicsContext.checkError(() -> "while setting (long, texturehandle):" + name + " / " + this.name);
					}
					else {

						int[] ti = rewriteToIntArray(t);
						if (ti != null) {
							GraphicsContext.checkError(() -> "while setting (4b):" + name + " / " + this.name);
							if (ti.length == 1) {
								glUniform1i(location, ti[0]);
								lastType = Integer.class;
							} else if (ti.length == 2) {
								glUniform2i(location, ti[0], ti[1]);
								lastType = int[].class;
							} else if (ti.length == 3) {
								glUniform3i(location, ti[0], ti[1], ti[2]);
								lastType = int[].class;
							} else if (ti.length == 4) {
								glUniform4i(location, ti[0], ti[1], ti[2], ti[3]);
								lastType = int[].class;
							} else
								throw new IllegalArgumentException(" bad dimension after conversion to int array " + t + " -> " + ti.length);
							GraphicsContext.checkError(() -> "while setting (5):" + name + " / " + this.name);
						} else {
							float[][] tm = rewriteToFloatMatrix(t);

							GraphicsContext.checkError(() -> "while setting (6):" + name + " / " + this.name);

							if (tm != null && !intOnly) {
								if (tm.length == 3) {
									matrix3.rewind();
									matrix3.put(tm[0]);
									matrix3.put(tm[1]);
									matrix3.put(tm[2]);
									matrix3.rewind();
									glUniformMatrix3fv(location, transpose, matrix3);
									lastType = Mat3.class;
								} else if (tm.length == 4) {
									matrix4.rewind();
									matrix4.put(tm[0]);
									matrix4.put(tm[1]);
									matrix4.put(tm[2]);
									matrix4.put(tm[3]);
									matrix4.rewind();
									lastType = Mat4.class;
									glUniformMatrix4fv(location, transpose, matrix4);
								} else if (tm.length == 2) {
									matrix4.rewind();
									matrix4.put(tm[0]);
									matrix4.put(tm[1]);
									matrix4.rewind();
									lastType = Mat2.class;
									glUniformMatrix2fv(location, transpose, matrix4);
								} else
									throw new IllegalArgumentException(" bad dimension after conversion to float matrix " + t + " -> " + tm.length);
							} else
								throw new IllegalArgumentException(" cannot convert " + t + " to something that OpenGL can use as a uniform");
							GraphicsContext.checkError(() -> "while setting (7):" + name + " / " + this.name);

						}
					}
				}
			}
			return true;
		} finally {
			GraphicsContext.checkError(() -> "while setting :" + name + " / " + this.name);
		}
	}

	int cachedLocation = -1;
	int cachedName = -1;
	Shader cachedShader = null;
	int cachedModCount = -1;

	private int cached_glGetUniformLocation(Integer shad, String name) {
		if (GraphicsContext.getContext()==null || GraphicsContext.getContext().uniformCache==null || GraphicsContext.getContext().uniformCache.getCurrentShader()==null) return -1;

		if (cachedName == shad && GraphicsContext.getContext().uniformCache.getCurrentShader() == cachedShader && GraphicsContext.getContext().uniformCache.getCurrentShader().getModCount() == cachedModCount)
			return cachedLocation;

		cachedName = shad;
		cachedShader = GraphicsContext.getContext().uniformCache.getCurrentShader();
		cachedModCount = GraphicsContext.getContext().uniformCache.getCurrentShader().getModCount();
		return cachedLocation = glGetUniformLocation(shad, name);
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1, 1};
	}

	/**
	 * returns the name that this Uniform is bound to in any shaders above this Perform
	 */
	public String getName() {
		return name;
	}

	/**
	 * returns the type that this Uniform ended up setting (note: this will be null, if the uniform has never been actually sent to OpenGL, or if an error occured doing so). It will be one of Float.class, Vec2.class, Vec3.class etc.
	 */
	public Class getLastType() {
		return lastType;
	}

	@Override
	public String toString() {
		return getName()+" = "+mostRecentValue+" / "+lastType;
	}
}
