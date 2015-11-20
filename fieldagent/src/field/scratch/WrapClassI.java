package field.scratch;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by marc on 3/12/14.
 */
public class WrapClassI {

	private final Method m;

	public WrapClassI(Method m)
	{
		this.m = m;
	}

	public void begin(Object source, Object[] args) {
		System.out.println(" getting called with "+ Arrays.asList(args)+" from "+source);
	}

	public Integer end(Object source, Integer returning) {
		return returning +10;
	}
}
