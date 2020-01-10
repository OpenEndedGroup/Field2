package fieldbox.boxes.plugins;

import field.graphics.Shader;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.ShaderPreprocessor;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.execution.Execution;
import fieldbox.execution.InverseDebugMapping;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for helping you write code for the Field graphics system.
 * <p>
 * Three pieces of functionality: commands for swapping the editor into editable properties that correspond to GLSL shader sources (vertex, geometry
 * and fragment); a _.newShader() function that associates a shader with a box; and finally a command for compiling / reloading all shaders associated with this box.
 */
public class GraphicsSupport extends Box {

    static public Dict.Prop<String> fragment = new Dict.Prop<>("fragment").doc("fragment shader text")
            .type()
            .toCanon()
            .set(IO.persistent, true);
    static public Dict.Prop<String> geometry = new Dict.Prop<>("geometry").doc("geometry shader text")
            .type()
            .toCanon()
            .set(IO.persistent, true);
    static public Dict.Prop<String> vertex = new Dict.Prop<>("vertex").doc("vertex shader text")
            .type()
            .toCanon()
            .set(IO.persistent, true);

    static public Dict.Prop<FunctionOfBox> newShader = new Dict.Prop<>("newShader")
            .doc("creates a shader from a box, populating the _vertex_,  _geometry_ and _fragment_ properties with defaults")
            .type()
            .toCanon();

    static public Dict.Prop<BiFunctionOfBoxAnd<ShaderPreprocessor2, Shader>> newFilteredShader = new Dict.Prop<>("newFilteredShader")
            .doc("creates a shader from a box, populating the _vertex_,  _geometry_ and _fragment_ properties with defaults, mediated by a Shader Preprocessor")
            .type()
            .toCanon();

    static public Dict.Prop<BiFunctionOfBoxAnd<Shader, Object>> bindShader = new Dict.Prop<>("bindShader")
            .doc("adds an existing Shader to this box, populating the _vertex_,  _geometry_ and _fragment_ properties with the source code for the shader, and letting 'reload shader' work")
            .type()
            .toCanon();

    static public Dict.Prop<FunctionOfBox<String>> reloadShaders = new Dict.Prop<>("reloadShaders").type().toCanon()
            .doc("`_.reloadShaders()` reloads all shaders associated with this box");

	static public Dict.Prop<List<Shader>> _shaders = new Dict.Prop<>("_shaders");
	static public Dict.Prop<ShaderPreprocessor2> _preprocessor = new Dict.Prop<>("_preprocessor");



    static {
        FieldBox.fieldBox.io.addFilespec("fragment", ".glslf", "glsl");
        FieldBox.fieldBox.io.addFilespec("vertex", ".glslv", "glsl");
        FieldBox.fieldBox.io.addFilespec("geometry", ".glslg", "glsl");
    }

    public GraphicsSupport(Box root_unused) {
        properties.put(reloadShaders, (b) -> {
            List<Shader> s = b.properties.get(_shaders);
            reload(b, s);
            return "";
        });

        properties.put(Commands.commands, () -> {
            Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
            RemoteEditor ed = this.find(RemoteEditor.editor, both()).findFirst().get();

            Box box = ed.getCurrentlyEditing();
            Dict.Prop<String> cep = ed.getCurrentlyEditingProperty();

            if (box == null) return m;

            List<Shader> s = box.properties.get(_shaders);
            if (!cep.equals(fragment) && (box.first(fragment, box.upwards()).isPresent() || s != null))
                m.put(new Pair<>("Edit Fragment", "Switch to editing fragment shader"), () -> {
                    ed.setCurrentlyEditingProperty(fragment);
                });

            if (!cep.equals(vertex) && (box.first(vertex, box.upwards()).isPresent() || s != null))
                m.put(new Pair<>("Edit Vertex", "Switch to editing vertex shader"), () -> {
                    ed.setCurrentlyEditingProperty(vertex);
                });

            if (!cep.equals(geometry) && (box.first(geometry, box.upwards()).isPresent() || s != null))
                m.put(new Pair<>("Edit Geometry", "Switch to editing geometry shader"), () -> {
                    ed.setCurrentlyEditingProperty(geometry);
                });

            if (!cep.equals(Execution.code))
                m.put(new Pair<>("Edit Code", "Switch to editing default code property"), () -> {
                    ed.setCurrentlyEditingProperty(Execution.code);
                });

            if (s != null) m.put(new Pair<>("Reload shader", "Reloads all " + s.size() + " shader" + (s
                    .size() == 1 ? "" : "s") + " associated with this box via `_.newShader()`"), () -> {
                reload(box, s);
            });


            return m;
        });

        properties.put(newShader, (FunctionOfBox<Shader>) this::newShaderFromBox);
        properties.put(newFilteredShader, (BiFunctionOfBoxAnd<ShaderPreprocessor2, Shader>) this::newShaderFromBoxProcessor);
        properties.put(bindShader, (BiFunctionOfBoxAnd<Shader, Object>) this::bindShaderToBox);

    }

    static public Shader.iErrorHandler errorHandler(Box b, String shader, Shader.Type inside) {
        return new Shader.iErrorHandler() {
            @Override
            public void beginError() {

            }

            @Override
            public void errorOnLine(int line, String error) {

                ShaderPreprocessor2 proc2 = b.properties.get(_preprocessor);
                if (proc2!=null)
                {
                    if (inside==Shader.Type.vertex)
                        line = proc2.translateVertexLine(line);
                    else if (inside==Shader.Type.fragment)
                        line = proc2.translateFragmentLine(line);
                    else if (inside==Shader.Type.geometry)
                        line = proc2.translateGeometryLine(line);
                }

                b.first(RemoteEditor.outputErrorFactory)
                        .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is)))
                        .apply(b)
                        .accept(new Pair<>(line, "Error on " + shader + " reload: " + error));
            }

            @Override
            public void endError() {

            }

            @Override
            public void noError() {
                b.first(RemoteEditor.outputFactory)
                        .orElse((x) -> (is -> System.out.println("message (without remote editor attached) :" + is)))
                        .apply(b)
                        .accept(shader + " reloaded correctly");
            }
        };
    }

    private void reload(Box b, List<Shader> s) {

        ShaderPreprocessor pre = new ShaderPreprocessor();

        ShaderPreprocessor2 proc2 = b.properties.get(_preprocessor);

        if (proc2!=null)
            proc2.beginProcess(b);

        for (Shader ss : s) {
            Map<Shader.Type, Shader.Source> sources = ss.getSources();
            {
                String v = b.properties.get(vertex);

				if (proc2!=null)
					v = proc2.processVertexSource(b, v);

                Shader.Source source = sources.get(Shader.Type.vertex);
                if (source == null && v != null && v.trim().length() > 0)
                    ss.addSource(Shader.Type.vertex, pre.new Preprocess(b, v));
                else if (source != null && v != null) source.source = pre.new Preprocess(b, v);
            }
            {
                String v = b.properties.get(fragment);

				if (proc2!=null)
					v = proc2.processFragmentSource(b, v);

                Shader.Source source = sources.get(Shader.Type.fragment);
                if (source == null && v != null && v.trim().length() > 0)
                    ss.addSource(Shader.Type.fragment, pre.new Preprocess(b, v));
                else if (source != null && v != null) source.source = pre.new Preprocess(b, v);
            }
            {
                String v = b.properties.get(geometry);

				if (proc2!=null)
					v = proc2.processGeometrySource(b, v);

                Shader.Source source = sources.get(Shader.Type.geometry);
                if (source == null && v != null && v.trim().length() > 0)
                    ss.addSource(Shader.Type.geometry, pre.new Preprocess(b, v));
                else if (source != null && v != null) source.source = pre.new Preprocess(b, v);
            }

        }

        Drawing.dirty(b, 2);
    }

    protected Shader newShaderFromBox(Box b) {
        ShaderPreprocessor pre = new ShaderPreprocessor();
        Shader s = new Shader();
        String vs = pre.preprocess(b, b.properties
                .computeIfAbsent(vertex, k -> "#version 410\n" +
                        "layout(location=0) in vec3 position;\n" +
                        "layout(location=1) in vec4 s_Color;\n" +
                        "\n" +
                        "out vec4 vertexColor;\n" +
                        "\n" +
                        "void main()\n" +
                        "{\n" +
                        "\tgl_Position = ( vec4(position, 1.0));\n" +
                        "\n" +
                        "\tvertexColor = s_Color;\n" +
                        "}"));
        String fs = pre.preprocess(b, b.properties
                .computeIfAbsent(fragment, k -> "#version 410\n" +
                        "\n" +
                        "layout(location=0) out vec4 _output;\n" +
                        "\n" +
                        "void main()\n" +
                        "{\n" +
                        "\t_output  = vec4(1,1,1,0.1);\n" +
                        "}"));
        String gs = b.properties.computeIfAbsent(geometry, k -> "");

        s.addSource(Shader.Type.vertex, vs).setOnError(errorHandler(b, "vertex shader", Shader.Type.vertex));

        if (gs.trim().length() > 0)
            s.addSource(Shader.Type.geometry, gs).setOnError(errorHandler(b, "geometry shader", Shader.Type.geometry));
        s.addSource(Shader.Type.fragment, fs).setOnError(errorHandler(b, "fragment shader", Shader.Type.fragment));

        b.properties.putToList(_shaders, s);

        s.setOnError(errorHandler(b, "shader", Shader.Type.vertex));

        InverseDebugMapping.provideExtraInformation(s, "constructed in box '" + b.properties.get(Box.name) + "'");

        return s;
    }

    protected Shader newShaderFromBoxProcessor(Box b, ShaderPreprocessor2 proc) {

        Shader s = new Shader();
        proc.beginProcess(b);
        String vs = proc.processVertexSource(b, b.properties
                .computeIfAbsent(vertex, k -> proc.initialVertexShaderSource(b)));
        String fs = proc.processFragmentSource(b, b.properties
                .computeIfAbsent(fragment, k ->  proc.initialFragmentShaderSource(b)));
        String gs = proc.processGeometrySource(b, b.properties.computeIfAbsent(geometry, k ->  proc.initialGeometryShaderSource(b)));

        s.addSource(Shader.Type.vertex, vs).setOnError(errorHandler(b, "vertex shader",Shader.Type.vertex));

        if (gs.trim().length() > 0)
            s.addSource(Shader.Type.geometry, gs).setOnError(errorHandler(b, "geometry shader",Shader.Type.geometry));
        s.addSource(Shader.Type.fragment, fs).setOnError(errorHandler(b, "fragment shader",Shader.Type.fragment));

        b.properties.putToList(_shaders, s);

        s.setOnError(errorHandler(b, "shader", Shader.Type.vertex));

        b.properties.put(_preprocessor, proc);

        InverseDebugMapping.provideExtraInformation(s, "constructed in box '" + b.properties.get(Box.name) + "'");

        return s;
    }

    protected Shader bindShaderToBox(Box b, Shader s) {
        Shader.Source v = s.getSources().get(Shader.Type.vertex);
        Shader.Source g = s.getSources().get(Shader.Type.geometry);
        Shader.Source f = s.getSources().get(Shader.Type.fragment);

        boolean needsReload = false;

        if (v != null && v.source != null && v.source.get().trim().length() != 0) {
            if (!b.properties.has(vertex)) {
                if (v.source instanceof ShaderPreprocessor.Preprocess)
                    b.properties.put(vertex, ((ShaderPreprocessor.Preprocess) v.source).getSource());
                else
                    b.properties.put(vertex, v.source.get());
            } else {
                needsReload = true;
            }
            v.setOnError(errorHandler(b, "vertex shader", Shader.Type.vertex));
        }

        if (g != null && g.source != null && g.source.get().trim().length() != 0) {
            if (!b.properties.has(geometry)) {
                if (g.source instanceof ShaderPreprocessor.Preprocess)
                    b.properties.put(geometry, ((ShaderPreprocessor.Preprocess) g.source).getSource());
                else
                    b.properties.put(geometry, g.source.get());
            } else {
                needsReload = true;
            }

            g.setOnError(errorHandler(b, "geometry shader", Shader.Type.geometry));
        }

        if (f != null && f.source != null && f.source.get().trim().length() != 0) {
            if (!b.properties.has(fragment)) {
                if (f.source instanceof ShaderPreprocessor.Preprocess)
                    b.properties.put(fragment, ((ShaderPreprocessor.Preprocess) f.source).getSource());
                else
                    b.properties.put(fragment, f.source.get());
            } else {
                needsReload = true;
            }
            f.setOnError(errorHandler(b, "fragment shader",Shader.Type.fragment));
        }

        b.properties.putToList(_shaders, s);

        s.setOnError(errorHandler(b, "shader",Shader.Type.vertex));

        InverseDebugMapping.provideExtraInformation(s, "bound to box '" + b.properties.get(Box.name) + "'");

        if (needsReload) {
            List<Shader> ss = b.properties.get(_shaders);

            reload(b, ss);
        }

        return s;
    }
}
