package fielded.plugins;

import field.graphics.FLine;
import field.app.RunLoop;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.execution.Completion;
import fieldbox.io.IO;
import fieldbox.execution.Execution;
import fielded.RemoteEditor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static field.graphics.StandardFLineDrawing.*;
import static fieldbox.boxes.FLineDrawing.*;


/**
 * Bridges code execution directly to the text editor
 * <p>
 * (Developed after discussion with Peter, very similar to ProcessingPlugin)
 */
public class BridgeToTextEditor extends Box {

	static public final Dict.Prop<Boolean> bridgedToEditor = new Dict.Prop<>("bridgedToEditor").toCannon();

	static {
		IO.persist(bridgedToEditor);
	}

	TextEditorExecution teExecution;
	private BridgedTernSupport tern;

	public BridgeToTextEditor(Box root) {
		root.find(Watches.watches, both()).findFirst().ifPresent(w -> w.addWatch(RemoteEditor.editor, (q) -> lazyInit(root)));
	}

	protected void lazyInit(Box root) {
		RemoteEditor delegate = root.find(RemoteEditor.editor, root.both()).findFirst()
			    .orElseThrow(() -> new IllegalArgumentException(" can't instantiate BridgeToTextEditor execution - no remote editor found"));
		teExecution = new TextEditorExecution(delegate);

		root.connect(teExecution);

		properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			List<Box> selected = selection().collect(Collectors.toList());
			if (selected.size() == 1) {
				if (selected.get(0).properties.isTrue(bridgedToEditor, false)) {
					m.put(new Pair<>("Remove bridge to Editor", "No longer will this box execute inside this editor"), () -> {
						disconnectFromEditor(selected.get(0));
					});
				} else {
					m.put(new Pair<>("Bridge to Editor", "This box will execute inside this editor. To print, use <code>_field.log</code> rather than <code>console.log</code>"), () -> {
						connectToEditor(selected.get(0));
					});
				}
			}
			return m;
		});


		Log.log("startup.editor", " searching for boxes that need editor support ");

		// we delay this for one update cycle to make sure that everybody has loaded everything that they are going to load
		RunLoop.main.once(() -> {
			root.breadthFirst(both()).forEach(box -> {
				if (box.properties.isTrue(bridgedToEditor, false)) {
					connectToEditor(box);
				}
			});
		});

		Log.log("startup.editor", " editor plugin is going to init Tern ");

		tern = new BridgedTernSupport();
		tern.inject(x -> delegate.sendJavaScript(x));

		Log.log("startup.editor", " editor plugin has finished starting up ");


	}

	protected void connectToEditor(Box box) {
		teExecution.connect(box);
		box.properties.put(bridgedToEditor, true);

		box.properties.putToMap(frameDrawing, "_editorBadge_", new Cached<Box, Object, FLine>((b, was) -> {

			Rect rect = box.properties.get(Box.frame);
			if (rect == null) return null;

			FLine f = new FLine();
			f.attributes.put(hasText, true);
			f.attributes.put(fillColor, new Vec4(0, 0, 0.25f, 0.5f));
			f.moveTo(rect.x + rect.w - 7, rect.y + rect.h - 5);
			f.nodes.get(f.nodes.size() - 1).attributes.put(text, "T");

			return f;

		}, (b) -> new Pair(b.properties.get(bridgedToEditor), b.properties.get(Box.frame))));
		Drawing.dirty(box);

	}

	protected void disconnectFromEditor(Box box) {
		teExecution.disconnect(box);
		box.properties.remove(bridgedToEditor);
		box.properties.removeFromMap(frameDrawing, "_editorBadge_");
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

	public class TextEditorExecution extends Execution {


		private RemoteEditor delegateTo;
		private List<Runnable> queue;

		public TextEditorExecution(RemoteEditor delegateTo) {
			super(null);

			this.properties.put(Boxes.dontSave, true);

			this.delegateTo = delegateTo;
			this.queue = queue;
		}

		@Override
		public Execution.ExecutionSupport support(Box box, Dict.Prop<String> prop) {
			return wrap(box);
		}

		private Execution.ExecutionSupport wrap(Box box) {

			return new Execution.ExecutionSupport() {

				protected Util.ExceptionlessAutoCloasable previousPush = null;

				@Override
				public Object getBinding(String name) {
					return null;
				}

				@Override
				public void executeTextFragment(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {

					if (previousPush!=null) previousPush.close();;
					previousPush = delegateTo
						    .pushToLogStack(s -> success.accept(s), e -> lineErrors.accept(new Pair<>(0, e)));
					delegateTo.sendJavaScript(textFragment);
				}

				@Override
				public void executeAndPrint(String textFragment, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
					executeTextFragment(textFragment, lineErrors, success);
				}

				@Override
				public void executeAll(String allText, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
					executeTextFragment(allText, lineErrors, success);
				}

				@Override
				public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success, Map<String, Object> initiator) {
					//TODO
/*
					System.out.println(" WRAPPED (begin)");
					String name = s.begin(lineErrors, success);
					if (name == null) return null;
					Supplier<Boolean> was = box.properties.removeFromMap(Boxes.insideRunLoop, name);
					String newName = name.replace("main.", "editor.");
					box.properties.putToMap(Boxes.insideRunLoop, newName, was);
					box.first(IsExecuting.isExecuting).ifPresent(x -> x.accept(box, newName));

					return name;
					*/
					return null;
				}

				@Override
				public void end(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
					//TODO
				}

				@Override
				public void setConsoleOutput(Consumer<String> stdout, Consumer<String> stderr) {
				}

				@Override
				public void completion(String allText, int line, int ch, Consumer<List<Completion>> results) {
					tern.completion(x -> delegateTo.sendJavaScript(x), "remoteFieldProcess", allText, line, ch);
					results.accept(Collections.emptyList());
				}

				@Override
				public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {
				}

				@Override
				public String getCodeMirrorLanguageName() {
					return "javascript";
				}

				@Override
				public String getDefaultFileExtension() {
					return ".js";
				}
			};
		}
	}
}
