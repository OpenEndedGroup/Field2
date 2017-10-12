package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.graphics.FLine;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.FLineInteraction;
import fieldbox.boxes.Mouse;

import java.util.LinkedHashSet;
import java.util.Map;

import static fieldbox.boxes.FLineInteraction._interactionTarget;

/**
 * Helper class for an FLine interaction handler that has hover, press and drag.
 */
public class FLineButton {

	private FLine target;
	private Map<String, Object> hover;
	private Map<String, Object> press;
	private Handle handle;
	private Box box;
	private LinkedHashSet<String> implicated;
	private Dict original;
	private Dict was;
	private boolean during = false;
	private boolean upSemantics = false;

	public interface Handle {
		void dragged(boolean up, FLine target, Window.Event<Window.MouseState> event);
	}

	public FLineButton() {
	}

	static public FLineButton attach(Box box, FLine target, Map<String, Object> hover, Map<String, Object> press, Handle h) {
		FLineButton b = new FLineButton();

		b.box = box;
		b.target = target;
		b.hover = hover;
		b.press = press;
		b.handle = h;

		target.attributes.putToMap(Mouse.onMouseEnter, "__FLineButton__", b::enter);
		target.attributes.putToMap(Mouse.onMouseExit, "__FLineButton__", b::exit);
		target.attributes.putToMap(Mouse.onMouseDown, "__FLineButton__", b::down);

		b.implicated = new LinkedHashSet<>();
		b.implicated.addAll(hover.keySet());
		b.implicated.addAll(press.keySet());

		b.original = target.attributes.duplicate();

		return b;
	}

	protected Mouse.Dragger enter(Window.Event<Window.MouseState> e) {
		Log.log("interactive.debug", () -> "ENTER !");

		if (during) return null;

		Dict d = new Dict();
		for (Map.Entry<String, Object> ee : hover.entrySet())
			d.put(new Dict.Prop<Object>(ee.getKey()), ee.getValue());

		was = target.attributes.putAll(d);
		target.modify();
		Drawing.dirty(box);

		return null;
	}

	protected Mouse.Dragger exit(Window.Event<Window.MouseState> e) {
		Log.log("interactive.debug", () -> "EXIT !");

		if (during) return null;

		target.attributes.putAll(was);
		target.modify();
		Drawing.dirty(box);

		return null;
	}

	/**
	 * set to true if you don't want to be called if the mouse up happens outside the box — this works like buttons that fire on mouse up; set to false if you want popup menu semantics, when the handler is stilled called when the up happes and the mouse has long moved away
	 */
	public FLineButton setUpSemantics(boolean upSemantics) {
		this.upSemantics = upSemantics;
		return this;
	}

	protected Mouse.Dragger down(Window.Event<Window.MouseState> d, int button) {
		Dict dp = new Dict();
		for (Map.Entry<String, Object> ee : press.entrySet())
			dp.put(new Dict.Prop<Object>(ee.getKey()), ee.getValue());

		target.attributes.putAll(dp);
		target.modify();
		Drawing.dirty(box);


		FLineInteraction interaction = d.properties.get(FLineInteraction.interaction);
		d.properties.put(Window.consumed, true);

		Log.log("interactive.debug", () -> "DOWN !, consuming " + d + " " + d.properties + " " + System.identityHashCode(d) + " " + interaction);

		during = true;

		return (e, t) -> {
			FLine ongoingTarget = e.properties.get(_interactionTarget);

			Log.log("interactive.debug", () -> "handling .... ");
			if (handle != null) {
				Vec2 point = interaction.convertCoordinateSystem(new Vec2(e.after.x, e.after.y));
				if (interaction.intersects(ongoingTarget, point)) {
					handle.dragged(t, this.target, e);

					System.out.println(" interaction is in ");
					target.attributes.putAll(dp);
					target.modify();
					Drawing.dirty(box);

				} else {
					if (!upSemantics)
						handle.dragged(t, this.target, e);

					System.out.println(" interaction is out, point :" + e.after.x + " " + e.after.y + " not in " + interaction.projectFLineToArea(ongoingTarget).getBounds());
					target.attributes.putAll(original, x -> implicated.contains(x.getName()));
					target.modify();
					Drawing.dirty(box);
				}

				Drawing.dirty(box);
			}

			e.properties.put(Window.consumed, true);

			if (t) {
				Log.log("interactive.debug", () -> "resetting on termination ");
				target.attributes.putAll(original, x -> implicated.contains(x.getName()));
				target.modify();

				during = false;

				Vec2 point = interaction.convertCoordinateSystem(new Vec2(e.after.x, e.after.y));
				if (interaction.intersects(ongoingTarget, point)) {
					RunLoop.main.delayTicks(() -> {
						enter(null);
					}, 2);
				}

				Drawing.dirty(box);
			}

			return !t;
		};
	}


}
