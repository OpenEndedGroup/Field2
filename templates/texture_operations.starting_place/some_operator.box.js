_.textureOperator = true

var MeshBuilder = Java.type('field.graphics.MeshBuilder')
var BaseMesh = Java.type('field.graphics.BaseMesh')

var FBO = Java.type('field.graphics.FBO')
var FBOSpecification = Java.type('field.graphics.FBO.FBOSpecification')


_.fbo = new FBO(FBOSpecification.singleFloat16(_.t_constants.nextUnit(), _.t_constants.width, _.t_constants.height))

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

_.fbo.scene.shader = _.shader

__.doSpaceMenu(_)

_.auto = 10
