package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.app.ThreadSync2;
import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.io.IO;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Feedback on boxes that are still running due to ThreadS
 */
public class ThreadSync2Feedback extends Box {

	static public Dict.Prop<FunctionOfBox<Boolean>> fPause = new Dict.Prop<>("fPause").toCanon().type().doc(" pauses all the fibers associated with a running box. Returns false if that box currently has no fibers. ");
	static public Dict.Prop<FunctionOfBox<Boolean>> fCont = new Dict.Prop<>("fCont").toCanon().type().doc(" continues all the fibers associated with a running box. Returns false if that box currently has no fibers. ");
	static public Dict.Prop<FunctionOfBox<Boolean>> fStep = new Dict.Prop<>("fStep").toCanon().type().doc(" steps once all the fibers associated with a running box. Returns false if that box currently has no fibers. ");
	static public Dict.Prop<FunctionOfBox<Boolean>> fKill = new Dict.Prop<>("fKill").toCanon().type().doc(" kills all the fibers associated with a running box. Returns false if that box currently has no fibers. ");

	static public Dict.Prop<FunctionOfBox<Boolean>> yield = new Dict.Prop<>("yield").toCanon().type().doc("call `_.yield()` to pause this code for one 'frame', allowing everything else (graphics, the rest of field) to update exactly once.");

	static public Dict.Prop<FunctionOfBox<Boolean>> finish = new Dict.Prop<>("finish").toCanon().type().doc("call `_.finish()` to stop this code from running anymore (without error)");

	static public Dict.Prop<FunctionOfBox<ThreadSync2.Fibre>> trace = new Dict.Prop<>("trace").toCanon().type().doc("call `_.trace()` to get an object that lives for this lifetime of this execution trace. Simultanous overlapping executions share the same namespace but different `_.trace()` objects");

	static public Dict.Prop<Boolean> mainThread = new Dict.Prop<>("mainThread").toCanon().type().doc("set `_.mainThread==true` to execute everything inside this box in the main thread").set(IO.persistent, true);

	Map<Box, Integer> lastRunning = new LinkedHashMap<>();

	public ThreadSync2Feedback(Box root) {

		if (ThreadSync2.getEnabled()) {
			this.properties.putToMap(Boxes.insideRunLoop, "main.threadsync2feedback", this::run);
			this.properties.put(MarkingMenus.menuSpecs, (event) -> {
				Stream<Box> s = selection();

				List<Box> c = s.filter(x -> lastRunning.containsKey(x))
					.collect(Collectors.toList());

				if (c.size() == 0) return null;

				return menuSpecification(c);

			});
		}

		this.properties.put(fPause, ThreadSync2Feedback::pause);
		this.properties.put(fCont, ThreadSync2Feedback::pause);
		this.properties.put(fStep, ThreadSync2Feedback::pause);
		this.properties.put(fKill, ThreadSync2Feedback::kill);
		this.properties.put(yield, ThreadSync2Feedback::yield);
		this.properties.put(trace, ThreadSync2Feedback::fibre);
		this.properties.put(finish, ThreadSync2Feedback::finish);
	}

	static public boolean isPaused(Box x) {
//		return false;

		return ThreadSync2.getSync()
			.getFibres()
			.stream()
			.filter(z -> z.tag == x)
			.filter(z -> z.paused)
			.findAny()
			.isPresent();
	}

	static public boolean yield(Box x) {
		if (ThreadSync2.getEnabled()) {
			ThreadSync2.Fibre f = ThreadSync2.fibre();
			if (f != null) {
				f.yield();
				return true;
			}
		} else throw new IllegalArgumentException("can't wait for a frame without running -threaded2 1");
		return false;
	}

	static public boolean yield() {
		if (ThreadSync2.getEnabled()) {
			ThreadSync2.Fibre f = ThreadSync2.fibre();
			if (f != null) {
				f.yield();
				return true;
			}
		} else throw new IllegalArgumentException("can't wait for a frame without running -threaded2 1");
		return false;
	}


	static public boolean finish(Box x) {
		if (ThreadSync2.getEnabled()) {
			ThreadSync2.Fibre f = ThreadSync2.fibre();
			if (f != null) {
				throw new ThreadSync2.KilledException();
			}
		}
		return false;
	}


	static public boolean maybeYield() {
		if (ThreadSync2.getEnabled()) {
			ThreadSync2.Fibre f = ThreadSync2.fibre();
			long tick = f.d.computeIfAbsent(ThreadSync2.get__maybeYieldAtFrame(), (k) -> RunLoop.tick);
			if (tick == RunLoop.tick) {
				yield();
				f.d.put(ThreadSync2.get__maybeYieldAtFrame(), RunLoop.tick);
				return true;
			}
			return false;
		} else throw new IllegalArgumentException("can't wait for a frame without running -threaded2 1");
	}

	static public ThreadSync2.Fibre fibre(Box x) {
		if (ThreadSync2.getEnabled()) {
			return ThreadSync2.fibre();
		}
		return null;
	}

	private MarkingMenus.MenuSpecification menuSpecification(List<Box> target) {

		MarkingMenus.MenuSpecification m = new MarkingMenus.MenuSpecification();
		MarkingMenus.MenuItem item = new MarkingMenus.MenuItem("Threads...", null);
		MarkingMenus.MenuSpecification menu = new MarkingMenus.MenuSpecification();
		item.setSubmenu(menu);
		m.items.put(MarkingMenus.Position.SE2, item);

		menu.items.put(MarkingMenus.Position.W, new MarkingMenus.MenuItem("Pause", () -> target.forEach(ThreadSync2Feedback::pause)));
		menu.items.put(MarkingMenus.Position.W2, new MarkingMenus.MenuItem("Kill", () -> target.forEach(ThreadSync2Feedback::kill)));
		menu.items.put(MarkingMenus.Position.E, new MarkingMenus.MenuItem("Step", () -> target.forEach(ThreadSync2Feedback::step)));
		menu.items.put(MarkingMenus.Position.E2, new MarkingMenus.MenuItem("Continue", () -> target.forEach(ThreadSync2Feedback::cont)));

		return m;
	}

	static protected boolean cont(Box box) {

		List<ThreadSync2.Fibre> live = ThreadSync2.getSync()
			.getFibres();
		boolean[] done = {false};

		live.stream()
			.filter(x -> x.tag == box)
			.forEach(x -> {
				x.paused = false;
				done[0] = true;
			});

		Drawing.dirty(box, 3);
		return done[0];
	}

	static protected boolean pause(Box box) {

		List<ThreadSync2.Fibre> live = ThreadSync2.getSync()
			.getFibres();
		boolean[] done = {false};
		live.stream()
			.filter(x -> x.tag == box)
			.forEach(x -> {
				x.paused = true;
				done[0] = true;
			});
		Drawing.dirty(box, 3);
		return done[0];
	}

	static protected boolean step(Box box) {

		List<ThreadSync2.Fibre> live = ThreadSync2.getSync()
			.getFibres();
		boolean[] done = {false};
		live.stream()
			.filter(x -> x.tag == box)
			.forEach(x -> {
				x.paused = false;
				x.pauseNext = true;
				done[0] = true;
			});
		Drawing.dirty(box, 3);
		return done[0];
	}

	public static boolean kill(Box box) {

		List<ThreadSync2.Fibre> live = ThreadSync2.getSync()
			.getFibres();
		boolean[] done = {false};
		live.stream()
			.filter(x -> x.tag == box)
			.forEach(x -> {
				x.killed = true;
				x.paused = false;
				x.pauseNext = false;
				done[0] = true;
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
		List<ThreadSync2.Fibre> live = ThreadSync2.getSync()
			.getFibres();

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

	public static List<ThreadSync2.Fibre> fibresFor(Box box) {
		if (!ThreadSync2.getEnabled()) return Collections.emptyList();
		List<ThreadSync2.Fibre> live = ThreadSync2.getSync()
			.getFibres();

		return live.stream()
			.filter(x -> x.tag == box).collect(Collectors.toList());
	}
}
