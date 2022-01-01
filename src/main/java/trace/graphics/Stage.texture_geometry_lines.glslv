#version 410
layout (lines) in;
layout (line_strip) out;
layout (max_vertices = 2) out;

out vec4 ovcolor;
out vec2 ottc;
in vec4[] vcolor;
in vec2[] ttc;

in float[] CD;
in int[] side;
flat out int oside;

in int[] id;
uniform int sides;

void main(void)
{
    if (sides==1 && id[0]!=1) return;
    if (sides==2 && id[0]!=0) return;
    int i;

    oside = side[0];

    for (i = 0; i < gl_in.length(); i++)
    {
        gl_Position = gl_in[i].gl_Position;
        gl_ClipDistance[0] = CD[i];
        ovcolor = vcolor[i];
        ottc = ttc[i];
        EmitVertex();
    }

    EndPrimitive();
}