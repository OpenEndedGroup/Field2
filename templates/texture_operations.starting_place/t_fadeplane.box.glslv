#version 410
layout(location=0) in vec3 position;
layout(location=1) in vec4 s_Color;

out vec4 vertexColor;

void main()
{
	gl_Position = ( vec4(position*2 -vec3(1,1,0), 1.0));

	vertexColor = s_Color;
}