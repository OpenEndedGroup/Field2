#version 410

layout(location=0) out vec4 _output;

uniform sampler2D inputTexture;
in vec2 tc;

void main()
{
	vec4 c = texture(inputTexture, tc);
	_output  = vec4(c.xyz,1);
		
}
