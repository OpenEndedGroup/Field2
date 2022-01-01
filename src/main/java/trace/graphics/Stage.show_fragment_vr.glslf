#version 410
layout(location=0) out vec4 _output;
uniform sampler2D tex;
uniform float disabled;
in vec2 tc;

void main()
{
    vec2 tt = tc;
    _output  = texture(tex, tt);
    _output.w = 1;

//    if (tc.x<0 || tc.x>1)
//        _output = vec4(0.03,0.03,0.03,1);



}
