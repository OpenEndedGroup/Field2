package fieldbox.boxes;

import field.graphics.BaseMesh;
import field.graphics.Scene;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import fieldbox.ui.FieldBoxWindow;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Allows the direct attachment of BaseMesh objects to the box-graph, and direct attachment of anything
 */
public class Meshes extends Box implements Drawing.Drawer {
	static public final Dict.Prop<IdempotencyMap<Supplier<BaseMesh>>> meshes = new Dict.Prop<>("meshes").type()
		.toCannon()
		.doc("Geometry (specifically instances of BaseMesh) to be drawn along with this box")
		.autoConstructs(() -> new IdempotencyMap<>(Supplier.class));

	static public final Dict.Prop<Scene> scene = new Dict.Prop<>("scene").type()
		.toCannon()
		.doc("Scenegraph of the main layer of the main window");


	public Meshes(Box root) {
		this.properties.putToList(Drawing.drawers, this);
		Optional<FieldBoxWindow> w = root.find(Boxes.window, root.both())
			.findFirst();
		this.properties.put(scene, w.get().getCompositor().getLayer("__main__").getScene());
	}

	@Override
	public void draw(DrawingInterface context) {
		context.getShader().attach(new Scene.Transient(this::traverseAndDraw, 1).setOnceOnly());
	}

	// Exception handling!
	protected void traverseAndDraw() {
		this.forEach(x -> {
			if (!x.properties.has(meshes)) return;
			IdempotencyMap<Supplier<BaseMesh>> m = x.properties.get(meshes);
			if (m.size() == 0) return;

			m.values().stream().filter(z -> z != null).map(z -> z.get()).filter(z -> z != null).forEach(z -> {
				draw(z);
			});
		});
	}

	private void draw(BaseMesh x) {
		for (int i : x.getPasses()) {
			x.perform(i);
		}
	}
}
