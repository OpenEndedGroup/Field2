package fieldbox;

import field.graphics.Window;
import field.linalg.Vec2;
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
					newBox(event);
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

	}

	private void newBox(Window.Event<Window.MouseState> event) {
		Vec2 at = convertCoordinateSystem(event.after);

		Box b1 = new Box();
		root.connect(b1);
		float w = 50;
		b1.properties.put(frame, new Rect(at.x-w, at.y-w, w*2, w*2));
		b1.properties.put(Box.name, "Untitled");
		Drawing.dirty(b1);

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
