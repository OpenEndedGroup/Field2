#version 410
layout(location=0) in vec3 position;

out vec2 tc;

void main()
{
    gl_Position = vec4(position.xy, 0.5, 1);

    tc = (position.xy+vec2(1,1))/2;
}