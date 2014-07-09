package fieldprocessing;

import field.utility.Dict;
import field.utility.Pair;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.plugins.IsExecuting;
import fieldbox.io.IO;
import fielded.Execution;
import fieldbox.boxes.Box;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**


 */
public class ProcessingExecution extends Execution {

	static public final Dict.Prop<Map<String, Supplier<Boolean>>> insideProcessingLoop = new Dict.Prop<>("_insideProcessingLoop");
	static public final Dict.Prop<Boolean> bridgedToProcessing = new Dict.Prop<>("bridgedToProcessing").toCannon();

	static {
		IO.persist(bridgedToProcessing);
	}

	private Execution delegateTo;
	private List<Runnable> queue;

	public ProcessingExecution(Execution delegateTo, List<Runnable> queue) {
		super(null);

		this.properties.put(Boxes.dontSave, true);

		this.delegateTo = delegateTo;
		this.queue = queue;
	}

	@Override
	public ExecutionSupport support(Box box, Dict.Prop<String> prop) {
		ExecutionSupport s = delegateTo.support(box, prop);

		return wrap(box, s);
	}

	private ExecutionSupport wrap(Box box, ExecutionSupport s) {

		return new ExecutionSupport() {
			@Override
			public void executeTextFragment(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				System.out.println(" WRAPPED :"+textFragment);
				queue.add(() -> {
					s.executeTextFragment(textFragment, lineErrors, success);
				});
			}

			@Override
			public void executeAndPrint(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				System.out.println(" WRAPPED :"+textFragment);
				queue.add(() -> {
					s.executeAndPrint(textFragment, lineErrors, success);
				});
			}

			@Override
			public void executeAll(String allText, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				System.out.println(" WRAPPED :"+allText);
				queue.add(() -> {
					s.executeAll(allText, lineErrors, success);
				});
			}

			@Override
			public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				System.out.println(" WRAPPED (begin)");
				String name = s.begin(lineErrors, success);
				if (name==null) return null;
				Supplier<Boolean> was = box.properties.removeFromMap(Boxes.insideRunLoop, name);
				String newName = name.replace("main.", "processing.");
				box.properties.putToMap(Boxes.insideRunLoop, newName, was);
				box.first(IsExecuting.isExecuting).ifPresent(x -> x.accept(box, newName));

				return name;
			}

			@Override
			public void end(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				System.out.println(" WRAPPED (end)");
				s.end(lineErrors, success);
			}

			@Override
			public void setConsoleOutput(Consumer<String> stdout, Consumer<String> stderr) {
				System.out.println(" WRAPPED (stdout)");
				s.setConsoleOutput(stdout, stderr);
			}

			@Override
			public void completion(String allText, int line, int ch, Consumer<List<Completion>> results) {
				System.out.println(" WRAPPED (completion) "+allText);
				s.completion(allText, line, ch, results);
			}

			@Override
			public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {
				System.out.println(" WRAPPED (imports) "+allText);
				s.imports(allText, line, ch, results);
			}

			@Override
			public String getCodeMirrorLanguageName() {
				return s.getCodeMirrorLanguageName();
			}

			@Override
			public String getDefaultFileExtension() {
				return s.getDefaultFileExtension();
			}
		};
	}
}
