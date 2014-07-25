package fieldclojure;

import clojure.java.api.Clojure;
import clojure.lang.*;
import clojure.lang.Compiler;
import field.utility.Dict;
import field.utility.Log;
import static field.utility.Log.log;
import field.utility.Pair;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.plugins.IsExecuting;
import fieldbox.io.IO;
import fielded.Execution;
import fieldbox.boxes.Box;
import fieldnashorn.JavaSupport;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**


 */
public class ClojureExecution extends Execution {

	static public final Dict.Prop<Boolean> bridgedToClojure = new Dict.Prop<>("bridgedToClojure").toCannon();
	static public final Dict.Prop<String> _clojureNS= new Dict.Prop<>("_clojureNS").toCannon();

	static {
		IO.persist(bridgedToClojure);
	}

	static public String nsClojure = "clojure.core";

	static  IFn ns;
	static  IFn out;

	static IFn completions;
	static IFn documentation;

	public ClojureExecution() {
		super(null);

		ns = Clojure.var(nsClojure, "*ns*");
		out = Clojure.var(nsClojure, "*out*");
		this.properties.put(Boxes.dontSave, true);
		Log.on("clojure.*", Log::green);

		wrap(this).executeTextFragment("(use 'compliment.core)", x -> System.out.println(x), x -> System.out.println(x));

		completions = Clojure.var("compliment.core", "completions");
		documentation= Clojure.var("compliment.core", "documentation");

		log("clojure.debug", completions.invoke("al-", null));

		Var.pushThreadBindings(PersistentHashMap.create(ns , Namespace.findOrCreate(Symbol.create("user"))));
	}

	@Override
	public ExecutionSupport support(Box box, Dict.Prop<String> prop) {
		return wrap(box);
	}

	private ExecutionSupport wrap(Box box) {

		return new ExecutionSupport() {
			@Override
			public void executeTextFragment(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {

				log("clojure.debug", "ns at entry :"+((Var)ns).get());

				String sticky = box.properties.get(_clojureNS);
				if (sticky!=null)
				{
					eval("(ns "+sticky+")", null);
				}

				log("clojure.debug", "ns at entry after sticky :"+((Var)ns).get());

				Object nsOnEntry = ((Var) ns).get();

				log("clojure.debug", " execute text fragment on :" + textFragment);
				StringWriter sw = new StringWriter();
				((Var)out).bindRoot(sw);

				Object result = eval(textFragment, lineErrors);

				String out = sw.toString();
				out = (out.length()>0 ? (out.trim()+"\n") : "")+(result!=null ? (""+result) : "");
				if (out.trim().length()>0) {
					success.accept(out);
				}
				log("clojure.debug", " result string is "+out);

				log("clojure.debug", "ns at exit :"+((Var)ns).get());

				Object nsAtExit = ((Var) ns).get();
				if (!nsAtExit.equals(nsOnEntry))
				{
					log("clojure.debug", "ns has changed, setting sticky");
					box.properties.put(_clojureNS, ((Namespace)nsAtExit).getName().getName());
				}

			}

			@Override
			public void executeAndPrint(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				executeTextFragment(textFragment, lineErrors, success);
			}

			@Override
			public void executeAll(String allText, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				executeTextFragment(allText, lineErrors, success);
			}

			@Override
			public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				return null;
			}

			@Override
			public void end(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
			}

			@Override
			public void setConsoleOutput(Consumer<String> stdout, Consumer<String> stderr) {
			}

			@Override
			public void completion(String allText, int line, int ch, Consumer<List<Completion>> results) {

				String[] lines = allText.split("\n");

				String before = "";
				String after = "";

				for(int i=0;i<line;i++)
					before += lines[i]+"\n";

				for(int i=line+1;i<lines.length;i++)
					after += lines[i]+"\n";

				String sub = "";
				int subStart = ch;
				int subEnd = ch;

				for(int i=ch-1;i>=0;i--)
				{
					if (isValidClojureThing(lines[line].charAt(i)))
					{
						subStart = i;
						sub = lines[line].charAt(i)+sub;
					}
					else break;
				}

				for(int i=ch;i<lines[line].length();i++)
				{
					if (isValidClojureThing(lines[line].charAt(i)))
					{
						subEnd = i+1;
					}
					else break;
				}

				log("clojure.debug", "parsed line <" + lines[line] + "> __prefix__ is " + subStart + " -> " + subEnd);

				String mid = lines[line].substring(0, subStart)+" __prefix__ "+lines[line].substring(subEnd, lines[line].length());

				log("clojure.debug", "parsed line thus "+mid+" / "+sub);

				Object ret = completions.invoke(sub, before + mid + "\n" + after);

				log("clojure.debug", "result :"+ret);


				List<Completion> c = new ArrayList<Completion>();

				for(String s : ((Collection<String>)ret))
				{
					Object testEval = eval(s, null);
					log("clojure.debug", " s -> "+testEval);

					c.add(new Completion(before.length()+subStart, before.length()+subEnd, s, "<span class=doc>"+(String)documentation.invoke(s)+"</span>"));
				}


				results.accept(c);

			}

			Set term = new LinkedHashSet<Character>(Arrays.asList('(',')',' ','[',']','{','}'));

			private boolean isValidClojureThing(char at) {
				return !term.contains(at);
			}


			@Override
			public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {

				List<Execution.Completion> r = new ArrayList<>();

				String[] lines = allText.split("\n");

				String before = "";
				String after = "";

				for(int i=0;i<line;i++)
					before += lines[i]+"\n";

				for(int i=line+1;i<lines.length;i++)
					after += lines[i]+"\n";

				String sub = "";
				int subStart = ch;
				int subEnd = ch;

				for(int i=ch-1;i>=0;i--)
				{
					if (isValidClojureThing(lines[line].charAt(i)))
					{
						subStart = i;
						sub = lines[line].charAt(i)+sub;
					}
					else break;
				}

				for(int i=ch;i<lines[line].length();i++)
				{
					if (isValidClojureThing(lines[line].charAt(i)))
					{
						subEnd = i+1;
					}
					else break;
				}
				List<Pair<String, String>> possibleJavaClassesFor = JavaSupport.javaSupport.getPossibleJavaClassesFor(sub);

				Log.log("completion.debug", " possible javaclasses :" + possibleJavaClassesFor);

				subStart += before.length();
				subEnd += before.length();

				for (Pair<String, String> p : possibleJavaClassesFor) {
					int tail = p.first.lastIndexOf(".");

					Execution.Completion ex = new Execution.Completion(subStart, subEnd, p.first
						    .substring(tail + 1), p.second);
					ex.header = "(import '"+p.first+")";
					r.add(ex);
				}

				results.accept(r);
			}

			@Override
			public String getCodeMirrorLanguageName() {
				return "clojure";
			}

			@Override
			public String getDefaultFileExtension() {
				return "clj";
			}
		};
	}

	protected Object eval(String textFragment, Consumer<Pair<Integer, String>> lineErrors) {
		LineNumberingPushbackReader rdr = new LineNumberingPushbackReader(new StringReader(textFragment));
		Object EOF = new Object();

		Object result = null;

		//start the loop
		for(; ;)
		{
			try
			{
				Object r = LispReader.read(rdr, false, EOF, false);
				if(r == EOF)
				{
					break;
				}
				log("clojure.debug", " form is :" + r);
				Object ret = Compiler.eval(r);
				log("clojure.debug", " ret is " + ret);
				result = ret;
			}
			catch(Throwable e)
			{
				if (lineErrors==null) return null;

				Throwable c = e;
				while(c.getCause() != null)
				{
					c = c.getCause();
				}

				IllegalArgumentException c2 = new IllegalArgumentException("Exception in clojure");
				c2.initCause(c);
				c.printStackTrace();
				lineErrors.accept(new Pair<>(0,""+c));
				break;
			}
		}
		return result;
	}
}
