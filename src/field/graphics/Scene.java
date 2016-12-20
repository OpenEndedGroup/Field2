package field.graphics;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import field.utility.*;
import fieldbox.boxes.Box;
import fieldbox.execution.Completion;
import fieldbox.execution.Errors;
import fieldbox.execution.InverseDebugMapping;
import fieldlinker.Linker;
import fieldnashorn.annotations.HiddenInAutocomplete;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

;

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
	public TreeMap<Integer, Set<Consumer<Integer>>> internalScene = new TreeMap<>();
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
		Set<Consumer<Integer>> c = internalScene.get(pass);
		if (c == null) internalScene.put(pass, c = new LinkedHashSet<Consumer<Integer>>());
		return c.add(p);
	}

	/**
	 * A (int pass, String tag, Consumer<Integer>) tuple can be effectively cast as a Perform. The int says what pass the consumer should be run for, and the consumer is called for that pass.
	 * <p>
	 * The additional "tag" gives this consumer a name, so that it can be referred to later. Note that at any given time there can only be one Perform or Consumer with any particular tag
	 * associated with a Scene, anything previously tagged with this internalScene is automatically disconnected. This allows you write code in "idempotent" style: specifically you can have connect(...)
	 * calls that can be safely (although not particularlly efficiently) called over and over again. This is good for rapidly iterating on a problem without having to worry about tearing down
	 * resources that you constructed.
	 */
	public boolean attach(int pass, String tag, Consumer<Integer> p) {
		Consumer<Integer> was = tagged.remove(tag);
		if (was != null) detach(was);

		Set<Consumer<Integer>> c = internalScene.computeIfAbsent(pass, k -> new LinkedHashSet<>());

		tagged.put(tag, p);

		return c.add(p);
	}

	/**
	 * The additional "tag" gives this consumer a name, so that it can be referred to later. Note that at any given time there can only be one Perform or Consumer with any particular tag
	 * associated with a Scene, anything previously tagged with this internalScene is automatically disconnected. This allows you write code in "idempotent" style: specifically you can have connect(...)
	 * calls that can be safely (although not particularlly efficiently) called over and over again. This is good for rapidly iterating on a problem without having to worry about tearing down
	 * resources that you constructed.
	 */
	public boolean attach(String tag, Perform p) {
		Consumer<Integer> was = tagged.remove(tag);
		if (was != null) detach(was);

		boolean pp = false;
		for (int pass : p.getPasses()) {
			Set<Consumer<Integer>> c = internalScene.computeIfAbsent(pass, k -> new LinkedHashSet<>());
			pp |= c.add(p);
		}

		tagged.put(tag, p);

		return pp;
	}

	/**
	 * Disconnects a Perform from this Scene. Care has been taken to ensure you can do this while the internalScene is being traversed.
	 */
	public void detach(Consumer<Integer> p) {

		List<Boolean> removed = internalScene.values()
			.stream()
			.map(x -> x.remove(p))
			.filter(x -> x)
			.collect(Collectors.toList());
		if (removed.size() == 0) {
		}

	}

	/**
	 * Disconnects a Perform from this Scene. Care has been taken to ensure you can do this while the internalScene is being traversed.
	 * <p>
	 * returns true if there was something tagged "tag";
	 */
	public boolean detach(String tag) {
		Consumer<Integer> was = tagged.remove(tag);
		if (was != null) detach(was);
		return was != null;
	}

	/**
	 * connects a Perform to this Scene. Care has been taken to ensure you can do this while the internalScene is being traversed.
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
	 * updates everything in the internalScene. This is the main entry point for performing a complete update cycle.
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
		GraphicsContext.checkError(() -> "on internalScene entry for " + this);
		exceptions.clear();

		boolean ret = true;

		try {

			TreeMap<Integer, Set<Consumer<Integer>>> c1 = collectChildrenPasses();
			if (c1 == null) c1 = new TreeMap<>();

			for (Map.Entry<Integer, Set<Consumer<Integer>>> c2 : internalScene.entrySet()) {
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
					GraphicsContext.checkError(() -> "on " + n);
					if (!wrappedCall(n, i)) {
						detach(n);
					}
					GraphicsContext.checkError(() -> "on " + n);
				}

			}

			while (!a.isEmpty()) ret = wrappedCall(a.poll().second);

			if (exceptions.size() > 0) {
				System.err.println(" Exceptions thrown in internalScene update ");
				System.err.println(" (Performs responsible have been removed from the internalScene, if they were directly attached) ");
				System.err.println(" Details: ");
				for (Throwable t : exceptions) {
					t.printStackTrace();
				}
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
//			System.exit(0);
		}

		return ret;
	}

	public List<Throwable> getException() {
		return exceptions;
	}

	private boolean wrappedCall(Consumer<Integer> c, Integer i) {
		if (c == null) return false;
		try {
			if (c instanceof Perform)
				return ((Perform) c).perform(i);
			else
				c.accept(i);

			return true;
		} catch (Throwable t) {
			if (!(t instanceof Cancel)) exceptions.add(t);
			else return false;
			return true;
		} finally {
			try {
				GraphicsContext.checkError(() -> "error on " + c);
			} catch (IllegalStateException e) {
				Errors.tryToReportTo(e, "pass " + i, c);
			}

		}
	}

	private boolean wrappedCall(Callable<Boolean> middle) {
		if (middle == null) return false;
		try {
			return middle.call();
		} catch (Throwable t) {
			if (!(t instanceof Cancel)) exceptions.add(t);
			else return false;
//			return false;
			return true;
		} finally {
			GraphicsContext.checkError(() -> "error on " + middle);
		}
	}

	/**
	 * returns a toString for everything in the internalScene.
	 */

	public String debugPrintScene() {
		Set<Object> seen = new LinkedHashSet<>();
		return _debugPrintScene(seen);
	}

	public String _debugPrintScene(Set<Object> seen) {

		String ret = "" + this + "\n<p>";
		String indent = "&nbsp;&nbsp;&nbsp;";
		String prefix = indent;
		if (seen.contains(this)) return ret;

		for (Map.Entry<Integer, Set<Consumer<Integer>>> c : internalScene.entrySet()) {
			String tag = tagged.inverse()
				.get(c.getValue());
			ret = ret + prefix + "" + c.getKey() + ":" + (tag == null ? "" : tag) + "\n<p>";
			prefix = prefix + indent;
			for (Consumer<Integer> cc : c.getValue())
				ret = ret + "\n<p>" + debugPrintScene(cc, prefix, seen);
			prefix = prefix.substring(indent.length());
		}
		return "{HTML}" + ret;
	}

	private String debugPrintScene(Consumer<Integer> cc, String prefix, Set<Object> seen) {


		String ccDesc = "" + cc;
		if (cc.getClass().getName().startsWith("jdk.nashorn.javaadapters."))
			ccDesc = "";

		String desc = InverseDebugMapping.describe(cc);

		if (!ccDesc.contains(desc))
			ccDesc += ":" + desc;

		String ret = prefix + "" + ccDesc + "\n<p>";
		String indent = "&nbsp;&nbsp;&nbsp;";

		if (seen.contains(cc)) return ret;
		seen.add(cc);
		if (cc instanceof Scene) {
			for (Map.Entry<Integer, Set<Consumer<Integer>>> c : ((Scene) cc).internalScene.entrySet()) {
				String tag = tagged.inverse()
					.get(c.getValue());
				ret = ret + prefix + "" + c.getKey() + ":" + (tag == null ? "" : tag) + "\n<p>";
				prefix = prefix + indent;
				for (Consumer<Integer> ccc : c.getValue())
					ret = ret + "\n<p>" + debugPrintScene(ccc, prefix, seen);
				prefix = prefix.substring(indent.length());
			}

		}
		return ret;
	}

	@Override
	public String toString() {
		return super.toString() + InverseDebugMapping.describe(this);
	}

	@Override
	@HiddenInAutocomplete
	public boolean asMap_isProperty(String p) {
		if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();
		return !knownNonProperties.contains(p);

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
		throw new Error();
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
	public List<Completion> getCompletionsFor(String prefix) {

		List<Completion> u = new ArrayList<>();
		if (defaultBundle != null) {
			u.addAll(defaultBundle.getCompletionsFor(prefix));
		}

		for (Completion c : u)
			c.rank = -100;

		List<Completion> c = super.getCompletionsFor(prefix);

		u.addAll(c);
		return u;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_set(String p, Object o) {
		Object fo = Conversions.convert(o, Supplier.class);
		if (fo instanceof Supplier) return getDefaultBundle().set(p, (Supplier) fo);
		if (fo instanceof InvocationHandler) {
			return getDefaultBundle().set(p, () -> {
				try {
					return ((InvocationHandler) fo).invoke(fo, supplier_get, nothing);
				} catch (Throwable throwable) {
					throwable.printStackTrace();
				}
				return null;
			});
		}

		if (Uniform.isAccepableInstance(fo))
			return getDefaultBundle().set(p, () -> fo).setIntOnly(fo instanceof Integer);

		o = Conversions.convert(o, Perform.class);
		if (o instanceof Perform) {
			return attach(p, (Perform) o);
		} else return super.asMap_set(p, o);
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object a) {
		throw new Error();
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object a, Object b) {
		throw new Error();
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
	public Object asMap_setElement(int element, Object v) {
		throw new IllegalArgumentException("Can't set [" + element + "] on a internalScene. Add it as a name instead (e.g internalScene[" + element + "].something = ...");
	}


	/**
	 * Although we can build the Scene structure out of (int, Consumer<int>) tuples, it's more along the grain of Java's type system to use an interface with both
	 */
	public interface Perform extends Consumer<Integer>, Errors.SavesErrorConsumer {
		boolean perform(int pass);

		default int[] getPasses() {
			return new int[]{0};
		}

		default void accept(Integer p) {
			perform(p);
		}

		default Errors.ErrorConsumer getErrorConsumer() {
			return Errors.errors.retrieve(this);
		}

		default void setErrorConsumer(Errors.ErrorConsumer c) {
			Errors.errors.store(this, c);
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
			ec = Errors.errors.get();
		}

		public Transient(Runnable p, int... passes) {
			this.passes = passes;
			this.call = (x) -> p.run();
			ec = Errors.errors.get();
		}

		public Transient(Consumer<Integer> m, int... passes) {
			this.passes = passes;
			this.call = (x) -> m.accept(x);
			ec = Errors.errors.get();
		}

		/**
		 * by default this transient will wait until it has run once in all contexts. Set this to change make this transient execute only once in the first context that happens to execute it
		 */
		public Transient setOnceOnly() {
			onceOnly = true;
			return this;
		}

		/**
		 * by default this transient will wait until it has run once in all contexts. Use this method to limit this to just some of the graphics contexts
		 */
		public Transient setAllContexts(Collection<GraphicsContext> c) {
			allContexts.clear();
			allContexts.addAll(c);
			return this;
		}

		/**
		 * by default this transient will wait until it has run once in all contexts. Use this method to limit this to just some of the graphics contexts where object 'o' has state stored
		 */
		public Transient setAllContextsFor(Object o) {
			allContexts.clear();
			GraphicsContext.allGraphicsContexts.stream().filter(x -> x.context.containsKey(o)).forEach(x -> allContexts.add(x));
			return this;
		}

		@Override
		public boolean perform(int pass) {
			if (allContexts.remove(GraphicsContext.getContext())) call.accept(pass);

			if (!onceOnly) {
				if (allContexts.size() > 0) {
					System.out.println(" transient will wait for remaining contexts :" + allContexts);
				}
			}

			return !onceOnly && allContexts.size() > 0;
		}

		@Override
		public int[] getPasses() {
			return passes;
		}

		Errors.ErrorConsumer ec;

		@Override
		public void setErrorConsumer(Errors.ErrorConsumer c) {
			ec = c;
		}

		@Override
		public Errors.ErrorConsumer getErrorConsumer() {
			return ec;
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

			if (o instanceof Perform) {
				return attach("__" + pass + "__" + p, new PassShift(pass, ((Perform) o)));
			}

			o = Conversions.convert(o, Perform.class);
			if (o instanceof Perform) {
				return attach(pass, "__" + pass + "__" + p, (Perform) o);
			} else return Scene.super.asMap_set(p, o);
		}

		@Override
		public boolean asMap_delete(Object o) {
			return Scene.this.asMap_delete("__" + pass + "__" + o);
		}

		@Override
		public Object asMap_new(Object a) {
			throw new Error();
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			throw new Error();
		}

		@Override
		public Object asMap_getElement(int element) {
			throw new Error();
		}

		@Override
		public Object asMap_setElement(int element, Object o) {
			throw new Error();
		}
	}

	public class PassShift implements Perform {
		private int shift;
		private final Perform child;

		public PassShift(int shift, Perform child) {
			this.shift = shift;
			this.child = child;
		}

		@Override
		public boolean perform(int pass) {
			return child.perform(-pass + shift);
		}

		@Override
		public int[] getPasses() {
			int[] p = child.getPasses();
			int[] p2 = new int[p.length];
			for (int i = 0; i < p.length; i++)
				p2[i] = p[i] + shift;
			return p2;
		}

		@Override
		public void setErrorConsumer(Errors.ErrorConsumer c) {
			child.setErrorConsumer(c);
		}

		@Override
		public Errors.ErrorConsumer getErrorConsumer() {
			return child.getErrorConsumer();
		}


		@Override
		public String toString() {
			return "<i>shift" + shift + "</i>:" + child;
		}
	}

}
