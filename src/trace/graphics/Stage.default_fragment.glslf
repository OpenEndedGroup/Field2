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

	// ----------------
	// this is a good spot to put other manipulations of color
	// ----------------

	// for example:
	//	_output.rgb = ovcolor.rgb*pow( abs(dot(fnormal.rgb, normalize(vec3(1,1,1)))), 2);
	//	_output.rgb += vec3(1,0,0)*pow( max(0,dot(fnormal.rgb, normalize(vec3(1,0,0)))), 2);
}
