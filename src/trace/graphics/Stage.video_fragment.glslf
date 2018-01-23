#version 410
layout(location=0) out vec4 _output;

uniform sampler2D T0;
uniform sampler2D T1;

uniform float alpha;

in vec2 ttc;
in vec4 vcolor;
uniform float opacity;

void main()
{

    vec2 outTC = vec2(ttc.x, ttc.y);

    vec2 tc = outTC;
    ${SPACE_REMAP};

	vec4 C0 = texture(T0, outTC);
	vec4 C1 = texture(T1, outTC);

	vec4 C = C0*(1-alpha)+(alpha)*C1;

	if (ttc.x<=0 || ttc.y<=0 || ttc.x>=1 || ttc.y>=1)
	{
    	discard;
	}

	_output  = vec4(C.xyz,1);
	_output  = _output*vcolor.xyzw;

   	_output.w *= opacity;

    vec4 outColor = _output;

    ${COLOR_REMAP};

    _output = outColor;
}
