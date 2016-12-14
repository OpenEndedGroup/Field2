package field.graphics;

import field.utility.Conversions;
import field.utility.Dict;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.execution.HandlesCompletion;
import fielded.boxbrowser.BoxBrowser;
import fieldlinker.Linker;
import fieldnashorn.annotations.HiddenInAutocomplete;
import org.lwjgl.opengl.*;
;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL20.*;

/**
 * An OpenGL Shader written in GL Shader Language (GLSL)
 * <p>
 * Much of the complexity of this class stems from the fact that an OpenGL program is linked from a variety of (sometimes optional, sometimes reusable) parts. This class supports all of them. The rest
 * of the complexity comes from the fact that both the compilation and the link stage can fail.
 * <p>
 * Assuming that all goes well shaders take three principle kinds of inputs: vertex attributes (make and set these with aux and nextVertex calls in MeshBuilders), uniforms (make and set these with
 * calls to Uniform and UniformBundle classes here) and input from earlier shaders.
 */
public class Shader extends BaseScene<Shader.State> implements Scene.Perform, Linker.AsMap, HandlesCompletion, BoxBrowser.HasMarkdownInformation {

	private ShaderIntrospection introspection;
	private int modCount;

	@Override
	public String generateMarkdown(Box inside, Dict.Prop property) {
		if (introspection==null)
			return "Shader has not been executed by the graphics system, is it correctly attached to something?";

		return introspection.getMarkdown(inside);
	}

	static public class State extends BaseScene.Modifiable {
		int name;
		boolean work = true;
		boolean valid = false;
	}

	public enum Type {
		vertex(GL20.GL_VERTEX_SHADER), geometry(GL32.GL_GEOMETRY_SHADER), fragment(GL20.GL_FRAGMENT_SHADER), tessControl(GL40.GL_TESS_CONTROL_SHADER), tessEval(
			    GL40.GL_TESS_EVALUATION_SHADER), compute(GL43.GL_COMPUTE_SHADER);

		public int gl;

		private Type(int gl) {
			this.gl = gl;
		}
	}

	public interface iErrorHandler {
		public void beginError();

		public void errorOnLine(int line, String error);

		public void endError();

		public void noError();
	}

	protected iErrorHandler onError = null;

	Map<Type, Source> source = new LinkedHashMap<>();

	static public class Source {
		public String source;
		protected final Type type;
		protected int status;
		public iErrorHandler onError = null;

		protected Set<Integer> attachedTo = new LinkedHashSet<>();

		public Source(String source, Type type) {
			this.type = type;
			this.source = source;
			this.status = 0;
		}

		public Source setOnError(iErrorHandler onError) {
			this.onError = onError;
			return this;
		}

		public iErrorHandler getOnError() {
			return onError;
		}

		public class State {
			String source;
			int name = -1;
			boolean good = false;
		}

		protected boolean clean() {
			State s = GraphicsContext.get(this, () -> {
				State state = new State();
				state.source = source;
				return state;
			});

			if (!s.source.equals(this.source)) {
				for (Integer ii : attachedTo)
					GL20.glDetachShader(ii, s.name);
				attachedTo.clear();
				GL20.glDeleteShader(s.name);

				s = new State();
				s.source = source;
				GraphicsContext.put(this, s);
			}

			final State finalS = s;
			Log.log("graphics.trace", ()->" shader name " + finalS.name);

			if (s.name == -1) {
				try {
					Log.log("graphics.trace", () -> " creating shader");
					s.name = GL20.glCreateShader(type.gl);
					GL20.glShaderSource(s.name, s.source);
					GL20.glCompileShader(s.name);
					status = GL20.glGetShaderi(s.name, GL20.GL_COMPILE_STATUS);
					Log.log("graphics.trace", () -> " shader compile status" + status);

					if (status == 0) {
						String ret = GL20.glGetShaderInfoLog(s.name, 10000);
						Log.log("graphics.error", () -> type + " program failed to compile");
						Log.log("graphics.error", () -> " log is <" + ret + ">");
						Log.log("graphics.error", () -> " shader source is <" + source + ">, reporting to <" + onError + ">");
						if (onError != null) {
							onError.beginError();
							String log = ret;
							String[] lines = log.split("\n");
							for (String ll : lines) {
								try {
									String[] ss = ll.split(":");
									if (ss.length > 2) {
										int ii = Integer.parseInt(ss[2]);
										onError.errorOnLine(ii, ll);
									}
								} catch (NumberFormatException e) {
									try {
										Matcher q = Pattern.compile(".*?\\((.*?)\\)")
											.matcher(ll);
										q.find();
										String g = q.group(1);
										int ii = Integer.parseInt(g);
										onError.errorOnLine(ii, ll);
									} catch (Exception e2) {
										e2.printStackTrace();
									}
								}
							}
							onError.endError();
						}
						Log.log("graphics.error", () -> " shader is not good");
						s.good = false;
						return false;
					} else {
						if (onError != null) onError.noError();

						s.good = true;
						return true;
					}
				}
				catch(Throwable t)
				{
					t.printStackTrace();
				}
			}
			return false;
		}

		public void finalize() {
			GraphicsContext.postQueueInAllContexts(this::destroy);
		}

		protected void destroy() {
			State s = GraphicsContext.remove(this);
			if (s != null) GL20.glDeleteShader(s.name);
		}
	}

	protected State setup() {
		State s = new State();
		s.name = GL20.glCreateProgram();
		return s;
	}

	public Source addSource(Type type, String source) {
		Source s = new Source(source, type);
		this.source.put(type, s);
		return s;
	}

	public Map<Type, Source> getSources() {
		return this.source;
	}


	@Override
	protected void deallocate(State s) {
		GL20.glDeleteProgram(s.name);
		// doesn't kill the shaders, because, technically they could be attached elsewhere
	}

	public Shader setOnError(iErrorHandler onError) {
		this.onError = onError;
		return this;
	}

	protected boolean perform0() {
		boolean work = false;

		GraphicsContext.checkError(() -> "on shader entry");

		Log.log("graphics.trace", () -> " checking :" + source.keySet());

		for (Map.Entry<Type, Source> s : source.entrySet()) {
			work |= s.getValue()
				 .clean();
		}

		State name = GraphicsContext.get(this);

		work |= name.work;
		name.work = false;

		if (work) {
			name.valid = true;
			for (Map.Entry<Type, Source> s : source.entrySet()) {
				Source.State state = GraphicsContext.get(s.getValue());
				if (s.getValue().status != 0 && state.name != -1 && !s.getValue().attachedTo.contains(name.name)) {
					GL20.glAttachShader(name.name, state.name);
					s.getValue().attachedTo.add(name.name);
				}
			}

			GL30.glBindFragDataLocation(name.name, 0, "_output");
			GL30.glBindFragDataLocation(name.name, 0, "_output0");
			GL30.glBindFragDataLocation(name.name, 1, "_output1");
			GL30.glBindFragDataLocation(name.name, 2, "_output2");
			GL30.glBindFragDataLocation(name.name, 3, "_output3");

			for (int i = 1; i < 16; i++)
				glBindAttribLocation(name.name, i, "attribute" + i);

			glLinkProgram(name.name);
			int linkStatus = glGetProgrami(name.name, GL20.GL_LINK_STATUS);
			if (linkStatus == 0) {
				String ret = GL20.glGetProgramInfoLog(name.name, 10000);
				System.err.println(" program failed to link");
				System.err.println(" log is <" + ret + ">");
				if (onError != null) {
					onError.beginError();
					onError.errorOnLine(-1, ret);
					onError.endError();
				}
				name.valid = false;
			}


			glValidateProgram(name.name);
			int validateStatus = glGetProgrami(name.name, GL20.GL_VALIDATE_STATUS);
			if (validateStatus == 0) {
				String ret = GL20.glGetProgramInfoLog(name.name, 10000);
				Log.log("graphics.warning",()-> " program failed to validate (note, this can be benign). Log is " + ret);
				if (!ret.trim()
					.toLowerCase()
					.equals("Validation Failed: No vertex array object bound.".toLowerCase())) if (onError != null) {
					onError.beginError();
					onError.errorOnLine(-1, ret);
					onError.endError();
				}
				// it didn't validate right now, but that doesn't mean that it wont in the future
//				name.valid = false;
			}

			if (name.valid)
			{
				if (introspection==null) {
					introspection = new ShaderIntrospection(this);
					introspection.reloadedAt = Instant.now();
				}
				else
				{
					introspection.reloadedAt = Instant.now();
					introspection.reloadedTimes++;
					introspection.invocationCountSinceReload=0;
				}

				introspection.introspectNow();

				modCount ++;
			}
		}

		if (name.valid) {
//			System.out.println(" setting shader to be :"+name.name);
			Log.log("graphics.trace", () -> " using program " + name.name);
			GraphicsContext.getContext().stateTracker.shader.set(name.name);
			GraphicsContext.getContext().uniformCache.changeShader(this, name.name);
		} else {
			System.err.println("WARNING: shader is invalid, not being used ");
			Log.log("graphics.trace", ()->"WARNING: program not valid, not being used");
			if (introspection!=null)
				introspection.errorIsInvalid = "Shader failed GL validation, it is not being used\n";
		}

		GraphicsContext.checkError(() -> "on shader exit");

		return true;
	}

	@Override
	protected boolean perform1() {
		GraphicsContext.getContext().stateTracker.shader.set(0);
		if (introspection!=null)
		{
			introspection.invocationCountSinceReload++;
			introspection.invocationCountTotal++;
			introspection.invocationAt = Instant.now();
		}
		return true;
	}

	public Integer getOpenGLName() {
		State s = GraphicsContext.get(this, () -> null);
		if (s == null) return null;
		return s.name;
	}

	@Override
	public int[] getPasses() {
		return new int[]{-2, 2};
	}


	@Override
	@HiddenInAutocomplete
	public boolean asMap_isProperty(String p) {
		if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();
		return !knownNonProperties.contains(p);

	}

	protected Set<String> knownNonProperties;

	protected Set<String> computeKnownNonProperties() {
		Set<String> r = new LinkedHashSet<>();
		Method[] m = this.getClass()
				 .getMethods();
		for (Method mm : m)
			r.add(mm.getName());
		Field[] f = this.getClass()
				.getFields();
		for (Field ff : f)
			r.add(ff.getName());
		return r;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_call(Object a, Object b) {
		throw new Error();
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_get(String p) {
		Uniform u = getDefaultBundle().get(new Dict.Prop(p));
		if (u != null) return u.get();
		else {
			return super.asMap_get(p);
		}
	}

	static public Method supplier_get;
	static private Object[] nothing = {};

	static {
		try {
			supplier_get = Supplier.class.getDeclaredMethod("get");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_set(String p, Object o) {
		Object fo = Conversions.convert(o, Supplier.class);
		if (fo instanceof Supplier) return getDefaultBundle().set(p, (Supplier) fo);
		if (fo instanceof InvocationHandler) {
			return getDefaultBundle().set(p, () -> {
				try {
					return ((InvocationHandler) fo).invoke(fo, supplier_get, nothing);
				} catch (Throwable throwable) {
					throwable.printStackTrace();
				}
				return null;
			});
		}
		if (Uniform.isAccepableInstance(fo)) return getDefaultBundle().set(p, () -> fo);

		if (o instanceof  OffersUniform)
		{
			getDefaultBundle().set(p, () -> ((OffersUniform)o).getUniform());
			// fall through -- connect things as well as set them as uniforms
		}

		return super.asMap_set(p, o);
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object a) {
		throw new Error();
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object a, Object b) {
		throw new Error();
	}

	public int getModCount() {
		return modCount;
	}
}
