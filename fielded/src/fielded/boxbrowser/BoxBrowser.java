package fielded.boxbrowser;

import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fielded.ServerSupport;
import fielded.webserver.NanoHTTPD;
import fielded.webserver.Server;
import org.pegdown.Extensions;
import org.pegdown.LinkRenderer;
import org.pegdown.Parser;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.AutoLinkNode;
import org.pegdown.ast.WikiLinkNode;
import org.pegdown.plugins.PegDownPlugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * In progress
 */
public class BoxBrowser extends Box implements IO.Loaded {

	static public final String NAMED = "/named/";
	static public final String ID = "/id/";
	private final Box root;
	private Server s;

	PegDownProcessor proc = new PegDownProcessor(1000, Extensions.WIKILINKS);

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
											       .equals(pieces[0]), pieces.length>1 ? pieces[1] : null);
			} else if (uri.startsWith(ID)) {
				uri = uri.substring(ID.length());
				String[] pieces = uri.split("/");
				return handleBy(x -> x.properties.has(IO.id) && x.properties.get(IO.id)
											       .equals(pieces[0]), pieces.length>1 ? pieces[1] : null);
			} else return null;
		});
	}

	static public final String preamble
		    = "<html xmlns='http://www.w3.org/1999/xhtml'> <meta charset='UTF-8'><head>" +
		    "<script src='/field/filesystem/codemirror-4.4/lib/codemirror.js'></script>"+
		    "<link rel='stylesheet' href='/field/filesystem/codemirror-4.4/lib/codemirror.css'>" +
		    "<link rel='stylesheet' href='/field/filesystem/codemirror-4.4/theme/default.css'>"+
		    //"<link rel='stylesheet' href='/field/filesystem/field-codemirror.css'>" +
		    "<link rel='stylesheet' href='/field/filesystem/field-boxbrowser.css' type='text/css'>" +
		    "<script src='/field/filesystem/codemirror-4.4/mode/javascript/javascript.js'></script>"+
		    "<script src='/field/filesystem/jquery-2.1.0.min.js'></script>"+
		    "</head><body>";
	static public final String postamble = "</body>";

	private NanoHTTPD.Response handleBy(Predicate<Box> b, String propName) {

		Optional<Box> first = root.breadthFirst(root.both())
					  .filter(b)
					  .findFirst();

		if (!first.isPresent()) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, null, "can't find box matching request");

		propName =propName==null ?  Execution.code.getName() : propName;

		return handleBox(first.get(), new Dict.Prop<Object>(propName));

	}

	private NanoHTTPD.Response handleBox(Box box, Dict.Prop<Object> property) {
		if (!box.properties.has(property)) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, preamble + "<p>no property called "+proc.markdownToHtml("`"+property.getName()+"`") + "</p>"+postamble);

		String source = box.properties.get(property) + "";

		return render(box, source, property);
	}

	class Section {
		String a;
		int comment = 0;
	}

	private NanoHTTPD.Response render(Box box, String source, Dict.Prop<Object> property) {
		// extract comment blocks and render as markdown

		// todo:
		String commentStart = "/*";
		String commentEnd = "*/";

		String[] ss = source.split("\n");

		List<Section> sections = new ArrayList<>();
		Section s = new Section();
		s.a = "";
		sections.add(s);

		for (int i = 0; i < ss.length; i++) {

			String z = ss[i]/*.trim()*/;
			if (z.equals(commentStart)) {
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
//			return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NO_CONTENT, null, "can't find a property called " + property.getName() + " in box " + box);
			return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, preamble + "<p>nothing in property called "+proc.markdownToHtml("`"+property.getName()+"`") + "</p>"+postamble);
		}


		String o = sections.stream()
				   .filter(x -> x.a.trim()
						   .length() > 0)
				   .map(x -> render(box, property, x))
				   .reduce((a, b) -> a + b)
				   .get();

		return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, preamble + o + postamble);

	}

	int cn = 0;
	private String render(Box box, Dict.Prop<Object> property, Section x) {
		if (x.comment > 0) {

			String h = proc.markdownToHtml(x.a, new LinkRenderer() {
				@Override
				public Rendering render(WikiLinkNode node) {

					// interpose awesome here (evening sketch)

					return super.render(node);
				}
			});

			return "<p>" + h + "</p>";
		} else {
			cn++;

			System.out.println(" Text in section is ||"+x.a.trim()+"||");

			return "<textarea readonly class='ta_"+(cn)+"'>"+x.a.trim()+"</textarea>"
				    +
				    "<script language='javascript'>CodeMirror.fromTextArea($('.ta_"+cn+"')[0], {viewportMargin:Infinity, mode:'javascript', readOnly:true})</script>";
		}
	}
}
