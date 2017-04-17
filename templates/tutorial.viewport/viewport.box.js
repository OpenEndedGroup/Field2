// a Viewport is a box that has a full-fledge _.scene and _.camera in it that embeds the complete Field graphics system into a box in the canvas. You can have as many of these as you like, rendering is automatically clipped to the bounds of the box

var GL11 = Java.type('org.lwjgl.opengl.GL11')

// attach a function to the scene in this box
_.scene[-10].clear_viewport_first = () => {
	// that clears the background to a dark gray
	GL11.glClearColor(0.1, 0.1, 0.1, 1)	
	
	// turn on depth testing
	GL11.glDepthFunc(GL11.GL_LESS)
	GL11.glDisable(GL11.GL_DEPTH_TEST)

	// actual clear the viewport
	GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
	
	// return true to do this function every time this scene is drawn
	return true
}

var KeyboardCamera = Java.type('field.graphics.util.KeyboardCamera')
var ArcBallCamera = Java.type('field.graphics.util.ArcBallCamera')

// attach an "ArcBall" mouse control to the viewport (this will only work when the box is selected)
abc = new ArcBallCamera(_.camera, _)

// attach a keyboard control to the viewport's camera (this will also only work when the box is selected)
kc = new KeyboardCamera(_.camera, _)

// set up a standard set of keys for controlling the camera:
// shift-arrow keys orbit the camera left/right and in/out, shift-pg-up / down orbits up and down
// non-shift keys move the camera target as well
kc.standardMap()

// export this box under the name 'viewport'
_.viewport = _

// and, just for readibility, let's call this box 'viewport'
_.name = "viewport"
