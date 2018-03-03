#version 410
layout (lines) in;
layout (line_strip) out;
layout (max_vertices = 2) out;

out vec4 ovcolor;
in vec4[] vcolor;

void main(void)
{
    for (int i = 0; i < gl_in.length(); i++)
    {
        gl_Position = gl_in[i].gl_Position;
        ovcolor = vcolor[i];
        EmitVertex();
    }

    EndPrimitive();
}