package fieldbox.boxes.plugins;

import field.app.ThreadSync;
import field.app.ThreadSync2;
import field.utility.Dict;
import fieldbox.boxes.Box;
import jdk.nashorn.api.scripting.AbstractJSObject;

import java.util.List;

/**
 * Created by marc on 5/24/16.
 */
public class Threading extends Box {

	static public final Dict.Prop<AbstractJSObject> wait = new Dict.Prop<>("wait").toCanon().doc("`_.wait()` waits for one whole animation cycle before continuing on from here");
	static public final Dict.Prop<FunctionOfBoxValued<Threads>> threads = new Dict.Prop<>("threads").toCanon().doc("`_.threads` returns a thread control object that provides a high-level interface to the fibre threading of a box");

	// high-level information and control over the threads in a box
	static public final class Threads {
		public final boolean enabled = ThreadSync2.getEnabled();

		public final long numRunning;
		private final Box on;

		protected Threads(Box on) {
			this.on = on;
			List<ThreadSync2.Fibre> q = ThreadSync2Feedback.fibresFor(on);
			numRunning = q.stream().filter(x -> !x.finished).count();
		}

		public void kill() {
			ThreadSync2Feedback.kill(on);
		}

		public void pause() {
			ThreadSync2Feedback.pause(on);
		}

		public void step() {
			ThreadSync2Feedback.step(on);
		}

		public void play() {
			ThreadSync2Feedback.cont(on);
		}
	}


	public Threading() {

		this.properties.put(threads, (x) -> new Threads(x));
		this.properties.put(wait, new AbstractJSObject() {
			@Override
			public Object call(Object o, Object... objects) {

				return waitSafely();
			}
		});
	}

	static public boolean waitSafely() {
		if (ThreadSync2.fibre().shouldEnd)
		{
			if (ThreadSync2.fibre().endingFor++>0)
			{
				ThreadSync2.fibre().killed = true;
				ThreadSync2.yield(); // will kill this
			}
		}
		else
			ThreadSync2.yield();
		return !ThreadSync2.fibre().shouldEnd;
	}
}
