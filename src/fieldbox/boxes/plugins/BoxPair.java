package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fielded.Commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Created by marc on 02/10/15.
 */
public class BoxPair extends Box {

	private final Box root;

	static public final Dict.Prop<Boolean> isPaired = new Dict.Prop<Box>("isPaired").doc("is this a special paired box").toCanon().type();
	static public final Dict.Prop<Box> f = new Dict.Prop<Box>("f").doc("the 'footage' box").toCanon().type().set(availableForCompletion, x -> x.properties.has(isPaired));
	static public final Dict.Prop<Box> m = new Dict.Prop<Box>("m").doc("the 'main' box").toCanon().type().set(availableForCompletion, x -> x.properties.has(isPaired));

	static
	{
		IO.persist(isPaired);
	}

	public BoxPair(Box root)
	{
		this.root = root;

		this.properties.putToMap(Commands.command, "Create Box Pair", (x) -> {
			this.newBoxPair();
			return null;
		});
		this.properties.putToMap(Commands.commandDoc, "Create Box Pair",
			"Create a pair of boxes that are linked together. This is useful for representing a selection of a interval of time from a longer interval. In the parlance of video editing, for example, there's a _selection_ from some _footage_. In Field these are both represented by boxes.");


		Cached<Object, Long, Object> work = FrameChangedHash.getCached(this, (a, last) -> {
			checkPairs();
			return null;
		}, () -> salt);

		this.properties.putToMap(Boxes.insideRunLoop, "main.__checkPairs__", () -> {
			work.apply(null);
			return true;
		});

		this.properties.putToMap(Callbacks.onFrameChanged, "__checkPairFrames__", (box, rect) -> {

			if (!box.properties.isTrue(isPaired, false)) return rect;
			boolean isMain = box.properties.get(Box.name).trim().length()>0;
			Rect now = box.properties.get(Box.frame);
			Rect next = rect;
			FieldBoxWindow window = find(Boxes.window, both()).findFirst().get();
			if (!window.getCurrentMouseState().keyboardState.isShiftDown()) {



				double dx1 = now.x - next.x;
				double dx2 = (now.x + now.w) - (next.x + next.w);
				double dy1 = now.y - next.y;
				double dy2 = (now.y + now.h) - (next.y + next.h);


				if (isMain && eq(dx1, dx2) && eq(dy1, dy2)) {
					// pure translation
					Rect ff = box.properties.get(f).properties.get(Box.frame);
					ff.x -= dx1;
					ff.y -= dy1;
				} else if (isMain && eq(now.x + now.w, next.x + next.w) && !eq(now.x, next.x)) {
					// scale around end point;
					double l1 = next.w / now.w;

					Rect ff = box.properties.get(f).properties.get(Box.frame);
					double newStart = (float) ((ff.x - (now.x + now.w)) * l1 + (now.x + now.w));
					double newEnd = (float) (((ff.x + ff.w) - (now.x + now.w)) * l1 + (now.x + now.w));

					ff.x = (float) newStart;
					ff.w = (float) (newEnd - newStart);
				} else if (isMain && eq(now.x, next.x) && !eq(now.x + now.w, next.x + next.w)) {
					// scale around start point;
					double l1 = next.w / now.w;

					Rect ff = box.properties.get(f).properties.get(Box.frame);

					double newStart = (float) ((ff.x - (now.x)) * l1 + (now.x));
					double newEnd = (float) (((ff.x + ff.w) - (now.x)) * l1 + (now.x));

					ff.x = (float) newStart;
					ff.w = (float) (newEnd - newStart);
				}

			}
			if (isMain)
			{
				double d = next.y+next.h;
				Rect ff = box.properties.get(f).properties.get(Box.frame);
				ff.y = (float) (d + gap);
				ff.h = h/3;
			}
			else
			{
				double d = next.y+next.h;
				Rect ff = box.properties.get(m).properties.get(Box.frame);
				ff.y = (float) (next.y-ff.h - gap);
			}

			return rect;
		});

	}

	private boolean eq(double dy1, double dy2) {
		return Math.abs(dy1-dy2)<0.1f;
	}

	private void checkPairs() {

		Set<Box> mainBoxes = new LinkedHashSet<>();
		Set<Box> frameBoxes = new LinkedHashSet<>();

		this.breadthFirst(this.both()).filter(x -> x.properties.has(isPaired) && x.properties.isTrue(isPaired, false)).forEach(x -> {
			if (x.properties.get(Box.name).length()>0)
			{
				mainBoxes.add(x);
			}
			else
			{
				frameBoxes.add(x);
			}
		});


		for(Box b : mainBoxes)
		{
			for(Box bb : b.children())
			{
				if (bb.properties.get(Box.name).trim().length()==0)
				{
					b.properties.put(f, bb);
					bb.properties.put(m, b);
					b.properties.put(isPaired, true);
					bb.properties.put(isPaired, true);
				}
			}
		}

		for(Box bb : frameBoxes)
		{
			bb.properties.put(name, "");
			// install decoration if there isn't any
			bb.properties.put(isPaired, true);
		}

		// callbacks for frame changed!
		for(Box bb : frameBoxes)
		{
			IdempotencyMap<Supplier<Collection<? extends Supplier<FLine>>>> q = bb.properties.getOrConstruct(FLineDrawing.bulkLines);
			Supplier<Collection<? extends Supplier<FLine>>> fr = q.get("__framemarking__");
			if (fr==null)
			{
				installFrameDecor(bb);
			}
		}
		for(Box bb : mainBoxes)
		{
			IdempotencyMap<Supplier<Collection<? extends Supplier<FLine>>>> q = bb.properties.getOrConstruct(FLineDrawing.bulkLines);
			Supplier<Collection<? extends Supplier<FLine>>> fr = q.get("__framemarking__");
			if (fr==null)
			{
				installMainDecor(bb);
			}
		}



	}


	private void installFrameDecor(Box bb) {
		Collection<Supplier<FLine>> r = new ArrayList<>();
		{
			FLine f = new FLine();
			f.moveTo(0, -100);
			f.lineTo(0, 100);
			f.attributes.put(StandardFLineDrawing.color, new Vec4(0,0,0,0.2));
			r.add(FLineDrawing.boxOrigin(() -> f, new Vec2(0, 0.5), bb));
		}
		{
			FLine f = new FLine();
			f.moveTo(0, -100);
			f.lineTo(0, 100);
			f.attributes.put(StandardFLineDrawing.color, new Vec4(0,0,0,0.2));
			r.add(FLineDrawing.boxOrigin(() -> f, new Vec2(1, 0.5), bb));
		}

		bb.properties.putToMap(FLineDrawing.bulkLines, "__framemarking__", () -> r);
	}

	private void installMainDecor(Box bb) {
		Collection<Supplier<FLine>> r = new ArrayList<>();
		{
			FLine f = new FLine();
			f.moveTo(0, -100);
			f.lineTo(0, 100);
			f.attributes.put(StandardFLineDrawing.color, new Vec4(0,0,0,0.2));
			r.add(FLineDrawing.boxOrigin(() -> f, new Vec2(0, 0.5), bb));
		}
		{
			FLine f = new FLine();
			f.moveTo(0, -100);
			f.lineTo(0, 100);
			f.attributes.put(StandardFLineDrawing.color, new Vec4(0,0,0,0.2));
			r.add(FLineDrawing.boxOrigin(() -> f, new Vec2(1, 0.5), bb));
		}

		Box frameBox = bb.children().iterator().next();

		r.add(() -> {
			Rect f1 = bb.properties.get(Box.frame);
			Rect f2 = frameBox.properties.get(Box.frame);

			FLine q = new FLine();
			q.rect(f1.x, f2.y-gap/2, f1.w, f2.h+gap);
			q.attributes.put(StandardFLineDrawing.filled, true);
			q.attributes.put(StandardFLineDrawing.color, new Vec4(0,0,0,0.2));
			return q;
		});

		bb.properties.putToMap(FLineDrawing.bulkLines, "__framemarking__", () -> r);
	}

	long salt = 0;
	float gap = 5;  int w = 100;
	int h = 60;


	public void newBoxPair()
	{
		salt ++;

		Box b1 = new Box();
		Box b2 = new Box();

		b1.connect(b2);
		root.connect(b1);
		b1.properties.put(Box.name, "Untitled");
		b2.properties.put(Box.name, "");

		FieldBoxWindow window = root.find(Boxes.window, root.both()).findFirst().get();
		Window.MouseState mouse = window.getCurrentMouseState();


		double cx = mouse.mx;
		double cy = mouse.my;
		b1.properties.put(Box.frame, new Rect(cx-w/2, cy-h/2, w, h));
		b2.properties.put(Box.frame, new Rect(cx-w/2, cy+gap, w, h/3));

		b1.properties.put(f, b2);
		b2.properties.put(m, b1);

		b1.properties.put(isPaired, true);
		b2.properties.put(isPaired, true);

		b2.properties.put(FrameManipulation.lockHeight, true);
	}
}
