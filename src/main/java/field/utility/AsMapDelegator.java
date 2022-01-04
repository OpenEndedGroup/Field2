package field.utility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base class for objects that delegate to another place (usually a Dict) for their property storage
 */
public abstract class AsMapDelegator implements fieldlinker.AsMap {

	private Set<String> knownNonProperties;

	@Override
	public boolean asMap_isProperty(String p) {

		System.out.println(" is property ? "+p+" "+knownNonProperties+" "+(knownNonProperties==null? false : knownNonProperties.contains(p)));
		System.out.println("  "+delegateTo());
		if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();

		if (knownNonProperties.contains(p)) return false;

		return delegateTo().asMap_isProperty(p);
	}

	@Override
	public boolean asMap_delete(Object o) {
		if (o==null) return false;

		String p = o.toString();
		if (knownNonProperties.contains(p)) return false;

		return delegateTo().asMap_delete(o);
	}

	protected Set<String> computeKnownNonProperties(Class clazz) {
		Set<String> r = new LinkedHashSet<>();
		Method[] m = clazz.getMethods();
		for (Method mm : m)
			r.add(mm.getName());
		Field[] f = clazz.getFields();
		for (Field ff : f)
			r.add(ff.getName());

		for (Class c : clazz.getInterfaces())
			r.addAll(computeKnownNonProperties(c));
		if (clazz.getSuperclass() != null) r.addAll(computeKnownNonProperties(clazz.getSuperclass()));
		return r;

	}

	protected Set<String> computeKnownNonProperties() {
		return computeKnownNonProperties(this.getClass());
	}

	@Override
	public Object asMap_call(Object a, Object b) {
		return delegateTo().asMap_call(a, b);
	}

	@Override
	public Object asMap_get(String p) {
		return delegateTo().asMap_get(p);
	}

	@Override
	public Object asMap_set(String p, Object o) {
		return delegateTo().asMap_set(p, o);
	}

	@Override
	public Object asMap_new(Object a) {
		return delegateTo().asMap_new(a);
	}

	@Override
	public Object asMap_new(Object a, Object b) {
		return delegateTo().asMap_new(a, b);
	}

	@Override
	public Object asMap_getElement(int element) {
		return delegateTo().asMap_getElement(element);
	}

	@Override
	public Object asMap_setElement(int element, Object o) {
		return delegateTo().asMap_setElement(element, o);
	}

	protected abstract fieldlinker.AsMap delegateTo();


}
