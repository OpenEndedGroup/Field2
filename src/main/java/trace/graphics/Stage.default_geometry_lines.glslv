#version 410
layout (lines) in;
layout (line_strip) out;
layout (max_vertices = 2) out;

out vec4 ovcolor;
in vec4[] vcolor;

in float[] CD;

uniform int sides;
in int[] id;

void main(void)
{
    if (sides==1 && id[0]!=1) return;
    if (sides==2 && id[0]!=0) return;

   for (int i = 0; i < gl_in.length(); i++)
    {
        gl_Position = gl_in[i].gl_Position;
        gl_ClipDistance[0] = CD[i];
        ovcolor = vcolor[i];
        EmitVertex();
    }

    EndPrimitive();
}