var GL11 = Java.type('org.lwjgl.opengl.GL11')
var BaseMesh = Java.type('field.graphics.BaseMesh')

_.shader = _.newShader()
_.mesh = BaseMesh.triangleList(0,0)
_.builder = _.mesh.builder()

_.shader.mesh = _.mesh

_.builder.open()
_.builder.v(0,0,0)
_.builder.v(1,0,0)
_.builder.v(1,1,0)
_.builder.v(0,1,0)
_.builder.e(0,1,2) 
_.builder.e(0,2,3) 
_.builder.close()

_.shader.clearColor = vec(1,1,1,0.01)
__.fadeplane = _.shader


__.fadeNow = (red, green, blue, amount) => {
	_.fadeplane.clearColor = vec(red, green, blue, amount)
	GL11.glEnable(GL11.GL_BLEND)
	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
	_.fadeplane.perform(-2)
}
