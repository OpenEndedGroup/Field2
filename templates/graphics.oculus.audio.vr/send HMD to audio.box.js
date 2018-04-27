var Mat4 = Java.type('field.linalg.Mat4')
var SimpleOculusTarget = Java.type("trace.graphics.SimpleOculusTarget")
var Space = Java.type("trace.graphics.remote.Space")
var Stage = Java.type('trace.graphics.Stage')

var so = SimpleOculusTarget.Companion
Stage.Companion.rs.nowHeadless()

var layer = _.stage.withName("asdf")

var space = new Space(Stage.Companion.rs)


_r = () => {
	var m = new Mat4(so.o.rightView().get())
	var m2 = new Mat4(layer.__camera.view().get())
	m2 = m2.transpose()

	// optional, if we are looking for this in world space
	m = Mat4.mul(m, m2, new Mat4())
	m.invert()

	a = m.transform(vec(0,0,0,1))
	b = m.transform(vec(0,0,-1,1))
	c = m.transform(vec(0,1,0,1))
	a = vec(a.x, a.y, a.z) * (1/a.w)
	b = vec(b.x, b.y, b.z) * (1/b.w)
	c = vec(c.x, c.y, c.z) * (1/c.w)

	// gaze direction is b-ashader.V = ()=> new Mat4().identity()
	gaze = b-a
	up = c-a
	
	
	space.r.execute("songbird.setListenerPosition("+a.x+", "+a.y+", "+a.z+")")	
	space.r.execute("songbird.setListenerOrientation("+gaze.x+", "+gaze.y+", "+gaze.z+", "+up.x+", "+up.y+", "+up.z+")")	
	
	//_.out(up)
}