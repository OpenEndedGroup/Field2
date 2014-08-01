package fieldbox.boxes;

import field.graphics.Window;
import field.utility.Dict;
import field.utility.Rect;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Entry-point from GLFW based Window.Event<Window.MouseState> into Field.
 * <p>
 * Boxes can add to properties "onMouseDown", "onMouseMove", "onMouseEnter" and "onMouseExit" to listen to these events. Optionally these can return
 * Draggers that will continue to be notified until corresponding mouse up events.
 */
public class Mouse {

	public interface Dragger {
		public boolean update(Window.Event<Window.MouseState> e, boolean termination);
	}

	public interface OnMouseDown {
		public Dragger onMouseDown(Window.Event<Window.MouseState> e, int button);
	}

	public interface OnMouseScroll {
		public void onMouseScroll(Window.Event<Window.MouseState> e);
	}

	public interface OnMouseMove {
		public Dragger onMouseMove(Window.Event<Window.MouseState> e);
	}

	public interface OnMouseEnter {
		public Dragger onMouseEnter(Window.Event<Window.MouseState> e);
	}

	public interface OnMouseExit {
		public void onMouseExit(Window.Event<Window.MouseState> e);
	}

	static public final Dict.Prop<Collection<OnMouseDown>> onMouseDown = new Dict.Prop<>("onMouseDown").type().toCannon();
	static public final Dict.Prop<Collection<OnMouseMove>> onMouseMove = new Dict.Prop<>("onMouseMove").type().toCannon();
	static public final Dict.Prop<Collection<OnMouseEnter>> onMouseEnter = new Dict.Prop<>("onMouseEnter").type().toCannon();
	static public final Dict.Prop<Collection<OnMouseScroll>> onMouseScroll = new Dict.Prop<>("onMouseScroll").type().toCannon();
	static public final Dict.Prop<Collection<OnMouseExit>> onMouseExit = new Dict.Prop<>("onMouseExit").type().toCannon();

	static public final Dict.Prop<Boolean> isSelected = new Dict.Prop<>("isSelected").type().toCannon();

	Map<Integer, Collection<Dragger>> ongoingDrags = new HashMap<Integer, Collection<Dragger>>();

	public void dispatch(Box root, Window.Event<Window.MouseState> event) {

		Set<Integer> pressed = Window.MouseState.buttonsPressed(event.before, event.after);
		Set<Integer> released = Window.MouseState.buttonsReleased(event.before, event.after);
		Set<Integer> down = event.after.buttonsDown;

		released.stream().map(r -> ongoingDrags.remove(r)).filter(x -> x != null).forEach(drags -> {
			drags.stream().filter(d -> d!=null).forEach(dragger -> dragger.update(event, true));
			drags.clear();
		});

		for (Collection<Dragger> d : ongoingDrags.values()) {
			Iterator<Dragger> dd = d.iterator();
			while (dd.hasNext()) {
				Dragger dragger = dd.next();
				boolean cont = false;
				try {
					cont = dragger.update(event, false);
				} catch (Throwable t) {
					System.err.println(" Exception thrown in dragger update :" + t);
					System.err.println(" dragger will not be called again");
					cont = false;
				}
				if (!cont) dd.remove();
			}
		}

		if (event.before.x != event.after.x || event.before.y != event.after.y) {
			Set<Dragger> draggers = root.find(onMouseMove, root.both()).flatMap(x -> x.stream()).map(x -> x.onMouseMove(event))
				    .filter(x -> x != null).collect(Collectors.toSet());
			ongoingDrags.computeIfAbsent(-1, (k) -> new LinkedHashSet<Dragger>()).addAll(draggers);
		}

		if (event.after.dwheely != 0.0 || event.after.dwheel != 0.0)
			root.find(onMouseScroll, root.both()).flatMap(x -> x.stream()).forEach(x -> x.onMouseScroll(event));


		pressed.stream().forEach(p -> {
			Collection<Dragger> dragger = ongoingDrags.computeIfAbsent(p, (x) -> new ArrayList<>());

			// change map to handle errors on x

			root.find(onMouseDown, root.both()).map(x -> {

				System.out.println(" on mouse down order :"+x);

				return x;
			}).flatMap(x -> x.stream()).map(x -> x.onMouseDown(event, p)).filter(x -> x != null)
				    .collect(Collectors.toCollection(() -> dragger));
		});

	}


}
