#version 410
layout(location=0) in vec3 position;
layout(location=1) in vec4 s_Color;

out vec4 vertexColor;

uniform mat4 P;
uniform mat4 V;

out vec2 tc;

void main()
{
	gl_Position = P*V*( vec4(position, 1.0));

	vertexColor = s_Color;
	vertexColor.z += position.z;
	
	tc = position.xy;
	
}