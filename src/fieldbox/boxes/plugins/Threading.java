package fieldbox.boxes.plugins;

import field.app.ThreadSync;
import field.utility.Dict;
import fieldbox.boxes.Box;
import jdk.nashorn.api.scripting.AbstractJSObject;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

import static field.app.ThreadSync.yield;

/**
 * Created by marc on 5/24/16.
 */
public class Threading extends Box {

    static public final Dict.Prop<AbstractJSObject> yield = new Dict.Prop<>("yield").toCannon().doc("`_.yield()` waits for one whole animation cycle before continuing on from here");
    static public final Dict.Prop<FunctionOfBoxValued<Threads>> threads = new Dict.Prop<>("threads").toCannon().doc("`_.threads` returns a thread control object that provides a high-level interface to the fibre threading of a box");

    // high-level information and control over thre threads in a box
    static public final class Threads {
        public final boolean enabled = ThreadSync.enabled;

        public final long numRunning;
        private final Box on;

        protected Threads(Box on) {
            this.on = on;
            Stream<ThreadSync.Fiber> q = ThreadSync.get().findByTag(on);
            numRunning = q.filter(x -> !x.runner.isDone()).count();
        }

        public void kill() {
            ThreadSyncFeedback.kill(on);
        }

        public void pause() {
            ThreadSyncFeedback.pause(on);
        }

        public void step() {
            ThreadSyncFeedback.step(on);
        }

        public void play() {
            ThreadSyncFeedback.cont(on);
        }
    }


    public Threading() {

        this.properties.put(threads, (x) -> new Threads(x));
        this.properties.put(yield, new AbstractJSObject() {
            @Override
            public Object call(Object o, Object... objects) {
                try {

                    return ThreadSync.yield(objects);
                } catch (InterruptedException e) {
                }
                return null;
            }
        });
    }
}
