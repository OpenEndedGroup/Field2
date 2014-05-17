package fieldbox.boxes;

import field.graphics.Window;
import field.utility.Dict;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by marc on 5/16/14.
 */
public class Keyboard {

	public interface OnKeyDown {
		public Hold onKeyDown(Window.Event<Window.KeyboardState> e, int key);
	}

	public interface Hold {
		public boolean update(Window.Event<Window.KeyboardState> e, boolean termination);
	}

	Map<Integer, Collection<Hold>> ongoingDrags = new HashMap<Integer, Collection<Hold>>();

	static public final Dict.Prop<Collection<OnKeyDown>> onKeyDown = new Dict.Prop<>("onKeyDown").type().toCannon();


	public void dispatch(Box root, Window.Event<Window.KeyboardState> event) {
		Set<Integer> pressed = Window.KeyboardState.keysPressed(event.before, event.after);
		Set<Integer> released = Window.KeyboardState.keysReleased(event.before, event.after);
		Set<Integer> down = event.after.keysDown;


		System.out.println(" dispatching keyboard to tree :"+pressed+" "+released+" "+down);

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

			root.find(onKeyDown, root.both()).flatMap(x -> x.stream()).map(x -> x.onKeyDown(event, p)).filter(x -> x != null).collect(Collectors.toCollection(() -> hold));
		});
	}
}