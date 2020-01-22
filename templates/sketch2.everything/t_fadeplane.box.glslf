#version 410

layout(location=0) out vec4 _output;

uniform vec4 clearColor;

void main()
{
	_output = vec4(clearColor);
}
