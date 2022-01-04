package fielded.boxbrowser;

import com.google.common.collect.MapMaker;
import com.thoughtworks.qdox.model.JavaClass;
import field.utility.Dict;
import field.utility.MarkdownToHTML;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.execution.Errors;
import fieldbox.execution.Execution;
import fieldbox.execution.JavaSupport;
import fieldbox.io.IO;
import fielded.ServerSupport;
import fielded.webserver.NanoHTTPD;
import fielded.webserver.Server;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * In progress
 * <p>
 * We should trawl properties for an "info" attribute, and then use that to render html into collapsable sections. Those sections should remember that they've collapsed.
 */
public class BoxBrowser extends Box implements IO.Loaded {

	static public final String NAMED = "/named/";
	static public final String ID = "/id/";
	static public final String DOCUMENTATION = "/doc/";

	private final Box root;
	private Server s;


	public interface HasMarkdownInformation {
		String generateMarkdown(Box inside, Dict.Prop property);
	}

	static public final Dict.Prop<BiFunction<Box, Object, String>> toMarkdown = new Dict.Prop<>("toMarkdown").set(Dict.domain, "*/attributes")
		.doc("function that takes a (box, value) and returns a markdown string that describes that box");
	static public final Dict.Prop<BiFunction<Box, Object, String>> toHTML = new Dict.Prop<>("toHTML").set(Dict.domain, "*/attributes")
		.doc("function that takes a (box, value) and returns a HTML string that describes that box");

	static public Map<String, Object> knownObjects = new MapMaker().weakKeys().weakValues().makeMap();

	public BoxBrowser(Box root) {
		this.root = root;
	}

	public void loaded() {

		this.s = root.find(ServerSupport.server, this.both())
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("need server for boxbrowser"));

		s.addDocumentRoot(fieldagent.Main.app + "/fieldbox/resources/");
		s.addURIHandler((uri, method, headers, params, files) -> {
			if (uri.startsWith(NAMED)) {
				uri = uri.substring(NAMED.length());
				String[] pieces = uri.split("/");
				return handleBy(x -> x.properties.has(Box.name) && x.properties.get(Box.name)
					.equals(pieces[0]), pieces.length > 1 ? pieces[1] : null);
			} else if (uri.startsWith(ID)) {
				uri = uri.substring(ID.length());
				String[] pieces = uri.split("/");
				return handleBy(x -> x.properties.has(IO.id) && x.properties.get(IO.id)
					.equals(pieces[0]), pieces.length > 1 ? pieces[1] : null);
			} else if (uri.startsWith(DOCUMENTATION)) {
				uri = uri.substring(DOCUMENTATION.length());
				String[] pieces = uri.split("/");
				Object object = knownObjects.get(pieces[0]);
				if (object != null) {
					return handleObject(object, pieces.length > 1 ? pieces[1] : null);
				}
			}
			return null;
		});
	}

	public NanoHTTPD.Response handleObject(Object object, String extraParameter) {
		List<Pair<String, String>> sections = objectToSections(object, extraParameter);

		return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, preamble + sections.stream()
			.map(x -> "<div class='grouped'><h1 id='section_" + x.first + "'>" + x.first + "</h1><div id='section_" + x.first + "''>" + x.second + "</div></div>")
			.reduce((a, b) -> a + "<BR>" + b)
			.orElse("") + postamble);
	}

	private List<Pair<String, String>> objectToSections(Object object, String extraParameter) {
		Class<?> c = object.getClass();
		List<Pair<String, String>> sections = new ArrayList<>();
		Set<Class> done = new LinkedHashSet<>();
		while (c != null) {
			if (!done.contains(c)) {
				objectToSections(object, c, extraParameter, sections);
				Class<?>[] ii = c.getInterfaces();
				for (Class iii : ii) {
					if (!done.contains(iii)) {
						done.add(iii);
						objectToSections(object, iii, extraParameter, sections);
					}
				}
			}
			c = c.getSuperclass();
		}
		return sections;
	}

	private void objectToSections(Object object, Class<?> of, String extraParameter, List<Pair<String, String>> into) {
		JavaClass jc = JavaSupport.javaSupport.sourceForClass(of);

	}

	public final String preamble = "<html xmlns='http://www.w3.org/1999/xhtml'> <meta charset='UTF-8'><head>" +
		"<script src='/field/filesystem/codemirror-5.25.2/lib/codemirror.js'></script>" +
		"<link rel='stylesheet' href='/field/filesystem/codemirror-5.25.2/lib/codemirror.css'>" +
		"<link rel='stylesheet' href='/field/filesystem/codemirror-5.25.2/theme/default.css'>" +
		"<link rel='stylesheet' href='/field/filesystem/field-codemirror.css' type='text/css'>" +
		"<link rel='stylesheet' href='/field/filesystem/field-boxbrowser.css' type='text/css'>" +
		"<script src='/field/filesystem/codemirror-5.25.2/mode/javascript/javascript.js'></script>" +
		"<script src='/field/filesystem/jquery-2.1.0.min.js'></script>" +
		"<script src='/field/filesystem/field-boxbrowser.js'></script>" +
		"</head><body><div class='all'>";
	static public final String postamble = "</div></body>";

	private NanoHTTPD.Response handleBy(Predicate<Box> b, String propName) {

		Optional<Box> first = root.breadthFirst(root.both())
			.filter(b)
			.findFirst();

		if (!first.isPresent())
			return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, null, "can't find box matching request");

		if (propName != null) return handleBox(first.get(), new Dict.Prop<>(propName));
		else return handleBoxAllProperties(first.get());
	}

	private NanoHTTPD.Response handleBoxAllProperties(Box box) {

		LinkedHashSet<Dict.Prop> props = new LinkedHashSet<Dict.Prop>();
		List<Pair<String, String>> sections = new ArrayList<>();

		box.breadthFirst(upwards())
			.forEach(x -> {
				for (Dict.Prop p : x.properties.getMap()
					.keySet()) {
					if (!props.contains(p) && hasInfo(x, p, x.properties.get(p))) {
						sections.add(new Pair<>(p.getName(), render(x, x.properties.get(p), p)));
						props.add(p);
					}

					if (x == box && x.properties.get(p) instanceof HasMarkdownInformation) {
						sections.add(new Pair<>(p.getName(), render(x, x.properties.get(p), p)));
						props.add(p);
					}
				}
			});

		return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, preamble + sections.stream()
			.map(x -> "<div class='grouped'><h1 id='section_" + x.first + "'>" + "_." + x.first + "</h1><div id='section_" + x.first + "''>" + x.second + "</div></div>")
			.reduce((a, b) -> a + "<BR>" + b)
			.orElseGet(() -> "") + postamble);
	}

	private boolean hasInfo(Box x, Dict.Prop p, Object value) {

		if (p.equals(Execution.code) && x.properties.has(p)) return true;

		return p.getAttributes()
			.has(toMarkdown);

	}

	private NanoHTTPD.Response handleBox(Box box, Dict.Prop<Object> property) {
		if (!box.properties.has(property)) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null,
			preamble + "<p>no property called " + MarkdownToHTML.convert("`" + property.getName() + "`") + "</p>" + postamble);

		return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, preamble + render(box, box.properties.get(property), property) + postamble);
	}

	class Section {
		String a;
		int comment = 0;
	}

	private String render(Box box, Object source, Dict.Prop property) {
		// extract comment blocks and render as markdown

		if (property.getAttributes()
			.has(toMarkdown)) {
			String md = property.getAttributes()
				.get(toMarkdown)
				.apply(box, source);

			String html = "<p>" + Errors.INSTANCE.handle(() -> MarkdownToHTML.convert(md), t -> "A error was thrown while rendering this property as markdown: <code>" + t + "</code>") + "</p>";
			return html;
		}
		if (property.getAttributes()
			.has(toHTML)) {
			String md = property.getAttributes()
				.get(toHTML)
				.apply(box, source);

			String html = md;
			return html;
		}

		if (source instanceof HasMarkdownInformation) {
			String html = "<p>" + Errors.INSTANCE.handle(() -> ((HasMarkdownInformation) source).generateMarkdown(box, property), t -> "An error was thrown while rendering this object as HTML: <code>" + t + "</code>") + "</p>";
			return html;
		}

		// todo, these are language specific
		String commentStart = "/*";
		String commentEnd = "*/";

		String[] ss = ("" + source).split("\n");

		List<Section> sections = new ArrayList<>();
		Section s = new Section();
		s.a = "";
		sections.add(s);

		for (int i = 0; i < ss.length; i++) {

			String z = ss[i]/*.trim()*/;
			if (z.startsWith("//")) {
				Section s2 = new Section();
				s2.a = z.substring(2);
				s2.comment = s.comment + 1;
				sections.add(s2);
				s2 = new Section();
				s2.a = "";
				s2.comment = s.comment;
				sections.add(s2);
				s = s2;
			} else if (z.equals(commentStart)) {
				Section s2 = new Section();
				s2.a = "";
				s2.comment = s.comment + 1;
				sections.add(s2);
				s = s2;
			} else if (z.equals(commentEnd)) {
				Section s2 = new Section();
				s2.a = "";
				s2.comment = s.comment - 1;
				sections.add(s2);
				s = s2;
			} else {
				s.a += "\n" + z;
			}
		}

		if (sections.size() == 0) {
			return "<p>nothing in property called " + MarkdownToHTML.convert("`" + property.getName() + "`") + "</p>";
		}

		String o = "" + sections.stream()
			.filter(x -> x.a.trim()
				.length() > 0)
			.map(x -> render(box, property, x))
			.reduce((a, b) -> ("" + a) + ("" + b))
			.get();

		o = o.replaceAll("<p></p>", "");

		return o;
	}

	int cn = 0;

	private String render(Box box, Dict.Prop<Object> property, Section x) {
		if (x.comment > 0) {
			String h = MarkdownToHTML.convert(ensureSentenceCase(x.a));

			return h;
		} else {
			cn++;
			return "<textarea readonly class='ta_" + (cn) + "'>" + x.a.trim() + "</textarea>" +
				"<script language='javascript'>CodeMirror.fromTextArea($('.ta_" + cn + "')[0], {lineNumbers:true, lineWrapping:true, viewportMargin:Infinity, mode:'javascript', readOnly:true})</script>";
		}
	}

	private String ensureSentenceCase(String a) {
		a = a.trim();
		if (a.length() > 2) {
			a = a.substring(0, 1).toUpperCase() + a.substring(1);
		}
		return a;
	}

}
