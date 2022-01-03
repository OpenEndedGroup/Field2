#version 410

layout(location=0) in vec3 position;
layout(location=1) in vec4 color;
layout(location=2) in vec2 pointControl;
out vec2 pc_q;
out vec4 vcolor;

layout(location=3) in vec3 normal;
layout(location=4) in vec2 textureCoordinates;

out vec3 onormal;
out vec2 otextureCoordinates;


uniform vec2 translation;
uniform vec2 scale;
uniform vec2 bounds;
uniform float displayZ;

uniform mat4 P;
uniform mat4 V;

uniform vec2 rotator;
uniform mat4 Pl;
uniform mat4 Vl;
uniform mat4 Pr;
uniform mat4 Vr;

uniform float isVR;

out float CD;

uniform float reallyVR;
uniform float vrOptIn;

out int id;

uniform mat4 localTransform;

void main()
{
	// --------------------------- ignore this ---------------------
	// here be dragons
	
    id = gl_InstanceID;
    onormal = normal;
	otextureCoordinates = textureCoordinates;

    vec2 at = ((position.xy+vec2(0.5,0.5))+translation.xy)/bounds.xy;
    gl_Position =  vec4(scale.x*(-1+at.x*2)+displayZ*position.z, scale.y*(-1+at.y*2), position.z/100, 1.0);
    gl_Position.xy = vec2(rotator.x*gl_Position.x + rotator.y*gl_Position.y, -rotator.y*gl_Position.x + rotator.x*gl_Position.y);
    if (vrOptIn>0)
        gl_Position = vec4(position.xyz,1);

    mat4 et = mat4(0);
    if (isVR>0)
    {
        if (reallyVR>0)
        {
            if (gl_InstanceID==1)
            {
                et = transpose(Pl)*transpose(Vl)*V;
            }
            else
            {
                et = transpose(Pr)*transpose(Vr)*V;
            }
        }
        else
        {
            if (gl_InstanceID==0)
            {
                et = (Pl)*(Vl);
            }
            else
            {
                et = (Pr)*(Vr);
            }
        }

    }
    else
    {
        et = P*V;
    }

	onormal = normalize(mat3(localTransform) * onormal);
	
    vec4 ep =  localTransform * gl_Position;

	// --------------------------------	
	// this is a good spot to put some code that changes 
	// the position of vertices
	// 'ep' is the position of the vertex and 'et' is the projection * camera matrix
	// --------------------------------
	
    gl_Position = et*ep;


	
    vcolor = color;
	
	// --------------------------------	
	// this is a good spot to put some code that changes 
	// the color of vertices
	// vcolor.rgba is the color of the vertex
	// sometimes, depending on the model, 'normal' is useful as well
	// --------------------------------
	
	// e.g, a simple two-sided lighting equation
	//vcolor.rgba *= abs(dot(normal, normalize(vec3(1,1,1))));
	
	
	
	// ------------------ dragons again --------------------------
	pc_q = pointControl;
    
	if (isVR>0)
    {
        if (gl_InstanceID==0)
        {
            gl_Position.x = gl_Position.x/2 + 0.5*gl_Position.w;
            CD = gl_Position.x;
        }
        else
        {
            gl_Position.x = gl_Position.x/2 - 0.5*gl_Position.w;
            CD = -gl_Position.x;
        }
    }
	
}