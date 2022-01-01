package field.utility;

import fieldbox.execution.Completion;
import fieldbox.execution.HandlesCompletion;
import fieldnashorn.annotations.SafeToToString;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptUtils;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IdempotencyMap<T> extends LinkedHashMapAndArrayList<T> implements Mutable<IdempotencyMap<T>>, fieldlinker.AsMap, HandlesCompletion {

	private final Class<? extends T> t;
	private Function<String, T> autoConstructor;

	public IdempotencyMap(Class t) {
		this.t = t;
	}

	public IdempotencyMap<T> setAutoconstruct(Function<String, T> auto) {
		this.autoConstructor = auto;
		return this;
	}


	public IdempotencyMap<T> setAutoconstruct(Class clazz) {
		this.autoConstructor = (k) -> {
			try {
				return (T) clazz.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return null;
		};
		return this;
	}

	@Override
	protected T massage(Object value) {
		if (t == null) return (T) value;
		if (value == null) return null;
		if (t.isAssignableFrom(value.getClass())) return (T) value;

		Object ovalue = value;

		if (value instanceof ScriptObjectMirror)
			value = ScriptUtils.unwrap(value);

		value = Conversions.convert(value, t);

		if (value != null && t.isAssignableFrom(value.getClass())) return (T) value;

		if (value == null) {
			throw new ClassCastException(" couldn't convert " + ovalue + " of class " + ovalue.getClass() + " to " + t);
		}
		if (!t.isAssignableFrom(value.getClass()))
			throw new ClassCastException(" expected " + t + ", got " + value + " / " + value.getClass());

		return (T) value;
	}

	@Override
	public IdempotencyMap<T> duplicate() {
		IdempotencyMap<T> r = new IdempotencyMap<>(t);

		for (Map.Entry<String, T> e : this.entrySet()) {
			r.put(e.getKey(), e.getValue() instanceof Mutable ? ((Mutable) e.getValue()).duplicate() : e.getValue());
		}

		return r;
	}


	@Override
	public boolean containsKey(Object key) {
		key = massageKey("" + key);
		if (!super.containsKey(key)) {
			if (autoConstructor != null) {
				T t = autoConstructor.apply((String) key);
				if (t != null) {
					_put((String) key, t);
					return true;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public T get(Object key) {
		key = massageKey("" + key);

		if (!containsKey(key) && autoConstructor != null) {
			T t = autoConstructor.apply((String) key);
			if (t != null) {
				put((String) key, t);
				return t;
			}
		}
		return super.get(key);
	}


	@Override
	public boolean asMap_isProperty(String s) {
		return containsKey(s);
	}

	@Override
	public Object asMap_call(Object o, Object o1) {
		throw new Error();
	}

	@Override
	public Object asMap_get(String s) {
		if (s.equals("allOf")) {
			return new AllOf();
		} else
			return get(s);
	}

	@Override
	public Object asMap_set(String s, Object o) {
		return put(s, o);
	}

	@Override
	public Object asMap_new(Object o) {
		return new Error();
	}

	@Override
	public Object asMap_new(Object o, Object o1) {
		return new Error();
	}

	@Override
	public Object asMap_getElement(int i) {
		return new Error();
	}

	@Override
	public Object asMap_setElement(int i, Object o) {
		return new Error();
	}

	@Override
	public boolean asMap_delete(Object p) {
		return remove(p) != null;
	}

	@Override
	public List<Completion> getCompletionsFor(String prefix) {
		List<Completion> c = new ArrayList<>();
		for (Map.Entry<String, T> entry : this.entrySet()) {
			if (entry.getKey().toLowerCase().startsWith(prefix.toLowerCase())) {
				c.add(new Completion(-1, -1, entry.getKey(), messageFor(entry.getValue())));
			}
		}
		return c;
	}

	private String messageFor(T value) {
		if (value == null) return " = null";
		if (value.getClass().isPrimitive()) return " = " + value;
		if (value.getClass().getAnnotation(SafeToToString.class) != null) return " = " + value;
		return "of class " + value.getClass().getName();
	}

	private class AllOf implements fieldlinker.AsMap {


		@Override
		public boolean asMap_isProperty(String p) {
			return IdempotencyMap.this.asMap_isProperty(p);
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			return IdempotencyMap.this.asMap_call(a, b);
		}

		@Override
		public Object asMap_get(String p) {
			return IdempotencyMap.this.entrySet().stream().filter(x -> x.getKey().startsWith("__prefix__." + p + "__")).map(x -> x.getValue()).collect(Collectors.toList());
		}

		@Override
		public Object asMap_set(String p, Object o) {
			if (o instanceof Map) {
				for (Map.Entry<Object, Object> oo : ((Map<Object, Object>) o).entrySet()) {
					IdempotencyMap.this.put("__prefix__." + p + "__" + oo.getKey().toString(), oo.getValue());
				}

			} else if (o instanceof Collection) {
				for (Object oo : ((Collection) o)) {
					IdempotencyMap.this.put("__prefix__." + p + "__", oo);
				}
			} else {
				throw new IllegalArgumentException(".allOf expects a Map or a Collection");
			}
			return o;
		}

		@Override
		public Object asMap_new(Object a) {
			return null;
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			return null;
		}

		@Override
		public Object asMap_getElement(int element) {
			return null;
		}

		@Override
		public Object asMap_setElement(int element, Object o) {
			return null;
		}

		@Override
		public boolean asMap_delete(Object o) {
			return false;
		}
	}


}
