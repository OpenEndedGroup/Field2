#version 410
layout(location=0) out vec4 _output;
in vec4 ovcolor;
uniform float opacity;
in vec2 ottc;

uniform sampler2D source;
uniform sampler2D source2;
uniform vec3 leftOffset;

flat in int oside;

vec2 rot(vec2 a, float angle)
{
    mat2 m = mat2(cos(angle), sin(angle), -sin(angle), cos(angle));
    return m*(a-vec2(0.5, 0.5))+vec2(0.5, 0.5);
}

void main()
{
    vec2 outTC = vec2(ottc.x, ottc.y);

    vec2 tc = outTC;
    ${SPACE_REMAP};

    vec4 tex0 = texture(source, rot(outTC-leftOffset.xy, leftOffset.z));
    vec4 tex1 = texture(source2, rot(outTC+leftOffset.xy, -leftOffset.z));

    vec4 tex = oside*tex0 + (1-oside)*tex1;

    vec4 color = ovcolor;

    vec4 outColor = tex*color;
    outColor.w *= opacity;

    ${COLOR_REMAP};

    _output = outColor;

//    _output.z = oside==0 ? 1 : 0;

//    _output = vec4(ottc.x,ottc.y,oside,1);


}
