package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec4;
import field.utility.Rect;
import fieldbox.boxes.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A plugin to use a part of the box graph modally
 */
public class SelectSubGraph extends Box {

	private final Box root;
	private Set<Box> proxyForDrawing = null;
	private List<FLine> cacheForDrawing = null;
	private List<Box> modalSet;
	private Consumer<Box> callback;
	private String explanatoryText;

	public SelectSubGraph(Box root) {
		this.root = root;
		this.properties.putToMap(FLineDrawing.bulkLines, "__drawproxy__", () -> {
			List<Supplier<FLine>> f = new ArrayList<>();
			int[] zz = {0};

			if (cacheForDrawing != null) {
				List<Supplier<FLine>> ff = new ArrayList<>(cacheForDrawing);

				Rect u = null;
				for (Box q : root.children()) {
					if (q.disconnected) continue;
					u = Rect.union(u, q.properties.get(Box.frame));
				}

				u = u.inset(-10);

				float large = 10000;
				float S = 0;
				FLine fl = new FLine().rect(-large, -large, large + u.x + u.w, large + u.y - S);
				fl.rect(-large, u.y, large + u.x - S, large);
				fl.rect(u.x + u.w + S, -large, large, large + u.y + u.h);
				fl.rect(u.x, u.y + u.h + S, large, large);
				fl.attributes.put(StandardFLineDrawing.filled, true);
				fl.attributes.put(StandardFLineDrawing.stroked, false);
				fl.attributes.put(StandardFLineDrawing.color, new Vec4(0.3, 0.3, 0.3, 0.3));
				ff.add(fl);

				FLine fl2 = new FLine().rect(u);
				fl2.attributes.put(StandardFLineDrawing.color, new Vec4(1, 1, 1, 0.2));
				fl2.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(3));
				ff.add(fl2);

				FLine t = new FLine().moveTo(u.x + u.w / 2, u.y - 4);
				t.attributes.put(StandardFLineDrawing.hasText, true);
				t.last().attributes.put(StandardFLineDrawing.textAlign, 0.5f);
				t.last().attributes.put(StandardFLineDrawing.text, explanatoryText);
				t.attributes.put(StandardFLineDrawing.color, new Vec4(1, 1, 1, 0.75));


				ff.add(t);
				return ff;
			}

			return Collections.emptyList();
		});

		this.properties.putToMap(Mouse.onDoubleClick, "__exitModal__", e -> {
			if (proxyForDrawing == null) return;
			Box x = e.properties.get(Mouse.originatesAt);
			if (modalSet.contains(x)) {
				FrameManipulation.setSelectionTo(root, Collections.emptySet());
				stopModal();
				if (callback != null) callback.accept(x);
			}
		});


	}

	private List<Box> selection(Box root) {
		return root.breadthFirst(root.both())
			.filter(x -> x.properties.isTrue(Mouse.isSelected, false) && x.properties.has(Box.frame) && x.properties.has(Box.name) && !x.properties.isTrue(Mouse.isSticky, false))
			.collect(Collectors.toList());
	}

	private <T> void remapColor(T f, Function<T, Supplier<Vec4>> get, BiConsumer<T, Vec4> set) {
		Supplier<Vec4> v = get.apply(f);
		if (v != null) {
			Vec4 v1 = v.get()
				.duplicate();
			v1.w /= 4;
			float ll = (float) ((v1.x + v1.y + v1.z) / 3);
			v1.x = ll;
			v1.y = ll;
			v1.z = ll;
			set.accept(f, v1);
		}
	}

	Runnable quitModal;

	public void goModal(List<Box> newChildren, String explanatoryText, Consumer<Box> callback) {
		List<Box> s = selection(root);
		FrameManipulation.setSelectionTo(root, Collections.emptySet());

		RunLoop.main.once(() -> {

			this.explanatoryText = explanatoryText;
			modalSet = newChildren;
			Variant.Memo m = Variant.freezeGraph(root);
			Set<Box> proxy = new LinkedHashSet<>();

			root.breadthFirstAll(root.allDownwardsFrom())
				.filter(x -> x != root)
				.forEach(x -> {
					if (!x.disconnected) proxy.add(x);
				});
			this.proxyForDrawing = proxy;
			cacheDrawingProxy();

			root.children().forEach(x -> x.disconnected = true);

			for (Box cc : newChildren) {
				cc.disconnected = false;
				cc.properties.put(Boxes.dontSave, true);
				root.connect(cc);
			}

			quitModal = () -> {
				for (Box cc : newChildren) {
					root.disconnect(cc);
					cc.disconnected = true;
				}
				proxyForDrawing = null;
				m.close();
				RunLoop.main.once(() -> {
					FrameManipulation.setSelectionTo(root, new LinkedHashSet<>(s));
				});
			};

			this.callback = callback;
		});
	}

	private void cacheDrawingProxy() {

		List<FLine> f = new ArrayList<>();

		if (proxyForDrawing != null) {
			for (Box b : proxyForDrawing) {
				Rect r = b.properties.get(Box.frame);
				if (r == null) continue;

				Map<String, Function<Box, FLine>> fr = b.properties.get(FLineDrawing.frameDrawing);
				if (fr == null) {
					FLine ff = new FLine().rect(r);
					f.add(ff);
				} else {
					for (Function<Box, FLine> q : fr.values()) {
						if (q != null) {
							FLine ff = q.apply(b);
							if (ff != null) {
								ff = ff.duplicate();
								remapColor(ff, x -> x.attributes.get(StandardFLineDrawing.color), (x, y) -> x.attributes.put(StandardFLineDrawing.color, y));
								remapColor(ff, x -> x.attributes.get(StandardFLineDrawing.strokeColor),
									(x, y) -> x.attributes.put(StandardFLineDrawing.strokeColor, y));
								remapColor(ff, x -> x.attributes.get(StandardFLineDrawing.fillColor),
									(x, y) -> x.attributes.put(StandardFLineDrawing.fillColor, y));
								for (FLine.Node n : ff.nodes) {
									remapColor(n, x -> x.attributes.get(StandardFLineDrawing.color),
										(x, y) -> x.attributes.put(StandardFLineDrawing.color, y));
									remapColor(n, x -> x.attributes.get(StandardFLineDrawing.strokeColor),
										(x, y) -> x.attributes.put(StandardFLineDrawing.strokeColor, y));
									remapColor(n, x -> x.attributes.get(StandardFLineDrawing.fillColor),
										(x, y) -> x.attributes.put(StandardFLineDrawing.fillColor, y));
								}
								if (ff.attributes.isTrue(StandardFLineDrawing.stroked, true)) {
									ff.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(1, 1, 1, 1, new float[]{12, 12}, 0));
								}
								f.add(ff);
							}
						}
					}
				}
			}
		}

		this.cacheForDrawing = f;
	}

	public void stopModal() {
		quitModal.run();
		quitModal = null;
		cacheForDrawing = null;
	}

}
