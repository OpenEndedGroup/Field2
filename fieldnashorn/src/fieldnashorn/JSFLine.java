package fieldnashorn;

import field.graphics.FLine;
import field.utility.Dict;
import field.utility.Log;
import fieldnashorn.annotations.HiddenInAutocomplete;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.InvokeByName;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

import static field.utility.Log.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Experimental Javascript interface to FLine
 */
public class JSFLine extends FLine implements JSObject {

	@Override
	@HiddenInAutocomplete
	public Object call(Object o, Object... objects) {

		Log.log("jsinnards", "CALL called <" + o + "> <" + objects + ">");

		if (objects.length == 1) {
			if (objects[0] instanceof Map) {
				doMap((Map) objects[0]);
			}
			else
			{
				throw new IllegalArgumentException(" pass in a map to FLine to add attributes");
			}
		}
		else
			throw new IllegalArgumentException(" pass in a map to FLine to add attributes");


		return this;
	}

	private void doMap(Map m) {
		for (Map.Entry<Object, Object> o : ((Map<Object, Object>) m).entrySet()) {
			doSet("" + o.getKey(), o.getValue());
		}
	}

	private void doSet(String s, Object value) {
		if (s.endsWith("_")) {
			last().attributes.put(new Dict.Prop(s.substring(0, s.length() - 1)), value);
		} else {
			this.attributes.put(new Dict.Prop(s), value);
		}
	}

	@Override
	@HiddenInAutocomplete
	public Object newObject(Object... objects) {



		// should return a new FLine with a copy of this one in it with, optionally, some attributes changed

		return null;
	}

	@Override
	@HiddenInAutocomplete
	public Object eval(String s) {
		throw new UnsupportedOperationException("eval");
	}

	@Override
	@HiddenInAutocomplete
	public Object getMember(String s) {
		if (s.endsWith("_")) {
			return last().attributes.get(new Dict.Prop(s.substring(0, s.length() - 1)));
		} else {
			return this.attributes.get(new Dict.Prop(s));
		}
	}

	@Override
	@HiddenInAutocomplete
	public Object getSlot(int i) {
		return nodes.get(i);
	}

	@Override
	@HiddenInAutocomplete
	public boolean hasMember(String s) {
		return getMember(s)!=null;
	}

	@Override
	public boolean hasSlot(int i) {
		return i>=0 && i<nodes.size();
	}

	@Override
	@HiddenInAutocomplete
	public void removeMember(String s) {
		if (s.endsWith("_")) {
			last().attributes.remove(new Dict.Prop(s.substring(0, s.length() - 1)));
		} else {
			this.attributes.remove(new Dict.Prop(s));
		}
	}

	@Override
	@HiddenInAutocomplete
	public void setMember(String s, Object o) {
		doSet(s, o);
	}

	@Override
	@HiddenInAutocomplete
	public void setSlot(int i, Object o) {
		if (o instanceof Node)
		{
			nodes.set(i, ((Node)o));
		}
	}

	@Override
	@HiddenInAutocomplete
	public Set<String> keySet() {
		return null;
	}

	@Override
	@HiddenInAutocomplete
	public Collection<Object> values() {
		return null;
	}

	@Override
	@HiddenInAutocomplete
	public boolean isInstance(Object o) {
		return false;
	}

	@Override
	@HiddenInAutocomplete
	public boolean isInstanceOf(Object o) {
		return false;
	}

	@Override
	@HiddenInAutocomplete
	public String getClassName() {
		return null;
	}

	@Override
	@HiddenInAutocomplete
	public boolean isFunction() {
		return false;
	}

	@Override
	@HiddenInAutocomplete
	public boolean isStrictFunction() {
		return false;
	}

	@Override
	@HiddenInAutocomplete
	public boolean isArray() {
		return false;
	}

	@Override
	@HiddenInAutocomplete
	public double toNumber() {
		return 0;
	}
}
