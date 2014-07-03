package fieldnashorn;

import field.utility.Pair;
import fieldbox.boxes.Box;
import fielded.Execution;
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
import java.util.function.Consumer;
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
		List<String> s = Arrays.asList(new String[]{"acorn.js", "acorn_loose.js", "walk.js", "defs.js", "signal.js", "infer.js", "tern.js", "comment.js", "condense.js"});

		Collection<File> f = s.stream().map(x -> new File(fieldagent.Main.app+"/fielded/external/tern/" + x)).collect(Collectors.toList());

		try {
			engine.eval("self = {}");
			engine.eval("self.tern = {}");
			engine.eval("self.acorn= {}");
			engine.eval("tern = {}");
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		new Thread( () -> {

		for (File ff : f) {
			try (FileReader fr = new FileReader(ff.getAbsolutePath())) {
				engine.put("__FILE__", ff.getName());
				engine.eval(fr);
				engine.eval("print(self.tern.signal)");
			} catch (ScriptException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			engine.put("__ecma5json", readFile(fieldagent.Main.app+"/fielded/external/tern/ecma5.json"));
			engine.eval("self.ternServer=new self.tern.Server({defs: [JSON.parse(__ecma5json)]})");
			engine.eval("delete __ecma5json");
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		}
		).start();
	}

	public List<Execution.Completion> completion(String boxName, String allText, int line, int ch) {
		List<Execution.Completion> r = new ArrayList<>();

		try {
			engine.put("__someFile", allText);
			engine.eval("__completions = new java.util.ArrayList()");
			engine.eval("self.ternServer.request({query:{type:\"completions\", types:true, docs:true, file:\"#0\", end:{line:" + line + ",ch:" + ch + "}}, \n" +
				    "files:[{type:\"full\",name:\""+boxName+".js\",text:__someFile}]},\n" +
				    "	function (e,r){\n" +
				    "		for(var i=0;i<r.completions.length;i++)" +
				    "			__completions.add(new fielded.Execution.Completion(r.start, r.end, r.completions[i].name, '<span class=type>'+r.completions[i].type.replace('->','&rarr;')+'</span> &mdash; <span class=doc>'+r.completions[i].doc+'</span>'))" +
				    "	})");
			r.addAll((ArrayList<Execution.Completion>) engine.get("__completions"));

			Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
			System.out.println(" bindings are");
			for (Object k : bindings.keySet()) {
				Object t = bindings.get(k);
				if (t==null) continue;
				if (t instanceof String)
					if (((String)t).contains("\n")) t = ((String)t).split("\n")[0] + " ...";
				System.out.println("    " + k + " " + t);
			}

			String[] lines = allText.split("\n");
			int c = 0;
			for(int i=0;i<line;i++)
			{
				c+=lines[i].length()+1;
			}

			c += ch;


			System.out.println(" line :"+line+" -> "+ch+" -> "+c);

			engine.eval("__c=new self.tern.Context()");
			Object[] ret = (Object[]) engine.eval("self.tern.withContext(__c, function(){\n" +
				    "\tvar a = self.tern.parse(__someFile)\n" +
				    "\tself.tern.analyze(a)\n" +
				    "\tvar n = self.tern.findExpressionAround(a, "+c+", "+c+")\n" +
				    "\treturn Java.to([n.node.start, n.node.end])"+
				    "})");
			System.out.println(" expression to analyze is :"+ret[0]+" "+ret[1]+" "+allText.substring( ((Number)ret[0]).intValue(), ((Number)ret[1]).intValue()));


			String s = allText.substring( ((Number)ret[0]).intValue(), ((Number)ret[1]).intValue());

			try{

				String left = s, right = "";

				if (s.lastIndexOf('.')!=-1)
				{
					left = s.substring(0, s.lastIndexOf('.'));
					right = s.substring(s.lastIndexOf('.')+1);
				}

				Object e = engine.eval("_e=eval('"+left.replace("'","\\'")+"')");
				System.out.println(" e is :"+e+" "+e.getClass());

				// now if e is an actual java object --- i.e. it's got nothing to do with nashorn, then we could use a more general Field java completion service
				// and just add the dot back in
				if (e instanceof ScriptObjectMirror)
				{

					Object[] retae = (Object[])engine.eval("_v=[]; _p = {}; Object.bindProperties(_p, _e); for(var _k in _p) _v.push(_k); Java.to(_v)");
					System.out.println(" auto eval completion got :" + Arrays.asList(retae));
				}
				else if (e instanceof Box)
				{
					e = new UnderscoreBox((Box)e);
					List<Execution.Completion> fromJava = javaSupport.getCompletionsFor(e, right);
					for(Execution.Completion x : fromJava)
					{
						if (x.start==-1) x.start=c-right.length();
						if (x.end==-1) x.end=c;
					}

					r.addAll(fromJava);
				}
				else if (e instanceof StaticClass)
				{
					e = ((StaticClass)e).getRepresentedClass();

					System.out.println(" asking java for completions for CLASS "+e);
					List<Execution.Completion> fromJava = javaSupport.getCompletionsFor(e, right);
					System.out.println(" got completions :"+fromJava);
					for(Execution.Completion x : fromJava)
					{
						if (x.start==-1) x.start=c-right.length();
						if (x.end==-1) x.end=c;
					}

					r.addAll(fromJava);
				}
				else
				{
					System.out.println(" asking java for completions for "+e);
					List<Execution.Completion> fromJava = javaSupport.getCompletionsFor(e, right);
					System.out.println(" got completions :"+fromJava);
					for(Execution.Completion x : fromJava)
					{
						if (x.start==-1) x.start=c-right.length();
						if (x.end==-1) x.end=c;
					}

					r.addAll(fromJava);
				}


			}
			catch(Throwable t)
			{
				System.err.println(" suppressed exception in autoevaluating completion <" + t + ">");
			}
		} catch (ScriptException e) {
			e.printStackTrace();
		}


		return r;
	}

	public List<Execution.Completion> imports(String boxName, String allText, int line, int ch) {
		List<Execution.Completion> r = new ArrayList<>();

		try {
			engine.put("__someFile", allText);
			engine.eval("__completions = new java.util.ArrayList()");


			String[] lines = allText.split("\n");
			int c = 0;
			for(int i=0;i<line;i++)
			{
				c+=lines[i].length()+1;
			}

			c += ch;


			System.out.println(" line :"+line+" -> "+ch+" -> "+c);

			engine.eval("__c=new self.tern.Context()");
			Object[] ret = (Object[]) engine.eval("self.tern.withContext(__c, function(){\n" +
				    "\tvar a = self.tern.parse(__someFile)\n" +
				    "\tself.tern.analyze(a)\n" +
				    "\tvar n = self.tern.findExpressionAround(a, "+c+", "+c+")\n" +
				    "\treturn Java.to([n.node.start, n.node.end])"+
				    "})");
			System.out.println(" expression to analyze is :"+ret[0]+" "+ret[1]+" "+allText.substring( ((Number)ret[0]).intValue(), ((Number)ret[1]).intValue()));


			String s = allText.substring( ((Number)ret[0]).intValue(), ((Number)ret[1]).intValue());

			try{

				String left = s, right = "";


				System.out.println(" inside import help left is <"+left+">");

				List<Pair<String, String>> possibleJavaClassesFor = javaSupport.getPossibleJavaClassesFor(left);

				System.out.println(" possible javaclasses :"+possibleJavaClassesFor);

				for(Pair<String, String> p : possibleJavaClassesFor)
				{
					int tail = p.first.lastIndexOf(".");

					Execution.Completion ex = new Execution.Completion(c - left.length(), c, p.first.substring(tail + 1), p.second);
					ex.header = "var "+p.first.substring(tail + 1)+" = Java.type('"+p.first+"')";
					r.add(ex);
				}

			}
			catch(Throwable t)
			{
				System.err.println(" suppressed exception in autoevaluating completion <" + t + ">");
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
