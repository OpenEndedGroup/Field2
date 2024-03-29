#version 410
layout (triangles) in;
layout (triangle_strip) out;
layout (max_vertices = 3) out;

out vec4 ovcolor;
out vec2 ottc;
in vec4[] vcolor;
in vec2[] ttc;

void main(void)
{
    int i;

    for (i = 0; i < gl_in.length(); i++)
    {
        gl_Position = gl_in[i].gl_Position;
        ovcolor = vcolor[i];
        ottc = ttc[i];
        EmitVertex();
    }

    EndPrimitive();
}