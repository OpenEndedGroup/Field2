package fieldnashorn;

import field.utility.LinkedHashMapAndArrayList;
import field.utility.Log;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.internal.objects.ScriptFunctionImpl;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

import java.lang.invoke.MethodHandles;

public class IdempotencyMap<T> extends LinkedHashMapAndArrayList<T> {

	private final Class<T> t;

	public IdempotencyMap(Class<T> t)
	{
		this.t = t;
	}

	@Override
	protected T massage(Object value) {
		if (value == null) return null;
		if (t.isAssignableFrom(value.getClass()))
			return (T)value;
		if (value instanceof ScriptFunctionImpl) {
			StaticClass adapterClassFor = JavaAdapterFactory
				    .getAdapterClassFor(new Class[]{t}, (ScriptObject) value, MethodHandles.lookup());
			try {
				return (T)adapterClassFor.getRepresentedClass().newInstance();
			} catch (InstantiationException e) {
				Log.log("processing.error", " problem instantiating adaptor class to take us from " + value + " ->" + t, e);
			} catch (IllegalAccessException e) {
				Log.log("processing.error", " problem instantiating adaptor class to take us from " + value + " ->" + t, e);
			}
		}
		throw new ClassCastException(" expected "+t+", got "+value+" / "+value.getClass());
	}
}
