package field.graphics;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import field.utility.*;
import fieldbox.boxes.Box;
import fieldbox.execution.Completion;
import fieldlinker.Linker;
import fieldnashorn.annotations.HiddenInAutocomplete;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The principle class of the Field Graphics "Scene List" structure.
 * <p>
 * A Scene is a list of Perform instances that can run at various passes of their choosing. Passes are ordered numerically and created on demand. By convention geometry is drawn on pass 0.
 * <p>
 * For example a texture Perform might set OpenGL texture state at a pass "-1" and restore it at pass "1", thus it can be added as a sibling of a Geometry object or as a Parent or as a Child and still
 * run in the right order with respect to that piece of Geometry. The topology of the tree is free to encoding a grouping thats useful to the application rather than something that's vital to
 * reexpressing the semantics of OpenGL's decaying state-machine just right.
 */
public class Scene extends Box implements Linker.AsMap {

	static public Dict.Prop<LinkedHashMapAndArrayList<Perform>> passes = new Dict.Prop<LinkedHashMapAndArrayList<Perform>>("passes").toCannon();
	protected Set<String> knownNonProperties;
	TreeMap<Integer, Set<Consumer<Integer>>> scene = new TreeMap<>();
	BiMap<String, Consumer<Integer>> tagged = HashBiMap.create();
	List<Throwable> exceptions = new ArrayList<Throwable>();

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

	/**
	 * A (int pass, Consumer<Integer>) tuple can be effectively cast as a Perform. The int says what pass the consumer should be run for, and the consumer is called for that pass
	 */
	public boolean attach(int pass, Consumer<Integer> p) {
		Set<Consumer<Integer>> c = scene.get(pass);
		if (c == null) scene.put(pass, c = new LinkedHashSet<Consumer<Integer>>());
		return c.add(p);
	}

	/**
	 * A (int pass, String tag, Consumer<Integer>) tuple can be effectively cast as a Perform. The int says what pass the consumer should be run for, and the consumer is called for that pass.
	 * <p>
	 * The additional "tag" gives this consumer a name, so that it can be referred to later. Note that at any given time there can only be one Perform or Consumer with any particular tag
	 * associated with a Scene, anything previously tagged with this scene is automatically disconnected. This allows you write code in "idempotent" style: specifically you can have connect(...)
	 * calls that can be safely (although not particularlly efficiently) called over and over again. This is good for rapidly iterating on a problem without having to worry about tearing down
	 * resources that you constructed.
	 */
	public boolean attach(int pass, String tag, Consumer<Integer> p) {
		Consumer<Integer> was = tagged.remove(tag);
		if (was != null) detach(was);

		Set<Consumer<Integer>> c = scene.computeIfAbsent(pass, k -> new LinkedHashSet<>());

		tagged.put(tag, p);

		return c.add(p);
	}

	/**
	 * The additional "tag" gives this consumer a name, so that it can be referred to later. Note that at any given time there can only be one Perform or Consumer with any particular tag
	 * associated with a Scene, anything previously tagged with this scene is automatically disconnected. This allows you write code in "idempotent" style: specifically you can have connect(...)
	 * calls that can be safely (although not particularlly efficiently) called over and over again. This is good for rapidly iterating on a problem without having to worry about tearing down
	 * resources that you constructed.
	 */
	public boolean attach(String tag, Perform p) {
		Consumer<Integer> was = tagged.remove(tag);
		if (was != null) detach(was);

		boolean pp = false;
		for (int pass : p.getPasses()) {
			Set<Consumer<Integer>> c = scene.computeIfAbsent(pass, k -> new LinkedHashSet<>());
			pp |= c.add(p);
		}

		tagged.put(tag, p);

		return pp;
	}

	/**
	 * Disconnects a Perform from this Scene. Care has been taken to ensure you can do this while the scene is being traversed.
	 */
	public void detach(Consumer<Integer> p) {

		scene.values()
		     .stream()
		     .map(x -> x.remove(p))
		     .count();

	}

	/**
	 * Disconnects a Perform from this Scene. Care has been taken to ensure you can do this while the scene is being traversed.
	 * <p>
	 * returns true if there was something tagged "tag";
	 */
	public boolean detach(String tag) {
		Consumer<Integer> was = tagged.remove(tag);
		if (was != null) detach(was);
		return was != null;
	}

	/**
	 * connects a Perform to this Scene. Care has been taken to ensure you can do this while the scene is being traversed.
	 */
	public boolean attach(Perform p) {
		boolean m = false;
		for (int i : p.getPasses())
			m |= attach(i, p);
		return m;
	}

	/**
	 * connects a Box to this Scene. Boxes can contain Performs, this operation is equivalent to connecting all the Performs in this box.
	 */
	@Override
	public Box connect(Box b) {
		Box p = super.connect(b);
		return p;
	}


	protected TreeMap<Integer, Set<Consumer<Integer>>> collectChildrenPasses() {


		if (this.children()
			.size() == 0) return null;

		TreeMap<Integer, Set<Consumer<Integer>>> t = new TreeMap<>();

		for (Box c : children()) {
			LinkedHashMapAndArrayList<Perform> p = c.properties.get(passes);
			if (p == null) continue;
			for (Perform pp : p.values()) {
				int[] ap = pp.getPasses();
				for (int x : ap)
					t.computeIfAbsent(x, k -> new LinkedHashSet<>())
					 .add(pp);
			}
		}

		return t;
	}


	/**
	 * updates everything in the scene. This is the main entry point for performing a complete update cycle.
	 */
	public void updateAll() {
		update(new ArrayDeque<Pair<Integer, Callable<Boolean>>>());
	}

	protected boolean update(int midpoint, Callable<Boolean> middle) {
		return update(new ArrayDeque<>(Arrays.asList(new Pair<Integer, Callable<Boolean>>(midpoint, middle))));
	}

	protected boolean update(int apoint, Callable<Boolean> a, int bpoint, Callable<Boolean> b) {
		return update(new ArrayDeque<>(Arrays.asList(new Pair<Integer, Callable<Boolean>>(apoint, a), new Pair<Integer, Callable<Boolean>>(bpoint, b))));
	}

	protected boolean update(Queue<Pair<Integer, Callable<Boolean>>> a) {
		exceptions.clear();

		boolean ret = true;

		try {

			TreeMap<Integer, Set<Consumer<Integer>>> c1 = collectChildrenPasses();
			if (c1 == null) c1 = new TreeMap<>();

			for (Map.Entry<Integer, Set<Consumer<Integer>>> c2 : scene.entrySet()) {
				if (c1.get(c2.getKey()) == null) c1.put(c2.getKey(), c2.getValue());
				else c1.get(c2.getKey())
				       .addAll(c2.getValue());
			}

			TreeMap<Integer, Set<Consumer<Integer>>> scene = c1;

			for (Integer i : new LinkedHashSet<>(scene.keySet())) {
				Log.log("graphics.trace", () -> this + " pass " + i + " -> " + scene.get(i));
				while (!a.isEmpty() && i >= a.peek().first) ret = wrappedCall(a.poll().second);

				ArrayList<Consumer<Integer>> previously = new ArrayList<>(scene.get(i));

				Iterator<Consumer<Integer>> ic = previously.iterator();
				while (ic.hasNext()) {
					Consumer<Integer> n = ic.next();
					if (!wrappedCall(n, i)) {
						detach(n);
					}
				}

			}

			while (!a.isEmpty()) ret = wrappedCall(a.poll().second);

			if (exceptions.size() > 0) {
				System.err.println(" Exceptions thrown in scene update ");
				System.err.println(" (Performs responsible have been removed from the scene, if they were directly attached) ");
				System.err.println(" Details: ");
				for (Throwable t : exceptions) {
					t.printStackTrace();
				}
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.exit(0);
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
		} finally {
		}
	}

	private boolean wrappedCall(Callable<Boolean> middle) {
		if (middle == null) return false;
		try {
			return middle.call();
		} catch (Throwable t) {
			if (!(t instanceof Cancel)) exceptions.add(t);
			return false;
		} finally {
		}
	}

	/**
	 * returns a toString for everything in the scene.
	 */

	public String debugPrintScene() {
		String ret = "" + this + "\n";
		String prefix = "   ";
		for (Map.Entry<Integer, Set<Consumer<Integer>>> c : scene.entrySet()) {
			String tag = tagged.inverse()
					   .get(c);
			ret = ret + prefix + "" + c.getKey() + ":" + (tag == null ? "" : tag) + "\n";
			prefix = prefix + "   ";
			for (Consumer<Integer> cc : c.getValue())
				ret = ret + "\n" + debugPrintScene(cc, prefix);
			prefix = prefix.substring(3);
		}
		return ret;
	}

	private String debugPrintScene(Consumer<Integer> cc, String prefix) {
		String ret = prefix + "" + cc + "\n";
		prefix += "   ";
		if (cc instanceof Scene) {
			for (Map.Entry<Integer, Set<Consumer<Integer>>> c : ((Scene) cc).scene.entrySet()) {
				String tag = tagged.inverse()
						   .get(c);
				ret = ret + prefix + "" + c.getKey() + ":" + (tag == null ? "" : tag) + "\n";
				prefix = prefix + " ";
				for (Consumer<Integer> ccc : c.getValue())
					ret = ret + "\n" + debugPrintScene(ccc, prefix);
				prefix = prefix.substring(3);
			}

		}
		return ret;
	}

	@Override
	@HiddenInAutocomplete
	public boolean asMap_isProperty(String p) {
		if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();
		if (knownNonProperties.contains(p)) return false;

		return true;
	}

	protected Set<String> computeKnownNonProperties() {
		Set<String> r = new LinkedHashSet<>();
		Method[] m = this.getClass()
				 .getMethods();
		for (Method mm : m)
			r.add(mm.getName());
		Field[] f = this.getClass()
				.getFields();
		for (Field ff : f)
			r.add(ff.getName());
		return r;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_call(Object a, Object b) {
		throw new NotImplementedException();
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_get(String p) {
		Consumer<Integer> t = tagged.get(p);
		if (t == null) return super.asMap_get(p);
		return t;
	}


	static public Method supplier_get;
	static private Object[] nothing = {};

	static {
		try {
			supplier_get = Supplier.class.getDeclaredMethod("get");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	UniformBundle defaultBundle = null;

	protected UniformBundle getDefaultBundle() {
		if (defaultBundle != null) return defaultBundle;
		defaultBundle = new UniformBundle();
		this.attach(defaultBundle);
		return defaultBundle;
	}


	@Override
	@HiddenInAutocomplete
	public Object asMap_set(String p, Object o) {


		Object fo = Conversions.convert(o, Supplier.class);
		if (fo instanceof Supplier) return getDefaultBundle().set(p, (Supplier) fo);
		if (fo instanceof InvocationHandler) {
			return getDefaultBundle().set(p, (Supplier) () -> {
				try {
					return ((InvocationHandler) fo).invoke(fo, supplier_get, nothing);
				} catch (Throwable throwable) {
					throwable.printStackTrace();
				}
				return null;
			});
		}

		if (Uniform.isAccepableInstance(fo)) return getDefaultBundle().set(p, () -> fo).setIntOnly(fo instanceof Integer);

		Log.log("doublescore", "converting to perform ? " + o);
		o = Conversions.convert(o, Perform.class);
		if (o instanceof Perform) {
			Log.log("doublescore", "actually attaching a perform");
			return attach(p, (Perform) o);
		} else return super.asMap_set(p, o);
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object a) {
		throw new NotImplementedException();
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object a, Object b) {
		throw new NotImplementedException();
	}

	@Override
	public Object asMap_getElement(int element) {
		return new PassShim(element);
	}

	@Override
	public Object asMap_getElement(Object element) {
		if (element instanceof Number) return new PassShim(((Number) element).intValue());
		else return super.asMap_getElement(element);
	}

	@Override
	public List<Completion> getCompletionsFor(String prefix) {
		return super.getCompletionsFor(prefix);
	}

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
		boolean onceOnly = false;

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

		/**
		 * by default this transient will wait until it has run once in all contexts
		 */
		public Transient setOnceOnly() {
			onceOnly = true;
			return this;
		}

		@Override
		public boolean perform(int pass) {
			if (allContexts.remove(GraphicsContext.getContext())) call.accept(pass);
			return !onceOnly && allContexts.size() > 0;
		}

		@Override
		public int[] getPasses() {
			return passes;
		}
	}

	public class PassShim implements Linker.AsMap {
		int pass;

		public PassShim(int pass) {
			this.pass = pass;
		}

		@Override
		public boolean asMap_isProperty(String p) {
			return Scene.this.asMap_isProperty(p);
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			return Scene.this.asMap_call(a, b);
		}

		@Override
		public Object asMap_get(String p) {

			if (p.equals("_"))
				return new Subscope(this);

			return Scene.this.asMap_get("__" + pass + "__" + p);
		}

		@Override
		public Object asMap_set(String p, Object o) {
			o = Conversions.convert(o, Perform.class);
			if (o instanceof Perform)
			{
				return attach(pass, "__" + pass + "__"+p, (Perform) o);
			}
			else return Scene.super.asMap_set(p, o);
		}

		@Override
		public Object asMap_new(Object a) {
			throw new NotImplementedException();
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			throw new NotImplementedException();
		}

		@Override
		public Object asMap_getElement(int element) {
			throw new NotImplementedException();
		}

		@Override
		public Object asMap_setElement(int element, Object o) {
			throw new NotImplementedException();
		}
	}
}
