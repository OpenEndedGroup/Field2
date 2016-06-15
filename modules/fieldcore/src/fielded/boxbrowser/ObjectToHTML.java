package fielded.boxbrowser;

import field.utility.IdempotencyMap;
import fielded.TextUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

/**
 * Created by marc on 9/12/15.
 */
public class ObjectToHTML {

	public interface MasqueradesAs {
		public Object masqueradesAs();
	}

	static public ThreadLocal<Stack<String>> contextStack = new ThreadLocal<Stack<String>>() {
		@Override
		protected Stack<String> initialValue() {
			return new Stack<>();
		}
	};

	public IdempotencyMap<Function<Object, Object>> map = new IdempotencyMap<Function<Object, Object>>(Function.class) {
		@Override
		protected String massageKey(String k) {
			return k == null ? null : k.toLowerCase();
		}
	};
	Function<Object, Object> nullHandler = null;

	public Function<Object, Object> lookup(Class clazz) {
		return lookup(clazz, Collections.emptySet());
	}

	public Function<Object, Object> lookup(Class clazz, Set<Function<Object, Object>> ignore) {

		if (clazz == null) return nullHandler;

		String name = clazz.getName();
		String[] pieces = name.split("\\.");

		for (int i = 0; i < pieces.length; i++) {
			String j = subArray(pieces, i);
			Function<Object, Object> f = map.get(j);
			if (f != null && !ignore.contains(f)) return f;
		}

		Class[] c = clazz.getInterfaces();
		for (Class cc : c) {
			Function<Object, Object> ccc = lookup(cc, ignore);
			if (ccc != null && !ignore.contains(ccc)) return ccc;
		}

		return lookup(clazz.getSuperclass(), ignore);
	}

	private String subArray(String[] pieces, int i) {

		String q = "";
		for (int ii = i; ii < pieces.length; ii++) {
			q += (ii == i ? "" : "_") + pieces[ii].toLowerCase();
		}
		return q;
	}

	public ObjectToHTML setNullHandler(Function<Object, Object> nullHandler) {
		this.nullHandler = nullHandler;
		return this;
	}


	public String convert(Object o, String context) {
		contextStack.get().push(context);
		try {
			return convert(o);
		} finally {
			contextStack.get().pop();
		}
	}

	public String joinContext() {
		if (contextStack.get().empty()) return "";
		return contextStack.get().stream().reduce((a, b) -> a + "_" + b).get();
	}

	public String convert(Object o) {
		if (o == null && nullHandler != null) return "" + nullHandler.apply(o);
		if (o == null) return "[null]";

		Set<Function<Object, Object>> found = new LinkedHashSet<>();
		if (o instanceof MasqueradesAs)
			o = ((MasqueradesAs)o).masqueradesAs();

		Function<Object, Object> f = lookup(o.getClass(), found);

		int chances = 0;
		while (f != null && chances < 2) {

			Object o2 = f.apply(o);

			if (o2 instanceof String) return clean((String) o2);
			if (o2 == null) return clean("" + o);
			if (o2.equals(o)) {
				found.add(f);
				f = lookup(o.getClass(), found);
				chances++;
			} else {
				o = o2;
				chances = 0;
				f = lookup(o.getClass(), found);
			}
		}
		return clean("" + o);
	}

	private String clean(String s) {
		if (s.startsWith("{HTML}")) return s.replace("{HTML}", "");
		return TextUtils.html(s).replace("\n", "<br>");
	}

}
