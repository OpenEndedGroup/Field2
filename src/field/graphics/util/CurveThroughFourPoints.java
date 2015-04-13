package field.graphics.util;

import field.graphics.FLinesAndJavaShapes;
import field.linalg.Vec3;

/**
 * Created by marc on 1/18/15.
 */
public class CurveThroughFourPoints {


	static public FLinesAndJavaShapes.CubicSegment3 interpolate(Vec3 p0, double u, Vec3 p1, double v, Vec3 p2, Vec3 p3) {
		double a = 0.0, b = 0.0, c = 0.0, d = 0.0, det = 0.0;
		Vec3 q1 = new Vec3(), q2 = new Vec3();

		if ((u <= 0.0) || (u >= 1.0) || (v <= 0.0) || (v >= 1.0) || (u >= v)) return null; /* failure */

		a = 3 * (1 - u) * (1 - u) * u;
		b = 3 * (1 - u) * u * u;
		c = 3 * (1 - v) * (1 - v) * v;
		d = 3 * (1 - v) * v * v;
		det = a * d - b * c;
		if (det == 0.0) return null; /* failure */

		Vec3[] pos = new Vec3[4];
		for (int i = 0; i < 4; i++) pos[i] = new Vec3();

		pos[0].x = p0.x;
		pos[0].y = p0.y;
		pos[0].z = p0.z;
		pos[3].x = p3.x;
		pos[3].y = p3.y;
		pos[3].z = p3.z;

		q1.x = p1.x - ((1 - u) * (1 - u) * (1 - u) * p0.x + u * u * u * p3.x);
		q1.y = p1.y - ((1 - u) * (1 - u) * (1 - u) * p0.y + u * u * u * p3.y);
		q1.z = p1.z - ((1 - u) * (1 - u) * (1 - u) * p0.z + u * u * u * p3.z);

		q2.x = p2.x - ((1 - v) * (1 - v) * (1 - v) * p0.x + v * v * v * p3.x);
		q2.y = p2.y - ((1 - v) * (1 - v) * (1 - v) * p0.y + v * v * v * p3.y);
		q2.z = p2.z - ((1 - v) * (1 - v) * (1 - v) * p0.z + v * v * v * p3.z);

		pos[1].x = d * q1.x - b * q2.x;
		pos[1].y = d * q1.y - b * q2.y;
		pos[1].z = d * q1.z - b * q2.z;
		pos[1].x /= det;
		pos[1].y /= det;
		pos[1].z /= det;

		pos[2].x = (-c) * q1.x + a * q2.x;
		pos[2].y = (-c) * q1.y + a * q2.y;
		pos[2].z = (-c) * q1.z + a * q2.z;
		pos[2].x /= det;
		pos[2].y /= det;
		pos[2].z /= det;

		return new FLinesAndJavaShapes.CubicSegment3(pos[0], pos[1], pos[2], pos[3]);
	}
}
