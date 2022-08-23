package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.utility.Dict;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.boxes.Callbacks;
import fieldbox.execution.Execution;
import fieldbox.io.IO;

import java.util.*;
import java.util.stream.Collectors;

import static fieldbox.boxes.plugins.Preamble.preamble;

public class Auto extends Box implements IO.Loaded {

    static public final Dict.Prop<Number> auto = new Dict.Prop<>("auto").type()
            .toCanon()
            .doc("set to non-zero integer to make this box automatically `_.begin()` when it is loaded; the integer gives the order: all boxes marked '1' are run before boxes marked '2'. In addition boxes are executed from top to bottom ");

    static {
        IO.persist(auto);
    }

    private final Map<Box, Long> done;
    private final Map<Box, Long> preambleDone;


    public Auto(Box root) {

        root.properties.put(auto, 0); // makes property appear in autocomplete for everyone

        done = new HashMap<>();
        preambleDone = new HashMap<>();

        properties.putToMap(Callbacks.onLoad, "__autoload__", (b) -> {
            // we only auto things once.
            loaded();
            return null;
        });
    }

    @Override
    public void loaded() {

        Set<Triple<Box, Float, String>> run0 = breadthFirst(both()).filter(x -> x.properties.has(preamble))
                .filter(x -> !preambleDone.containsKey(x))
                .map(x -> new Triple<>(x, x.properties.get(Box.frame).y, x.properties.get(preamble)))
                .filter(x -> x.third.trim().length() > 0)
                .collect(Collectors.toSet());


        System.out.println(" -- preamble to execute is " + run0);

        int offset = 1;

        run0.forEach(t -> {
            preambleDone.put(t.first, System.currentTimeMillis());
        });

        for (Triple<Box, Float, String> t : run0) {

            RunLoop.main.delayTicks(() -> {

                System.out.println(" -- preamble for " + t.first + " is automatically executing on startup");
                // just need to execute "", since the preamble will be automatically prepended
                t.first.find(Execution.execution, t.first.upwards()).findFirst().get().support(t.first, Execution.code).executeTextFragment("", t.first);

            }, (offset++));
        }


        List<Triple<Box, Float, Number>> run = breadthFirst(both()).filter(x -> x.properties.has(auto))
                .filter(x -> !done.containsKey(x))
                .filter(x -> x.properties.getFloat(auto, 0) > 0)
                .map(x -> new Triple<>(x, x.properties.get(Box.frame).y, x.properties.get(auto)))
                .collect(Collectors.toList());

        // mark everything as done right now, in case there are exceptions.
        run.forEach(x -> done.put(x.first, System.currentTimeMillis()));

        Collections.sort(run, (a, b) -> {
            if (a.third.doubleValue() > b.third.doubleValue()) return 1;
            if (a.third.doubleValue() < b.third.doubleValue()) return -1;

            if (a.second.doubleValue() < b.second.doubleValue()) return 1;
            if (a.second.doubleValue() > b.second.doubleValue()) return -1;

            return 0;
        });


        if (run.size() > 0) {
            System.out.println("\n\n\n gathered auto list is ");
            run.forEach((t) -> {
                System.out.println("      " + t);
            });
        }


        for (Triple<Box, Float, Number> t : run) {

            RunLoop.main.delayTicks(() -> {
                t.first.find(Chorder.begin, both())
                        .findFirst().map(x -> x.apply(t.first));

            }, (offset + t.third.intValue()));

        }
    }
}
