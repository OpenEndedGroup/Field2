package fieldbox.boxes.plugins;

import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Mutable;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fieldbox.io.IO;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command-shift drag to copy boxes
 */
public class DragToCopy extends Box {

	static public final Dict.Prop<FunctionOfBox<Box>> duplicate = new Dict.Prop<FunctionOfBox<Box>>("duplicate").toCanon().doc("duplicates a box (and all of it's properties)");

	public DragToCopy(Box root)
	{
		this.properties.put(duplicate, x -> {
			List<Box> o = duplicateGroup(Collections.singletonList(x));
			return o.get(0);
		});
		this.properties.putToMap(Mouse.onMouseDown, "__dragToCopy__", (e, button)->{

			if (button==0 && e.after.keyboardState.isSuperDown() && e.after.keyboardState.isShiftDown() && selection().findAny().isPresent())
			{
				e.properties.put(Window.consumed, true);

				Log.log("duplicate", ()->"we are go "+e);

				return duplicateAndDrag(e);

			}

			return null;
		});
		this.properties.putToMap(Mouse.onMouseMove, "__dragToCopy__", e -> {

			// feedback
			return null;
		});

	}

	private Mouse.Dragger duplicateAndDrag(Window.Event<Window.MouseState> e) {

		List<Box> s = selection().collect(Collectors.toList());

		List<Box> s2 = duplicateGroup(s);

		Log.log("duplicate", ()->"duplicated, got :"+s2);

		Optional<Drawing> drawing = this.find(Drawing.drawing, both())
						.findFirst();
		Vec2 downAt = new Vec2(e.after.mx, e.after.my);


		return (d, term) -> {

			Vec2 next = new Vec2(d.after.mx, d.after.my);

			Vec2 delta = new Vec2(next).sub(downAt);

			downAt.set(next);
			Log.log("duplicate", ()->"delta :"+delta);

			for(Box b : s2)
			{
				Rect f = b.properties.get(Box.frame);
				f.x+=delta.x;
				f.y+=delta.y;
			}

			Drawing.dirty(this);

			return !term;
		};

	}

	private List<Box> duplicateGroup(List<Box> s) {

		Map<Box, Box> mapping = new LinkedHashMap<>();

		for(Box b : s)
		{
			mapping.put(b, duplicateBox(b, b.getClass()));
		}


		for(Box b : mapping.keySet())
		{
			Box o = mapping.get(b);
			for(Box p : b.parents())
			{
				p = mapping.getOrDefault(p, p);
				p.connect(o);

				final Box finalP = p;
				Log.log("duplicate", ()->"connect parent :"+ finalP +" -> "+o);
			}
			for(Box p : b.children())
			{
				p = mapping.getOrDefault(p, p);
				o.connect(p);

				final Box finalP = p;
				Log.log("duplicate", ()->"connect child :"+o+" -> "+ finalP);
			}
		}

		for(Box b : mapping.values())
		{
			if (b instanceof IO.Loaded)
				((IO.Loaded)b).loaded();
			IO.uniqify(b);
		}

		return new ArrayList<>(mapping.values());

	}

	static public Box duplicateBox(Box b, Class clazz)
	{
		try {
			Box c = (Box) clazz
				.getConstructor()
				.newInstance();

			// copy any IO.persist and Mutable attributes

			Set<Map.Entry<Dict.Prop, Object>> es = b.properties.getMap()
				.entrySet();
			for(Map.Entry<Dict.Prop, Object> e : es)
			{
				if (!e.getKey().getAttributes().isTrue(IO.dontCopy, false) && (IO.isPeristant(e.getKey()) || e.getValue() instanceof Mutable || e.getValue() instanceof Serializable))
				{
					Object v = e.getValue();
					if (v instanceof Mutable) v = ((Mutable)v).duplicate();
					else
					if (v instanceof Serializable)
					{
						v = duplicateSerializable((Serializable) v);
						if (v==null) continue;
					}

					c.properties.put(e.getKey(), v);
				}
			}


			return c;
		} catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			Log.log("duplicate.error", ()->"problem while invoking class noarg constructor");
			e.printStackTrace();
			return null;
		}
	}


	private static Serializable duplicateSerializable(Serializable v) {
		ByteArrayOutputStream t = new ByteArrayOutputStream();
		try {
			ObjectOutputStream o = new ObjectOutputStream(t);
			o.writeObject(v);
			o.close();
			ObjectInputStream i = new ObjectInputStream(new ByteArrayInputStream(t.toByteArray()));
			return (Serializable) i.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(
			    x -> x.properties.isTrue(Mouse.isSelected, false) && !x.properties.isTrue(Mouse.isSticky, false));
	}

}
