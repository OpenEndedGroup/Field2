#version 410
layout(location=0) out vec4 _output;
in vec4 vcolor;
in vec2 ttc;
uniform float opacity;
uniform sampler2D source;

void main()
{
    vec4 s = texture(source, ttc/textureSize(source,0));
	_output  = s*vcolor.xyzw;
	_output.w *= opacity;
}
