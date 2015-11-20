package fieldbox.boxes;

import field.nashorn.api.scripting.ScriptObjectMirror;
import field.nashorn.api.scripting.ScriptUtils;
import field.nashorn.internal.runtime.ScriptObject;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Log;
import field.utility.Rect;
import fieldbox.execution.Execution;
import fielded.DisabledRangeHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A central spot for definitions for notifications concerning various things happening to boxs
 */
public class Callbacks {

	static public final Dict.Prop<IdempotencyMap<Box.FunctionOfBox>> onDelete
		    = new Dict.Prop<>("onDelete").type()
						 .toCannon()
						 .doc("callback that's called when a box is deleted")
						 .autoConstructs(() -> new IdempotencyMap<>(
							     Box.FunctionOfBox.class));


	static public final Dict.Prop<IdempotencyMap<Box.FunctionOfBox>> onLoad
		    = new Dict.Prop<>("onLoad").type()
						 .toCannon()
						 .doc("callback that's called when a box is loaded")
						 .autoConstructs(() -> new IdempotencyMap<>(
							     Box.FunctionOfBox.class));

	static public final Dict.Prop<IdempotencyMap<Box.FunctionOfBox>> onSelect
		    = new Dict.Prop<>("onSelect").type()
					       .toCannon()
					       .doc("callback that's called when a box is selected")
					       .autoConstructs(() -> new IdempotencyMap<>(
							   Box.FunctionOfBox.class));

	static public final Dict.Prop<IdempotencyMap<Box.FunctionOfBox>> onDeselect
		    = new Dict.Prop<>("onDeselect").type()
						 .toCannon()
						 .doc("callback that's called when a box is deselected")
						 .autoConstructs(() -> new IdempotencyMap<>(
							     Box.FunctionOfBox.class));


	static public void call(Box from, Dict.Prop<IdempotencyMap<Box.FunctionOfBox>> prop) {
		from.breadthFirst(from.upwards())
		    .map(x -> x.properties.get(prop))
		    .filter(x -> x != null)
		    .flatMap(x -> x.values()
				   .stream())
		    .forEach(x -> x.apply(from));
	}

	static public void transition(Box on, Dict.Prop<Boolean> property, boolean transitionTo,  boolean defaultState, Dict.Prop<IdempotencyMap<Box.FunctionOfBox>> toTrue,  Dict.Prop<IdempotencyMap<Box.FunctionOfBox>> toFalse)
	{
		boolean currentState = on.properties.isTrue(property, defaultState);
		if (currentState==transitionTo) return;

		on.properties.put(property, transitionTo);
		if (transitionTo)
			call(on, toTrue);
		else
			call(on, toFalse);
	}




	static public void delete(Box from) {
		call(from, onDelete);
	}

	static public void load(Box from) {
		call(from, onLoad);
	}

	static public final Dict.Prop<IdempotencyMap<Box.BiFunctionOfBoxAnd<Rect, Rect>>>
		    onFrameChanged = new Dict.Prop<>("onFrameChanged").type()
								      .toCannon()
								      .doc("callback that's called when aa box is moved. Signature is .onFrameChanged(oldRect, newRect) -> Rect ")
								      .autoConstructs(
										  () -> new IdempotencyMap<>(
											      Box.BiFunctionOfBoxAnd.class));


	static public Rect frameChange(Box from, Rect a) {
		return thread(from, a, onFrameChanged);
	}

	//todo: generalize
	static public void frameModified(Box b, Consumer<Rect> r)
	{
		Rect rect = b.properties.get(Box.frame);
		Rect r2 = rect.duplicate();
		r.accept(r2);

		Rect r3 = Callbacks.frameChange(b, r2);

		b.properties.put(Box.frame, r3);

	}

	static public final Dict.Prop<IdempotencyMap<Box.BiFunctionOfBoxAnd<String, String>>>
		    onNameChange = new Dict.Prop<>("onNameChange").type()
								    .toCannon()
								    .doc("callback that's called when a boxes name is changed")
								    .autoConstructs(
										() -> new IdempotencyMap<>(
											    Box.BiFunctionOfBoxAnd.class));

	static public String nameChange(Box from, String a) {
		return thread(from, a, onNameChange);
	}

	static public <T> T thread(Box from, T initial, Dict.Prop<IdempotencyMap<Box.BiFunctionOfBoxAnd<T, T>>> prop) {
		Object[] az = {initial};

		/*
		from.breadthFirst(from.upwards())
		    .map(x -> x.properties.get(prop))
		    .filter(x -> x != null)
		    .flatMap(x -> x.values()
				   .stream())
		    .forEach(x -> {
			    az[0] = x.apply(from, (T) az[0]);
		    });

		    */

		// this logic lets children override parents (you can use '_._' for a guarenteed unique namespace
		from.find(prop, from.upwards()).reduce(new IdempotencyMap<>(Box.BiFunctionOfBoxAnd.class), (a,b) -> {
			IdempotencyMap<Box.BiFunctionOfBoxAnd<T, T>> i = new IdempotencyMap<>(Box.BiFunctionOfBoxAnd.class);
			i.putAll(a);
			i.putAll(b);
			return i;
		}).values().stream().forEachOrdered(x -> {
			az[0] = x.apply(from, (T) az[0]);
		});
		return (T) az[0];
	}

	static public final Dict.Prop<IdempotencyMap<Supplier<Object>>> main = new Dict.Prop<IdempotencyMap<Supplier<Object>>>("main").toCannon()
																      .type()
																      .doc("`_.main.name = function(){...}` defines what happens when a box is 'called' (e.g. _()). If this isn't defined or is empty, the whole box is executed instead (and, should that result in `_.main` being defined, then that's called")
																      .autoConstructs(() -> new IdempotencyMap<>(Supplier.class));


	public static Object call(Box box, Object a, Object b) {
		Object call = call(box, main, b);
		return call;
	}

	private static Object call(Box box,Dict.Prop<IdempotencyMap<Supplier<Object>>> main, Object argMap) {
		Map<String, Object> undoMap = new LinkedHashMap<>();

		Object ret = null;

		boolean success = false;
		try {
			Map<?, ?> m = null;

			Class c = argMap == null ? null : argMap.getClass();

			if (argMap instanceof Map || argMap instanceof ScriptObject) {
				m = (Map<?, ?>) ScriptUtils.convert(argMap, Map.class);
				for (Map.Entry<?, ?> e : m.entrySet()) {
					Object was = box.properties.get(new Dict.Prop("" + e.getKey()));
					if (box.properties.has(new Dict.Prop("" + e.getKey()))) undoMap.put("" + e.getKey(), was);

					box.asMap_set("" + e.getKey(), e.getValue());
				}
			}
			else
			{
				Dict.Prop arg = new Dict.Prop("arg");
				if (box.properties.has(arg)) undoMap.put("arg", box.properties.get(arg));
				box.asMap_set("arg", argMap);
			}

			success = true;

			try {

				if (!box.properties.has(main) || box.properties.get(main)
									       .values()
									       .size() == 0) {
					if (Execution.context.get()
							     .stream()
							     .filter(x -> x == box)
							     .count() > 1 && !box.properties.isTrue(new Dict.Prop("recur"), false)) return null;

//					String code = box.properties.get(Execution.code);
					String code = DisabledRangeHelper.getStringWithDisabledRanges(box, Execution.code, "/* -- start -- ", "-- end -- */");
					if (code != null) box.first(Execution.execution)
							     .map(x -> x.support(box, Execution.code)).filter(x -> x!=null).ifPresent(x -> x
									      .executeAll(code, box));

				}

				Log.log("calllogic",()-> "about to reduce starting from " + box);

				IdempotencyMap<Supplier<Object>> map = box.find(main, box.upwards())
									  .reduce(new IdempotencyMap<Supplier<Object>>(Supplier.class), (a1, a2) -> {
										  Log.log("calllogic", ()->"reducing " + a1 + " " + a2);
										  IdempotencyMap<Supplier<Object>> q = new IdempotencyMap<>(Supplier.class);
										  q.putAll(a1);
										  q.putAll(a2);
										  return q;
									  });


				Log.log("calllogic", ()->"reduced upwards to get " + map);


				Object[] firstRet = {null};
				map.values()
				   .forEach(x -> {
					   Object r = x.get();
					   if (firstRet[0] == null) firstRet[0] = r;
				   });

				return firstRet[0];

			} finally {

				if (m!=null)
				for (Map.Entry<?, ?> e : m.entrySet()) {
					if (undoMap.containsKey("" + e.getKey())) box.properties.put(new Dict.Prop("" + e.getKey()), undoMap.get("" + e.getKey()));
					else box.properties.remove(new Dict.Prop("" + e.getKey()));

				}
			}

		} catch (UnsupportedOperationException e) {

		}


		return ret;
	}

	private static Object callFunction(Box box,Dict.Prop<IdempotencyMap<Function<Object, Object>>> main, Object argMap) {
		Map<String, Object> undoMap = new LinkedHashMap<>();


		Object ret = null;


		boolean success = false;
		try {
			Map<?, ?> m = (Map<?, ?>) ScriptUtils.convert(argMap, Map.class);
			for (Map.Entry<?, ?> e : m.entrySet()) {
				Object was = box.properties.get(new Dict.Prop("" + e.getKey()));
				if (box.properties.has(new Dict.Prop("" + e.getKey()))) undoMap.put("" + e.getKey(), was);

				box.asMap_set("" + e.getKey(), e.getValue());
			}
			success = true;

			try {

				if (!box.properties.has(main) || box.properties.get(main)
									       .values()
									       .size() == 0) {
					if (Execution.context.get()
							     .stream()
							     .filter(x -> x == box)
							     .count() > 1 && !box.properties.isTrue(new Dict.Prop("recur"), false)) return null;

//					String code = box.properties.get(Execution.code);
					String code = DisabledRangeHelper.getStringWithDisabledRanges(box, Execution.code, "/* -- start -- ", "-- end -- */");
					if (code != null) box.first(Execution.execution)
							     .ifPresent(x -> x.support(box, Execution.code)
									      .executeAll(code, box));

				}

				IdempotencyMap<Function<Object, Object>> map = box.find(main, box.upwards())
									  .reduce(new IdempotencyMap<Function<Object, Object>>(Function.class), (a1, a2) -> {
										  IdempotencyMap<Function<Object, Object>> q = new IdempotencyMap<>(Function.class);
										  q.putAll(a1);
										  q.putAll(a2);
										  return q;
									  });


				Object[] firstRet = {null};
				map.values()
				   .forEach(x -> {
					   firstRet[0] = x.apply(firstRet[0]);
				   });

				return firstRet[0];

			} finally {

				for (Map.Entry<?, ?> e : m.entrySet()) {
					if (undoMap.containsKey("" + e.getKey())) box.properties.put(new Dict.Prop("" + e.getKey()), undoMap.get("" + e.getKey()));
					else box.properties.remove(new Dict.Prop("" + e.getKey()));

				}
			}

		} catch (UnsupportedOperationException e) {

		}


		return ret;
	}
}
