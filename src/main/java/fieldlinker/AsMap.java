package fieldlinker;
import java.util.Collections;

public interface AsMap extends CustomDelete {
	boolean asMap_isProperty(String p);

	Object asMap_call(Object a, Object b);

	Object asMap_get(String p);

	Object asMap_set(String p, Object o);

	Object asMap_new(Object a);

	Object asMap_new(Object a, Object b);

	Object asMap_getElement(int element);

	default Object asMap_getElement(Object element) {
		throw new Error();
	}

	Object asMap_setElement(int element, Object o);

	default Object asMap_setElement(Object element, Object o) {
		return asMap_set("" + element, o);
	}

	default Object asMap_call(Object o) {
		return asMap_call(o, Collections.EMPTY_MAP);
	}
}
