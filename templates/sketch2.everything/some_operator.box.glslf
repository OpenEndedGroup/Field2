#version 410
${_._uniform_decl_}
layout(location=0) out vec4 _output;
in vec2 tc;


void main()
{

	vec4 c = someother_operator(tc);
	
	_output  = vec4(c.xyz,1);
}
