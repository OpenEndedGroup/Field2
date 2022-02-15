#version 410
layout(location=0) in vec3 position;

uniform mat4 P;
uniform mat4 V;

void main()
{
	gl_Position = P*V*( vec4(position, 1.0));
}