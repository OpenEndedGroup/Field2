#version 410

layout(location=0) out vec4 _output;

uniform sampler2D mainView;

in vec2 tc;

void main()
{
	_output  = texture(mainView, tc);
	_output.w = 1.0;
}