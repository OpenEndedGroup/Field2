package field.graphics;

import field.utility.Dict;
import field.utility.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * Our main representation for an OpenGL graphics context.
 * <p>
 * The key complexity in doing graphics is that we wish to defer execution of drawing code to a particular time and place in the scene graph and that
 * this scene graph drawing can only take place inside an OpenGL graphics context. Additionally we want to be able to share pieces of the scene graph
 * between multiple OpenGL graphics context while scene graphs have all kinds of state (OpenGL names and configuration state back from OpenGL) that's
 * per Graphics Context. This class contains all of the utilities to help.
 * <p>
 * Let's take a concrete example. Say you want to delete a mesh, because you are done with it. what could mesh.delete() do? Well, it could remove mesh
 * from the scenegraph. But that's not enough. It also must conspire to deallocate any OpenGL resources that mesh allocated, and it must do that from
 * all OpenGL contexts that there has ever been --- OpenGL contexts that we don't necessarily control the execution of. The calls
 * postQueueInAllContexts help push code inside calls like delete into the running of OpenGL contexts.
 * <p>
 * Additionally there's code for the lazy one-time initialization of per-context state (get, put, computeIfAbset amd exists). Things that serve OpenGL
 * contexts (like Window) handle newContext() and enterContext() and exitContext().
 */
public class GraphicsContext {

	static protected GraphicsContext currentGraphicsContext;
	static List<GraphicsContext> allGraphicsContexts = new ArrayList<GraphicsContext>();

	protected WeakHashMap<Object, Object> context = new WeakHashMap<>();
	protected Dict storage = new Dict();

	static public GraphicsContext getContext() {
		return currentGraphicsContext;
	}

	static public boolean isResizing = false;

	public final BlockingQueue<Runnable> preQueue = new LinkedBlockingQueue<>();
	public final BlockingQueue<Runnable> postQueue = new LinkedBlockingQueue<>();

	static public GraphicsContext newContext() {
		GraphicsContext c = new GraphicsContext();
		allGraphicsContexts.add(c);
		currentGraphicsContext = c;
		return c;
	}

	static public void enterContext(GraphicsContext c) {
		Log.log("graphics.trace", ">> graphics context begin ");
		currentGraphicsContext = c;
		for (Runnable r : currentGraphicsContext.preQueue)
			r.run();
	}

	static public void exitContext(GraphicsContext c) {
		for (Runnable r : currentGraphicsContext.postQueue)
			r.run();
		currentGraphicsContext = null;
		Log.log("graphics.trace", "<< graphics context end");
	}

	static public <T> T get(Object o) {
		return (T) currentGraphicsContext.context.get(o);
	}

	static public <T> T get(Object o, Supplier<T> initializer) {
		T t = (T) currentGraphicsContext.context.get(o);
		if (t == null) currentGraphicsContext.context.put(o, t = initializer.get());
		return t;
	}

	static public <T> T get(Dict.Prop<T> o) {
		return get(currentGraphicsContext, o);
	}

	static public <T> void put(Dict.Prop<T> o, T v) {
		put(currentGraphicsContext, o, v);
	}

	static public <T> T computeIfAbsent(Dict.Prop<T> o, Supplier<T> initializer) {
		return computeIfAbsent(currentGraphicsContext, o, initializer);
	}

	static public <T> T get(GraphicsContext context, Dict.Prop<T> o) {
		return context.storage.get(o);
	}

	static public <T> T computeIfAbsent(GraphicsContext context, Dict.Prop<T> o, Supplier<T> initializer) {
		return context.storage.computeIfAbsent(o, (k) -> initializer.get());
	}

	static public <T> void put(GraphicsContext context, Dict.Prop<T> o, T v) {
		context.storage.put(o, v);
	}

	static public void invalidateInThisContext(Object o) {
		currentGraphicsContext.context.remove(o);
	}

	static public void invalidateInAllContexts(Object o) {
		for (GraphicsContext cc : GraphicsContext.allGraphicsContexts)
			cc.context.remove(o);
	}

	public boolean exists(Object o) {
		return context.containsKey(o);
	}

	static public <T> void put(Object o, T val) {
		currentGraphicsContext.context.put(o, val);
	}

	static public void postQueueInAllContexts(Runnable c) {
		allGraphicsContexts.stream().map(x -> x.postQueue.add(c));
	}

	static public void preQueueInAllContexts(Supplier<Boolean> c) {
		allGraphicsContexts.stream().map(x -> x.preQueue.add(new Sticky(x.preQueue, c)));
	}

	static public void preQueueInAllContexts(Runnable c) {
		allGraphicsContexts.stream().map(x -> x.preQueue.add(c));
	}

	public static <T> T remove(Object key) {
		return (T) currentGraphicsContext.context.remove(key);
	}

	static public class Sticky implements Runnable {
		private final Supplier<Boolean> target;
		private final Collection<Runnable> queue;

		public Sticky(Collection<Runnable> queue, Supplier<Boolean> target) {
			this.target = target;
			this.queue = queue;
		}

		public void run() {
			if (target.get()) queue.add(this);
		}
	}


}
