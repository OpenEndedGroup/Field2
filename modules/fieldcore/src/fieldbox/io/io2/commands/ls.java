package fieldbox.io.io2.commands;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import field.utility.Options;
import field.utility.Pair;
import fieldagent.Trampoline;
import fieldbox.io.IO2;
import fieldbox.io.io2.IO2Interface;
import fieldbox.io.io2.Queries;
import org.ocpsoft.prettytime.PrettyTime;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by marc on 7/11/16.
 */
public class ls implements Runnable {
	IO2 io = new IO2();
	Queries q = new Queries(io);

	@Override
	public void run() {
		System.out.println(" ============================================================================ ");

		if (Options.remainingArgs.length == 0)
			listRoot();
		else if (Options.remainingArgs.length == 1)
			listTopology(Options.remainingArgs[0]);
		else {
			System.err.println("Error: wrong number of arguments. Usage: 'ls' [name-of-field-document]");
			System.exit(1);
		}
		System.exit(0);
	}

	private void listRoot() {
		System.out.println(" ls * — all Field documents in your repository: ");
		System.out.println(" ============================================================================ ");

		Map<String, List<String>> versions = new LinkedHashMap<>();
		Map<String, List<Date>> dates = new LinkedHashMap<>();
		for (Vertex v : io.graph.query().has("topology").vertices()) {
			Object to = v.getProperty("topology");

			if (to != null && to instanceof String) {
				String name = "" + to;

				String base = name.contains("@") ? name.split("@")[0] : name;
				versions.computeIfAbsent(base, k -> new ArrayList<>()).add(name);

				Date lm = (Date) v.getProperty("lastModified");
				if (lm != null)
					dates.computeIfAbsent(base, k -> new ArrayList<>()).add(lm);
			}
		}

		LinkedHashMap<String, Integer> tags = q.extractAllTags();

		if (tags.size()==0)
			System.out.println("Repository contains no tagged boxes");
		else
		{
			String m = tags.entrySet().stream().map(x -> x.getKey() + "(" + x.getValue() + ")").reduce("", (a, b) -> a + (a.length()>0 ? ", " : "")+ b);
			System.out.println("Repository contains "+tags.size()+" tag"+(tags.size()==1 ? "" : "s")+", specifically :"+m);
		}

		for (String n : versions.keySet()) {
			System.out.println(n + "\n\t+" + versions.get(n).size() + " version" + (versions.get(n).size() == 1 ? "" : "s"));
			if (dates.get(n) != null) {
				if (dates.get(n).size() == 1)
					System.out.println("\t" + dates.get(n));
				else if (dates.get(n).size() > 1) {
					Collections.sort(dates.get(n));
					Date recently = dates.get(n).get(dates.get(n).size() - 1);
					Date first = dates.get(n).get(0);

					String s0 = new PrettyTime().formatDuration(recently);
					String s1 = new PrettyTime().formatDuration(first);

					if (s0.equals(s1))
						System.out.println("\tmodified around " + s0 + " ago");
					else
						System.out.println("\tmodified from " + s0 + " ago to " + s1 + " ago");
				}
			}

			List<String> q = versions.get(n);
			String found = null;
			for (String qq : q)
				if (!qq.contains("@")) {
					found = qq;
				}
			if (found == null) {
				System.out.println("\t(HEADLESS)");
			} else {
				Iterable<Vertex> v = io.graph.getVertices("topology", found);
				Vertex head = v.iterator().next();
				Iterable<Edge> e = head.getEdges(Direction.OUT, "contents");
				int count = 0;
				for (Edge eee : e)
					count++;
				System.out.println("\tcontains " + count + " box" + (count == 1 ? "" : "es"));
			}

		}
	}

	private void listTopology(String topology) {
		System.out.println(" ls " + topology + " — information about document '" + topology + "'");
		System.out.println(" ============================================================================ ");

		Map<String, List<String>> versions = new LinkedHashMap<>();
		Map<String, List<Date>> dates = new LinkedHashMap<>();
		List<Pair<Date, Vertex>> vertices = new ArrayList<>();

		for (Vertex v : io.graph.query().has("topology").vertices()) {
			Object to = v.getProperty("topology");

			if (to != null && to instanceof String && (to.equals(topology) || ((String) to).startsWith(topology + "@"))) {
				String name = "" + to;

				String base = name.contains("@") ? name.split("@")[0] : name;
				versions.computeIfAbsent(base, k -> new ArrayList<>()).add(name);

				Date lm = v.getProperty("lastModified");
				if (lm != null)
					dates.computeIfAbsent(base, k -> new ArrayList<>()).add(lm);

				vertices.add(new Pair<>(lm, v));
			}
		}

		if (versions.size() == 0) {
			System.err.println(" document '" + topology + "' not found in repository");
			System.exit(1);
		}

		Collections.sort(vertices, (a, b) -> {
			if (a.first == null && b.first == null) return 0;
			if (a.first == null && b.first != null) return 1;
			if (b.first == null) return -1;
			return -a.first.compareTo(b.first);
		});

		for (String n : versions.keySet()) {
			System.out.println(n + "\n\t+" + versions.get(n).size() + " version" + (versions.get(n).size() == 1 ? "" : "s"));
			if (dates.get(n) != null) {
				if (dates.get(n).size() == 1)
					System.out.println("\t" + dates.get(n));
				else if (dates.get(n).size() > 1) {
					Collections.sort(dates.get(n));
					Date recently = dates.get(n).get(dates.get(n).size() - 1);
					Date first = dates.get(n).get(0);

					String s0 = new PrettyTime().formatDuration(recently);
					String s1 = new PrettyTime().formatDuration(first);

					if (s0.equals(s1))
						System.out.println("\tmodified around " + s0 + " ago");
					else
						System.out.println("\tmodified from " + s0 + " ago to " + s1 + " ago");

				}
			}

			List<String> q = versions.get(n);
			String found = null;
			for (String qq : q)
				if (!qq.contains("@")) {
					found = qq;
				}
			if (found == null) {
				System.out.println("\t(HEADLESS)");
			} else {
				Iterable<Vertex> v = io.graph.getVertices("topology", found);
				Vertex head = v.iterator().next();
				Iterable<Edge> e = head.getEdges(Direction.OUT, "contents");
				int count = 0;
				for (Edge eee : e)
					count++;
				System.out.println("\tcontains " + count + " box" + (count == 1 ? "" : "es"));
			}
		}

		boolean first = true;
		for (Pair<Date, Vertex> vv : vertices) {
			System.out.println("\tVersion at " + (vv.first == null ? "(unknown)" : new PrettyTime().format(vv.first)));
			Iterable<Edge> e = vv.second.getEdges(Direction.OUT, "contents");
			int count = 0;
			List<Vertex> boxes = new ArrayList<>();
			for (Edge eee : e) {
				count++;
				Vertex box = eee.getVertex(Direction.IN);
				boxes.add(box);
			}
			if (!first)
				System.out.println("\t\tcontains " + count + " box" + (count == 1 ? "" : "es"));
			for (Vertex bb : boxes) {
				System.out.println("\t\t\t" + q.propertyFor("name", bb) + " - " + filter(bb, bb.getPropertyKeys()));
			}
			System.out.println("\n");
			if (first)
				first = false;

			break;
		}

	}

	static public final Set<String> exclude = new LinkedHashSet(Arrays.asList("uid", "lastModified"));

	private String filter(Vertex v, Set<String> propertyKeys) {
		return "" + propertyKeys.stream().filter(x -> !x.startsWith("_")).filter(x -> !exclude.contains(x)).map(x -> {
			Object q = v.getProperty(x);
			if (q instanceof String) {
				q = this.q.propertyFor(x, v) + "";

				if (((String) q).length() > 50)
					return x + "(" + ((String) q).length() + "b)";
				else
					return x + "('" + q + "')";


			} else {
				return "" + x;
			}
		}).collect(Collectors.toSet());
	}
}
