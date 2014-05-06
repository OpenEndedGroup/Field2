package field.scratch;

import fieldagent.transformations.Wrap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by marc on 3/13/14.
 */
public class Canceller {
	private final Method m;

	public Canceller(Method m)
	{
		this.m = m;
	}

	public void begin(Object source, Object[] args) {
//		System.out.println(" methods are :" + Arrays.asList(source.getClass().getMethods()));
//		System.out.println("    "+m);

		System.out.println(" about to call it for the hell of it ");
		try {
			m.invoke(source, args);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		throw new Wrap.Cancel("peach");
	}

	public Object end(Object source, Object returning) {
		if (returning instanceof String)
			return returning+"--I was here from "+source;
		return returning;
	}

}
