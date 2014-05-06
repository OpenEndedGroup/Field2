package fieldnashorn;

import field.graphics.RunLoop;
import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fielded.Execution;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.internal.objects.NativeJava;
import jdk.nashorn.internal.objects.ScriptFunctionImpl;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
		return at.find(new Dict.Prop(m), at.upwards()).findFirst().orElse(null);
	}

	public void setMember(String name, Object value) {

		System.out.println(" underscore box set :" + name + " to " + value.getClass()+" <"+Function.class.getName()+">");

		if (value instanceof ScriptFunctionImpl)
		{

			StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{Function.class}, (ScriptObject) value, MethodHandles.lookup());
			try {
				System.out.println("MAGIC? :" + ((Function) adapterClassFor.getRepresentedClass().newInstance()).apply(10));
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}



		at.properties.put(new Dict.Prop(name), value);
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
}
