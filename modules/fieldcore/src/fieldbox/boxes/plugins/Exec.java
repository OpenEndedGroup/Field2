package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Pair;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.execution.Execution;
import fielded.RemoteEditor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plugin that adds '_.exec' to the graph
 */
public class Exec extends Box {

    static public final Dict.Prop<Box.BiFunctionOfBoxAnd<String, Triple<Object, List<String>, List<Pair<Integer, String>>>>> exec = new Dict.Prop<>("exec").type().toCannon().doc("`_.exec('foo()')` executes the expression `foo()` as if you'd typed it into the text editor with `_` selected and pressed command-return. This returns a `Triple` containing the the `Object` returned, a `List<String>` of everything 'printed' by this code and the `List<Pair<Integer, String>>` of all the errors and error-line numbers if any.");

    public Exec(Box root)
    {
        this.properties.put(exec, (box, string) -> {


            Execution ex = RemoteEditor.getExecution(box, Execution.code);
            Execution.ExecutionSupport support = ex.support(box, Execution.code);

            List<String> out = new ArrayList<>();
            List<Pair<Integer, String>> err = new ArrayList<>();
            Consumer<String> success = out::add;
            Consumer<Pair<Integer, String>> errors= err::add;

            Object o = support.executeTextFragment(string, "raw", success, errors);

            return new Triple<>(o, out, err);
        });
    }
}
