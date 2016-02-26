package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec4;
import fieldbox.boxes.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Useful classes for turning FLines into editable things using Handles
 */
public class HandlesForFLines extends Box {

	static public class DraggableNode extends Handles.Draggable {

		private final int index;
		private final FLine nextCubic;
		private final FLine prevCubic;
		private final DraggableCubicHandle nextCubicDrag;
		private final DraggableCubicHandle prevCubicDrag;

		public DraggableNode(FLine source, FLine.Node on) {

			index = source.nodes.indexOf(on);

			if (index < source.nodes.size() - 1&&source.nodes.get(index + 1) instanceof FLine.CubicTo) {
				FLine.CubicTo c = (FLine.CubicTo) source.nodes.get(index + 1);
				nextCubic = new FLine();
				nextCubic.moveTo(c.c1.x, c.c1.y);
				nextCubic.lineTo(on.to.x, on.to.y);
				nextCubic.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, -0.5));
				nextCubicDrag = new DraggableCubicHandle(nextCubic, nextCubic.nodes.get(0), c, 0);
				nextCubic.nodes.get(0).attributes.putToMap(Handles.draggables, "__cubic", nextCubicDrag);
			}
			else {
				nextCubic = null;
				nextCubicDrag = null;
			}

			if (on instanceof FLine.CubicTo)
			{
				FLine.CubicTo c = (FLine.CubicTo) source.nodes.get(index );
				prevCubic= new FLine();
				prevCubic.moveTo(c.c2.x, c.c2.y);
				prevCubic.lineTo(on.to.x, on.to.y);
				prevCubic.attributes.put(StandardFLineDrawing.color, new Vec4(0,0,0,-0.5));
				prevCubicDrag = new DraggableCubicHandle(prevCubic, prevCubic.nodes.get(0), c, 1);
				prevCubic.nodes.get(0).attributes.putToMap(Handles.draggables, "__cubic", prevCubicDrag);
			}
			else {
				prevCubic = null;
				prevCubicDrag = null;
			}

			this.get = () -> on.to.toVec2();

			this.setAndConstrain = (next, previous, initial) -> {
				on.to.x = next.x;
				on.to.y = next.y;
				source.modify();

				if (nextCubic!=null)
				{
					nextCubic.nodes.get(1).to.x = next.x;
					nextCubic.nodes.get(1).to.y = next.y;
					nextCubic.modify();
				}

				if (prevCubic!=null)
				{
					prevCubic.nodes.get(1).to.x = next.x;
					prevCubic.nodes.get(1).to.y = next.y;
					prevCubic.modify();
				}

				return next;
			};
			this.select = x -> x;
			this.appearance = () -> {

				FLine f = new FLine();
				f.moveTo(on.to.x, on.to.y);
				f.attributes.put(StandardFLineDrawing.pointSize, isSelected() ? 10f : 5f);
				f.attributes.put(StandardFLineDrawing.pointed, true);
				f.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, isSelected() ? 0.5 : 0.15f));

				List<FLine> ret = new ArrayList<>();
				ret.add(f);
				if (isSelected() || (prevCubic!=null && prevCubicDrag.isSelected()) || (nextCubic!=null && nextCubicDrag.isSelected())) {
					if (nextCubic != null) ret.add(nextCubic);
					if (prevCubic != null) ret.add(prevCubic);
				}
				return ret;

			};
			this.finisher = (v) -> v;

			source.attributes.put(Handles.hasDraggables, true);

			init();

		}
	}

	static public class DraggableCubicHandle extends Handles.Draggable {

		public DraggableCubicHandle(FLine source, FLine.Node vis, FLine.CubicTo on, int c) {


			this.get = () -> vis.to.toVec2();
			this.setAndConstrain = (next, previous, inital) -> {

				vis.to.x = next.x;
				vis.to.y = next.y;

				if (c==0) {
					on.c1.x = next.x;
					on.c1.y = next.y;
				}
				else
				{
					on.c2.x = next.x;
					on.c2.y = next.y;
				}
				source.modify();
				on.modify();

				return next;
			};
			this.select = x -> x;
			this.appearance = () -> {

				FLine f = new FLine();
				f.moveTo(vis.to.x, vis.to.y);
				f.attributes.put(StandardFLineDrawing.pointSize, isSelected() ? 10f : 5f);
				f.attributes.put(StandardFLineDrawing.pointed, true);
				f.attributes.put(StandardFLineDrawing.color, new Vec4(0.25f, 0, 0, isSelected() ? 0.5 : 0.15f));


				return Collections.singletonList(f);

			};
			this.finisher = (v) -> v;

			source.attributes.put(Handles.hasDraggables, true);

			init();

		}
	}

}
