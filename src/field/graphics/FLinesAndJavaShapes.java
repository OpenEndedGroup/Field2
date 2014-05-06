package field.graphics;

import field.linalg.Vec2;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

public class FLinesAndJavaShapes {

	static public Shape flineToJavaShape(FLine f) {
		GeneralPath p = new GeneralPath();

		for (FLine.Node n : f.nodes) {
			if (n instanceof FLine.MoveTo) p.moveTo(n.to.x, n.to.y);
			else if (n instanceof FLine.LineTo) p.lineTo(n.to.x, n.to.y);
			else if (n instanceof FLine.CubicTo)
				p.curveTo(((FLine.CubicTo) n).c1.x, ((FLine.CubicTo) n).c1.y, ((FLine.CubicTo) n).c2.x, ((FLine.CubicTo) n).c2.y, n.to.x, n.to.y);

		}
		return p;
	}

	static public FLine javaShapeToFLine(Shape f) {
		PathIterator pi = f.getPathIterator(null);
		float[] cc = new float[6];
		Vec2 lastAt = null;
		Vec2 lastMoveTo = null;

		FLine in = new FLine();
		while (!pi.isDone()) {
			int ty = pi.currentSegment(cc);
			if (ty == PathIterator.SEG_CLOSE) {
				if (lastMoveTo != null && lastAt.distanceFrom(lastMoveTo) > 1e-6)
					in.lineTo(lastMoveTo.x, lastMoveTo.y);
				lastAt = null;
			} else if (ty == PathIterator.SEG_CUBICTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[4]) + Math.abs(lastAt.y - cc[5]) > 1e-15))
					in.cubicTo(cc[0], cc[1], cc[2], cc[3], cc[4], cc[5]);
				if (lastAt == null) lastAt = new Vec2(cc[4], cc[5]);
				else {
					lastAt.x = cc[4];
					lastAt.y = cc[5];
				}
			} else if (ty == PathIterator.SEG_LINETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15))
					in.lineTo(cc[0], cc[1]);
				if (lastAt == null) lastAt = new Vec2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_MOVETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15))
					in.moveTo(cc[0], cc[1]);

				lastMoveTo = new Vec2(cc[0], cc[1]);

				if (lastAt == null) lastAt = new Vec2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_QUADTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[2]) + Math.abs(lastAt.y - cc[3]) > 1e-15))
					in.cubicTo((cc[0] - lastAt.x) * (2 / 3f) + lastAt.x, (cc[1] - lastAt.y) * (2 / 3f) + lastAt.y, (cc[0] - cc[2]) * (2 / 3f) + cc[2], (cc[1] - cc[3]) * (2 / 3f) + cc[3], cc[2], cc[3]);

				if (lastAt == null) lastAt = new Vec2(cc[2], cc[3]);
				else {
					lastAt.x = cc[2];
					lastAt.y = cc[3];
				}

			}

			pi.next();

		}
		return in;
	}

	static public FLine insetShape(FLine f, float amount)
	{
		Shape s = flineToJavaShape(f);
		Shape ss = new BasicStroke(amount, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(s);

		Area sa = new Area(s);
		Area ssa = new Area(ss);
		sa.subtract(ssa);
		return javaShapeToFLine(sa);
	}

}
