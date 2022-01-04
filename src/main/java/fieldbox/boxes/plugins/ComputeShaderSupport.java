package fieldbox.boxes.plugins;

import field.graphics.Shader;
import field.utility.Dict;
import field.utility.Pair;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
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
public class ComputeShaderSupport extends Box {

    static public Dict.Prop<String> compute = new Dict.Prop<>("compute").doc("compute shader text")
            .type()
            .toCanon()
            .set(IO.persistent, true);

    static public Dict.Prop<FunctionOfBox> newComputeShader = new Dict.Prop<>("newComputeShader")
            .doc("creates a shader from a box, populating the _compute_ property with defaults")
            .type()
            .toCanon();





    static {
        FieldBox.fieldBox.io.addFilespec("compute", ".glslc", "glsl");
    }

    public ComputeShaderSupport(Box root_unused) {


        properties.put(Commands.commands, () -> {
            Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
            RemoteEditor ed = this.find(RemoteEditor.editor, both()).findFirst().get();

            Box box = ed.getCurrentlyEditing();
            Dict.Prop<String> cep = ed.getCurrentlyEditingProperty();

            if (box == null) return m;

            List<Shader> s = box.properties.get(GraphicsSupport._shaders);
            if (!cep.equals(compute) && (box.first(compute, box.upwards()).isPresent() || s != null))
                m.put(new Pair<>("Edit Compute", "Switch to editing compute shader"), () -> {
                    ed.setCurrentlyEditingProperty(compute);
                });

            return m;
        });

        properties.put(newComputeShader, (FunctionOfBox<Shader>) this::newShaderFromBox);
    }



    protected Shader newShaderFromBox(Box b) {
        Shader s = new Shader();
        String cs = b.properties
                .computeIfAbsent(compute, k -> "#version 450\nlayout(local_size_x = 1, local_size_y = 1, local_size_z =1) in;\nvoid main(){\n}\n\n");


        s.addSource(Shader.Type.compute, cs).setOnError(GraphicsSupport.errorHandler(b, "compute shader", Shader.Type.vertex));

        b.properties.putToList(GraphicsSupport._shaders, s);

        s.setOnError(GraphicsSupport.errorHandler(b, "shader", Shader.Type.vertex));

        InverseDebugMapping.provideExtraInformation(s, "constructed in box '" + b.properties.get(Box.name) + "'");

        return s;
    }


}
