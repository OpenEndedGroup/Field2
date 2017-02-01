package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec3;
import fieldbox.boxes.FLineDrawing;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Since points have no area, they don't fit into FLineInteraction; they are also much easier to hittest
 */
public class FLinePointHitTest {

	private final Transformer transformer;

	static public class Hit {
		public FLine on;
		public FLine.Node node;
		public int index;
		public double distance;

		public Hit(FLine ff, FLine.Node nn, int index, double dd) {
			this.on = ff;
			this.node = nn;
			this.index = index;
			this.distance = dd;
		}
	}

	/**
	 * transforms to window space pixels
	 */

	public interface Transformer extends Function<Vec3, Vec2> {
		boolean begin(Window.Event<Window.MouseState> provokedBy);

		void result(List<Hit> hit);
	}

	public FLinePointHitTest(Transformer transformer) {

		this.transformer = transformer;
	}


	public List<Hit> hit(Window.Event<Window.MouseState> event, List<FLine> f, float radius) {
		List<Hit> hits = new ArrayList<>();

		Vec2 d = new Vec2(event.after.mx, event.after.my);

		float r2 = radius * radius;

		if (transformer.begin(event)) {
			for (FLine ff : f) {
				int index = 0;
				for (FLine.Node nn : ff.nodes) {

					Vec2 v2 = transformer.apply(nn.to);

					double dd = v2.distanceSquared(d);
					if (dd < r2) {
						hits.add(new Hit(ff, nn, index, dd));
					}

					index++;
				}
			}
		}

		Collections.sort(hits, Comparator.comparingDouble(a -> a.distance));

		transformer.result(hits);

		return hits;
	}

	public void select(Collection<FLine.Node> n, Supplier<Double> radius, Supplier<Vec3> plane)
	{
		for(FLine.Node nn : n)
		{
			nn.attributes.putToMap(StandardFLineDrawing.subLines, "__pointSelection__", () -> {
				FLine f = new FLine();

				Vec3 pt = plane.get();
				Vec3 p1 = new Vec3().cross(pt.randomNonParallelVector(), pt);
				Vec3 p2 = new Vec3().cross(p1, pt);

				p1.normalize();
				p2.normalize();

				return f;
			});
		}
	}

}
