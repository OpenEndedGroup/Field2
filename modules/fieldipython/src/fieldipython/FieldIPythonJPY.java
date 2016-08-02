package fieldipython;

import field.app.RunLoop;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Mouse;
import fieldbox.boxes.plugins.Planes;
import fieldbox.execution.Completion;
import fieldbox.execution.Execution;
import fieldipython.zmq.IPythonInterface;
import fieldipython.zmq.IPythonTransform;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by marc on 4/13/15.
 */
public class FieldIPythonJPY extends Execution {

	static public final Dict.Prop<JpySystem> buffers = new Dict.Prop<>("buffers").toCannon().doc("Access live updating NumPy memory views at little to no cost. This is set on `_.root` not on the Ipython box.");

	public final IPythonInterface i;
	private JpySystem jpySystem;
	private Consumer<Pair<Integer, String>> errors = null;
	private Consumer<String> output = null;

	public FieldIPythonJPY(Box root) {
		super(null);

		this.properties.put(Planes.plane, "__always__");

		try {
			// todo, make this global on root
			jpySystem = new JpySystem(root, this);

			root.properties.put(buffers, jpySystem);

		} catch (ExecutionException e) {
			//TOOD: report this
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Log.log("startup.ipython", ()->"IPythonplugin(Jpy) is starting up ");

		i = new IPythonTransform().get();

		Dict o = this.properties.putToMap(Boxes.insideRunLoop, "main.__ipythonoutputupdate__", () -> {
			if (errors == null) return true;

			List<String> out = i.getOutput();
			List<String> err = i.getErrors();

			if (out.size() > 0) System.out.println(":: output " + out);


			for (String s : out) {
				if (s.trim()
				     .length() == 0) continue;

				// take a horrible guess as to whether this is html or not.
				if (s.contains("<") && s.contains(">"))
				{

				}
				else
				{
					s = s.replaceAll("\n\n\n", "<br>");
					s = s.replaceAll("\n\n", "<br>");
					s = s.replaceAll("\n", "<br>");
				}

				output.accept(s);
			}
			for (String s : err)
				errors.accept(new Pair<>(-1, s));


			return true;
		});


		Log.log("startup.ipython", ()->"IPythonplugin(Jpy) has finished starting up ");
	}


	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

	private Object safeGet(Collection o, int i) {
		Iterator a = o.iterator();
		Object last = null;
		for (int ii = 0; ii < i + 1; ii++) {
			if (a.hasNext()) last = a.next();
			else break;
		}
		return last;
	}

	@Override
	public Execution.ExecutionSupport support(Box box, Dict.Prop<String> prop) {
		FunctionOfBox<Boolean> ef = this.properties.get(executionFilter);
		if (box == this || ef == null || ef.apply(box)) return wrap(box, prop);
		return null;
	}

	private Execution.ExecutionSupport wrap(Box box, Dict.Prop<String> prop) {

		return new Execution.ExecutionSupport() {

			public int lineOffset = 0;
			long uniq;
			Set term = new LinkedHashSet<Character>(Arrays.asList('(', ')', ' ', '[', ']', '{', '}'));

			@Override
			public Object executeTextFragment(String textFragment, String suffix, Consumer<String> success, Consumer<Pair<Integer, String>> lineErrors) {

				try {
					Execution.context.get()
							 .push(box);


					if (lineOffset > 0) {
						for (int i = 0; i < lineOffset; i++)
							textFragment = "\n" + textFragment;
					}

					Object result = eval(textFragment, lineErrors, success);

					success.accept(result != null ? ("" + result) : "");

					return result;
				} finally {
					Execution.context.get()
							 .pop();
				}
			}


			@Override
			public Object getBinding(String name) {

				return null;
			}

			@Override
			public void executeAll(String allText, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				executeTextFragment(allText, "", success, lineErrors);
			}

			@Override
			public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success, Map<String, Object> initiator) {

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

				if (!RunLoop.main.isMainThread()) {
					System.err.println(" skipping autocomplete while we sort out threading issues ");
					return;
				}


				if (allText.trim()
					   .length() == 0) return;

				String[] lines = allText.split("\n");
				int c = 0;
				for (int i = 0; i < line; i++) {
					c += lines[i].length() + 1;
				}

				c += ch;
				int cf = c;

				List<String> a = i.inpsectCallable(allText.substring(0, c), 1);

				List all = new ArrayList<>();


				List<Triple<Integer, Integer, String>> completions = i.complete(allText.substring(0, c));

				boolean[] isFirst = {true};
				all.addAll(completions.stream()
						      .map(s -> {
							      Completion cc = new Completion(s.first, s.second, s.third, "");

							      System.out.println("cc :" + s.first + " " + s.second + " " + s.third + " |" + allText.substring(s.first, s.second) + "|");

							      if (allText.substring(s.first, s.second)
									 .equals(s.third) && isFirst[0] && a.size() > 0) {
								      isFirst[0] = false;
								      cc.info = a.get(0);
							      }
							      return cc;
						      })
						      .collect(Collectors.toList()));

				results.accept(all);

			}


			@Override
			public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {


			}

			@Override
			public String getCodeMirrorLanguageName() {
				return "python";
			}

			@Override
			public String getDefaultFileExtension() {
				return ".py";
			}

			@Override
			public void setLineOffsetForFragment(int lineOffset, Dict.Prop<String> origin) {
				this.lineOffset = lineOffset;
			}
		};
	}

	protected Object eval(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> output) {
		this.errors = lineErrors;
		this.output = output;
		i.send(textFragment);
		return null;
	}

	private int scourStacktraceForBoxes(Throwable c) {
		Pattern p = Pattern.compile("bx\\[.*\\]");
		for (StackTraceElement s : c.getStackTrace()) {

			if (s.getFileName() == null) continue;

			if (p.matcher(s.getFileName())
			     .find()) {
				return s.getLineNumber();
			}
		}
		return -1;
	}
}
