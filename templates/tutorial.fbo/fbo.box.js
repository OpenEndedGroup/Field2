var GL11 = Java.type('org.lwjgl.opengl.GL11')
var Camera = Java.type('field.graphics.Camera')
var FBOSpecification = Java.type('field.graphics.FBO.FBOSpecification')
var FBO = Java.type('field.graphics.FBO')


fbo = new FBO(FBOSpecification.rgba(0, 1024, 1024))

_.fboScene = fbo.scene
_.fbo = fbo

camera = new Camera()

camera.advanceState( s => {
	s.aspect=1
	return s
})

_.fboCamera = camera


_.fboScene[-10].clear_viewport_first = () => {
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