#version 410
layout(location=0) out vec4 _output;
in vec4 ovcolor;
in vec2 pc;
uniform float opacity;
void main()
{
	_output  = ovcolor.xyzw;
	_output.w *= opacity *smoothstep(0.1, 0.2, (1-(length(pc.xy))));

}
