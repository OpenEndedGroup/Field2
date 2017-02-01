package fieldbox.ui;

import field.graphics.*;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Options;
import fieldagent.Main;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by marc on 3/24/14.
 */
public class Compositor {

	private final FieldBoxWindow window;

	boolean fadeup = false;
	boolean resizing = true;

	Layer mainLayer;
	Map<String, Layer> layers = new LinkedHashMap<>();
	int fading = 0;

	public Compositor(FieldBoxWindow window) {
		this.window = window;
		this.mainLayer = new Layer(0);
		layers.put("__main__", mainLayer);
	}

	public void updateScene() {
		fadeup = fading++ < 50 && Main.os!=Main.OS.mac;

		if (GraphicsContext.isResizing) {
			for (Layer l : layers.values()) {
				if (l.fbo.specification.width * l.res != window.getWidth() || l.fbo.specification.height * l.res != window.getHeight()) {
					l.fbo.finalize();
					Scene sceneWas = l.fbo.scene;
					l.fbo = newFBO(l.fbo.specification.unit, l.res);
					l.fbo.setScene(sceneWas);
					resizing = true;
					Log.log("graphics.debug", () -> " scene was :" + sceneWas);
				}
			}
		}

		for (Layer l : layers.values()) {
			l.needsRedrawing = Math.max(-1, l.needsRedrawing - 1);
			if (resizing) l.fbo.draw();
			else if (l.needsRedrawing > -1 || fadeup) {
				Log.log("drawing", () -> " drawing dependancies of " + l);
				l.drawDependancies();
				Log.log("drawing", () -> " drawing because dirty " + l);
				l.fbo.draw();
			}
		}

		if (fadeup) {
			window.requestRepaint();
		}
		resizing = false;
	}

	private FBO newFBO() {
		return newFBO(0);
	}

	private FBO newFBO(int unit) {
		if (Options.dict()
			   .isTrue(new Dict.Prop<Boolean>("multisample"), Main.os != Main.OS.mac))
			return new FBO(FBO.FBOSpecification.rgbaMultisampleAndDepth(unit, window.getFrameBufferWidth(), window.getFrameBufferHeight()));

		return new FBO(FBO.FBOSpecification.rgbaAndDepth(unit, window.getFrameBufferWidth(), window.getFrameBufferHeight()));
	}

	private FBO newFBO(int unit, int res) {

		if (res > 1) {
			return new FBO(FBO.FBOSpecification.rgbaAndDepth(unit, window.getFrameBufferWidth() / res, window.getFrameBufferHeight() / res));

		} else {
			if (Options.dict()
				   .isTrue(new Dict.Prop<Boolean>("multisample"), Main.os != Main.OS.mac))
				return new FBO(FBO.FBOSpecification.rgbaMultisampleAndDepth(unit, window.getFrameBufferWidth() / res, window.getFrameBufferHeight() / res));
			return new FBO(FBO.FBOSpecification.rgbaAndDepth(unit, window.getFrameBufferWidth() / res, window.getFrameBufferHeight() / res));
		}
	}

	public Layer getMainLayer() {
		return getLayer("__main__");
	}

	public Layer newLayer(String name) {
		return newLayer(name, 0);
	}

	public Layer newLayer(String name, int unit) {
		Layer layer = new Layer(unit).setName(name);
		layers.put(name, layer);
		return layer;
	}

	public Layer newLayer(String name, int unit, int res) {
		Layer layer = new Layer(unit, res).setName(name);
		layers.put(name, layer);
		return layer;
	}

	public Layer getLayer(String name) {
		return layers.get(name);
	}

	static public class Cache<T> {
		private final Function<T, Long> getMod;
		private final Consumer<T> update;
		T target;
		long mod;
		boolean lock = false;

		public Cache(T target, Function<T, Long> getMod, Consumer<T> update) {
			this.target = target;
			this.mod = getMod.apply(target) - 1;
			this.getMod = getMod;
			this.update = update;
		}

		public void update() {
			if (lock) return;
			try {
				lock = true;
				long m = getMod.apply(target);
				if (m != mod) {
					update.accept(target);
					mod = getMod.apply(target);
				}
			} finally {
				lock = false;
			}
		}
	}

	public class Layer {
		public Map<Layer, Cache<Layer>> dependsOn = new HashMap<>();
		protected int needsRedrawing = 100;
		protected long mod;
		int res;
		String name;
		private FBO fbo;
		private Guard guard;


		public Layer(int unit) {
			fbo = newFBO(unit);
			res = 1;
		}

		public Layer(int unit, int res) {
			fbo = newFBO(unit, res);
			this.res = res;
		}

		public Layer setName(String name) {
			this.name = name;
			return this;
		}

		public FBO getFBO() {
			return fbo;
		}

		public void addDependancy(Layer l) {
			// javaC / IDEA need these casts
			dependsOn.put(l, new Cache<Layer>(l, x -> x.mod, x -> {
				x.drawDependancies();
				Log.log("drawing", () -> "layer:" + x);
				x.fbo.draw();
				x.mod++;
			}));
			l.dependsOn.put(this, new Cache<Layer>(this, x -> x.mod, x -> {
				x.drawDependancies();
				Log.log("drawing", () -> "layer2:" + x);
				x.fbo.draw();
				x.mod++;
			}));
		}

		public void drawDependancies() {
			dependsOn.values()
				 .forEach(x -> x.update());
		}

		public void dirty() {
			needsRedrawing = 1;
		}

		public Scene getScene() {
			return fbo.scene;
		}

		public void drawInto(Scene s) {
			BaseMesh mesh = BaseMesh.triangleList(4, 2);
			MeshBuilder mb = new MeshBuilder(mesh);
			mb.nextVertex(-1, -1, 0);
			mb.nextVertex(1, -1, 0);
			mb.nextVertex(1, 1, 0);
			mb.nextVertex(-1, 1, 0);
			mb.nextElement_quad(0, 1, 2, 3);
			Shader shader = new Shader();

			shader.addSource(Shader.Type.vertex, "#version 410\n" +
				    "layout(location=0) in vec3 position;\n" +
				    "out vec2 tc;\n" +
				    "void main()\n" +
				    "{\n" +
				    "   gl_Position =  vec4(position.xy, 0.5, 1.0);\n" +
				    "	tc = vec2(position.xy+vec2(1,1))/2;\n" +
				    "}");

			shader.addSource(Shader.Type.fragment, "#version 410\n" +
				    "layout(location=0) out vec4 _output;\n" +
				    "uniform sampler2D te;\n" +
				    "uniform float mainAlpha;\n" +
				    "in vec2 tc;\n" +
				    "void main()\n" +
				    "{\n" +
//						"	vec4 t = texelFetch(te, ivec2(gl_FragCoord.xy), 0);\n" +
				    "	vec4 t = texture(te, tc.xy);\n" +
				    "\t_output  = vec4(t.xyz, mainAlpha);\n" +
				    "\n" +
				    "}");
			s.attach(mesh);
			shader.attach(new Uniform<Float>("mainAlpha", () -> fadeup ? 0.1f : 1f));
			mesh.attach(shader);
			guard = new Guard(() -> fbo, (i) -> true);
			mesh.attach(guard);
		}

		public void compositeWith(Layer underlayer, Scene s) {
			BaseMesh mesh = BaseMesh.triangleList(4, 2);
			MeshBuilder mb = new MeshBuilder(mesh);
			mb.nextVertex(-1, -1, 0);
			mb.nextVertex(1, -1, 0);
			mb.nextVertex(1, 1, 0);
			mb.nextVertex(-1, 1, 0);
			mb.nextElement_quad(0, 1, 2, 3);
			Shader shader = new Shader();

			shader.addSource(Shader.Type.vertex, "#version 410\n" +
				    "layout(location=0) in vec3 position;\n" +
				    "out vec2 tc;\n" +
				    "void main()\n" +
				    "{\n" +
				    "   gl_Position =  vec4(position.xy, 0.5, 1.0);\n" +
				    "	tc = vec2(position.xy+vec2(1,1))/2;\n" +
				    "}");

			shader.addSource(Shader.Type.fragment, "#version 410\n" +
				    "layout(location=0) out vec4 _output;\n" +
				    "uniform sampler2D te;\n" +
				    "uniform sampler2D blur;\n" +
				    "in vec2 tc;\n" +
				    "void main()\n" +
				    "{\n" +
				    "	vec4 ta = texture(te, tc);" +
				    "	vec4 tb = texture(blur, tc);" +
				    "	float miz = pow(ta.w, 0.1);" +
				    "	float m2 = pow(ta.w, 0.85);" +
				    "_output = mix(vec4(tb.xyz+ta.xyz, 0), vec4(tb.xyz*ta.xyz+ta.xyz*m2, 1), miz);" +
				    "\n" +
				    "}");

			shader.attach(new Uniform<Integer>("te", () -> fbo.specification.unit).setIntOnly(true));
			shader.attach(new Uniform<Integer>("blur", () -> underlayer.fbo.specification.unit).setIntOnly(true));

			s.attach(mesh);
			mesh.attach(shader);
			guard = new Guard(() -> fbo, (i) -> true);
			mesh.attach(guard);
			mesh.attach(new Guard(() -> underlayer.fbo, (i) -> true));
		}

		public void blurXInto(int taps, Scene s) {
			blurInto(taps, s, "vec2(i*2/1024.0,0)");
		}

		public void blurYInto(int taps, Scene s) {
			blurInto(taps, s, "vec2(0,i*2/1024.0)");
		}
//		public void blurXInto(int taps, Scene s)
//		{
//			blurInto(taps, s, "ivec2(i*2,0)");
//		}
//
//		public void blurYInto(int taps, Scene s)
//		{
//			blurInto(taps, s, "ivec2(0,i*2)");
//		}

		protected void blurInto(int taps, Scene s, String access) {
			BaseMesh mesh = BaseMesh.triangleList(4, 2);
			MeshBuilder mb = new MeshBuilder(mesh);
			mb.nextVertex(-1, -1, 0);
			mb.nextVertex(1, -1, 0);
			mb.nextVertex(1, 1, 0);
			mb.nextVertex(-1, 1, 0);
			mb.nextElement_quad(0, 1, 2, 3);
			Shader shader = new Shader();

			shader.addSource(Shader.Type.vertex, "#version 410\n" +
				    "layout(location=0) in vec3 position;\n" +
				    "out vec2 tc;\n" +
				    "void main()\n" +
				    "{\n" +
				    "   gl_Position =  vec4(position.xy, 0.5, 1.0);\n" +
				    "	tc = vec2(position.xy+vec2(1,1))/2;\n" +
				    "}");

			String we = "";
			float[] weight = new float[taps * 2 + 1];
			float tot = 0;
			for (int i = -taps; i < taps + 1; i++) {
				weight[i + taps] = (float) Math.exp(-Math.pow(1.1f * i / (float) taps, 2));
				tot += weight[i + taps];
			}

			for (int i = -taps; i < taps + 1; i++) {
				we = we + (weight[i + taps] / tot) + ",";
			}
			we = we.substring(0, we.length() - 1);
			shader.addSource(Shader.Type.fragment, "#version 410\n" +
				    "layout(location=0) out vec4 _output;\n" +
				    "uniform sampler2D te;\n" +
				    "uniform vec2 bounds;\n" +
				    "in vec2 tc;\n" +
				    "ivec2 cl(ivec2 v)" +
				    "{" +
//				    "	return v;\n"+
				    "	return clamp(v, ivec2(0,0), ivec2(bounds));\n" +
				    "}" +
				    "void main()\n" +
				    "{\n" +
				    "	vec4 t = vec4(0);" +
				    "	const float[" + (taps * 2 + 1) + "] we = float[" + (taps * 2 + 1) + "](" + we + ");\n" +
				    "	int n = " + taps + ";" +
//				    "	for(int i=-n;i<n+1;i++) t+=we[i+n]*texelFetch(te, cl(ivec2(gl_FragCoord.xy)+"+access+"),0);" +
				    "	for(int i=-n;i<n+1;i++) t+=we[i+n]*texture(te, tc+" + access + ");" +
				    "	_output  = vec4(t.xyz,1);\n" +
				    "\n" +
				    "}");
			shader.attach(new Uniform<Vec2>("bounds", () -> new Vec2(fbo.specification.width - 1, fbo.specification.height - 1)));
			s.attach(mesh);
			mesh.attach(shader);
			guard = new Guard(() -> fbo, (i) -> true);
			mesh.attach(guard);

		}

		@Override
		public String toString() {
			return "layer<" + name + ":" + mod + ">";
		}
	}

}
