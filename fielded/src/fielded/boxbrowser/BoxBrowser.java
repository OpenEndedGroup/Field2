package fielded.boxbrowser;

import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fielded.ServerSupport;
import fielded.webserver.NanoHTTPD;
import fielded.webserver.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In progress
 */
public class BoxBrowser extends Box implements IO.Loaded {

	static public final String NAMED = "/named/";
	private final Box root;
	private Server s;

	public BoxBrowser(Box root) {
		this.root = root;
	}

	public void loaded() {

		this.s = root.find(ServerSupport.server, this.both())
			     .findFirst()
			     .orElseThrow(() -> new IllegalArgumentException("need server for boxbrowser"));

		s.addDocumentRoot(fieldagent.Main.app + "/fieldbox/resources/");
		s.addURIHandler((uri, method, headers, params, files) -> {
			System.out.println(" checking uri :<" + uri + ">");
			if (uri.startsWith(NAMED)) {
				uri = uri.substring(NAMED.length());
				String[] pieces = uri.split("/");
				return handleByName(pieces);
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

	private NanoHTTPD.Response handleByName(String[] pieces) {

		Optional<Box> first = root.breadthFirst(root.both())
					  .filter(x -> x.properties.has(Box.name))
					  .filter(x -> x.properties.get(Box.name)
								   .equals(pieces[0]))
					  .findFirst();
		if (!first.isPresent()) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, null, "can't find box by name " + pieces[0]);

		String propName = (pieces.length > 1) ? pieces[1] : Execution.code.getName();

		return handleBox(first.get(), new Dict.Prop<Object>(propName));

	}

	private NanoHTTPD.Response handleBox(Box box, Dict.Prop<Object> property) {
		if (!box.properties.has(property)) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, null, "can't find a property called " + property.getName() + " in box " + box);

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

			String z = ss[i].trim();
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

		if (sections.size() == 0) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NO_CONTENT, null, "can't find a property called " + property.getName() + " in box " + box);


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
			return "<p>" + x.a + "</p>";
		} else {
			cn++;
			return "<textarea readonly class='ta_"+(cn)+"'>"+x.a.trim()+"</textarea>"
				    +
				    "<script language='javascript'>CodeMirror.fromTextArea($('.ta_"+cn+"')[0], {viewportMargin:Infinity, mode:'javascript'})</script>";
		}
	}
}
