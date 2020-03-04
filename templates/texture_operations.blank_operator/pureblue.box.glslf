#version 410
${_._uniform_decl_}
layout(location=0) out vec4 _output;
in vec2 tc;
// -----------------------------------------------------------

void main()
{
	vec4 stageColor = stage(tc);
	
	
	// a nice, 90s style, vertical color gradient
	_output  = vec4(stageColor.rg,1,1);
}
