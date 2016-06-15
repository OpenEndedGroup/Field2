package fieldbox.boxes.plugins;

import static org.lwjgl.glfw.GLFW.*;

import field.graphics.Window;
import fieldbox.boxes.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adds: Press command/meta delete to delete selected boxes, swipe down to delete selection
 */
public class Delete extends Box {

	protected final Box root;

	public Delete(Box root) {

		this.properties.put(Planes.plane, "__root__ creation");

		this.root = root;
		this.properties.putToMap(Keyboard.onKeyDown, "__delete__", (event, key) -> {
			if (event.properties.isTrue(Window.consumed, false)) return null;
			if (event.after.isSuperDown() && key == GLFW_KEY_DELETE) {
				{
					Stream<Box> all = selected();
					all.forEach(bb -> Callbacks.delete(bb));
				}
				{
					Stream<Box> all = selected();
					all.forEach(bb -> bb.disconnectFromAll());
				}
				Drawing.dirty(Delete.this);
			}
			return null;
		});

		properties.put(MarkingMenus.menuSpecs, (event) -> {
			if (selected().findAny()
				.isPresent()) {
				MarkingMenus.MenuSpecification spec = new MarkingMenus.MenuSpecification();
				long count = selected().count();

				MarkingMenus.MenuSpecification really = new MarkingMenus.MenuSpecification();
				really.items.put(MarkingMenus.Position.N2, new MarkingMenus.MenuItem("Really, delete " + count + " box" + (count == 1 ? "" : "es") + "?", () -> {
					{
						Stream<Box> all = selected();
						all.forEach(bb -> Callbacks.delete(bb));
					}
					{
						Stream<Box> all = selected();
						all.forEach(bb -> bb.disconnectFromAll());
					}

					find(Watches.watches, both()).forEach(w -> w.getQueue()
						.accept("selection.changed", null));

					Drawing.dirty(Delete.this);
				}));

				spec.items.put(MarkingMenus.Position.S2, new MarkingMenus.MenuItem("Delete " + count + " box" + (count == 1 ? "" : "es"), () -> {
				}).setSubmenu(really));

				// disconnected for now
				{
					List cc = selected().flatMap(x -> x.breadthFirst(x.downwards())
						.filter(y -> x != y))
						.filter(x -> !x.properties.isTrue(Box.hidden, false))
						.filter(x -> !x.properties.isTrue(Box.decorative, false))
						.collect(Collectors.toList());

					long c = cc.size();
					if (c > 0) {
						spec.items.put(MarkingMenus.Position.NE2,
							new MarkingMenus.MenuItem("Hide " + count + " child" + (count == 1 ? "" : "ren") + " box" + (count == 1 ? "" : "es"), () -> {
								recursivelyHideFrom(selected());
							}));
					}
				}
				{
					List cc = selected().flatMap(x -> x.breadthFirstAll(x.allDownwardsFrom())
						.filter(y -> x != y))
						.filter(x -> x.disconnected)
						.filter(x -> !x.properties.isTrue(Box.decorative, false))
						.collect(Collectors.toList());

					long c = cc.size();
					if (c > 0) {
						spec.items.put(MarkingMenus.Position.SW2,
							new MarkingMenus.MenuItem("Show" + count + " hidden child" + (count == 1 ? "" : "ren") + " box" + (count == 1 ? "" : "es"), () -> {
								recursivelyShowFrom(selected());
							}));
					}
				}
				return spec;
			}
			return null;
		});

	}

	private void recursivelyHideFrom(Stream<Box> selected) {
		selected.flatMap(x -> x.breadthFirst(x.downwards())
			.filter(y -> y != x)).collect(Collectors.toList()).stream()
			.forEach(x -> x.disconnected = true);
		Drawing.dirty(this);
	}

	private void recursivelyShowFrom(Stream<Box> selected) {
		selected.flatMap(x -> x.breadthFirstAll(x.allDownwardsFrom())
			.filter(y -> y != x)).collect(Collectors.toList()).stream()
			.forEach(x -> x.disconnected = false);
		Drawing.dirty(this);
	}

	private Stream<Box> selected() {
		return root.breadthFirst(root.allDownwardsFrom())
			.filter(x -> x.properties.isTrue(Mouse.isSelected, false))
			.filter(x -> !x.properties.isTrue(Box.undeletable, false));
	}

}
