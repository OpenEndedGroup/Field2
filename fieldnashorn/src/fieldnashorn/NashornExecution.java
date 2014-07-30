package fieldnashorn;

import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.IsExecuting;
import fieldbox.io.IO;
import fielded.Animatable;
import fielded.Execution;
import fielded.RemoteEditor;

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
import java.util.function.Supplier;

/**
 * An implementation of Execution.ExecutionSupport for Nashorn/Javascript
 */
public class NashornExecution implements Execution.ExecutionSupport {

	private final Dict.Prop<String> property;
	private final Box box;
	private final ScriptContext context;
	private final ScriptEngine engine;
	private TernSupport ternSupport;
	public String filename = null;
	private int lineOffset;

	public NashornExecution(Box box, Dict.Prop<String> property, ScriptContext b, ScriptEngine engine) {
		this.box = box;
		this.property = property;
		this.context = b;
		this.engine = engine;
	}


	@Override
	public void executeTextFragment(String textFragment, Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		executeAndReturn(textFragment, lineErrors, success, false);
	}

	protected void executeAndReturn(String textFragment, Consumer<Pair<Integer, String>> lineErrors, final Consumer<String> success, boolean printResult) {

		try {
			Writer writer = null;
			boolean[] written = {false};
			if (success != null) {


				writer = new Writer() {
					StringBuilder b = new StringBuilder();

					@Override
					public void write(char[] cbuf, int off, int len) throws IOException {
						if (len > 0) {
							String s = new String(cbuf, off, len);
							if (s.endsWith("\n")) s = s.substring(0, s.length() - 1);
							if (s.trim().length() == 0) return;
							written[0] = true;
							success.accept(s);
						}
					}

					@Override
					public void flush() throws IOException {
					}

					@Override
					public void close() throws IOException {

					}
				};
				engine.getContext().setWriter(writer);
			}

			Log.log("nashorn.general", "\n>>javascript in");
			Log.log("nashorn.general", textFragment);

			engine.put("__LINE__", 200);


			// we prefix the code with a sufficient number of \n's so that the line number of any error message actually refers to the correct line
			// dreadful hack, but there's no other option right now in Nashorn (sourceMaps aren't supported for example)
			StringBuffer prefix = new StringBuffer(lineOffset);
			for(int i=0;i<lineOffset;i++)
				prefix.append('\n');

			if (filename!=null)
				textFragment = prefix+textFragment+"//# sourceURL="+filename;


			Object ret = engine.eval(textFragment, context);
			Log.log("nashorn.general", () -> "\n<<javascript out" + ret + " " + (ret != null ? ret.getClass() + "" : ""));
			if (writer != null) writer.flush();
			if (success != null) if (ret != null) success.accept("" + ret); else if (!written[0]) success.accept(" &#10003; ");

			RemoteEditor.boxFeedback(Optional.of(box), new Vec4(0.3f, 0.7f, 0.3f, 0.5f));

		} catch (ScriptException e) {
			lineErrors.accept(new Pair<>(e.getLineNumber(), e.getMessage()));
			e.printStackTrace();
		} catch (Throwable t) {
			lineErrors.accept(new Pair<>(-1, t.getMessage()));
			t.printStackTrace();
		}
		finally
		{
			lineOffset = 0;
		}
	}

	@Override
	public void executeAndPrint(String textFragment, Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		executeAndReturn(textFragment, lineErrors, success, true);
		lineOffset = 0;
	}

	@Override
	public void executeAll(String allText, Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		lineOffset = 0;
		executeAndReturn(allText, lineErrors, success, false);
		lineOffset = 0;
	}


	@Override
	public void setLineOffsetForFragment(int line)
	{
		lineOffset = line;
	}
	int uniq = 0;

	@Override
	public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
		context.setAttribute("_r", null, ScriptContext.ENGINE_SCOPE);

		String allText = box.first(property).orElse("");

		executeAndReturn(allText, lineErrors, success, false);
		Object _r = context.getBindings(ScriptContext.ENGINE_SCOPE).get("_r");

		Supplier<Boolean> r = interpretAnimation(_r);
		if (r != null) {
			end(lineErrors, success);
			String name = "main._animator_" + (uniq);
			box.properties.putToMap(Boxes.insideRunLoop, name, r);
			box.first(IsExecuting.isExecuting).ifPresent(x -> x.accept(box, name));

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
	public void completion(String allText, int line, int ch, Consumer<List<Execution.Completion>> results) {
		List<Execution.Completion> r1 = ternSupport.completion(box.properties.get(IO.id), allText, line, ch);
		if (r1 != null) {
			results.accept(r1);
		}
	}

	@Override
	public void imports(String allText, int line, int ch, Consumer<List<Execution.Completion>> results) {
		List<Execution.Completion> r1 = ternSupport.imports(box.properties.get(IO.id), allText, line, ch);
		if (r1 != null) {
			results.accept(r1);
		}
	}

	public void setTernSupport(TernSupport ternSupport) {
		this.ternSupport = ternSupport;
	}

	public void setFilenameForStacktraces(String filename)
	{
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
}
