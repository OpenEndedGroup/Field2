package field.utility;

import field.dynalink.beans.StaticClass;
import field.nashorn.api.scripting.ScriptObjectMirror;
import field.nashorn.api.scripting.ScriptUtils;
import field.nashorn.internal.objects.ScriptFunctionImpl;
import field.nashorn.internal.runtime.ScriptObject;
import field.nashorn.internal.runtime.linker.JavaAdapterFactory;
import fieldlinker.Linker;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.function.Function;

public class IdempotencyMap<T> extends LinkedHashMapAndArrayList<T> implements Mutable<IdempotencyMap<T>>, Linker.AsMap{

	private final Class<? extends T> t;
	private Function<String, T> autoConstructor;

	public IdempotencyMap(Class t) {
		this.t = t;
	}

	public IdempotencyMap<T> setAutoconstruct(Function<String, T> auto)
	{
		this.autoConstructor = auto;
		return this;
	}


	@Override
	protected T massage(Object value) {
		if (t==null) return (T)value;
		if (value == null) return null;
		if (t.isAssignableFrom(value.getClass())) return (T) value;

		if (value instanceof ScriptObjectMirror)
			value = ScriptUtils.unwrap(value);

		if (value instanceof ScriptFunctionImpl) {
			StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{t}, (ScriptObject) value, MethodHandles.lookup());
			try {
				return (T) adapterClassFor.getRepresentedClass()
							  .newInstance();
			} catch (InstantiationException e) {
				Log.log("processing.error", " problem instantiating adaptor class to take us from " + value + " ->" + t, e);
			} catch (IllegalAccessException e) {
				Log.log("processing.error", " problem instantiating adaptor class to take us from " + value + " ->" + t, e);
			}
		}


		throw new ClassCastException(" expected " + t + ", got " + value + " / " + value.getClass());
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
		if (!containsKey(key) && autoConstructor!=null)
		{
			T t = autoConstructor.apply((String)key);
			if (t!=null) {
				put((String)key, t);
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
;	}

	@Override
	public Object asMap_get(String s) {


		System.out.println("\n\n >> get :"+s+" \n\n");

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
}
