package field.graphics;

import fieldbox.execution.Errors;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interpose this class between a Scene.Perform and it's Scene to only execute a Scene.Perform is a Function is true. Use this, for example, to
 * implement View Frustum Culling or to skip the drawing of a Mesh that has no vertices (which is, in general, not a safe thing to do, because while a
 * mesh might have no verities, it might have some children which have other side-effects)
 */
public class Guard implements Scene.Perform {

	private final Supplier<Scene.Perform> p;
	private final Function<Integer, Boolean> guard;
	Errors.ErrorConsumer ec;

	public Guard(Scene.Perform p, Function<Integer, Boolean> guard) {
		this.p = () -> p;
		this.guard = guard;
		ec = Errors.errors.get();
	}

	@Override
	public void setErrorConsumer(Errors.ErrorConsumer c) {
		this.ec = c;
	}

	@Override
	public Errors.ErrorConsumer getErrorConsumer() {
		return ec;
	}

	public Guard(Supplier<Scene.Perform> p, Function<Integer, Boolean> guard) {
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
