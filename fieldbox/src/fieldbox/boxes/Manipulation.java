package fieldbox.boxes;

import field.utility.Dict;
import field.utility.Rect;

import field.graphics.Window;

import java.util.*;
import java.util.stream.Collectors;

import static field.utility.Curry.ignore;

/**
 * Created by marc on 3/18/14.
 */
public class Manipulation {

	public interface Dragger {
		public boolean update(Window.Event<Window.MouseState> e, boolean termination);
	}

	public interface OnMouseDown {
		public Dragger onMouseDown(Window.Event<Window.MouseState> e, int button);
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



	static public final Dict.Prop<Rect> frame = new Dict.Prop<>("frame").toCannon();

	static public final Dict.Prop<Collection<OnMouseDown>> onMouseDown = new Dict.Prop<>("onMouseDown").toCannon();
	static public final Dict.Prop<Collection<OnMouseMove>> onMouseMove = new Dict.Prop<>("onMouseMove").toCannon();
	static public final Dict.Prop<Collection<OnMouseEnter>> onMouseEnter = new Dict.Prop<>("onMouseEnter").toCannon();
	static public final Dict.Prop<Collection<OnMouseExit>> onMouseExit = new Dict.Prop<>("onMouseExit").toCannon();

	static public final Dict.Prop<Boolean> isSelected = new Dict.Prop<>("isSelected").toCannon();

	Map<Integer, Collection<Dragger>> ongoingDrags = new HashMap<Integer, Collection<Dragger>>();

	public void dispatch(Box root, Window.Event<Window.MouseState> event) {

		Set<Integer> pressed = Window.MouseState.buttonsPressed(event.before, event.after);
		Set<Integer> released = Window.MouseState.buttonsReleased(event.before, event.after);
		Set<Integer> down = event.after.buttonsDown;

		released.stream().map(r -> ongoingDrags.remove(r)).filter(x -> x != null).forEach(drags -> {
			drags.stream().forEach(dragger -> dragger.update(event, true));
			drags.clear();
		});

		for (Collection<Dragger> d : ongoingDrags.values())
		{
			Iterator<Dragger> dd = d.iterator();
			while(dd.hasNext())
				if (!dd.next().update(event, false)) dd.remove();
		}

		if (event.before.x != event.after.x || event.before.y != event.after.y) {
			Set<Dragger> draggers = root.find(onMouseMove, root.both()).flatMap(x -> x.stream()).map(x -> x.onMouseMove(event)).filter(x -> x!=null).collect(Collectors.toSet());
			ongoingDrags.computeIfAbsent(-1, (k) -> new LinkedHashSet<Dragger>()).addAll(draggers);
		}

		pressed.stream().forEach(p -> {
			Collection<Dragger> dragger = ongoingDrags.computeIfAbsent(p, (x) -> new ArrayList<>());
			root.find(onMouseDown, root.both()).flatMap(x -> x.stream()).map(x -> x.onMouseDown(event, p)).filter(x -> x != null).collect(Collectors.toCollection(() -> dragger));
		});

	}


}
