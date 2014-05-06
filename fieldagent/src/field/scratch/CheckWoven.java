package field.scratch;

import fieldagent.annotations.Woven;
import fieldagent.annotations.Wrap;

@Woven
public class CheckWoven {

	@Wrap(WrapClass.class)
	public void checkWrap(int a, String b, long[] c, String[] d) {

	}

	@Wrap(WrapClass.class)
	public String checkWrapReturnsString(int a, String b, long[] c, String[] d) {
		return "hello there";
	}

	@Wrap(Canceller.class)
	public String checkCancel(int a, String b, long[] c, String[] d) {
		System.out.println(" i'm running and I'm not supposed to <" + a + " " + b + " " + c + " " + d + ">");
		return "hello there";
	}

	@Wrap(Canceller.class)
	public String checkCancel2(int a, String b, long[] c, String[] d) throws Exception {
		System.out.println("inside checkCancel2 <" + a + " " + b + " " + c + " " + d + ">");
		if (Math.random() < 0.5) throw new Exception(" valid exception ");
		return "";
	}

	@Wrap(WrapClass.class)
	public String checkWrapReturnsString2(int a, String b, long[] c, String[] d) throws Exception {
		int x = 10;
		x *= 20;
		if (Math.random() < 0.5 * x) throw new Exception(" valid exception ");
		throw new Exception(" valid exception ");
	}

	@Wrap(WrapClass.class)
	public String checkWrapReturnsString3(int a, String b, long[] c, String[] d) throws Exception {
		int x = 10;
		x *= 20;
		for (int i = 0; i < 10; i++) {
			if (Math.random() < 0.2) return "hello there 0";
			if (Math.random() < 0.2 && System.currentTimeMillis() % x == 0) {
				if (Math.random() < 0.2) return "hello there 2";
			}
		}
		if (Math.random() < 0.2) {
			throw new Exception(" hi, I'm an exception, not a return ");
		}
		if (Math.random() < 0.2) return "something else";
		return "done";
	}

	@Wrap(CancellerI.class)
	public int checkWrapReturnsString4() throws Exception {
		if (Math.random() < 0.2) return 0;
		if (Math.random() < 0.2) return 0;
		if (Math.random() < 0.2) throw new Exception(" valid exception ");
		if (Math.random() < 0.2) return 0;
		if (Math.random() < 0.2) return -1;
		return -2;
	}

}
