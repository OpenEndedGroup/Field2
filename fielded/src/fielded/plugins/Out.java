package fielded.plugins;

import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Pair;
import fieldbox.boxes.Box;
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
												    .doc(" ... ");
	static public final Dict.Prop<IdempotencyMap<Function<Object, Object>>> outMap = new Dict.Prop<>("outMap").toCannon()
														  .type()
														  .doc(" ... ");
	static public final Dict.Prop<Out> __out = new Dict.Prop<>("__out").toCannon()
									   .type();

	Writer theWriter = new PrintWriter(System.out);
	Consumer<Pair<Box, Integer>> theLineOut;

	public Out(Box root_unused) {
		this.properties.put(__out, this);

		this.properties.put(out, x -> {
			StackTraceElement[] st = new Exception().getStackTrace();
			String uid = null;
			int uidLine = 0;
			for (StackTraceElement ee : st) {
				if (ee.getFileName()
				      .startsWith("bx[")) {
					Pattern c = Pattern.compile("bx\\[(.*?)\\]/(.*)");
					Matcher m = c.matcher(ee.getFileName());
					if (m.find()) {
						uid = m.group(2);
						uidLine = ee.getLineNumber();
						break;
					}
				}
			}

			if (uid != null) {
				theLineOut.accept(new Pair<>(find(uid), uidLine));
			}
			else
				theLineOut.accept(null);


			try {
				theWriter.append(convert(x));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return x;
		});

		this.properties.put(outMap, output.map);

		// setup some defaults

		output.map._put("Object", x -> {

			String groupName = "o" + output.joinContext();

			return "{HTML}<div class='maptable-entry'><b>[[__" + groupName + "__:" + safe(x.toString()) + " " + cheapSynax(
				    x.toString()) + " ]]</b>[[__" + groupName + "smaller__:" + shorten(x.getClass()) + " <span class='smaller'>" + shorten(x.getClass()) + "</span> ]]</div>";
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

			System.out.println("S ||" + s);

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

			System.out.println("S ||" + s);

			return s;
		});
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
		return s.replaceAll(" ", "_");
	}

	Pattern stuff = Pattern.compile("([+-]?(\\d+\\.)?\\d+)|([\\%\\$\\#\\@\\!\\^\\&\\(\\)\\[\\]\\{\\}\\'\\,\\.\\;\\:\\+\\-\\*])");

	private String cheapSynax(String s) {

//		s = stuff.matcher(s)
//			 .replaceAll((x) -> {
//				 String found = x.group(1);
//				 if (found != null && found.length() > 0) return "<span class='number'>" + found + "</span>";
//				 else {
//					 found = x.group(3);
//					 if (found != null && found.length() > 0) return "<span class='operator'>" + found + "</span>";
//				 }
//				 return x.group(0);
//			 });


		return s;

	}

	private String shorten(Class c) {
		String nn = c.getName();
		String[] p = nn.split("\\.");
		String shor = p[p.length - 1];
		return shor;
	}

	public Out setWriter(Writer theWriter, Consumer<Pair<Box, Integer>> lineNumber) {
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
			Pattern p = Pattern.compile("\\[\\[__(.+?)__:(.+?) (.+?)\\]\\]");
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
