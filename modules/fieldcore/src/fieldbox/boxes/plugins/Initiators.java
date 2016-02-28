package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldlinker.Linker;

import java.util.function.Supplier;

/**
 * Class that's passed into .begin() methods to hook up timesliders and mouse presses to running boxes
 */
public class Initiators {


	static public Linker.AsMap get(Box forBox, Supplier<Number> x) {
		double xn = x.get()
			     .doubleValue();
		Box b = new _tx(x, forBox, xn);
		return b;
	}

	static public Linker.AsMap get(Box forBox, Supplier<Number> x, Supplier<Number> y) {
		double xn = x.get()
			     .doubleValue();
		double yn = x.get()
			     .doubleValue();
		Box b = new _txy(x, forBox, y, xn, yn);
		return b;
	}

	static public Supplier<Number> mouseX(Box forBox, double other)
	{
		return () -> forBox.find(Boxes.window, forBox.upwards()).findFirst().map(x -> x.getCurrentMouseState().x).orElse(other);
	}

	static public Supplier<Number> mouseY(Box forBox, double other)
	{
		return () -> forBox.find(Boxes.window, forBox.upwards()).findFirst().map(x -> x.getCurrentMouseState().y).orElse(other);
	}


	// Method Handle dispatch needs these to be public
	public static class _tx extends Box {
		private final Supplier<Number> x;
		private final Box forBox;
		private final double xn;

		public _tx(Supplier<Number> x, Box forBox, double xn) {
			this.x = x;
			this.forBox = forBox;
			this.xn = xn;
		}

		@Override
		public Object asMap_call(Object o) {
			double d = x.get()
				    .doubleValue();
			Rect f = forBox.properties.get(Box.frame);
			return Math.max(0, Math.min(1, (d - f.x) / f.w));
		}

		@Override
		public Object asMap_get(String m) {
			if (m.equals("x")) {
				return x.get();
			}
			if (m.equals("dx")) {
				return x.get()
					.doubleValue() - xn;
			}
			if (m.equals("t")) {
				return asMap_call(null);
			}
			if (m.equals("rawt")) {
				double d = x.get()
					    .doubleValue();
				Rect f = forBox.properties.get(Box.frame);
				return (d - f.x) / f.w;

			}
			return super.asMap_get(m);
		}
	}

	// Method Handle dispatch needs these to be public
	public  static class _txy extends Box {
		private final Supplier<Number> x;
		private final Box forBox;
		private final Supplier<Number> y;
		private final double xn;
		private final double yn;

		public _txy(Supplier<Number> x, Box forBox, Supplier<Number> y, double xn, double yn) {
			this.x = x;
			this.forBox = forBox;
			this.y = y;
			this.xn = xn;
			this.yn = yn;
		}

		@Override
		public Object asMap_call(Object o) {
			double d = x.get()
				    .doubleValue();
			Rect f = forBox.properties.get(Box.frame);
			return Math.max(0, Math.min(1, (d - f.x) / f.w));
		}

		@Override
		public Object asMap_get(String m) {
			if (m.equals("x")) {
				return x.get();
			}
			if (m.equals("y")) {
				return y.get();
			}
			if (m.equals("dx")) {
				return x.get()
					.doubleValue() - xn;
			}
			if (m.equals("dx")) {
				return y.get()
					.doubleValue() - yn;
			}
			if (m.equals("at")) {
				return new Vec2(x.get()
						 .doubleValue(), y.get()
								  .doubleValue());
			}
			if (m.equals("dat")) {
				return new Vec2(x.get()
						 .doubleValue() - xn, y.get()
								       .doubleValue() - yn);
			}
			if (m.equals("t")) {
				return asMap_call(null);
			}
			if (m.equals("s")) {
				double d = y.get()
					    .doubleValue();
				Rect f = forBox.properties.get(Box.frame);
				return Math.max(0, Math.max(1, (d - f.y) / f.h));
			}
			if (m.equals("rawt") || m.equals("raw"))
			{
				double d = x.get()
					    .doubleValue();
				Rect f = forBox.properties.get(Box.frame);
				return (d - f.x) / f.w;
			}
			if (m.equals("raws"))
			{
				double d = y.get()
					    .doubleValue();
				Rect f = forBox.properties.get(Box.frame);
				return (d - f.y) / f.h;
			}
			return super.asMap_get(m);
		}
	}
}
