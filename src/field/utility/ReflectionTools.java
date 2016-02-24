package field.utility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionTools {


	static public Object get(Object from, String path) throws NoSuchFieldException, IllegalAccessException {
		String[] pieces = path.split("/");
		for (int i = 0; i < pieces.length; i++) {
			from = _get(from, pieces[i]);
		}
		return from;
	}

	static public Method getMethod(Object from, String path) throws NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
		String[] pieces = path.split("/");
		for (int i = 0; i < pieces.length; i++) {
			if (i < pieces.length - 1) from = _get(from, pieces[i]);
			else return getMethod(from.getClass(), pieces[pieces.length - 1]);
		}
		return null;
	}


	public static Method getMethod(Class c, String name) throws NoSuchMethodException {

//		System.out.println(" check :" + c + " for " + name+" ? ");

		try {
			Method f = c.getDeclaredMethod(name);
			if (f != null) {
				f.setAccessible(true);
				return f;
			}
		} catch (NoSuchMethodException e) {
		}

		System.out.println(" ? ");
		Method[] m = c.getDeclaredMethods();
		for (Method mm : m) {
//			System.out.println(" check :" + c + " / "+mm+" for " + name);
			if (mm.getName()
			      .equals(name)) {
				mm.setAccessible(true);
				return mm;
			}
		}

		try {
			Method f = c.getMethod(name);
			if (f != null) {
				f.setAccessible(true);
				return f;
			}
		} catch (NoSuchMethodException e) {
		}
		Class[] inter = c.getInterfaces();
		for (Class cc : inter) {
			try {
				Method o = getMethod(cc, name);
				if (o != null) return o;
			} catch (NoSuchMethodException e) {
			}
		}
		c = c.getSuperclass();
		if (c != null) return getMethod(c, name);

		throw new NoSuchMethodException(" can't find " + name + " tried everywhere");
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
				Object o = __get(from, cc, name);
				if (o != null) return o;
			} catch (NoSuchFieldException e) {
			}
		}
		c = c.getSuperclass();
		if (c != null) return __get(from, c, name);

		throw new NoSuchFieldException(" can't find " + name + " tried everywhere");
	}
}
