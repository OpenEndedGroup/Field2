#version 410
layout(location=0) out vec4 _output;

uniform sampler2D T0;
uniform sampler2D T1;

uniform float alpha;

in vec2 ottc;
in vec4 ovcolor;
uniform float opacity;

in vec2 pc;

void main()
{

    vec2 outTC = vec2(ottc.x, ottc.y);

    vec2 tc = outTC;
    ${SPACE_REMAP};

	vec4 C0 = texture(T0, outTC);
	vec4 C1 = texture(T1, outTC);

	vec4 C = C0*(1-alpha)+(alpha)*C1;

	if (ottc.x<=0 || ottc.y<=0 || ottc.x>=1 || ottc.y>=1)
	{
    	discard;
	}

	_output  = vec4(C.xyz,1);
	_output  = _output*ovcolor.xyzw;

   	_output.w *= opacity;

    vec4 outColor = _output;

    ${COLOR_REMAP};

    _output = outColor *smoothstep(0.1, 0.2, (1-(length(pc.xy))));
}
