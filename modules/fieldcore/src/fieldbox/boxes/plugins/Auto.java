package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.utility.Dict;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.boxes.Callbacks;
import fieldbox.io.IO;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Auto extends Box implements IO.Loaded {

	static public final Dict.Prop<Number> auto = new Dict.Prop<>("auto").type()
									     .toCannon()
									     .doc("set to non-zero integer to make this box automatically `_.begin()` when it is loaded; the integer gives the order: all boxes marked '1' are run before boxes marked '2'. In addition boxes are executed from top to bottom ");

	static {
		IO.persist(auto);
	}

	private final Map<Box, Long> done;


	public Auto(Box root) {

		root.properties.put(auto, 0); // makes property appear in autocomplete for everyone

		done = new HashMap<>();

		properties.putToMap(Callbacks.onLoad, "__autoload__", (b) -> {
			// we only auto things once.
			loaded();
			return null;
		});


	}

	@Override
	public void loaded() {

		List<Triple<Box, Float, Number>> run = breadthFirst(both()).filter(x -> x.properties.has(auto))
									    .filter(x -> !done.containsKey(x))
									    .filter(x -> x.properties.getFloat(auto, 0) > 0)
									    .map(x -> new Triple<>(x, x.properties.get(Box.frame).y, x.properties.get(auto)))
									    .collect(Collectors.toList());

		// mark everything as done right now, in case there are exceptions.
		run.forEach(x -> done.put(x.first, System.currentTimeMillis()));

		Collections.sort(run, (a, b) -> {
			if (a.third.doubleValue() < b.third.doubleValue()) return 1;
			if (a.third.doubleValue() > b.third.doubleValue()) return -1;

			if (a.second.doubleValue() < b.second.doubleValue()) return 1;
			if (a.second.doubleValue() > b.second.doubleValue()) return -1;

			return 0;
		});

		for (Triple<Box, Float, Number> t : run) {

			RunLoop.main.delay(() -> {
				t.first.find(Chorder.begin, both())
				       .findFirst().map( x -> x.apply(t.first));

			}, 1000);

		}
	}
}
