package fieldnashorn;

import field.app.ThreadSync;
import field.app.ThreadSync2;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Callbacks;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.plugins.Chorder;
import fieldbox.boxes.plugins.IsExecuting;
import fieldbox.boxes.plugins.ThreadSync2Feedback;
import fieldbox.execution.Completion;
import fieldbox.execution.Errors;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fielded.Animatable;
import fielded.DisabledRangeHelper;
import fielded.RemoteEditor;
import fielded.plugins.Out;
import fieldnashorn.babel.SourceTransformer;
import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An implementation of Execution.ExecutionSupport for Nashorn/Javascript
 */
public class NashornExecution implements Execution.ExecutionSupport {

    static public final Dict.Prop<SourceTransformer> sourceTransformer = new Dict.Prop<SourceTransformer>(
            "sourceTransformer").doc(
            "an instanceof of a SourceTransformer that will take the source code here and transform it into JavaScript. This allows things like Babel.js to be used in Field")
            .toCanon();
    static public final ThreadLocal<ScriptEngine> currentEngine = new ThreadLocal<>();
    static int uniq = 0;
    public final Dict.Prop<String> property;
    public final Box box;
    public final ScriptContext context;
    final String prefix = "" + (uniq++);
    private final ScriptEngine engine;
    private final Out output;
    public String filename = null;
    Function<Integer, Integer> lineTransform;
    private Nashorn factory;
    private TernSupport ternSupport;
    private int lineOffset;
    private Triple<Box, Integer, Boolean> currentLineNumber = null;
    private Dict.Prop<String> originProperty;
    private boolean all = false;

    public NashornExecution(Nashorn factory, Box box, Dict.Prop<String> property, ScriptContext b, ScriptEngine engine) {
        this.factory = factory;
        this.box = box;
        this.property = property;
        this.context = b;
        this.engine = engine;

        output = box.find(Out.__out, box.both())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find html output support"));
    }

    @Override
    public Object executeTextFragment(String textFragment, String suffix, Consumer<String> success, Consumer<Pair<Integer, String>> lineErrors) {
        if (suffix.equals("print")) {
            return executeAndReturn("print(" + textFragment + ")", lineErrors, success, false);
        } else return executeAndReturn(textFragment, lineErrors, success, !suffix.equals("noprint"));
    }

    protected Object executeAndReturn(String textFragment, Consumer<Pair<Integer, String>> lineErrors, final Consumer<String> success, boolean printResult) {

        lineErrors = new ErrorHelper().errorHelper(lineErrors, box);
        Callbacks.call(box, Callbacks.onExecute);
        try {

            Execution.context.get().push(box);

            Writer writer = null;
            boolean[] written = {false};
            if (success != null) {

                currentLineNumber = null;


                writer = new Writer() {
                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        if (len > 0) {
                            String s = new String(cbuf, off, len);
//							if (s.endsWith("\n"))
//								s = s.substring(0, s.length() - 1) + "<br>";
                            if (s.trim().length() == 0) return;
                            written[0] = true;

                            if (currentLineNumber == null || currentLineNumber.first == null || currentLineNumber.second == -1) {
//								success.accept(s);
                                final String finalS = s;
                                Set<Consumer<Quad<Box, Integer, String, Boolean>>> o = box.find(
                                        Execution.directedOutput, box.upwards())
                                        .collect(Collectors.toSet());
                                o.forEach(x -> x.accept(new Quad<>(box, -1, finalS, true)));

                            } else {

                                final String finalS = s;
                                Set<Consumer<Quad<Box, Integer, String, Boolean>>> o = box.find(
                                        Execution.directedOutput, box.upwards())
                                        .collect(Collectors.toSet());

                                if (o.size() > 0) {
                                    o.forEach(x -> x.accept(
                                            new Quad<>(currentLineNumber.first, currentLineNumber.second, finalS,
                                                       currentLineNumber.third)));
                                } else {
//									success.accept(finalS);

									o.forEach(x -> x.accept(new Quad<>(box, -1, finalS, true)));

								}
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

			Log.log("nashorn.general", () -> "\n>>javascript in");
			final String finalTextFragment = textFragment;
			Log.log("nashorn.general", () -> finalTextFragment);

			// we prefix the code with a sufficient number of \n's so that the line number of any error message actually refers to the correct line
			// dreadful hack, but there's no other option right now in Nashorn (perhaps with sourceMaps?)
			StringBuffer prefix = new StringBuffer(Math.max(0, lineOffset));
			for (int i = 0; i < lineOffset; i++)
				prefix.append('\n');

			textFragment = prefix + textFragment + (filename == null ? "" : ("//# sourceURL=" + filename + "\n"));

			//TODO: should be find?
			SourceTransformer st = box.properties.get(sourceTransformer);


			if (st != null) {
				try {
					Pair<String, Function<Integer, Integer>> transformation = st.transform(textFragment, !all);
					textFragment = transformation.first;
					lineTransform = transformation.second;
				} catch (SourceTransformer.TranslationFailedException t) {
					lineErrors.accept(new Pair<>(-1, t.getMessage() + "<br>"));
					return null;
				}
			} else {
				lineTransform = x -> x;
			}

			RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__");

			output.setWriter(writer, this::setCurrentLineNumberForPrinting);

			Consumer<Pair<Integer, String>> finalLineErrors = lineErrors;
			boolean[] error = {false};

			Object ret = engineeval(textFragment, context, e -> {
				error[0] = true;
				handleScriptException(e, finalLineErrors, lineTransform);
			});

			Log.log("nashorn.general", () -> "\n<<javascript out" + ret + " " + (ret != null ? ret.getClass() + "" : ""));
			if (writer != null) writer.flush();
			if (success != null && printResult && !written[0] && !error[0]) {
				if (ret != null) {
					if (ret instanceof ScriptObjectMirror && ((ScriptObjectMirror) ret).isFunction()) {
						success.accept("[function defined]<br>");
					} else {
						success.accept(output.convert(ret) + "<br>");
					}
				} else if (!written[0]) success.accept(" &#10003; ");
			}

			RemoteEditor.boxFeedback(Optional.of(box), new Vec4(0.3f, 0.7f, 0.3f, 0.5f));

			return ret;
		} catch (ScriptException e) {
			handleScriptException(e, lineErrors, lineTransform);
		} catch (Throwable t) {
			handleScriptException(t, lineErrors, x -> x);

		} finally {
			lineOffset = 0;
			Execution.context.get().pop();
		}
		return null;
	}

	private void setCurrentLineNumberForPrinting(Triple<Box, Integer, Boolean> boxLine) {
		currentLineNumber = boxLine;
	}


	private void handleScriptException(Throwable t, Consumer<Pair<Integer, String>> lineErrors, Function<Integer, Integer> lineTransform) {
		handleScriptException(t, lineErrors, lineTransform, "");
	}

	private void handleScriptException(Throwable e, Consumer<Pair<Integer, String>> lineErrors, Function<Integer, Integer> lineTransform, String extraMessage) {

		RemoteEditor.boxFeedback(Optional.of(box), new Vec4(1, 0, 0, 0.5), "__redmark__", -1, -1);

		try {
			if (e instanceof ThreadSync2.KilledException) {
			} if (e instanceof ScriptException) {
				lineErrors.accept(new Pair<>(lineTransform.apply(((ScriptException) e).getLineNumber()), extraMessage + " " + e.getMessage()));
				e.printStackTrace();
			} else if (e instanceof NashornException) {
				Integer lt = lineTransform.apply(((NashornException) e).getLineNumber());
				lineErrors.accept(new Pair<>(lt, extraMessage + " " + e.getMessage()));
				e.printStackTrace();
			} else {                        // let's see if we can't scrape a line number out of the exception stacktrace
				StackTraceElement[] s = e.getStackTrace();
				boolean found = false;
				if (s != null) {
					for (int i = 0; i < s.length; i++) {
						if (s[i].getFileName() != null && s[i].getFileName()
							.startsWith("bx[")) {
							String m = e.getMessage();
							if (m == null)
								m = "" + e.getClass();
							lineErrors.accept(new Pair<>(lineTransform.apply(s[i].getLineNumber()), extraMessage + " " + m));
							found = true;
						}
					}
				}
				if (!found) {
					lineErrors.accept(new Pair<>(-1, extraMessage + " " + e.getMessage() + "<br>"));
				}
				e.printStackTrace();
			}
		} catch (Throwable t) {
			System.err.println(" exception thrown while handling an exception (!) (malfunctioning lineTransform?) ");
			t.printStackTrace();
			System.err.println(" original error is :" + extraMessage + " " + e.getMessage());
			lineErrors.accept(new Pair<>(-1, extraMessage + " " + e.getMessage() + "<br>"));
		}
	}

	private Object engineeval(String textFragment, ScriptContext context, Consumer<Throwable> exception) throws ScriptException {
		Set<Throwable> seenBefore = new LinkedHashSet<>();
		if (ThreadSync.enabled && Thread.currentThread() == ThreadSync.get().mainThread) {
			try {
				ThreadSync.Fiber f = ThreadSync.get()
					.run("execution of {{" + textFragment + "}}", () -> engine.eval(textFragment, context), t -> {
						if (seenBefore.add(t))
							exception.accept(t);
					});
				f.tag = box;
				return f.lastReturn;

			} catch (InterruptedException e) {
				e.printStackTrace();

				return null;
			}
		}
		if (!box.properties.isTrue(ThreadSync2Feedback.mainThread, false) && ThreadSync2.getEnabled() && Thread.currentThread() == ThreadSync2.getSync().getMainThread()) {

			try {
				ThreadSync2.Fibre f = ThreadSync2.getSync()
						.launchAndServiceOnce("execution of {{" + textFragment + "}}",
											  () -> engine.eval(textFragment, context), t -> {
									if (!(t instanceof ThreadSync2.KilledException))
										if (seenBefore.add(t))
											exception.accept(t);
								});
			f.tag = box;
			return f.lastReturn;
			}
			catch(ThreadSync2.KilledException e)
			{			return null;
			}


		} else {
			Object ret = engine.eval(textFragment, context);

			return ret;
		}
	}

	@Override
	public void executeAll(String allText, Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		lineOffset = 0;
		all = true;
		try {
			executeAndReturn(allText, lineErrors, success, true);
			lineOffset = 0;
		} finally {
			all = false;
		}
	}

	@Override
	public void setLineOffsetForFragment(int line, Dict.Prop<String> origin) {
		lineOffset = line;
		originProperty = origin;
	}

	@Override
	public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success, Map<String, Object> initiator, boolean endOngoing) {

		if (!endOngoing) {
			return factory.fork(this, Collections.emptyMap()).begin(lineErrors, success, initiator, true); // there wont be anything ended here because there will be another prefix
		}
		// is "run" defined here or anywhere above?

		if (box.find(Callbacks.run, box.upwards()).findAny().isPresent()) {
			if (endOngoing) end(lineErrors, success);

			String name = "main._animator" + prefix + "_" + (uniq);
			boolean[] first = {true};

			box.properties.putToMap(Boxes.insideRunLoop, name, () -> {
				try {
					Callbacks.call(box, Callbacks.run, initiator, first[0], box.upwards());
				} catch (Throwable t) {
					Errors.INSTANCE.tryToReportTo(t, "Exception in `_.run()`, called from box `" + box + "`", null);
					first[0] = false;
					return false;
				}
				first[0] = false;
				return true;

			});
			box.first(IsExecuting.isExecuting)
				.ifPresent(x -> x.accept(box, name));

			uniq++;
			return name;
		}

		// otherwise, use the old system
		context.removeAttribute("_r", ScriptContext.ENGINE_SCOPE);
		context.removeAttribute("_r", ScriptContext.GLOBAL_SCOPE);

		initiator.entrySet()
			.forEach(x -> context.setAttribute(x.getKey(), x.getValue(), ScriptContext.ENGINE_SCOPE));

		String allText = DisabledRangeHelper.getStringWithDisabledRanges(box, property, "/* -- start -- ", "-- end -- */");

		try {
			all = true;
			executeAndReturn(allText, lineErrors, success, true);
		} finally {
			all = false;
		}

		Object _r = context.getAttribute("_r");

		// if _r is null, but that executeAndReturn has launched fibres then we need a dummy _r that calls _.fkill() for 'end' and calls _.end at the end of the fiber to synchronize the two
		// execution models we have here

		List<ThreadSync2.Fibre> fibres = ThreadSync2Feedback.fibresFor(box);
		if (_r == null && ThreadSync2.getEnabled() && fibres.size() > 0) {
			_r = new Animatable.AnimationElement() {
				@Override
				public Object end(boolean isEnding) {
					ThreadSync2Feedback.shouldEnd(box);
//					ThreadSync2Feedback.kill(box);
					return this;
				}
			};

			fibres.get(fibres.size() - 1).setOnExit(() -> {
				box.first(Chorder.end).ifPresent(x -> x.apply(box));
			});
		}

		Supplier<Boolean> r = interpretAnimation(_r);
		if (r != null) {
			if (endOngoing) end(lineErrors, success);
			String name = "main._animator" + prefix + "_" + (uniq);
			box.properties.putToMap(Boxes.insideRunLoop, name, r);
			box.first(IsExecuting.isExecuting)
				.ifPresent(x -> x.accept(box, name));

			uniq++;
			return name;
		}

		return null;
	}

	private Supplier<Boolean> interpretAnimation(Object r) {
		if (r instanceof Supplier && r instanceof Consumer) return (Supplier<Boolean>) r;
		Animatable.AnimationElement res = Animatable.interpret(r, null);
		if (res == null) return null;
		Animatable.Shim s = new Animatable.Shim(res);
		return s;
	}

	@Override
	public void end(Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		end("main\\._animator" + prefix + "_.*", lineErrors, success);
	}

	@Override
	public void end(String regex, Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		Map<String, Supplier<Boolean>> m = box.properties.get(Boxes.insideRunLoop);
		if (m == null) return;


		Pattern p = Pattern.compile(regex);

		for (String s : new ArrayList<>(m.keySet())) {
			if (p.matcher(s).matches()) {
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
	public void completion(String allText, int line, int ch, Consumer<List<Completion>> results, boolean explicitlyRequested) {
		List<Completion> r1 = ternSupport.completion(engine, box.properties.get(IO.id), allText, line, ch, explicitlyRequested);

		System.out.println(" completions are :" + r1.size());
		r1.forEach(x -> {
			System.out.println("   " + x);
		});

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
		return engine.getContext().getAttribute(name);
	}
}
