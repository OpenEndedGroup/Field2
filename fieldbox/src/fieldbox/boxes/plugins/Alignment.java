package fieldbox.boxes.plugins;

import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.MarkingMenus;
import fieldbox.boxes.Mouse;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Offers a marking menuSpecs for aligning boxes
 */
public class Alignment extends Box {

	public Alignment(Box root) {

		properties.put(MarkingMenus.menuSpecs, (event) -> {
			MarkingMenus.MenuSpecification m = new MarkingMenus.MenuSpecification();
			m.items.put(MarkingMenus.Position.W, new MarkingMenus.MenuItem("Left", this::left));
			m.items.put(MarkingMenus.Position.E2, new MarkingMenus.MenuItem("Abut", this::abut));
			m.items.put(MarkingMenus.Position.E, new MarkingMenus.MenuItem("Right", this::right));
			m.items.put(MarkingMenus.Position.N, new MarkingMenus.MenuItem("Top", this::top));
			m.items.put(MarkingMenus.Position.S, new MarkingMenus.MenuItem("Bottom", this::bottom));
			m.items.put(MarkingMenus.Position.S2, new MarkingMenus.MenuItem("Abut", this::abutV));

			if (selected().count() > 1) {
				MarkingMenus.MenuSpecification spec = new MarkingMenus.MenuSpecification();

				spec.items.put(MarkingMenus.Position.W, new MarkingMenus.MenuItem("Alignment", () -> {
				}).setSubmenu(m));

				return spec;
			}
			return null;
		});
	}

	private void abut() {
		List<Box> s = selected().sorted((a, b) -> Float.compare(a.properties.get(Box.frame).x, b.properties.get(Box.frame).x))
			    .collect(Collectors.toList());
		float ax = s.get(0).properties.get(Box.frame).x+s.get(0).properties.get(Box.frame).w;
		for(int i=1;i<s.size();i++)
		{
			s.get(i).properties.get(Box.frame).x = ax;
			ax = s.get(i).properties.get(Box.frame).x+s.get(i).properties.get(Box.frame).w;
		}
	}
	private void abutV() {
		List<Box> s = selected().sorted((a, b) -> Float.compare(a.properties.get(Box.frame).y, b.properties.get(Box.frame).y))
			    .collect(Collectors.toList());
		float ax = s.get(0).properties.get(Box.frame).y+s.get(0).properties.get(Box.frame).h;
		for(int i=1;i<s.size();i++)
		{
			s.get(i).properties.get(Box.frame).y = ax;
			ax = s.get(i).properties.get(Box.frame).y+s.get(i).properties.get(Box.frame).h;
		}
	}

	private void left() {
		selected().map(a -> a.properties.get(Box.frame).x).min(Float::compare).ifPresent(x -> {
			selected().forEach(bx -> {
				bx.properties.get(Box.frame).x = x.floatValue();
			});
			Drawing.dirty(this);
		});
	}

	private void right() {
		selected().map(a -> a.properties.get(Box.frame).x + a.properties.get(Box.frame).w).max(Float::compare).ifPresent(x -> {
			selected().forEach(bx -> {
				bx.properties.get(Box.frame).x = x.floatValue() - bx.properties.get(Box.frame).w;
			});
			Drawing.dirty(this);
		});
	}

	private void top() {
		selected().map(a -> a.properties.get(Box.frame).y).min(Float::compare).ifPresent(x -> {
			selected().forEach(bx -> {
				bx.properties.get(Box.frame).y = x.floatValue();
			});
			Drawing.dirty(this);
		});
	}

	private void bottom() {
		selected().map(a -> a.properties.get(Box.frame).y + a.properties.get(Box.frame).h).max(Float::compare).ifPresent(x -> {
			selected().forEach(bx -> {
				bx.properties.get(Box.frame).y = x.floatValue() - bx.properties.get(Box.frame).h;
			});
			Drawing.dirty(this);
		});
	}

	public Stream<Box> selected() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}
}
