#version 410

layout(location=0) out vec4 _output;

uniform sampler2D mainView;
uniform sampler2D view_a;
uniform sampler2D view_b;
uniform sampler2D view_c;
uniform sampler2D view_d;

uniform vec4 viewControls;

in vec2 tc;

void main()
{
	_output  = texture(mainView, tc);
	
	if (tc.y<0.25)
	{
		if (tc.x>=0 && tc.x<=0.25)
		{
			vec2 ttc = tc;
			ttc.xy *= 4;
			_output = _output*(1.0-viewControls.x)+viewControls.x*texture(view_a, ttc);
		}
		if (tc.x>=0.25 && tc.x<=0.5)
		{
			vec2 ttc = tc;
			ttc.xy *= 4;
			ttc.x -= 1;
			_output = _output*(1.0-viewControls.y)+viewControls.y*texture(view_b, ttc);
		}
		if (tc.x>=0.5 && tc.x<=0.75)
		{
			vec2 ttc = tc;
			ttc.xy *= 4;
			ttc.x -= 2;
			_output = _output*(1.0-viewControls.z)+viewControls.z*texture(view_c, ttc);
		}
		if (tc.x>=0.75 && tc.x<=1)
		{
			vec2 ttc = tc;
			ttc.xy *= 4;
			ttc.x -= 3;
			_output = _output*(1.0-viewControls.w)+viewControls.w*texture(view_d, ttc);
		}
	}
	
	_output.w = 1.0;
}