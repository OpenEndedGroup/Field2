package fieldnashorn;

import field.utility.Log;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.execution.Completion;
import fieldbox.execution.JavaSupport;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tern.js-based completion for Nashorn/Javascript that isn't afraid to drop down into Java/Reflection based completion when it can (see JavaSupport)
 */
public class TernSupport {

	private final JavaSupport javaSupport;
	private ScriptEngine engine;

	JavaSupport javaSupprt;

	public TernSupport() {
		javaSupport = new JavaSupport();
	}

	public void inject(ScriptEngine engine) {

		this.engine = engine;
		List<String> s = Arrays
			    .asList(new String[]{"acorn.js", "acorn_loose.js", "walk.js", "defs.js", "signal.js", "infer.js", "tern.js", "comment.js", "condense.js"});

		Collection<File> f = s.stream().map(x -> new File(fieldagent.Main.app + "/fielded/external/tern/" + x)).collect(Collectors.toList());

		try {
			engine.eval("self = {}");
			engine.eval("self.tern = {}");
			engine.eval("self.acorn= {}");
			engine.eval("tern = {}");
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		new Thread(() -> {

			for (File ff : f) {
				try (FileReader fr = new FileReader(ff.getAbsolutePath())) {
					engine.put("__FILE__", ff.getName());
					engine.eval(fr);
				} catch (ScriptException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				engine.put("__ecma5json", readFile(fieldagent.Main.app + "/fielded/external/tern/ecma5.json"));
				engine.eval("self.ternServer=new self.tern.Server({defs: [JSON.parse(__ecma5json)]})");
				engine.eval("delete __ecma5json");
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public List<Completion> completion(String boxName, String allText, int line, int ch) {
		List<Completion> r = new ArrayList<>();


		try {
			engine.put("__someFile", allText);
			engine.eval("__completions = new java.util.ArrayList()");
			engine.eval("self.ternServer.request({query:{type:\"completions\", types:true, docs:true, file:\"#0\", end:{line:" + line + ",ch:" + ch + "}}, \n" +
				    "files:[{type:\"full\",name:\"" + boxName + ".js\",text:__someFile}]},\n" +
				    "	function (e,r){\n" +
				    "		for(var i=0;i<r.completions.length;i++)" +
				    "			__completions.add(new fieldbox.execution.Completion(r.start, r.end, r.completions[i].name, '<span class=type>'+r.completions[i].type.replace('->','&rarr;')+'&nbsp;&mdash;&nbsp;</span><span class=doc>'+r.completions[i].doc+'</span>'))" +
				    "	})");
			r.addAll((ArrayList<Completion>) engine.get("__completions"));

			Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
			Log.log("completion.debug", () -> {
				Log.log("completion.debug", "bindings are...");
				for (Object k : bindings.keySet()) {
					Object t = bindings.get(k);
					if (t == null) continue;
					if (t instanceof String) if (((String) t).split("\n").length>0) t = ((String) t).split("\n")[0] + " ...";
					Log.log("completion.debug", "    " + k + " " + t);
				}
				return null;
			});

			if (allText.trim().length()==0) return r;

			String[] lines = allText.split("\n");
			int c = 0;
			for (int i = 0; i < line; i++) {
				c += lines[i].length() + 1;
			}

			c += ch;


			Log.log("completion.debug", " line :" + line + " -> " + ch + " -> " + c+" alltext is <"+allText+">");

			engine.eval("__c=new self.tern.Context()");
			try {
				Object[] ret = (Object[]) engine.eval("self.tern.withContext(__c, function(){\n" +
					    "\tvar a = self.tern.parse(__someFile)\n" +
					    "\tself.tern.analyze(a)\n" +
					    "\tvar n = self.tern.findExpressionAround(a, " + c + ", " + c + ")\n" +
					    "\tif (n!=null)\n" +
					    "\treturn Java.to([n.node.start, n.node.end]);\n" + "\treturn null;}) ");
				if (ret == null) return r;
				if (ret.length==0) return r;

				Log.log("completion.debug", " expression to analyze is :" + ret[0] + " " + ret[1] + " " + allText
					    .substring(((Number) ret[0]).intValue(), ((Number) ret[1]).intValue()));


				String s = allText.substring(((Number) ret[0]).intValue(), ((Number) ret[1]).intValue());

				String left = s, right = "";

				if (s.lastIndexOf('.') != -1) {
					left = s.substring(0, s.lastIndexOf('.'));
					right = s.substring(s.lastIndexOf('.') + 1);
				}

				Object e = engine.eval("_e=eval('" + left.replace("'", "\\'") + "')");
				Log.log("completion.debug", " e is :" + e + " " + e.getClass()+" computed prefix from <"+s+"> <"+s.lastIndexOf('.')+">");

				if (right.trim().length()!=right.length()) right="";

				// now if e is an actual java object --- i.e. it's got nothing to do with nashorn, then we could use a more general Field java completion service
				// and just add the dot back in
				if (e instanceof ScriptObjectMirror) {

					Object[] retae = (Object[]) engine
						    .eval("_v=[]; _p = {}; Object.bindProperties(_p, _e); for(var _k in _p) _v.push(_k); Java.to(_v)");
					Log.log("completion.debug", " auto eval completion got :" + Arrays.asList(retae));
				} else if (e instanceof Box) {
//					e = new UnderscoreBox((Box) e);
					List<Completion> fromJava = javaSupport.getCompletionsFor(e, right);
					for (Completion x : fromJava) {
						if (x.start == -1) x.start = c - right.length();
						if (x.end == -1) x.end = c;
					}

					r.addAll(fromJava);
				} else if (e instanceof StaticClass) {
					e = ((StaticClass) e).getRepresentedClass();

					Log.log("completion.debug", " asking java for completions for CLASS " + e);
					List<Completion> fromJava = javaSupport.getCompletionsFor(e, right);
					Log.log("completion.debug", " got completions :" + fromJava);
					for (Completion x : fromJava) {
						if (x.start == -1) x.start = c - right.length();
						if (x.end == -1) x.end = c;
					}

					r.addAll(fromJava);
				} else {
					Log.log("completion.debug", " asking java for completions for " + e);
					List<Completion> fromJava = javaSupport.getCompletionsFor(e, right);
					Log.log("completion.debug", " got completions :" + fromJava);
					for (Completion x : fromJava) {
						if (x.start == -1) x.start = c - right.length();
						if (x.end == -1) x.end = c;
					}

					r.addAll(fromJava);
				}


			} catch (Throwable t) {
				Log.log("completion.error", " suppressed exception in autoevaluating completion <" + t + ">");
			}
		} catch (ScriptException e) {
			e.printStackTrace();
		}


		return r;
	}

	public List<Completion> imports(String boxName, String allText, int line, int ch) {
		List<Completion> r = new ArrayList<>();

		try {
			engine.put("__someFile", allText);
			engine.eval("__completions = new java.util.ArrayList()");


			String[] lines = allText.split("\n");
			int c = 0;
			for (int i = 0; i < line; i++) {
				c += lines[i].length() + 1;
			}

			c += ch;


			Log.log("completion.debug", " line :" + line + " -> " + ch + " -> " + c);

			engine.eval("__c=new self.tern.Context()");
			Object[] ret = (Object[]) engine.eval("self.tern.withContext(__c, function(){\n" +
				    "\tvar a = self.tern.parse(__someFile)\n" +
				    "\tself.tern.analyze(a)\n" +
				    "\tvar n = self.tern.findExpressionAround(a, " + c + ", " + c + ")\n" +
				    "\treturn Java.to([n.node.start, n.node.end])" +
				    "})");
			Log.log("completion.debug", " expression to analyze is :" + ret[0] + " " + ret[1] + " " + allText
				    .substring(((Number) ret[0]).intValue(), ((Number) ret[1]).intValue()));


			String s = allText.substring(((Number) ret[0]).intValue(), ((Number) ret[1]).intValue());

			try {

				String left = s, right = "";


				Log.log("completion.debug", " inside import help left is <" + left + ">");

				List<Pair<String, String>> possibleJavaClassesFor = javaSupport.getPossibleJavaClassesFor(left);

				Log.log("completion.debug", " possible javaclasses :" + possibleJavaClassesFor);

				for (Pair<String, String> p : possibleJavaClassesFor) {
					int tail = p.first.lastIndexOf(".");

					Completion ex = new Completion(c - left.length(), c, p.first
						    .substring(tail + 1), p.second);
					ex.header = "var " + p.first.substring(tail + 1) + " = Java.type('" + p.first + "')";
					r.add(ex);
				}

			} catch (Throwable t) {
				Log.log("completion.debug", " suppressed exception in autoevaluating completion <" + t + ">");
			}
		} catch (ScriptException e) {
			e.printStackTrace();
		}


		return r;
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


}
