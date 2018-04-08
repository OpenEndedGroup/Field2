#version 410
layout (points) in;
layout (triangle_strip) out;
layout (max_vertices = 4) out;

out vec4 ovcolor;
in vec4[] vcolor;

in vec2[] pc_q;
out vec2 pc;

out vec2 ottc;
in vec2[] ttc;

in float[] CD;
in int[] side;
flat out int oside;

void main(void)
{
        int i = 0;
        oside = side[0];

        gl_Position = gl_in[0].gl_Position;
        gl_ClipDistance[0] = CD[i];
        vec2 r = gl_Position.w*pc_q[0].x*vec2(1,1)/100;

        gl_Position = gl_in[0].gl_Position + vec4(-r.x, -r.y, 0, 0);
        ovcolor = vcolor[i];
        pc = vec2(-1,-1);
        ottc = ttc[i];
        EmitVertex();

        gl_Position = gl_in[0].gl_Position + vec4(r.x, -r.y, 0, 0);
        ovcolor = vcolor[i];
        pc = vec2(1,-1);
        ottc = ttc[i];
        EmitVertex();

        gl_Position = gl_in[0].gl_Position + vec4(-r.x, r.y, 0, 0);
        ovcolor = vcolor[i];
        pc = vec2(-1,1);
        ottc = ttc[i];
        EmitVertex();

        gl_Position = gl_in[0].gl_Position + vec4(r.x, r.y, 0, 0);
        ovcolor = vcolor[i];
        pc = vec2(1,1);
        ottc = ttc[i];
        EmitVertex();

    EndPrimitive();
}