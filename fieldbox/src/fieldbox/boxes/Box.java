package fieldbox.boxes;

import field.graphics.RunLoop;
import field.utility.*;
import fieldbox.DefaultMenus;
import fieldbox.execution.Completion;
import fieldbox.execution.HandlesCompletion;
import fieldbox.io.IO;
import fieldlinker.Linker;
import fieldbox.execution.JavaSupport;
import fieldnashorn.annotations.HiddenInAutocomplete;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.ScriptUtils;
import jdk.nashorn.internal.objects.ScriptFunctionImpl;
import jdk.nashorn.internal.runtime.ConsString;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The fundemental unit in Field --- the box.
 * <p>
 * A Box has properties and it's in a graph structure.
 * <p>
 * Specifically this is a directed, ordered, cyclic graph structure. It's not a multi-graph --- Boxes can only be connected 0 or 1 times. Parent /
 * Child relationships are ordered and the order is maintained. And the graph can have cycles (it's typically visited in breadth-first without
 * cycles).
 * <p>
 * Much of the time properties are looked up in the graph in breadth first fashion either "upwards" (towards parents) or less-often downwards
 * (collecting over all children).
 */
public class Box implements Linker.AsMap, HandlesCompletion {

	static public final Dict.Prop<String> name = new Dict.Prop<>("name").type()
									    .toCannon()
									    .doc("the name of this box");
	static public final Dict.Prop<Rect> frame = new Dict.Prop<>("frame").type()
									    .toCannon()
									    .doc("the rectangle that this box occupies");
	// not currently implemented everywhere
	static public final Dict.Prop<Boolean> hidden = new Dict.Prop<>("hidden").type()
										 .toCannon()
										 .doc("set this to true to hide this box (but be careful, for if it's hidden, how will you get it back again?)");


	/**
	 * Marker interface, marks functions as taking a box as a parameter. Allows us to finesse the dispatch of functions stored in properties
	 */
	static public interface FunctionOfBox<T> extends Function<Box, T> {
	}

	public Set<Box> parents = new LinkedHashSet<>();
	public Set<Box> children = new LinkedHashSet<>();
	public Deque<Box> all = new ArrayDeque<>();

	public final Dict properties = new Dict();

	public Box() {
		properties.put(IO.id, UUID.randomUUID()
					  .toString());
	}

	/**
	 * Connect box 'b' to this box. b is now a child of this box, this box is now a parent of 'b'
	 */
	public Box connect(Box b) {
		if (children.add(b)) all.addLast(b);

		if (b.parents.add(this)) b.all.addFirst(this);

		return this;
	}

	/**
	 * Disconnect box 'b' to this box. b is now no longer child of this box, this box is now no longer parent of 'b'
	 */
	public Box disconnect(Box b) {
		children.remove(b);
		b.parents.remove(this);

		all.remove(b);
		b.all.remove(this);

		return this;
	}

	/**
	 * Disconnect this box from everything.
	 */
	public Box disconnectFromAll() {
		for (Box b : new ArrayList<Box>(children))
			disconnect(b);

		for (Box b : new ArrayList<>(parents))
			b.disconnect(this);

		all.clear();

		return this;
	}

	public Set<Box> parents() {
		return Collections.unmodifiableSet(parents);
	}

	public Set<Box> children() {
		return Collections.unmodifiableSet(children);
	}

	protected Set<Box> _parents() {
		return parents;
	}

	protected Set<Box> _children() {
		return children;
	}

	protected Deque<Box> _all() {
		return all;
	}

	public <T> Stream<T> has(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		return breadthFirst(direction).map(x -> x.properties.get(find))
					      .filter(x -> x != null);
	}

	public <T> Stream<Box> whereHas(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		return breadthFirst(direction).filter(x -> x.properties.has(find));
	}

	public <T> Stream<T> find(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		return breadthFirst(direction).map(x -> x.properties.get(find))
					      .filter(x -> x != null);
	}

	public <T> Optional<T> first(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		if (properties.has(find)) return Optional.of(properties.get(find));
		return breadthFirst(direction).map(x -> x.properties.get(find))
					      .filter(x -> x != null)
					      .findFirst();
	}

	public <T> Optional<T> next(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		if (properties.has(find)) return Optional.of(properties.get(find));
		return breadthFirst(direction).map(x -> x.properties.get(find))
					      .filter(x -> x != null)
					      .skip(1)
					      .findFirst();
	}

	public <T> Optional<T> first(Dict.Prop<T> find) {
		return first(find, Box::_parents);
	}

	public <T> Optional<T> next(Dict.Prop<T> find) {
		return next(find, Box::_parents);
	}

	public <T> Optional<Box> where(Dict.Prop<T> find) {
		return whereHas(find, upwards()).findFirst();
	}

	public <G, T> Stream<T> call(Function<G, T> f, Class<G> guard, Function<Box, Collection<Box>> direction) {
		return breadthFirst(direction).filter(x -> guard.isInstance(x))
					      .map(x -> f.apply((G) x));
	}

	public <G, T> Stream<T> call(Function<G, T> f, Class<G> guard) {
		return call(f, guard, upwards());
	}

	/**
	 * returns direction for upwards (parents) (e.g breadthFirst(Box::parents))
	 */
	public Function<Box, Collection<Box>> upwards() {
		return Box::_parents;
	}

	/**
	 * returns direction for downwards (children) (e.g breadthFirst(Box::children))
	 */
	public Function<Box, Collection<Box>> downwards() {
		return Box::_children;
	}


	/**
	 * returns direction for downwards and upwards (children and then parents) (e.g breadthFirst(Box::children))
	 */
	public Function<Box, Collection<Box>> both() {
		return Box::_all;
	}

	/**
	 * returns breadth first Stream given a direction function. It is an error to call this when this box is not connected to anything (which is a common error --- calling this method at construction time).
	 */
	public Stream<Box> breadthFirst(Function<Box, Collection<Box>> map) {

		if (this.all.size()==0) throw new IllegalArgumentException(" breadthFirst called on a box not connected to the box graph");

		return new Lazy<Box>() {
			LinkedHashSet<Box> ret = null;
			Set<Box> thisLevel = null;

			protected Iterator<Box> initialize() {
				ret = new LinkedHashSet<>();
				ret.add(Box.this);
				thisLevel = ret;
				return ret.iterator();
			}

			@Override
			protected Iterator<Box> pull() {
				if (thisLevel.size() == 0) return null;
				Set<Box> nextLevel = new LinkedHashSet<>();
				for (Box b : thisLevel)
					nextLevel.addAll(map.apply(b));
				nextLevel.removeAll(ret);
				ret.addAll(nextLevel);
				thisLevel = nextLevel;
				return thisLevel.iterator();
			}
		}.reset()
		 .stream();
	}

	@Override
	public String toString() {
		String name = properties.get(Box.name);
		if (name == null) return "bx[" + this.getClass()
						     .getSimpleName() + "]";
		else return "bx[" + name + "]";
	}


	@Override
	@HiddenInAutocomplete
	public boolean asMap_isProperty(String p) {
		if (Dict.Canonical.findCannon(p) != null) return true;

		if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();

		if (knownNonProperties.contains(p)) return false;

		return true;
	}

	protected Set<String> knownNonProperties;
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
	public Object asMap_get(String m) {

		Dict.Prop cannon = new Dict.Prop(m).toCannon();

		Object ret = find(cannon, upwards()).findFirst()
						    .orElse(null);

		if (ret instanceof Box.FunctionOfBox) {
			final Object fret = ret;
			return ((Supplier) (() -> ((Box.FunctionOfBox) fret).apply(this)));
		}

		if (ret == null && cannon.autoConstructor != null) {
			properties.put(cannon, ret = cannon.autoConstructor.get());
		}

		return ret;
	}

	private long tick = 0;

	@Override
	@HiddenInAutocomplete
	public Object asMap_set(String name, Object value) {

		// workaround bug in Nashorn
		if (value instanceof ConsString) value = ((ConsString) value).toString();


		Log.log("underscore.debug", " underscore box set :" + name + " to " + value.getClass() + " <" + Function.class.getName() + ">");
		Dict.Prop cannon = new Dict.Prop(name).toCannon();

		Log.log("underscore.debug", " cannonical type information " + cannon.getTypeInformation());

		Object converted = convert(value, cannon.getTypeInformation());

		properties.put(cannon, converted);

		Log.log("underscore.debug", () -> {
			Log.log("underscore.debug", " PROPERTIES NOW :");
			for (Map.Entry<Dict.Prop, Object> q : properties.getMap()
									.entrySet()) {
				try {
					Log.log("underscore.debug", "     " + q.getKey() + " = " + q.getValue());
				} catch (NullPointerException e) {
					//JDK bug JDK-8035426 --- sometimes Nashorn lambdas throw NPE's when they are .toString'd
				}
			}
			return null;
		});

		if (tick != RunLoop.tick) {
			Drawing.dirty(this);
			tick = RunLoop.tick;
		}

		return this;
	}

	static public Object convert(Object value, List<Class> fit) {
		if (fit == null) return value;
		if (fit.get(0)
		       .isInstance(value)) return value;

		// promote non-arrays to arrays
		if (List.class.isAssignableFrom(fit.get(0))) {
			if (!(value instanceof List)) {
				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
			} else {
				return value;
			}
		} else if (Map.class.isAssignableFrom(fit.get(0)) && String.class.isAssignableFrom(fit.get(1))) {
			// promote non-Map<String, V> to Map<String, V>
			if (!(value instanceof Map)) {
				return Collections.singletonMap("" + value + ":" + System.identityHashCode(value), convert(value, fit.subList(2, fit.size())));
			} else {
				return value;
			}

		} else if (Collection.class.isAssignableFrom(fit.get(0))) {
			if (!(value instanceof Collection)) {
				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
			} else {
				return value;
			}

		}

		if (value instanceof ScriptFunctionImpl) {
			StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{fit.get(0)}, (ScriptObject) value, MethodHandles.lookup());
			try {
				return adapterClassFor.getRepresentedClass()
						      .newInstance();
			} catch (InstantiationException e) {
				Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
			} catch (IllegalAccessException e) {
				Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
			}
		}

		return value;
	}

	@Override
	public Object asMap_call(Object a, Object b) {
		System.err.println(" call called :" + a + " " + b + " " + (b instanceof Map ? ((Map) b).keySet() : b.getClass()
														    .getSuperclass() + " " + Arrays.asList(b.getClass()
																			    .getInterfaces())));
		boolean success = false;
		try {
			Map<?, ?> m = (Map<?, ?>) ScriptUtils.convert(b, Map.class);
			for (Map.Entry<?, ?> e : m.entrySet()) {
				asMap_set("" + e.getKey(), e.getValue());
			}
			success = true;
		} catch (UnsupportedOperationException e) {

		}
		if (!success) {
			throw new IllegalArgumentException(" can't understand parameter :" + b);
		}
		return this;
	}


	@Override
	public Object asMap_new(Object a) {


		FunctionOfBox<Box> b = find(DefaultMenus.newBox, both()).findFirst()
									.get();
		Box b2 = b.apply(this);
		// we need a deeper copy than putAll can provide right now (Set, List and Map for example);
		//b2.properties.putAll(this.properties);

		b2.asMap_call(null, a);
		return b2;
	}

	@Override
	public Object asMap_new(Object tag, Object a) {

		Optional<Box> m = children().stream()
					    .filter(x -> x.properties.has(Boxes.tag))
					    .filter(x -> x.properties.get(Boxes.tag)
								     .equals(tag))
					    .findFirst();

		Box b2 = m.orElseGet(() -> {

			FunctionOfBox<Box> b = find(DefaultMenus.newBox, both()).findFirst()
										.get();
			return b.apply(this);
			// we need a deeper copy than putAll can provide right now (Set, List and Map for example);
			//b2.properties.putAll(this.properties);
		});
		b2.asMap_call(null, a);
		return b2;
	}

	@Override
	public List<Completion> getCompletionsFor(String prefix) {
		Set<String> s1 = this.breadthFirst(this.upwards()).map(x -> x.properties.getMap().keySet()).flatMap(x -> x.stream()).map(x -> x.getName())
				   .filter(x -> !x.startsWith("_")).collect(Collectors.toSet());

		List<Completion> l1 = s1.stream().filter(x -> x.startsWith(prefix)).sorted().map(x -> {
			Dict.Prop q = new Dict.Prop(x).findCannon();
			if (q == null) {
				return null;
			} else return new Completion(-1, -1, x, "<span class='type'>" + Conversions
				    .fold(q.getTypeInformation(), t -> compress(t)) + "</span> <span class='doc'>" + q
				    .getDocumentation() + "</span>");
		}).filter(x -> x != null).collect(Collectors.toList());

		List<Completion> l2 = JavaSupport.javaSupport.getOptionCompletionsFor(this, prefix);

		l1.addAll(l2.stream().filter(x -> {
			for (Completion c : l1)
				if (c.replacewith.equals(x.replacewith)) return false;
			return true;
		}).collect(Collectors.toList()));
		return l1;
	}


	protected Set<String> getAllPublicMethods() {
		Set<String> m1 = new LinkedHashSet<>();
		Method[] m = this.getClass().getDeclaredMethods();
		for (Method mm : m) {
			if (mm.isAccessible()) {
				m1.add(mm.getName());
			}
		}
		return m1;
	}

	static public String compress(String signature) {
		signature = " " + signature;

		Pattern p = Pattern.compile("([A-Za-z]*?)[\\.\\$]([A-Za-z]*?)");
		Matcher m = p.matcher(signature);

		while (m.find()) {
			signature = m.replaceAll("$2");
			m = p.matcher(signature);
		}

		signature = signature.replace(" public ", " ");
		signature = signature.replace(" final ", " ");
		signature = signature.replace(" void ", " ");
		signature = signature.replace("  ", " ");
		signature = signature.replace("  ", " ");

		return signature.trim();
	}
}
