package field.graphics;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by marc on 3/23/14.
 */
public class Guard implements Scene.Perform {

	private final Supplier<Scene.Perform> p;
	private final Function<Integer, Boolean> guard;

	public Guard(Scene.Perform p, Function<Integer, Boolean> guard)
	{
		this.p = () -> p;
		this.guard = guard;
	}

	public Guard(Supplier<Scene.Perform> p, Function<Integer, Boolean> guard)
	{
		this.p = p;
		this.guard = guard;
	}

	@Override
	public boolean perform(int pass) {
		if (guard.apply(pass)) return p.get().perform(pass);
		else return true;
	}

	@Override
	public int[] getPasses() {
		return p.get().getPasses();
	}
}
