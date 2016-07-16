package fieldbox.io.io2;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientElementIterable;
import field.utility.Pair;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.io.IO;
import fieldbox.io.IO2;
import fielded.Commands;
import fielded.RemoteEditor;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fieldbox.io.IO2.allOf;

/**
 * Plugin to give access to all the fun things that IO2 makes possible
 */
public class IO2Interface extends Box {

	private final IO2 on;
	private final Queries q;
	private final Box root;

	public interface QueryChain {
		public List<QueryChain> run();

		public Pair<String, String> describe();
	}

	public IO2Interface(Box root) {
		this.root = root;
		on = FieldBox.fieldBox.io2;
		q = new Queries(on);

		properties.put(Commands.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			m.put(new Pair<>("Copy by tag", "Searches the repository for boxes with `_.tags='...'` and copies one of them, and any direct descendants, into this sheet"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					this.p = prompt;
				}

				@Override
				public void run() {

					LinkedHashMap<String, Integer> tags = q.extractAllTags();

					Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					for (Map.Entry<String, Integer> entry : tags.entrySet()) {
						m.put(new Pair<>(entry.getKey(), "used " + entry.getValue() + " time" + ((entry.getValue() == 1) ? "" : "s")), runForTagCopy(entry.getKey()));
					}

					p.prompt("Known tags", m, null);
				}
			});
			m.put(new Pair<>("Copy document", "takes a whole document, copies it, and inserts it into this one"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					this.p = prompt;
				}

				@Override
				public void run() {

					Collection<Vertex> tags = listAllTopologies();

					Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					for (Vertex entry : tags) {
						m.put(new Pair<>("" + entry.getProperty("topology"), informationForTopologyVertex(entry)), () -> topologyCopy(entry));
					}

					p.prompt("Known documents", m, null);
				}
			});

			return m;
		});
	}

	private String informationForTopologyVertex(Vertex entry) {
		Iterator<Edge> q = entry.getEdges(Direction.OUT, "contents").iterator();
		int c = 0;
		while (q.hasNext()) {
			q.next();
			c++;
		}
		return "document with " + c + " box" + (c == 1 ? "" : "es");
	}


	public Collection<Vertex> listAllTopologies() {
		Collection<Vertex> allWithTags = q.allTopologies();
		allWithTags = q.currentOnly(allWithTags);
		return allWithTags;
	}

	private RemoteEditor.ExtendedCommand runForTagCopy(String key) {
		return new RemoteEditor.ExtendedCommand() {

			public RemoteEditor.SupportsPrompt p;

			@Override
			public void run() {
				Collection<Vertex> allWithTags = q.known("__tagged__", "tags");
				allWithTags = q.currentOnly(allWithTags);

				Set<Vertex> found = new LinkedHashSet<>();

				for (Vertex v : allWithTags) {
					if (hasTag(v, key)) {
						found.add(v);
					}
				}

				if (found.size() > 0) {
					Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
					for (Vertex f : found) {
						m.put(new Pair<String, String>("" + q.propertyFor(Box.name, f) + "<span class=smaller-inframe>|" + f+"</span>", informationForBoxVertex(f)), () -> copyIntoCurrent(f, x -> {


							if (hasTag(x, key))
								return true;

							Object c2 = q.propertyFor("__parents", x);

							if (c2 instanceof OrientElementIterable) {
								OrientElementIterable<Vertex> v = ((OrientElementIterable) c2);
								for (Vertex vv : v) {
									if (hasTag(vv, key))
										return true;
								}
							}

							return false;
						}));
					}

					ArrayList<Pair<String, String>> k = new ArrayList<>(m.keySet());
					Collections.sort(k, (a, b) -> {
						if (a.first.contains("original template") && !b.first.contains("original template"))
							return -1;
						if (b.first.contains("original template"))
							return 1;

						return a.first.compareTo(b.first);
					});

					Map<Pair<String, String>, Runnable> m2 = new LinkedHashMap<>();
					for (Pair<String, String> kk : k)
						m2.put(kk, m.get(kk));


					p.prompt("Known boxes", m2, null);
				}
			}

			@Override
			public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
				this.p = prompt;
			}
		};
	}

	public boolean hasTag(Vertex v, String key) {
		try {
			String prop = (String) q.propertyFor("tags", v);
			if (prop == null) return false;
			if (prop.contains(",") || prop.contains(" ")) {
				String[] pieces = prop.split("[, ]+");
				for (String p : pieces) {
					if (p.trim().equals(key))
						return true;
				}
			} else if (prop.trim().equals(key))
				return true;

		} catch (ClassCastException e) {
		}
		return false;
	}

	private void topologyCopy(Vertex f) {
		Function<String, Box> standardAlias = name -> {
			if (name.equals(">>root<<"))
				return root;
			return null;
		};
		Function<Box, String> backwardsAlias = name -> {
			if (name == root)
				return ">>root<<";
			return null;
		};
		try {

			Set<Box> allLoaded = on.loadTopology(f, root, standardAlias, x -> true);

			// what if this box is already part of this topology?
			for (Box b : allLoaded) on.advanceBox(b, backwardsAlias);

			Set<Box> toRemove = new LinkedHashSet<>();
			for (Box qq : allLoaded) {
				if (qq instanceof IO.Loaded) {
					System.out.println("        " + qq);
					try {
						((IO.Loaded) qq).loaded();
					} catch (Throwable t) {
						t.printStackTrace();
						toRemove.add(qq);
					}
				}
			}
			for (Box b : toRemove) {
				b.disconnectFromAll();
			}


		} catch (IOException e) {
			e.printStackTrace(); // TODO: report trouble
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	/**
	 * this will copy the whole heirarchy into the document
	 */
	private void copyIntoCurrent(Vertex f) {
		copyIntoCurrent(f, x -> true);
	}

	/**
	 * this will copy the whole heirarchy into the document
	 */
	private void copyIntoCurrent(Vertex f, Predicate<Vertex> vv) {
		Function<String, Box> standardAlias = name -> {
			if (name.equals(">>root<<"))
				return root;
			return null;
		};
		Function<Box, String> backwardsAlias = name -> {
			if (name == root)
				return ">>root<<";
			return null;
		};
		try {

			Box loaded = on.loadBox(f, standardAlias, vv);

			List<Box> allLoaded = loaded.breadthFirstAll(loaded.downwards()).collect(Collectors.toList());

			// what if this box is already part of this topology?
			for (Box b : allLoaded) on.advanceBox(b, backwardsAlias);

			if (loaded.parents().size() == 0) {
				System.err.println(" WARNING: box seems to not be connected to anything at all");
				root.connect(loaded);
			}

			Set<Box> toRemove = new LinkedHashSet<>();
			for (Box qq : allLoaded) {

				// remove this, so that these things don't keep claiming to be HEAD?
//				qq.properties.remove(IO2.tags);

				if (qq instanceof IO.Loaded) {
					System.out.println("        " + qq);
					try {
						((IO.Loaded) qq).loaded();
					} catch (Throwable t) {
						t.printStackTrace();
						toRemove.add(qq);
					}
				}
			}

			for (Box b : toRemove) {
				b.disconnectFromAll();
			}


		} catch (IOException e) {
			e.printStackTrace(); // TODO: report trouble
		}
	}

	private String informationForBoxVertex(Vertex f) {
		int m = (q.propertyFor("code", f) + "").length();
		String c1 = "";

		try {
			Collection<Vertex> previous = q.previousVersions(f);

			int count = 0;
			for (Vertex p : previous) {
				Iterable<Edge> e = p.getEdges(Direction.IN, "copied");
				for (Edge ee : e) {
					count++;
				}

				System.out.println(" in count = " + count);
			}
			if (count == 0) {
				c1 += "This is the <b>original template</b>.<br>";
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		c1+=(m > 10 ? "contains " + m + " char" + (m == 1 ? "" : "s") + " of code. " : "");

		try {
			Object lm = on.fromValue(null, f.getProperty("lastModified"), n -> null, n -> true);

			if (lm != null) {
				System.out.println(" last mod is " + lm + " " + lm.getClass());
				String desc = new PrettyTime().formatDuration((Date) lm);
				if (desc.trim().length() == 0) desc = "now";
				else
					desc = desc + " ago";
				c1 += "Last modified " + desc + ". ";
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		try {

			Iterable<Edge> o = f.getEdges(Direction.IN, "contents");
			String top = null;
			for (Edge ee : o) {
				System.out.println(" contents ? " + ee + " " + ee.getVertex(Direction.OUT) + " " + ee.getVertex(Direction.IN) + " " + ee.getVertex(Direction.OUT).getProperty("topology"));

				top = ee.getVertex(Direction.OUT).getProperty("topology");
				break;
			}

			if (top != null) {
				if (top.contains("@")) {
					top = top.split("@")[0];
					c1 += "Was used in "+top+". ";
				}
				else
					c1 += "Used in " + top + ". ";
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}




		try {
			Collection c = (Collection) allOf(f.getProperty("__children"));
			if (c != null && c.size() > 0) {
				c1 += "Has " + c.size() + " " + (c.size() == 1 ? "child. " : "children. ");

			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		System.out.println("c1 :" + c1);
		return c1;
	}

}
