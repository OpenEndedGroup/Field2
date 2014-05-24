package field.graphics;

import field.utility.Pair;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * The principle class of the Field Graphics "Scene List" structure.
 * <p>
 * A Scene is a list of Perform instances that can run at various passes of their choosing. Passes are ordered numerically and created on demand. By
 * convention geometry is drawn on pass 0.
 * <p>
 * For example a texture Perform might set OpenGL texture state at a pass "-1" and restore it at pass "1", thus it can be added as a sibling of a
 * Geometry object or as a Parent or as a Child and still run in the right order with respect to that piece of Geometry. The topology of the tree is
 * free to encoding a grouping thats useful to the application rather than something that's vital to reexpressing the semantics of OpenGL's decaying
 * state-machine just right.
 */
public class Scene {

	/**
	 * Although we can build the Scene structure out of (int, Consumer<int>) tuples, it's more along the grain of Java's type system to use an interface with both
	 */
	static public interface Perform extends Consumer<Integer> {
		public boolean perform(int pass);

		default public int[] getPasses() {
			return new int[]{0};
		}

		default public void accept(Integer p) {
			perform(p);
		}
	}

	static public class Cancel extends RuntimeException {
	}

	static public class Transient implements Perform {
		int[] passes;
		Consumer<Integer> call;
		LinkedHashSet<GraphicsContext> allContexts = new LinkedHashSet<>(GraphicsContext.allGraphicsContexts);

		public Transient(Perform p) {
			this.passes = p.getPasses();
			this.call = (x) -> p.perform(x);
		}

		public Transient(Runnable p, int... passes) {
			this.passes = passes;
			this.call = (x) -> p.run();
		}

		public Transient(Consumer<Integer> m, int... passes) {
			this.passes = passes;
			this.call = (x) -> m.accept(x);
		}

		@Override
		public boolean perform(int pass) {
			if (allContexts.remove(GraphicsContext.getContext())) call.accept(pass);
			return allContexts.size() > 0;
		}

		@Override
		public int[] getPasses() {
			return passes;
		}
	}

	TreeMap<Integer, Set<Consumer<Integer>>> scene = new TreeMap<>();

	/**
	 * A (int pass, Consumer<Integer>) tuple can be effectively cast as a Perform. The int says what pass the consumer should be run for, and the
	 * consumer is called for that pass
	 */
	public boolean connect(int pass, Consumer<Integer> p) {
		Set<Consumer<Integer>> c = scene.get(pass);
		if (c == null) scene.put(pass, c = new LinkedHashSet<Consumer<Integer>>());
		return c.add(p);
	}

	/**
	 * Disconnects a Perform from this Scene. Care has been taken to ensure you can do this while the scene is being traversed.
	 */
	public void disconnect(Perform p) {
		scene.values().stream().map(x -> x.remove(p));
	}

	/**
	 * connects a Perform to this Scene. Care has been taken to ensure you can do this while the scene is being traversed.
	 */
	public boolean connect(Perform p) {
		boolean m = false;
		for (int i : p.getPasses())
			m |= connect(i, p);
		return m;
	}


	/**
	 * returns the current scene as a Map<Integer, Set<Consumer<Integer>>>
	 */
	public Map<Integer, Set<Consumer<Integer>>> getScene() {
		return Collections.unmodifiableMap(scene);
	}

	/**
	 * resets the scene to a particular Map<Integer, Set<Consumer<Integer>>>
	 * @param scene
	 */
	public void setScene(Map<Integer, Set<Consumer<Integer>>> scene) {
		this.scene.clear();
		this.scene.putAll(scene);
	}


	/**
	 * updates everything in the scene. This is the main entry point for performing a complete update cycle.
	 */
	public void updateAll() {
		update(new ArrayDeque<Pair<Integer, Callable<Boolean>>>());
	}

	List<Throwable> exceptions = new ArrayList<Throwable>();

	protected boolean update(int midpoint, Callable<Boolean> middle) {
		return update(new ArrayDeque<>(Arrays.asList(new Pair<Integer, Callable<Boolean>>(midpoint, middle))));
	}

	protected boolean update(int apoint, Callable<Boolean> a, int bpoint, Callable<Boolean> b) {
		return update(new ArrayDeque<>(Arrays
			    .asList(new Pair<Integer, Callable<Boolean>>(apoint, a), new Pair<Integer, Callable<Boolean>>(bpoint, b))));
	}

	protected boolean update(Queue<Pair<Integer, Callable<Boolean>>> a) {
		exceptions.clear();

		int first = 0;
		boolean ret = true;
		for (Integer i : new LinkedHashSet<>(scene.keySet())) {
			if (GraphicsContext.trace) System.out.println(this + " pass " + i + " -> " + scene.get(i));
			while (!a.isEmpty() && i >= a.peek().first) ret = wrappedCall(a.poll().second);

			ArrayList<Consumer<Integer>> previously = new ArrayList<>(scene.get(i));

			Iterator<Consumer<Integer>> ic = previously.iterator();
			while (ic.hasNext()) {
				Consumer<Integer> n = ic.next();
				if (!wrappedCall(n, i)) scene.get(i).remove(n);
			}

		}

		while (!a.isEmpty()) ret = wrappedCall(a.poll().second);

		if (exceptions.size() > 0) {
			System.err.println(" Exceptions thrown in scene update ");
			System.err.println(" (Performs responsible have been removed from the scene) ");
			System.err.println(" Details: ");
			for (Throwable t : exceptions) {
				t.printStackTrace();
			}
		}

		return ret;
	}


	public List<Throwable> getException() {
		return exceptions;
	}

	private boolean wrappedCall(Consumer<Integer> c, Integer i) {
		if (c == null) return false;
		try {
			if (c instanceof Perform) return ((Perform) c).perform(i);
			c.accept(i);
			return true;
		} catch (Throwable t) {
			if (!(t instanceof Cancel)) exceptions.add(t);
			return false;
		}
	}

	private boolean wrappedCall(Callable<Boolean> middle) {
		if (middle == null) return false;
		try {
			return middle.call();
		} catch (Throwable t) {
			if (!(t instanceof Cancel)) exceptions.add(t);
			return false;
		}
	}

	/**
	 * returns a toString for everything in the scene.
	 */

	public String debugPrintScene() {
		String ret = "" + this + "\n";
		String prefix = "   ";
		for (Map.Entry<Integer, Set<Consumer<Integer>>> c : scene.entrySet()) {
			ret = ret + prefix + "" + c.getKey() + ":\n";
			prefix = prefix + " ";
			for (Consumer<Integer> cc : c.getValue())
				ret = ret + "\n" + debugPrintScene(cc, prefix);
		}
		return ret;
	}

	private String debugPrintScene(Consumer<Integer> cc, String prefix) {
		String ret = prefix + "" + cc + "\n";
		prefix += "   ";
		if (cc instanceof Scene) {
			for (Map.Entry<Integer, Set<Consumer<Integer>>> c : ((Scene) cc).scene.entrySet()) {
				ret = ret + prefix + "" + c.getKey() + ":\n";
				prefix = prefix + " ";
				for (Consumer<Integer> ccc : c.getValue())
					ret = ret + "\n" + debugPrintScene(ccc, prefix);
			}

		}
		return ret;
	}


	/**
	 * utility, takes a consumer and returns a version that runs only once every "count" iterations
	 */

	static public <T> Consumer<T> strobe(Consumer<T> c, int count) {
		return new Consumer<T>() {
			int tick = 0;

			@Override
			public void accept(T t) {
				if (tick++ % count == 0) c.accept(t);
			}
		};
	}

}
