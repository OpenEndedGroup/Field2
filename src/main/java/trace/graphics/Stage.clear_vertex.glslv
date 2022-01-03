#version 410
layout(location=0) in vec3 position;

void main()
{
    gl_Position = vec4(position.xy, 0.5, 1);

}