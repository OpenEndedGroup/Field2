package field.graphics;


import field.utility.Util;
import fieldbox.execution.Errors;

import java.util.function.Supplier;

/**
 * this Base class codifies a general pattern for Scene.Perform classes:
 * <p>
 * 1. a setup() method called once and then 2. an upload() method called if and only if something has changed 3. a perform0() method called each
 * update cycle 4. optionally a perform1() method called at a later point in the update cycle
 * <p>
 * there's a type parameter <t_state extends BaseScene.Modifiable> that keeps track of that "something" in "something has changed". It's returned by
 * the setup method and passed into upload().
 */
public abstract class BaseScene<t_state extends BaseScene.Modifiable> extends Scene implements Scene.Perform {
	static public class Modifiable {
		int mod = 0;
	}

	Errors.ErrorConsumer ec;

	protected BaseScene() {
		// its generally important that things get initialized as early as possible (and, furthermore, not in some random spot in the Scene update)
		GraphicsContext.postQueueInAllContexts(() -> {
			try(Util.ExceptionlessAutoCloasable st = GraphicsContext.getContext().stateTracker.save()) {
				GraphicsContext.put(this, setup());
			}
		});


		ec = Errors.errors.get();
	}

	@Override
	public void setErrorConsumer(Errors.ErrorConsumer c) {
		this.ec = c;
	}

	@Override
	public Errors.ErrorConsumer getErrorConsumer() {
		return this.ec;
	}

	protected int mod = 0;

	@Override
	public boolean perform(int pass) {

		if (disabled.get()) return true;

		if (pass == getPasses()[0]) {
			t_state s = GraphicsContext.get(this, () -> setup());

			if (s.mod != mod) s.mod = upload(s);

			update(pass, this::perform0);
		}

		if (getPasses().length > 1) if (pass == getPasses()[1]) this.perform1();

		return true;
	}

	protected int upload(t_state s) {
		return mod;
	}

	protected abstract boolean perform0();

	protected boolean perform1() {
		return true;
	}

	/**
	 * it is the caller's responsibility to ensure that this is called per context. To do so, you can wrap all calls to this in
	 * GraphicsContext.preQueueInAllContexts(() -> ... and GraphicsContext.get(this, () -> ... )
	 */
	protected abstract t_state setup();

	public void finalize() {
		GraphicsContext.postQueueInAllContexts(this::destroy);
	}

	protected void destroy() {
		t_state s = GraphicsContext.remove(this);
		if (s == null) return;
		deallocate(s);
	}

	protected abstract void deallocate(t_state s);

	public Supplier<Boolean> disabled = () -> false;

	public void disable()
	{
		disabled = () -> true;
	}

	public void enable()
	{
		disabled = () -> false;
	}
	public void disable(Supplier<Boolean> when)
	{
		disabled = when;
	}

}
