#version 410
layout(location=0) out vec4 _output;
in vec4 ovcolor;
uniform float opacity;

in vec2 ftextureCoordinate;
in vec3 fnormal;


void main()
{
	_output  = ovcolor.xyzw;
	_output.w *= opacity;

}
