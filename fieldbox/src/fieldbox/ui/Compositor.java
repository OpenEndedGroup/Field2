package fieldbox.ui;

import field.graphics.*;
import field.linalg.Vec2;
import field.utility.Log;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by marc on 3/24/14.
 */
public class Compositor {

	private final Window window;


	public class Layer {
		private FBO fbo ;
		private Guard guard;

		public Layer(int unit)
		{
			fbo = newFBO(unit);
		}

		public Scene getScene() {
			return fbo.display;
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
				    "void main()\n" +
				    "{\n" +
				    "   gl_Position =  vec4(position.xy, 0.5, 1.0);\n" +
				    "}");

			shader.addSource(Shader.Type.fragment, "#version 410\n" +
				    "layout(location=0) out vec4 _output;\n" +
				    "uniform sampler2D te;\n" +
				    "void main()\n" +
				    "{\n" +
				    "	vec4 t = texelFetch(te, ivec2(gl_FragCoord.xy), 0);" +
				    "\t_output  = vec4(t.xyz, 1);\n" +
				    "\n" +
				    "}");
			s.connect(mesh);
			mesh.connect(shader);
			guard = new Guard(() -> fbo, (i) -> true);
			mesh.connect(guard);
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
				    "void main()\n" +
				    "{\n" +
				    "   gl_Position =  vec4(position.xy, 0.5, 1.0);\n" +
				    "}");

			shader.addSource(Shader.Type.fragment, "#version 410\n" +
				    "layout(location=0) out vec4 _output;\n" +
				    "uniform sampler2D te;\n" +
				    "uniform sampler2D blur;\n" +
				    "void main()\n" +
				    "{\n" +
				    "	vec4 ta = texelFetch(te, ivec2(gl_FragCoord.xy), 0);" +
				    "	vec4 tb = texelFetch(blur, ivec2(gl_FragCoord.xy), 0);" +
				    "	float mix = pow(ta.w, 0.1);"+
				    "	float m2 = pow(ta.w, 0.25);"+
				    //"	_output  = vec4((ta.xyz+(tb.xyz-vec3(0.85))*0.4), mix);\n" +
				    //"	_output  = vec4(ta.xyz*mix+(1-mix)*tb.xyz, mix);\n" +
				    "_output = vec4(tb.xyz*(1-m2)*0.5+m2*ta.xyz, mix);"+
				    "\n" +
				    "}");

			shader.connect(new Uniform<Integer>("te", () -> fbo.specification.unit).setIntOnly(true));
			shader.connect(new Uniform<Integer>("blur", () -> underlayer.fbo.specification.unit).setIntOnly(true));

			s.connect(mesh);
			mesh.connect(shader);
			guard = new Guard(() -> fbo, (i) -> true);
			mesh.connect(guard);
			mesh.connect(new Guard( () -> underlayer.fbo, (i) ->true));
		}

		public void blurXInto(int taps, Scene s)
		{
			blurInto(taps, s, "ivec2(i*2,0)");
		}

		public void blurYInto(int taps, Scene s)
		{
			blurInto(taps, s, "ivec2(0,i*2)");
		}

		protected void blurInto(int taps, Scene s, String access)
		{
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
				    "void main()\n" +
				    "{\n" +
				    "   gl_Position =  vec4(position.xy, 0.5, 1.0);\n" +
				    "}");

			String we = "";
			float[] weight = new float[taps*2+1];
			float tot = 0;
			for(int i=-taps;i<taps+1;i++)
			{
				weight[i+taps] = (float) Math.exp(-Math.pow(1.1f*i/(float)taps,2));
				tot+=weight[i+taps];
			}

			for(int i=-taps;i<taps+1;i++)
			{
				we = we+(weight[i+taps]/tot)+",";
			}
			we = we.substring(0, we.length()-1);
			shader.addSource(Shader.Type.fragment, "#version 410\n" +
				    "layout(location=0) out vec4 _output;\n" +
				    "uniform sampler2D te;\n" +
				    "uniform vec2 bounds;\n" +
				    "ivec2 cl(ivec2 v)"+
				    "{"+
//				    "	return v;\n"+
				    "	return clamp(v, ivec2(0,0), ivec2(bounds));\n"+
				    "}"+
				    "void main()\n" +
				    "{\n" +
				    "	vec4 t = vec4(0);" +
				    "	const float["+(taps*2+1)+"] we = float["+(taps*2+1)+"]("+we+");\n" +
				    "	int n = "+taps+";" +
				    "	for(int i=-n;i<n+1;i++) t+=we[i+n]*texelFetch(te, cl(ivec2(gl_FragCoord.xy)+"+access+"), 0);" +
				    "	_output  = vec4(t.xyz,1);\n" +
				    "\n" +
				    "}");
			shader.connect(new Uniform<Vec2>("bounds", () -> new Vec2(fbo.specification.width-1, fbo.specification.height-1)));
			s.connect(mesh);
			mesh.connect(shader);
			guard = new Guard(() -> fbo, (i) -> true);
			mesh.connect(guard);

		}



	}

	Layer mainLayer;

	Map<String, Layer> layers = new LinkedHashMap<>();

	public Compositor(Window window) {
		this.window = window;
		this.mainLayer = new Layer(0);
		layers.put("__main__", mainLayer);
	}

	public void updateScene() {
		if (GraphicsContext.isResizing) {
			for (Layer l : layers.values()) {
				if (l.fbo.specification.width != window.getWidth() || l.fbo.specification.height != window.getHeight()) {
					l.fbo.finalize();
					Map<Integer, Set<Consumer<Integer>>> sceneWas = l.fbo.display.getScene();
					l.fbo = newFBO(l.fbo.specification.unit);
					l.fbo.display.setScene(sceneWas);

					Log.log("graphics.debug", " scene was :" + sceneWas);
				}
			}
		}
		for (Layer l : layers.values()) {
			l.fbo.draw();
		}
	}

	private FBO newFBO() {
		return newFBO(0);
	}

	private FBO newFBO(int unit) {
		return new FBO(FBO.FBOSpecification.rgbaMultisample(unit, window.getWidth(), window.getHeight()));
//		return new FBO(FBO.FBOSpecification.(unit, window.getWidth(), window.getHeight()));
	}

	public Layer getMainLayer() {
		return getLayer("__main__");
	}

	public Layer newLayer(String name)
	{
		return newLayer(name, 0);
	}

	public Layer newLayer(String name, int unit)
	{
		Layer layer = new Layer(unit);
		layers.put(name, layer);
		return layer;
	}

	public Layer getLayer(String name)
	{
		return layers.get(name);
	}

}
