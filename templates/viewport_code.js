// a viewport is a canvas that's embedded inside this window

var Camera = Java.type('field.graphics.Camera')
var GL11 = Java.type('org.lwjgl.opengl.GL11')

init = function(x)
{
	GL11.glClearColor(0.2,0,0,1)
	GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
	
	// return true to keep this function executing each frame
	return true;
}

// we put this function early (i.e. -10) into the scene
_.scene[-10].clear = init

// lets other boxes look this box up with "_.canvas"
_.canvas = _

// build a camera for shaders to share here
_.camera = new Camera()
