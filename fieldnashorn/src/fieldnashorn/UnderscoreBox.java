package fieldnashorn;

import field.graphics.RunLoop;
import field.utility.Conversions;
import field.utility.Dict;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fielded.Execution;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.internal.objects.ScriptFunctionImpl;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

import javax.script.ScriptContext;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Nashorn/Javascript specific shim object to refer to the current box. In Field1 this was called _self. Here let's call it "_"
 * <p>
 * So, _ to talk about "this", the current box and _(something) as a notation to wrap a box. _() will execute all of the code in this box (watch for
 * loops!)
 * <p>
 * so: _.children()
 * <p>
 * property access:
 * <p>
 * _.name = "banana" _.children()[1].frame.x=20
 * <p>
 * type information is pulled out automatically, so conversions from lambdas to various SAM's are done automatically, see Conversions.java
 * <p>
 * completions and documentation is done based on the Dict.Prop framework
 * <p>
 * Todo: similar shim class for FLine drawing class
 */
public class UnderscoreBox extends AbstractJSObject implements JavaSupport.HandlesCompletion {

	private final Box at;
	private long tick;

	public UnderscoreBox(Box at) {
		this.at = at;
	}

	public List<UnderscoreBox> children() {
		Set<Box> c = at.children();
		return c.stream().map(x -> new UnderscoreBox(x)).collect(Collectors.toList());
	}

	public List<UnderscoreBox> parents() {
		Set<Box> c = at.parents();
		return c.stream().map(x -> new UnderscoreBox(x)).collect(Collectors.toList());
	}

	@Override
	public Object call(Object thiz, Object... args) {

		// TODO: allow array targets?
		if (args.length == 0) {
			ScriptContext sc = at.first(Nashorn.boxBindings).get();
			Object was = sc.getAttribute("_");
			try {
				sc.setAttribute("_", thiz, ScriptContext.GLOBAL_SCOPE);
				at.first(Execution.execution)
					    .ifPresent(x -> x.support(at, Execution.code).executeAll(at.first(Execution.code).get(), at));
			} finally {
				sc.setAttribute("_", was, ScriptContext.GLOBAL_SCOPE);
			}
			return null;
		} else if (args.length == 1) {
			if (args[0] instanceof Box) {
				return new UnderscoreBox((Box) args[0]);
			}
			throw new IllegalArgumentException();
		}
		throw new IllegalArgumentException(" can call _ with either 0 or 1 arguments. 0 executes all of the text in the box, 1 makes a new JavaScript wrapper for a box");
	}

	Set<String> exposed = new LinkedHashSet<>(Arrays.asList("children", "parents"));

	@Override
	public boolean hasMember(String m) {
		if (exposed.contains(m)) return true;
		if (at.find(new Dict.Prop(m), at.upwards()).findFirst().isPresent()) return true;
		return false;
	}

	@Override
	public Object getMember(String m) {

		if (m.equals("at")) return at;
		if (m.equals("box")) return at;
		if (m.equals("children")) return (Supplier<List<UnderscoreBox>>) this::children;
		if (m.equals("parents")) return (Supplier<List<UnderscoreBox>>) this::parents;

		Dict.Prop cannon = new Dict.Prop(m).toCannon();
		if (!cannon.isCannon()) {
			return at.find(new Dict.Prop(m), at.upwards()).findFirst().orElse(null);
		}

		Log.log("underscore.debug", " type information for cannon property "+m+" is " + cannon.getTypeInformation());
		Object ret = at.find(cannon, at.upwards()).findFirst().orElse(null);

		if (ret instanceof Box.FunctionOfBox) {
			return enunderscoreReturn((Supplier) (() -> ((Box.FunctionOfBox) ret).apply(at)));
		}

		return enunderscoreReturn(ret);
	}

	private Object enunderscoreReturn(Object ret) {
		if (ret ==null) return null;

//		if (ret instanceof Box) return new UnderscoreBox((Box) ret);

		// fancy subclasses of Box shouldn't be wrapped
		if (ret.getClass()==Box.class) return new UnderscoreBox((Box) ret);

		if (ret instanceof List) return ((List) ret).stream().map(this::enunderscoreReturn).collect(Collectors.toList());
		if (ret instanceof Set) return ((Set) ret).stream().map(this::enunderscoreReturn).collect(Collectors.toSet());
		return ret;
	}


	public void setMember(String name, Object value) {
		Log.log("underscore.debug", " underscore box set :" + name + " to " + value.getClass() + " <" + Function.class.getName() + ">");
		Dict.Prop cannon = new Dict.Prop(name).toCannon();

		Log.log("underscore.debug", " cannonical type information " + cannon.getTypeInformation());

		Object converted = convert(value, cannon.getTypeInformation());

		at.properties.put(cannon, converted);

		Log.log("underscore.debug", () -> {
			Log.log("underscore.debug", " PROPERTIES NOW :");
			for (Map.Entry<Dict.Prop, Object> q : at.properties.getMap().entrySet()) {
				try {
					Log.log("underscore.debug", "     " + q.getKey() + " = " + q.getValue());
				} catch (NullPointerException e) {
					//JDK bug JDK-8035426 --- sometimes Nashorn lambdas throw NPE's when they are .toString'd
				}
			}
			return null;
		});

		if (tick != RunLoop.tick) {
			Drawing.dirty(at);
			tick = RunLoop.tick;
		}
	}

	@Override
	public Set<String> keySet() {
		// all non private properties
		Set<String> s1 = at.breadthFirst(at.upwards()).map(x -> x.properties.getMap().keySet()).flatMap(x -> x.stream()).map(x -> x.getName())
			    .filter(x -> !x.startsWith("_")).collect(Collectors.toSet());

		// and all public methods
		Set<String> m1 = getAllPublicMethods();

		s1.addAll(m1);
		return s1;
	}

	protected Set<String> getAllPublicMethods() {
		Set<String> m1 = new LinkedHashSet<>();
		Method[] m = at.getClass().getDeclaredMethods();
		for (Method mm : m) {
			if (mm.isAccessible()) {
				m1.add(mm.getName());
			}
		}
		return m1;
	}

	@Override
	public String toString() {
		return at.toString();
	}

	@Override
	public List<Execution.Completion> getCompletionsFor(String prefix) {
		Set<String> apm = getAllPublicMethods();
		List<Execution.Completion> l1 = keySet().stream().filter(x -> x.startsWith(prefix)).sorted().map(x -> {
			Dict.Prop q = new Dict.Prop(x).findCannon();
			if (q == null) {
				return null;
			} else return new Execution.Completion(-1, -1, x, "<span class='type'>" + Conversions
				    .fold(q.getTypeInformation(), t -> compress(t)) + "</span> <span class='doc'>" + q
				    .getDocumentation() + "</span>");
		}).filter(x -> x != null).collect(Collectors.toList());

		List<Execution.Completion> l2 = JavaSupport.javaSupport.getCompletionsFor(at, prefix);

		l1.addAll(l2.stream().filter(x -> {
			for (Execution.Completion c : l1)
				if (c.replacewith.equals(x.replacewith)) return false;
			return true;
		}).collect(Collectors.toList()));
		return l1;
	}


	static public String compress(String signature) {
		signature = " " + signature;

		Pattern p = Pattern.compile("([A-Za-z]*?)[\\.\\$]([A-Za-z]*?)");
		Matcher m = p.matcher(signature);

		while (m.find()) {
			signature = m.replaceAll("$2");
			m = p.matcher(signature);
		}

		signature = signature.replace(" public ", " ");
		signature = signature.replace(" final ", " ");
		signature = signature.replace(" void ", " ");
		signature = signature.replace("  ", " ");
		signature = signature.replace("  ", " ");

		return signature.trim();
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
				return Collections.singletonMap("" + value + ":" + System.identityHashCode(value), convert(value, fit
					    .subList(2, fit.size())));
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
			StaticClass adapterClassFor = JavaAdapterFactory
				    .getAdapterClassFor(new Class[]{fit.get(0)}, (ScriptObject) value, MethodHandles.lookup());
			try {
				return adapterClassFor.getRepresentedClass().newInstance();
			} catch (InstantiationException e) {
				Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
			} catch (IllegalAccessException e) {
				Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
			}
		}

		return value;
	}

	public Box getAt() {
		return at;
	}
}
