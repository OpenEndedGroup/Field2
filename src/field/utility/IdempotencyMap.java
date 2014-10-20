package field.utility;

import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.internal.objects.ScriptFunctionImpl;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

public class IdempotencyMap<T> extends LinkedHashMapAndArrayList<T> implements Mutable<IdempotencyMap<T>> {

	private final Class<T> t;

	public IdempotencyMap(Class<T> t) {
		this.t = t;
	}


	@Override
	protected T massage(Object value) {
		if (t==null) return (T)value;
		if (value == null) return null;
		if (t.isAssignableFrom(value.getClass())) return (T) value;
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
}
