package field.utility;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import field.dynalink.beans.StaticClass;
import field.nashorn.api.scripting.ScriptObjectMirror;
import field.nashorn.api.scripting.ScriptUtils;
import field.nashorn.internal.objects.ScriptFunctionImpl;
import field.nashorn.internal.runtime.ScriptObject;
import field.nashorn.internal.runtime.linker.JavaAdapterFactory;
import fieldbox.execution.Errors;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Conversions {

	/**
	 * note: this doesn't do interfaces right now
	 * <p>
	 * take a type and expand it out recursively to a flat list of parameterized types. There's no ambiguity here --- type in Java take a fixed number of parameters
	 */
	static public List<Class> linearize(Type c) {
		List<Class> r = new ArrayList<>();
		if (c instanceof ParameterizedType) {
			r.add((Class) ((ParameterizedType) c).getRawType());
			Type[] args = ((ParameterizedType) c).getActualTypeArguments();
			for (Type t : args) {
				r.addAll(linearize(t));
			}
		} else {
			r.add((Class) c);
		}
		return r;
	}

	/**
	 * the opposite of linearize. Take a flat list of parameterized types and build up a human-readable String representation of them. Filters the names of classes through "cleaner"
	 */
	static public String fold(List<Class> typeInformation, Function<String, String> cleaner) {
		return typeInformation == null ? "" : _fold(new ArrayList<>(typeInformation), cleaner);
	}

	static protected String _fold(List<Class> typeInformation, Function<String, String> cleaner) {
		return _fold(typeInformation, cleaner, true);
	}

	static protected String _fold(List<Class> typeInformation, Function<String, String> cleaner, boolean append) {
		if (typeInformation == null) return "";
		if (typeInformation.size() == 0) return "";
		Class c = typeInformation.remove(0);
		TypeVariable[] tp = c.getTypeParameters();
		if (tp == null || tp.length == 0) {
			return cleaner.apply(c.getName()) + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");
		}

		if (cleaner.apply(c.getName())
			   .equals("FunctionOfBox")) {
			String inside = "";
			for (int i = 0; i < tp.length; i++) {
				if (typeInformation.size() == 0) break;
				inside += _fold(typeInformation, cleaner);
			}
			return "() &rarr; " + optionalBracket(inside) + " " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");

		}
		if (cleaner.apply(c.getName())
			   .equals("IdempotencyMap")) {
			String inside = "";
			for (int i = 0; i < tp.length; i++) {
				if (typeInformation.size() == 0) break;
				inside += _fold(typeInformation, cleaner);
			}
			return "&middot; name = " + optionalBracket(inside) + " " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");

		} else if (cleaner.apply(c.getName())
				  .equals("Supplier")) {
			String inside = "";
			for (int i = 0; i < tp.length; i++) {
				if (typeInformation.size() == 0) break;
				inside += _fold(typeInformation, cleaner);
			}
			return "() &rarr; " + optionalBracket(inside) + " " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");

		} else if (cleaner.apply(c.getName())
				  .equals("Predicate")) {
			String inside = "";
			for (int i = 0; i < tp.length; i++) {
				if (typeInformation.size() == 0) break;
				inside += _fold(typeInformation, cleaner);
			}
			return "(" + inside + ") &rarr; Boolean " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");

		} else if (cleaner.apply(c.getName())
				  .equals("FunctionOfBoxValued")) {
			String inside = "";
			for (int i = 0; i < tp.length; i++) {
				if (typeInformation.size() == 0) break;
				inside += _fold(typeInformation, cleaner);
			}
			return "&#9178; " + optionalBracket(inside) + " " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");

		} else if (cleaner.apply(c.getName())
				  .equals("Function") && tp.length >= 2) {
			String a = _fold(typeInformation, cleaner, false);
			String b = "";
			if (typeInformation.size() == 0) b = "?";
			else b = _fold(typeInformation, cleaner, false);

			return optionalBracket(a) + " &rarr; " + optionalBracket(b) + " " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");
		} else if (cleaner.apply(c.getName())
				  .equals("BiFunctionOfBoxAnd") && tp.length >= 2) {
			String a = _fold(typeInformation, cleaner, false);
			String b = "";
			if (typeInformation.size() == 0) b = "?";
			else b = _fold(typeInformation, cleaner, false);

			return "&#9178; " + optionalBracket(a) + " &rarr; " + optionalBracket(b) + " " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");
		} else if (cleaner.apply(c.getName())
				  .equals("BiFunction") && tp.length >= 2) {
			String a = _fold(typeInformation, cleaner, false);
			String b = "";
			if (typeInformation.size() == 0) b = "?";
			else b = _fold(typeInformation, cleaner, false);
			String d = "";
			if (typeInformation.size() == 0) d = "?";
			else d = _fold(typeInformation, cleaner, false);

			return "(" + a + ", " + b + ") &rarr; " + optionalBracket(d) + " " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");
		} else if (cleaner.apply(c.getName())
				  .equals("TriFunctionOfBoxAnd") && tp.length >= 2) {
			String a = _fold(typeInformation, cleaner, false);
			String b = "";
			if (typeInformation.size() == 0) b = "?";
			else b = _fold(typeInformation, cleaner, false);
			String d = "";
			if (typeInformation.size() == 0) d = "?";
			else d = _fold(typeInformation, cleaner, false);

			return "&#9178; (" + a + ", " + b + ") &rarr; " + optionalBracket(d) + " " + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");
		} else if (cleaner.apply(c.getName())
				  .equals("Collection")) {
			String a = _fold(typeInformation, cleaner);
			return "[" + a + "]&hellip; ";
		} else {

			String inside = "&lt;";
			for (int i = 0; i < tp.length; i++) {
				if (typeInformation.size() == 0) break;
				inside += _fold(typeInformation, cleaner);
			}
			inside += "&gt;";
			return cleaner.apply(c.getName()) + inside + (append ? (typeInformation.size() == 0 ? "" : (", " + _fold(typeInformation, cleaner))) : "");
		}
	}

	static protected String optionalBracket(String q) {
		q = q.trim();
		if (q.contains(",")) q = "(" + q + ")";
		return q;
	}

	static public boolean typeInformationEquals(List<Class> c1, List<Class> c2) {
		if (c1.size() != c2.size()) return false;
		for (int i = 0; i < c1.size(); i++) {
			if (!c1.get(i)
			       .equals(c2.get(i))) return false;
		}
		return true;
	}


	static public <A, B> Pair<List<Class>, List<Class>> function(Function<A, B> f) {
		Type inter = f.getClass()
			      .getGenericInterfaces()[0];

		if (inter instanceof Class) throw new IllegalArgumentException("alas, you can't pass in a lambda to function <" + inter + ">");

		Type[] at = ((ParameterizedType) inter).getActualTypeArguments();

		return new Pair<>(linearize(at[0]), linearize(at[1]));
	}

	public static class Conversion {
		List<Class> input;
		List<Class> output;
		float length;
		String name;

		Function converter;

		@Override
		public String toString() {
			return "c<" + name + ">" + length;
		}
	}

	static SetMultimap<List<Class>, Conversion> inputs = MultimapBuilder.linkedHashKeys()
									    .linkedHashSetValues()
									    .build();
	static SetMultimap<List<Class>, Conversion> outputs = MultimapBuilder.linkedHashKeys()
									     .linkedHashSetValues()
									     .build();

	static public <A, B> void provideConversion(float length, Function<A, B> c, String name) {
		Conversion conversion = new Conversion();
		conversion.length = length;
		conversion.name = name;

		Pair<List<Class>, List<Class>> io = function(c);

		conversion.input = io.first;
		conversion.output = io.second;

		conversion.converter = c;

		inputs.put(conversion.input, conversion);
		outputs.put(conversion.output, conversion);

		Log.log("conversions.general", " REGISTERED conversion " + length + " " + conversion.input + " -> " + conversion.output);

	}

	static public List<Pair<List<Class>, Conversion>> getConversion(Object from, List<Class> to) {
		Set<List<Class>> alt = genericAlternativesFor(from.getClass());

		for (List<Class> c : alt) {
			if (c.get(0)
			     .getName()
			     .contains("$$Lambda$")) throw new IllegalArgumentException(" alas, you cannot pass in a lambda into getConversion ");
			List<Pair<List<Class>, Conversion>> r = getConversion(c, to);
			if (r != null) return r;
		}

		Log.log("conversions.general", " no conversion found ");

		return null;
	}

	static public <T> T runConversion(List<Pair<List<Class>, Conversion>> c, Object a) {
		for (Pair<List<Class>, Conversion> pp : c) {
			a = pp.second.converter.apply(a);
		}
		return (T) a;
	}

	static public List<Pair<List<Class>, Conversion>> getConversion(List<Class> from, List<Class> to) {
		Dijkstra<List<Class>, Conversion> d = new Dijkstra<>(x -> x.length, x -> x.output, x -> inputs.get(x));

		List<Class> nto = normalize(to, outputs);
		List<Class> nfrom = normalize(from, inputs);

		if (nto == null) return null;
		if (nfrom == null) return null;

		d.computePaths(nfrom);
		return d.getShortestPathTo(nto);

	}

	private static List<Class> normalize(List<Class> to, SetMultimap<List<Class>, Conversion> m) {
		if (m.containsKey(to)) return to;

		List<List<Class>> alternatives = new ArrayList<>();
		for (Class c : to) {
			alternatives.add(new ArrayList<>(alternativesFor(c)));
		}

		int[] counts = new int[to.size()];
		while (true) {
			counts[0] += 1;
			int in = 0;
			while (counts[in] > alternatives.get(in)
							.size() - 1) {
				counts[in] = 0;
				in++;
				if (in > alternatives.size() - 1) return null;
				counts[in]++;
			}

			List<Class> assembled = new ArrayList<>();
			for (int index = 0; index < counts.length; index++) {
				assembled.add(alternatives.get(index)
							  .get(counts[index]));
			}

			if (m.containsKey(assembled)) return assembled;
		}
	}

	public static Set<List<Class>> genericAlternativesFor(Type c) {
		if (c == null) return Collections.emptySet();

		Set<List<Class>> alt = new LinkedHashSet<>();

		if (c instanceof Class) {
			alt.add(Collections.singletonList((Class) c));
			alt.addAll(genericAlternativesFor(((Class) c).getGenericSuperclass()));
			Type[] ii = ((Class) c).getGenericInterfaces();
			if (ii != null) for (Type iii : ii)
				alt.addAll(genericAlternativesFor(iii));
		}
		if (c instanceof ParameterizedType) alt.add(linearize(c));

		return alt;
	}

	public static Set<Class> alternativesFor(Class c) {
		if (c == null) return Collections.emptySet();

		Set<Class> alt = new LinkedHashSet<>();

		alt.add(c);
		alt.addAll(alternativesFor(c.getSuperclass()));
		Class[] cc = c.getInterfaces();
		if (cc != null) {
			for (Class ccc : cc)
				alt.addAll(alternativesFor(ccc));
		}
		return alt;
	}


	static public Object convert(Object value, List<Class> fit) {
		String[] ei = {null};

		Object o = _convert(value, fit, m -> {
			ei[0] = m;
		});

		// set error consumer on conversion

		if (o instanceof Errors.SavesErrorConsumer) {
			if (value instanceof Errors.SavesErrorConsumer) {
				((Errors.SavesErrorConsumer) o).setErrorConsumer(wrap(ei[0], ((Errors.SavesErrorConsumer) value).getErrorConsumer()));
			} else if (value instanceof Errors.ErrorConsumer) {
				((Errors.SavesErrorConsumer) o).setErrorConsumer(wrap(ei[0], ((Errors.
					    ErrorConsumer) value)));
			} else {
				((Errors.SavesErrorConsumer) o).setErrorConsumer(wrap(ei[0], Errors.errors.get()));
			}
		}
		return o;
	}

	private static Errors.ErrorConsumer wrap(String s, Errors.ErrorConsumer errorConsumer) {

		return (t, m) -> {
			errorConsumer.consume(t, s + "||" + m);
		};

	}


	static public Object convert(Object value, Class fit) {
		return convert(value, Collections.singletonList(fit));
	}


	static protected Object _convert(Object value, List<Class> fit, Consumer<String> extraInfo) {

		if (fit == null) return value;
		if (fit.get(0)
		       .isInstance(value)) return value;

		// promote non-arrays to arrays
		if (List.class.isAssignableFrom(fit.get(0))) {
			if (!(value instanceof List)) {
				return Collections.singletonList(_convert(value, fit.subList(1, fit.size()), extraInfo));
			} else {
				return value;
			}
		} else if (Map.class.isAssignableFrom(fit.get(0)) && String.class.isAssignableFrom(fit.get(1))) {
			// promote non-Map<String, V> to Map<String, V>
			if (!(value instanceof Map)) {
				return Collections.singletonMap("" + value + ":" + System.identityHashCode(value), _convert(value, fit.subList(2, fit.size()), extraInfo));
			} else {
				return value;
			}

		} else if (Collection.class.isAssignableFrom(fit.get(0))) {
			if (!(value instanceof Collection)) {
				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
			} else {
				return value;
			}

		}

		if (fit.get(0)
		       .isInterface() && value instanceof InvocationHandler) {
			return Proxy.newProxyInstance(Thread.currentThread()
							    .getContextClassLoader(), new Class[]{fit.get(0)}, (InvocationHandler) value);
		}


		if (value instanceof ScriptObjectMirror) return convert(ScriptUtils.unwrap(value), fit);


		if (value instanceof ScriptFunctionImpl) {
			StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{fit.get(0)}, (ScriptObject) value, MethodHandles.lookup());

			try {
				String functionName = (String) ReflectionTools.get(value, "data/functionName");
				Integer lineNumber = (Integer) ReflectionTools.get(value, "data/lineNumber");
				String url = (String) ReflectionTools.get(value, "data/source/explicitURL");

				System.out.println(" extra secret information about this function is :" + functionName + " " + lineNumber + " " + url);

				extraInfo.accept("LN<"+lineNumber + "@" + url + "> function is called [" + functionName + "]");

			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			try {
				return adapterClassFor.getRepresentedClass()
						      .newInstance();
			} catch (InstantiationException e) {
				Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
			} catch (IllegalAccessException e) {
				Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
			}
		}


		return value;
	}

}
