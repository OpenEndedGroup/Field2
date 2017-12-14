#version 410
layout(location=0) out vec4 _output;
in vec4 vcolor;
uniform float opacity;
void main()
{
	_output  = vcolor.xyzw;
	_output.w *= opacity;
}
