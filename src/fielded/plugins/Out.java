package fielded.plugins;

import field.app.RunLoop;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Pair;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.execution.InverseDebugMapping;
import fieldbox.io.IO;
import fielded.boxbrowser.ObjectToHTML;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marc on 9/13/15.
 */
public class Out extends Box {
	ObjectToHTML output = new ObjectToHTML();
	static public final Dict.Prop<Function<Object, Object>> out = new Dict.Prop<Function<Object, Object>>("out").toCannon()
		.type()
		.doc("write an object to the editor window. This will attempt to map the object into a useful (HTML) view of it using routines in `_.outMap`. `_.out(...)` clears any previous output from any previous update cycle.");
	static public final Dict.Prop<Function<Object, Object>> outClear = new Dict.Prop<Function<Object, Object>>("outClear").toCannon()
		.type()
		.doc("write an object to the editor window. This will attempt to map the object into a useful (HTML) view of it using routines in `_.outMap`. `_.outClear(...)` clears any previous output.");
	static public final Dict.Prop<Function<Object, Object>> outAppend = new Dict.Prop<Function<Object, Object>>("outAppend").toCannon()
		.type()
		.doc("write an object to the editor window. This will attempt to map the object into a useful (HTML) view of it using routines in `_.outMap`. `_.outAppend(...)` appends to any previous output.");

	static public final Dict.Prop<IdempotencyMap<Function<Object, Object>>> outMap = new Dict.Prop<>("outMap").toCannon()
		.type()
		.doc("a map of functions that can be called upon to transform objects to HTML for the purposes of `_.out(...)`. Objects are transformed until a string starting with `{HTML`} is returned, or until no transformation changes anything.");
	static public final Dict.Prop<Out> __out = new Dict.Prop<>("__out").toCannon()
		.type();

	Writer theWriter = new PrintWriter(System.out);
	Consumer<Triple<Box, Integer, Boolean>> theLineOut;
	Pattern c = Pattern.compile("bx\\[(.*?)\\]/(.*)");

	long t = 0;

	public Out(Box root_unused) {
		this.properties.put(__out, this);

		this.properties.put(out, x -> {
			if (t == RunLoop.tick)
				return doOutput(x, true);
			else {
				t = RunLoop.tick;
				return doOutput(x, false);
			}
		});
		this.properties.put(outClear, x -> {
			return doOutput(x, false);
		});
		this.properties.put(outAppend, x -> {
			return doOutput(x, true);
		});

		this.properties.put(outMap, output.map);

		// setup some defaults

		output.map._put("Object", x -> {

			String groupName = "o" + output.joinContext();

			String found = InverseDebugMapping.describe(x);
			if (found.startsWith(":")) found = found.substring(1);
			if (found.length() > 0) found = "'<i>" + found + "</i>'";

			return "{HTML}<div class='maptable-entry'><b>[[__" + groupName + "__:" + safe(
				x.toString()) + " " + found + " " + (x.toString()) + " ]]</b>[[__" + groupName + "smaller__:" + shorten(x.getClass()) + " <span class='smaller'>" + shorten(
				nonAnonymous(x.getClass())) + "</span> ]]</div>";
		});

		output.map._put("field_nashorn_api_scripting_ScriptObjectMirror", x -> {

			jdk.nashorn.api.scripting.ScriptObjectMirror xx = (jdk.nashorn.api.scripting.ScriptObjectMirror) x;
			if (xx.isFunction()) {
				String found = InverseDebugMapping.describe(xx);

				return "{HTML}<div class='maptable-entry'><b>function" + found + "</b></div>";
			}
			return x;
		});

		output.map._put("java_util_List", x -> {
			String s = "{HTML}";
			if (((List) x).size() == 0) {
				s += "<table class='maptable' cellspacing=0>";
				s += "<i>Empty List of class " + shorten(x.getClass()) + "</i>";
				s += "</table>";
				return s;
			}
			List k = ((List) x);

			s += "<div class='maptable' cellspacing=0><div class='smaller-inframe'>" + shorten(x.getClass()) + ", length " + k.size() + "</div>";
			int num = 0;
			for (Object oo : k) {
				s += "<div class='maptable-entry'> <div class='maptable-value'>" + output.convert(oo, "value") + "</div></div>";
				num++;
				if (num > 10 && k.size() > 15) {
					s += "<div class='maptable-entry'><div class='maptable-key'> ... </div> <div class='maptable-value'> " + num + "/" + k.size() + " total </div></div>";
					break;
				}

			}
			s += "</div>";

			return s;
		});

		output.map._put("Java_Util_Map", x -> {
			String s = "{HTML}";

			if (((Map) x).size() == 0) {
				s += "<table class='maptable' cellspacing=0>";
				s += "<i>Empty Map of class " + shorten(x.getClass()) + "</i>";
				s += "</table>";
				return s;
			}
			Set k = ((Map) x).keySet();

			s += "<div class='maptable' cellspacing=0><div class='smaller-inframe'>" + shorten(x.getClass()) + ", length " + k.size() + "</div>";
			int num = 0;
			for (Object oo : k) {
				s += "<div class='maptable-entry'> <div class='maptable-key'>" + output.convert(oo, "key") + "</div><div class='maptable-value'>" + output.convert(((Map) x).get(oo),
					"value") + "</div></div>";
				num++;
				if (num > 10 && k.size() > 15) {
					s += "<div class='maptable-entry'><div class='maptable-key'> ... </div> <div class='maptable-value'> " + num + "/" + k.size() + " total </div></div>";
					break;
				}

			}
			s += "</div>";

			return s;
		});
	}

	public Object doOutput(Object x, boolean append) {
		StackTraceElement[] st = new Exception().getStackTrace();
		String uid = null;
		int uidLine = 0;
		for (StackTraceElement ee : st) {
			if (ee.getFileName() != null && ee.getFileName().startsWith("bx[")) {
				Matcher m = c.matcher(ee.getFileName());
				if (m.find()) {
					uid = m.group(2);
					uidLine = ee.getLineNumber();
					break;
				}
			}
		}

		if (uid != null) {
			theLineOut.accept(new Triple<>(find(uid), uidLine, append));
		} else theLineOut.accept(null);


		try {
			theWriter.append(convert(x));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return x;
	}

	private Class nonAnonymous(Class a) {
		if (!a.isAnonymousClass()) return a;
		while (a != null && a.getName()
			.contains("$")) a = a.getSuperclass();

		return a;
	}


	private Box find(String uid) {
		return this.breadthFirst(this.both())
			.filter(x -> x.properties.has(IO.id))
			.filter(x -> x.properties.get(IO.id)
				.equals(uid))
			.findFirst()
			.orElse(null);
	}

	private String safe(String s) {
		return s.trim()
			.replaceAll(" ", "_")
			.replaceAll("\n", "_");
	}

	Pattern stuff = Pattern.compile("([+-]?(\\d+\\.)?\\d+)|([\\%\\$\\#\\@\\!\\^\\&\\(\\)\\[\\]\\{\\}\\'\\,\\.\\;\\:\\+\\-\\*])");

	private String cheapSynax(String s) {
		return s;
	}

	private String shorten(Class c) {
		String nn = c.getName();
		String[] p = nn.split("\\.");
		String shor = p[p.length - 1];
		return shor;
	}

	public Out setWriter(Writer theWriter, Consumer<Triple<Box, Integer, Boolean>> lineNumber) {
		this.theWriter = theWriter;
		this.theLineOut = lineNumber;
		return this;
	}

	public String convert(Object x) {
		return elideGroups(output.convert(x));
	}


	private String elideGroups(String s) {
		// replace subsequent sequential [[__groupName__:value foo foo]] with nothing; otherwise replace it with "foo foo"
		// we don't support nesting here

		boolean found = false;
		Map<String, String> prev = new LinkedHashMap<>();

		do {
			Pattern p = Pattern.compile("\\[\\[__(.+?)__:(.+?) (.+?)\\]\\]", Pattern.DOTALL);
			Matcher m = p.matcher(s);
			if (m.find()) {
				String groupName = m.group(1);
				String groupValue = m.group(2);
				String groupPayload = m.group(3);

				if (!prev.containsKey(groupName) || !prev.get(groupName)
					.equals(groupValue)) {
					prev.put(groupName, groupValue);
					s = s.substring(0, m.start()) + groupPayload + s.substring(m.end());
					continue;
				} else {
					prev.put(groupName, groupValue);
					s = s.substring(0, m.start()) + "<span class='smaller'>.</span>" + s.substring(m.end());
					continue;
				}
			}
			break;
		} while (true);

		return s;

	}
}
