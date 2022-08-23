package fielded.plugins;

import field.app.RunLoop;
import field.linalg.Mat4;
import field.utility.Dict;
import field.utility.IdempotencyMap;
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
	static public final Dict.Prop<Function<Object, Object>> out = new Dict.Prop<Function<Object, Object>>("out").toCanon()
		.type()
		.doc("write an object to the editor window. This will attempt to map the object into a useful (HTML) view of it using routines in `_.outMap`. `_.out(...)` clears any previous output from any previous update cycle.");
	static public final Dict.Prop<Function<Object, Object>> outClear = new Dict.Prop<Function<Object, Object>>("outClear").toCanon()
		.type()
		.doc("write an object to the editor window. This will attempt to map the object into a useful (HTML) view of it using routines in `_.outMap`. `_.outClear(...)` clears any previous output.");
	static public final Dict.Prop<Function<Object, Object>> outAppend = new Dict.Prop<Function<Object, Object>>("outAppend").toCanon()
		.type()
		.doc("write an object to the editor window. This will attempt to map the object into a useful (HTML) view of it using routines in `_.outMap`. `_.outAppend(...)` appends to any previous output.");

	static public final Dict.Prop<IdempotencyMap<Function<Object, Object>>> outMap = new Dict.Prop<>("outMap").toCanon()
		.type()
		.doc("a map of functions that can be called upon to transform objects to HTML for the purposes of `_.out(...)`. Objects are transformed until a string starting with `{HTML`} is returned, or until no transformation changes anything.");
	static public final Dict.Prop<Out> __out = new Dict.Prop<>("__out").toCanon()
		.type();

	Writer theWriter = new PrintWriter(System.out);
	Consumer<Triple<Box, Integer, Boolean>> theLineOut;
	Pattern c = Pattern.compile("bx\\[(.*?)\\]/(.*)");
	Pattern c2 = Pattern.compile("bx\\$(.*?)\\$");

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

		output.map._put("[F", x -> {
			String s = "{HTML}";
			if (((float[]) x).length == 0) {
				s += "<table class='maptable' cellspacing=0>";
				s += "<i>Empty Array of Floats" + shorten(x.getClass()) + "</i>";
				s += "</table>";
				return s;
			}
			float[] k = ((float[]) x);

			s += "<div class='maptable' cellspacing=0><div class='smaller-inframe'>Float Array, len " + k.length + "</div>";
			int num = 0;
			for (Float oo : k) {
				s += "<div class='maptable-entry'> <div class='maptable-value'>" + output.convert(oo, "value") + "</div></div>";
				num++;
				if (num > 10 && k.length > 15) {
					s += "<div class='maptable-entry'><div class='maptable-key'> ... </div> <div class='maptable-value'> " + num + "/" + k.length + " total </div></div>";
					break;
				}

			}
			s += "</div>";

			return s;
		});

		output.map._put("[I", x -> {
			String s = "{HTML}";
			if (((float[]) x).length == 0) {
				s += "<table class='maptable' cellspacing=0>";
				s += "<i>Empty Array of Ints" + shorten(x.getClass()) + "</i>";
				s += "</table>";
				return s;
			}
			int[] k = ((int[]) x);

			s += "<div class='maptable' cellspacing=0><div class='smaller-inframe'>Int Array, len " + k.length + "</div>";
			int num = 0;
			for (Integer oo : k) {
				s += "<div class='maptable-entry'> <div class='maptable-value'>" + output.convert(oo, "value") + "</div></div>";
				num++;
				if (num > 10 && k.length > 15) {
					s += "<div class='maptable-entry'><div class='maptable-key'> ... </div> <div class='maptable-value'> " + num + "/" + k.length + " total </div></div>";
					break;
				}

			}
			s += "</div>";

			return s;
		});

		output.map._put("[B", x -> {
			String s = "{HTML}";
			if (((float[]) x).length == 0) {
				s += "<table class='maptable' cellspacing=0>";
				s += "<i>Empty Array of Bytes" + shorten(x.getClass()) + "</i>";
				s += "</table>";
				return s;
			}
			byte[] k = ((byte[]) x);

			s += "<div class='maptable' cellspacing=0><div class='smaller-inframe'>Int Array, len " + k.length + "</div>";
			int num = 0;
			for (Byte oo : k) {
				s += "<div class='maptable-entry'> <div class='maptable-value'>" + output.convert(oo, "value") + "</div></div>";
				num++;
				if (num > 10 && k.length > 15) {
					s += "<div class='maptable-entry'><div class='maptable-key'> ... </div> <div class='maptable-value'> " + num + "/" + k.length + " total </div></div>";
					break;
				}

			}
			s += "</div>";

			return s;
		});

		output.map._put("[D", x -> {
			String s = "{HTML}";
			if (((double[]) x).length == 0) {
				s += "<table class='maptable' cellspacing=0>";
				s += "<i>Empty Array of Floats" + shorten(x.getClass()) + "</i>";
				s += "</table>";
				return s;
			}
			double[] k = ((double[]) x);

			s += "<div class='maptable' cellspacing=0><div class='smaller-inframe'>Double Array, len " + k.length + "</div>";
			int num = 0;
			for (Double oo : k) {
				s += "<div class='maptable-entry'> <div class='maptable-value'>" + output.convert(oo, "value") + "</div></div>";
				num++;
				if (num > 10 && k.length > 15) {
					s += "<div class='maptable-entry'><div class='maptable-key'> ... </div> <div class='maptable-value'> " + num + "/" + k.length + " total </div></div>";
					break;
				}

			}
			s += "</div>";

			return s;
		});

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

			org.openjdk.nashorn.api.scripting.ScriptObjectMirror xx = (org.openjdk.nashorn.api.scripting.ScriptObjectMirror) x;
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

			s += "<div class='maptable' cellspacing=0><div class='smaller-inframe'>" + shorten(x.getClass()) + ", len " + k.size() + "</div>";
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

			s += "<div class='maptable' cellspacing=0><div class='smaller-inframe'>" + shorten(x.getClass()) + ", len " + k.size() + "</div>";
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

		output.map._put("field_linalg_Mat4", x -> {
			String s = "{HTML}";
			Mat4 m = (Mat4) x;
			s += "<table class='mattable' cellspacing=0>";
			s += "<tr>";
			s += "<td>" + formatDouble(m.m00) + "</td>";
			s += "<td>" + formatDouble(m.m10) + "</td>";
			s += "<td>" + formatDouble(m.m20) + "</td>";
			s += "<td>" + formatDouble(m.m30) + "</td>";
			s += "</tr><tr>";
			s += "<td>" + formatDouble(m.m01) + "</td>";
			s += "<td>" + formatDouble(m.m11) + "</td>";
			s += "<td>" + formatDouble(m.m21) + "</td>";
			s += "<td>" + formatDouble(m.m31) + "</td>";
			s += "</tr><tr>";
			s += "<td>" + formatDouble(m.m02) + "</td>";
			s += "<td>" + formatDouble(m.m12) + "</td>";
			s += "<td>" + formatDouble(m.m22) + "</td>";
			s += "<td>" + formatDouble(m.m32) + "</td>";
			s += "</tr><tr>";
			s += "<td>" + formatDouble(m.m03) + "</td>";
			s += "<td>" + formatDouble(m.m13) + "</td>";
			s += "<td>" + formatDouble(m.m23) + "</td>";
			s += "<td>" + formatDouble(m.m33) + "</td>";
			s += "</tr></table>";
			return s;
		});


	}

	private String formatDouble(double d) {
		if (Math.abs(d) > 0.01f)
			return String.format("%.2f", d);
		else
			return String.format("%6.3e", d);
	}

	public Object doOutput(Object x, boolean append) {
		StackTraceElement[] st = new Exception().getStackTrace();
		String uid = null;
		int uidLine = 0;
		for (StackTraceElement ee : st) {
			System.out.println(ee.getFileName()+" "+ee.getClassName()+" "+ee.getMethodName()+" "+ee.getModuleName());
			if (ee.getFileName() != null && ee.getFileName().startsWith("bx[")) {
				Matcher m = c.matcher(ee.getFileName());
				if (m.find()) {
					uid = m.group(2);
					uidLine = ee.getLineNumber();
					break;
				}
			}
			if (ee.getFileName() != null && ee.getFileName().startsWith("bx$")) {
				Matcher m = c2.matcher(ee.getFileName());
				if (m.find()) {
					uid = m.group(1);
					uidLine = ee.getLineNumber();
					break;
				}
			}

		}

		System.out.println(" uid :"+uid);
		if (uid != null) {
			theLineOut.accept(new Triple<>(find(uid), uidLine, append));
		} else theLineOut.accept(null);


		try {
			String cc = convert(x);
			theWriter.append(cc);
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
		String[] p = nn.split("[\\.\\$]");
		String shor = p[p.length - 1];

		if (shor.equalsIgnoreCase("ScriptObjectMirror")) return "Object";

		return shor;
	}

	public Out setWriter(Writer theWriter, Consumer<Triple<Box, Integer, Boolean>> lineNumber) {
		this.theWriter = theWriter;
		this.theLineOut = lineNumber;
		return this;
	}

	public Consumer<Triple<Box, Integer, Boolean>> getLineOut() {
		return theLineOut;
	}

	public Writer getWriter() {
		return theWriter;
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
