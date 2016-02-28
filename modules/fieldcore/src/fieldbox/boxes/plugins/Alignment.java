package fieldbox.boxes.plugins;

import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.boxes.annotations.PublishAsCommand;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Offers a marking menuSpecs for aligning and selecting boxes
 */
public class Alignment extends Box {

	protected final Box root;

	public Alignment(Box root) {

		this.root = root;
		properties.put(MarkingMenus.menuSpecs, (event) -> {
			MarkingMenus.MenuSpecification m = new MarkingMenus.MenuSpecification();
			m.items.put(MarkingMenus.Position.W, new MarkingMenus.MenuItem("Left", this::left));
			m.items.put(MarkingMenus.Position.E2, new MarkingMenus.MenuItem("Abut", this::abut));
			m.items.put(MarkingMenus.Position.E, new MarkingMenus.MenuItem("Right", this::right));
			m.items.put(MarkingMenus.Position.N, new MarkingMenus.MenuItem("Top", this::top));
			m.items.put(MarkingMenus.Position.S, new MarkingMenus.MenuItem("Bottom", this::bottom));
			m.items.put(MarkingMenus.Position.S2, new MarkingMenus.MenuItem("Abut", this::abutV));

			MarkingMenus.MenuSpecification m2 = new MarkingMenus.MenuSpecification();
			m2.items.put(MarkingMenus.Position.W, new MarkingMenus.MenuItem("Left", this::selLeft));
			m2.items.put(MarkingMenus.Position.W2, new MarkingMenus.MenuItem("All Left", this::selAllLeft));
			m2.items.put(MarkingMenus.Position.E, new MarkingMenus.MenuItem("Right", this::selRight));
			m2.items.put(MarkingMenus.Position.E2, new MarkingMenus.MenuItem("All Right", this::selAllRight));
			m2.items.put(MarkingMenus.Position.S, new MarkingMenus.MenuItem("Below", this::selBelow));
			m2.items.put(MarkingMenus.Position.S2, new MarkingMenus.MenuItem("All Below", this::selAllBelow));
			m2.items.put(MarkingMenus.Position.N, new MarkingMenus.MenuItem("Above", this::selAbove));
			m2.items.put(MarkingMenus.Position.N2, new MarkingMenus.MenuItem("All Above", this::selAllAbove));

			MarkingMenus.MenuSpecification spec = new MarkingMenus.MenuSpecification();

			if (selected().count() > 0) {
				spec.items.put(MarkingMenus.Position.N, new MarkingMenus.MenuItem("Selection", () -> {
				}).setSubmenu(m2));
			}

			if (selected().count() > 1) {

				spec.items.put(MarkingMenus.Position.W, new MarkingMenus.MenuItem("Alignment", () -> {
				}).setSubmenu(m));

			}

			if (spec.items.size() > 0) return spec;

			return null;
		});
	}

	@PublishAsCommand(name="select")
	protected void selRight() {
		Rect[] r = {null};
		selected().forEach(x -> r[0] = Rect.union(r[0], x.properties.get(Box.frame)));
		root.breadthFirst(root.allDownwardsFrom())
		    .filter(x -> x.properties.has(Box.frame))
		    .map(x -> {
			    System.out.println(" checking :" + x.properties.get(Box.frame) + " against " + r[0]);
			    return x;
		    })
		    .filter(x -> x.properties.get(Box.frame).y < r[0].y + r[0].h && x.properties.get(Box.frame).y + x.properties.get(Box.frame).h > r[0].y && x.properties.get(Box.frame).x > r[0].x)
		    .forEach(x -> Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect));
	}

	protected void selAllRight() {
		Rect[] r = {null};
		selected().forEach(x -> r[0] = Rect.union(r[0], x.properties.get(Box.frame)));
		root.breadthFirst(root.allDownwardsFrom())
		    .filter(x -> x.properties.has(Box.frame))
		    .filter(x -> x.properties.get(Box.frame).x > r[0].x)
		    .forEach(x -> Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect));
	}

	protected void selLeft() {
		Rect[] r = {null};
		selected().forEach(x -> r[0] = Rect.union(r[0], x.properties.get(Box.frame)));
		root.breadthFirst(root.allDownwardsFrom())
		    .filter(x -> x.properties.has(Box.frame))
		    .filter(x -> x.properties.get(Box.frame).y < r[0].y + r[0].h && x.properties.get(Box.frame).y + x.properties.get(Box.frame).h > r[0].y && x.properties.get(
				Box.frame).x < r[0].x + r[0].w)
		    .forEach(x -> Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect));
	}

	protected void selAllLeft() {
		Rect[] r = {null};
		selected().forEach(x -> r[0] = Rect.union(r[0], x.properties.get(Box.frame)));
		root.breadthFirst(root.allDownwardsFrom())
		    .filter(x -> x.properties.has(Box.frame))
		    .filter(x -> x.properties.get(Box.frame).x < r[0].x + r[0].w)
		    .forEach(x -> Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect));
	}

	protected void selAbove() {
		Rect[] r = {null};
		selected().forEach(x -> r[0] = Rect.union(r[0], x.properties.get(Box.frame)));
		root.breadthFirst(root.allDownwardsFrom())
		    .filter(x -> x.properties.has(Box.frame))
		    .filter(x -> x.properties.get(Box.frame).x < r[0].x + r[0].w && x.properties.get(Box.frame).x + x.properties.get(Box.frame).w > r[0].x && x.properties.get(
				Box.frame).y < r[0].y + r[0].h)
		    .forEach(x -> Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect));
	}

	protected void selAllAbove() {
		Rect[] r = {null};
		selected().forEach(x -> r[0] = Rect.union(r[0], x.properties.get(Box.frame)));
		root.breadthFirst(root.allDownwardsFrom())
		    .filter(x -> x.properties.has(Box.frame))
		    .filter(x -> x.properties.get(Box.frame).y < r[0].y + r[0].h)
		    .forEach(x -> Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect));
	}

	protected void selBelow() {
		Rect[] r = {null};
		selected().forEach(x -> r[0] = Rect.union(r[0], x.properties.get(Box.frame)));
		root.breadthFirst(root.allDownwardsFrom())
		    .filter(x -> x.properties.has(Box.frame))
		    .filter(x -> x.properties.get(Box.frame).x < r[0].x + r[0].w && x.properties.get(Box.frame).x + x.properties.get(Box.frame).w > r[0].x && x.properties.get(
				Box.frame).y + x.properties.get(Box.frame).h > r[0].y)
		    .forEach(x -> Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect));
	}

	protected void selAllBelow() {
		Rect[] r = {null};
		selected().forEach(x -> r[0] = Rect.union(r[0], x.properties.get(Box.frame)));
		root.breadthFirst(root.allDownwardsFrom())
		    .filter(x -> x.properties.has(Box.frame))
		    .filter(x -> x.properties.get(Box.frame).y + x.properties.get(Box.frame).h > r[0].y)
		    .forEach(x -> Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect));
	}

	protected void abut() {
		List<Box> s = selected().sorted((a, b) -> Float.compare(a.properties.get(Box.frame).x, b.properties.get(Box.frame).x))
					.collect(Collectors.toList());
		float ax = s.get(0).properties.get(Box.frame).x + s.get(0).properties.get(Box.frame).w;
		for (int i = 1; i < s.size(); i++) {
			final float finalAx = ax;
			set(s.get(i), z -> {
				z.x = finalAx;
			});
			ax = s.get(i).properties.get(Box.frame).x + s.get(i).properties.get(Box.frame).w;
		}
	}

	private void set(Box b, Consumer<Rect> r) {
		Callbacks.frameModified(b, r);
	}

	private void abutV() {
		List<Box> s = selected().sorted((a, b) -> Float.compare(a.properties.get(Box.frame).y, b.properties.get(Box.frame).y))
					.collect(Collectors.toList());
		float ax = s.get(0).properties.get(Box.frame).y + s.get(0).properties.get(Box.frame).h;
		for (int i = 1; i < s.size(); i++) {
			final float finalAx = ax;
			set(s.get(i), z -> {
				z.y = finalAx;
			});

			ax = s.get(i).properties.get(Box.frame).y + s.get(i).properties.get(Box.frame).h;
		}
	}

	protected void left() {
		selected().map(a -> a.properties.get(Box.frame).x)
			  .min(Float::compare)
			  .ifPresent(x -> {
				  selected().forEach(bx -> {
					  set(bx, z -> z.x = x.floatValue());
					  bx.properties.get(Box.frame).x = x.floatValue();
				  });
				  Drawing.dirty(this);
			  });
	}

	private void right() {
		selected().map(a -> a.properties.get(Box.frame).x + a.properties.get(Box.frame).w)
			  .max(Float::compare)
			  .ifPresent(x -> {
				  selected().forEach(bx -> {
					  set(bx, z -> z.x = x.floatValue() - bx.properties.get(Box.frame).w);
					  bx.properties.get(Box.frame).x = x.floatValue() - bx.properties.get(Box.frame).w;
				  });
				  Drawing.dirty(this);
			  });
	}

	protected void top() {
		selected().map(a -> a.properties.get(Box.frame).y)
			  .min(Float::compare)
			  .ifPresent(x -> {
				  selected().forEach(bx -> {
					  set(bx, z->z.y = x.floatValue());
					  bx.properties.get(Box.frame).y = x.floatValue();
				  });
				  Drawing.dirty(this);
			  });
	}

	private void bottom() {
		selected().map(a -> a.properties.get(Box.frame).y + a.properties.get(Box.frame).h)
			  .max(Float::compare)
			  .ifPresent(x -> {
				  selected().forEach(bx -> {
					  set(bx, z -> z.y = x.floatValue() - bx.properties.get(Box.frame).h);
					  bx.properties.get(Box.frame).y = x.floatValue() - bx.properties.get(Box.frame).h;
				  });
				  Drawing.dirty(this);
			  });
	}

	public Stream<Box> selected() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}
}
