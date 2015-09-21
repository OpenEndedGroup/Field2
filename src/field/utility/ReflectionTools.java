package field.utility;

import java.lang.reflect.Field;

public class ReflectionTools {


	static public Object get(Object from, String path) throws NoSuchFieldException, IllegalAccessException {
		String[] pieces = path.split("/");
		for (int i = 0; i < pieces.length; i++) {
			from = _get(from, pieces[i]);
		}
		return from;
	}

	static protected Object _get(Object from, String name) throws IllegalAccessException, NoSuchFieldException {
		if (from == null) return null;
		return __get(from, from.getClass(), name);
	}

	private static Object __get(Object from, Class c, String name) throws IllegalAccessException, NoSuchFieldException {
		try {
			Field f = c.getDeclaredField(name);
			if (f != null) {
				f.setAccessible(true);
				return f.get(from);
			}
		} catch (NoSuchFieldException e) {
		}
		try {
			Field f = c.getField(name);
			if (f != null) {
				f.setAccessible(true);
				return f.get(from);
			}
		} catch (NoSuchFieldException e) {
		}
		Class[] inter = c.getInterfaces();
		for (Class cc : inter) {
			try {
				Object o = __get(from, c, name);
				if (o != null) return o;
			} catch (NoSuchFieldException e) {
			}
		}
		c = c.getSuperclass();
		if (c != null) return __get(from, c, name);

		throw new NoSuchFieldException(" can't find "+name+" tried everywhere");
	}
}
