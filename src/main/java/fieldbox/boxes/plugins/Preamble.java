package fieldbox.boxes.plugins;

import field.graphics.Shader;
import field.utility.Dict;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Preamble extends Box {
    static public Dict.Prop<String> preamble = new Dict.Prop<>("preamble")
            .doc("code that is executed once in this context automatically when this box is loaded")
            .type()
            .toCanon().set(IO.persistent, true);

//    static public Dict.Prop<String> preroll = new Dict.Prop<>("preroll")
//            .doc("code that is prepended to every 'begin' invocation of this box")
//            .type()
//            .toCanon().set(IO.persistent, true);

    public Preamble(Box root) {
        properties.put(Commands.commands, () -> {
            Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
            RemoteEditor ed = this.find(RemoteEditor.editor, both()).findFirst().get();

            Box box = ed.getCurrentlyEditing();
            Dict.Prop<String> cep = ed.getCurrentlyEditingProperty();

            if (box == null) return m;

            if (!cep.equals(preamble))
                m.put(new Pair<>("Edit Preamble", "Switch to editing the code preamble"), () -> {
                    box.properties.computeIfAbsent(preamble, a -> "");
                    ed.setCurrentlyEditingProperty(preamble);
                });

//            if (!cep.equals(preroll))
//                m.put(new Pair<>("Edit Preroll", "Switch to editing the code preroll"), () -> {
//                    box.properties.computeIfAbsent(preroll, a -> "");
//                    ed.setCurrentlyEditingProperty(preroll);
//                });

            return m;
        });
    }


}
