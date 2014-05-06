package field.graphics;


public abstract class BaseScene<t_state extends BaseScene.Modifiable> extends Scene implements Scene.Perform {
	static public class Modifiable {
		int mod = 0;
	}

	protected BaseScene()
	{
		// its generally important that things get initialized as early as possible (and, furthermore, not in some random spot in the Scene update)
		GraphicsContext.preQueueInAllContexts(() -> GraphicsContext.put(this, setup()));
	}

	protected int mod = 0;

	@Override
	public boolean perform(int pass) {

		if (pass == getPasses()[0]) {
			t_state s = GraphicsContext.get(this, () -> setup());

			if (s.mod != mod) s.mod = upload(s);

			update(pass, this::perform0);
		}

		if (getPasses().length > 1)
			if (pass == getPasses()[1]) this.perform1();

		return true;
	}

	protected int upload(t_state s)
	{
		return mod;
	}

	protected abstract boolean perform0();

	protected boolean perform1() {
		return true;
	}

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

}
