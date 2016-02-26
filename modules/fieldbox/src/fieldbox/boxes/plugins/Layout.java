package fieldbox.boxes.plugins;

import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.io.IO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * utilities for automatically laying out boxes incrementally
 */
public class Layout {

	Map<String, Rect> r = new LinkedHashMap<>();
	float inset = 10;

	public Box right(Box b) {
		return advance(b, right());
	}

	public Box left(Box b) {
		return advance(b, left());
	}

	public Box down(Box b) {
		return advance(b, down());
	}

	public Box up(Box b) {
		return advance(b, up());
	}


	public Box right(Box b, float maxTravel) {
		return advance(b, right(), maxTravel, down());
	}

	public Box left(Box b, float maxTravel) {
		return advance(b, left(), maxTravel, down());
	}

	public Box down(Box b, float maxTravel) {
		return advance(b, down(), maxTravel, right());
	}

	public Box up(Box b, float maxTravel) {
		return advance(b, up(), maxTravel, right());
	}

	protected BiFunction<Rect, Rect, Rect> right() {
		return (rr, R) -> new Rect(rr.x + inset + rr.w, rr.y, R.w, R.h);
	}

	protected BiFunction<Rect, Rect, Rect> left() {
		return (rr, R) -> new Rect(rr.x - inset - R.w, rr.y, R.w, R.h);
	}


	protected BiFunction<Rect, Rect, Rect> up() {
		return (rr, R) -> new Rect(R.x, rr.y - inset - R.h, R.w, R.h);
	}

	protected BiFunction<Rect, Rect, Rect> down() {
		return (rr, R) -> new Rect(R.x, rr.y + inset + rr.h, R.w, R.h);
	}


	public void initialize(Box root) {
		root.breadthFirst(root.allDownwardsFrom())
		    .forEach(x -> r.put(x.properties.getOrConstruct(IO.id), x.properties.get(Box.frame)));
	}

	public Box advance(Box b, BiFunction<Rect, Rect, Rect> advance) {
		Rect R = b.properties.get(Box.frame);
		boolean repeat = false;
		do {
			repeat = false;
			for (Rect rr : r.values()) {
				if (overlaps(rr, R)) {
					R = advance.apply(rr, R);
					repeat = true;
					break;
				}
			}
		} while (repeat);

		b.properties.put(Box.frame, R);
		r.put(b.properties.getOrConstruct(IO.id), R);
		return b;
	}

	public Box advance(Box b, BiFunction<Rect, Rect, Rect> advance, float maxTravel, BiFunction<Rect, Rect, Rect> andThen) {
		Rect R = b.properties.get(Box.frame);
		Rect RR = R.duplicate();
		boolean repeat = false;

		do {
			repeat = false;
			for (Rect rr : r.values()) {
				if (overlaps(rr, R)) {
					R = advance.apply(rr, R);

					if (Math.abs(RR.x - R.x) > maxTravel) {
						RR = R = andThen.apply(RR, RR);
						repeat = true;
						break;
					}

					repeat = true;
					break;
				}
			}
		} while (repeat);

		b.properties.put(Box.frame, R);
		r.put(b.properties.getOrConstruct(IO.id), R);
		return b;
	}

	private boolean overlaps(Rect a, Rect b) {
		return a.inset(-inset)
			.intersects(b);
	}


}
