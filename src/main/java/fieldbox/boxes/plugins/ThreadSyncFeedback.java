package fieldbox.boxes.plugins;

import field.app.ThreadSync;
import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.boxes.*;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Feedback on boxes that are still running due to ThreadS
 */
public class ThreadSyncFeedback extends Box {

	static public Dict.Prop<FunctionOfBox<Boolean>> fPause = new Dict.Prop<>("fPause").toCanon().doc(" pauses all the fibers associated with a running box. Returns false if that box currently has no fibers. ");
	static public Dict.Prop<FunctionOfBox<Boolean>> fCont = new Dict.Prop<>("fCont").toCanon().doc(" continues all the fibers associated with a running box. Returns false if that box currently has no fibers. ");
	static public Dict.Prop<FunctionOfBox<Boolean>> fStep = new Dict.Prop<>("fStep").toCanon().doc(" steps once all the fibers associated with a running box. Returns false if that box currently has no fibers. ");
	static public Dict.Prop<FunctionOfBox<Boolean>> fKill = new Dict.Prop<>("fKill").toCanon().doc(" kills all the fibers associated with a running box. Returns false if that box currently has no fibers. ");

	Map<Box, Integer> lastRunning = new LinkedHashMap<>();

	public ThreadSyncFeedback(Box root) {
		if (ThreadSync.enabled) {
			this.properties.putToMap(Boxes.insideRunLoop, "main.threadsyncfeedback", this::run);
			this.properties.put(MarkingMenus.menuSpecs, (event) -> {
				Stream<Box> s = selection();

				List<Box> c = s.filter(x -> lastRunning.containsKey(x))
					.collect(Collectors.toList());

				if (c.size() == 0) return null;

				return menuSpecification(c);

			});
		}

		this.properties.put(fPause, ThreadSyncFeedback::pause);
		this.properties.put(fCont, ThreadSyncFeedback::pause);
		this.properties.put(fStep, ThreadSyncFeedback::pause);
		this.properties.put(fKill, ThreadSyncFeedback::kill);
	}

	static public boolean isPaused(Box x) {
//		return false;

		return ThreadSync.get()
			.getFibers()
			.stream()
			.filter(z -> z.tag == x)
			.filter(z -> z.wasPaused)
			.findAny()
			.isPresent();
	}

	private MarkingMenus.MenuSpecification menuSpecification(List<Box> target) {

		MarkingMenus.MenuSpecification m = new MarkingMenus.MenuSpecification();
		MarkingMenus.MenuItem item = new MarkingMenus.MenuItem("Threads...", null);
		MarkingMenus.MenuSpecification menu = new MarkingMenus.MenuSpecification();
		item.setSubmenu(menu);
		m.items.put(MarkingMenus.Position.SE2, item);

		menu.items.put(MarkingMenus.Position.W, new MarkingMenus.MenuItem("Pause", () -> target.forEach(ThreadSyncFeedback::pause)));
		menu.items.put(MarkingMenus.Position.W2, new MarkingMenus.MenuItem("Kill", () -> target.forEach(ThreadSyncFeedback::kill)));
		menu.items.put(MarkingMenus.Position.E, new MarkingMenus.MenuItem("Step", () -> target.forEach(ThreadSyncFeedback::step)));
		menu.items.put(MarkingMenus.Position.E2, new MarkingMenus.MenuItem("Continue", () -> target.forEach(ThreadSyncFeedback::cont)));

		return m;
	}

	static protected boolean cont(Box box) {

		List<ThreadSync.Fiber> live = ThreadSync.get()
			.getFibers();
		boolean[] done = {false};

		live.stream()
			.filter(x -> x.tag == box)
			.forEach(x -> {
				x.paused = () -> false;
				done[0] = true;
			});

		Drawing.dirty(box, 3);
		return done[0];
	}

	static protected boolean pause(Box box) {

		List<ThreadSync.Fiber> live = ThreadSync.get()
			.getFibers();
		boolean[] done = {false};
		live.stream()
			.filter(x -> x.tag == box)
			.forEach(x -> {
				x.paused = () -> true;
				done[0] = true;
			});
		Drawing.dirty(box, 3);
		return done[0];
	}

	static protected boolean step(Box box) {

		List<ThreadSync.Fiber> live = ThreadSync.get()
			.getFibers();
		boolean[] done = {false};
		live.stream()
			.filter(x -> x.tag == box)
			.forEach(x -> {
				x.paused = () -> {
					x.paused = () -> true;
					return false;
				};
				done[0] = true;
			});
		Drawing.dirty(box, 3);
		return done[0];
	}

	static protected boolean kill(Box box) {

		List<ThreadSync.Fiber> live = ThreadSync.get()
			.getFibers();
		boolean[] done = {false};
		live.stream()
			.filter(x -> x.tag == box)
			.forEach(x -> {
				x.stopped = true;
				done[0] = true;
				x.paused = () -> false;
			});
		Drawing.dirty(box, 3);
		return done[0];
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false))
			.filter(x -> !x.properties.isTrue(Mouse.isSticky, false))
			.filter(x -> x.properties.has(Box.name));
	}

	protected boolean run() {
		List<ThreadSync.Fiber> live = ThreadSync.get()
			.getFibers();

		Map<Box, Integer> running = new LinkedHashMap<>();

		live.forEach(x -> {
			if (x.tag instanceof Box)
				running.put((Box) x.tag, running.computeIfAbsent((Box) x.tag, (k) -> 0) + 1);
		});

		Set<Box> started = new LinkedHashSet<>(running.keySet());
		started.removeAll(lastRunning.keySet());

		Set<Box> finished = new LinkedHashSet<>(lastRunning.keySet());
		finished.removeAll(running.keySet());

		started.stream()
			.forEach(x -> addBadge(x));
		finished.stream()
			.forEach(x -> removeBadge(x));

		if (!running.equals(lastRunning)) Drawing.dirty(this);

		lastRunning = running;

		return true;
	}

	private void removeBadge(Box x) {

		x.properties.removeFromMap(FLineDrawing.frameDrawing, "__threadsyncfeedback__");
		x.properties.removeFromMap(FLineDrawing.frameDrawing, "__threadsyncfeedback2__");

	}

	private void addBadge(Box x) {

		x.properties.putToMap(FLineDrawing.frameDrawing, "__threadsyncfeedback__", (q) -> {

			try {
				Integer count = lastRunning.get(x);

				Rect f = x.properties.get(Box.frame);

				FLine boxes = new FLine();

				int r = 15;
				int space = 5;
				if (!isPaused(x)) {
					for (int i = 0; i < count; i++) {
						boxes.circle(f.x + f.w + r + space - 3, f.y + 5 + r / 2 + i * (r + space), r / 2);
					}
					boxes.attributes.put(StandardFLineDrawing.strokeColor, new Vec4(Colors.executionColor, 0.7));
				} else {
					for (int i = 0; i < count; i++) {
						boxes.rect(-r / 2 + f.x + f.w + r + space - 3, -r / 2 + f.y + 5 + r / 2 + i * (r + space), r, r);
					}
					boxes.attributes.put(StandardFLineDrawing.strokeColor, new Vec4(Colors.executionColor, 0.35));
				}

				boxes.attributes.put(StandardFLineDrawing.stroked, true);

				boxes.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(1.5f));

				return boxes;
			} catch (Throwable t) {
				t.printStackTrace();
			}
			return null;
		});
		x.properties.putToMap(FLineDrawing.frameDrawing, "__threadsyncfeedback2__", (q) -> {
			try {

				Integer count = lastRunning.get(x);

				Rect f = x.properties.get(Box.frame);

				FLine boxes = new FLine();

				int r = 15;
				int space = 5;
				if (!isPaused(x)) {
					for (int i = 0; i < count; i++) {
						boxes.circle(f.x + f.w + r + space - 3, f.y + 5 + r / 2 + i * (r + space), r / 2);
					}
					boxes.attributes.put(StandardFLineDrawing.color, new Vec4(Colors.executionColor, -0.5));
				} else {
					for (int i = 0; i < count; i++) {
						boxes.rect(-r / 2 + f.x + f.w + r + space - 3, -r / 2 + f.y + 5 + r / 2 + i * (r + space), r, r);
					}
					boxes.attributes.put(StandardFLineDrawing.color, new Vec4(Colors.executionColor, -0.25));
				}

				boxes.attributes.put(StandardFLineDrawing.filled, true);
				boxes.attributes.put(StandardFLineDrawing.stroked, false);


				return boxes;
			} catch (Throwable t) {
				t.printStackTrace();
			}
			return null;

		});
	}
}
