package fielded;

import field.linalg.Vec4;
import fieldbox.execution.Errors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What does it mean to Animate a box? This class handles conversions from various dynamic language constructs to things with a beginning, a middle
 * and an end. Runtimes can register things that can be converted into this format here with this registry.
 * <p>
 * Question: should this just use the Conversions.java framework instead?
 */
public class Animatable {


	public interface AnimationElement {

		default Object beginning(boolean isEnding) {
			return this;
		}

		default Object middle(boolean isEnding) {
			return this;
		}

		default Object end(boolean isEnding) {
			return this;
		}
	}

	static public class Shim implements Supplier<Boolean>, Consumer<Boolean> {
		protected final AnimationElement e;
		protected boolean stopping = false;
		protected boolean first = true;

		public Shim(AnimationElement e) {
			this.e = e;
		}

		@Override
		public Boolean get() {
			try {
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
			} catch (Throwable t) {
				t.printStackTrace();
				Errors.INSTANCE.tryToReportTo(t, "Error thrown in box animation, box stopped", null);

				return false;
			}
		}

		@Override
		public void accept(Boolean willContinue) {
			stopping = !willContinue;
		}

	}

	static List<BiFunction<AnimationElement, Object, AnimationElement>> handlers = new ArrayList<>();

	static {
		registerHandler((current, incomming) -> {
			if (incomming instanceof AnimationElement) {
				return (AnimationElement) incomming;
			}
			return current;
		});
	}

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
