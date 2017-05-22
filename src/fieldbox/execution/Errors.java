package fieldbox.execution;

import com.google.common.collect.MapMaker;
import field.utility.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Fundamental problem in sending code off for execution (remotely or in draw loops) is what you do when it throws an exception. This is particularly bad in the case of a draw loop --- you might
 * only catch an OpenGL error long after some code has executed. Therefore we often push things onto this stack and leave them there.
 */
public class Errors {

	static public final Errors errors = new Errors((t, m) -> {
		System.err.println(" Exception thrown with no error consumer in place ");
		System.err.println(" Message is :" + m);
		System.err.println(" Exception is :");
		t.printStackTrace();
	});

	static public Map<Object, ErrorConsumer> consumerStore = new MapMaker().weakKeys().makeMap();

	private final ErrorConsumer defaultConsumer;

	public static void tryToReportTo(Throwable throwable, String message, Object c) {

		if (c instanceof SavesErrorConsumer) {
			ErrorConsumer ec = ((SavesErrorConsumer) c).getErrorConsumer();
			if (ec == c) {
				ec.consume(throwable, message);
			} else {
				tryToReportTo(throwable, message, ec);
			}
		} else if (c instanceof ErrorConsumer) {
			((ErrorConsumer) c).consume(throwable, message);
		} else {
			Errors.errors.get().consume(throwable, message);
		}
	}

	public interface ErrorConsumer {
		void consume(Throwable t, String message);
	}

	public interface SavesErrorConsumer {
		void setErrorConsumer(ErrorConsumer c);

		ErrorConsumer getErrorConsumer();
	}

	protected Errors(ErrorConsumer defaultConsumer) {
		this.defaultConsumer = defaultConsumer;
	}

	List<ErrorConsumer> currentConsumer = new ArrayList<>();

	public void setErrorConsumer(ErrorConsumer c) {
		this.currentConsumer.clear();
		this.currentConsumer.add(c);
	}

	public void push(ErrorConsumer c) {
		this.currentConsumer.add(c);
	}

	public void pop() {
		if (this.currentConsumer.size() > 0) this.currentConsumer.remove(this.currentConsumer.size() - 1);
		else
			Log.log("errors.warning", () -> "warning, popped error consumer that wasn't there (continuing on)");
	}

	public ErrorConsumer get() {
		if (this.currentConsumer.size() == 0) {
			return defaultConsumer;
		}

		return this.currentConsumer.get(this.currentConsumer.size() - 1);
	}

	public void store(Object o, ErrorConsumer c) {
		consumerStore.put(o, c);
	}

	public ErrorConsumer retrieve(Object o) {
		return consumerStore.get(o);
	}

	static public <T> T handle(Supplier<T> c, Function<Throwable, T> orElse) {

		try {
			T t = c.get();
			return t;
		} catch (Throwable t) {
			return orElse.apply(t);
		}

	}

}
