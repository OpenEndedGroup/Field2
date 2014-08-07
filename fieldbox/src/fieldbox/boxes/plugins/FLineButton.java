package fieldbox.boxes.plugins;

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

/**
 * Helper class for an FLine interaction handler that has hover, press and drag
 */
public class FLineButton {

	private final FLine target;
	private final Map<String, Object> hover;
	private final Map<String, Object> press;
	private final Handle handle;
	private final Box box;
	private final LinkedHashSet<String> implicated;
	private Dict original;
	private Dict was;
	private boolean during = false;

	public interface Handle {
		public void dragged(FLine target, Window.Event<Window.MouseState> event);
	}

	public FLineButton(Box box, FLine target, Map<String, Object> hover, Map<String, Object> press, Handle h) {
		this.box = box;
		this.target = target;
		this.hover = hover;
		this.press = press;
		this.handle = h;

		target.attributes.putToList(Mouse.onMouseEnter, this::enter);
		target.attributes.putToList(Mouse.onMouseExit, this::exit);
		target.attributes.putToList(Mouse.onMouseDown, this::down);


		implicated = new LinkedHashSet<>();
		implicated.addAll(hover.keySet());
		implicated.addAll(press.keySet());

		original = target.attributes.duplicate();
	}

	protected Mouse.Dragger enter(Window.Event<Window.MouseState> e) {
		Log.log("iteractive.debug", "ENTER !");

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
		Log.log("iteractive.debug", "EXIT !");

		if (during) return null;

		target.attributes.putAll(was);
		target.modify();
		Drawing.dirty(box);

		return null;
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

		Log.log("iteractive.debug", "DOWN !, consuming "+d+" "+d.properties+" "+System.identityHashCode(d)+" "+interaction);

		during = true;

		return (e, t) -> {

			Log.log("interactive.debug", "handling .... ");
			if (handle != null) {
				handle.dragged(this.target, e);
				Drawing.dirty(box);
			}

			e.properties.put(Window.consumed, true);

			if (t) {
				Log.log("interactive.debug", "resetting on termination ");
				target.attributes.putAll(original, x -> implicated.contains(x.getName()));
				target.modify();

				during = false;

				Vec2 point = interaction.convertCoordinateSystem(new Vec2(e.after.x, e.after.y));
				if (target.attributes.get(FLineInteraction.projectedArea).apply(target).contains(point.x, point.y))
				{
					enter(null);
				}

				Drawing.dirty(box);
			}

			return true;
		};
	}


}
