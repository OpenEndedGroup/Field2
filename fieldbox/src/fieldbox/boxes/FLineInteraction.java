package fieldbox.boxes;

import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.linalg.Vec2;
import field.utility.Cached;
import field.utility.Dict;
import field.utility.Rect;
import field.graphics.Window;

import java.awt.*;
import java.awt.geom.Area;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by marc on 3/23/14.
 */
public class FLineInteraction extends Box implements Drawing.Drawer, Manipulation.OnMouseDown, Manipulation.OnMouseMove {

	static public final Dict.Prop<FLineInteraction> interaction = new Dict.Prop<>("interaction");
	static public final Dict.Prop<Cached<FLine, Object, Area>> projectedArea = new Dict.Prop<>("_projectedArea");
	static public final Dict.Prop<Map<String, Function<Box, FLine>>> interactiveDrawing = new Dict.Prop<>("interactiveDrawing");

	public FLineInteraction() {
		properties.putToList(Drawing.drawers, this);
		properties.put(interaction, this);
		properties.putToList(Manipulation.onMouseDown, this);
		properties.putToList(Manipulation.onMouseMove, this);
	}

	Set<FLine> all = null;


	@Override
	public void draw(Drawing context) {
		all = new LinkedHashSet<>();
		this.breadthFirst(this.both()).forEach(x -> {
			Rect r = x.properties.get(Manipulation.frame);

			Map<String, Function<Box, FLine>> drawing = x.properties.get(interactiveDrawing);
			if (drawing == null) return;

			Iterator<Function<Box, FLine>> it = drawing.values().iterator();
			while (it.hasNext()) {
				Function<Box, FLine> f = it.next();
				FLine fl = f.apply(x);
				if (fl == null) it.remove();
				else all.add(fl);
			}
		});
		for (FLine f : all)
			f.attributes.computeIfAbsent(projectedArea, (k) -> new Cached<FLine, Object, Area>((fline, previously) -> projectFLineToArea(fline), (fline) -> new Object[]{fline, fline.getModCount()}));
	}

	public Area projectFLineToArea(FLine fline) {
		Shape s = FLinesAndJavaShapes.flineToJavaShape(fline);
		return new Area(s);
	}

	public Vec2 convertCoordinateSystem(Vec2 event) {
		Optional<Drawing> drawing = this.find(Drawing.drawing, both()).findFirst();
		return drawing.map(x -> x.windowSystemToDrawingSystem(event)).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));
	}

	public boolean intersects(FLine f, Vec2 position)
	{
		Cached<FLine, Object, Area> area = f.attributes.computeIfAbsent(projectedArea, (k) -> new Cached<FLine, Object, Area>((fline, previously) -> projectFLineToArea(fline), (fline) -> new Object[]{fline, fline.getModCount()}));
		return area.apply(f).contains(position.x, position.y);
	}


	@Override
	public Manipulation.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {

		// if we haven't been drawn, then we can't interact
		
		if (all==null) return null;

		Vec2 point = convertCoordinateSystem(new Vec2(e.after.x, e.after.y));

		Set<FLine> hit = new LinkedHashSet<FLine>();

		Window.Event<Window.MouseState> eMarked = e.copy();
		eMarked.properties.put(interaction, this);

		List<Manipulation.Dragger> draggers = all.stream().filter(f -> f.attributes.get(projectedArea).apply(f).contains(point.x, point.y))
			    .flatMap(f -> f.attributes.getOr(Manipulation.onMouseDown, Collections::emptyList).stream())
			    .map(omd -> omd.onMouseDown(eMarked, button))
			    .filter(x -> x != null)
			    .collect(Collectors.toList());

		if (draggers.size()>0)
		{
			return (event, termination) -> {
				event = event.copy();
				event.properties.put(interaction, this);

				Iterator<Manipulation.Dragger> it = draggers.iterator();
				while(it.hasNext())
				{
					if (!it.next().update(event, termination)) it.remove();
				}
				return draggers.size()>0;
			};
		}

		return null;
	}

	Set<FLine> previousIntersection = new LinkedHashSet<>();

	@Override
	public Manipulation.Dragger onMouseMove(Window.Event<Window.MouseState> e) {
		if (all==null) return null;

		Vec2 point = convertCoordinateSystem(new Vec2(e.after.x, e.after.y));

		Set<FLine> hit = new LinkedHashSet<FLine>();

		Window.Event<Window.MouseState> eMarked = e.copy();
		eMarked.properties.put(interaction, this);

		Set<FLine> intersects = all.stream().filter(f -> f.attributes.has(projectedArea)).filter(f -> f.attributes.get(projectedArea).apply(f).contains(point.x, point.y)).collect(Collectors.toSet());

		Set<FLine> enter = new LinkedHashSet<FLine>(intersects);
		enter.removeAll(previousIntersection);

		Set<FLine> exit = new LinkedHashSet<FLine>(previousIntersection);
		exit.removeAll(intersects);

		previousIntersection = intersects;

		List<Manipulation.Dragger> draggers = intersects.stream()
			    .flatMap(f -> f.attributes.getOr(Manipulation.onMouseMove, Collections::emptyList).stream())
			    .map(omd -> omd.onMouseMove(eMarked))
			    .filter(x ->x !=null)
			    .collect(Collectors.toList());

		draggers.addAll(enter.stream()
			    .flatMap(f -> f.attributes.getOr(Manipulation.onMouseEnter, Collections::emptyList).stream())
			    .map(omd -> omd.onMouseEnter(eMarked))
			    .filter(x ->x !=null)
			    .collect(Collectors.toList()));

		exit.stream()
			    .flatMap(f -> f.attributes.getOr(Manipulation.onMouseExit, Collections::emptyList).stream())
			    .forEach(omd -> omd.onMouseExit(eMarked));


		if (draggers.size()>0)
		{
			return (event, termination) -> {
				event = event.copy();
				event.properties.put(interaction, this);

				Iterator<Manipulation.Dragger> it = draggers.iterator();
				while(it.hasNext())
				{
					if (!it.next().update(event, termination)) it.remove();
				}
				return draggers.size()>0;
			};
		}


		return null;
	}

}
