package field.utility;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.MapMaker;
import sun.reflect.CallerSensitive;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * This is our typed map class (Dict) with typed map keys (Dict.Prop) with some brain surgery level code to get type information out of Java's
 * generics. It's used throughout Field (the type information is important because it enables the Conversions class which in turn allows us to massage
 * dynamic languages well while style exploiting Java's type system)
 */
public class Dict implements Serializable {
	private static final long serialVersionUID = 4506062700963421662L;

	static public class Canonical {
		static protected Map<String, Prop> cannon = Collections.synchronizedMap(new HashMap<>());

		static public <T> Prop<T> cannonicalize(Prop<T> p) {
			Prop<T> prop = cannon.computeIfAbsent(p.name, x -> p);
			if (p.isCannon() && !prop.isCannon()) {
				cannon.put(p.name, p);
				prop = p;
			} else if (p.isCannon() && prop.isCannon() && p != prop) {
				// should be an Error?
				System.err.println(" WARNING: two competing canonical definitions of a Prop");
				if (p.typeInformation != null && prop.typeInformation == null) {
					cannon.put(p.name, p);
					prop = p;
				} else if (p.typeInformation == null && prop.typeInformation != null) {

				} else if (p.typeInformation != null && prop.typeInformation != null) {
					if (!Conversions.typeInformationEquals(p.typeInformation, prop.typeInformation)) {
						System.err.println(" ERROR: the two competing canonical definitions of " + p + " have different type information");
						throw new IllegalArgumentException(p.typeInformation + " " + prop.typeInformation + " " + p + " " + prop);
					}
				}
			}
			prop.setCannon();
			return prop;
		}

		static public <T> Prop<T> findCannon(Prop<T> p)
		{
			return cannon.get(p.name);
		}

	}

	static public class Prop<T> implements Serializable {
		String name;
		int flags;

		// optional type information
		private List<Class> typeInformation;
		private Class definedInClass;
		private String documentation;

		public Prop(String name) {
			this.name = name;
		}

		public boolean containsSuffix(String string) {
			if (name.contains("_")) {
				String[] s = name.split("_");
				for (int i = 1; i < s.length; i++) {
					if (s[i].equals(string)) return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final Prop other = (Prop) obj;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			return true;
		}

		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return name + (typeInformation == null ? "" : "" + typeInformation);
		}

		protected void setCannon() {
			flags |= 1;
		}

		public boolean isCannon() {
			return (flags & 1) != 0;
		}

		public <T> Prop<T> toCannon() {
			return (Prop<T>) Canonical.cannonicalize(this);
		}

		public <T> Prop<T> doc(String doc) {
			this.documentation = doc;
			return (Prop<T>) this;
		}

		public String getDocumentation() {
			return documentation;
		}

		@CallerSensitive
		public <T> Prop<T> type() {
			Class c = sun.reflect.Reflection.getCallerClass(2);

			definedInClass = c;

			Field f = null;
			try {
				f = c.getField(name);
			} catch (NoSuchFieldException e) {
				if (name.startsWith("_")) try {
					f = c.getField(name.substring(1));
				} catch (NoSuchFieldException e1) {
					if (name.startsWith("__")) try {
						f = c.getField(name.substring(1));
					} catch (NoSuchFieldException e2) {
					}
				}
			}
			if (f == null)
				throw new IllegalStateException(" cannot type a Dict.Prop<T> that we can't find. Name is :" + name + " class is :" + c);

			typeInformation = Conversions.linearize(f.getGenericType());

			typeInformation.remove(0);

			setCannon();

			return (Prop<T>) this;
		}

		public List<Class> getTypeInformation() {
			return typeInformation == null ? null : new ArrayList<Class>(typeInformation);
		}

		public Prop<T> findCannon() {
			return Canonical.findCannon(this);
		}
	}

	Map<Prop, Object> dictionary = new MapMaker().concurrencyLevel(2).makeMap();

	@SuppressWarnings("unchecked")
	public <T> T get(Prop<T> key) {
		return (T) dictionary.get(key);
	}

	public <T> T getOr(Prop<T> key, Supplier<T> def) {
		T t = (T) get(key);
		if (t == null) return def.get();
		return t;
	}

	public <T> T computeIfAbsent(Prop<T> k, Function<Prop<T>, T> def) {

		// this is redundant, but it makes sure we register a 'get'
		T t = get(k);
		if (t!=null) return t;

		return (T) dictionary.computeIfAbsent(k, (x) -> def.apply(k));
	}

	public float getFloat(Prop<? extends Number> n, float def) {
		Object x = get(n);
		if (x instanceof Float[]) return ((Float[]) x)[0].floatValue();
		if (x instanceof float[]) return ((float[]) x)[0];

		Number gotten = (Number) x;
		if (gotten != null) return gotten.floatValue();
		return def;
	}


	public Dict duplicate() {
		Dict r = new Dict();

		r.dictionary = new LinkedHashMap<Prop, Object>(dictionary);
		return r;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final Dict other = (Dict) obj;
		if (dictionary == null) {
			if (other.dictionary != null) return false;
		} else if (!dictionary.equals(other.dictionary)) return false;
		return true;
	}


	public Map<Prop, Object> getMap() {
		return dictionary;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dictionary == null) ? 0 : safeHash(dictionary));
		return result;
	}

	private int safeHash(Map<Prop, Object> d) {

		Set<Entry<Prop, Object>> ee = d.entrySet();
		int c = 0;
		for (Entry<Prop, Object> e : ee) {
			Prop k = e.getKey();
			Object v = e.getValue();
			c += k.hashCode();
			c += (v == null ? 0 : v.hashCode());
		}
		return c;
	}

	public boolean isTrue(Prop<?> prop, boolean def) {
		if (!dictionary.containsKey(prop)) return def;

		Object p = dictionary.get(prop);
		if (p == null) return def;
		if (p instanceof Boolean) return ((Boolean) p);
		if (p instanceof Number) return ((Number) p).intValue() > 0;
		if (p instanceof String) return ((String) p).length() > 0;
		return true;
	}


	public <T> Dict put(Prop<T> key, T value) {
		dictionary.put(key, value);
		return this;
	}

	@SuppressWarnings("unchecked")
	public Dict putAll(Dict d) {
		for (Map.Entry<Prop, Object> e : d.dictionary.entrySet()) {
			put(e.getKey(), e.getValue());
		}
		return this;
	}

	/**
	 * putAll with a chance to handle collisions. Function3<Key, Object_here, Object_there, Object_or_null_as_merged>
	 */
	public Dict putAll(Dict d, Curry.Function3<Object, Object, Object, Object> collision) {
		for (Map.Entry<Prop, Object> e : d.dictionary.entrySet()) {
			if (!dictionary.containsKey(e.getKey())) put(e.getKey(), e.getValue());
			else {
				Object r = collision.apply(e.getKey(), dictionary.get(e.getKey()), e.getValue());
				if (r != null) put(e.getKey(), r);
			}
		}
		return this;
	}


	public <T> Dict putToList(Prop<? extends Collection<T>> key, T value) {
		Collection<T> c = (Collection<T>) dictionary.computeIfAbsent(key, (k) -> new ArrayList<T>());
		c.add(value);
		return this;
	}

	public <K, T> Dict putToMap(Prop<? extends Map<String, T>> key, K tok, T value) {
		Map<K, T> c = (Map<K, T>) dictionary.computeIfAbsent(key, (k) -> new LinkedHashMap<K, T>());
		c.put(tok, value);
		return this;
	}

	public <K, T> T getFromMap(Prop<? extends Map<String, T>> key, K tok) {
		Map<K, T> c = (Map<K, T>) get(key);
		if (c == null) return null;
		return c.get(tok);
	}


	public <T> void removeFromCollection(Prop<? extends Collection<T>> p, T q) {
		Collection<T> ll = get(p);
		if (ll == null) return;
		ll.remove(q);
	}


	public <K, T> T removeFromMap(Prop<? extends Map<K, T>> p, K q) {
		Map<K, T> ll = get(p);
		if (ll == null) return null;
		return ll.remove(q);
	}

	@Override
	public String toString() {
		return "" + dictionary;
	}

	public <T> T remove(Prop<T> t) {
		Object x = dictionary.remove(t);
		return (T) x;
	}

	public long longHash() {
		long start = 1;
		Set<Entry<Prop, Object>> e = dictionary.entrySet();
		for (Entry ee : e) {
			start = start * 31L + (long) (/*ee.getValue() instanceof PyObject ? System.identityHashCode(ee.getValue()) :*/ ee.hashCode());
		}
		return start;
	}

	public long longHash(Set<String> ex) {
		long start = 1;
		Set<Entry<Prop, Object>> e = dictionary.entrySet();
		for (Entry<Prop, Object> ee : e) {
			if (!ex.contains(ee.getKey().name))
				start = start * 31L + (long) (/*ee.getValue() instanceof PyObject ? System.identityHashCode(ee.getValue()) :*/ ee
					    .hashCode());
		}
		return start;
	}

	public boolean has(Prop<?> context) {
		return dictionary.containsKey(context);
	}

	public void removeValue(Object c) {
		Set<Entry<Prop, Object>> es = dictionary.entrySet();
		Iterator<Entry<Prop, Object>> is = es.iterator();
		while (is.hasNext()) {
			Entry<Prop, Object> n = is.next();
			if (n.getValue().equals(c)) is.remove();
		}
	}

	public void multiply(Prop<Float> k, float missingValue, float by) {
		put(k, getFloat(k, missingValue) * by);
	}


}
