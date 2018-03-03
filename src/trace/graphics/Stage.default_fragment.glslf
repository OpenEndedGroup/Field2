#version 410
layout(location=0) out vec4 _output;
in vec4 ovcolor;
uniform float opacity;
void main()
{
	_output  = ovcolor.xyzw;
	_output.w *= opacity;

}
