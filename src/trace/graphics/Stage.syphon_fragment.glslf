#version 410
layout(location=0) out vec4 _output;
in vec4 ovcolor;
uniform float opacity;
in vec2 ottc;
uniform sampler2DRect source;

void main()
{
    vec2 outTC = vec2(ottc.x, ottc.y);

    vec2 tc = outTC;
    ${SPACE_REMAP};

    vec4 tex = texture(source, outTC*textureSize(source));
    vec4 color = ovcolor;

    vec4 outColor = tex*color;
    outColor.w *= opacity;

    ${COLOR_REMAP};

    _output = outColor;
}
