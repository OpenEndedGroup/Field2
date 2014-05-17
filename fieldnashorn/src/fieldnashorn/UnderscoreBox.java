package fieldnashorn;

import field.graphics.RunLoop;
import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fielded.Execution;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.internal.objects.ScriptFunctionImpl;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by marc on 4/16/14.
 */
public class UnderscoreBox extends AbstractJSObject implements JavaSupport.HandlesCompletion {

	private final Box at;
	private long tick;

	public UnderscoreBox(Box at) {
		this.at = at;
	}

	@Override
	public boolean hasMember(String m) {
		return at.find(new Dict.Prop(m), at.upwards()).findFirst().isPresent();
	}

	@Override
	public Object getMember(String m) {

		if (m.equals("at")) return at;

		Dict.Prop cannon = new Dict.Prop(m).toCannon();
		if (!cannon.isCannon()) {
			return at.find(new Dict.Prop(m), at.upwards()).findFirst().orElse(null);
		}

		System.out.println(" type information for cannon property is " + cannon.getTypeInformation());
		return at.find(cannon, at.upwards()).findFirst().orElse(null);
	}


	public void setMember(String name, Object value) {
		System.out.println(" underscore box set :" + name + " to " + value.getClass() + " <" + Function.class.getName() + ">");
		Dict.Prop cannon = new Dict.Prop(name).toCannon();

		System.out.println(" cannonical type information " + cannon.getTypeInformation());

		Object converted = convert(value, cannon.getTypeInformation());

		at.properties.put(cannon, converted);

		System.out.println(" PROPERTIES NOW :");
		for(Map.Entry<Dict.Prop, Object> q : at.properties.getMap().entrySet())
		{
			System.out.println("     "+q.getKey()+" = "+q.getValue());
		}


		if (tick != RunLoop.tick) {
			Drawing.dirty(at);
			tick = RunLoop.tick;
		}
	}

	@Override
	public Set<String> keySet() {
		// all non private properties
		return at.breadthFirst(at.upwards()).map(x -> x.properties.getMap().keySet()).flatMap(x -> x.stream()).map(x -> x.getName()).filter(x -> !x.startsWith("_")).collect(Collectors.toSet());
	}

	@Override
	public String toString() {
		return at.toString();
	}

	@Override
	public List<Execution.Completion> getCompletionsFor(String prefix) {
		// todo: documentation and type information via canonicalization framework
		return keySet().stream().filter(x -> x.startsWith(prefix)).sorted().map(x -> new Execution.Completion(-1, -1, x, "")).collect(Collectors.toList());
	}


	public Object convert(Object value, List<Class> fit) {
		if (fit == null) return value;
		if (fit.get(0).isInstance(value)) return value;

		// promote non-arrays to arrays
		if (List.class.isAssignableFrom(fit.get(0))) {
			if (!(value instanceof List)) {
				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
			} else {
				return value;
			}
		} else if (Map.class.isAssignableFrom(fit.get(0)) && String.class.isAssignableFrom(fit.get(1))) {
			// promote non-Map<String, V> to Map<String, V>
			if (!(value instanceof Map)) {
				return Collections.singletonMap("" + value + ":" + System.identityHashCode(value), convert(value, fit.subList(2, fit.size())));
			} else {
				return value;
			}

		} else if (Collection.class.isAssignableFrom(fit.get(0))) {
			if (!(value instanceof Collection)) {
				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
			} else {
				return value;
			}

		}


		if (value instanceof ScriptFunctionImpl) {
			StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{fit.get(0)}, (ScriptObject) value, MethodHandles.lookup());
			try {
				return adapterClassFor.getRepresentedClass().newInstance();
			} catch (InstantiationException e) {
				System.err.println(" problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0));
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				System.err.println(" problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0));
				e.printStackTrace();
			}
		}

		return value;
	}

	public Box getAt() {
		return at;
	}
}
