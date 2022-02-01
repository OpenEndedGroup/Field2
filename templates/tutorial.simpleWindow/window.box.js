var Camera = Java.type('field.graphics.Camera')
var FullScreenWindow = Java.type('field.graphics.FullScreenWindow')
var GL11 = Java.type('org.lwjgl.opengl.GL11')
var SimpleMouse = Java.type("trace.input.SimpleMouse")

// builds a window that's 1024x1024 wide. Note, on a OS X retina screen this will have 2048x2048 pixels in it 
window = new FullScreenWindow(0,0,1024, 1024, null)

// exports the 'scene' of this window as _.scene so other boxes can talk about it
_.scene = window.scene

// attach a function to the scene in this box
_.scene[-10].clear_viewport_first = () => {
	// that clears the background to a dark red
	GL11.glClearColor(0.1, 0.05, 0.05, 1)	
	
	// turn on depth testing
	GL11.glDepthFunc(GL11.GL_LESS)
	GL11.glDisable(GL11.GL_DEPTH_TEST)

	// actual clear the viewport
	GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
	GL11.glEnable(GL11.GL_BLEND)
	if (_.t_constants)
	{
		_.t_constants.updateAllFBO()
	}
	
	// return true to do this function every time this scene is drawn
	return true
}

_.camera = new Camera()

var KeyboardCamera = Java.type('field.graphics.util.KeyboardCamera')

// attach a keyboard control to the viewport's camera (this will also only work when the box is selected)
kc = new KeyboardCamera(_.camera, window)

// set up a standard set of keys for controlling the camera:
// shift-arrow keys orbit the camera left/right and in/out, shift-pg-up / down orbits up and down
// non-shift keys move the camera target as well
kc.standardMap()

_.sceneWindow = window

// let's export some simple mouse handling as _.mouse
_.mouse = new SimpleMouse()
_.sceneWindow.addMouseHandler(_.mouse)

// and, just for readibility, let's call this box 'viewport'
_.name = "window"