package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A box that draws, and maintains, itself as a box that connects two other boxes. Automatically deletes itself should either of its two ends disappears.
 */
public class TopologyBox extends Box {

	protected final Box head;
	protected final Box tail;

	public TopologyBox(Box head, Box tail)
	{
		this.head = head;
		this.tail = tail;

		this.properties.putToMap(Boxes.insideRunLoop, "__checkfordeletion__", this::checkForDeletion);
		this.properties.putToMap(Boxes.insideRunLoop, "__updateframe__", this::updateFrameToMiddle);
		this.properties.computeIfAbsent(FrameDrawer.frameDrawing, this::defaultdrawsLines);
		this.properties.put(Manipulation.frame, head.properties.get(Manipulation.frame).union(tail.properties.get(Manipulation.frame)));
		this.properties.put(FrameManipulation.lockHeight, true);
		this.properties.put(FrameManipulation.lockWidth, true);
		this.properties.put(FrameManipulation.lockX, true);
		this.properties.put(FrameManipulation.lockY, true);

		this.properties.put(Box.name, head.properties.get(Box.name)+"->"+tail.properties.get(Box.name));

	}

	protected Map<String, Function<Box, FLine>> defaultdrawsLines(Dict.Prop<Map<String, Function<Box, FLine>>> k) {
		Map<String, Function<Box, FLine>> r = new LinkedHashMap<>();

		r.put("__outline__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(Manipulation.frame);
			if (rect == null) return null;

			FLine f = arc(((TopologyBox)box).head.properties.get(Manipulation.frame), ((TopologyBox)box).tail.properties.get(Manipulation.frame));

			f = thickenArc(f, ((TopologyBox)box).head.properties.get(Manipulation.frame), ((TopologyBox)box).tail.properties.get(Manipulation.frame));

			boolean selected = box.properties.isTrue(Manipulation.isSelected, false);

			f.attributes.put(FrameDrawer.fillColor, selected ? new Vec4(1, 1, 1, 1.0f) : new Vec4(1, 1, 1, 0.5f));
			f.attributes.put(FrameDrawer.strokeColor, selected ? new Vec4(1, 1, 1, 0.25f) : new Vec4(1, 1, 1, 0.1f));
			f.attributes.put(FrameDrawer.thicken, new BasicStroke(selected ? 16 : 0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(FrameDrawer.filled, true);
			f.attributes.put(FrameDrawer.stroked, true);

			return f;
		}, (box) -> new Triple<Boolean, Rect, Rect>(properties.get(Manipulation.isSelected),  ((TopologyBox)box).head.properties.get(Manipulation.frame), (((TopologyBox)box).tail.properties.get(Manipulation.frame)))));


		r.put("__middle_nub__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(Manipulation.frame);
			if (rect == null) return null;

			FLine f = arc(((TopologyBox)box).head.properties.get(Manipulation.frame), ((TopologyBox)box).tail.properties.get(Manipulation.frame));

			Vec2 at = middleOf(f);

			boolean selected = box.properties.isTrue(Manipulation.isSelected, false);

			System.out.println(" is selected ? "+selected);

			f = new FLine();
			float w = selected ? 10 : 5;

			f.moveTo(at.x-w, at.y-w);
			f.lineTo(at.x + w, at.y - w);
			f.lineTo(at.x + w, at.y + w);
			f.lineTo(at.x - w, at.y + w);
			f.lineTo(at.x - w, at.y - w);


			f.attributes.put(FrameDrawer.fillColor, selected ? new Vec4(1, 1, 1, 1.0f) : new Vec4(1, 1, 1, 0.5f));
			f.attributes.put(FrameDrawer.strokeColor, selected ? new Vec4(1, 1, 1, 0.25f) : new Vec4(1, 1, 1, 0.1f));
			f.attributes.put(FrameDrawer.thicken, new BasicStroke(selected ? 16 : 0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(FrameDrawer.filled, true);
			f.attributes.put(FrameDrawer.stroked, true);

			return f;
		}, (box) -> new Triple<Boolean, Rect, Rect>(properties.get(Manipulation.isSelected), ((TopologyBox)box).head.properties.get(Manipulation.frame), (((TopologyBox)box).tail.properties.get(Manipulation.frame)))));

		return r;
	}

	protected FLine thickenArc(FLine f, Rect from, Rect to)
	{
		Shape shape = FLinesAndJavaShapes.flineToJavaShape(f);
		Area r1 = new Area(new BasicStroke(5.5f).createStrokedShape(shape));
		Area r2 = new Area(new Rectangle2D.Float(from.x, from.y, from.w, from.h));
		Area r3 = new Area(new Rectangle2D.Float(to.x, to.y, to.w, to.h));

		r1.subtract(r2);
		r1.subtract(r3);

		f = FLinesAndJavaShapes.javaShapeToFLine(r1);

		return f;
	}


	protected FLine arc(Rect from, Rect to) {

		FLine f = new FLine();

		Vec2 a = new Vec2(from.x+from.w/2, from.y+from.h/2);
		Vec2 b = new Vec2(to.x+to.w/2, to.y+to.h/2);

		float d = (float) b.distanceFrom(a);

		Vec2 normal = Vec2.sub(b, a, new Vec2());

		Vec2 tan = new Vec2(-normal.y, normal.x).normalise().scale(d*0.15f);
		if (normal.x>0)
			tan.scale(-1);

		Vec2 c1 = new Vec2(a.x+normal.x*1/3f+tan.x,a.y+normal.y*1/3f+tan.y);
		Vec2 c2 = new Vec2(a.x+normal.x*2/3f+tan.x,a.y+normal.y*2/3f+tan.y);

		f.moveTo(a.x, a.y);
		f.cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y);

		return f;
	}

	public Vec2 middleOf(FLine f)
	{
		FLine.MoveTo m = (FLine.MoveTo) f.nodes.get(0);
		FLine.CubicTo c = (FLine.CubicTo) f.nodes.get(f.nodes.size()-1);

		return FLinesAndJavaShapes.evaluateCubicFrame(m.to.toVec2(),c.c1.toVec2(), c.c2.toVec2(), c.to.toVec2(), 0.5f, new Vec2());
	}

	protected boolean checkForDeletion()
	{
		if (head.parents().size()==0 || tail.parents().size()==0)
			this.disconnectFromAll();
		return true;
	}

	protected boolean updateFrameToMiddle()
	{
		FLine f = arc(head.properties.get(Manipulation.frame), tail.properties.get(Manipulation.frame));

		Vec2 m = middleOf(f);

		Rect frame1 = this.properties.get(Manipulation.frame);
		float w = 10;
		Rect frame2 = new Rect(m.x-w, m.y-w, w*2, w*2);

		if (!frame1.equals(frame2)) {
			this.properties.put(Manipulation.frame, frame2);
			Drawing.dirty(this);
		}

		return true;
	}


}
