#version 410
layout (points) in;
layout (triangle_strip) out;
layout (max_vertices = 4) out;

out vec4 ovcolor;
in vec4[] vcolor;

in vec2[] pc_q;
out vec2 pc;

in float[] CD;

in int[] id;
uniform int sides;

void main(void)
{
    if (sides==1 && id[0]!=1) return;
    if (sides==2 && id[0]!=0) return;
        int i = 0;

        gl_ClipDistance[0] = CD[0];
        gl_Position = gl_in[0].gl_Position;
        vec2 r = gl_Position.w*pc_q[0].x*vec2(1,1)/100;

        gl_Position = gl_in[0].gl_Position + vec4(-r.x, -r.y, 0, 0);
        ovcolor = vcolor[i];
        pc = vec2(-1,-1);
        EmitVertex();

        gl_Position = gl_in[0].gl_Position + vec4(r.x, -r.y, 0, 0);
        ovcolor = vcolor[i];
        pc = vec2(1,-1);
        EmitVertex();

        gl_Position = gl_in[0].gl_Position + vec4(-r.x, r.y, 0, 0);
        ovcolor = vcolor[i];
        pc = vec2(-1,1);
        EmitVertex();

        gl_Position = gl_in[0].gl_Position + vec4(r.x, r.y, 0, 0);
        ovcolor = vcolor[i];
        pc = vec2(1,1);
        EmitVertex();

    EndPrimitive();
}