package fielded.plugins;

import field.app.RunLoop;
import field.utility.Dict;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fieldcef.plugins.TextEditor;
import fielded.Commands;
import fielded.RemoteEditor;
import fielded.ServerSupport;
import fielded.webserver.Server;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

/**
 * Exports a command for making a new, independent, text editor (with it's own server)
 *
 * Has problems with multiple editors on the same box (changes are never propagated)
 *
 * Put behind experimental plugin setting
 *
 * Created by marc on 6/8/16.
 */
public class MakeNewTextEditor extends Box {

	static public Dict.Prop<FunctionOfBox<Box>> makeNewTextEditor = new Dict.Prop<>("makeNewTextEditor").type().toCannon();

	static public Dict.Prop<TriFunctionOfBoxAnd<Box, String, Boolean>> setCurrentlyEdited = new Dict.Prop<>("setCurrentlyEdited").type().toCannon();

	public MakeNewTextEditor(Box root) {

		this.properties.put(Boxes.dontSave, true);

		this.properties.put(Commands.commands, () -> {
			if (selection().count() != 1) return Collections.EMPTY_MAP;

			Box target = selection().findFirst().get();

			LinkedHashMap<Pair<String, String>, Runnable> r = new LinkedHashMap<>();
			r.put(new Pair<String, String>("New Text Editor", "Makes a new, independent text editor"), () -> {
				makeNewTextEditor(target);
			});


			boolean anyUnPinned = target.breadthFirst(upwards()).filter(x -> x instanceof TextEditor).findFirst().map(x -> !((TextEditor) x).isPinned()).filter(x -> x).isPresent();
			if (anyUnPinned)
				r.put(new Pair<String, String>("Pin Editor", "Make this editor no longer track which box is selected"), () -> {
					pin(target);
				});
			boolean anyPinned = target.breadthFirst(upwards()).filter(x -> x instanceof TextEditor).findFirst().map(x -> ((TextEditor) x).isPinned()).filter(x -> x).isPresent();

			if (anyPinned)
				r.put(new Pair<String, String>("Unpin Editor", "Make this editor automatically track which box is selected"), () -> {
					unpin(target);
				});

			return r;
		});

		this.properties.put(makeNewTextEditor, MakeNewTextEditor::makeNewTextEditor);

		this.properties.put(setCurrentlyEdited, (target, to, prop) -> {
			RemoteEditor ed = target.find(RemoteEditor.editor, upwards()).findFirst().get();
			ed.changeSelection(to, new Dict.Prop<String>(prop));
			return true;
		});
	}

	private void pin(Box target) {
		RemoteEditor ed = target.find(RemoteEditor.editor, upwards()).findFirst().get();
		ed.pin();
		target.breadthFirst(upwards()).filter(x -> x instanceof TextEditor).forEach(x -> ((TextEditor) x).pin());
	}

	private void unpin(Box target) {
		RemoteEditor ed = target.find(RemoteEditor.editor, upwards()).findFirst().get();
		ed.unpin();
		target.breadthFirst(upwards()).filter(x -> x instanceof TextEditor).forEach(x -> ((TextEditor) x).unpin());
	}

	static public Box makeNewTextEditor(Box target) {
		ServerSupport q = new ServerSupport(target);
//		Server oldServer = target.find(ServerSupport.server, target.upwards()).findFirst().get();

		TextEditor te = new TextEditor(target);
		te.connect(target);
		te.loaded();

		RunLoop.main.when(q.getRemoteEditor(), e -> {
			RunLoop.main.delay(() -> {
				e.pin();
				te.pin();
				Drawing.dirty(te);
			},1000);
		});

		return te.browser_;
	}


	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}
