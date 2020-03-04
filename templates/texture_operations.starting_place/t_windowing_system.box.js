var MeshBuilder = Java.type('field.graphics.MeshBuilder')
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

_.scene.windowShader = _.shader

var s = __.children.stream().filter( b => b.textureOperator && b.here.textureOperator)
s.forEach( a => {
	if (a.fbo)
	{
		_.shader.mainView = a.fbo	
		_.shader.view_a = a.fbo	
		_.shader.view_b = a.fbo	
		_.shader.view_c = a.fbo	
		_.shader.view_d = a.fbo	
	}
})

__.windowSystemShader = _.shader
__.windowSystemShader.viewControls = vec(1,1,1,1)

__.doSpaceMenu = (_) => {
	_.spaceMenu.clear()
	_.spaceMenu.Send_to_main_window_n = () => {
		__.windowSystemShader.mainView = _.fbo
	}
	_.spaceMenu.Send_to_main_window_exclusively_n2 = () => {
		__.windowSystemShader.mainView = _.fbo
		__.windowSystemShader.viewControls = vec(1,1,1,1)*0.0
	}

	_.spaceMenu.A_w = () => {
		__.windowSystemShader.view_a = _.fbo
		__.windowSystemShader.viewControls.x = 1
	}

	_.spaceMenu.B_sw = () => {
		__.windowSystemShader.view_b = _.fbo
		__.windowSystemShader.viewControls.y = 1
	}

	_.spaceMenu.C_se = () => {
		__.windowSystemShader.view_c = _.fbo
		__.windowSystemShader.viewControls.z = 1
	}

	_.spaceMenu.D_e = () => {
		__.windowSystemShader.view_d = _.fbo
		__.windowSystemShader.viewControls.w = 1
	}

	_.spaceMenu.Hide_subwindows_s2 = () => {
		__.windowSystemShader.viewControls = vec(1,1,1,1)*0.0
	}
}