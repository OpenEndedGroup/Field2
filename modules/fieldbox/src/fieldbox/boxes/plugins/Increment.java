package fieldbox.boxes.plugins;

import field.utility.Quad;
import fieldbox.boxes.Box;
import fielded.Commands;
import fielded.EditorUtils;
import fielded.Numbers;
import fielded.RemoteEditor;

public class Increment extends Box {

	public Increment(Box root_unused) {
		this.properties.putToMap(Commands.command, "Increment Number", (x) -> {
			EditorUtils ed = this.first(RemoteEditor.editorUtils, both()).orElseThrow(() -> new IllegalStateException(" no editortools ? "));
			String ct = ed.getCurrentText();
			int pos = ed.getCursorPosition();

			Quad<Integer, Integer, String, Double> t = Numbers.extractNumberAt(ct, pos);
			if (t != null) {
				String n = Numbers.increment(t.third.trim(), t.fourth.intValue());
				ed.replaceRange(t.first, t.second, n);
				ed.executeCurrentBracket();
				ed.setCursorPosition(pos + n.length() - (t.second - t.first));
			}
			return null;
		});
		this.properties.putToMap(Commands.commandDoc, "Increment Number",
					 "Increments a number that the cursor is over in the editor. The position of the cursor with respect to the decimal point determines how large the increment is. The current block at the cursor is then executed.");
		this.properties.putToMap(Commands.command, "Decrement Number", (x) -> {
			EditorUtils ed = this.first(RemoteEditor.editorUtils, both()).orElseThrow(() -> new IllegalStateException(" no editortools ? "));
			String ct = ed.getCurrentText();
			int pos = ed.getCursorPosition();

			Quad<Integer, Integer, String, Double> t = Numbers.extractNumberAt(ct, pos);
			if (t != null) {
				String n = Numbers.decrement(t.third.trim(), t.fourth.intValue());
				ed.replaceRange(t.first, t.second, n);
				ed.executeCurrentBracket();
				ed.setCursorPosition(pos + n.length() - (t.second - t.first));
			}
			return null;
		});
		this.properties.putToMap(Commands.commandDoc, "Decrement Number",
					 "Decrements  a number that the cursor is over in the editor. The position of the cursor with respect to the decimal point determines how large the increment is. The current block at the cursor is then executed.");
	}
}
