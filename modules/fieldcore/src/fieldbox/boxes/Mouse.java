package fieldbox.boxes;

import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Entry-point from GLFW based Window.Event<Window.MouseState> into Field.
 * <p>
 * Boxes can add to properties "onMouseDown", "onMouseMove", "onMouseEnter" and "onMouseExit" to listen to these events. Optionally these can return Draggers that will continue to be notified until
 * corresponding mouse up events.
 */
public class Mouse {

	static public final Dict.Prop<Map<String, OnMouseDown>> onMouseDown = new Dict.Prop<>("onMouseDown").type()
													    .toCannon()
													    .autoConstructs(() -> new IdempotencyMap<>(OnMouseDown.class));
	static public final Dict.Prop<Map<String, OnDoubleClick>> onDoubleClick = new Dict.Prop<>("onDoubleClick").type()
													    .toCannon()
													    .autoConstructs(() -> new IdempotencyMap<>(OnDoubleClick.class));
	static public final Dict.Prop<Map<String, OnMouseMove>> onMouseMove = new Dict.Prop<>("onMouseMove").type()
													    .toCannon()
													    .autoConstructs(() -> new IdempotencyMap<>(OnMouseMove.class));
	static public final Dict.Prop<Map<String, OnMouseEnter>> onMouseEnter = new Dict.Prop<>("onMouseEnter").type()
													       .toCannon()
													       .autoConstructs(() -> new IdempotencyMap<>(OnMouseEnter.class));
	static public final Dict.Prop<Map<String, OnMouseScroll>> onMouseScroll = new Dict.Prop<>("onMouseScroll").type()
														  .toCannon()
														  .autoConstructs(() -> new IdempotencyMap<>(OnMouseScroll.class));
	static public final Dict.Prop<Map<String, OnMouseExit>> onMouseExit = new Dict.Prop<>("onMouseExit").type()
													    .toCannon()
													    .autoConstructs(() -> new IdempotencyMap<>(OnMouseExit.class));
	static public final Dict.Prop<Boolean> isSelected = new Dict.Prop<>("isSelected").type()
											 .toCannon();
	static public final Dict.Prop<Boolean> isManipulated = new Dict.Prop<>("isManipulated").type()
											       .toCannon();
	static public final Dict.Prop<Boolean> isSticky = new Dict.Prop<>("isSticky").type()
										     .toCannon();

	static public final Dict.Prop<Integer> clickNumber= new Dict.Prop<>("clickNumber").type()
											  .toCannon();
	static public final Dict.Prop<Box> originatesAt= new Dict.Prop<>("originatesAt").type()
											  .toCannon();

	Map<Integer, Collection<Dragger>> ongoingDrags = new HashMap<Integer, Collection<Dragger>>();

	Box lastStartAt = null;
	long lastStartAtTime = 0;
	int click = 0;

	public void dispatch(Box root, Window.Event<Window.MouseState> event) {

		Optional<Drawing> drawing = root.find(Drawing.drawing, root.both())
						.findFirst();
		if (drawing.isPresent())
		{
			fixDrawingSpace(drawing, event.after);
			fixDrawingSpace(drawing, event.before);
		}

		Box startAt = Intersects.startAt(event.after, root);

		event.properties.put(originatesAt, startAt);

		Set<Integer> pressed = Window.MouseState.buttonsPressed(event.before, event.after);
		Set<Integer> released = Window.MouseState.buttonsReleased(event.before, event.after);
		Set<Integer> down = event.after.buttonsDown;

		if (pressed.size()==1) {
			if (startAt == lastStartAt && System.currentTimeMillis() - lastStartAtTime < 300) {
				event.properties.put(clickNumber, ++click);

			} else {
				event.properties.put(clickNumber, click=1);
				lastStartAt = startAt;
			}

			lastStartAtTime = System.currentTimeMillis();
		}

		released.stream()
			.map(r -> ongoingDrags.remove(r))
			.filter(x -> x != null)
			.forEach(drags -> {
				drags.stream()
				     .filter(d -> d != null)
				     .forEach(dragger -> dragger.update(event, true));
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
					t.printStackTrace();
					cont = false;
				}
				if (!cont) dd.remove();
			}
		}

		Util.Errors errors = new Util.Errors();


		if (event.before.x != event.after.x || event.before.y != event.after.y) {
			Set<Dragger> draggers = startAt.find(onMouseMove, startAt.both())
						       .flatMap(x -> x.values()
								      .stream())
						       .map(Util.wrap(x -> x.onMouseMove(event), errors, null, null))
						       .filter(x -> x != null)
						       .collect(Collectors.toSet());
			ongoingDrags.computeIfAbsent(-1, (k) -> new LinkedHashSet<Dragger>())
				    .addAll(draggers);
		}


		if (event.after.dwheely != 0.0 || event.after.dwheel != 0.0) startAt.breadthFirst(startAt.both())
										    .map(x -> {
//					System.out.println(" mapped :" + x);
											    return x;

										    })
										    .map(x -> x.properties.get(onMouseScroll))
										    .filter(x -> x != null)
										    .flatMap(x -> x.values()
												   .stream())
										    .forEach(Util.wrap(x -> x.onMouseScroll(event), errors));


		if (event.properties.getFloat(clickNumber, 0f)>1) {
			pressed.stream()
			       .forEach(p -> {
				       startAt.find(onDoubleClick, startAt.both())
					      .flatMap(x -> x.values()
							     .stream())
					      .forEach(x -> x.onDoubleClick(event));
			       });
		}

		pressed.stream()
		       .forEach(p -> {
			       Collection<Dragger> dragger = ongoingDrags.computeIfAbsent(p, (x) -> new ArrayList<>());

			       startAt.find(onMouseDown, startAt.both())
					   .map(x -> {
						   return x;
					   })
				      .flatMap(x -> x.values()
						     .stream())
				      .map(Util.wrap(x -> x.onMouseDown(event, p), errors, null, Dragger.class))
				      .filter(x -> x != null)
				      .collect(Collectors.toCollection(() -> dragger));
		       });

		if (errors.hasErrors()) {
			errors.getErrors()
			      .forEach(x -> {
				      x.first.printStackTrace();
			      });
		}

	}

	private void fixDrawingSpace(Optional<Drawing> drawing, Window.MouseState after) {
		Vec2 af = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(after.x, after.y)))
				 .get();

		after.mx = af.x;
		after.my = af.y;

		Vec2 daf = drawing.map(x -> x.windowSystemToDrawingSystemDelta(new Vec2(after.dx, after.dy)))
				  .get();

		after.mdx = daf.x;
		after.mdy = daf.y;

	}

	public interface Dragger {
		boolean update(Window.Event<Window.MouseState> e, boolean termination);
	}


	public interface OnMouseDown {
		Dragger onMouseDown(Window.Event<Window.MouseState> e, int button);
	}

	public interface OnDoubleClick {
		void onDoubleClick(Window.Event<Window.MouseState> e);
	}

	public interface OnMouseScroll {
		void onMouseScroll(Window.Event<Window.MouseState> e);
	}
	public interface OnMouseMove {
		Dragger onMouseMove(Window.Event<Window.MouseState> e);
	}

	public interface OnMouseEnter {
		Dragger onMouseEnter(Window.Event<Window.MouseState> e);
	}

	public interface OnMouseExit {
		void onMouseExit(Window.Event<Window.MouseState> e);
	}


}
