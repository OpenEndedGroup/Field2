package fielded.plugins;

import field.utility.Log;
import fieldbox.boxes.Box;
import fielded.Execution;
import fieldnashorn.UnderscoreBox;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.json.JSONStringer;
import org.json.JSONWriter;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Completion helpers for BridgeToTextEditor, very much in the style of TernSupport
 */
public class BridgedTernSupport {

	public void inject(Consumer<String> engine) {
		List<String> s = Arrays
			    .asList(new String[]{"acorn.js", "acorn_loose.js", "walk.js", "defs.js", "signal.js", "infer.js", "tern.js", "comment.js", "condense.js"});

		Collection<File> f = s.stream().map(x -> new File(fieldagent.Main.app + "/fielded/external/tern/" + x)).collect(Collectors.toList());

		engine.accept("self = {}");
		engine.accept("self.tern = {}");
		engine.accept("self.acorn= {}");
		engine.accept("tern = {}");


		for (File ff : f) {
			engine.accept(readFile(ff.getAbsolutePath()));
		}

		engine.accept("__ecma5json=" + readFile(fieldagent.Main.app + "/fielded/external/tern/ecma5.json"));
		engine.accept("self.ternServer=new self.tern.Server({defs: [__ecma5json]})");
		engine.accept("delete __ecma5json");
	}

	private static String readFile(String s) {
		try (BufferedReader r = new BufferedReader(new FileReader(new File(s)))) {
			String line = "";
			while (r.ready()) {
				line += r.readLine() + "\n";
			}
			return line;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public List<Execution.Completion> completion(Consumer<String> engine, String boxName, String allText, int line, int ch) {
		List<Execution.Completion> r = new ArrayList<>();


		JSONStringer s = new JSONStringer();
		s.object();
		s.key("at").value(allText);
		s.endObject();

		engine.accept("__someFile=" + s.toString() + ".at");
		engine.accept("var __completions = []");
		engine.accept("self.ternServer.request({query:{type:\"completions\", types:true, docs:true, file:\"#0\", end:{line:" + line + ",ch:" + ch + "}}, \n" +
			    "files:[{type:\"full\",name:\"" + boxName + ".js\",text:__someFile}]},\n" +
			    "	function (e,r){\n" +
			    "		for(var i=0;i<r.completions.length;i++)" +
			    "			__completions = __completions.concat([[r.start, r.end, r.completions[i].name, r.completions[i].type]])" +
			    "	})");

		engine.accept("__extraCompletions = __completions");

		return r;
	}
}
