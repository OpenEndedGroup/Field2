package fieldbox;

import com.badlogic.jglfw.Glfw;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fielded.RemoteEditor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Plugin: Adds Standard Menus and Shortcuts too basic not to have (new box, save etc.)
 */
public class HotkeyMenus extends Box {

	//TODO: consider being able to .toCommand("A command") Suppliers and FunctionOfBox's
	static public final Dict.Prop<FunctionOfBox<Box>> newBox = new Dict.Prop<FunctionOfBox<Box>>("newBox").toCannon().doc("create a new box that's a peer of this one");

	private final Box root;
	private final String filename;

	public HotkeyMenus(Box root, String filename)
	{
		this.root = root;
		this.filename = filename;

		properties.put(RemoteEditor.hotkeyCommands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			m.put(new Pair<>("Peter's Test Hotkey", "Edits the hotkey that peter made"), HotkeyMenus.this::doThis);

			return m;
		});

		properties.putToList(Keyboard.onKeyDown, (e, k) -> {
			if (e.properties.isTrue(Window.consumed, false)) return null;

			if ( k == Glfw.GLFW_KEY_N)
			{
				newBox(convertCoordinateSystem(e.after.mouseState), root);
			}
			return null;
		});

		properties.put(newBox, (box) -> {
			return newBox(box.find(Box.frame, box.both()).findFirst().map(x -> new Vec2(x.x+x.w+5, x.y+x.h+5)).orElseGet(() -> new Vec2(0,0)), box.parents().toArray(new Box[]{}));
		});

	}

	private Box newBox(Vec2 at, Box... parents) {

		Box b1 = new Box();
		for(Box p : parents)
			p.connect(b1);
		float w = 50;
		b1.properties.put(frame, new Rect(at.x-w, at.y-w, w*2, w*2));
		b1.properties.put(Box.name, "Untitled");
		Drawing.dirty(b1);
		return b1;
	}

	public Vec2 convertCoordinateSystem(Window.MouseState event) {
		Optional<Drawing> drawing = this.find(Drawing.drawing, both()).findFirst();
		return drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(event.x, event.y))).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));
	}

	private void doThis() {

		find(RemoteEditor.editor, both())
			    .forEach(editor -> editor.sendJavaScript("onKeyEvent: function(editor, event){\n" +
					"\tevent= $.event.fix(event);\n" +
					"\tif (event.type == \"keypress\" && event.keyCode == 13){\n" +
					"\t\tconsole.log(\"onkeyevent is doing what i think it is\\n\");\n" +
					"\n" +
					"\t}\n" +
					"}"));
	}
}
