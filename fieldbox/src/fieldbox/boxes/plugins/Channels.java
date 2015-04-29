package fieldbox.boxes.plugins;

import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldlinker.Linker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Structure for properties that can be surfaced in the UI in various ways, including mapped onto relative 2d-input devices
 */
public class Channels extends Box {

	static public final Dict.Prop<Number> max = new Dict.Prop<Number>("max").toCannon().type();
	static public final Dict.Prop<Number> min = new Dict.Prop<Number>("min").toCannon().type();
	static public final Dict.Prop<Class> typeOf = new Dict.Prop<Class>("typeOf").toCannon().type();

	static public final Dict.Prop<FunctionOfBoxValued<List<String>>> channels = new Dict.Prop<>("channels").toCannon().type();

	static public class Info<T>
	{
		Dict attributes = new Dict();
		Dict.Prop<T> target;

		public Info(Dict.Prop<T> target)
		{
			this.target = target;
		}
	}

	static public class Infoer implements Linker.AsMap
	{
		protected final Box on;

		public Infoer(Box on) {
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
		public Object asMap_get(String s) {
			Dict.Prop p = new Dict.Prop(s);
			Box locatedAt = on.breadthFirst(on.upwards())
				    .filter(x -> x.properties.has(p))
				    .findFirst()
				    .orElseGet(() -> on);

			Dict.Prop p2 = new Dict.Prop("_"+s+"_info");

			return locatedAt.properties.computeIfAbsent(p2, x -> new Info(p));
		}

		@Override
		public Object asMap_set(String s, Object o) {
			Dict.Prop p = new Dict.Prop(s);
			Box locatedAt = on.breadthFirst(on.upwards())
					  .filter(x -> x.properties.has(p))
					  .findFirst()
					  .orElseGet(() -> on);

			Dict.Prop p2 = new Dict.Prop("_"+s+"_info");
			if (o instanceof Info)
			{
				return locatedAt.properties.put(p2, o);
			}
			else
			{
				throw new ClassCastException();
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

	static public final Dict.Prop<FunctionOfBoxValued<Infoer>> info = new Dict.Prop<Info>("info").type().toCannon().doc("returns an object that lets you set sidecar properties of properties, such as max, min, accessors etc. A property with a sidecar Dict is a 'channel'");

	public Channels(Box root)
	{
		properties.put(info, x -> new Infoer(x));
		properties.put(channels, x -> {

			ArrayList<String> a = new ArrayList<>();

			// any property of this box that there's a channel info for anywhere above it
			Map<Dict.Prop, Object> m = x.properties.getMap();
			m.entrySet().forEach(e -> {
				Object i = x.asMap_get("_" + e.getKey()
							      .getName() + "_info");
				if (i instanceof Info) {
					a.add(e.getKey()
					       .getName());
				}
			});


			return a;

		});
	}



}
