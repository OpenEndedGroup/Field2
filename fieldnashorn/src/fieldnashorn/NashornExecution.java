package fieldnashorn;

import field.graphics.FLine;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Rect;
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
			if (success != null) {

				writer = new Writer() {
					StringBuilder b = new StringBuilder();

					@Override
					public void write(char[] cbuf, int off, int len) throws IOException {
						if (len>0) {
							String s = new String(cbuf, off, len);
							if (s.endsWith("\n")) s = s.substring(0, s.length()-1);
							if (s.trim().length()==0) return;
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

			System.out.println("\n>>javascript in");
			System.out.println(textFragment);
			Object ret = engine.eval(textFragment, context);
			System.out.println("\n<<javascript out" + ret + " " + (ret != null ? ret.getClass() + "" : ""));
			if (writer != null) writer.flush();
			if (success != null) if (ret != null) success.accept("" + ret);

			RemoteEditor.boxFeedback(Optional.of(box), new Vec4(0.3f, 0.7f, 0.3f, 0.5f));

		} catch (ScriptException e) {
			lineErrors.accept(new Pair<>(e.getLineNumber(), e.getMessage()));
			e.printStackTrace();
		} catch (Throwable t) {
			lineErrors.accept(new Pair<>(-1, t.getMessage()));
			t.printStackTrace();
		}
	}

	@Override
	public void executeAndPrint(String textFragment, Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		executeAndReturn(textFragment, lineErrors, success, true);
	}

	@Override
	public void executeAll(String allText, Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		executeAndReturn(allText, lineErrors, success, false);
	}

	int uniq = 0;

	@Override
	public void begin(Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		context.setAttribute("_r", null, ScriptContext.ENGINE_SCOPE);

		String allText = box.first(property).orElse("");

		executeAndReturn(allText, lineErrors, success, false);
		Object _r = context.getBindings(ScriptContext.ENGINE_SCOPE).get("_r");

		System.out.println(" interpreting :" + _r + " " + (_r == null ? null : _r.getClass()));

		Supplier<Boolean> r = interpretAnimation(_r);
		System.out.println(" obtained :" + r);
		if (r != null) {
			end(lineErrors, success);
			String name = "_animator_" + (uniq);
			box.properties.putToMap(Boxes.insideRunLoop, name, r);
			box.first(IsExecuting.isExecuting).ifPresent( x-> x.accept(box, name));

			uniq++;
		}
	}

	private Supplier<Boolean> interpretAnimation(Object r) {
		Animatable.AnimationElement res = Animatable.interpret(r, null);

		if (res==null) return null;

		return new Animatable.Shim(res);
	}

	@Override
	public void end(Consumer<field.utility.Pair<Integer, String>> lineErrors, Consumer<String> success) {
		Map<String, Supplier<Boolean>> m = box.properties.get(Boxes.insideRunLoop);
		if (m == null) return;
		for (String s : new ArrayList<>(m.keySet())) {
			if (s.startsWith("_animator_")) {
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

	@Override
	public String getCodeMirrorLanguageName() {
		return "javascript";
	}

	@Override
	public String getDefaultFileExtension() {
		return ".js";
	}
}
