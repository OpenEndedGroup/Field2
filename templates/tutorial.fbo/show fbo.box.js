var MeshBuilder = Java.type('field.graphics.MeshBuilder')
var BaseMesh = Java.type('field.graphics.BaseMesh')

// we'll need a shader
shader = _.newShader()

// and a mesh (which will start with 0 triangles and 0 vertices)
mesh = BaseMesh.triangleList(0,0)

// and a way of changing its content
builder = mesh.builder()


// connect the mesh to the shader
shader.myMesh = mesh

// and the shader to the scene
_.scene.shader = shader

// and other boxes might be interested in the builder and the mesh
_.mesh = mesh
_.builder = builder


builder.open()
builder.v(0,0,0)
builder.v(1,0,0)
builder.v(1,1,0)
builder.v(0,1,0)
builder.e(0,1,2) 
builder.e(0,2,3) 
builder.close()


shader.t = _.fbo
_.fbo