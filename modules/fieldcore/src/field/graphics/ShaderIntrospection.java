package field.graphics;

import field.linalg.*;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fielded.plugins.Out;
import org.lwjgl.opengl.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

/**
 * Created by marc on 11/24/15.
 */
public class ShaderIntrospection {

	private final Shader s;

	public int invocationCountTotal;
	public int invocationCountSinceReload;
	public int reloadedTimes;
	public Instant reloadedAt;
	public Instant invocationAt;

	public ShaderIntrospection(Shader s) {
		this.s = s;
	}

	public class Uniform {
		String name;
		int type;
		int size;

		public Uniform(String name, int size, int type) {
			this.name = name;
			this.size = size;
			this.type = type;
		}

		@Override
		public String toString() {
			return name + "(" + uniformTypeConstants_readable.getOrDefault(type, new Pair<>("", "UNKNOWN")).second + (size == 1 ? "" : ("x " + size)) + ")";
		}
	}

	public class Attribute {
		int location;
		String name;
		int type;
		int size;

		public Attribute(String name, int size, int type, int location) {
			this.name = name;
			this.size = size;
			this.type = type;
			this.location = location;
		}

		@Override
		public String toString() {
			return name + "(" + typeConstants_readable.getOrDefault(type, "UNKNOWN") + (size == 1 ? "" : ("x " + size)) + ")";
		}
	}

	// string taken from the spec
	static String typeConstants
		    = "GL_FLOAT, GL_FLOAT_VEC2, GL_FLOAT_VEC3, GL_FLOAT_VEC4, GL_FLOAT_MAT2, GL_FLOAT_MAT3, GL_FLOAT_MAT4, GL_FLOAT_MAT2x3, GL_FLOAT_MAT2x4, GL_FLOAT_MAT3x2, GL_FLOAT_MAT3x4, GL_FLOAT_MAT4x2, GL_FLOAT_MAT4x3, GL_INT, GL_INT_VEC2, GL_INT_VEC3, GL_INT_VEC4, GL_UNSIGNED_INT, GL_UNSIGNED_INT_VEC2, GL_UNSIGNED_INT_VEC3, GL_UNSIGNED_INT_VEC4";
	static Map<Integer, String> typeConstants_readable = new LinkedHashMap<>();

	static {
		String[] names = typeConstants.replace(",", "")
					      .split(" ");

		Class[] classes = {GL11.class, GL20.class, GL21.class, GL30.class, ARBVertexAttrib64Bit.class, GL41.class, GL42.class};

		for (Class c : classes) {
			Map<String, Field> m = Arrays.asList(c.getDeclaredFields())
						     .stream()
						     .collect(Collectors.toMap(x -> x.getName(), x -> x));

			for (String n : names) {
				Field q = m.get(n);
				if (q == null) ;
				else try {
					typeConstants_readable.put(((Number) q.get(null)).intValue(), n);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static String uniformTypeConstants
		    = "GL_FLOAT, float, GL_FLOAT_VEC2, vec2, GL_FLOAT_VEC3, vec3, GL_FLOAT_VEC4, vec4, GL_DOUBLE, double, GL_DOUBLE_VEC2, dvec2, GL_DOUBLE_VEC3, dvec3, GL_DOUBLE_VEC4, dvec4, GL_INT, int, GL_INT_VEC2, ivec2, GL_INT_VEC3, ivec3, GL_INT_VEC4, ivec4, GL_UNSIGNED_INT, unsigned_int, GL_UNSIGNED_INT_VEC2, uvec2, GL_UNSIGNED_INT_VEC3, uvec3, GL_UNSIGNED_INT_VEC4, uvec4, GL_BOOL, bool, GL_BOOL_VEC2, bvec2, GL_BOOL_VEC3, bvec3, GL_BOOL_VEC4, bvec4, GL_FLOAT_MAT2, mat2, GL_FLOAT_MAT3, mat3, GL_FLOAT_MAT4, mat4, GL_FLOAT_MAT2x3, mat2x3, GL_FLOAT_MAT2x4, mat2x4, GL_FLOAT_MAT3x2, mat3x2, GL_FLOAT_MAT3x4, mat3x4, GL_FLOAT_MAT4x2, mat4x2, GL_FLOAT_MAT4x3, mat4x3, GL_DOUBLE_MAT2, dmat2, GL_DOUBLE_MAT3, dmat3, GL_DOUBLE_MAT4, dmat4, GL_DOUBLE_MAT2x3, dmat2x3, GL_DOUBLE_MAT2x4, dmat2x4, GL_DOUBLE_MAT3x2, dmat3x2, GL_DOUBLE_MAT3x4, dmat3x4, GL_DOUBLE_MAT4x2, dmat4x2, GL_DOUBLE_MAT4x3, dmat4x3, GL_SAMPLER_1D, sampler1D, GL_SAMPLER_2D, sampler2D, GL_SAMPLER_3D, sampler3D, GL_SAMPLER_CUBE, samplerCube, GL_SAMPLER_1D_SHADOW, sampler1DShadow, GL_SAMPLER_2D_SHADOW, sampler2DShadow, GL_SAMPLER_1D_ARRAY, sampler1DArray, GL_SAMPLER_2D_ARRAY, sampler2DArray, GL_SAMPLER_1D_ARRAY_SHADOW, sampler1DArrayShadow, GL_SAMPLER_2D_ARRAY_SHADOW, sampler2DArrayShadow, GL_SAMPLER_2D_MULTISAMPLE, sampler2DMS, GL_SAMPLER_2D_MULTISAMPLE_ARRAY, sampler2DMSArray, GL_SAMPLER_CUBE_SHADOW, samplerCubeShadow, GL_SAMPLER_BUFFER, samplerBuffer, GL_SAMPLER_2D_RECT, sampler2DRect, GL_SAMPLER_2D_RECT_SHADOW, sampler2DRectShadow, GL_INT_SAMPLER_1D, isampler1D, GL_INT_SAMPLER_2D, isampler2D, GL_INT_SAMPLER_3D, isampler3D, GL_INT_SAMPLER_CUBE, isamplerCube, GL_INT_SAMPLER_1D_ARRAY, isampler1DArray, GL_INT_SAMPLER_2D_ARRAY, isampler2DArray, GL_INT_SAMPLER_2D_MULTISAMPLE, isampler2DMS, GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, isampler2DMSArray, GL_INT_SAMPLER_BUFFER, isamplerBuffer, GL_INT_SAMPLER_2D_RECT, isampler2DRect, GL_UNSIGNED_INT_SAMPLER_1D, usampler1D, GL_UNSIGNED_INT_SAMPLER_2D, usampler2D, GL_UNSIGNED_INT_SAMPLER_3D, usampler3D, GL_UNSIGNED_INT_SAMPLER_CUBE, usamplerCube, GL_UNSIGNED_INT_SAMPLER_1D_ARRAY, usampler2DArray, GL_UNSIGNED_INT_SAMPLER_2D_ARRAY, usampler2DArray, GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE, usampler2DMS, GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, usampler2DMSArray, GL_UNSIGNED_INT_SAMPLER_BUFFER, usamplerBuffer, GL_UNSIGNED_INT_SAMPLER_2D_RECT, usampler2DRect, GL_IMAGE_1D, image1D, GL_IMAGE_2D, image2D, GL_IMAGE_3D, image3D, GL_IMAGE_2D_RECT, image2DRect, GL_IMAGE_CUBE, imageCube, GL_IMAGE_BUFFER, imageBuffer, GL_IMAGE_1D_ARRAY, image1DArray, GL_IMAGE_2D_ARRAY, image2DArray, GL_IMAGE_2D_MULTISAMPLE, image2DMS, GL_IMAGE_2D_MULTISAMPLE_ARRAY, image2DMSArray, GL_INT_IMAGE_1D, iimage1D, GL_INT_IMAGE_2D, iimage2D, GL_INT_IMAGE_3D, iimage3D, GL_INT_IMAGE_2D_RECT, iimage2DRect, GL_INT_IMAGE_CUBE, iimageCube, GL_INT_IMAGE_BUFFER, iimageBuffer, GL_INT_IMAGE_1D_ARRAY, iimage1DArray, GL_INT_IMAGE_2D_ARRAY, iimage2DArray, GL_INT_IMAGE_2D_MULTISAMPLE, iimage2DMS, GL_INT_IMAGE_2D_MULTISAMPLE_ARRAY, iimage2DMSArray, GL_UNSIGNED_INT_IMAGE_1D, uimage1D, GL_UNSIGNED_INT_IMAGE_2D, uimage2D, GL_UNSIGNED_INT_IMAGE_3D, uimage3D, GL_UNSIGNED_INT_IMAGE_2D_RECT, uimage2DRect, GL_UNSIGNED_INT_IMAGE_CUBE, uimageCube, GL_UNSIGNED_INT_IMAGE_BUFFER, uimageBuffer, GL_UNSIGNED_INT_IMAGE_1D_ARRAY, uimage1DArray, GL_UNSIGNED_INT_IMAGE_2D_ARRAY, uimage2DArray, GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE, uimage2DMS, GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE_ARRAY, uimage2DMSArray, GL_UNSIGNED_INT_ATOMIC_COUNTER, atomic_uint";
	static Map<Integer, Pair<String, String>> uniformTypeConstants_readable = new LinkedHashMap<>();

	static {
		String[] names = uniformTypeConstants.replace(",", "")
						     .split(" ");

		Class[] classes = {GL11.class, GL20.class, GL21.class, GL30.class, ARBVertexAttrib64Bit.class, GL41.class, GL42.class};

		for (Class c : classes) {
			Map<String, Field> m = Arrays.asList(c.getDeclaredFields())
						     .stream()
						     .collect(Collectors.toMap(x -> x.getName(), x -> x));

			for (int i = 0; i < names.length; i += 2) {
				Field q = m.get(names[i]);
				if (q == null) ;
				else try {
					uniformTypeConstants_readable.put(((Number) q.get(null)).intValue(), new Pair<>(names[i], names[i + 1]));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	List<Uniform> uniforms = new ArrayList<>();
	List<Attribute> attrs = new ArrayList<>();

	IntBuffer sz = ByteBuffer.allocateDirect(4)
				 .order(ByteOrder.nativeOrder())
				 .asIntBuffer();
	IntBuffer ty = ByteBuffer.allocateDirect(4)
				 .order(ByteOrder.nativeOrder())
				 .asIntBuffer();

	public String introspectionHasAnError = null;
	public String errorIsInvalid = null;

	public void introspectNow() {
		uniforms.clear();
		attrs.clear();

		GraphicsContext co = GraphicsContext.getContext();
		if (co == null) throw new IllegalArgumentException(" can't call introspectNow() unless you are inside an OpenGL context");
		int uniforms = GL20.glGetProgrami(this.s.getOpenGLName(), GL20.GL_ACTIVE_UNIFORMS);

		for (int i = 0; i < uniforms; i++) {
			String name = GL20.glGetActiveUniform(this.s.getOpenGLName(), i, 255, sz, ty);
			this.uniforms.add(new Uniform(name, sz.get(0), ty.get(0)));
		}

		int attrs = GL20.glGetProgrami(this.s.getOpenGLName(), GL20.GL_ACTIVE_ATTRIBUTES);
		for (int i = 0; i < attrs; i++) {
			String name = GL20.glGetActiveAttrib(this.s.getOpenGLName(), i, sz, ty);

			int location = GL20.glGetAttribLocation(this.s.getOpenGLName(), name);
			this.attrs.add(new Attribute(name, sz.get(0), ty.get(0), location));
		}

		int e = GL11.glGetError();

		if (e != 0) {
			introspectionHasAnError = "Shader Introspection caused a GL error " + /*GLUtil.getErrorString(*/e/*)*/ + "\n";
		}
	}


	List<BaseMesh> okMeshes = null;
	HashMap<BaseMesh, String> problemMeshes = null;

	String directAttachmentErrors = null;

	public void lint(Function<Object, String> convert) {
		Collection<BaseMesh> meshes = collect(s, x -> x instanceof BaseMesh);

		problemMeshes = new LinkedHashMap<>();
		okMeshes = new ArrayList<>();

		for (BaseMesh m : meshes) {
			String errors = checkMesh(m, convert);
			if (errors == null) okMeshes.add(m);
			else problemMeshes.put(m, errors);
		}

		UniformBundle ubp = s.getDefaultBundle();
		Set<field.graphics.Uniform> up = new LinkedHashSet<>(ubp.getUniforms()
									.values());

		s.children().stream().filter(x -> x instanceof field.graphics.Uniform).forEach(x -> up.add((field.graphics.Uniform)x));
		s.internalScene.values().stream().flatMap(x -> x.stream()).filter(x -> x instanceof field.graphics.Uniform).forEach(x -> up.add((field.graphics.Uniform)x));

		String e = checkUniforms("", up, up, convert);
		directAttachmentErrors = e;

	}

	public String getMarkdown(Box inside) {

		Function<Object, String> convert = x -> ("" + x);

		if (inside != null) convert = inside.find(Out.__out, inside.both())
						    .findFirst()
						    .map(x -> (Function<Object, String>) x::convert)
						    .orElseGet(() -> (z -> ("" + z)));

		lint(convert);

		String e0 = null;

		Instant now = Instant.now();
		if (invocationAt == null) {
			e0 = "Shader has never been executed. Are you sure it is attached?<br>";
		} else {
			Duration d0 = Duration.between(invocationAt, now);
			Duration dr = Duration.between(reloadedAt, now);

			if (invocationCountTotal == invocationCountSinceReload) {
				e0 = "Executed <b>" + invocationCountTotal + "</b> time" + (invocationCountTotal == 1 ? "" : "s") + ". Last executed <b>" + timeString(
					    d0) + "</b> ago; compiled, <b>once</b>, <b>" + timeString(dr) + "</b>ago.<br>";
			} else {
				e0
					    = "Executed <b>" + invocationCountTotal + "</b> time" + (invocationCountTotal == 1 ? "" : "s") + " in total (<b>" + invocationCountSinceReload + "</b> time" + (invocationCountSinceReload == 1 ? "" : "s") + " since reloading). Last executed <b>" + timeString(
					    d0) + "</b> ago; compiled, <b>" + reloadedTimes + "</b> time" + (reloadedTimes == 1 ? "" : "s") + " in total; most recently <b>" + timeString(
					    dr) + "</b>ago.<br>";
			}
		}


		String e1 = null;

		if (okMeshes.size() == 0 && problemMeshes.size() == 0) {
			e1 = "No meshes are attached to this shader.\n";
		}
		if (okMeshes.size() > 0 && problemMeshes.size() == 0) {
			e1 = "No problems found in " + okMeshes.size() + " mesh" + (okMeshes.size() == 1 ? "" : "es") + ". Meshes are :" + convert.apply(okMeshes);
		} else {
			String a = "Problems found in " + problemMeshes.size() + " mesh" + (problemMeshes.size() == 1 ? "" : "es") + " &mdash;\n";
			for (Map.Entry<BaseMesh, String> entry : problemMeshes.entrySet()) {
				a += "<div class='mesh-problem'>" + convert.apply(entry.getKey()) + "<br>";
				a += entry.getValue() + "\n</div>";
			}
			if (okMeshes.size() > 0) {
				a += "No problems found in " + okMeshes.size() + " other mesh" + (okMeshes.size() == 1 ? "" : "es") + ". Meshes are :" + convert.apply(okMeshes);
			}
			e1 = a;
		}

		if (directAttachmentErrors != null && directAttachmentErrors.trim()
									    .length() > 0) {
			e1 += "<p>Uniforms connected directly to this shader have problems &mdash;</p>";
			e1 += "<div class='mesh-problem'>" + directAttachmentErrors + "</dv>";
		}

		return e0+"<br>"+e1.replace("\n", "<br>");
	}

	private String timeString(Duration d0) {
		String q = d0.toString()
			     .toLowerCase();
		if (q.startsWith("pt")) q = q.substring(2);
		return q+" ";
	}

	private String checkMesh(BaseMesh m, Function<Object, String> convert) {

		Map<Integer, ArrayBuffer> q = m.buffers();

		Map<Integer, Attribute> v = attrs.stream()
						 .collect(Collectors.toMap(x -> x.location, x -> x));

		String errors = "";

		for (Map.Entry<Integer, ArrayBuffer> e : q.entrySet()) {

			Attribute a = v.get(e.getKey());

			if (a == null) {
				errors += "&mdash; this mesh declares <i>unused aux channel</i> <b>" + e.getKey() + "</b> of <b>" + simpleFloatTypeName(e.getValue()
																			  .getDimension()) + "</b>\n";
			} else {
				switch (a.type) {
					case GL11.GL_FLOAT:
						if (e.getValue()
						     .getDimension() != 1) errors += "&mdash; this mesh has a <i>dimension mismatch</i> between aux channel (of <b>" + simpleFloatTypeName(e.getValue()
																							    .getDimension()) + "</b>) and shader (<b>float</b>)\n";
						else v.remove(e.getKey());
						break;
					case GL20.GL_FLOAT_VEC2:
						if (e.getValue()
						     .getDimension() != 2) errors += "&mdash; this mesh has a <i>dimension mismatch</i> between aux channel (of <b>" + simpleFloatTypeName(e.getValue()
																							    .getDimension()) + "</b>) and shader (<b>vec2</b>) called <b>" + a.name + "</b> in location <b>" + a.location + "</b>\n";
						else v.remove(e.getKey());
						break;
					case GL20.GL_FLOAT_VEC3:
						if (e.getValue()
						     .getDimension() != 3) errors += "&mdash; this mesh has a <i>dimension mismatch</i> between aux channel (of <b>" + simpleFloatTypeName(e.getValue()
																							    .getDimension()) + "</b>) and shader (<b>vec3</b>) called <b>" + a.name + "</b> in location <b>" + a.location + "</b>\n";
						else v.remove(e.getKey());
						break;
					case GL20.GL_FLOAT_VEC4:
						if (e.getValue()
						     .getDimension() != 4) errors += "&mdash; this mesh has a <i>dimension mismatch</i> between aux channel (of <b>" + simpleFloatTypeName(e.getValue()
																							    .getDimension()) + "</b>) and shader (<b>vec4</b>) called <b>" + a.name + "</b> in location <b>" + a.location + "</b>\n";
						else v.remove(e.getKey());
						break;
					default:
						errors
							    += "&mdash; shader declares an attribute in location <b>" + a.location + "</b> that Field doesn't support (specifically <b>" + uniformTypeConstants_readable.getOrDefault(
							    a.type, new Pair<>("" + a.type, "")).second + "</b> called <b>" + a.name + "</b> in location <b>" + a.location + "</b>)\n";
				}
			}
		}

		if (v.size() > 0) {
			for (Map.Entry<Integer, Attribute> e : v.entrySet()) {
				errors
					    += "&mdash; this mesh is <i>missing an attribute</i> required by the shader called <b>" + e.getValue().name + "</b> in location <b>" + e.getValue().location + "</b> of type <b>" + uniformTypeConstants_readable.get(
					    e.getValue().type).second + "</b>";
			}
		}

		UniformBundle ub = m.getDefaultBundle();
		Set<field.graphics.Uniform> u = new LinkedHashSet<>(ub.getUniforms()
								      .values());

		Collection<field.graphics.Uniform> u2 = collect(m, x -> x instanceof field.graphics.Uniform);
		u.addAll(u2);

		UniformBundle ubp = s.getDefaultBundle();
		Set<field.graphics.Uniform> up = new LinkedHashSet<>(ubp.getUniforms()
									.values());
		s.children().stream().filter(x -> x instanceof field.graphics.Uniform).forEach(x -> up.add((field.graphics.Uniform)x));
		s.internalScene.values().stream().flatMap(x -> x.stream()).filter(x -> x instanceof field.graphics.Uniform).forEach(x -> up.add((field.graphics.Uniform)x));


		String e2 = checkUniforms("mesh", up, u, convert);
		if (e2 != null) errors += e2;

		return errors.trim()
			     .length() == 0 ? null : errors;
	}

	private String checkUniforms(String kind, Set<field.graphics.Uniform> parentUniforms, Set<field.graphics.Uniform> localUniforms, Function<Object, String> convert) {

		Map<String, Uniform> v = uniforms.stream()
						 .collect(Collectors.toMap(x -> x.name, x -> x));

		String errors = "";

		Set<String> seen = new LinkedHashSet<String>();

		for (field.graphics.Uniform uniform : localUniforms) {

			String n = uniform.getName();
			if (!v.containsKey(n)) {
				errors += "&mdash; this " + kind + " declares an <i>unused uniform</i> called <b>" + n + "</b>, currently of value <b>" + convert.apply(uniform.get()) + "</b>\n";
			} else {
				Uniform un = v.get(n);
				Class ty = uniform.getLastType();
				if (ty == null) errors += "&mdash; this " + kind + " declares a <i>uniform that has no type</i> (an error?)\n";
				if (ty == Float.class && un.type != GL11.GL_FLOAT)
					errors += "&mdash; this " + kind + " <i>declares a uniform with the wrong type</i>. Shader expects a <b>" + uniformTypeConstants_readable.getOrDefault(un.type,
																							       new Pair<>("" + un.type,
																									  "")).second + "</b> for a uniform called <b>" + un.name + "</b>, " + kind + " provided a float (currently, <b>" + convert.apply(
						    uniform.get()) + "</b>)\n";
				else if (ty == Vec2.class && un.type != GL20.GL_FLOAT_VEC2)
					errors += "&mdash; this mesh <i>declares a uniform with the wrong type</i>. Shader expects a <b>" + uniformTypeConstants_readable.getOrDefault(un.type,
																						       new Pair<>("" + un.type,
																								  "")).second + "</b> for a uniform called <b>" + un.name + "</b> , " + kind + " provided a <b>Vec2</b> (currently, <b>" + convert.apply(
						    uniform.get()) + "</b>)\n";
				else if (ty == Vec3.class && un.type != GL20.GL_FLOAT_VEC3)
					errors += "&mdash; this mesh <i>declares a uniform with the wrong type</i>. Shader expects a <b>" + uniformTypeConstants_readable.getOrDefault(un.type,
																						       new Pair<>("" + un.type,
																								  "")).second + "</b> for a uniform called <b>" + un.name + "</b> , " + kind + " provided a <b>Vec3</b> (currently, <b>" + convert.apply(
						    uniform.get()) + "</b>)\n";
				else if (ty == Vec4.class && un.type != GL20.GL_FLOAT_VEC4)
					errors += "&mdash; this mesh <i>declares a uniform with the wrong type</i>. Shader expects a <b>" + uniformTypeConstants_readable.getOrDefault(un.type,
																						       new Pair<>("" + un.type,
																								  "")).second + "</b> for a uniform called <b>" + un.name + "</b> , " + kind + " provided a <b>Vec4</b> (currently, <b>" + convert.apply(
						    uniform.get()) + "</b>)\n";
				else if (ty == Integer.class) {
					// could also be a texture
					if (un.type == GL11.GL_INT) {
					} else if (un.type == GL20.GL_SAMPLER_2D) {
						Texture found = findTexture(((Integer) uniform.get()));
						if (found == null) {
							FBO fbo = findFBO((Integer) uniform.get());
							if (fbo == null) {
								errors
									    += "&mdash; this " + kind + (kind.length() > 0 ? "'s" : "") + " shader is <i>expecting a texture</i> or a single layer FBO at uniform <b>" + un.name + "</b> and instead it got an integer?\n";
							}
							if (fbo.specification.layers != 1) {
								errors
									    += "&mdash; this has an FBO connected bound to unit <b>" + uniform.get() + "</b> called <b>" + un.name + "</b> is of the <i>wrong type</i>. Shader expects a <b>GL_TEXTURE_2D</b>, you've supplied a multi layer FBO (with <b>" + fbo.specification.layers + "!=1</b> layers)\n";
							}
						} else if (found.specification.target != GL11.GL_TEXTURE_2D) {
							errors
								    += "&mdash; this " + kind + " has a texture bound to unit <b>" + uniform.get() + "</b> called <b>" + un.name + "</b> is of the <i>wrong type</i>. Shader expects a <b>GL_TEXTURE_2D</b>, you've supplied a <b>" + found.specification + "</b>\n";
						}
					} else if (un.type == GL30.GL_SAMPLER_2D_ARRAY) {
						FBO fbo = findFBO((Integer) uniform.get());
						if (fbo == null) {
							errors
								    += "&mdash; from this " + kind + ", Field can't find an FBO bound to the <b>GL_TEXTURE_2D_ARRAY</b> uniform called <b>" + un.name + "</b>\n";
						} else if (fbo.specification.layers == 1) {
							errors += "&mdash; this " + kind + (kind.length() > 0 ? "'s" : "") + " has a FBO bound to unit <b>" + convert.apply(
								    uniform.get()) + "</b> called <b>" + un.name + "</b> is of the wrong type. Shader expects a <b>GL_TEXTURE_2D_ARRAY</b>, you've supplied a single layer FBO\n";
						}
					}else if (un.type == GL32.GL_SAMPLER_2D_MULTISAMPLE_ARRAY) {
						FBO fbo = findFBO((Integer) uniform.get());
						if (fbo == null) {
							errors
								    += "&mdash; from this " + kind + ", Field can't find an FBO bound to the <b>GL_TEXTURE_2D_MULTISAMPLE_ARRAY</b> uniform called <b>" + un.name + "</b>\n";
						} else if (fbo.specification.layers == 1) {
							errors += "&mdash; this " + kind + (kind.length() > 0 ? "'s" : "") + " has a FBO bound to unit <b>" + convert.apply(
								    uniform.get()) + "</b> called <b>" + un.name + "</b> is of the wrong type. Shader expects a <b>GL_TEXTURE_2D_MULTISAMPLE_ARRAYffc ccc</b>, you've supplied a single layer FBO\n";
						}
					} else {
						errors
							    += "&mdash; this " + kind + (kind.length() > 0 ? "'s" : "") + " shader has a uniform called <b>" + un.name + "</b> is currently of a type that Field doesn't send (specifically <b>" + uniformTypeConstants_readable.getOrDefault(
							    un.type, new Pair<>("" + un.type, "")).second + "</b>)\n";
					}
				} else if (ty == int[].class && un.type != GL20.GL_INT_VEC2 && un.type != GL11.GL_INT && un.type != GL20.GL_INT_VEC3 && un.type != GL20.GL_INT_VEC4)
					errors += "&mdash; this " + kind + " declares a <i>uniform with the wrong type</i>. Shader expects a <b>" + uniformTypeConstants_readable.getOrDefault(un.type,
																							       new Pair<>("" + un.type,
																									  "")).second + "</b> for a uniform called <b>" + un.name + "</b> , " + kind + " provided an int array of some dimension (currently, <b>" + convert.apply(
						    uniform.get()) + "</b>)\n";
				else if (ty == Mat2.class && un.type != GL20.GL_FLOAT_MAT2)
					errors += "&mdash; this " + kind + " declares a <i>uniform with the wrong type</i>. Shader expects a <b>" + uniformTypeConstants_readable.getOrDefault(un.type,
																							       new Pair<>("" + un.type,
																									  "")).second + "</b> for a uniform called <b>" + un.name + "</b> , " + kind + " provided <b>Mat2</b> (currently, <b>" + convert.apply(
						    uniform.get()) + "</b>)\n";
				else if (ty == Mat3.class && un.type != GL20.GL_FLOAT_MAT3)
					errors += "&mdash; this " + kind + " declares a <i>uniform with the wrong type</i>. Shader expects a <b>" + uniformTypeConstants_readable.getOrDefault(un.type,
																							       new Pair<>("" + un.type,
																									  "")).second + "</b> for a uniform called <b>" + un.name + "</b> , " + kind + " provided <b>Mat3</b> (currently, <b>" + convert.apply(
						    uniform.get()) + "</b>)\n";
				else if (ty == Mat4.class && un.type != GL20.GL_FLOAT_MAT4)
					errors += "&mdash; this " + kind + " declares a <i>uniform with the wrong type</i>. Shader expects a <b>" + uniformTypeConstants_readable.getOrDefault(un.type,
																							       new Pair<>("" + un.type,
																									  "")).second + "</b> for a uniform called <b>" + un.name + "</b> , " + kind + " provided <b>Mat4</b> (currently, <b>" + convert.apply(
						    uniform.get()) + "</b>)\n";
//				else errors += "&mdash; this " + kind + " declares a uniform called <b>" + un.name + "</b> type <b>"+uniformTypeConstants_readable.get(un.type)+" that <i>Field cannot send<i> (currently, <b>" + convert.apply(
//						    uniform.get()) + "</b>)\n";

				v.remove(n);
			}
		}

		parentUniforms.forEach(x -> v.remove(x.getName()));

		if (v.size() > 0) {
			for (Map.Entry<String, Uniform> x : v.entrySet()) {
				errors += "&mdash; this " + kind + " is <i>missing a uniform</i> called <b>" + x.getKey() + "</b> of type <b>" + uniformTypeConstants_readable.getOrDefault(
					    x.getValue().type, new Pair<>("" + x.getValue().type, "")).second + "</b>. It is set neither on the mesh nor on the shader itself.\n";
			}
		}

		return errors.trim()
			     .length() > 0 ? errors : "";
	}

	private Texture findTexture(Integer i) {
		Collection<Scene.Perform> c = collect(s, x -> {
			if (!(x instanceof Texture)) return false;
			return ((Texture) x).specification.unit == i.intValue();
		});
		if (c.size() == 0) return null;
		return (Texture) c.iterator()
				  .next();
	}

	private FBO findFBO(Integer i) {
		Collection<Scene.Perform> c = collect(s, x -> {
			if (!(x instanceof FBO)) return false;
			return ((FBO) x).specification.unit == i.intValue();
		});
		if (c.size() == 0) return null;
		return (FBO) c.iterator()
			      .next();
	}


	private String simpleFloatTypeName(int dimension) {
		switch (dimension) {
			case 1:
				return "float";
			case 2:
				return "vec2";
			case 3:
				return "vec3";
			case 4:
				return "vec4";
			default:
				throw new IllegalArgumentException("" + dimension);
		}
	}

	protected <T extends Scene.Perform> Collection<T> collect(Scene s, Predicate<Scene.Perform> p) {
		Set<T> r = new LinkedHashSet<>();

		s.internalScene.values()
		       .stream()
		       .flatMap(x -> x.stream())
//		       .map(x -> {
//			       System.out.println(x);
//			       return x;
//		       })
		       .forEach(x -> {
			       if (x instanceof Scene.Perform && p.test((Scene.Perform) x)) r.add((T) x);
			       if (x instanceof Scene) r.addAll(collect(((Scene) x), p));
		       });

		return r;
	}

}
