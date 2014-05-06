package field.scratch;

/**
 * Created by marc on 3/13/14.
 */
public class WovenChecker {

	static public void main(String[] a)
	{
		CheckWoven w = new CheckWoven();
		try{
			System.out.println("  a");
			w.checkWrap(0, "b", new long[]{1,2,3}, new String[]{"s1", "s2"});
			System.out.println("\n\n\n0: "+w.checkWrapReturnsString(0, "b", new long[]{1,2,3}, new String[]{"s1", "s2"}));
			System.out.println("\n\n\n1: "+w.checkCancel(0, "b", new long[]{1,2,3}, new String[]{"s1", "s2"}));
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		try{
			System.out.println("\n\n\n2: "+w.checkCancel2(0, "b", new long[]{1,2,3}, new String[]{"s1", "s2"}));
		}
		catch(Throwable e)
		{
			e.printStackTrace();
		}
		try{
			System.out.println("\n\n\n3: "+w.checkWrapReturnsString2(0, "b", new long[]{1, 2, 3}, new String[]{"s1", "s2"}));
		}
		catch(Throwable e)
		{
			e.printStackTrace();
		}
		try{
			System.out.println("\n\n\n4: "+w.checkWrapReturnsString3(0, "b", new long[]{1, 2, 3}, new String[]{"s1", "s2"}));
		}
		catch(Throwable e)
		{
			e.printStackTrace();
		}
		try{
			System.out.println("\n\n\n5: "+w.checkWrapReturnsString4());
		}
		catch(Throwable e)
		{
			e.printStackTrace();
		}


	}

}
