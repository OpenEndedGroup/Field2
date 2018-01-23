
// a Viewport is a box that has a full-fledged _.scene and _.camera in it that embeds the complete Field graphics system into a box in the canvas. You can have as many of these as you like, rendering is automatically clipped to the frame of the box

// import some OpenGL
var GL11 = Java.type('org.lwjgl.opengl.GL11')

// attach a function to the scene in this box
_.scene[-10].clear_viewport_first = () => {
	// that clears the background to a dark gray
	GL11.glClearColor(0.1, 0.1, 0.1, 1)	
	
	// turn on depth testing
	GL11.glDepthFunc(GL11.GL_LESS)
	GL11.glDisable(GL11.GL_DEPTH_TEST)
	GL11.glEnable(GL11.GL_BLEND)
	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
	
	// actual clear the viewport
	GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
	
	// return true to do this function every time this scene is drawn
	return true
}

// export this box under the name 'viewport'
_.viewport = _

// and, just for readibility, let's call this box 'viewport'
_.name = "viewport"

// we'll take this viewport and add a "Stage" to it, which is a better, more "pristine" place to draw things:

var Stage = Java.type('trace.graphics.Stage')

var stage = new Stage(1024,1024)

showShader = stage.show("thisStage", _)
_.bindShader(showShader)

stage.background.w=1
stage.background.x=0
stage.background.y=0
stage.background.z=0

// export this as "_.stage"
_.stage = stage

// export it globally as "_.stage"
__.stage = stage

// add a menu item to pop this out into its own window
_.menu.pop_out_w = () => {
	_.bindShader(stage.popOut())
}

// automatically execute all this code when this box is loaded
_.auto = 1
