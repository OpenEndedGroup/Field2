package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Log;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.execution.Completion;
import fieldbox.execution.HandlesQuoteCompletion;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Plugin offers _.runCommand("match") and associated functionality.
 * <p>
 * Note we return boolean as to whether matches were found and ran, but Nashorn right now just eats the return value and turns it into Undefined
 * <p>
 * We need a way of chaining these things together, but it's hard to do without access to the return value of runCommand...
 */
public class RunCommand extends Box {

    static public final Dict.Prop<BiFunctionOfBoxAnd<String, Boolean>> runCommand
            = new Dict.Prop<>("runCommand").doc("`_.runCommand(x)` runs commands for box `_` that match string `x`")
            .type()
            .toCannon();

    private Box root;

    public RunCommand(Box root) {
        this.root = root;
        this.properties.put(runCommand, new RunCompletor());
    }

    protected class RunCompletor implements BiFunctionOfBoxAnd<String, Boolean>, HandlesQuoteCompletion {

        @Override
        public List<Completion> getQuoteCompletionsFor(String prefix) {
            List<Triple<String, String, Runnable>> commands = Commands.getCommandsAndDocs(root);
            return commands.stream().filter(x -> stripFormatting(x.first.toLowerCase()).startsWith(prefix.toLowerCase())).map(x -> {
                Completion c = new Completion(-1, -1, x.first, "<span class='doc'>" + x.second + "</span>");
                return c;
            }).collect(Collectors.toList());
        }

        @Override
        public Boolean apply(Box box, String of) {
            Pattern p = Pattern.compile(of);
            List<Triple<String, String, Runnable>> commands = Commands.getCommandsAndDocs(box);

            Log.log("run.command", () -> "command size is " + commands.size());
            if (commands.size() == 0) return false;
            Log.log("run.command", () -> box + " " + of);


            System.out.println(" commands ? " + commands);

            Log.log("run.command", () -> "command size is " + commands.size());
            if (commands.size() == 0) return false;

            // Nashorn doesn't like a lambda here

            boolean[] found = {false};

            commands.stream().filter(x -> stripFormatting(x.first.toLowerCase()).equals(of.toLowerCase())).forEach(r -> {
                if (r.third instanceof RemoteEditor.ExtendedCommand) {
                    ((RemoteEditor.ExtendedCommand) r.third).begin((pr, o, a) -> {
                        a.begin(null, null); // SHOULD BE AN ARG
                        a.run();
                        found[0] = true;
                    }, null);
                    r.third.run();
                } else {
                    r.third.run();
                    found[0] = true;
                }
            });
            return found[0];
        }
    }


    private String stripFormatting(String s) {
        return s.replaceAll("<[^>]*>", "");
    }


}


