package fieldbox.boxes.plugins;

import field.graphics.Shader;
import field.utility.Dict;
import field.utility.Pair;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.execution.Execution;
import fieldbox.execution.InverseDebugMapping;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for helping you write code for the Field graphics system.
 * <p>
 * Three pieces of functionality: commands for swapping the editor into editing properties that correspond to GLSL shader sources (vertex, geometry
 * and fragment); _.newShader() function (injected into the Box graph via a FunctionOfBox implementation) and a command for compiling / reloading a
 * shader from a box.
 */
public class GraphicsSupport extends Box {

	static public Dict.Prop<String> fragment = new Dict.Prop<>("fragment").doc("fragment shader text").type().toCannon();
	static public Dict.Prop<String> geometry = new Dict.Prop<>("geometry").doc("geometry shader text").type().toCannon();
	static public Dict.Prop<String> vertex = new Dict.Prop<>("vertex").doc("vertex shader text").type().toCannon();

	static public Dict.Prop<FunctionOfBox> newShader = new Dict.Prop<>("newShader")
		    .doc("creates a shader from a box, populating the _vertex_,  _geometry_ and _fragment_ properties with defaults").type().toCannon();

	static public Dict.Prop<List<Shader>> _shaders = new Dict.Prop<>("_shaders");

	static {
		FieldBox.fieldBox.io.addFilespec("fragment", ".glslf", "glsl");
		FieldBox.fieldBox.io.addFilespec("vertex", ".glslv", "glsl");
		FieldBox.fieldBox.io.addFilespec("geometry", ".glslg", "glsl");
	}

	public GraphicsSupport(Box root_unused) {


		properties.put(Commands.commands, () -> {
			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			RemoteEditor ed = this.find(RemoteEditor.editor, both()).findFirst().get();

			Box box = ed.getCurrentlyEditing();
			Dict.Prop<String> cep = ed.getCurrentlyEditingProperty();

			if (box == null) return m;

			List<Shader> s = box.properties.get(_shaders);
			if (!cep.equals(fragment) && (box.first(fragment, box.upwards()).isPresent() || s != null))
				m.put(new Pair<>("Edit <i>Fragment</i>", "Switch to editing fragment shader"), () -> {
					ed.setCurrentlyEditingProperty(fragment);
				});

			if (!cep.equals(vertex) && (box.first(vertex, box.upwards()).isPresent() || s != null))
				m.put(new Pair<>("Edit <i>Vertex</i>", "Switch to editing vertex shader"), () -> {
					ed.setCurrentlyEditingProperty(vertex);
				});

			if (!cep.equals(geometry) && (box.first(geometry, box.upwards()).isPresent() || s != null))
				m.put(new Pair<>("Edit <i>Geometry</i>", "Switch to editing geometry shader"), () -> {
					ed.setCurrentlyEditingProperty(geometry);
				});

			if (!cep.equals(Execution.code)) m.put(new Pair<>("Edit <i>Code</i>", "Switch to editing default code property"), () -> {
				ed.setCurrentlyEditingProperty(Execution.code);
			});

			if (s != null) m.put(new Pair<>("Reload shader", "Reloads all " + s.size() + " shader" + (s
				    .size() == 1 ? "" : "s") + " associated with this box via `_.newShader()`"), () -> {
				reload(box, s);
			});


			return m;
		});

		properties.put(newShader, (FunctionOfBox<Shader>)this::newShaderFromBox);

	}

	private void reload(Box b, List<Shader> s) {
		for (Shader ss : s) {
			Map<Shader.Type, Shader.Source> sources = ss.getSources();

			{
				String v = b.properties.get(vertex);
				Shader.Source source = sources.get(Shader.Type.vertex);
				if (source == null && v != null && v.trim().length() > 0) ss.addSource(Shader.Type.vertex, v);
				else if (source != null && v != null) source.source = v;
			}
			{
				String v = b.properties.get(fragment);
				Shader.Source source = sources.get(Shader.Type.fragment);
				if (source == null && v != null && v.trim().length() > 0) ss.addSource(Shader.Type.fragment, v);
				else if (source != null && v != null) source.source = v;
			}
			{
				String v = b.properties.get(geometry);
				Shader.Source source = sources.get(Shader.Type.geometry);
				if (source == null && v != null && v.trim().length() > 0) ss.addSource(Shader.Type.geometry, v);
				else if (source != null && v != null) source.source = v;
			}

		}
	}

	protected Shader newShaderFromBox(Box b) {
		Shader s = new Shader();
		String vs = b.properties
			    .computeIfAbsent(vertex, k -> "#version 410\n" +
					"layout(location=0) in vec3 position;\n" +
					"\n" +
					"out vec4 vertexColor;\n" +
					"in vec4 s_Color;\n" +
					"\n" +
					"void main()\n" +
					"{\n" +
					"\tgl_Position = ( vec4(position, 1.0));\n" +
					"\n" +
					"\tvertexColor = s_Color;\n" +
					"}");
		String fs = b.properties
			    .computeIfAbsent(fragment, k -> "#version 410\n" +
					"\n" +
					"layout(location=0) out vec4 _output;\n" +
					"\n" +
					"void main()\n" +
					"{\n" +
					"\t_output  = vec4(1,1,1,0.1);\n" +
					"}");
		String gs = b.properties.computeIfAbsent(geometry, k -> "");

		s.addSource(Shader.Type.vertex, vs).setOnError(errorHandler(b, "vertex shader"));

		if (gs.trim().length() > 0) s.addSource(Shader.Type.geometry, gs).setOnError(errorHandler(b, "geometry shader"));
		s.addSource(Shader.Type.fragment, fs).setOnError(errorHandler(b, "fragment shader"));

		b.properties.putToList(_shaders, s);

		s.setOnError(errorHandler(b, "shader"));

		InverseDebugMapping.provideExtraInformation(s, "created from "+b);

		return s;
	}

	private Shader.iErrorHandler errorHandler(Box b, String shader) {
		return new Shader.iErrorHandler() {
			@Override
			public void beginError() {

			}

			@Override
			public void errorOnLine(int line, String error) {

				System.out.println(" HAS ERROR :"+b.first(RemoteEditor.outputErrorFactory).isPresent());

				b.first(RemoteEditor.outputErrorFactory)
				 .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is))).apply(b).accept(new Pair<>(line, "Error on "+shader+" reload: "+error));
			}

			@Override
			public void endError() {

			}

			@Override
			public void noError() {
				b.first(RemoteEditor.outputFactory)
				 .orElse((x) -> (is -> System.out.println("message (without remote editor attached) :" + is))).apply(b).accept(shader+" reloaded correctly");
			}
		};
	}
}
