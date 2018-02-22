#version 410

layout(location=0) in vec3 position;
layout(location=1) in vec4 color;
layout(location=2) in vec2 tc;

out vec4 vcolor;
out vec2 ttc;

uniform vec2 translation;
uniform vec2 scale;
uniform vec2 bounds;
uniform float displayZ;
uniform vec2 rotator;

uniform mat4 P;
uniform mat4 V;

void main()
{
    vec2 at = ((position.xy+vec2(0.5,0.5))+translation.xy)/bounds.xy;
    gl_Position =  vec4(scale.x*(-1+at.x*2)+displayZ*position.z, scale.y*(-1+at.y*2), 0.5, 1.0);
    gl_Position.xy = vec2(rotator.x*gl_Position.x + rotator.y*gl_Position.y, -rotator.y*gl_Position.x + rotator.x*gl_Position.y);


    gl_Position = P*V*gl_Position;

    vcolor = color;
    ttc = tc;
}