package fieldjython;

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
import fielded.TextUtils;
import org.python.core.*;
import org.python.util.PythonInterpreter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static field.utility.Log.log;

/**
 * Created by marc on 10/23/14.
 */
public class JythonExecution extends Execution {
	static public final Dict.Prop<Boolean> bridgedToJython = new Dict.Prop<>("bridgedToJython").toCannon();
	private final PythonInterpreter py;

	public JythonExecution() {
		super(null);

		IO.persist(bridgedToJython);

		this.properties.put(Boxes.dontSave, true);
		Log.on("jython.*", Log::green);

		py = new PythonInterpreter();


		Animatable.registerHandler( (was, o) -> {
			if (o instanceof PyFunction)
			{
				log("jython.debug", "jython found");
				return new Animatable.AnimationElement() {
					@Override
					public Object middle(boolean isEnding) {
						return ((PyFunction)o).__call__();
					}
				};
			}
			return null;
		});

	}

	@Override
	public ExecutionSupport support(Box box, Dict.Prop<String> prop) {
		return wrap(box, prop);
	}

	private ExecutionSupport wrap(Box box, Dict.Prop<String> prop) {

		return new ExecutionSupport() {

			@Override
			public void executeTextFragment(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {

				log("jython.debug", " execute text fragment on :" + textFragment);
				Writer w = new Writer() {
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

				Object result = eval(w, textFragment, lineErrors);

				log("jython.debug", " result string is " + result);
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

				String allText = box.first(prop)
						    .orElse("");

				deleteVar("_r");

				executeAll(allText, lineErrors, success);


				Object ret = getVar("_r");

				log("jython.debug", "_r at eXit :" + ret);

				log("jython.debug", "invoking");
				log("jython.debug", ret instanceof Runnable, ret instanceof Collection, ret instanceof Map);


				Animatable.AnimationElement ae = Animatable.interpret(ret, null);


				if (ae != null) {
					end(lineErrors, success);
					String name = "main._animatorJython_" + (uniq);
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
					if (s.contains("_animatorJython_")) {
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

				for (int i = 0; i < line; i++)
					before += lines[i] + "\n";

				for (int i = line + 1; i < lines.length; i++)
					after += lines[i] + "\n";

				String sub = "";
				int subStart = ch;
				int subEnd = ch;

				for (int i = ch - 1; i >= 0; i--) {
					if (isValidPythonThing(lines[line].charAt(i))) {
						subStart = i;
						sub = lines[line].charAt(i) + sub;
					} else break;
				}

				for (int i = ch; i < lines[line].length(); i++) {
					if (isValidPythonThing(lines[line].charAt(i))) {
						subEnd = i + 1;
					} else break;
				}

				log("clojure.debug", "parsed line <" + lines[line] + "> __prefix__ is " + subStart + " -> " + subEnd);

				String mid = lines[line].substring(0, subStart) + " __prefix__ " + lines[line].substring(subEnd, lines[line].length());

				log("clojure.debug", "parsed line thus " + mid + " / " + sub);

				List<Completion> c = new ArrayList<>();

				/*
				Object ret = completions.invoke(sub, before + mid + "\n" + after);

				log("clojure.debug", "result :"+ret);



				for(String s : ((Collection<String>)ret))
				{
					Object testEval = eval(null, s, null);
					log("clojure.debug", " s -> "+testEval);

					c.add(new Completion(before.length()+subStart, before.length()+subEnd, s, "<span class=doc>"+(String)documentation.invoke(s)+"</span>"));
				}

*/
				log("clojure.debug", "completions are :" + c);
				results.accept(c);

			}

			private boolean isValidPythonThing(char at) {
				return Character.isJavaIdentifierPart(at); //FIXME
			}


			@Override
			public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {

				List<Completion> r = new ArrayList<>();

				String[] lines = allText.split("\n");

				String before = "";
				String after = "";

				for (int i = 0; i < line; i++)
					before += lines[i] + "\n";

				for (int i = line + 1; i < lines.length; i++)
					after += lines[i] + "\n";

				String sub = "";
				int subStart = ch;
				int subEnd = ch;

				for (int i = ch - 1; i >= 0; i--) {
					if (isValidPythonThing(lines[line].charAt(i))) {
						subStart = i;
						sub = lines[line].charAt(i) + sub;
					} else break;
				}

				for (int i = ch; i < lines[line].length(); i++) {
					if (isValidPythonThing(lines[line].charAt(i))) {
						subEnd = i + 1;
					} else break;
				}
				List<Pair<String, String>> possibleJavaClassesFor = JavaSupport.javaSupport.getPossibleJavaClassesFor(sub);

				Log.log("completion.debug", " possible javaclasses :" + possibleJavaClassesFor);

				subStart += before.length();
				subEnd += before.length();

				for (Pair<String, String> p : possibleJavaClassesFor) {
					int tail = p.first.lastIndexOf(".");

					Completion ex = new Completion(subStart, subEnd, p.first.substring(tail + 1), p.second);
					ex.header = "from " + p.first + " import " + p.first; // FIXME
					r.add(ex);
				}

				results.accept(r);
			}

			@Override
			public String getCodeMirrorLanguageName() {
				return "python";
			}

			@Override
			public String getDefaultFileExtension() {
				return "py";
			}
		};
	}

	private Object getVar(String r) {
		return py.get(r, Object.class);
	}

	private void deleteVar(String r) {
		py.set(r, null);
	}


	protected Object eval(Writer output, String textFragment, Consumer<Pair<Integer, String>> lineErrors) {

		py.setOut(output);
		try {
//			py.exec(textFragment);
			return py.eval(py.compile(textFragment));

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String split = sw.toString();
			String[] pieces = split.split("\n");
			StringBuffer o = new StringBuffer();
			for(int i=0;i<Math.min(8, pieces.length);i++)
			{
				if (pieces[i].trim().isEmpty()) break;
				o.append(pieces[i] + "\n");
			}
			lineErrors.accept(new Pair<>(1, TextUtils.html(o.toString())));
		}

		return null;
	}


}
