package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.app.ThreadSync;
import field.utility.Conversions;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.execution.Completion;
import fieldbox.execution.HandlesCompletion;
import fieldbox.io.IO;
import fieldlinker.AsMap;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Adds properties that help navigate the dispatch tree
 */
public class Pseudo extends Box {

    static public Dict.Prop<FunctionOfBoxValued<First>> where = new Dict.Prop<FunctionOfBoxValued<First>>("where").doc(
            "`_.where.x` returns the box that contains the property `_.x`. This means that `_.where.x=someOtherBox` can be used to move properties around.")
            .toCannon()
            .type();

    static public Dict.Prop<FunctionOfBoxValued<All>> all = new Dict.Prop<FunctionOfBoxValued<All>>("all").doc(" `_.all.x` returns all values of `x` above this box and at this box")
            .toCannon()
            .type();

    static public Dict.Prop<FunctionOfBoxValued<Up>> up = new Dict.Prop<FunctionOfBoxValued<Up>>("up").doc(" `_.up.x` returns _a_ first value of `x` above this box, or `null` if there isn't one")
            .toCannon()
            .type();


    static public Dict.Prop<FunctionOfBoxValued<Has>> has = new Dict.Prop<FunctionOfBoxValued<All>>("has").doc(" `_.has.x` returns true if this box, or any box above it, has a property `x` ")
            .toCannon()
            .type();

    static public Dict.Prop<FunctionOfBoxValued<Herer>> here = new Dict.Prop<FunctionOfBoxValued<Herer>>("here").doc(" `_.here.x` returns true if this box has a property `x` ")
            .toCannon()
            .type();
    static public Dict.Prop<FunctionOfBoxValued<Signal>> signal = new Dict.Prop<FunctionOfBoxValued<All>>("signal").doc(
            " `_.signal.x` returns `_.has.x`, and deletes this value at the same time. ")
            .toCannon()
            .type();

    static public Dict.Prop<FunctionOfBoxValued<Queue>> queue = new Dict.Prop<FunctionOfBoxValued<Queue>>("queue").doc(" `_.queue.A = 10`, pushes a value to queue `A`, `_.queue.A` pops it")
            .toCannon()
            .type();
    static public Dict.Prop<FunctionOfBoxValued<Peek>> peek = new Dict.Prop<FunctionOfBoxValued<Queue>>("peek").doc(
            " `_.peek.A = 10`, pushes a value to queue `A`, `_.peek.A` peeks at it (returns it without popping)")
            .toCannon()
            .type();


    static public Dict.Prop<FunctionOfBoxValued<Until>> yieldUntil = new Dict.Prop<FunctionOfBoxValued<Until>>("yieldUntil").doc(
            " `_.yieldUntil.A` yields until property `A` is non-null / non-false,`_yieldUntil.A=10`, yields until `A==10`")
            .toCannon()
            .type();

    static public Dict.Prop<FunctionOfBoxValued<Sync>> sync = new Dict.Prop<FunctionOfBoxValued<Sync>>("sync").doc(
            " `_.sync.canvas = function() { ... return blah } executes that function on the main thread, and sets `_.canvas` to be the return value of that function. This does this synchronously.")
            .toCannon()
            .type();


    static public Dict.Prop<FunctionOfBoxValued<Down>> down = new Dict.Prop<FunctionOfBoxValued<Down>>("down").doc(" `_.down.x` searches for `x` _down_ the dispatch graph rather than upwards ")
            .toCannon()
            .type();
    static public Dict.Prop<FunctionOfBoxValued<AllDown>> allDown = new Dict.Prop<FunctionOfBoxValued<AllDown>>("allDown").doc(
            "`_.allDown.x` searches for `x` _down_ the dispatch graph rather than upwards, and returns all results")
            .toCannon()
            .type();

    static public Dict.Prop<IdempotencyMap<Runnable>> next = new Dict.Prop<IdempotencyMap<Runnable>>("next").doc(
            "`_.next.A = function(){}` executes this function in the next update cycle. Note, `A` will overwrite anything else that's been set in this box with this name for this cycle")
            .toCannon()
            .type()
            .autoConstructs(() -> new IdempotencyMap<>(Runnable.class));

    static public Dict.Prop<FunctionOfBoxValued<MainThreader>> inMainThread = new Dict.Prop<FunctionOfBoxValued<MainThreader>>("inMainThread").doc(
            "`_.inMainThread.foo = ()=>{ ... }` execute that function in the main thread, waiting for it to return and sets `_.foo` to the return value")
            .toCannon()
            .type();


    static public Dict.Prop<IdempotencyMap<Runnable>> next10 = new Dict.Prop<IdempotencyMap<Runnable>>("next10").doc(
            "`_.next10.A = ()=>{ ... }` executes this function 10 update cycles later. Note, `A` will overwrite anything else that's been set in this box with this name for this cycle")
            .toCannon()
            .type()
            .autoConstructs(() -> new IdempotencyMap<>(Runnable.class));


    static public Dict.Prop<FunctionOfBoxValued<Replacer>> replace = new Dict.Prop<FunctionOfBoxValued<Replacer>>("replace").doc(
            "`_.replace.x = 10` replaces the value of `x` where it is found (e.g. here or some parent).")
            .toCannon();

    static public Dict.Prop<FunctionOfBoxValued<Refer>> ref = new Dict.Prop<FunctionOfBoxValued<Refer>>("ref").toCannon()
            .type()
            .doc("`_.ref.x` is equivalent to `function(){ return _.x }`");

    //	static public Dict.Prop<FunctionOfBoxValued<XPath>> query = new Dict.Prop<>("query").toCannon().type();
    static public Dict.Prop<FunctionOfBoxValued<Namer>> named = new Dict.Prop<>("named").toCannon()
            .type()
            .doc("`_.named.x` returns an array of all the boxes named `x` that are _below_ this box. If you want to search everywhere, try `_.root.named.x`. To match regex or use whitespace in names, try `_.named['.*x']`");


    static public Dict.Prop<FunctionOfBoxValued<WithID>> withID = new Dict.Prop<>("withID").toCannon()
            .type()
            .doc("`_.withID['abc']` returns any box with ID `abc` _below_ this box. If you want to search everywhere, try `_.root.withID.x`. ID's are uniqe (across the universe).");

    static public Dict.Prop<FunctionOfBoxValued<Oncer>> once = new Dict.Prop<FunctionOfBoxValued<Oncer>>("once").toCannon()
            .type()
            .doc("`_.once.x = function(){ ... do something ... }` will call that function if `x` isn't set here and set `x` to the result if that function returns something. It's a fine way to initialize something once.");

    public Pseudo(Box r) {
        this.properties.put(where, First::new);
        this.properties.put(all, All::new);
        this.properties.put(up, Up::new);
        this.properties.put(down, Down::new);
        this.properties.put(allDown, AllDown::new);
        this.properties.put(has, Has::new);
        this.properties.put(signal, Signal::new);
        this.properties.put(queue, Queue::new);
        this.properties.put(peek, Peek::new);
        this.properties.put(yieldUntil, Until::new);
        this.properties.put(replace, Replacer::new);
//		this.properties.put(query, XPath::new);
        this.properties.put(ref, Refer::new);
        this.properties.put(named, Namer::new);
        this.properties.put(withID, WithID::new);
        this.properties.put(once, Oncer::new);
        this.properties.put(here, Herer::new);
        this.properties.put(inMainThread, MainThreader::new);

        this.properties.putToMap(Boxes.insideRunLoop, "main.__next__", () -> {
            r.breadthFirst(r.downwards())
                    .map(x -> x.properties.get(next))
                    .filter(x -> x != null)
                    .forEach(x -> {
                        ArrayList<Runnable> q = new ArrayList<>(x.values());
                        x.clear();
                        q.forEach(z -> z.run());
                    });
            return true;
        });
        this.properties.putToMap(Boxes.insideRunLoop, "main.__next10__", () -> {
            r.breadthFirst(r.downwards())
                    .map(x -> x.properties.get(next10))
                    .filter(x -> x != null)
                    .forEach(x -> {
                        ArrayList<Runnable> q = new ArrayList<>(x.values());
                        x.clear();
                        q.forEach(z -> RunLoop.main.delayTicks(z, 10));
                    });
            return true;
        });

        this.properties.put(sync, Sync::new);
    }

    static public class Namer implements AsMap, HandlesCompletion {

        protected final Box on;

        public Namer(Box on) {
            this.on = on;
        }

        @Override
        public boolean asMap_isProperty(String s) {
            return true;
        }

        @Override
        public Object asMap_call(Object o, Object o1) {
            return asMap_getElement(o1);
        }

        @Override
        public Object asMap_getElement(Object element) {
            return asMap_get(element + "");
        }

        @Override
        public Object asMap_setElement(int element, Object o) {
            return null;
        }

        @Override
        public Object asMap_get(String p) {
            return on.breadthFirst(on.downwards())
                    .filter(x -> x.properties.has(Box.name))
                    .filter(x -> x.properties.get(Box.name)
                            .matches(p))
                    .collect(Collectors.toList());
        }

        @Override
        public Object asMap_set(String p, Object val) {
            return null;
        }


        @Override
        public Object asMap_new(Object a) {
            return null;
        }

        @Override
        public Object asMap_new(Object a, Object b) {
            return null;
        }

        @Override
        public Object asMap_getElement(int element) {
            return asMap_get("" + element);
        }

        @Override
        public boolean asMap_delete(Object o) {
            return false;
        }

        public List<Completion> getCompletionsFor(String prefix) {
            Set<String> q = on.breadthFirst(on.downwards())
                    .filter(x -> x.properties.has(Box.name))
                    .map(x -> x.properties.get(Box.name)).filter(x -> x.startsWith(prefix)).collect(Collectors.toSet());

            List<Completion> c = new ArrayList<>();

            q.forEach(x -> {
                c.add(new Completion(-1, -1, x, ""));
            });
            return c;

        }

    }

    static public class MainThreader extends Namer {

        public MainThreader(Box on) {
            super(on);
        }


        @Override
        public Object asMap_setElement(int element, Object o) {
            return asMap_set("" + element, o);
        }

        @Override
        public Object asMap_set(String p, Object val) {

            System.out.println(" in main thread :"+p+" -> "+val);

            Supplier q = (Supplier) Conversions.convert(val, Supplier.class);
            if (q == null)
                throw new IllegalArgumentException(" can't convert " + val + " to something I can call");
            try {
                Object m = ThreadSync.callInMainThreadAndWait((Callable<Object>) q::get);
                on.properties.put(new Dict.Prop<>(p), m);
                return m;
            } catch (Exception e) {
                e.printStackTrace();
                IllegalArgumentException t = new IllegalArgumentException(" function threw an exception ");
                t.initCause(e);
                throw t;
            }
        }

    }

    static public class WithID implements AsMap {

        private final Box on;

        public WithID(Box on) {
            this.on = on;
        }

        @Override
        public boolean asMap_isProperty(String s) {
            return true;
        }

        @Override
        public Object asMap_call(Object o, Object o1) {
            return asMap_getElement(o1);
        }

        @Override
        public Object asMap_getElement(Object element) {
            return asMap_get(element + "");
        }

        @Override
        public Object asMap_setElement(int element, Object o) {
            return null;
        }

        @Override
        public Object asMap_get(String p) {
            return on.breadthFirst(on.downwards())
                    .filter(x -> x.properties.has(IO.id))
                    .filter(x -> x.properties.get(IO.id)
                            .matches(p))
                    .findFirst().orElse(null);
        }

        @Override
        public Object asMap_set(String p, Object val) {
            return null;
        }


        @Override
        public Object asMap_new(Object a) {
            return null;
        }

        @Override
        public Object asMap_new(Object a, Object b) {
            return null;
        }

        @Override
        public Object asMap_getElement(int element) {
            return asMap_get("" + element);
        }

        @Override
        public boolean asMap_delete(Object o) {
            return false;
        }
    }

    static public class Oncer implements AsMap {

        private final Box on;

        public Oncer(Box on) {
            this.on = on;
        }

        @Override
        public boolean asMap_isProperty(String s) {
            return true;
        }

        @Override
        public Object asMap_call(Object o, Object o1) {
            return asMap_getElement(o1);
        }

        @Override
        public Object asMap_getElement(Object element) {
            return asMap_get(element + "");
        }

        @Override
        public Object asMap_setElement(int element, Object o) {
            return null;
        }

        @Override
        public Object asMap_get(String p) {
            return null;
        }

        @Override
        public Object asMap_set(String p, Object val) {

            Dict.Prop cc = new Dict.Prop(p);

            Dict.Prop cannon = cc.findCannon();
            if (cannon != null) cc = cannon;
            Supplier s = (Supplier) Conversions.convert(val, Supplier.class);

            return on.properties.computeIfAbsent(cc, x -> s.get());
        }


        @Override
        public Object asMap_new(Object a) {
            return null;
        }

        @Override
        public Object asMap_new(Object a, Object b) {
            return null;
        }

        @Override
        public Object asMap_getElement(int element) {
            return asMap_get("" + element);
        }

        @Override
        public boolean asMap_delete(Object o) {
            return false;
        }
    }

    static public class Sync implements AsMap {

        private final Box on;

        public Sync(Box on) {
            this.on = on;
        }

        @Override
        public boolean asMap_isProperty(String s) {
            return true;
        }

        @Override
        public Object asMap_call(Object o, Object o1) {
            return asMap_getElement(o1);
        }

        @Override
        public Object asMap_getElement(Object element) {
            return asMap_get(element + "");
        }

        @Override
        public Object asMap_setElement(int element, Object o) {
            return null;
        }

        @Override
        public Object asMap_get(String p) {
            return null;
        }

        @Override
        public Object asMap_set(String p, Object val) {

            Supplier c = (Supplier) Conversions.convert(val, Supplier.class);

            Object[] o = {null};

            if (c != null) {
                try {
                    ThreadSync.inMainThread(() -> {
                        o[0] = c.get();
                    });
                } catch (InterruptedException e) {
                    throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
                } catch (ExecutionException e) {
                    throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
                }
            }


            return on.properties.put(new Dict.Prop(p), o[0]);
        }


        @Override
        public Object asMap_new(Object a) {
            return null;
        }

        @Override
        public Object asMap_new(Object a, Object b) {
            return null;
        }

        @Override
        public Object asMap_getElement(int element) {
            return asMap_get("" + element);
        }

        @Override
        public boolean asMap_delete(Object o) {
            return false;
        }
    }

    static public class First implements AsMap {

        protected final Box on;

        public First(Box on) {
            this.on = on;
        }

        @Override
        public boolean asMap_isProperty(String s) {
            return true;
        }

        @Override
        public Object asMap_call(Object o, Object o1) {
            return null;
        }

        @Override
        public boolean asMap_delete(Object o) {
            Box q = (Box) asMap_get("" + o);
            if (q != null) {
                return q.properties.asMap_delete(o);
            }
            return false;
        }

        @Override
        public Object asMap_get(String s) {
            Dict.Prop p = new Dict.Prop(s);
            return on.breadthFirst(on.upwards())
                    .filter(x -> x.properties.has(p))
                    .findFirst()
                    .orElseGet(() -> null);
        }

        @Override
        public Object asMap_set(String s, Object o) {

            if (o instanceof Box) {
                Dict.Prop p = new Dict.Prop(s);
                return on.breadthFirst(on.upwards())
                        .filter(x -> x.properties.has(p))
                        .findFirst()
                        .map(x -> {
                            Object v = x.properties.remove(p);
                            ((Box) o).properties.put(p, v);
                            return v;
                        });
            } else {
                throw new IllegalArgumentException(" can't move property to something that isn't a box");
            }
        }

        @Override
        public Object asMap_new(Object o) {
            return null;
        }

        @Override
        public Object asMap_new(Object o, Object o1) {
            return null;
        }

        @Override
        public Object asMap_getElement(int i) {
            return null;
        }

        @Override
        public Object asMap_setElement(int i, Object o) {
            return null;
        }
    }

    static public class Herer implements AsMap {

        protected final Box on;

        public Herer(Box on) {
            this.on = on;
        }

        @Override
        public boolean asMap_isProperty(String s) {
            return true;
        }

        @Override
        public Object asMap_call(Object o, Object o1) {
            return null;
        }

        @Override
        public boolean asMap_delete(Object o) {
            throw new IllegalArgumentException(" can't delete here");
        }

        @Override
        public Object asMap_get(String s) {
            Dict.Prop p = new Dict.Prop(s);
            return (on.properties.has(p));

        }

        @Override
        public Object asMap_set(String s, Object o) {
            throw new IllegalArgumentException(" can't set here");
        }

        @Override
        public Object asMap_new(Object o) {
            return null;
        }

        @Override
        public Object asMap_new(Object o, Object o1) {
            return null;
        }

        @Override
        public Object asMap_getElement(int i) {
            return null;
        }

        @Override
        public Object asMap_setElement(int i, Object o) {
            return null;
        }
    }

    static public class All extends First implements AsMap {

        public All(Box on) {
            super(on);
        }


        @Override
        public Object asMap_get(String s) {
            Dict.Prop<?> p = new Dict.Prop(s);
            return on.breadthFirst(on.upwards())
                    .filter(x -> x.properties.has(p))
                    .map(x -> x.properties.get(p))
                    .collect(Collectors.toList());
        }

    }

    static public class Up extends First implements AsMap {

        public Up(Box on) {
            super(on);
        }


        @Override
        public Object asMap_get(String s) {
            Dict.Prop<?> p = new Dict.Prop(s);

            for (Box b : on.parents()) {
                Optional<?> q = b.breadthFirst(b.upwards())
                        .filter(x -> x.properties.has(p))
                        .map(x -> x.properties.get(p))
                        .findFirst();

                if (q.isPresent()) return q.get();
            }

            return null;
        }

    }


    static public class Refer extends First implements AsMap {

        public Refer(Box on) {
            super(on);
        }


        @Override
        public Object asMap_get(String s) {
            return new Dict.Prop(s).toCannon();
        }


    }

    static public class Replacer extends First implements AsMap {

        public Replacer(Box on) {
            super(on);
        }


        @Override
        public Object asMap_set(String s, Object v) {
            Box at = (Box) super.asMap_get(s);
            if (at == null) return null;

            return at.properties.put(new Dict.Prop("" + s), v);
        }
    }

    static public class Queue extends First implements AsMap {

        public Queue(Box on) {
            super(on);
        }


        @Override
        public Object asMap_get(String s) {
            Dict.Prop<Collection> p = new Dict.Prop(s);
            Collection q = on.breadthFirst(on.upwards())
                    .filter(x -> x.properties.has(p))
                    .map(x -> x.properties.get(p))
                    .filter(x -> x != null)
                    .filter(x -> x.size() > 0)
                    .findFirst()
                    .orElse(null);
            if (q == null) return null;
            Iterator i = q.iterator();
            Object r = i.next();
            i.remove();
            return r;
        }

        @Override
        public Object asMap_set(String s, Object o) {
            Dict.Prop<Collection<Object>> p = new Dict.Prop<>(s);
            on.properties.putToList(p, o);
            return o;
        }
    }

    static public class Peek extends First implements AsMap {

        public Peek(Box on) {
            super(on);
        }


        @Override
        public Object asMap_get(String s) {
            Dict.Prop<Collection> p = new Dict.Prop(s);
            Collection q = on.breadthFirst(on.upwards())
                    .filter(x -> x.properties.has(p))
                    .map(x -> x.properties.get(p))
                    .filter(x -> x != null)
                    .filter(x -> x.size() > 0)
                    .findFirst()
                    .orElse(null);
            if (q == null) return null;
            Iterator i = q.iterator();
            Object r = i.next();
//			i.remove();
            return r;
        }

        @Override
        public Object asMap_set(String s, Object o) {
            Dict.Prop<Collection<Object>> p = new Dict.Prop<>(s);
            on.properties.putToList(p, o);
            return o;
        }
    }


    static public class Until extends First implements AsMap {

        private final int extra;

        public Until(Box on) {
            this(on, 0);
        }

        public Until(Box on, int extra) {
            super(on);
            this.extra = extra;
        }

        @Override
        public Object asMap_getElement(int i) {
            return new Until(on, extra + i);
        }

        @Override
        public Object asMap_getElement(Object i) {
            return new Until(on, extra + ((Number) i).intValue());
        }

        @Override
        public Object asMap_get(String s) {
            try {
                Dict.Prop<Object> p = new Dict.Prop(s);
                Object q = null;
                while (true) {
                    q = on.breadthFirst(on.upwards())
                            .filter(x -> x.properties.has(p))
                            .map(x -> x.properties.get(p))
                            .filter(x -> x != null)
                            .findFirst()
                            .orElse(null);

                    if (q == null || (q instanceof Boolean && ((Boolean) q).booleanValue() == false)) {
                        try {
                            ThreadSync.yield(1);
                        } catch (InterruptedException e) {
                        }
                    } else {

                        return q;
                    }
                }
            } finally {
                for (int i = 0; i < extra; i++) {
                    try {
                        ThreadSync.yield(1);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        @Override
        public Object asMap_set(String s, Object o) {
            while (true) {
                Object q = asMap_get(s);
                if (o == null && q == null) return o;
                try {
                    if (o != null && q != null && o.equals(q))
                        return o;
                    try {
                        ThreadSync.yield(1);
                    } catch (InterruptedException e) {
                    }

                } finally {
                    for (int i = 0; i < extra; i++) {
                        try {
                            ThreadSync.yield(1);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    }

    static public class Has extends First implements AsMap {

        public Has(Box on) {
            super(on);
        }


        @Override
        public Object asMap_get(String s) {
            Dict.Prop p = new Dict.Prop(s);
            return on.breadthFirst(on.upwards())
                    .filter(x -> x.properties.has(p))
                    .findAny()
                    .isPresent();
        }
    }

    static public class Signal extends First implements AsMap {

        public Signal(Box on) {
            super(on);
        }

        public boolean asMap_isProperty(String s) {
            return true;
        }


        @Override
        public Object asMap_get(String s) {
            Dict.Prop p = new Dict.Prop(s);
            Optional<Box> q = on.breadthFirst(on.upwards())
                    .filter(x -> x.properties.has(p))
                    .findAny();


            if (!q.isPresent()) return null;

            return q.get().properties.remove(p);
        }


        @Override
        public String toString() {
            return "sig:" + on;
        }
    }

    static public class AllDown extends First implements AsMap {

        public AllDown(Box on) {
            super(on);
        }


        @Override
        public Object asMap_get(String s) {
            Dict.Prop<?> p = new Dict.Prop(s);
            return on.breadthFirst(on.downwards())
                    .filter(x -> x.properties.has(p))
                    .map(x -> x.properties.get(p))
                    .collect(Collectors.toList());
        }

    }

    static public class Down extends First implements AsMap {

        public Down(Box on) {
            super(on);
        }


        @Override
        public Object asMap_get(String s) {
            Dict.Prop<?> p = new Dict.Prop(s);
            return on.breadthFirst(on.downwards())
                    .filter(x -> x.properties.has(p))
                    .map(x -> x.properties.get(p))
                    .findFirst()
                    .orElseGet(() -> null);
        }

    }
}
