#version 410
layout(location=0) out vec4 _output;
in vec4 vcolor;
uniform float opacity;
in vec2 ttc;
uniform sampler2DRect source;

void main()
{
    vec2 outTC = vec2(ttc.x, ttc.y);

    vec2 tc = outTC;
    ${SPACE_REMAP};

    vec4 tex = texture(source, outTC*textureSize(source));
    vec4 color = vcolor;

    vec4 outColor = tex*color;
    outColor.w *= opacity;

    ${COLOR_REMAP};

    _output = outColor;
}
