package fielded;

import fieldbox.execution.Errors;

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


	public interface AnimationElement {

		default Object beginning(boolean isEnding) {
			return this;
		}

		Object middle(boolean isEnding);

		default Object end(boolean isEnding) {
			return this;
		}
	}

	static public class Shim implements Supplier<Boolean>, Consumer<Boolean>, Errors.SavesErrorConsumer {
		protected final AnimationElement e;
		protected boolean stopping = false;
		protected boolean first = true;
		protected Errors.ErrorConsumer errorConsumer;

		public Shim(AnimationElement e) {
			this.e = e;
			this.errorConsumer = Errors.errors.get();
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
			}
			catch(Throwable t)
			{
				t.printStackTrace();
				if (errorConsumer!=null)
				{
					errorConsumer.consume(t, "(Error thrown in box animation, we'll stop this box now)");
				}
				return false;
			}
		}

		@Override
		public void accept(Boolean willContinue) {
			stopping = !willContinue;
		}

		@Override
		public void setErrorConsumer(Errors.ErrorConsumer c) {
			this.errorConsumer = c;
		}

		@Override
		public Errors.ErrorConsumer getErrorConsumer() {
			return this.errorConsumer;
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
