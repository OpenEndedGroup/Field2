#version 410

layout(location=0) in vec3 position;
layout(location=1) in vec4 color;
layout(location=4) in vec2 tc;
layout(location=2) in vec2 pointControl;
out vec2 pc_q;

out vec4 vcolor;
out vec2 ttc;
out int side;

uniform vec2 translation;
uniform vec2 scale;
uniform vec2 bounds;
uniform float displayZ;

uniform vec2 rotator;

uniform mat4 P;
uniform mat4 V;
uniform mat4 Pl;
uniform mat4 Vl;
uniform mat4 Pr;
uniform mat4 Vr;
uniform float isVR;
out float CD;

void main()
{
    vec2 at = ((position.xy+vec2(0.5,0.5))+translation.xy)/bounds.xy;
    gl_Position =  vec4(scale.x*(-1+at.x*2)+displayZ*position.z, scale.y*(-1+at.y*2), position.z/25, 1.0);
    gl_Position.xy = vec2(rotator.x*gl_Position.x + rotator.y*gl_Position.y, -rotator.y*gl_Position.x + rotator.x*gl_Position.y);

     mat4 et = mat4(0);
    if (isVR>0)
    {
        if (gl_InstanceID==0)
        {
            et = Pl*Vl;
            side = 0;
        }
        else
        {
            et = Pr*Vr;
            side = 1;
        }
    }
    else
    {
        et = P*V;
    }

    gl_Position = et*gl_Position;

    if (isVR>0)
    {
        if (gl_InstanceID==0)
        {
            gl_Position.x = gl_Position.x/2 + 0.5*gl_Position.w;
            CD = gl_Position.x;
        }
        else
        {
            gl_Position.x = gl_Position.x/2 - 0.5*gl_Position.w;
            CD = -gl_Position.x;
        }
    }

    pc_q = pointControl;
    vcolor = color;
    ttc = tc;
}