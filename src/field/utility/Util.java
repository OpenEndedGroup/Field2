package field.utility;

import field.graphics.Bracketable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
}
