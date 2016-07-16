package fieldbox.io.io2;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldbox.io.IO2;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * useful ways of queries the database of boxes
 * <p>
 * ideas:
 * <p>
 * search for tagged things or things that are children of "known" that aren't a previous version
 * <p>
 * now for templates? are they just a parent saved by a name? Is that a topology?
 * <p>
 * include a save timestamp
 */
public class Queries {

	private final IO2 on;

	public Queries(IO2 on) {
		this.on = on;
	}

	// if _dbvertex is null, you need to save this box first
	public Vertex v(Box of) {
		return of.properties.get(IO2._dbvertex);
	}

	public List<Vertex> allTopologies() {
		Iterable<Vertex> m = on.graph.query().has("topology").vertices();
		if (m == null) return null;
		Iterator<Vertex> mi = m.iterator();
		if (!mi.hasNext()) return null;

		List<Vertex> a = new ArrayList<>();
		while (mi.hasNext())
			a.add(mi.next());

		return a;
	}

	public Vertex t(String name) {
		Iterable<Vertex> m = on.graph.getVertices("topology", name);
		if (m == null) return null;
		Iterator<Vertex> mi = m.iterator();
		if (!mi.hasNext()) return null;

		return mi.next();
	}

	public Collection<Vertex> contentsOfTopology(Vertex v) {
		return get(v.getEdges(Direction.OUT, "contents"), e -> e.getVertex(Direction.IN), x -> true);
	}

	public Set<Vertex> currentOnly(Collection<Vertex> v) {
		Set<Vertex> l = new LinkedHashSet<>();
		for (Vertex vv : v) {
			String u = vv.getProperty("topology");
			if (u!=null)
			{
				if (!u.contains("@")) l.add(vv);
			}
			else {
				u = "" + vv.getProperty("uid");
				if (!u.contains("@")) l.add(vv);
			}
		}
		return l;
	}

	public List<Vertex> known(String dbparent, String edgeName) {

		List<Vertex> roots = getVertices("known", dbparent);
		List<Vertex> all = new ArrayList<>();

		for (Vertex vv : roots) {
			all.addAll(get(vv.getEdges(Direction.IN, edgeName), e -> e.getVertex(Direction.OUT), x -> true));
		}
		return all;
	}

	protected List<Vertex> get(Iterable<Edge> e, Function<Edge, Vertex> f, Predicate<Vertex> a) {
		List<Vertex> vv = new ArrayList<>();
		for (Edge ee : e) {
			Vertex v = f.apply(ee);
			if (v != null && a.test(v))
				vv.add(v);
		}
		return vv;
	}

	public LinkedHashMap<String, Integer> extractAllTags() {
		Collection<Vertex> allWithTags = known("__tagged__", "tags");
		allWithTags = currentOnly(allWithTags);

		LinkedHashMap<String, Integer> tags = new LinkedHashMap<String, Integer>();
		for (Vertex v : allWithTags) {
			try {
				String prop = (String) propertyFor("tags", v);
				if (prop.contains(",") || prop.contains(" ")) {
					String[] pieces = prop.split("[, ]+");
					for (String p : pieces) {
						if (p.trim().length() > 0)
							tags.compute(p.trim(), (k, val) -> (val == null ? 1 : (val + 1)));
					}
				} else if (prop.trim().length() > 0)
					tags.compute(prop.trim(), (k, val) -> (val == null ? 1 : (val + 1)));

			} catch (ClassCastException e) {
			}
		}
		return tags;
	}

	public List<Vertex> getVertices(String classname, String name) {
		Iterable<Vertex> v = on.graph.getVertices(classname, name);
		if (v == null) return Collections.emptyList();

		Iterator<Vertex> vi = v.iterator();
		if (!vi.hasNext()) return Collections.emptyList();

		List<Vertex> vv = new ArrayList<>();
		while (vi.hasNext())
			vv.add(vi.next());

		return vv;
	}

	public Object propertyFor(Dict.Prop p, Vertex v) {
		try {
			return on.fromValue(p, v.getProperty(p.getName()), (x) -> null, (x) -> false);
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
	}

	public Object propertyFor(String p, Vertex v) {
		try {
			return on.fromValue(new Dict.Prop(p), v.getProperty(p), (x) -> null, (x) -> false);
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
	}

	public Collection<Vertex> previousVersions(Vertex of) {
		ArrayList<Vertex> v = new ArrayList<Vertex>();
		v.add(of);
		_previousVersions(of, v);
		return v;
	}

	private void _previousVersions(Vertex of, ArrayList<Vertex> v) {
		Iterable<Edge> e = of.getEdges(Direction.IN, "nextVersion");
		if (e != null) {
			Iterator<Edge> ee = e.iterator();
			if (ee.hasNext()) {
				Edge q = ee.next();
				Vertex next = q.getVertex(Direction.OUT);
				v.add(next);
				_previousVersions(next, v);
			}
		}

	}


}
