package fieldbox.boxes;

import field.graphics.Window;
import field.utility.Dict;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Entry-point from GLFW based Window.Event<Window.KeyboardState> into Field. Defines OnKeyDown and Hold interfaces for Boxes to implement if they
 * want to listen to key presses (and holds and, thus, releases).
 * <p>
 * This operates in exactly the same manner as "Mouse" except there is no analogue of the OnMouseMove (no hovering of hands over the keyboard).
 */
public class Keyboard {

	public interface OnKeyDown {
		public Hold onKeyDown(Window.Event<Window.KeyboardState> e, int key);
	}

	public interface OnCharTyped {
		public void onCharTyped(Window.Event<Window.KeyboardState> e, char key);
	}

	public interface Hold {
		public boolean update(Window.Event<Window.KeyboardState> e, boolean termination);
	}

	Map<Integer, Collection<Hold>> ongoingDrags = new HashMap<Integer, Collection<Hold>>();

	static public final Dict.Prop<Collection<OnKeyDown>> onKeyDown = new Dict.Prop<>("onKeyDown").type().toCannon();
	static public final Dict.Prop<Collection<OnCharTyped>> onCharTyped = new Dict.Prop<>("onCharTyped").type().toCannon();


	public void dispatch(Box root, Window.Event<Window.KeyboardState> event) {
		Set<Integer> pressed = Window.KeyboardState.keysPressed(event.before, event.after);
		Set<Character> typed = Window.KeyboardState.charsPressed(event.before, event.after);
		Set<Integer> released = Window.KeyboardState.keysReleased(event.before, event.after);
		Set<Integer> down = event.after.keysDown;

		released.stream().map(r -> ongoingDrags.remove(r)).filter(x -> x != null).forEach(drags -> {
			drags.stream().forEach(Hold -> Hold.update(event, true));
			drags.clear();
		});

		for (Collection<Hold> d : ongoingDrags.values()) {
			Iterator<Hold> dd = d.iterator();
			while (dd.hasNext()) {
				Hold Hold = dd.next();
				boolean cont = false;
				try {
					cont = Hold.update(event, false);
				} catch (Throwable t) {
					System.err.println(" Exception thrown in Hold update :" + t);
					System.err.println(" Hold will not be called again");
					cont = false;
				}
				if (!cont) dd.remove();
			}
		}

		pressed.stream().forEach(p -> {
			Collection<Hold> hold = ongoingDrags.computeIfAbsent(p, (x) -> new ArrayList<>());

			// change map to handle errors on x

			root.find(onKeyDown, root.both()).flatMap(x -> x.stream()).map(x -> x.onKeyDown(event, p)).filter(x -> x != null)
				    .collect(Collectors.toCollection(() -> hold));
		});

		typed.stream().forEach(p -> {
			root.find(onCharTyped, root.both()).flatMap(x -> x.stream()).forEach(x -> x.onCharTyped(event, p));
		});

	}
}