package fieldbox.boxes.plugins;

import field.app.ThreadSync;
import field.utility.Dict;
import fieldbox.boxes.Box;

import static field.app.ThreadSync.yield;

/**
 * Created by marc on 5/24/16.
 */
public class Threading extends Box {

	public interface PolyFunction {
		Object apply(Object... a);
	}

	static public final Dict.Prop<PolyFunction> yield = new Dict.Prop<>("yield").toCannon().doc("`_.yield()` waits for one whole animation cycle before continuing on from here");

	public Threading() {
		this.properties.put(yield, (a) -> {
			try {
				return ThreadSync.yield(a);
			} catch (InterruptedException e) {
				return null;
			}
		});
	}
}
