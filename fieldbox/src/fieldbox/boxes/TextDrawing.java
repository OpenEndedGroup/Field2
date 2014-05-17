package fieldbox.boxes;

import field.graphics.*;
import field.graphics.gdxtext.DrawBitmapFont;
import field.linalg.Vec2;
import field.utility.Dict;
import fieldbox.ui.FieldBoxWindow;
import field.graphics.Window;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by marc on 3/22/14.
 */
public class TextDrawing extends Box {

	public class PerLayer
	{
		protected Shader mainShader;
		private FontSupport defaultFont;
		protected Map<String, FontSupport> fontsLoaded = new LinkedHashMap<String, FontSupport>();
	}
	Map<String, PerLayer> layerLocal = new LinkedHashMap<>();


	public class FontSupport
	{
		public final MeshBuilder mesh;
		public final DrawBitmapFont font;

		public FontSupport(String name, String layer) {
			BaseMesh mesh = BaseMesh.triangleList(4,4);
			this.mesh = new MeshBuilder(mesh);
			font = new DrawBitmapFont(Thread.currentThread().getContextClassLoader().getResource("fonts/"+name).getFile(), this.mesh, 0, 5000);
			mesh.connect(font.getTexture());

			layerLocal.get(layer).mainShader.connect(new Guard(mesh, (p) -> mesh.getVertexLimit() > 0));

			Drawing drawing = first(Drawing.drawing, both()).orElseThrow(() -> new IllegalArgumentException(" where has drawing gone ?"));
			drawing.addBracketable(this.mesh);
		}
	}

	static public final Dict.Prop<TextDrawing> textDrawing = new Dict.Prop<>("textDrawing").toCannon();


	public float smoothing = 0.02f;
	public float gamma = 1.9f;

	public TextDrawing()
	{}

	public Box install(Box root)
	{
		return install(root, "__main__");
	}
	public Box install(Box root, String layerName)
	{
		FieldBoxWindow window = root.first(Boxes.window).orElseThrow(() -> new IllegalArgumentException(" can't draw a box hierarchy with no window to draw it in !"));
		Drawing drawing = root.first(Drawing.drawing).orElseThrow(() -> new IllegalArgumentException(" can't install textdrawing into something without drawing support"));

		properties.put(textDrawing, this);

		PerLayer layer = layerLocal.computeIfAbsent(layerName, (k) -> new PerLayer());

		layer.mainShader = new Shader();

		layer.mainShader.addSource(Shader.Type.vertex, "#version 410\n" +
			    "layout(location=0) in vec3 position;\n" +
			    "layout(location=1) in vec4 color;\n" +
			    "layout(location=3) in vec4 tc;\n" +
			    "out vec4 vertexColor;\n" +
			    "out vec4 vtc;\n" +

			    "uniform vec2 translation;\n" +
			    "uniform vec2 scale;\n" +
			    "uniform vec2 bounds;\n" +

			    "void main()\n" +
			    "{\n" +
			    "	vec2 at = (scale.xy*position.xy+translation.xy)/bounds.xy;\n" +
			    "   gl_Position =  vec4(-1+at.x*2, 1-at.y*2, 0.5, 1.0);\n" +
			    "   vertexColor = color;\n" +
			    "   vtc =tc;\n" +
			    "}");

		layer.mainShader.addSource(Shader.Type.fragment, "#version 410\n" +
			    "layout(location=0) out vec4 _output;\n" +
			    "in vec4 vertexColor;\n" +
			    "in vec4 vtc;\n" +
			    "uniform sampler2D te;\n" +
			    "\n" +
			    "uniform float smoothing;\n" +
			    "uniform float gamma;\n" +
			    "uniform float opacity;\n" +
			    "\n" +
			    "void main()\n" +
			    "{\n" +
			    "\tfloat w = min(0.5, 0.5*0.5/smoothing);\n" +
			    "\n" +
			    "\tvec4 current = texture(te, vtc.xy*(1/512.0),0);\n" +
			    "\tfloat currenta = smoothstep(0.5-w, 0.5+w, current.r);\n" +
			    "\tcurrenta = pow(currenta, 1/gamma);\n" +
			    "\t_output  = vec4(1,1,1,currenta*opacity)*vertexColor;\n" +
			    "\n" +
			    "}");

		layer.mainShader.connect(new Uniform<Vec2>("translation", () -> drawing.getTranslation()));
		layer.mainShader.connect(new Uniform<Vec2>("scale", () -> drawing.getScale()));
		layer.mainShader.connect(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));
		layer.mainShader.connect(new Uniform<Float>("smoothing", () -> smoothing));
		layer.mainShader.connect(new Uniform<Float>("gamma", () -> gamma));
		layer.mainShader.connect(new Uniform<Float>("opacity", drawing::getOpacity));

		window.getCompositor().getLayer(layerName).getScene().connect(layer.mainShader);

		return this;
	}

	public FontSupport getDefaultFont() {
		return getFontSupport("source-sans-pro-regular.fnt");
	}

	public FontSupport getFontSupport(String filename)
	{
		return getFontSupport(filename, "__main__");
	}
	
	public FontSupport getFontSupport(String filename, String layer)
	{
		return layerLocal.get(layer).fontsLoaded.computeIfAbsent(filename, (k) -> new FontSupport(filename, layer));
	}



}
