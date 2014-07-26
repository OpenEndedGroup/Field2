package field.utility;

import com.google.common.collect.MapMaker;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class LinkedHashMapAndArrayList<V> extends LinkedHashMap<String, V> {

	protected int uniq = 0;

	Map<Object, String> keys = new MapMaker().weakKeys().makeMap();

	public void add(Object vraw)
	{
		while(containsKey("__internal_"+(++uniq)));
		keys.put(vraw, "__internal_"+uniq);
		super.put("__internal_"+(++uniq), massage(vraw));
	}

	@Override
	public V put(String key, Object value) {
		return super.put(key, massage(value));
	}

	protected V massage(Object vraw) {
		return (V)vraw;
	}

	public V remove(Object v)
	{
		Log.log("lhmaal_remove", "removing "+v+" "+this);
		V q = super.remove(v);
		V q2 = super.remove(keys.remove(v));
		Log.log("lhmaal_remove", "now "+this);
		return q!=null ? q : q2;
	}

}
