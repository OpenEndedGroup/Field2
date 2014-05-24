package fielded;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What does it mean to Animate a box? This class handles conversions from various dynamic language constructs to things with a beginning, a middle
 * and an end. Runtimes can register things that can be converted into this format here with this registry.
 *
 * Question: should this just use the Conversions.java framework instead?
 */
public class Animatable {


	static public interface AnimationElement {

		default public Object beginning(boolean isEnding) {
			return this;
		}

		public Object middle(boolean isEnding);

		default public Object end(boolean isEnding) {
			return this;
		}
	}

	static public class Shim implements Supplier<Boolean>, Consumer<Boolean> {
		private final AnimationElement e;
		boolean stopping = false;
		boolean first = true;

		public Shim(AnimationElement e) {
			this.e = e;
		}

		@Override
		public Boolean get() {
			if (first) {
				first = false;
				e.beginning(stopping);
				return true;
			} else if (stopping) {
				e.end(stopping);
				return false;
			} else {
				e.middle(stopping);
				return true;
			}
		}

		@Override
		public void accept(Boolean willContinue) {
			stopping = !willContinue;
		}
	}

	static List<BiFunction<AnimationElement, Object, AnimationElement>> handlers = new ArrayList<>();

	static public void registerHandler(BiFunction<AnimationElement, Object, AnimationElement> h) {
		handlers.add(0, h);
	}

	static public AnimationElement interpret(Object r, AnimationElement current) {
		for (BiFunction<AnimationElement, Object, AnimationElement> b : handlers) {
			current = b.apply(current, r);
		}
		return current;
	}

}
