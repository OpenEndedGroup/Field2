#version 410

layout(location=0) out vec4 _output;

uniform sampler2D t1;

in vec2 t;

void main()
{
	vec4 c = texture(t1, t);	
	
	_output  = vec4(c.xyz,1);
}