package field.utility;

import field.graphics.Bracketable;
import field.nashorn.internal.runtime.Undefined;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Every project needs a Util class with statics in it. Here it is.
 */
public class Util {
	static public boolean safeEq(Object a, Object b) {
		if (a == null) return b == null;
		if (b == null) return false;
		return a.equals(b);
	}

	/**
	 * exception munging list autoclosable
	 */
	static public AutoCloseable closeable(Collection<? extends AutoCloseable> c) {

		for(AutoCloseable cc : c)
		{
			if (cc instanceof Bracketable) ((Bracketable)cc).open();
		}

		return () -> {
			List<Throwable> thrown = new ArrayList<>();
			c.forEach((autoCloseable) -> {
				try {
					autoCloseable.close();
				} catch (Exception e) {
					e.printStackTrace();
					thrown.add(e);
				}
			});
			if (thrown.size()>0)
			{
				Exception e = new Exception(" exception(s) throw during close "+thrown);
				e.initCause(thrown.get(0));
				throw e;
			}
		};
	}
	/**
	 * exception munging list autoclosable
	 */

	static public AutoCloseable closeable(AutoCloseable... c1) {
		return closeable(Arrays.asList(c1));
	}

	/** An autoclosable that doesn't throw an exception */
	static public interface ExceptionlessAutoCloasable extends AutoCloseable
	{
		@Override
		public void close();
	}


	/**
	 * dynamic languages often have broad notions of what truth is, and we often don't have the opportunity to cast and box all of them to true
	 */
	public static boolean truthy(Object x) {
		if (x instanceof Number)
			return ((Number)x).doubleValue()!=0;
		if (x instanceof String)
			return ((String)x).length()>0;
		if (x instanceof Boolean)
			return ((Boolean)x).booleanValue();
		return x!=null;
	}

	static public class Errors
	{
		List<Pair<Throwable, Object>> errors = new ArrayList<>();

		public boolean hasErrors()
		{
			return errors.size()>0;
		}

		public List<Pair<Throwable, Object>> getErrors()
		{
			return errors;
		}

		public void clear()
		{
			errors.clear();
		}

		public <T, R> void add(Throwable t, Object originator) {
			errors.add(new Pair<>(t, originator));
		}
	}

	public static <T, R> Function<T, R> wrap(Function<T, R> a, Errors error, R def, Class convertReturn)
	{
		return x -> {
			try {
				Object q = a.apply(x);

				if (convertReturn!=null)
					q = Conversions.convert(q,convertReturn);


				if (q instanceof Undefined)
					return null;

				return (R)q;
			}
			catch(Throwable t)
			{
				t.printStackTrace();
				error.add(t, a);
				return def;
			}
		};
	}

	public static <T> Consumer<T> wrap(Consumer<T> a, Errors error)
	{
		return x -> {
			try {
				a.accept(x);
			}
			catch(Throwable t)
			{
				t.printStackTrace();
				error.add(t, new Pair(a, x));
			}
		};
	}

}
