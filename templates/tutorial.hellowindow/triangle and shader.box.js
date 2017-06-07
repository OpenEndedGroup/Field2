var MeshBuilder = Java.type('field.graphics.MeshBuilder')
var BaseMesh = Java.type('field.graphics.BaseMesh')

// we'll need a shader
shader = _.newShader()

// this lets us edit the glsl (command-space 'vertex' and 'fragment' and 'geometry')
// reload with ctrl-space 'reload' or with, 'ctrl-r' thank's to this line:
_.shortcut.ctrl_r = () => _.runCommand("reload shader")

// and a mesh (which will start with 0 triangles and 0 vertices)
mesh = BaseMesh.triangleList(0,0)

// and a way of changing its content
builder = mesh.builder()

// connect the mesh to the shader calling it 'myMesh'
shader.myMesh = mesh

// and the shader to the scene calling it 'myShader'
_.scene.myShader = shader

// we'll need some matrices
shader.P = ()=>_.camera.projectionMatrix(0)
shader.V = ()=>_.camera.view(0)

// and other boxes might be interested in the builder and the mesh
_.mesh = mesh
_.builder = builder

//note: all the code in this box is safe to run multiple times. Previous shaders and meshes will get overwritten with new ones and the overwritten ones will get deallocated correctly
