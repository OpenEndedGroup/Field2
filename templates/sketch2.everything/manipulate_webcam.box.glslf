#version 410
${_._uniform_decl_}
layout(location=0) out vec4 _output;
in vec2 tc;

float random (vec2 st) {
    return fract(sin(dot(st.xy,
                         vec2(12.9898,78.233)))*
        43758.5453123);
}

void main()
{
	vec4 c = webcam(tc);
	
	c = c*0.3;
	
	_output  = vec4(c.xyz*1,1);
}


