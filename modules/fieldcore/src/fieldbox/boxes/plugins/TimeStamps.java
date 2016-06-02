package fieldbox.boxes.plugins;

import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldbox.boxes.Callbacks;
import fieldbox.io.IO;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Collects data about what is executed, when. Should also do what's executed at startup. For that, we need to save properties that are persisant on at least Root.
 */
public class TimeStamps extends Box {

	static public Dict.Prop<List<Instant>> historyExecuted = new Dict.Prop<>("historyExecuted").type().autoConstructs(() -> new ArrayList<>()).doc("a log of `Instant`s for whenever this box has been executed").set(IO.persistent, true);
	static public Dict.Prop<List<Instant>> historyEdited = new Dict.Prop<>("historyEdited").type().autoConstructs(() -> new ArrayList<>()).doc("a log of `Instant`s for whenever this box has been edited").set(IO.persistent, true);

	public TimeStamps(Box root) {
		this.properties.putToMap(Callbacks.onExecute, "__timestamps__", (FunctionOfBox<Boolean>) (x -> onExecute(x)));
		this.properties.putToMap(Callbacks.onEdit, "__timestamps__", (FunctionOfBox<Boolean>) (x -> onEdit(x)));
	}

	protected boolean onExecute(Box what) {
		what.properties.putToList(historyExecuted, Instant.now());
		return true;
	}

	protected boolean onEdit(Box what) {
		what.properties.putToList(historyEdited, Instant.now());
		return true;
	}

	public Instant mostRecent(Box from, Dict.Prop<List<Instant>> of) {
		Instant[] best = {null};
		from.breadthFirst(from.downwards()).forEach(x -> {
			List<Instant> h = from.properties.get(of);
			if (h != null && h.size() > 0) {
				if (best[0] == null || best[0].isBefore(h.get(h.size() - 1)))
					best[0] = h.get(h.size() - 1);
			}
		});
		return best[0];
	}

	// need to compute a sensible scale over this to do a heatmap
	public Map<Box, Double> heatMapOver(Box from, double seconds) {
		Instant mostRecent = mostRecent(from, historyExecuted);
		if (mostRecent==null) return Collections.emptyMap();

		LinkedHashMap<Box, Double> r = new LinkedHashMap<>();
		double norm = from.breadthFirst(from.downwards()).map(x -> {
			double d = heat(x, mostRecent, seconds);
			r.put(x, d);
			return d;
		}).reduce((a, b) -> a + b).get();

		LinkedHashMap<Box, Double> r2 = new LinkedHashMap<>();
		r.entrySet().stream().forEach(x -> r2.put(x.getKey(), x.getValue() / norm));

		return r2;
	}

	private double heat(Box from, Instant start, double scale) {
		List<Instant> ex = from.properties.get(historyExecuted);
		if (ex==null || ex.size()==0) return 0;

		return ex.stream().map(x -> Math.abs(Duration.between(x, start).getSeconds())).map(x -> Math.exp(-x/scale)).reduce((a,b) -> a+b).get();
	}


}
