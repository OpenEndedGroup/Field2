package fieldbox;

import com.badlogic.jglfw.Glfw;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
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
public class DefaultMenus extends Box {

	//TODO: consider being able to .toCommand("A command") Suppliers and FunctionOfBox's
	static public final Dict.Prop<FunctionOfBox<Box>> newBox = new Dict.Prop<FunctionOfBox<Box>>("newBox").toCannon().doc("create a new box that's a peer of this one");

	private final Box root;
	private final String filename;

	public DefaultMenus(Box root, String filename)
	{
		this.root = root;
		this.filename = filename;
		properties.put(MarkingMenus.menu, (event) -> {
			if (isNothingSelected()) {
				MarkingMenus.MenuSpecification spec = new MarkingMenus.MenuSpecification();
				spec.items.put(MarkingMenus.Position.E, new MarkingMenus.MenuItem("Save", () -> {
					save();
				}));
				spec.items.put(MarkingMenus.Position.N, new MarkingMenus.MenuItem("New Box", () -> {
					Vec2 at = convertCoordinateSystem(event.after);
					newBox(at, root);
				}));
				return spec;
			}
			return null;
		});

		properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			m.put(new Pair<>("Save all", "Saves this document"), DefaultMenus.this::save);

			return m;
		});

		properties.putToList(Keyboard.onKeyDown, (e, k) -> {
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

	private void save() {

		System.out.println(" saving .... ");
		Map<Box, String> special = new LinkedHashMap<>();
		special.put(root, ">>root<<");

		IO.Document doc = FieldBox.fieldBox.io.compileDocument(root, special);

		boolean error = false;
		try {
			FieldBox.fieldBox.io.writeOutDocument(FieldBox.fieldBox.io.WORKSPACE + "/"+filename, doc);
		} catch (IOException e) {
			e.printStackTrace();
			Drawing.notify("Error saving " + e.getMessage(), this, 200);
			error=true;
		}

		if (!error) {
			System.out.println(" going to notify ...");
			Drawing.notify("Saved to " + filename, this, 200);
			System.out.println(" ... notified ");
		}
	}

	private boolean isNothingSelected() {
		return !find(Mouse.isSelected, both()).filter(x -> x.booleanValue()).findFirst().isPresent();
	}

}
