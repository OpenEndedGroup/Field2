package field.utility;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.istack.internal.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * A Microscopically small logging framework
 */
public class Log {

	static protected ConcurrentLinkedDeque<Pair<Pattern, BiConsumer<String, Object>>> active = new ConcurrentLinkedDeque<>();
	static protected Cache<String, String> earlyFail = CacheBuilder.newBuilder().concurrencyLevel(2).maximumSize(100).build();

	// we default to logging everything
	static protected
	@Nullable
	BiConsumer<String, Object> _fallthrough = Log::println;

	// Use --------------------------------

	static public void log(String channel, Object message) {
		log(channel, () -> message);
	}

	static public void log(String channel, Object... message) {
		log(channel, () -> Arrays.asList(message));
	}

	static public void log(String channel, Supplier<Object> message) {
		boolean found = false;
		if (earlyFail.getIfPresent(channel) == null) {
			for (Pair<Pattern, BiConsumer<String, Object>> p : active) {
				if (p.first.matcher(channel).matches()) {
					Object m = message.get();
					if (m != null) p.second.accept(channel, m);
					found = true;
					break;
				}
			}
			if (!found) earlyFail.put(channel, channel);
		}
		if (!found && _fallthrough != null) _fallthrough.accept(channel, message.get());
	}

	// Configure --------------------------------

	/**
	 * adds a pattern to be logged. This rule is of higher priority than anything previously called "on" (and lower than anything subsequently
	 * called "on").
	 */
	static public void add(String pattern, BiConsumer<String, Object> to) {
		earlyFail.invalidateAll();
		active.addFirst(new Pair<>(Pattern.compile(pattern), to));
	}

	/**
	 * adds a pattern to be logged. This rule is of higher priority than anything previously called "on" (and lower than anything subsequently
	 * called "on"). Previous rules identical to "pattern" are removed.
	 */
	static public void on(String pattern, BiConsumer<String, Object> to) {
		off(pattern);
		add(pattern, to);
	}

	/**
	 * turns a pattern off, by removing anything else associated with this pattern. If other rules (including the default) capture this output it
	 * will still be logged
	 */
	static public void off(String pattern) {
		earlyFail.invalidateAll();
		Iterator<Pair<Pattern, BiConsumer<String, Object>>> i = active.iterator();
		while (i.hasNext()) {
			if (i.next().first.equals(pattern)) i.remove();
		}
	}

	/**
	 * turns a pattern off completely. All things that are associated with this pattern are removed, and a rule matching this pattern is added
	 * that eats this pattern without logging it anywhere
	 */
	static public void disable(String pattern) {
		off(pattern);
		add(pattern, (m, s) -> {
		});
	}

	/**
	 * sets the default action for messages that are not matched by anything (by default the default action is "println")
	 */
	static public void fallthrough(@Nullable BiConsumer<String, Object> b) {
		_fallthrough = b;
	}

	// Standard loggers -----------------------------------


	static public ThreadLocal<String> indent = new ThreadLocal<String>(){
		AtomicInteger threadNumber = new AtomicInteger(0);

		@Override
		protected String initialValue() {
			int num = threadNumber.getAndIncrement();
			return "\033[1;30m"+(char)((int)('a')+num)+"\033[0m ";
		}
	};

	static public void println(String message, Object text) {
		printlnDecorated("", message, text, "");
	}

	static protected void printlnDecorated(String prefix, String message, Object text, String suffix) {
		String tt = toString(text);
		if (tt.startsWith("-- ") && tt.endsWith(" --"))
		{
			System.out.println("\n"+indent.get()+ prefix + "------------------------------------------------------------------------------------" + suffix);
			System.out.format("%s%s%25s :: %s%s\n",  indent.get(),prefix, message, tt, suffix);
			System.out.println(indent.get() + prefix +"------------------------------------------------------------------------------------" + suffix+"\n");
		} else System.out.format("%s%s%25s :: %s%s\n", indent.get(), prefix, message, tt, suffix);
	}

	static public void red(String message, Object text) {
		printlnDecorated("\033[31m", message, text, "\033[0m");
	}

	static public void blue(String message, Object text) {
		printlnDecorated("\033[34m", message, text, "\033[0m");
	}

	static public void green(String message, Object text) {
		printlnDecorated("\033[33m", message, text, "\033[0m");
	}

	static protected String toString(Object o) {
		if (o == null) return "null";
		if (o instanceof Throwable) {
			StringWriter ps = new StringWriter();
			((Throwable) o).printStackTrace(new PrintWriter(ps));
			return ps.toString();
		}
		if (o.getClass().isArray()) return "" + Arrays.asList((Object[]) o);
		return ("" + o).trim();
	}

}
