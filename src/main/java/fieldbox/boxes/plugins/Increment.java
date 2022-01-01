package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Quad;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.boxes.Mouse;
import fielded.Commands;
import fielded.EditorUtils;
import fielded.Numbers;
import fielded.RemoteEditor;
import fielded.plugins.Out;
import fieldnashorn.Nashorn;

import javax.script.ScriptContext;
import java.io.IOException;

public class Increment extends Box {

	private Box root;

	public Increment(Box root) {
		this.root = root;
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

		this.properties.putToMap(Commands.command, "Inspect At Cursor", (x) -> {
			EditorUtils ed = this.first(RemoteEditor.editorUtils, both()).orElseThrow(() -> new IllegalStateException(" no editortools ? "));
			String ct = ed.getCurrentText();
			int pos = ed.getCursorPosition();

			Quad<Integer, Integer, String, Double> t = Numbers.extractNameAt(ct, pos);
			if (t == null) return null;

			System.out.println(" identifier at cursor is '" + t + "'");

			Box selected = selection();
			if (selected == null) return null;

			ScriptContext bindings = selected.properties.get(Nashorn.boxBindings);
			Object o = bindings.getAttribute(t.third);

			Object o2 = selected.properties.get(new Dict.Prop(t.third));

			selected.first(Out.__out).ifPresent(out -> {
				out.getLineOut().accept(new Triple<>(selected, ed.lastLine + 1, false));
				try {
					if (o != null) out.getWriter().append("" + o);
					else if (o2 != null) out.getWriter().append("" + o2);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});


			return null;
		});
		this.properties.putToMap(Commands.commandDoc, "Inspect At Cursor",
			"prints the value of the thing that the cursor is over (if it can be found). This works for variables global to the box, and for properties in the box");

	}

	public Box selection() {
		return root.breadthFirst(root.both())
			.filter(x -> x.properties.isTrue(Mouse.isSelected, false) && !x.properties.isTrue(Mouse.isSticky, false))
			.findFirst()
			.orElseGet(() -> null);

	}
}
