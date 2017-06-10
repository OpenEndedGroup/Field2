#version 410

layout(location=0) out vec4 _output;

in vec4 vertexColor;

void main()
{
	_output  = vec4(vertexColor.xyz* 0.5266666666666666 ,1);
}