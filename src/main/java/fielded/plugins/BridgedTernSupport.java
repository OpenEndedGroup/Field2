package fielded.plugins;

import field.utility.Log;
import fieldagent.Main;
import fieldbox.execution.Completion;
import fielded.ServerSupport;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Completion helpers for BridgeToTextEditor, very much in the style of TernSupport
 */
public class BridgedTernSupport {

	public void inject(Consumer<String> engine, boolean doAllPlaylist) {
		List<String> s = Arrays.asList("acorn.js", "acorn_loose.js", "walk.js", "defs.js", "signal.js", "infer.js", "tern.js", "comment.js", "condense.js");

		Collection<File> f = s.stream()
//			.map(x -> new File(fieldagent.Main.app + "/modules/fieldcore/resources/tern/" + x))
			.map(x -> new File(fieldagent.Main.app + "/lib/web/tern/" + x))
				      .collect(Collectors.toList());

		engine.accept("self = {}");
		engine.accept("self.tern = {}");
		engine.accept("self.acorn= {}");
		engine.accept("tern = {}");


		for (File ff : f) {
			engine.accept(readFile(ff.getAbsolutePath()));
		}

//		engine.accept("__ecma5json=" + readFile(fieldagent.Main.app + "/modules/fieldcore/resources/tern/ecma5.json"));
		engine.accept("__ecma5json=" + readFile(fieldagent.Main.app + "/lib/web/tern/ecma5.json"));
		engine.accept("self.ternServer=new self.tern.Server({defs: [__ecma5json]})");
		engine.accept("delete __ecma5json");


		if (doAllPlaylist)
		for (String name : ServerSupport.playlist) {
			JSONStringer j = new JSONStringer();
//			Log.log("TERN", " quoting ");

			j.object()
			 .key("at")
//				.value(readFile(Main.app + "modules/fieldcore/resources/" + name))
				.value(readFile(Main.app + "lib/web/" + name))
			 .endObject();

//			Log.log("TERN", " injecting code mirror source file <" + name + ">");
			engine.accept("self.ternServer.addFile('" + name + "', " + j.toString() + ".at)");

		}
		Log.log("TERN", ()->" done ");


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

	public List<Completion> completion(Consumer<String> engine, String boxName, String allText, int line, int ch, boolean explicitlyRequested) {
		List<Completion> r = new ArrayList<>();


		JSONStringer s = new JSONStringer();
		s.object();
		s.key("at")
		 .value(allText);
		s.endObject();

		engine.accept("__someFile=" + s.toString() + ".at");
		engine.accept("__completions = []");
		engine.accept("self.ternServer.request({query:{type:\"completions\", types:true, docs:true, file:\"#0\", end:{line:" + line + ",ch:" + ch + "}}, \n" +
			    "files:[{type:\"full\",name:\"" + boxName + ".js\",text:__someFile}]},\n" +
			    "	function (e,r){\n" +
					  "	post('A'+e)\n"+
					  "	post('A'+r)\n"+
					  "	post('A'+r.completions)\n"+
					  "	post('A'+r.completions.length)\n"+
			    "		for(var i=0;i<r.completions.length;i++)" +
			    "			__completions = __completions.concat([[r.start, r.end, r.completions[i].name, '<span class=type>'+r.completions[i].type+'</span>']])" +
			    "	})");

		engine.accept("__extraCompletions = __completions");

		return r;
	}
}
