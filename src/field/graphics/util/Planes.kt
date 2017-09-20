package field.graphics.util

import field.graphics.MeshBuilder
import field.linalg.Quat
import field.linalg.Vec3
import field.utility.Vec3
import field.utility.minus
import field.utility.plus
import field.utility.remAssign

/**
 * Created by marc on 7/30/17.
 */
class Planes {

	var center = Vec3()
	var x = Vec3(1.0, 0.0, 0.0)
	var y = Vec3(0.0, 1.0, 0.0)
	var size = 1.0

	@JvmOverloads
	fun rotate(axis: Vec3 = Vec3(0, 1, 0), by: Double = 0.0) {
		val q = Quat().fromAxisAngleRad(axis, by);
		q.transform(x)
		q.transform(y)
	}

	@JvmOverloads
	fun rotateAround(axis: Vec3 = Vec3(0, 1, 0), by: Double = 0.0, rotationCenter: Vec3 = Vec3(0,0,0)) {
		val q = Quat().fromAxisAngleRad(axis, by);
		q.transform(x)
		q.transform(y)

		center %= q.transform(center-rotationCenter)+rotationCenter
	}

	fun render(b: MeshBuilder) {
		b.open()
		b.v(center - x - y)
		b.v(center + x - y)
		b.v(center + x + y)
		b.v(center - x + y)
		b.e(0, 1, 2)
		b.e(0, 2, 3)
		b.close()
	}

}