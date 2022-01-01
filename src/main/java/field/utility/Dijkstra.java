package field.utility;

import java.util.*;
import java.util.function.Function;

/**
 * Created by marc on 3/26/14.
 */
public class Dijkstra<t_vertex, t_edge> {

	private final Function<t_edge, Number> length;
	private final Function<t_edge, t_vertex> output;
	private final Function<t_vertex, Collection<t_edge>> edgesFor;

	public Dijkstra(Function<t_edge, Number> length, Function<t_edge, t_vertex> output, Function<t_vertex, Collection<t_edge>> edgesFor) {

		this.length = length;
		this.output = output;
		this.edgesFor = edgesFor;
	}

	Map<t_vertex, Float> minDistance = new HashMap<>();
	Map<t_vertex, Pair<t_vertex, t_edge>> previous = new HashMap<>();

	public void computePaths(t_vertex source) {
		minDistance.put(source, 0f);
		PriorityQueue<t_vertex> vertexQueue = new PriorityQueue<>((o1, o2) -> Float.compare(minDistance.computeIfAbsent(o1, (k) -> Float.POSITIVE_INFINITY), minDistance.computeIfAbsent(o2, (k) -> Float.POSITIVE_INFINITY)));

		vertexQueue.add(source);

		while (!vertexQueue.isEmpty()) {
			t_vertex u = vertexQueue.poll();
			for (t_edge e : edgesFor.apply(u)) {
				t_vertex v = output.apply(e);
				double weight = length.apply(e).doubleValue();
				double min = minDistance.computeIfAbsent(u, (k) -> Float.POSITIVE_INFINITY);
				double distanceThroughU = min + weight;
				if (distanceThroughU < minDistance.computeIfAbsent(v, (k) -> Float.POSITIVE_INFINITY)) {
					vertexQueue.remove(v);
					minDistance.put(v, (float) distanceThroughU);
					previous.put(v, new Pair<>(u, e));
					vertexQueue.add(v);
				}
			}
		}
	}

	public List<Pair<t_vertex, t_edge>> getShortestPathTo(t_vertex target) {
		if (!previous.containsKey(target)) return null;

		List<Pair<t_vertex, t_edge>> path = new ArrayList<Pair<t_vertex, t_edge>>();
		for (t_vertex vertex = target; vertex != null; ) {
			Pair<t_vertex, t_edge> p = previous.get(vertex);
			if (p != null) {
				path.add(p);
				vertex = p.first;
			} else {
				break;
			}
		}
		Collections.reverse(path);
		return path;
	}
}
