package field.utility;

import com.google.common.collect.MapMaker;
import fieldnashorn.Watchdog;

import java.util.*;
import java.util.function.Function;

public class LinkedHashMapAndArrayList<V> extends LinkedHashMap<String, V> {

	protected int uniq = 0;

	Map<Object, String> keys = new MapMaker().weakKeys().makeMap();

	int resourceLimit = -1;
	String resourceMessage = null;

	public <M extends LinkedHashMapAndArrayList<V>> M configureResourceLimits(int max, String message) {
		resourceLimit = max;
		resourceMessage = message;
		return (M) this;
	}

	public void add(Object value) {
		while (containsKey("__internal_" + (++uniq))) ;
		keys.put(value, "__internal_" + uniq);
		_put("__internal_" + (++uniq), massage(value));
	}

	public void addAll(Collection<?> value) {
		for (Object o : value)
			add(o);
	}

	@Override
	public V put(String key, Object value) {
		return _put(key, massage(value, get(massageKey(key))));
	}

	// this one is better for writing Java, because it gives you type inference on lambdas as V
	public V _put(String key, V v) {
		V displaced = super.put(massageKey(key), v);
		if (displaced != null && displaced != v) {
			_removed(displaced, v);
		}

		if (resourceLimit != -1)
			Watchdog.Companion.limit(size(), resourceLimit, resourceMessage);

		return displaced;
	}

	protected V massage(Object vraw) {
		return (V) vraw;
	}

	protected V massage(Object vraw, Object previously) {
		return massage(vraw);
	}


	protected String massageKey(String k) {
		return k;
	}

	public V remove(Object v) {
		V q = super.remove(v);
		V q2 = super.remove(keys.remove(massageKey("" + v)));

		if (q != null)
			_removed(q);
		if (q2 != null)
			_removed(q2);

		return q != null ? q : q2;
	}

	public void removeIf(Function<V, Boolean> predicate) {
		Iterator<Map.Entry<String, V>> e = entrySet().iterator();
		while (e.hasNext()) {
			V v = e.next().getValue();
			if (v != null)
				if (predicate.apply(v)) {
					e.remove();
					_removed(v);
				}
		}
	}

	public void removeValue(V v) {
		removeIf(x -> x == v);
	}

	@Override
	public void clear() {
		ArrayList<V> val = new ArrayList<>(values());
		super.clear();
		val.forEach(x -> _removed(x));
	}

	protected void _removed(Object v) {

	}

	protected void _removed(Object v, Object inExchangeFor) {
		_removed(v);
	}

}
