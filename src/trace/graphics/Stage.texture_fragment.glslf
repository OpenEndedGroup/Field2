#version 410
layout(location=0) out vec4 _output;
in vec4 ovcolor;
uniform float opacity;
in vec2 ottc;

uniform sampler2D source;
uniform sampler2D source2;

flat in int oside;

void main()
{
    vec2 outTC = vec2(ottc.x, ottc.y);

    vec2 tc = outTC;
    ${SPACE_REMAP};

    vec4 tex0 = texture(source, outTC);
    vec4 tex1 = texture(source2, outTC);

    vec4 tex = oside*tex0 + (1-oside)*tex1;

    vec4 color = ovcolor;

    vec4 outColor = tex*color;
    outColor.w *= opacity;

    ${COLOR_REMAP};

    _output = outColor;

//    _output.z = oside==0 ? 1 : 0;

//    _output = vec4(ottc.x,ottc.y,oside,1);


}
