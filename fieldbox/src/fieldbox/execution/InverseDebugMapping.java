package fieldbox.execution;

import com.google.common.collect.MapMaker;
import field.app.RunLoop;
import field.graphics.Scene;
import field.utility.Dict;
import field.utility.Pair;
import fieldbox.boxes.Box;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * When you want to print debug information, like say "error in Shader" or "hello, I'm executing this function", it's important to be able to print something better than "...Shader@12312f3". The best thing to print is the name (or at lease _a_ name) of the variable that was used to create that shader.
 */
public class InverseDebugMapping {

	static public Box defaultRoot;// although this is an awful static reference to a Box root (which will destroy our ability to have multiple documents open), we want to be able to sprinkle huntForReferences around without knowing what graph we should be in. This is for debug information only.

	static public Map<Object, Pair<Box, String>> singleCache = new LinkedHashMap<>();
	{
		RunLoop.main.mainLoop.attach(-100, x -> {
			singleCache.clear();
		});
	}

	static public Map<Object, String> extraDescriptions= new MapMaker().weakKeys().makeMap();

	static public Pair<Box, String> huntForReference(Object of)
	{
		return huntForReference(of);
	}

	static public String describe(Object of)
	{
			Pair<Box, String> p = huntForReference(defaultRoot,of);
		String m = extraDescriptions.get(of);
		return (p==null ? "" : (":"+p.toString()))+(m==null ? "" : (":"+m));
	}

	static public void provideExtraInformation(Object of, String name)
	{
		String q = extraDescriptions.get(of);

		if (q==null)
		{
			extraDescriptions.put(of, name);
			return;
		}

		extraDescriptions.put(of, limit(q)+","+name);
	}

	private static String limit(String s) {
		if (s.length()>100) return s.substring(100);
		return s;
	}

	static public Pair<Box, String> huntForReference(Box startFrom, Object of)
	{
		Pair<Box, String> c = singleCache.get(of);
		if (c!=null && singleCache.containsKey(of)) return c;

		c = startFrom.breadthFirst(startFrom.both()).map(x -> {

			Map<Dict.Prop, Object> m = x.properties.getMap();
			for(Map.Entry<Dict.Prop, Object> e : m.entrySet())
			{
				if (e.getValue()==of)
				{
					return new Pair<Box, String>(x, e.getKey().getName())
					{
						@Override
						public String toString() {
							return "<b>"+second+"</b>@"+first;
						}
					};
				}
			}
			return null;

		}).filter(x -> x!=null).findFirst().orElse(null);
		singleCache.put(of, c);
		return c;
	}

	public static String describeWithToString(Object o) {
		String s1 = ""+o;
		String s2 = describe(o);

		if (s1.contains(s2))
			return s1;
		return s1+":"+s2;
	}
}
