package fieldprocessing;

import field.utility.*;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Mouse;
import fieldbox.boxes.plugins.IsExecuting;
import fieldbox.execution.Completion;
import fieldbox.execution.Execution;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The Processing Plugin. Refer to Processing.__applet to get at the __applet.
 * <p>
 * E.g. var P = Java.type('fieldprocessing.Processing').__applet
 * <p>
 * This adds a command "Bridge box to Processing". Run that to move this box (and any children) into the Processing draw cycle. Then you can write
 * things like:
 * <p>
 * P.background(0) // sets background to black
 */
public class Processing extends Execution {

	static public final Dict.Prop<FieldProcessingAppletDelgate> P = new Dict.Prop<FieldProcessingAppletDelgate>("P").toCannon().type().doc("the Processing Applet. e.g. `_.P.background(0)` sets the background to black.");

	private ProcessingExecution processingExecution;
	public FieldProcessingApplet __applet;
	public static FieldProcessingAppletDelgate applet;

	private List<Runnable> queue = new ArrayList<>();


	protected JFrame frame;

	int sizeX = AutoPersist.persist("processing_sizeX", () -> 400, x -> Math.min(2560, Math.max(100, x)), (x) -> frame.getSize().width);
	int sizeY = AutoPersist.persist("processing_sizeY", () -> 400, x -> Math.min(2560, Math.max(100, x)), (x) -> frame.getSize().height);

	public interface MouseHandler {
		public void handle(FieldProcessingApplet applet, Object/*MouseEvent or MouseMoveEvent*/ event);
	}

	public interface KeyHandler
	{
		public void handle(FieldProcessingApplet applet, processing.event.KeyEvent event);
	}


	public Processing(Box root) {
		super(null);

		Log.log("startup.processing", " processing plugin is starting up ");


		frame = new JFrame("Field/Processing");
		__applet = new FieldProcessingApplet(sizeX, sizeY, queue, this, s -> {
			if (getLastErrorOutput()!=null)
				getLastErrorOutput().accept(new Pair<>(-1, s));
		});

		__applet.init();
		__applet.loop();
		frame.add(__applet, BorderLayout.CENTER);
		frame.setSize(sizeX, sizeY);
		frame.setVisible(true);
		frame.validate();

		applet = new FieldProcessingAppletDelgate(__applet);

		this.properties.put(P, applet);



		Log.log("startup.processing", " searching for boxes that need processing support ");


		Log.log("startup.processing", " processing plugin has finished starting up ");


	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}


	public Execution.ExecutionSupport support(Box box, Dict.Prop<String> prop) {
		FunctionOfBox<Boolean> ef = this.properties.get(executionFilter);
		if (box==this || ef == null || ef.apply(box)) return wrap(box, prop);
		return null;
	}

	public Consumer<Pair<Integer, String>> lastErrorOutput;

	public Consumer<Pair<Integer, String>> getLastErrorOutput() {
		return lastErrorOutput;
	}

	private Execution.ExecutionSupport wrap(Box box, Dict.Prop<String> prop) {

		return new Execution.ExecutionSupport() {

			@Override
			public void executeTextFragment(String textFragment, String suffix, Consumer<String> success, Consumer<Pair<Integer, String>> lineErrors) {
				System.out.println(" WRAPPED :"+textFragment);
				queue.add(() -> {
					Execution delegateTo = box.find(Execution.execution, box.upwards())
								  .findFirst()
								  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
					Execution.ExecutionSupport s = delegateTo.support(box, prop);

					s.executeTextFragment(textFragment, "", success, lineErrors);
				});
			}

			@Override
			public Object getBinding(String name) {
				Execution delegateTo = box.find(Execution.execution, box.upwards())
							  .findFirst()
							  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
				Execution.ExecutionSupport s = delegateTo.support(box, prop);
				return s.getBinding(name);
			}



			@Override
			public void executeAll(String allText, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
				System.out.println(" WRAPPED :"+allText);
				queue.add(() -> {
					Execution delegateTo = box.find(Execution.execution, box.upwards())
								  .findFirst()
								  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
					Execution.ExecutionSupport s = delegateTo.support(box, prop);
					s.executeAll(allText, lineErrors, success);
				});
			}

			@Override
			public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success, Map<String, Object> initiator) {

				lastErrorOutput = lineErrors;
				Execution delegateTo = box.find(Execution.execution, box.upwards())
							  .findFirst()
							  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
				Execution.ExecutionSupport s = delegateTo.support(box, prop);

				String name = s.begin(lineErrors, success, initiator);
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
				Execution delegateTo = box.find(Execution.execution, box.upwards()).filter(x -> x != Processing.this)
							  .findFirst()
							  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
				Execution.ExecutionSupport s = delegateTo.support(box, prop);
				s.end(lineErrors, success);
			}

			@Override
			public void setConsoleOutput(Consumer<String> stdout, Consumer<String> stderr) {
				System.out.println(" WRAPPED (stdout)");
				Execution delegateTo = box.find(Execution.execution, box.upwards()).filter(x -> x != Processing.this)
							  .findFirst()
							  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
				Execution.ExecutionSupport s = delegateTo.support(box, prop);
				s.setConsoleOutput(stdout, stderr);
			}

			@Override
			public void completion(String allText, int line, int ch, Consumer<List<Completion>> results) {
				System.out.println(" WRAPPED (completion) "+allText);
				Execution delegateTo = box.find(Execution.execution, box.upwards()).filter(x -> x != Processing.this)
							  .findFirst()
							  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
				Execution.ExecutionSupport s = delegateTo.support(box, prop);
				s.completion(allText, line, ch, results);
			}

			@Override
			public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {
				System.out.println(" WRAPPED (imports) "+allText);
				Execution delegateTo = box.find(Execution.execution, box.upwards()).filter(x -> x != Processing.this)
							  .findFirst()
							  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
				Execution.ExecutionSupport s = delegateTo.support(box, prop);
				s.imports(allText, line, ch, results);
			}

			@Override
			public String getCodeMirrorLanguageName() {
				Execution delegateTo = box.find(Execution.execution, box.upwards()).filter(x -> x != Processing.this)
							  .findFirst()
							  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
				Execution.ExecutionSupport s = delegateTo.support(box, prop);
				return s.getCodeMirrorLanguageName();
			}

			@Override
			public String getDefaultFileExtension() {
				Execution delegateTo = box.find(Execution.execution, box.upwards()).filter(x -> x != Processing.this)
							  .findFirst()
							  .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
				Execution.ExecutionSupport s = delegateTo.support(box, prop);
				return s.getDefaultFileExtension();
			}
		};
	}


}
