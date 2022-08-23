package fieldbox.boxes.plugins;

import field.graphics.OffersUniform;
import field.graphics.Shader;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.IdempotencyMap;
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
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extends newFilteredShader to use the shadertoy filter
 */
public class GraphicsSupport_shadertoy extends Box {


    static public Dict.Prop<FunctionOfBox> newShaderToy = new Dict.Prop<>("newShaderToy")
            .doc("creates a shader mostly compatible with ShaderToy; from a box, populating the _vertex_,  _geometry_ and _fragment_ properties with defaults")
            .type()
            .toCanon();

    String preamble = """
            #version 410
            uniform float iTime;
            uniform vec2 iResolution;
            layout(location=0) out vec4 _output;
            """.trim();

    String postamble = """
            void main() {
                vec4 a = vec4(0);
                mainImage(a, gl_FragCoord.xy / iResolution);
                _output = a;
            }
            """;


    private ShaderPreprocessor2 toyProcessor = new ShaderPreprocessor2() {
        @Override
        public int translateVertexLine(int line) {
            return line;
        }

        @Override
        public int translateFragmentLine(int line) {
            return line + preamble.split("\n").length;
        }

        @Override
        public int translateGeometryLine(int line) {
            return line;
        }

        @NotNull
        @Override
        public String processVertexSource(@NotNull Box b, @NotNull String v) {
            return v;
        }

        @NotNull
        @Override
        public String processFragmentSource(@NotNull Box b, @NotNull String v) {
            return preamble+v+"\n"+postamble;
        }

        @NotNull
        @Override
        public String processGeometrySource(@NotNull Box b, @NotNull String v) {
            return v;
        }

        @Override
        public void beginProcess(@NotNull Box b) {

        }

        @NotNull
        @Override
        public String initialVertexShaderSource(@NotNull Box b) {
            return """
                    #version 410
                    layout(location=0) in vec3 position;
                    layout(location=1) in vec4 s_Color;
                                        
                    out vec4 vertexColor;
                                        
                    void main()
                    {
                        gl_Position = ( vec4(position, 1.0));
                                        
                        vertexColor = s_Color;
                    }
                    """.trim();
        }

        @NotNull
        @Override
        public String initialGeometryShaderSource(@NotNull Box b) {
            return "";
        }

        @NotNull
        @Override
        public String initialFragmentShaderSource(@NotNull Box b) {
            return """
                    void mainImage( out vec4 fragColor, in vec2 fragCoord )
                    {
                        fragColor  = vec4(fragCoord.x/, fragCoord.y,1,1);
                    }
                    """.trim();
        }
    };


    public GraphicsSupport_shadertoy(Box root_unused) {

        properties.put(newShaderToy, (FunctionOfBox<Shader>) this::newToy);

    }

    private Shader newToy(Box box) {
        var s = this.find(GraphicsSupport.newFilteredShader, both()).findFirst().get().apply(box, toyProcessor);
        var start = System.currentTimeMillis();
        s.asMap_set("iResolution", new OffersUniform() {
            @Override
            public Object getUniform() {
                return new Vec2(1000.0, 1000.0);
            }
        });
        s.asMap_set("iTime", new OffersUniform() {
            @Override
            public Object getUniform() {
                return new Float((System.currentTimeMillis()-start)/1000.0);
            }
        });
        return s;
    }


}
