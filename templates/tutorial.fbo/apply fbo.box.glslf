#version 410

layout(location=0) out vec4 _output;

in vec2 tc;

uniform sampler2D t1;
uniform sampler2D t2;

void main()
{
	vec4 c1 = texture(t1, tc);
	
	c1 += texture(t1, tc+vec2(50,0)/textureSize(t1,0).x);
	c1 += texture(t1, tc+vec2(-50,0)/textureSize(t1,0).x);
	c1 += texture(t1, tc+vec2(0,-50)/textureSize(t1,0).x);
	c1 += texture(t1, tc+vec2(0,50)/textureSize(t1,0).x);
	
	c1/=5;
	vec4 c2 = texture(t2, vec2(1-tc.x+0.5, tc.y));
	
	_output  = vec4(1,c1.x,c2.y,1);
}