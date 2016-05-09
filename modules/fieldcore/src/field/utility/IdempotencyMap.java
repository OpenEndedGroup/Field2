package field.utility;

import jdk.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;
import fieldlinker.Linker;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IdempotencyMap<T> extends LinkedHashMapAndArrayList<T> implements Mutable<IdempotencyMap<T>>, Linker.AsMap {

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
		value = Conversions.convert(value, t);
//		if (value instanceof ScriptObjectMirror)
//			value = ScriptUtils.unwrap(value);
//
//		if (value instanceof ScriptFunction) {
//			StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{t}, (ScriptObject) value, MethodHandles.lookup());
//			try {
//				return (T) adapterClassFor.getRepresentedClass()
//							  .newInstance();
//			} catch (InstantiationException e) {
//				Object fv = value;
//				Log.log("processing.error", ()->" problem instantiating adaptor class to take us from " + fv + " ->" + t+ e);
//			} catch (IllegalAccessException e) {
//				Object fv = value;
//				Log.log("processing.error", ()->" problem instantiating adaptor class to take us from " + fv + " ->" + t+e);
//			}
//		}

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
		throw new NotImplementedException()
			;
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
		return new NotImplementedException();
	}

	@Override
	public Object asMap_new(Object o, Object o1) {
		return new NotImplementedException();
	}

	@Override
	public Object asMap_getElement(int i) {
		return new NotImplementedException();
	}

	@Override
	public Object asMap_setElement(int i, Object o) {
		return new NotImplementedException();
	}

	@Override
	public boolean asMap_delete(Object p) {
		return remove(p) != null;
	}

	private class AllOf implements Linker.AsMap {


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
