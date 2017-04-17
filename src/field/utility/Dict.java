package field.utility;

import com.google.common.collect.MapMaker;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import fieldbox.boxes.plugins.BoxDefaultCode;
import fieldbox.execution.Execution;
import fieldbox.execution.JavaSupport;
import fieldlinker.Linker;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

//import sun.reflect.CallerSensitive;
//;


/**
 * This is our typed map class (Dict) with typed map keys (Dict.Prop) with some brain surgery level code to get type information out of Java's
 * generics. It's used throughout Field (the type information is important because it enables the Conversions class which in turn allows us to massage
 * dynamic languages well while style exploiting Java's type system)
 */
public class Dict implements Serializable, fieldlinker.AsMap {
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
				System.err.println(" WARNING: two competing canonical definitions of a Prop <" + p + ">");
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

		static public <T> Prop<T> findCannon(Prop<T> p) {
			return cannon.get(p.name);
		}

		static public <T> Prop<T> findCannon(String p) {
			return cannon.get(p);
		}

	}

	/**
	 * this property is used to tag other properties as pertaining to a particular "domain" of use, for example FLine attributes
	 */
	public static Dict.Prop<String> domain = new Dict.Prop<>("domain").toCannon();
	/**
	 * this property tags a property as writeOnly (from the poiint of view of the scripting world, from Java there's nothing in place to prevent writing properties)
	 */
	public static Prop<Boolean> writeOnly = new Prop<>("writeOnly").toCannon().set(domain, "*/attributes");
	/**
	 * this lets you add to a property a function that massages values as they are set
	 */
	public static Prop<Function<Object, Object>> customCaster = new Prop<>("customCaster").toCannon().set(domain, "*/attributes");

	static {
		domain.set(domain, "*/attributes");
	}

	static public class Prop<T> implements Serializable, fieldlinker.AsMap {
		String name;

		// optional type information
		private List<Class> typeInformation;
		public Class definedInClass;
		private String documentation;

		public Supplier<T> autoConstructor;

		private Dict attributes;

		private boolean cannon = false;

		public Prop(String name) {
			this.name = name;

			if (false)
			{
				StackTraceElement[] s = new Exception().getStackTrace();
				String[] pieces = s[1].getClassName()
					.split("\\.");
				documentation = "undocumented (defined inside <b>" + pieces[pieces.length - 1] + "</b>)";
			}
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
			cannon = true;
		}

		public boolean isCannon() {
			return cannon;
		}

		public <T> Prop<T> toCannon() {
			return (Prop<T>) Canonical.cannonicalize(this);
		}

		public <T> Prop<T> doc(String doc) {
			Prop on = this;
			if (!isCannon()) {
				Prop<T> already = (Prop<T>) findCannon();
				if (already == null) {
					toCannon();
					on.setCannon();
				} else {
					on = already;
				}
			}

			on.documentation = doc;
			return (Prop<T>) on;
		}

		public String getDocumentation() {
			return documentation;
		}

		public Class getDefiningClass()
		{
			return definedInClass;
		}

		public String getExtendedDocumentation(){
			Prop on = this;
			if (!isCannon()) {
				Prop<T> already = (Prop<T>) findCannon();
				if (already == null) {
					toCannon();
					on.setCannon();
				} else {
					on = already;
				}
			}
			if (on.definedInClass==null) return null;

			String type1 = BoxDefaultCode.find(on.definedInClass, getName() + ".documentation.md");
			if (type1!=null) return type1;

			try{
				JavaClass source = JavaSupport.javaSupport.sourceForClass(definedInClass);
				JavaField n = source.getFieldByName(name);
				if (n==null)
					n = source.getFieldByName("_"+name);
				if (n==null)
					n = source.getFieldByName("__"+name);
				if (n==null) return null;

				return n.getComment();
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
			return null;
		}

		public <T> Prop<T> type() {

			Prop on = this;
			if (!isCannon()) {
				Prop<T> already = (Prop<T>) findCannon();
				if (already == null) {
					toCannon();
					on.setCannon();
				} else {
					on = already;
				}
			}

			Class c = _getCallerClass(2);

			on.definedInClass = c;

			Field f = null;
			try {
				f = c.getField(name);
			} catch (NoSuchFieldException e) {

				try {
					f = c.getField("_" + name);
				} catch (NoSuchFieldException e3) {
					if (name.startsWith("_")) try {
						f = c.getField(name.substring(1));
					} catch (NoSuchFieldException e1) {
						if (name.startsWith("__")) try {
							f = c.getField(name.substring(1));
						} catch (NoSuchFieldException e2) {
						}
					}

				}


			}
			if (f == null)
				throw new IllegalStateException(" cannot type a Dict.Prop<T> that we can't find. Name is :" + name + " class is :" + c);

			on.typeInformation = Conversions.linearize(f.getGenericType());

			on.typeInformation.remove(0);


			return (Prop<T>) on;
		}

		private Class _getCallerClass(int i) {
			return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(x -> {

				for (Iterator<StackWalker.StackFrame> it = x.iterator(); it.hasNext(); ) {
					StackWalker.StackFrame f = it.next();
					if (!f.getClassName().equals(this.getClass().getName()))
						return f.getDeclaringClass();
				}
				return null;
			});
		}

		public <T> Prop<T> autoConstructs(Supplier<T> t) {
			Prop on = this;
			if (!isCannon()) {
				Prop<T> already = (Prop<T>) findCannon();
				if (already == null) {
					toCannon();
					on.setCannon();
				} else {
					on = already;
				}
			}


			on.autoConstructor = (Supplier) t;
			return (Prop<T>) on;
		}


		public List<Class> getTypeInformation() {
			return typeInformation == null ? null : new ArrayList<Class>(typeInformation);
		}

		public Prop<T> findCannon() {
			return Canonical.findCannon(this);
		}

		public Dict getAttributes() {
			Prop on = this;
			if (!isCannon()) {
				Prop<T> already = findCannon();
				if (already == null) {
					toCannon();
					on.setCannon();
				} else {
					on = already;
				}
			}
			return on.attributes == null ? (on.attributes = new Dict()) : on.attributes;
		}

		// typing issues with javac, no need for this additional <V>
		public <Q, V> Prop<V> set(Prop<Q> p, Q v) {
			getAttributes().put(p, v);
			return (Prop<V>) this;
		}

		public <V> Prop<V> set(Prop<Boolean> m) {
			getAttributes().put(m, true);
			return (Prop<V>)this;
		}


		@Override
		public boolean asMap_isProperty(String p) {
			return Canonical.findCannon(p) != null;
		}

		@Override
		public Object asMap_call(Object o, Object o1) {
			return new IllegalArgumentException("" + o + " / " + o1);
		}

		@Override
		public Object asMap_call(Object o) {
			return Execution.context.get().peek().asMap_get(this.getName());
		}

		@Override
		public Object asMap_get(String p) {
			return getAttributes().asMap_get(p);
		}

		@Override
		public Object asMap_new(Object a) {
			return null;
		}

		@Override
		public Object asMap_getElement(int element) {
			return asMap_get(element + "");
		}

		@Override
		public Object asMap_getElement(Object element) {
			return asMap_get("" + element);
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			return null;
		}

		@Override
		public Object asMap_set(String p, Object o) {
			return getAttributes().asMap_set(p, o);
		}

		@Override
		public Object asMap_setElement(int element, Object o) {
			return getAttributes().asMap_setElement(element, o);
		}

		@Override
		public boolean asMap_delete(Object o) {
			return getAttributes().asMap_delete(o);
		}

	}

	Map<Prop, Object> dictionary = new MapMaker().concurrencyLevel(2)
		.makeMap();

	Function<Prop, Object> failure = null;

	@SuppressWarnings("unchecked")
	public <T> T get(Prop<T> key) {
		Object o = dictionary.get(key);
		if (failure != null && o == null && !dictionary.containsKey(key))
			return (T) failure.apply(key);
		return (T) o;
	}

	public Dict setFailure(Function<Prop, Object> failure) {
		this.failure = failure;
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T getOrConstruct(Prop<T> key) {

		Prop<T> cc = key.findCannon();
		if (cc != null && cc.autoConstructor != null) {
			return (T) dictionary.computeIfAbsent(key, k -> cc.autoConstructor.get());
		}
		return get(key);
	}

	public <T> T getOr(Prop<T> key, Supplier<T> def) {
		T t = get(key);
		if (t == null) return def.get();
		return t;
	}

	public <T> T computeIfAbsent(Prop<T> k, Function<Prop<T>, T> def) {

		// this is redundant, but it makes sure we register a 'get'
		T t = get(k);
		if (t != null) return t;

		return (T) dictionary.computeIfAbsent(k, (x) -> def.apply(k));
	}

	public float getFloat(Prop<? extends Number> n, float def) {
		Object x = get(n);
		if (x instanceof Float[]) return ((Float[]) x)[0].floatValue();
		if (x instanceof Boolean) return ((Boolean) x).booleanValue() ? 1 : 0f;
		if (x instanceof float[]) return ((float[]) x)[0];

		Number gotten = (Number) x;
		if (gotten != null) return gotten.floatValue();
		return def;
	}


	public Dict duplicate() {
		Dict r = new Dict();

		r.dictionary = new LinkedHashMap<Prop, Object>(dictionary.size());
		for (Map.Entry<Prop, Object> e : dictionary.entrySet()) {
			r.dictionary.put(e.getKey(), e.getValue() instanceof Mutable ? ((Mutable) e.getValue()).duplicate() : e.getValue());
		}
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
		if (value == null) return this;

		dictionary.put(key, value);
		return this;
	}

	/**
	 * returns a dict of the things displaced by putting this
	 */
	public Dict putAll(Dict d) {
		Dict r = new Dict();
		for (Map.Entry<Prop, Object> e : d.dictionary.entrySet()) {
			Object was = get(e.getKey());
			put(e.getKey(), e.getValue());
			if (was != null) r.put(e.getKey(), was instanceof Mutable ? ((Mutable) was).duplicate() : was);
		}
		return r;
	}

	/**
	 * putAll with a chance to handle collisions. Function3<Key, Object_here, Object_there, Object_or_null_as_merged>
	 */
	public Dict putAll(Dict d, Curry.Function3<Object, Object, Object, Object> collision) {
		for (Map.Entry<Prop, Object> e : d.dictionary.entrySet()) {
			if (!dictionary.containsKey(e.getKey())) put(e.getKey(), e.getValue());
			else {
				Object r = collision.apply(e.getKey(), dictionary.get(e.getKey()), e.getValue());
				if (r != null) put(e.getKey(), r instanceof Mutable ? ((Mutable) r).duplicate() : r);
			}
		}
		return this;
	}


	/**
	 * putAll, filtered
	 */
	public Dict putAll(Dict d, Function<Prop, Boolean> filter) {
		for (Map.Entry<Prop, Object> e : d.dictionary.entrySet()) {

			if (filter.apply(e.getKey())) {
				put(e.getKey(), e.getValue() instanceof Mutable ? ((Mutable) e.getValue()).duplicate() : e.getValue());
			}
		}
		return this;
	}


	public <T> Dict putToList(Prop<? extends Collection<T>> key, T value) {

		if (key.toCannon().autoConstructor != null) {
			Collection<T> c = (Collection<T>) dictionary.computeIfAbsent(key, (k) -> key.toCannon().autoConstructor.get());
			c.add(value);
			return this;
		} else {
			Collection<T> c = (Collection<T>) dictionary.computeIfAbsent(key, (k) -> new ArrayList<T>());
			c.add(value);
			return this;
		}
	}

	public <T> Dict putToList(Prop<? extends Collection<T>> key, T value, Supplier<? extends Collection<T>> def) {

		if (key.toCannon().autoConstructor != null) {
			Collection<T> c = (Collection<T>) dictionary.computeIfAbsent(key, (k) -> key.toCannon().autoConstructor.get());
			c.add(value);
			return this;
		} else {
			Collection<T> c = (Collection<T>) dictionary.computeIfAbsent(key, (k) -> def.get());
			c.add(value);
			return this;
		}
	}

	public <T> Dict putToListMap(Prop<? extends LinkedHashMapAndArrayList<T>> key, T value) {

		if (key.toCannon().autoConstructor != null) {
			LinkedHashMapAndArrayList<T> c = (LinkedHashMapAndArrayList<T>) dictionary.computeIfAbsent(key, (k) -> key.toCannon().autoConstructor.get());
			c.add(value);
			return this;
		} else {
			LinkedHashMapAndArrayList<T> c = (LinkedHashMapAndArrayList<T>) dictionary.computeIfAbsent(key, (k) -> new ArrayList<T>());
			c.add(value);
			return this;
		}
	}

	public <K, T> Dict putToMap(Prop<? extends Map<String, T>> key, K tok, T value) {
		if (key.toCannon().autoConstructor != null) {
			Map<K, T> c = (Map<K, T>) dictionary.computeIfAbsent(key, (k) -> key.toCannon().autoConstructor.get());
			c.put(tok, value);
			return this;
		} else {
			Map<K, T> c = (Map<K, T>) dictionary.computeIfAbsent(key, (k) -> new IdempotencyMap<T>(null));
			c.put(tok, value);
			return this;
		}
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
				start = start * 31L + (long) (/*ee.getValue() instanceof PyObject ? System.identityHashCode(ee.getValue()) :*/ ee.hashCode());
		}
		return start;
	}

	public boolean has(Prop<?> context) {
		return dictionary.containsKey(context);
	}

	/**
	 * null-safe equals.
	 */
	public <T> boolean equals(Prop<T> context, T v) {
		Object d = dictionary.get(context);
		if (d == null) return v == null;
		return d.equals(v);
	}

	public void removeValue(Object c) {
		Set<Entry<Prop, Object>> es = dictionary.entrySet();
		Iterator<Entry<Prop, Object>> is = es.iterator();
		while (is.hasNext()) {
			Entry<Prop, Object> n = is.next();
			if (n.getValue()
				.equals(c)) is.remove();
		}
	}

	public void multiply(Prop<Float> k, float missingValue, float by) {
		put(k, getFloat(k, missingValue) * by);
	}


	@Override
	public boolean asMap_isProperty(String p) {

//		return has(new Prop(p));
		return true;
	}

	@Override
	public Object asMap_call(Object a, Object b) {
		throw new Error();
	}

	@Override
	public Object asMap_get(String p) {
		return get(new Prop(p));
	}

	@Override
	public boolean asMap_delete(Object o) {
		return remove(new Prop("" + o)) != null;
	}

	@Override
	public Object asMap_set(String p, Object o) {
		Dict r = put(new Prop(p), o);
		return r;
	}

	@Override
	public Object asMap_new(Object a) {
		throw new Error();
	}

	@Override
	public Object asMap_new(Object a, Object b) {
		throw new Error();
	}

	@Override
	public Object asMap_getElement(int element) {
		throw new Error();
	}

	@Override
	public Object asMap_setElement(int element, Object v) {
		throw new Error();
	}


	static public Stream<Prop> cannonicalProperties() {
		return Canonical.cannon.values().stream();
	}

}
