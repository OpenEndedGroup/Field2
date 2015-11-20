package fieldnashorn;

import field.app.ThreadSync;
import field.linalg.Vec4;
import field.nashorn.api.scripting.ScriptObjectMirror;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.plugins.IsExecuting;
import fieldbox.execution.Completion;
import fieldbox.execution.Errors;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fielded.Animatable;
import fielded.DisabledRangeHelper;
import fielded.RemoteEditor;
import fielded.boxbrowser.ObjectToHTML;
import fielded.plugins.Out;
import fieldnashorn.babel.SourceTransformer;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of Execution.ExecutionSupport for Nashorn/Javascript
 */
public class NashornExecution implements Execution.ExecutionSupport {

	static public final Dict.Prop<SourceTransformer> sourceTransformer = new Dict.Prop<SourceTransformer>("sourceTransformer").doc(
		    "an instanceof of a SourceTransformer that will take the source code here and transform it into JavaScript. This allows things like Babel.js to be used in Field")
																  .toCannon();

	private final Dict.Prop<String> property;
	private final Box box;
	private final ScriptContext context;
	private final ScriptEngine engine;
	private final Out output;
	public String filename = null;
	int uniq = 0;
	private TernSupport ternSupport;
	private int lineOffset;
	private Pair<Box, Integer> currentLineNumber = null;


	public NashornExecution(Box box, Dict.Prop<String> property, ScriptContext b, ScriptEngine engine) {
		this.box = box;
		this.property = property;
		this.context = b;
		this.engine = engine;

		output = box.find(Out.__out, box.both()).findFirst().orElseThrow(() -> new IllegalStateException("Can't find html output support"));
	}

	@Override
	public void executeTextFragment(String textFragment, String suffix, Consumer<String> success, Consumer<Pair<Integer, String>> lineErrors) {
		if (suffix.equals("print")) {
			executeAndReturn("print(" + textFragment + ")", lineErrors, success, false);
		} else executeAndReturn(textFragment, lineErrors, success, !suffix.equals("noprint"));
	}

	Function<Integer, Integer> lineTransform;

	protected void executeAndReturn(String textFragment, Consumer<Pair<Integer, String>> lineErrors, final Consumer<String> success, boolean printResult) {
		try {
			Execution.context.get()
					 .push(box);

			Errors.errors.push((t, m) -> {
				System.out.println(" exception thrown inside box " + box);
				System.out.println(" message is :" + m);
				t.printStackTrace();

				int ln = -1;
				Matcher matcher = Pattern.compile("LN<(.*)@(.*)>")
							 .matcher(m);

				if (matcher.find()) {
					try {
						ln = Integer.parseInt(matcher.group(1));
					} catch (NumberFormatException e) {
						System.err.println(" malformed number ? " + matcher.group(1));
						ln = -1;
					}

					String boxName = matcher.group(2);

					lineErrors.accept(new Pair<>(ln, "Error in deferred execution on line " + ln + " in box " + boxName+"\n"+"Full message is " + m + " / " + t.getMessage()+"<br>"));
				} else {
					lineErrors.accept(new Pair<>(ln, "Error in deferred execution '" + m + "'\n"+t.getMessage()+"<br>"));
				}

				RemoteEditor.boxFeedback(Optional.of(box), new Vec4(1,0,0,1), "__redmark__", 1, 1000);

			});

			Writer writer = null;
			boolean[] written = {false};
			if (success != null) {

				currentLineNumber = null;


				writer = new Writer() {
					StringBuilder b = new StringBuilder();

					@Override
					public void write(char[] cbuf, int off, int len) throws IOException {
						if (len > 0) {
							String s = new String(cbuf, off, len);
							if (s.endsWith("\n")) s = s.substring(0, s.length() - 1)+"<br>";
							if (s.trim()
							     .length() == 0) return;
							written[0] = true;

							if (currentLineNumber==null || currentLineNumber.first==null || currentLineNumber.second==-1)
								success.accept(s);
							else {
								final String finalS = s;
								box.find(Execution.directedOutput, box.upwards()).findFirst().ifPresentOrElse(x -> x.accept(new Triple<>(currentLineNumber.first, currentLineNumber.second,
																			     finalS)), () -> success.accept(finalS));
							}
						}
					}

					@Override
					public void flush() throws IOException {
					}

					@Override
					public void close() throws IOException {

					}
				};
				engine.getContext()
				      .setWriter(writer);
			}

			Log.log("nashorn.general", "\n>>javascript in");
			Log.log("nashorn.general", textFragment);
			Log.log("nashorn.general", "applying lineOffset of :" + lineOffset);

			// we prefix the code with a sufficient number of \n's so that the line number of any error message actually refers to the correct line
			// dreadful hack, but there's no other option right now in Nashorn (sourceMaps aren't supported for example)
			StringBuffer prefix = new StringBuffer(Math.max(0, lineOffset));
			for (int i = 0; i < lineOffset; i++)
				prefix.append('\n');

			textFragment = prefix + textFragment + (filename == null ? "" : ("//# sourceURL=" + filename));


			//TODO: should be find?
			SourceTransformer st = box.properties.get(sourceTransformer);


			if (st != null) {
				try {
					Pair<String, Function<Integer, Integer>> transformation = st.transform(textFragment);
					textFragment = transformation.first;
					lineTransform = transformation.second;
				} catch (SourceTransformer.TranslationFailedException t) {
					lineErrors.accept(new Pair<>(-1, t.getMessage()+"<br>"));
					return;
				}
			} else {
				lineTransform = x -> x;
			}

			RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__");

			output.setWriter(writer, this::setCurrentLineNumberForPrinting);

			Object ret = engineeval(textFragment, context, e -> handleScriptException(e, lineErrors, lineTransform));

			Log.log("nashorn.general", () -> "\n<<javascript out" + ret + " " + (ret != null ? ret.getClass() + "" : ""));
			if (writer != null) writer.flush();
			if (success != null && printResult && !written[0]) {
				if (ret != null) {
					if (ret instanceof ScriptObjectMirror && ((ScriptObjectMirror) ret).isFunction()) {
						success.accept("[function defined]");
					} else {
						success.accept(output.convert(ret));
					}
				} else if (!written[0]) success.accept(" &#10003; ");
			}

			RemoteEditor.boxFeedback(Optional.of(box), new Vec4(0.3f, 0.7f, 0.3f, 0.5f));

		} catch (ScriptException e) {
			handleScriptException(e, lineErrors, lineTransform);
		} catch (Throwable t) {
			handleScriptException(t, lineErrors, x -> x);

		} finally {
			lineOffset = 0;
			Execution.context.get()
					 .pop();

			Errors.errors.pop();
		}
	}

	private void setCurrentLineNumberForPrinting(Pair<Box, Integer> boxLine) {
		currentLineNumber = boxLine;
	}

	private void handleScriptException(Throwable e, Consumer<Pair<Integer, String>> lineErrors, Function<Integer, Integer> lineTransform) {
		System.out.println(" handling exception to :" + lineErrors);
		try {
			if (e instanceof ScriptException) {
				lineErrors.accept(new Pair<>(lineTransform.apply(((ScriptException) e).getLineNumber()), e.getMessage()));
				e.printStackTrace();
			} else {                        // let's see if we can't scrape a line number out of the exception stacktrace
				StackTraceElement[] s = e.getStackTrace();
				boolean found = false;
				if (s != null) {
					for (int i = 0; i < s.length; i++) {
						if (s[i].getFileName() != null && s[i].getFileName()
										      .startsWith("bx[")) {
							lineErrors.accept(new Pair<>(lineTransform.apply(s[i].getLineNumber()), e.getMessage()));
							found = true;
						}
					}
				}
				if (!found) {
					lineErrors.accept(new Pair<>(-1, e.getMessage()+"<br>"));
				}
				e.printStackTrace();
			}
		} catch (Throwable t) {
			System.err.println(" exception thrown while handling an exception (!) (malfunctioning lineTransform?) ");
			t.printStackTrace();
			System.err.println(" original error is :" + e.getMessage());
			lineErrors.accept(new Pair<>(-1, e.getMessage()+"<br>"));
		}
	}

	private Object engineeval(String textFragment, ScriptContext context, Consumer<Throwable> exception) throws ScriptException {
		if (ThreadSync.enabled && Thread.currentThread() == ThreadSync.get().mainThread) {
			try {
				ThreadSync.Fiber f = ThreadSync.get()
							       .run(() -> engine.eval(textFragment, context), exception);
				f.tag = box;
				return f.lastReturn;

			} catch (InterruptedException e) {
				e.printStackTrace();

				return null;
			}
		} else {
			return engine.eval(textFragment, context);
		}
	}

	@Override
	public void executeAll(String allText, Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		lineOffset = 0;
		executeAndReturn(allText, lineErrors, success, true);
		lineOffset = 0;
	}

	@Override
	public void setLineOffsetForFragment(int line) {
		lineOffset = line;
	}

	@Override
	public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success, Map<String, Object> initiator) {
		context.setAttribute("_r", null, ScriptContext.ENGINE_SCOPE);

		initiator.entrySet()
			 .forEach(x -> context.setAttribute(x.getKey(), x.getValue(), ScriptContext.ENGINE_SCOPE));

		String allText = DisabledRangeHelper.getStringWithDisabledRanges(box, property, "/*", "*/");

		executeAndReturn(allText, lineErrors, success, true);
		Object _r = context.getBindings(ScriptContext.ENGINE_SCOPE)
				   .get("_r");

		Supplier<Boolean> r = interpretAnimation(_r);
		if (r != null) {
			end(lineErrors, success);
			String name = "main._animator_" + (uniq);
			box.properties.putToMap(Boxes.insideRunLoop, name, r);
			box.first(IsExecuting.isExecuting)
			   .ifPresent(x -> x.accept(box, name));

			uniq++;
			return name;
		}

		return null;
	}

	private Supplier<Boolean> interpretAnimation(Object r) {
		Animatable.AnimationElement res = Animatable.interpret(r, null);

		if (res == null) return null;

		return new Animatable.Shim(res);
	}

	@Override
	public void end(Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		Map<String, Supplier<Boolean>> m = box.properties.get(Boxes.insideRunLoop);
		if (m == null) return;
		for (String s : new ArrayList<>(m.keySet())) {
			if (s.contains("_animator_")) {
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
		List<Completion> r1 = ternSupport.completion(engine, box.properties.get(IO.id), allText, line, ch);

		System.out.println(" using completion :" + r1);

		if (r1 != null) {
			results.accept(r1);
		}

		this.box.find(Execution.completions, this.box.upwards())
			.flatMap(x -> x.values()
				       .stream())
			.forEach(x -> x.completion(this.box, allText, line, ch, results));
	}

	@Override
	public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {
		List<Completion> r1 = ternSupport.imports(engine, box.properties.get(IO.id), allText, line, ch);
		if (r1 != null) {
			results.accept(r1);
		}

		this.box.find(Execution.imports, this.box.upwards())
			.flatMap(x -> x.values()
				       .stream())
			.forEach(x -> x.completion(this.box, allText, line, ch, results));
	}

	public void setTernSupport(TernSupport ternSupport) {
		this.ternSupport = ternSupport;
	}

	public void setFilenameForStacktraces(String filename) {
		this.filename = filename;
	}

	@Override
	public String getCodeMirrorLanguageName() {
		return "javascript";
	}

	@Override
	public String getDefaultFileExtension() {
		return ".js";
	}

	public Object getBinding(String name) {
		return engine.get(name);
	}
}
