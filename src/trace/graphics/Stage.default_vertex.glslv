#version 410

layout(location=0) in vec3 position;
layout(location=1) in vec4 color;
out vec4 vcolor;
uniform vec2 translation;
uniform vec2 scale;
uniform vec2 bounds;
uniform float displayZ;
void main()
{
    vec2 at = (scale.xy*(position.xy+vec2(0.5,0.5))+translation.xy)/bounds.xy;
    gl_Position =  vec4(-1+at.x*2+displayZ*position.z, -1+at.y*2, 0.5, 1.0);
    vcolor = color;
}