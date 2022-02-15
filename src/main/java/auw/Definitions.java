package auw;

import field.utility.Dict;
import field.utility.IdempotencyMap;
import fieldbox.boxes.Watches;
import org.openjdk.nashorn.api.scripting.JSObject;

import java.nio.FloatBuffer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Definitions {
    static public final Dict.Prop<IdempotencyMap<Function<FBuffer, Object>>> a
            = new Dict.Prop<IdempotencyMap<Function<FBuffer, Object>>>("a").toCanon().type()
            .autoConstructs(() -> new IdempotencyMap<>(Function.class));

    static public final Dict.Prop<JSObject> audio
            = new Dict.Prop<JSObject>("audio").toCanon().type().doc("the audio callback function associated with this box");

    static public final Dict.Prop<Boolean> __isAudio
            = new Dict.Prop<JSObject>("__isAudio").toCanon().type();

}
