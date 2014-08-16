package fieldlinker;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by marc on 8/13/14.
 */
public class Testy implements Linker.AsMap {

	@Override
	public Object asMap_call(Object a, Object b) {
		System.err.println(" call called :"+a+" "+b);
		return this;
	}

	public int something()
	{
		System.err.println(" something called ");
		return 1;
	}


	public int somethingElse(String el)
	{
		System.err.println(" seomthing else "+el+" called ");
		return 10;
	}



	@Override
	public boolean asMap_isProperty(String p) {

		return true;
	}

	@Override
	public Object asMap_get(String p) {
		System.out.println(" get arguments :"+p);
		return p+"--ret"+this;
	}

	@Override
	public Object asMap_set(String p, Object o) {
		System.out.println(" set calls <"+p+" "+o+">");
		return p+this;
	}

	@Override
	public Object asMap_new(Object a) {
		return null;
	}
	@Override
	public Object asMap_new(Object a, Object b) {
		return null;
	}
}
