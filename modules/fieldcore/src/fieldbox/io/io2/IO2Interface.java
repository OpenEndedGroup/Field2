package fieldbox.io.io2;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import field.utility.Pair;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.io.IO;
import fieldbox.io.IO2;
import fielded.Commands;
import fielded.RemoteEditor;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
			m.put(new Pair<>("Copy by tag", "Searches the repository for boxes with `_.tags='...'` and copies one of them into this sheet"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					this.p = prompt;
				}

				@Override
				public void run() {

					LinkedHashMap<String, Integer> tags = extractAllTags();

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

	public LinkedHashMap<String, Integer> extractAllTags() {
		Collection<Vertex> allWithTags = q.known("__tagged__", "tags");
		allWithTags = q.currentOnly(allWithTags);

		LinkedHashMap<String, Integer> tags = new LinkedHashMap<String, Integer>();
		for (Vertex v : allWithTags) {
			try {
				String prop = (String) q.propertyFor("tags", v);
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
					try {
						String prop = (String) q.propertyFor("tags", v);
						if (prop.contains(",") || prop.contains(" ")) {
							String[] pieces = prop.split("[, ]+");
							for (String p : pieces) {
								if (p.trim().equals(key))
									found.add(v);
							}
						} else if (prop.trim().equals(key))
							found.add(v);

					} catch (ClassCastException e) {
					}
				}

				if (found.size() > 0) {
					Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
					for (Vertex f : found) {
						m.put(new Pair<String, String>("" + q.propertyFor(Box.name, f), informationForBoxVertex(f)), () -> copyIntoCurrent(f));
					}

					p.prompt("Known boxes", m, null);

				}
			}

			@Override
			public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
				this.p = prompt;
			}
		};
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


	private void copyIntoCurrent(Vertex f) {
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

			Box loaded = on.loadBox(f, standardAlias, x -> true);

			List<Box> allLoaded = loaded.breadthFirstAll(loaded.downwards()).collect(Collectors.toList());

			// what if this box is already part of this topology?
			for (Box b : allLoaded) on.advanceBox(b, backwardsAlias);

			if (loaded.parents().size() == 0) {
				System.out.println(" warning: box seems to not be connected to anything at all");
				root.connect(loaded);
			}

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
		}
	}

	private String informationForBoxVertex(Vertex f) {
		int m = (q.propertyFor("code", f) + "").length();
		return "contains " + m + " char" + (m == 1 ? "" : "s") + " of code";
	}

}
