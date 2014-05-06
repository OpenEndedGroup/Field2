package field.utility;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public class Conversions {

	/**
	 * note: this doesn't do interfaces right now
	 *
	 * @param c
	 * @return
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

	static public boolean typeInformationEquals(List<Class> c1, List<Class> c2) {
		if (c1.size() != c2.size()) return false;
		for (int i = 0; i < c1.size(); i++) {
			if (!c1.get(i).equals(c2.get(i))) return false;
		}
		return true;
	}


	static public <A, B> Pair<List<Class>, List<Class>> function(Function<A, B> f) {
		Type inter = f.getClass().getGenericInterfaces()[0];

		if (inter instanceof Class)
			throw new IllegalArgumentException("alas, you can't pass in a lambda to function <"+inter+">");

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
			return "c<"+name+">"+length;
		}
	}

	static SetMultimap<List<Class>, Conversion> inputs = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();
	static SetMultimap<List<Class>, Conversion> outputs = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();

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

		System.out.println(" REGISTERED conversion "+length+" "+conversion.input+" -> "+conversion.output);

	}

	static public List<Pair<List<Class>, Conversion>> getConversion(Object from, List<Class> to) {
		Set<List<Class>> alt = genericAlternativesFor(from.getClass());

		for (List<Class> c : alt)
		{
			if (c.get(0).getName().contains("$$Lambda$")) throw new IllegalArgumentException(" alas, you cannot pass in a lambda into getConversion ");
			List<Pair<List<Class>, Conversion>> r = getConversion(c, to);
			if (r!=null) return r;
		}

		System.out.println(" no conversion found ");

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
			while (counts[in] > alternatives.get(in).size()-1) {
				counts[in] = 0;
				in++;
				if (in > alternatives.size() - 1) return null;
				counts[in]++;
			}

			List<Class> assembled = new ArrayList<>();
			for (int index = 0; index < counts.length; index++) {
				assembled.add(alternatives.get(index).get(counts[index]));
			}

			if (m.containsKey(assembled)) return assembled;
		}
	}

	public static Set<List<Class>> genericAlternativesFor(Type c) {
		if (c == null) return Collections.emptySet();

		Set<List<Class>> alt = new LinkedHashSet<>();

		if (c instanceof Class)
		{
			alt.add( Collections.singletonList((Class) c));
			alt.addAll( genericAlternativesFor( ((Class)c).getGenericSuperclass()));
			Type[] ii = ((Class)c).getGenericInterfaces();
			if (ii!=null)
				for(Type iii : ii)
					alt.addAll(genericAlternativesFor(iii));
		}
		if (c instanceof ParameterizedType)
			alt.add( linearize(c));

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


}
