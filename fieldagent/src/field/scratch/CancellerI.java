package field.scratch;

import java.lang.reflect.Method;

public class CancellerI {
	private final Method m;

	public CancellerI(Method m)
	{
		this.m = m;
	}

//	public void begin(Object source, Object[] args) {
//		System.out.println(" methods are :" + Arrays.asList(source.getClass().getMethods()));
//		System.out.println("    "+m);
//
//		System.out.println(" about to call it for the hell of it ");
//		try {
//			m.invoke(source, args);
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
//
//		throw new Wrap.Cancel(10);
//	}

	public Object end(Object source, Object returning) {
		if (returning instanceof Number)
			return 12;
		return returning;
	}

}
