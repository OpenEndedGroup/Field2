#version 410
layout(location=0) out vec4 _output;

uniform sampler2D T0;
uniform sampler2D T1;

uniform float alpha;

in vec2 ottc;
in vec4 ovcolor;
uniform float opacity;

uniform vec3 blur;
uniform vec3 colorAdd;
uniform vec3 colorMul;

void main()
{
    vec2 outTC = vec2(ottc.x, ottc.y);

    vec2 tc = outTC;
    ${SPACE_REMAP};

	vec4 C0 = texture(T0, outTC);
	vec4 C1 = texture(T1, outTC);


    int i = 1;

    for(i=1;i<blur.z+1;i++)
    {
        C0 += texture(T0, outTC+blur.xy*i);
    	C1 += texture(T1, outTC+blur.xy*i);
    }


    C0 /= i;
    C1 /= i;

    vec4 C = C0*(1-alpha)+(alpha)*C1;


	float xx = fract(ottc.x*2);
	float yy = ottc.y;
	
	xx = max(0, xx);
	yy = max(0, yy);

	float vig = xx*(1-xx)*yy*(1-yy)*4*4;

	vig = pow(vig, 0.1);
	
	
	if (ottc.x<=0 || ottc.y<=0 || ottc.x>=1 || ottc.y>=1)
	{
    	discard;
	}

	_output  = vec4(C.xyz,1);
	_output  = _output*ovcolor.xyzw*vec4(colorMul, 1.0) + vec4(colorAdd, 0.0);

   	_output.w *= opacity;

    vec4 outColor = _output;

    ${COLOR_REMAP};

    _output = outColor;
	
	
	_output.xyz *= _output.xyz;
	
	
	_output.w *= vig;
}
