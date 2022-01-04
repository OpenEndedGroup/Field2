package fieldbox.boxes;

import field.app.RunLoop;
import field.app.ThreadSync2;
import field.utility.*;
import fieldbox.DefaultMenus;
import fieldbox.boxes.plugins.BoxDefaultCode;
import fieldbox.boxes.plugins.Missing;
import fieldbox.execution.*;
import fieldbox.io.IO;
import fieldnashorn.annotations.HiddenInAutocomplete;
import kotlin.jvm.functions.Function1;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The fundamental unit in Field --- the box.
 * <p>
 * A Box has properties and it's in a graph structure.
 * <p>
 * Specifically this is a directed, ordered, cyclic graph structure. It's not a multi-graph --- Boxes can only be connected 0 or 1 times. Parent / Child relationships are ordered and the order is
 * maintained. And the graph can have cycles (it's typically visited in breadth-first without cycles).
 * <p>
 * Much of the time properties are looked up in the graph in breadth first fashion either "upwards" (towards parents) or less-often downwards (collecting over all children).
 */
public class Box implements fieldlinker.AsMap, HandlesCompletion {

	static public final Dict.Prop<String> name = new Dict.Prop<>("name").type()
		.toCanon()
		.doc("the name of this box");
	static public final Dict.Prop<Rect> frame = new Dict.Prop<>("frame").type()
		.toCanon()
		.doc("the rectangle that this box occupies").set(IO.persistent, true).set(IO.perDocument, true);
	static public final Dict.Prop<Number> depth = new Dict.Prop<>("depth").type()
		.toCanon()
		.doc("provides a completely cosmetic 'z' coordinate for this box. Visible in VR and on stereo displays.").set(IO.persistent, true).set(IO.perDocument, true);
	static public final Dict.Prop<Boolean> hidden = new Dict.Prop<>("hidden").type()
		.toCanon()
		.doc("set this to true to hide this box (but be careful, for if it's hidden, how will you get it back again?)");

	static public final Dict.Prop<Boolean> decorative = new Dict.Prop<>("decorative").type()
		.toCanon()
		.doc("boxes like arrows and text have this set");

	static public final Dict.Prop<Boolean> undeletable = new Dict.Prop<>("undeletable").type()
		.toCanon()
		.doc("set this to true to make this box not deletable by conventional means");

	static public final Dict.Prop<Predicate<Box>> availableForCompletion = new Dict.Prop<>("availableForCompletion").type()
		.toCanon()
		.doc("provides a `Predicate<Box>` to help decide if a Property should be shown as available for completion").set(Dict.domain, "attributes");


	@HiddenInAutocomplete
	public final Dict properties = new Dict();

	@HiddenInAutocomplete
	public Set<Box> parents = new LinkedHashSet<>();
	@HiddenInAutocomplete
	public Set<Box> children = new LinkedHashSet<>();

	@HiddenInAutocomplete
	public Deque<Box> all = new ArrayDeque<>();

	protected Set<String> knownNonProperties;
	private String __cachedSimpleName = null;
	private long tick = 0;

	@HiddenInAutocomplete
	public boolean disconnected = false;


	public Box() {
		properties.put(IO.id, newID());
		BoxDefaultCode.configure(this);
	}

	@HiddenInAutocomplete
	static public String newID() {
		// ensure CallLogic is loaded
		Dict.Prop<IdempotencyMap<Supplier<Object>>> ignored = Callbacks.main;
		return "_" + UUID.randomUUID()
			.toString()
			.replace("-", "_");
	}


	@HiddenInAutocomplete
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

	/**
	 * Connect box 'b' to this box. b is now a child of this box, this box is now a parent of 'b'
	 */
	public Box connect(Box b) {
		if (children.add(b)) all.addLast(b);
		else {
			// restore ordering to LinkedHashSet
			children.remove(b);
			children.add(b);
		}

		if (b.parents.add(this)) b.all.addFirst(this);
		else {
			// restore ordering to LinkedHashSet
			b.parents.remove(this);
			b.parents.add(this);
		}

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

	@HiddenInAutocomplete
	public <T> Stream<T> has(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		return breadthFirst(direction).map(x -> x.properties.get(find))
			.filter(x -> x != null);
	}

	@HiddenInAutocomplete
	public <T> Stream<Box> whereHas(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		return breadthFirst(direction).filter(x -> x.properties.has(find));
	}

	@HiddenInAutocomplete
	public <T> Stream<T> find(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		return breadthFirst(direction).map(x -> x.properties.get(find))
			.filter(x -> x != null);
	}

	@HiddenInAutocomplete
	public <T> Optional<T> first(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		if (properties.has(find)) return Optional.of(properties.get(find));
		return breadthFirst(direction).map(x -> x.properties.get(find))
			.filter(x -> x != null)
			.findFirst();
	}

	@HiddenInAutocomplete
	public <T> Optional<T> next(Dict.Prop<T> find, Function<Box, Collection<Box>> direction) {
		if (properties.has(find)) return Optional.of(properties.get(find));
		return breadthFirst(direction).map(x -> x.properties.get(find))
			.filter(x -> x != null)
			.skip(1)
			.findFirst();
	}

	@HiddenInAutocomplete
	public <T> Optional<T> first(Dict.Prop<T> find) {
		return first(find, Box::_parents);
	}

	@HiddenInAutocomplete
	public <T> Optional<T> next(Dict.Prop<T> find) {
		return next(find, Box::_parents);
	}

	@HiddenInAutocomplete
	public <T> Optional<Box> where(Dict.Prop<T> find) {
		return whereHas(find, upwards()).findFirst();
	}

	@HiddenInAutocomplete
	public <G, T> Stream<T> call(Function<G, T> f, Class<G> guard, Function<Box, Collection<Box>> direction) {
		return breadthFirst(direction).filter(x -> guard.isInstance(x))
			.map(x -> f.apply((G) x));
	}

	/**
	 * vastly less garbage-y
	 */
	@HiddenInAutocomplete
	public void forEach(Consumer<Box> b) {
		LinkedHashSet<Box> visited = new LinkedHashSet<>();
		_forEach(this, b, visited);
	}

	static private void _forEach(Box t, Consumer<Box> b, LinkedHashSet<Box> visited) {
		if (visited.contains(t)) return;
		if (t.disconnected) return;
		b.accept(t);
		visited.add(t);
		for (Box c : new ArrayList<>(t.children))
			_forEach(c, b, visited);
		for (Box c : new ArrayList<>(t.parents))
			_forEach(c, b, visited);
	}


	@HiddenInAutocomplete
	public <G, T> Stream<T> call(Function<G, T> f, Class<G> guard) {
		return call(f, guard, upwards());
	}

	/**
	 * returns direction for upwards (parents) (e.g breadthFirst(Box::parents))
	 */
	@HiddenInAutocomplete
	public Function<Box, Collection<Box>> upwards() {
		return Box::_parents;
	}

	/**
	 * returns direction that goes upwards if it can, otherwise downwards.
	 */
	@HiddenInAutocomplete
	public Function<Box, Collection<Box>> upwardsOrDownwards() {
		return x -> {
			if (x.parents().size() > 0) return x.parents();
			return x.children();
		};
	}

	/**
	 * returns direction that goes downwards from here, but both downwards and upwards from everywhere else. This is good for getting everything _below_ a point in the graph
	 */
	@HiddenInAutocomplete
	public Function<Box, Collection<Box>> allDownwardsFrom() {
		Function<Box, Collection<Box>> b = both();
		return x -> {
			if (x == this)
				return x.children();
			else return b.apply(x);
		};
	}

	/**
	 * returns direction for downwards (children) (e.g breadthFirst(Box::children))
	 */
	@HiddenInAutocomplete
	public Function<Box, Collection<Box>> downwards() {
		return Box::_children;
	}

	/**
	 * returns direction for downwards and upwards (children and then parents) (e.g breadthFirst(Box::children))
	 */
	@HiddenInAutocomplete
	public Function<Box, Collection<Box>> both() {
		return Box::_all;
	}

	/**
	 * returns breadth first Stream given a direction function. It is an error to call this when this box is not connected to anything (which is a common error --- calling this method at
	 * construction time).
	 */
	@HiddenInAutocomplete
	public Stream<Box> breadthFirst(Function<Box, Collection<Box>> map) {

		if (this.all.size() == 0)
			Log.log("box.warning", () -> " breadthFirst called on a box not connected to the box graph");

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
					if (!b.disconnected)
						nextLevel.addAll(map.apply(b));
				nextLevel.removeAll(ret);
				ret.addAll(nextLevel);
				thisLevel = nextLevel;

				return thisLevel.iterator();

			}
		}.reset()
			.stream().filter(x -> !x.disconnected);
	}

	/**
	 * returns breadth first Stream given a direction function. This function ignores "disconnected" states. It's typically used only for IO
	 */
	@HiddenInAutocomplete
	public Stream<Box> breadthFirstAll(Function<Box, Collection<Box>> map) {

		if (this.all.size() == 0)
			Log.log("box.warning", () -> " breadthFirst called on a box not connected to the box graph");

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
		if (name == null) return "bx[" + (__cachedSimpleName == null ? __cachedSimpleName = this.getClass()
			.getSimpleName() : __cachedSimpleName) + "]";
		else return "bx[" + name + "]";
	}


	@Override
	@HiddenInAutocomplete
	public boolean asMap_isProperty(String p) {

		if (p == null) return false;

		if (p.equals("_")) return true;

		if (Dict.Canonical.findCanon(p) != null) return true;

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
			if (!Modifier.isStatic(ff.getModifiers()))
				r.add(ff.getName());

		r.remove("children");
		r.remove("parents");

		return r;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_get(String m) {

		if (m == null) return null;

		if (m.equals("_")) return new Subscope(this);

		if (m.equals("children")) return new BoxChildHelper(children);
		if (m.equals("parents")) return new BoxChildHelper(parents);

		Object ret = asMap_get_find(m);

		return asMap_get_interpret(ret);
	}

	@HiddenInAutocomplete
	public Object asMap_get_interpret(Object ret) {
		if (ret instanceof FunctionOfBox) {
			final Object fret = ret;
			try {
				return ((Supplier) (() -> ((FunctionOfBox) fret).apply(this)));
			}
			catch(ThreadSync2.KilledException e){}
		}

		if (ret instanceof BiFunctionOfBoxAnd) {
			final Object fret = ret;
			return QuoteCompletionHelpers.curry((BiFunction<Box, Object, Object>) ret, () -> this);
//			return ((Function) ((c) -> ((Box.BiFunctionOfBoxAnd) fret).apply(this, c)));
		}

		if (ret instanceof TriFunctionOfBoxAnd) {
			final Object fret = ret;
			return QuoteCompletionHelpers.curry((TriFunctionOfBoxAnd<Object, Object, Object>) ret, () -> this);
//			return ((BiFunction) ((a, b) -> ((Box.TriFunctionOfBoxAnd) fret).apply(this, a, b)));
		}

		if (ret instanceof FunctionOfBoxValued) {
			return ((FunctionOfBoxValued) ret).apply(this);
		}

		return ret;
	}

	@HiddenInAutocomplete
	public Object asMap_get_find(String m) {
		Dict.Prop canon = new Dict.Prop(m).toCanon();

		Object ret = null;

		if (!properties.has(canon) && canon.autoConstructor != null) {
			ret = canon.autoConstructor.get();
			if (ret != null) properties.put(canon, ret);

		}
		if (ret == null) ret = Missing.findFrom(this, canon);
		return ret;
	}

	@Override
	@HiddenInAutocomplete
	public boolean asMap_delete(Object o) {
		return Missing.delete(this, new Dict.Prop("" + o)) != null;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_set(String name, Object value) {

		// workaround bug in Nashorn
//		if (value instanceof ConsString) value = value.toString(); //jdk9 module security breaks this
		if (value != null && value.getClass().getName().endsWith("ConsString")) value = "" + value;

		Dict.Prop canon = new Dict.Prop(name).toCanon();

		if (canon.getAttributes().isTrue(Dict.readOnly, false))
			throw new IllegalArgumentException("can't write to property " + name);

/*
		if (value instanceof Iterable && canon.getTypeInformation()!=null && !Iterable.class.isAssignableFrom((Class<?>) canon.getTypeInformation().get(0)))
		{
			value = Drivers.iterableAsTrappedSet((Iterable)value);
		}
*/
		if (value instanceof Iterator && canon.getTypeInformation()!=null && !Iterable.class.isAssignableFrom((Class<?>) canon.getTypeInformation().get(0)))
		{
			value = Drivers.iteratorAsTrappedSet((Iterator)value);
		}

		if (value instanceof ThreadSync2.TrappedSet) {
			Object firstValue = ((ThreadSync2.TrappedSet) value).next();

			Object r = asMap_set(name, firstValue);

			Drivers.provokeCurrentFibre(System.identityHashCode(this) + "_" + name, new Function1<Object, Object>() {
				@Override
				public Object invoke(Object o) {
					if (o != null) {
						asMap_set(name, o);
					}
					return o;
				}
			}, ((ThreadSync2.TrappedSet) value));

			return r;
		}

		Function<Object, Object> c = canon.getAttributes().get(Dict.customCaster);
		if (c != null)
			value = c.apply(value);

		Object converted = Conversions.convert(value, canon.getTypeInformation());

		Missing.setTo(this, canon, converted);

		if (tick != RunLoop.tick) {
//			Drawing.dirty(this);
			tick = RunLoop.tick;
		}

		return this;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_call(Object a, Object b) {

		return Callbacks.call(this, b);
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object a) {


		FunctionOfBox<Box> b = find(DefaultMenus.newBox, both()).findFirst()
			.get();
		Box b2 = b.apply(this);

		b2.asMap_call(null, a);
		return b2;
	}

	@Override
	@HiddenInAutocomplete
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
		});
		b2.asMap_call(null, a);
		return b2;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_getElement(int element) {
		throw new Error();
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_getElement(Object element) {
//
//		return new XPathSupport(this).get(""+element);
		return asMap_get("" + element);
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_setElement(int element, Object v) {
		throw new Error();
	}


	@Override
	@HiddenInAutocomplete
	public List<Completion> getCompletionsFor(String prefix) {

		Log.log("completion.debug", () -> "inside getCompletionsFor (box) " + prefix);

		Stream<Dict.Prop> propStream = this.breadthFirst(this.upwards())
			.map(x -> x.properties.getMap()
				.keySet()).flatMap(Collection::stream);

		List<Completion> l1 = assembleCompletions(prefix, propStream);
		l1.forEach(x -> x.rank++);

		List<Completion> l1b = assembleCompletions(prefix, Dict.canonicalProperties());
		l1.addAll(l1b.stream()
			.filter(x -> {
				for (Completion c : l1)
					if (c.replacewith.equals(x.replacewith)) return false;
				return true;
			})
			.collect(Collectors.toList()));

		List<Completion> l2 = JavaSupport.javaSupport.getOptionCompletionsFor(this, prefix);

		l1.addAll(l2.stream()
			.filter(x -> {
				for (Completion c : l1)
					if (c.replacewith.equals(x.replacewith)) return false;
				return true;
			})
			.collect(Collectors.toList()));


		Log.log("completion.debug", () -> "returning " + l1 + " as completions");

		return l1;
	}

	private List<Completion> assembleCompletions(String prefix, Stream<Dict.Prop> propStream) {
		Set<String> s1 = new LinkedHashSet<>();

		try {
			s1 = propStream
				.filter(x -> x.getAttributes().get(availableForCompletion) == null || x.getAttributes().get(availableForCompletion).test(this))
				.map(Dict.Prop::getName)
				.filter(x -> !x.startsWith("_"))
				.collect(Collectors.toSet());
		} catch (IllegalArgumentException e) {// skip error about unconnected boxes
		}


		return s1.stream()
			.filter(x -> x.startsWith(prefix))
			.sorted()
			.map(x -> {
				Dict.Prop q = new Dict.Prop(x).findCanon();
				if (q == null) {
					return null;
				} else
					return new Completion(-1, -1, x, "<span class='type'>" + Conversions.fold(q.getTypeInformation(), t -> compress(
						t)) + "</span> " + possibleToString(this, q) + " &mdash; <span class='doc'>" + format(q.getDocumentation()) + "</span>");
			})
			.filter(x -> x != null)
			.collect(Collectors.toList());
	}

	private String possibleToString(Box box, Dict.Prop q) {
//		if (!box.properties.has(q)) return "";

		Optional m = box.find(q, box.upwards()).findFirst();
		if (!m.isPresent()) return "(unset)";

		Object v = m.get();
		if (v instanceof Box.FunctionOfBoxValued) {
			v = ((Box.FunctionOfBoxValued) v).apply(this);
		}

		// does this v have something to say?

		if (v == null)
			return "null";

		try {
			if (v.getClass().getMethod("toString").getDeclaringClass() != Object.class) {
				String r = " = " + v;
				if (r.length() < 40)
					return r;
			}

		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return "";
	}

	@HiddenInAutocomplete
	static public String format(String documentation) {
		String doc = MarkdownToHTML.convert(documentation);
		doc = doc.trim();
		if (doc.startsWith("<p>") && doc.endsWith("</p>")) {
			doc = doc.substring(0, doc.length() - 4).replaceFirst("<p>", "");
		}
		return doc;
	}

	protected Set<String> getAllPublicMethods() {
		Set<String> m1 = new LinkedHashSet<>();
		Method[] m = this.getClass()
			.getDeclaredMethods();
		for (Method mm : m) {
			if (mm.isAccessible()) {
				m1.add(mm.getName());
			}
		}

		return m1;
	}

	/**
	 * Marker interface, marks functions as taking a box as a parameter. Allows us to finesse the dispatch of functions stored in properties
	 */
	public interface FunctionOfBox<T> extends Function<Box, T> {
	}

	/**
	 * Marker interface, marks functions as taking a box as a parameter. Allows us to finesse the dispatch of functions stored in properties. Unlike FunctionOfBox this function is called before
	 * being returned
	 */
	public interface FunctionOfBoxValued<T> extends Function<Box, T> {
	}


	/**
	 * Marker interface, marks functions as taking a box and something else as a parameter. Allows us to finesse the dispatch of functions stored in properties
	 */
	public interface BiFunctionOfBoxAnd<T, R> extends BiFunction<Box, T, R> {
	}

	/**
	 * Marker interface, marks functions as taking a box and something else as a parameter. Allows us to finesse the dispatch of functions stored in properties
	 */
	public interface TriFunctionOfBoxAnd<T1, T2, R> {
		R apply(Box b, T1 t, T2 u);
	}

	/**
	 * todo: if 'prop' is persistent, so should prefix+'prop'
	 */
	static public class Subscope implements fieldlinker.AsMap {
		protected String prefix;
		protected fieldlinker.AsMap delegateTo;

		public Subscope(fieldlinker.AsMap from) {

			this.prefix = Execution.context.get()
				.peek().properties.getOrConstruct(IO.id);
			this.delegateTo = from;
		}

		public Subscope(Box prefixFrom, Box from) {
			this.prefix = prefixFrom.properties.getOrConstruct(IO.id);
			this.delegateTo = from;
		}

		public Subscope(String prefixFrom, Box from) {
			this.prefix = prefixFrom;
			this.delegateTo = from;
		}


		@Override
		public boolean asMap_isProperty(String p) {
			return delegateTo.asMap_isProperty(prefix + p);
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			return delegateTo.asMap_call(a, b);
		}

		@Override
		public Object asMap_get(String p) {
			return delegateTo.asMap_get(prefix + p);
		}


		@Override
		public boolean asMap_delete(Object o) {
			return delegateTo.asMap_delete(prefix + o);
		}

		@Override
		public Object asMap_set(String p, Object o) {
			return delegateTo.asMap_set(prefix + p, o);
		}

		@Override
		public Object asMap_new(Object a) {
			return delegateTo.asMap_new(a);
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			return delegateTo.asMap_new(a, b);
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


	static public class CollectedMap<T> implements fieldlinker.AsMap {
		private final Dict.Prop<IdempotencyMap<T>> storageProperty;
		private final Box from;
		private Function<Box, IdempotencyMap<T>> autoconstructor;
		private BiFunction<CollectedMap<T>, Object, Object> calling;

		public CollectedMap(Dict.Prop<IdempotencyMap<T>> storageProperty, Box from, Function<Box, IdempotencyMap<T>> autoconstructor) {
			this.storageProperty = storageProperty;
			this.from = from;
			this.autoconstructor = autoconstructor;
		}

		@Override
		public boolean asMap_isProperty(String p) {
			return true;
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			if (calling == null) throw new Error();
			return calling.apply(this, b);
		}

		@Override
		public Object asMap_call(Object a) {
			if (calling == null) throw new Error();
			return calling.apply(this, a);
		}

		@Override
		public Object asMap_get(String p) {
			T q = from.breadthFirst(from.upwards())
				.map(x -> x.properties.get(storageProperty))
				.filter(x -> x != null)
				.filter(x -> x.containsKey(p))
				.findFirst()
				.map(x -> x.get(p))
				.orElseGet(() -> null);
			if (q != null) return q;

			return from.properties.computeIfAbsent(storageProperty, (k) -> autoconstructor.apply(from))
				.get(p);
		}

		@Override
		public boolean asMap_delete(Object o) {
			return false;
		}

		@Override
		public Object asMap_set(String p, Object o) {
			return from.properties.computeIfAbsent(storageProperty, (k) -> autoconstructor.apply(from))
				.asMap_set(p, o);
		}

		@Override
		public Object asMap_new(Object a) {
			return from.properties.getOrConstruct(storageProperty)
				.asMap_new(a);
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			return from.properties.getOrConstruct(storageProperty)
				.asMap_new(a, b);
		}

		@Override
		public Object asMap_getElement(int element) {
			throw new Error();
		}

		@Override
		public Object asMap_setElement(int element, Object o) {
			throw new Error();
		}

		public CollectedMap<T> makeCallable(BiFunction<CollectedMap<T>, Object, Object> calling) {
			this.calling = calling;
			return this;
		}
	}


	static public class TemplateMap<T extends Box> implements fieldlinker.AsMap {


		private final String namePrefix;
		private final Box startFrom;
		private final Class<T> clazz;
		private Function<Box, T> autoconstructor;

		private BiFunction<TemplateMap<T>, Object, Object> calling;

		public TemplateMap(Box startFrom, String namePrefix, Class<T> clazz, Function<Box, T> autoconstructor) {
			this.startFrom = startFrom;
			this.namePrefix = namePrefix;
			this.clazz = clazz;
			this.autoconstructor = autoconstructor;
		}

		@Override
		public boolean asMap_isProperty(String p) {
			return true;
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			if (calling == null) throw new Error();
			return calling.apply(this, b);
		}

		@Override
		public Object asMap_call(Object a) {
			if (calling == null) throw new Error();
			return calling.apply(this, a);
		}

		@Override
		public Object asMap_get(String p) {

			Optional<Box> q = startFrom.breadthFirst(startFrom.upwards())
				.flatMap(x -> x.children()
					.stream()
					.filter(bx -> clazz.isInstance(bx) && bx.properties.getOr(Box.name, () -> "")
						.equals(namePrefix + ":" + p)))
				.findFirst();


			if (q.isPresent()) return q.get();

			Box b = autoconstructor.apply(startFrom);
			b.properties.put(Box.name, namePrefix + ":" + p);
			if (!clazz.isInstance(b))
				throw new IllegalArgumentException(" autoconstructor didn't return an object of the correct class <" + b.getClass() + "> <" + clazz + ">");

			startFrom.connect(b);

			return b;
		}

		@Override
		public boolean asMap_delete(Object o) {
			return false;
		}

		@Override
		public Object asMap_set(String p, Object o) {
			return null;
		}

		@Override
		public Object asMap_new(Object a) {
			return null;
		}

		@Override
		public Object asMap_new(Object a, Object b) {
			return null;
		}

		@Override
		public Object asMap_getElement(int element) {
			return null;
		}

		@Override
		public Object asMap_setElement(int element, Object o) {
			return null;
		}

		public TemplateMap<T> makeCallable(BiFunction<TemplateMap<T>, Object, Object> calling) {
			this.calling = calling;
			return this;
		}
	}
}