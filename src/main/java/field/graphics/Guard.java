package field.graphics;

import fieldbox.execution.InverseDebugMapping;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interpose this class between a Scene.Perform and it's Scene to only execute a Scene.Perform is a Function is true. Use this, for example, to
 * implement View Frustum Culling or to skip the drawing of a Mesh that has no vertices (which is, in general, not a safe thing to do, because while a
 * mesh might have no vertices, it might have some children which have other side-effects)
 */
public class Guard implements Scene.Perform {

	private final Supplier<Scene.Perform> p;
	private final Function<Integer, Boolean> guard;

	public Guard(Scene.Perform p, Function<Integer, Boolean> guard) {
		this.p = () -> p;
		this.guard = guard;
	}


	public Guard(Supplier<Scene.Perform> p, Function<Integer, Boolean> guard) {
		this.p = p;
		this.guard = guard;
	}

	Scene.Perform last;
	@Override
	public boolean perform(int pass) {
		if (guard.apply(pass)) return (last=p.get()).perform(pass);
		else return true;
	}

	@Override
	public int[] getPasses() {
		return p.get().getPasses();
	}

	@Override
	public String toString() {
		return "Guard("+InverseDebugMapping.describeWithToString(last)+" | "+ InverseDebugMapping.describeWithToString(guard)+")="+InverseDebugMapping.describe(this);
	}
}
