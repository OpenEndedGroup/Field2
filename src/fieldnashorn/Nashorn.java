package fieldnashorn;


import field.utility.*;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.plugins.BoxDefaultCode;
import fielded.Animatable;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.*;
import javax.script.*;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Adds Nashorn-based JavaScript support to Field.
 * <p>
 */
public class Nashorn implements BiFunction<Box, Dict.Prop<String>, NashornExecution> {

	final public static Dict.Prop<ScriptContext> boxBindings = new Dict.Prop<ScriptContext>("_boxBindings");

	final public String _stdlib = BoxDefaultCode.findSource(this.getClass(), "stdlib");

	private TernSupport ternSupport;

	NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
	ScriptEngine engine;
	private SimpleBindings global;

	public Nashorn() {

		engine = factory.getScriptEngine("-scripting", "--global-per-engine", "--optimistic-types=true", "--language=es6");

		global = new SimpleBindings();
		try {
			global.put("__fieldglobal", engine.eval("({})"));
			global.put("global", global);
			Log.log("bindings", () -> "__fieldglobal set up to be :" + global.get("__fieldglobal"));
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		engine.setBindings(global, ScriptContext.GLOBAL_SCOPE);

		try {
			engine.eval("__fieldglobal.fielded = Packages.fielded");
			engine.eval("__fieldglobal.fieldbox = Packages.fieldbox");
			engine.eval("__fieldglobal.field = Packages.field");
			engine.eval("__fieldglobal.fieldnashorn = Packages.fieldnashorn");
			engine.eval("__fieldglobal.fieldcef = Packages.fieldcef");

		} catch (ScriptException e) {
			e.printStackTrace();
		}

		ternSupport = new TernSupport();
		ternSupport.inject(engine);

		Animatable.registerHandler((was, o) -> {

			if (o instanceof ScriptObjectMirror) {
				ScriptObjectMirror som = ((ScriptObjectMirror) o);
				if (som.isFunction()) {
					return Animatable.interpret((Supplier) () -> som.call(som), was);
				}

				Animatable.AnimationElement start = null;
				Animatable.AnimationElement middle = null;
				Animatable.AnimationElement end = null;

				if (som.hasSlot(0)) start = Animatable.interpret(som.getSlot(0), was);
				if (som.hasSlot(1)) middle = Animatable.interpret(som.getSlot(1), was);
				if (som.hasSlot(2)) end = Animatable.interpret(som.getSlot(2), was);

				if (som.hasMember("start")) start = Animatable.interpret(som.getMember("start"), was);
				if (som.hasMember("middle"))
					middle = Animatable.interpret(som.getMember("middle"), was);
				if (som.hasMember("end")) end = Animatable.interpret(som.getMember("end"), was);

				if (start == null) start = noop();
				if (middle == null) middle = noop();
				if (end == null) end = noop();

				if (start == middle) start = noop();
				if (end == middle) end = noop();

				Animatable.AnimationElement fstart = start;
				Animatable.AnimationElement fmiddle = middle;
				Animatable.AnimationElement fend = end;

				return new Animatable.AnimationElement() {

					Animatable.AnimationElement[] targets = {fstart, fmiddle, fend};
					int index = 0;
					boolean finished = false;

					@Override
					public Object beginning(boolean isEnding) {
						targets[0] = interpretReturn(targets[0], targets[0].beginning(isEnding));
						targets[0] = interpretReturn(targets[0], targets[0].middle(isEnding));
						targets[0] = interpretReturn(targets[0], targets[0].end(isEnding));
						targets[1] = interpretReturn(targets[1], targets[1].beginning(isEnding));
						return this;
					}

					public Object middle(boolean isEnding) {
						targets[1] = interpretReturn(targets[1], targets[1].middle(isEnding));
						return this;
					}

					public Object end(boolean isEnding) {
						targets[1] = interpretReturn(targets[1], targets[1].end(isEnding));
						targets[2] = interpretReturn(targets[2], targets[2].beginning(isEnding));
						targets[2] = interpretReturn(targets[2], targets[2].middle(isEnding));
						targets[2] = interpretReturn(targets[2], targets[2].end(isEnding));
						return this;
					}

				};
			}
			return was;
		});

		Animatable.registerHandler((was, o) -> {
			if (o instanceof Runnable) {
				return new Animatable.AnimationElement() {

					@Override
					public Object middle(boolean isEnding) {
						((Runnable) o).run();
						return null;
					}

					@Override
					public Object beginning(boolean isEnding) {
						return middle(isEnding);
					}

					@Override
					public Object end(boolean isEnding) {
						return middle(isEnding);
					}


				};
			}
			return was;
		});

		Animatable.registerHandler((was, o) -> {
			if (o instanceof Supplier) {
				return new Animatable.AnimationElement() {

					@Override
					public Object middle(boolean isEnding) {
						return ((Supplier) o).get();
					}

					@Override
					public Object beginning(boolean isEnding) {
						return null;/*return middle(isEnding);*/
					}

					@Override
					public Object end(boolean isEnding) {
						return null;/*return middle(isEnding);*/
					}

				};
			}
			return was;
		});

	}

	static public Animatable.AnimationElement noop() {
		return new Animatable.AnimationElement() {
		};
	}

	static public Animatable.AnimationElement interpretReturn(Animatable.AnimationElement was, Object next) {
		if (next == null) return was;
		if (next instanceof Animatable.AnimationElement) return (Animatable.AnimationElement) next;
		Animatable.AnimationElement nextElement = Animatable.interpret(next, was);
		if (nextElement != null) return nextElement;
		return was;
	}

	public Map<Box, ScriptEngine> bindingsPerBox = new WeakHashMap<Box, ScriptEngine>();
	Map<Box, NashornExecution> executionsPerBox = new WeakHashMap<Box, NashornExecution>();

	static public final List<String> scriptArgs = new ArrayList(Arrays.asList("-scripting", "--language=es6"));

	static {
		if (Options.dict().isTrue(new Dict.Prop<Number>("strict"), false))
			scriptArgs.add("-strict");
	}


	Cached<Pair<Box, Dict.Prop<String>>, Pair<Box, Dict.Prop<String>>, NashornExecution> cached = new Cached<>((next, was) -> {

		ScriptEngine en = bindingsPerBox.computeIfAbsent(next.first, k -> factory.getScriptEngine(scriptArgs.toArray(new String[0])));

		ScriptContext b = en.getContext();
		en.setBindings(global, ScriptContext.GLOBAL_SCOPE);

		setupInitialBindings(b, next.first);

		try {
			en.eval(_stdlib);
		} catch (ScriptException e) {
			System.err.println(" _stdlib threw an exception, can't be a good thing ");
			e.printStackTrace();
		}

		NashornExecution ex = new NashornExecution(this, next.first, next.second, b, en);
		ex.setTernSupport(ternSupport);

		return ex;
	}, (x) -> x);


	NashornExecution fork(NashornExecution from, Map<String, Object> parameters) {
		ScriptEngine se = factory.getScriptEngine("-scripting", "--language=es6");
		ScriptContext context = se.getContext();

		Bindings dest = from.context.getBindings(ScriptContext.GLOBAL_SCOPE);
		Bindings source = from.context.getBindings(ScriptContext.GLOBAL_SCOPE);

		dest.entrySet().forEach(e -> source.put(e.getKey(), e.getValue()));

		context.setAttribute("_", from.box, ScriptContext.ENGINE_SCOPE);
		from.box.find(Boxes.root, from.box.both()).findFirst().ifPresent(x -> context.setAttribute("__", x, ScriptContext.ENGINE_SCOPE));

		NashornExecution ex = new NashornExecution(this, from.box, from.property, context, se);
		return ex;
	}


	void setupInitialBindings(ScriptContext context, Box first) {

		if (Watchdog.getLimits()) {
			SimpleBindings e = new SimpleBindings() {
				Watchdog w = new Watchdog();

				@Override
				public Object get(Object key) {
					w.tick();

					Object v = super.get(key);
					if (v != null)
						return v;

					ScriptObjectMirror mm = (ScriptObjectMirror) super.get("nashorn.global");
					if (mm != null) {
						Object mmm = mm.get(key);
						return mmm;
					}
					return null;

				}

				@Override
				public boolean containsKey(Object key) {
					ScriptObjectMirror som = (ScriptObjectMirror) super.get("nashorn.global");
					if (super.containsKey(key)) return true;
					if (som != null) {
						return som.containsKey(key);
					}
					return false;
				}
			};
			context.setBindings(e, ScriptContext.ENGINE_SCOPE);
		}

		context.setAttribute("_", first, ScriptContext.ENGINE_SCOPE);
		first.find(Boxes.root, first.both()).

			findFirst().

			ifPresent(x -> context.setAttribute("__", x, ScriptContext.ENGINE_SCOPE));

		first.properties.put(boxBindings, context);
	}

	@Override
	public NashornExecution apply(Box box, Dict.Prop<String> stringProp) {
		return executionsPerBox.computeIfAbsent(box, k -> cached.apply(new Pair<>(box, stringProp)));
	}
}
