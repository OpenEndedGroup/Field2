package field.utility;

import com.google.common.collect.MapMaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class LinkedHashMapAndArrayList<V> extends LinkedHashMap<String, V> {

	protected int uniq = 0;

	Map<Object, String> keys = new MapMaker().weakKeys().makeMap();

	public void add(Object value)
	{
		while(containsKey("__internal_"+(++uniq)));
		keys.put(value, "__internal_"+uniq);
		_put("__internal_" + (++uniq), massage(value));
	}

	public void addAll(Collection<Object> value)
	{
		for(Object o : value)
			add(o);
	}

	@Override
	public V put(String key, Object value) {
		return _put(key, massage(value));
	}

	// this one is better for writing Java, because it gives you type inference on lambdas as V
	public V _put(String key, V v)
	{
		V displaced = super.put(massageKey(key), v);
		if (displaced!=null)
		{
			_removed(displaced);
		}
		return displaced;
	}

	protected V massage(Object vraw) {
		return (V)vraw;
	}

	protected String massageKey(String k) {
		return k;
	}

	public V remove(Object v)
	{
		Log.log("lhmaal_remove", ()->"removing "+v+" "+this);
		V q = super.remove(v);
		V q2 = super.remove(keys.remove(massageKey(""+v)));
		Log.log("lhmaal_remove",()-> "now "+this);

		if (q!=null)
			_removed(q);
		if (q2!=null)
			_removed(q2);

		return q!=null ? q : q2;
	}

	@Override
	public void clear() {
		ArrayList<V> val = new ArrayList<>(values());
		super.clear();
		val.forEach(x -> _removed(x));
	}

	protected void _removed(Object v) {

	}

}
