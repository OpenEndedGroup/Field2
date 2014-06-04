package fieldbox.boxes;

import field.graphics.Window;
import field.utility.Dict;

import java.util.Collection;

/**
 * Entry-point for File drops from GLFW into Field
 */
public class Drops {

	public interface OnDrop
	{
		public void onDrop(Window.Event<Window.Drop> drop);
	}

	static public final Dict.Prop<Collection<OnDrop>> onDrop = new Dict.Prop<>("onDrop").type().toCannon();

	public void dispatch(Box root, Window.Event<Window.Drop> drop)
	{
		root.find(onDrop, root.both()).flatMap( x-> x.stream()).forEach( x-> x.onDrop(drop));
	}

}
