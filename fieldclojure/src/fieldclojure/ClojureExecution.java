package fieldclojure;

import clojure.java.api.Clojure;
import clojure.lang.Compiler;
import clojure.lang.*;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.plugins.IsExecuting;
import fieldbox.execution.Completion;
import fieldbox.execution.Execution;
import fieldbox.execution.JavaSupport;
import fieldbox.io.IO;
import fielded.Animatable;
import fieldnashorn.Nashorn;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static field.utility.Log.log;

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

		wrap(this, null).executeTextFragment("(use 'compliment.core)", x -> System.out.println(x), x -> System.out.println(x));

		completions = Clojure.var("compliment.core", "completions");
		documentation= Clojure.var("compliment.core", "documentation");

		log("clojure.debug", completions.invoke("al-", null));

		Var.pushThreadBindings(PersistentHashMap.create(ns , Namespace.findOrCreate(Symbol.create("user"))));

		Animatable.registerHandler( (was, o) -> {
			if (o instanceof IFn && !(o instanceof Collection) && !(o instanceof Map))
			{
				log("clojure.debug", "IFn found");
				return new Animatable.AnimationElement() {
					@Override
					public Object middle(boolean isEnding) {
						return ((IFn)o).invoke();
					}
				};
			}
			return null;
		});
		Animatable.registerHandler( (was, o) -> {
			if (o instanceof IFn && (o instanceof Collection) && !(o instanceof Map))
			{
				log("clojure.debug", "IFn found -- collection");

				Animatable.AnimationElement start = null;
				Animatable.AnimationElement middle = null;
				Animatable.AnimationElement end = null;

				Object o0 = safeGet((Collection)o, 0);
				Object o1 = safeGet((Collection)o, 1);
				Object o2 = safeGet((Collection)o, 2);

				if (o0!=null) start = Animatable.interpret(o0, was);
				if (o1!=null) middle = Animatable.interpret(o1, was);
				if (o2!=null) end = Animatable.interpret(o2, was);


				if (start == null) start = Nashorn.noop();
				if (middle == null) middle = Nashorn.noop();
				if (end == null) end = Nashorn.noop();

				Animatable.AnimationElement fstart = start;
				Animatable.AnimationElement fmiddle = middle;
				Animatable.AnimationElement fend = end;

				return new Animatable.AnimationElement() {

					Animatable.AnimationElement[] targets = {fstart, fmiddle, fend};
					int index = 0;
					boolean finished = false;

					@Override
					public Object beginning(boolean isEnding) {
						targets[0] = Nashorn.interpretReturn(targets[0], targets[0].beginning(isEnding));
						targets[0] = Nashorn.interpretReturn(targets[0], targets[0].middle(isEnding));
						targets[0] = Nashorn.interpretReturn(targets[0], targets[0].end(isEnding));
						targets[1] = Nashorn.interpretReturn(targets[1], targets[1].beginning(isEnding));
						return this;
					}

					public Object middle(boolean isEnding) {
						targets[1] = Nashorn.interpretReturn(targets[1], targets[1].middle(isEnding));
						return this;
					}

					public Object end(boolean isEnding) {
						targets[1] = Nashorn.interpretReturn(targets[1], targets[1].end(isEnding));
						targets[2] = Nashorn.interpretReturn(targets[2], targets[2].beginning(isEnding));
						targets[2] = Nashorn.interpretReturn(targets[2], targets[2].middle(isEnding));
						targets[2] = Nashorn.interpretReturn(targets[2], targets[2].end(isEnding));
						return this;
					}

				};
			}
			return null;
		});
		Animatable.registerHandler( (was, o) -> {
			if (o instanceof IFn && !(o instanceof Collection) && (o instanceof Map))
			{
				log("clojure.debug", "IFn found -- map");
				Animatable.AnimationElement start = null;
				Animatable.AnimationElement middle = null;
				Animatable.AnimationElement end = null;

				List<Object> kk = new ArrayList<>(((Map)o).keySet());

				Object o0 = ((Map)o).get("start");
				Object o1 = ((Map)o).get("middle");
				Object o2 = ((Map)o).get("end");

				if (o0!=null) start = Animatable.interpret(o0, was);
				if (o1!=null) middle = Animatable.interpret(o1, was);
				if (o2!=null) end = Animatable.interpret(o2, was);


				if (start == null) start = Nashorn.noop();
				if (middle == null) middle = Nashorn.noop();
				if (end == null) end = Nashorn.noop();

				Animatable.AnimationElement fstart = start;
				Animatable.AnimationElement fmiddle = middle;
				Animatable.AnimationElement fend = end;

				return new Animatable.AnimationElement() {

					Animatable.AnimationElement[] targets = {fstart, fmiddle, fend};
					int index = 0;
					boolean finished = false;

					@Override
					public Object beginning(boolean isEnding) {
						targets[0] = Nashorn.interpretReturn(targets[0], targets[0].beginning(isEnding));
						targets[0] = Nashorn.interpretReturn(targets[0], targets[0].middle(isEnding));
						targets[0] = Nashorn.interpretReturn(targets[0], targets[0].end(isEnding));
						targets[1] = Nashorn.interpretReturn(targets[1], targets[1].beginning(isEnding));
						return this;
					}

					public Object middle(boolean isEnding) {
						targets[1] = Nashorn.interpretReturn(targets[1], targets[1].middle(isEnding));
						return this;
					}

					public Object end(boolean isEnding) {
						targets[1] = Nashorn.interpretReturn(targets[1], targets[1].end(isEnding));
						targets[2] = Nashorn.interpretReturn(targets[2], targets[2].beginning(isEnding));
						targets[2] = Nashorn.interpretReturn(targets[2], targets[2].middle(isEnding));
						targets[2] = Nashorn.interpretReturn(targets[2], targets[2].end(isEnding));
						return this;
					}

				};			}
			return null;
		});
	}

	private Object safeGet(Collection o, int i) {
		Iterator a = o.iterator();
		Object last = null;
		for(int ii=0;ii<i+1;ii++)
		{
			if (a.hasNext())
				last = a.next();
			else
				break;
		}
		return last;
	}

	@Override
	public ExecutionSupport support(Box box, Dict.Prop<String> prop) {
		return wrap(box, prop);
	}

	private ExecutionSupport wrap(Box box, Dict.Prop<String> prop) {

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
				Writer w = new Writer()
				{
					@Override
					public void write(char[] cbuf, int off, int len) throws IOException {
						success.accept(new String(cbuf, off, len));
					}

					@Override
					public void flush() throws IOException {
					}
					@Override
					public void close() throws IOException {
					}
				};
				((Var)out).bindRoot(w);

				Object result = eval(textFragment, lineErrors);

				try {
					w.append("\n"+(result!=null ? (""+result) : ""));
				} catch (IOException e) {
					e.printStackTrace();
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


			long uniq;

			@Override
			public void executeAll(String allText, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				executeTextFragment(allText, lineErrors, success);
			}

			@Override
			public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {

				log("clojure.debug", "ns at entry :"+((Var)ns).get());

				String sticky = box.properties.get(_clojureNS);
				if (sticky!=null)
				{
					eval("(ns "+sticky+")", null);
				}
				else
				{
					sticky = ((Namespace)((Var)ns).get()).getName().getName();
				}

				Var v = (Var)Clojure.var(sticky, "_r");
				log("clojure.debug", "_r at entry :"+v.get());
				v.bindRoot(null);
				log("clojure.debug", "_r at entry :"+v.get());

				String allText = box.first(prop)
						    .orElse("");

				executeAll(allText, lineErrors, success);

				sticky = box.properties.get(_clojureNS);
				if (sticky!=null)
				{
					eval("(ns "+sticky+")", null);
				}
				else
				{
					sticky = ((Namespace)((Var)ns).get()).getName().getName();
				}

				v = (Var)Clojure.var(sticky, "_r");
				Object ret = v.get();

				log("clojure.debug", "_r at eXit :" + ret);

				log("clojure.debug", "invoking");
				log("clojure.debug", ret instanceof Runnable, ret instanceof Collection, ret instanceof Map);


				Animatable.AnimationElement ae = Animatable.interpret(ret, null);


				if (ae != null) {
					end(lineErrors, success);
					String name = "main._animatorClojure_" + (uniq);
					box.properties.putToMap(Boxes.insideRunLoop, name, new Animatable.Shim(ae));
					box.first(IsExecuting.isExecuting)
					   .ifPresent(x -> x.accept(box, name));

					uniq++;
					return name;
				}



				return null;
			}

			@Override
			public void end(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				Map<String, Supplier<Boolean>> m = box.properties.get(Boxes.insideRunLoop);
				if (m == null) return;
				for (String s : new ArrayList<>(m.keySet())) {
					if (s.contains("_animatorClojure_")) {
						Supplier<Boolean> b = m.get(s);
						if (b instanceof Consumer) ((Consumer<Boolean>) b).accept(false);
						else {
							m.remove(s);
						}
					}
				}
				Drawing.dirty(box);
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


				log("clojure.debug", "completions are :"+c);
				results.accept(c);

			}

			Set term = new LinkedHashSet<Character>(Arrays.asList('(',')',' ','[',']','{','}'));

			private boolean isValidClojureThing(char at) {
				return !term.contains(at);
			}


			@Override
			public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {

				List<Completion> r = new ArrayList<>();

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

					Completion ex = new Completion(subStart, subEnd, p.first
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

		for(; ;)
		{
			try
			{
				Object r = LispReader.read(rdr, false, EOF, false);
				if(r == EOF)
				{
					break;
				}
				log("clojure.debug", " form is :" + r+" "+Thread.currentThread().getContextClassLoader());
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
