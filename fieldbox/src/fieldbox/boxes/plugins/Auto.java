package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Log;
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
									     .doc("set to non-zero integer to make this box automatically _.begin() when it is loaded; the integer gives the order: all boxes marked '1' are run before boxes marked '2'. In addition boxes are executed from top to bottom ");

	static {
		IO.persist(auto);
	}

	private final Map<Box, Long> done;


	public Auto(Box root) {

		root.properties.put(auto, 0); // makes property appear in autocomplete for everyone

		System.out.println(" atuo is starting up ");

		done = new HashMap<>();

		properties.putToMap(Callbacks.onLoad, "__autoload__", (b) -> {
			// we only auto things once.
			loaded();
			return null;
		});


	}

	@Override
	public void loaded() {


		System.out.println(" atuo is loading up ["+System.identityHashCode(this)+"]");
		System.out.println(" previously run is :"+done);

		Log.log("auto", "loaded called");

		List<Triple<Box, Float, Number>> run = breadthFirst(both()).filter(x -> x.properties.has(auto))
									    .map(x -> {

										    Log.log("auto", "filtered by having auto :" + x);
										    return x;
									    })
									    .filter(x -> !done.containsKey(x))
									    .filter(x -> x.properties.getFloat(auto, 0) > 0)
									    .map(x -> new Triple<>(x, x.properties.get(Box.frame).y, x.properties.get(auto)))
									    .collect(Collectors.toList());

		// mark everything as done right now, in case there are exceptions.
		run.forEach(x -> done.put(x.first, System.currentTimeMillis()));

		System.out.println(" loading :"+run);
		System.out.println(" next previous is :"+done);

		Collections.sort(run, (a, b) -> {
			if (a.third.doubleValue() < b.third.doubleValue()) return 1;
			if (a.third.doubleValue() > b.third.doubleValue()) return -1;

			if (a.second.doubleValue() < b.second.doubleValue()) return 1;
			if (a.second.doubleValue() > b.second.doubleValue()) return -1;

			return 0;
		});

		for (Triple<Box, Float, Number> t : run) {
			System.out.println(" auto load is running :" + t);
			t.first.find(Chorder.begin, both())
			       .findFirst()
			       .get()
			       .apply(t.first);
		}
	}
}
