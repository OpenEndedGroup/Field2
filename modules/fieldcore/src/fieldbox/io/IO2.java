package fieldbox.io;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import field.utility.Dict;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A database backed fileformat
 */
public class IO2 {

	// known roots
	static public final String tagged = "__tagged__";

	static public final Dict.Prop<Vertex> _dbvertex = new Dict.Prop<Vertex>("_dbvertex").type().toCannon();
	static public final Dict.Prop<String> _dbparent = new Dict.Prop<Vertex>("_dbparent").type().toCannon().set(Dict.domain, "attributes");

	static public final Dict.Prop<String> tags = new Dict.Prop<Vertex>("tags").type().toCannon().doc("TODO").set(IO.persistent, true).set(_dbparent, tagged);

	public final OrientGraph graph;
	private UUID version;

	public IO2() {

		try {
			printHooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
		graph = new OrientGraph("remote:/localhost/FIELD");
		Orient.instance().removeShutdownHook();
		try {
			printHooks();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Set<String> indexedKeys = graph.getIndexedKeys(Vertex.class);
		if (indexedKeys.size() == 0) {
			graph.createKeyIndex("uid", Vertex.class, new Parameter<>("type", "UNIQUE"));
		}


	}

	private static void printHooks() throws Exception {
		Class clazz = Class.forName("java.lang.ApplicationShutdownHooks");
		Field field = clazz.getDeclaredField("hooks");
		field.setAccessible(true);
		Object hooks = field.get(null);

		Map map = (Map) hooks;
		System.out.println("class = " + hooks.getClass().getName());
		System.out.println(hooks); //hooks is a Map<Thread, Thread>

		Set<Object> toRemove = new LinkedHashSet<>();
		for (Object o : map.entrySet()) {
			Map.Entry e = (Map.Entry) o;
			java.lang.Object key = e.getKey();
			java.lang.Object value = e.getValue();

			System.out.println(e + " =========== " + key.getClass().getName() + " -- " + value.getClass().getName());

			if (e.getKey().getClass().getName().contains("OrientShutdownHook")) {
				toRemove.add(e.getKey());
			}

		}

		System.out.println(" removing :" + toRemove);
		boolean q = map.keySet().removeAll(toRemove);
		System.out.println(" removed :" + q + " -> " + map.keySet());

	}

	Map<Box, Vertex> insideSave = new LinkedHashMap<>();
	Map<Vertex, Box> insideLoad = new LinkedHashMap<>();

	public Set<Box> saveTopology(String name, Box root, Predicate<Box> save, Function<Box, String> alias) throws IOException {

		version = UUID.randomUUID();

		// version this, this seems like a common pattern. We could connect all nodes to a particular "version" node here as well.
		// export rafts do not contain versions ?

		Vertex topology = versionForward("topology", name, null); //TODO, we might have loaded an earlier topology
		topology.setProperty("__thisVersion", version.toString());

		List<Vertex> all = new ArrayList<>();
		Set<Box> complete = root.breadthFirstAll(root.allDownwardsFrom()).collect(Collectors.toSet());

		insideSave.clear();

		Function<Box, String> aalias = x -> {
			String q = alias.apply(x);
			if (q != null) return q;
			if (x == root) return ">>root<<";
			return null;
		};


		root.breadthFirstAll(root.allDownwardsFrom()).forEach(x -> {
			String a = aalias.apply(x);
			if (a == null && save.test(x) && !x.properties.isTrue(Boxes.dontSave, false)) {

				System.out.println(" -------------- check for dontsave at :" + x + " " + x.properties.isTrue(Boxes.dontSave, false));

				try {
					all.add(_saveBox(x, aalias, t -> save.test(t) && complete.contains(t), q -> {
						String uid = q.properties.getOrConstruct(IO.id);
						Vertex existingVertex = q.properties.get(_dbvertex);


						return versionForward("uid", uid, existingVertex);
					}));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		// shouldn't this be a set of bi directional edges?
		for (Vertex v : all) {
			topology.addEdge("contents", v);
		}

		Map<Dict.Prop, Object> q = root.properties.getMap();
		for (Map.Entry<Dict.Prop, Object> e : q.entrySet()) {
			System.out.println(" looking at property for root :" + e.getKey());
			if (!e.getKey().getName().equals("uid") && (IO.isPeristant(e.getKey()) || e.getKey().getName().equals("code"))) { // code is aliased in IO1
				System.out.println(" this property is persistent ");
				topology.setProperty(e.getKey().getName(), toValue(e.getKey(), e.getValue(), alias, save, null));
			}
		}

		//TODO: load this

		graph.commit();

		// todo, check existing things
		// todo topology itself properties per uid with lists of children should do it

		// per topology properties needed, rather than per box?
		// is "disconnected" per topology rather than box?
		Set<Box> ret = insideSave.keySet();


		return ret;
	}

	public Vertex createOnDemand(String className, String name) {
		Collection<Vertex> c = allOf(graph.getVertices(className, name));

		System.out.println(" looked for " + name + " / " + className + " got " + c);

		if (c.size() == 0)
			return graph.addVertex(null, className, name);

		return c.iterator().next();
	}

	private Vertex versionForward(String className, String name, Vertex existingVertex) {
		Vertex topology = null;
		if (existingVertex == null) {

			Collection<Vertex> c = allOf(graph.getVertices(className, name));

			System.out.println(" looked for " + name + " / " + className + " got " + c);

			if (c.size() == 0) {
				topology = graph.addVertex(null, className, name);
			} else {

				// version this? here
				topology = c.iterator().next();
				topology.setProperty(className, name + "@" + version);

				graph.commit();
				graph.begin();


				System.out.println(" renamed existing to :" + name + "@" + version);

				Vertex next = graph.addVertex(null, className, name);

				topology.addEdge("nextVersion", next);
				topology = next;

				// question: what do we do if this thing hasn't actually changed?
				// we could compute a hash as we are saving the box, and if it hasn't changed, we could rollback the transaction.

			}
		} else {
			((ODocument) ((OrientVertex) existingVertex).getRecord()).reload();

			topology = existingVertex;
			name = trimVersionFrom(name);

			topology.setProperty(className, name + "@" + version);

			System.out.println(" renamed existing to :" + name + "@" + version);

			Vertex next = graph.addVertex(null, className, name);

			topology.addEdge("nextVersion", next);
			topology = next;

		}
		return topology;
	}

	public Set<Box> loadTopology(String name, Box root, Function<String, Box> alias, Predicate<Vertex> load) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
		Collection<Vertex> c = allOf(graph.getVertices("topology", name));
		if (c.size() == 0)
			return null;

		Vertex topology = c.iterator().next();
		return loadTopology(topology, root, alias, load);
	}

	public Set<Box> loadTopology(Vertex topology, Box root, Function<String, Box> alias, Predicate<Vertex> load) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {

		Function<String, Box> aalias = x -> {
			Box q = alias.apply(x);
			if (q != null) return q;
			if (x.equals(">>root<<")) return root;
			return null;
		};


		Collection<Vertex> all = allOf(topology.getVertices(Direction.OUT, "contents"));

		insideLoad.clear();
		Set<Box> loaded = all.stream().map(x -> {
			try {
				return _loadBox(x, aalias, load);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return null;
		}).filter(x -> x != null).collect(Collectors.toSet());


		return loaded;
	}

	public Box loadBox(String uid, Function<String, Box> alias, Predicate<Vertex> load) {

		insideLoad.clear();
		try {
			return _loadBox(uid, alias, load);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			insideLoad.clear();
		}
		return null;
	}

	public Box loadBox(Vertex uid, Function<String, Box> alias, Predicate<Vertex> load) {

		insideLoad.clear();
		try {
			return _loadBox(uid, alias, load);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			insideLoad.clear();
		}
		return null;
	}


	public Box _loadBox(String uid, Function<String, Box> alias, Predicate<Vertex> load) throws IllegalAccessException, InstantiationException, IOException, ClassNotFoundException {
		Iterable<Vertex> vert = graph.getVertices("uid", uid);
		Collection<Vertex> v = allOf(vert);
		if (v.size() == 0) return null;

		Vertex vertex = v.iterator().next();

		return _loadBox(vertex, alias, load);
	}

	public Box _loadBox(Vertex vertex, Function<String, Box> alias, Predicate<Vertex> load) throws InstantiationException, IllegalAccessException, IOException, ClassNotFoundException {
		if (insideLoad.containsKey(vertex)) {
			return insideLoad.get(vertex);
		}
		if (!load.test(vertex)) return null;

		Set<String> keys = vertex.getPropertyKeys();
		Class<? extends Box> boxClass = Box.class;

		if (keys.contains("__class")) {
			try {
				Object c = fromValue(null, vertex.getProperty("__class"), alias, load);
				System.err.println(" trying to load :'" + c + "'");
				boxClass = (Class<? extends Box>) Thread.currentThread().getContextClassLoader().loadClass(""+c);
				System.err.println(" succeeded ");
			} catch (ClassNotFoundException e) {
				Log.log("error", () -> "can't find class to instantiate " + vertex.getProperty("__class") + " of type " + (vertex.getProperty("__class") == null ? null : vertex.getProperty("__class").getClass()) + " box, continuing on anyway");
			}
			// keys.remove("__class");
		}

		System.out.println(" about to instantiate '" + boxClass.getName() + "'");

		Box b = null;

		try {
			Class c = this.getClass()
				.getClassLoader()
				.loadClass("" + boxClass.getName());

			System.out.println(" constructing class " + c);
			try {
				Constructor<Box> cc = c.getDeclaredConstructor();
				System.out.println(" constructor " + cc);
				cc.setAccessible(true);
				b = cc.newInstance();
				b.properties.put(IO.desiredBoxClass, "" + boxClass);
				System.out.println(" got instance " + b);
			} catch (Throwable e) {
				Constructor<Box> cc = c.getDeclaredConstructor(Box.class);
				System.out.println(" constructor " + cc);
				cc.setAccessible(true);
				b = cc.newInstance(alias.apply(">>root<<"));
				b.properties.put(IO.desiredBoxClass, "" + boxClass);
				System.out.println(" got instance " + b);
			}
		} catch (Throwable e) {
			final Class<? extends Box> finalBoxClass = boxClass;
			Log.log("io.error", () -> " while looking for class <" + finalBoxClass + "> needed for an exception was thrown");
			Log.log("io.error", () -> e);
			Log.log("io.error", () -> " will proceed with just a vanilla Box class, but custom behavior will be lost ");
		}

		if (b == null)
			b = new Box();

		if (keys.contains("__class")) {
			b.properties.put(IO.desiredBoxClass, fromValue(IO.desiredBoxClass, vertex.getProperty("__class"), alias, load));
			keys.remove("__class");
		}

		insideLoad.put(vertex, b);

		for (String s : keys) {
			if (s.startsWith("__")) continue;

			Dict.Prop p = new Dict.Prop(s);
			Dict.Prop pc = p.findCannon();
			if (pc != null) p = pc;

			Object val = fromValue(p, vertex.getProperty(s), alias, load);
			if (val != null) {
				b.properties.put(p, val);
				p.getAttributes().put(IO.persistent, true);
			}
		}

		Collection<Object> children = allOf(((OrientVertex) vertex).getProperty("__children"));
		for (Object c : children) {
			if (c instanceof Vertex) {
				Box c2 = _loadBox((OrientVertex) c, alias, load);
				if (c2 != null) {
					System.out.println(" loaded child :" + c2 + " for parent " + b);
					b.connect(c2);
				}
			} else if (c instanceof String) {
				Box c2 = alias.apply((String) c);
				if (c2 != null) {
					b.connect(c2);
				}
			} else
				throw new IllegalArgumentException(" don't know what to make of child " + c + " / " + (c == null ? null : c.getClass()));
		}
		Collection<Object> parents = allOf(vertex.getProperty("__parents"));
		for (Object c : parents) {
			if (c instanceof Vertex) {
				Box c2 = _loadBox((OrientVertex) c, alias, load);
				if (c2 != null) {
					System.out.println(" loaded parent :" + c2 + " for child " + b);
					c2.connect(b);
				}
			} else if (c instanceof String) {
				Box c2 = alias.apply((String) c);
				if (c2 != null)
					c2.connect(b);
			} else
				throw new IllegalArgumentException(" don't know what to make of parent " + c + " / " + (c == null ? null : c.getClass()));
		}

		children = allOf(((OrientVertex) vertex).getProperty("__childrenAliased"));
		for (Object c : children) {
			if (c instanceof Vertex) {
				Box c2 = _loadBox((OrientVertex) c, alias, load);
				if (c2 != null) {
					System.out.println(" loaded child :" + c2 + " for parent " + b);
					b.connect(c2);
				}
			} else if (c instanceof String) {
				Box c2 = alias.apply((String) c);
				if (c2 != null) {
					b.connect(c2);
				}
			} else
				throw new IllegalArgumentException(" don't know what to make of child " + c + " / " + (c == null ? null : c.getClass()));
		}
		parents = allOf(vertex.getProperty("__parentsAliased"));
		for (Object c : parents) {
			if (c instanceof Vertex) {
				Box c2 = _loadBox((OrientVertex) c, alias, load);
				if (c2 != null) {
					System.out.println(" loaded parent :" + c2 + " for child " + b);
					c2.connect(b);
				}
			} else if (c instanceof String) {
				Box c2 = alias.apply((String) c);
				if (c2 != null)
					c2.connect(b);
			} else
				throw new IllegalArgumentException(" don't know what to make of parent " + c + " / " + (c == null ? null : c.getClass()));
		}


		b.properties.put(_dbvertex, vertex);
		b.properties.put(IO.id, trimVersionFrom(vertex.getProperty("uid")));

		return b;
	}

	private String trimVersionFrom(String uid) {

		if (uid.contains("@")) {
			return uid.split("@")[0];
		}
		return uid;
	}

	Pattern extractClass = Pattern.compile("%%(.*?)%%(.*)", Pattern.DOTALL);

	public <T> T fromValue(Dict.Prop<T> key, Object value, Function<String, Box> alias, Predicate<Vertex> load) throws InstantiationException, IllegalAccessException, IOException, ClassNotFoundException {
		if (value == null) return null;

		if (value instanceof String) {
			if (((String) value).startsWith("%%")) {
				Matcher m = extractClass.matcher(((String) value));
				m.find();
				String className = m.group(1);
				String content = m.group(2);

				return (T) fromValue(key, className, content, alias, load);
			}
		}

		if (value instanceof Vertex) {
			// consider this a little...
			return (T) _loadBox((String) ((Vertex) value).getProperty("uid"), alias, load);
		}


		System.out.println(" value is :" + value + " for " + key);
		return (T) value;
	}


	public <T> T fromValue(Dict.Prop<T> key, String className, String content, Function<String, Box> alias, Predicate<Vertex> load) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

		if (className.equals("java.lang.String")) return (T) content;
		if (className.equals("java.io.Serializable")) return (T) deserialize(content);
		if (className.equals("java.lang.Class"))
			return (T) Thread.currentThread().getContextClassLoader().loadClass(content);

		if (className.equals("vertex")) return (T) _loadBox(content, alias, load);

		return (T) content;
	}

	public Vertex saveBox(Box x, Function<Box, String> alias, Predicate<Box> save) throws IOException {
		insideSave.clear();
		try {
			Vertex v = _saveBox(x, alias, save, q -> {
				String uid = q.properties.getOrConstruct(IO.id);
				Vertex existingVertex = q.properties.get(_dbvertex);
				return versionForward("uid", uid, existingVertex);
			});
			return v;
		} finally {
			insideSave.clear();
		}
	}

	protected Vertex _saveBox(Box x, Function<Box, String> alias, Predicate<Box> save, Function<Box, Vertex> forward) throws IOException {
		System.out.println(" -------- check, again, for dontSave :" + x.properties.isTrue(Boxes.dontSave, false));
		if (x.properties.isTrue(Boxes.dontSave, false)) return null;

		if (insideSave.containsKey(x)) return insideSave.get(x);
		if (!save.test(x)) {
			Vertex existingVertex = x.properties.get(_dbvertex);
			if (existingVertex != null)
				((ODocument) ((OrientVertex) existingVertex).getRecord()).reload();
			return existingVertex;
		}
		if (alias.apply(x) != null)
			throw new IllegalArgumentException(" can't save a box called " + alias.apply(x) + " / " + x);

		String uid = x.properties.getOrConstruct(IO.id);

		System.out.println(" getting a vertex for " + uid);

		Vertex at = forward.apply(x);
		insideSave.put(x, at);

		Map<Dict.Prop, Object> q = x.properties.getMap();
		for (Map.Entry<Dict.Prop, Object> e : q.entrySet()) {
			System.out.println(" looking at property :" + e.getKey());
			if (!e.getKey().getName().equals("uid") && (IO.isPeristant(e.getKey()) || e.getKey().getName().equals("code"))) { // code is aliased in IO1
				System.out.println(" this property is persistent ");
				at.setProperty(e.getKey().getName(), toValue(e.getKey(), e.getValue(), alias, save, forward));

				String p = e.getKey().getAttributes().get(_dbparent);
				if (p != null && p.length() > 0) {

					Vertex v = createOnDemand("known", p);
					v.setProperty("uid", p);
					at.addEdge(e.getKey().getName(), v);

				}
			}
		}

		x.properties.put(_dbvertex, at);

		if (x.properties.get(IO.desiredBoxClass) != null)
			at.setProperty("__class", x.properties.get(IO.desiredBoxClass));
		else
			at.setProperty("__class", x.getClass().getName());

		// disconnected?

		((OrientVertex) at).setProperty("__children", x.children().stream().filter(z -> save.test(z)).map(z -> {
			try {
				String name = alias.apply(z);
				if (name == null)
					return _saveBox(z, alias, save, forward);
				else return null;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}).filter(z -> z != null).map(z -> {
			if (z instanceof OrientVertex) return ((OrientVertex) z).getRecord();
			return z;
		}).collect(Collectors.toList()), OType.LINKLIST);

		((OrientVertex) at).setProperty("__childrenAliased", x.children().stream().filter(z -> save.test(z)).map(z -> {
			String name = alias.apply(z);
			if (name != null) return name;
			return null;
		}).filter(z -> z != null).collect(Collectors.toList()), OType.EMBEDDEDLIST);

		((OrientVertex) at).setProperty("__parents", x.parents().stream().filter(z -> save.test(z)).map(z -> {
			try {
				String name = alias.apply(z);
				if (name != null) return null;
				return _saveBox(z, alias, save, forward);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}).filter(z -> z != null).map(z -> {
			if (z instanceof OrientVertex) return ((OrientVertex) z).getRecord();
			return z;
		}).collect(Collectors.toList()), OType.LINKLIST);

		((OrientVertex) at).setProperty("__parentsAliased", x.parents().stream().filter(z -> save.test(z)).map(z -> {
			String name = alias.apply(z);
			if (name != null) return name;
			return null;
		}).filter(z -> z != null).collect(Collectors.toList()), OType.EMBEDDEDLIST);

		return at;
	}

	private Object toValue(Dict.Prop key, Object value, Function<Box, String> alias, Predicate<Box> save, Function<Box, Vertex> forward) throws IOException {

		if (value == null) return null;
		if (value instanceof String) return "%%java.lang.String%%" + value;
		if (value instanceof Box)
			return _saveBox((Box) value, alias, save, forward);

		if (value instanceof Serializable) return "%%java.io.Serializable%%" + serialize((Serializable) value);
		if (value instanceof Class) return "%%java.lang.Class%%" + ((Class) value).getName();

		if (value instanceof Vertex) return "%%vertex%%" + ((Vertex) value).getProperty("uid");

		// error
		return "" + value;
	}

	private String serialize(Serializable value) throws IOException {
		ByteArrayOutputStream a = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(a)) {
			oos.writeObject(value);
			return Base64.getEncoder().encodeToString(a.toByteArray());
		}
	}

	static private Object deserialize(String value) throws IOException, ClassNotFoundException {
		byte[] a = Base64.getDecoder().decode(value);
		ByteArrayInputStream b = new ByteArrayInputStream(a);
		ObjectInputStream oos = new ObjectInputStream(b);
		try {
			return oos.readObject();
		} finally {
			oos.close();
		}
	}


	private <T> Collection<T> allOf(Iterable<T> vert) {

		List<T> tt = new ArrayList<>();
		for (T t : vert)
			tt.add(t);

		return tt;
	}


	public void advanceBox(Box loaded, Function<Box, String> alias) throws IOException {
		Vertex v = loaded.properties.get(_dbvertex);
		if (v == null) return;

		if (v != null)
			((ODocument) ((OrientVertex) v).getRecord()).reload();

		// 'v' as it is, make a new vertex with, crucially, an independent ID

		UUID newID = UUID.randomUUID();

		_saveBox(loaded, alias, x -> x == loaded, x -> {
			Vertex next = graph.addVertex(null, "uid", newID.toString());
			graph.commit();

			v.addEdge("copied", next);
			graph.begin();

			System.out.println(" moving forward from :" + x + " to " + next);
			return next;
		});

		loaded.properties.put(IO.id, newID.toString());

	}
}
