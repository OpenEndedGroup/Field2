#version 410
layout(location=0) out vec4 _output;
uniform sampler2D tex;
uniform float disabled;
in vec2 tc;

void main()
{
    vec2 tt = tc;

    tt.y *= 1-15/2205.0;

    if (tt.y<1080.0/2205.0)
    {
        tt.y = tt.y / (1080.0/2205.0);
        tt.x = tt.x / 2.0;

    }
    else if (tt.y<(1080.0+45.0)/2205.0)
    {
        _output = vec4(0.5, 0.3, 0.2, 1);
        return;
    }
    else
    {
        tt.y = (tt.y-(1080.0+45.0)/2205.0)/ (1080.0/2205.0);
        tt.x = tt.x/2 + 0.5;


    }

    _output  = texture(tex, tt);

    _output.w = 1;

//    if (tc.x<0 || tc.x>1)
//        _output = vec4(0.03,0.03,0.03,1);



}
