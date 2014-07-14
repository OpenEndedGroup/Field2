package fielded;

import field.utility.Dict;
import field.utility.Pair;
import fieldbox.boxes.Box;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interfaces for supporting code execution and runtimes in Field
 */
public class Execution extends Box {

	static public Dict.Prop<Execution> execution = new Dict.Prop<Execution>("execution");

	private final BiFunction<Box, Dict.Prop<String>, ? extends ExecutionSupport> support;

	/**
	 * this is the default "code" property for a box. Obviously we can define others, and editors can edit other properties, but there's a lot of
	 * UI that's built around a default notion of "run"ing and "begin"ing a box.
	 */
	static public final Dict.Prop<String> code = new Dict.Prop<>("code");

	/**
	 * helper class for completion results
	 */
	static public class Completion {
		public int start, end;
		public String replacewith;
		public String info;
		public String header;

		public Completion(int start, int end, String replacewith, String info) {
			this.start = start;
			this.end = end;
			this.replacewith = replacewith;
			this.info = info;
		}

		@Override
		public String toString() {
			return "comp<" + replacewith + " | " + info + ">";
		}
	}

	/**
	 * absolutely everything you need to support a language in Field
	 */
	static public interface ExecutionSupport {

		public void executeTextFragment(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success);

		public void executeAndPrint(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success);

		public void executeAll(String allText, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success);

		public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success);

		public void end(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success);

		public void setConsoleOutput(Consumer<String> stdout, Consumer<String> stderr);

		public void completion(String allText, int line, int ch, Consumer<List<Completion>> results);

		public void imports(String allText, int line, int ch, Consumer<List<Completion>> results);

		public String getCodeMirrorLanguageName();

		public String getDefaultFileExtension();

		default public void setFilenameForStacktraces(String name)
		{

		}

		default public void begin(Box box) {

			Function<Box, Consumer<Pair<Integer, String>>> ef = box.first(RemoteEditor.outputErrorFactory)
				    .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is)));
			Function<Box, Consumer<String>> of = box.first(RemoteEditor.outputFactory)
				    .orElse(x -> (is -> System.out.println("output (without remote editor attached) :" + is)));

			begin(ef.apply(box), of.apply(box));
		}

		default public void executeTextFragment(String allText, Box box) {
			executeTextFragment(allText, box.first(RemoteEditor.outputErrorFactory)
				    .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is))).apply(box), box
				    .first(RemoteEditor.outputFactory)
				    .orElse(x -> (is -> System.out.println("output (without remote editor attached) :" + is))).apply(box));
		}

		default public void executeAndPrint(String allText, Box box) {
			executeAndPrint(allText, box.first(RemoteEditor.outputErrorFactory)
				    .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is))).apply(box), box
				    .first(RemoteEditor.outputFactory)
				    .orElse(x -> (is -> System.out.println("output (without remote editor attached) :" + is))).apply(box));
		}

		default public void executeAll(String allText, Box box) {
			executeAll(allText, box.first(RemoteEditor.outputErrorFactory)
				    .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is))).apply(box), box
				    .first(RemoteEditor.outputFactory)
				    .orElse(x -> (is -> System.out.println("output (without remote editor attached) :" + is))).apply(box));
		}

		default public void end(Box box) {
			end(box.first(RemoteEditor.outputErrorFactory)
				    .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is))).apply(box), box
				    .first(RemoteEditor.outputFactory)
				    .orElse(x -> (is -> System.out.println("output (without remote editor attached) :" + is))).apply(box));
		}
	}

	/**
	 * multi language abstractions enter Field here
	 */
	public Execution(BiFunction<Box, Dict.Prop<String>, ? extends ExecutionSupport> support) {
		this.support = support;
		this.properties.put(execution, this);
	}

	/**
	 * returns the ExecutionSupport for a particular property for a particular box here
	 */
	public ExecutionSupport support(Box box, Dict.Prop<String> prop) {
		return support.apply(box, prop);
	}


}
