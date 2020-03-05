#version 410
layout(location=0) in vec3 position;
layout(location=1) in vec4 s_Color;

out vec2 tc;

void main()
{
	gl_Position = ( vec4(position.xy*2-vec2(1,1), 0.5, 1.0));

	
	tc = position.xy;
	
}