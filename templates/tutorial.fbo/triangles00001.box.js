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
_.fboScene.shader = shader

// we'll need some matrices
shader.P = ()=>_.fboCamera.projectionMatrix(0)
shader.V = ()=>_.fboCamera.view(0)

// and other boxes might be interested in the builder and the mesh
_.mesh = mesh
_.builder = builder