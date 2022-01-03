#version 410
layout(location=0) out vec4 _output;
in vec4 ovcolor;
uniform float opacity;
in vec2 ottc;

uniform sampler2D source;
uniform sampler2D source2;

in vec2 pc;
flat in int oside;

void main()
{
    vec2 outTC = vec2(ottc.x, ottc.y);

    vec2 tc = outTC;
    ${SPACE_REMAP};

    vec4 tex = texture(oside==0 ? source : source2, outTC);
    vec4 color = ovcolor;

    vec4 outColor = tex*color;
    outColor.w *= opacity;

    ${COLOR_REMAP};

    _output = outColor *smoothstep(0.1, 0.2, (1-(length(pc.xy))));
}
