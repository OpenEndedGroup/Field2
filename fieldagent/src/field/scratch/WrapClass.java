package field.scratch;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by marc on 3/12/14.
 */
public class WrapClass {

	private final Method m;

	public WrapClass(Method m)
	{
		this.m = m;
	}

	public void begin(Object source, Object[] args) {
		System.out.println(" getting called with "+ Arrays.asList(args)+" from "+source);
	}

	public Object end(Object source, Object returning) {
		return returning;
//		return returning+"--I was here from "+source;
	}
}
