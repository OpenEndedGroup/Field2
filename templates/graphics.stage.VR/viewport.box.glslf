#version 410
layout(location=0) out vec4 _output;
uniform sampler2D tex;
uniform float disabled;
in vec2 tc;

vec2 remapTexture(vec2 src)
{
    vec2 dest = src;

	dest.y = 1-dest.y;
	
    return dest;
}

void main()
{
    _output  = texture(tex, remapTexture(tc));

    _output.w = 1;

    if (tc.x<0 || tc.x>1)
        _output = vec4(0.03,0.03,0.03,1);

    if (disabled>0)
    {
        float f = mod(gl_FragCoord.x-gl_FragCoord.y,20)/20.0;
        f= (sin(f*3.14*2)+1)/2;
        f = (smoothstep(0.5, 0.55, f)+1)/2;
        _output.w *= f;
        _output.z *= (1-f/3);
        _output.x += f/5;
    }
}
