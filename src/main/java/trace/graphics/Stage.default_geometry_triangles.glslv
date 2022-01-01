#version 410
layout (triangles) in;
layout (triangle_strip) out;
layout (max_vertices = 3) out;

out vec4 ovcolor;
in vec4[] vcolor;

in float[] CD;

in int[] id;
uniform int sides;


in vec3[] onormal;
in vec2[] otextureCoordinates;

out vec2 ftextureCoordinate;
out vec3 fnormal;


void main(void)
{

    // this is the geometry shader, 
    // unless you want to do things to whole triangles
    // that you can't do to individual vertices
    // I'd look elsewhere....

    if (sides==1 && id[0]!=1) return;
    if (sides==2 && id[0]!=0) return;
    int i;

    for (i = 0; i < gl_in.length(); i++)
    {
        gl_Position = gl_in[i].gl_Position;
        gl_ClipDistance[0] = CD[i];
        ovcolor = vcolor[i];

        fnormal = onormal[i];
        ftextureCoordinate = otextureCoordinates[i];

        EmitVertex();
    }

    EndPrimitive();
}