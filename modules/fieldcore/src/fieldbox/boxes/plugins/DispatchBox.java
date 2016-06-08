package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.io.IO;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static field.graphics.StandardFLineDrawing.strokeColor;
import static field.graphics.StandardFLineDrawing.stroked;
import static fieldbox.boxes.FLineDrawing.frameDrawing;

/**
 * A Box with a custom drawer for holding onto and drawning a connection between two other boxes.
 * <p>
 * Automatically deletes itself should either of its two ends disappears.
 * <p>
 * head() is the parent of tail() is the dispatch graph
 */
public class DispatchBox extends Box implements IO.Loaded // the drawer is initialized after all the properties are loaded
{

	static public final Dict.Prop<BoxRef> head = new Dict.Prop<>("head").type()
									    .toCannon()
									    .doc("the head of this topology arrow box");
	static public final Dict.Prop<BoxRef> tail = new Dict.Prop<>("tail").type()
									    .toCannon()
									    .doc("the tail of this topology arrow box");

	static public final Dict.Prop<FunctionOfBoxValued<Collection<Missing.Log>>> logThrough = new Dict.Prop<>("logThrough").type()
															      .toCannon()
															      .doc("property transcript filtered to include only property access that's _through_ this connection");

	static {
		// these properties need to be saved in our document
		IO.persist(head);
		IO.persist(tail);
	}

	static private final boolean notForInsert = true; // tell FileBrowser not to bother offering us for insertion
	Rect cache_h = null;
	Rect cache_t = null;

	public DispatchBox(Box head, Box tail) {
		this.properties.put(DispatchBox.head, new BoxRef(head));
		this.properties.put(DispatchBox.tail, new BoxRef(tail));
		this.properties.put(Chorder.nox, true);

		loaded();
	}


	/**
	 * custom constructor, call init after you are done
	 */
	protected DispatchBox() {
	}

	static public Pair<FLine, Vec2> arc(Rect from, Rect to, boolean selected) {

		return Dispatch.arc(from, to, selected);

		/*FLine f = new FLine();

		Vec2 a = new Vec2(from.x + from.w / 2, from.y + from.h / 2);
		Vec2 b = new Vec2(to.x + to.w / 2, to.y + to.h / 2);

		float d = (float) b.distance(a);

		Vec2 normal = Vec2.sub(b, a, new Vec2());

		Vec2 tan = new Vec2(-normal.y, normal.x).normalize().mul(d * 0.15f);
		if (normal.x > 0) tan.mul(-1);

		Vec2 c1 = new Vec2(a.x + normal.x * 1 / 3f + tan.x, a.y + normal.y * 1 / 3f + tan.y);
		Vec2 c2 = new Vec2(a.x + normal.x * 2 / 3f + tan.x, a.y + normal.y * 2 / 3f + tan.y);

		f.moveTo(a.x, a.y);
		f.cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y);

		return f;*/
	}

	public void loaded() {

		if (head() == null || tail() == null) throw new RuntimeException("can't load");

		String p1 = head().properties.get(Planes.plane);
		String p2 = tail().properties.get(Planes.plane);
		p1 = p1==null ? "" : p1;
		p2 = p2==null ? "" : p2;
		String q = (p1+" "+p2).trim();

		if (q.length()>0)
			this.properties.put(Planes.plane, q);

		System.out.println(" new dispatch box has plane :"+q);

		this.properties.put(Box.decorative, true);
		this.properties.putToMap(Boxes.insideRunLoop, "main.__checkfordeletion__", this::checkForDeletion);
		this.properties.putToMap(Boxes.insideRunLoop, "main.__updateframe__", this::updateFrameToMiddle);
		this.properties.computeIfAbsent(frameDrawing, this::defaultdrawsLines);
		this.properties.put(frame, head().properties.get(frame)
							    .union(tail().properties.get(frame)));
		this.properties.put(FrameManipulation.lockHeight, true); // the dimensions of this box cannot be changed
		this.properties.put(FrameManipulation.lockWidth, true);
		this.properties.put(FrameManipulation.lockX, true); // nor can its position
		this.properties.put(FrameManipulation.lockY, true);

		this.properties.put(Box.name, head().properties.get(Box.name) + "->" + tail().properties.get(Box.name));
		this.properties.putToMap(Callbacks.onDelete, "__cleanup__", (x) -> {
			if (x == this) {
				if (head() != null && tail() != null) head().disconnect(tail());
			}
			return null;
		});
		this.properties.put(logThrough, x -> {
			Predicate<Missing.Log> p = Missing.across(this.head(), this.tail());
			return Missing.getLog()
				      .stream()
				      .filter(p)
				      .collect(Collectors.toList());
		});

		this.properties.put(Chorder.nox, true);


	}

	protected Map<String, Function<Box, FLine>> defaultdrawsLines(Dict.Prop<Map<String, Function<Box, FLine>>> k) {
		Map<String, Function<Box, FLine>> r = new LinkedHashMap<>();

		r.put("__middle_nub__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;
			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			Rect r1 = ((DispatchBox) box).head().properties.get(frame);
			Rect r2 = ((DispatchBox) box).tail().properties.get(frame);

			if (r1 == null || r2 == null) {
				return null;
			}

			Pair<FLine, Vec2> fa = arc(r1, r2, selected);

			Vec2 at = fa.second;
			FLine f = fa.first;

			float w = selected ? 10 : 5;

			Log.log("nub", ()->"middle is " + at);

			f.attributes.put(strokeColor, selected ? new Vec4(1, 1, 1, -0.4f) : new Vec4(0, 0, 0, 0.1f));
			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Triple<Boolean, Rect, Rect>(properties.get(Mouse.isSelected), ((DispatchBox) box).head().properties.get(frame), (((DispatchBox) box).tail().properties.get(frame)))));

		return r;
	}

	public Vec2 middleOf(FLine f) {
		FLine.MoveTo m = (FLine.MoveTo) f.nodes.get(0);
		FLine.CubicTo c = (FLine.CubicTo) f.nodes.get(f.nodes.size() - 1);

		return FLinesAndJavaShapes.evaluateCubicFrame(m.to.toVec2(), c.c1.toVec2(), c.c2.toVec2(), c.to.toVec2(), 0.5f, new Vec2());
	}

	protected boolean checkForDeletion() {
		Box h = head();
		Box t = tail();
		if (h == null || t == null) {
			Drawing.dirty(this);
//			Callbacks.delete(this);
			this.disconnectFromAll();
			return true;
		}

		if (h.disconnected || t.disconnected) {
			Drawing.dirty(this);
//			Callbacks.delete(this);
			this.disconnectFromAll();
			return true;
		}

		Optional<Box> r = h.find(Boxes.root, h.both()).findAny();
		if (!r.isPresent()) {
			Drawing.dirty(this);
//			Callbacks.delete(this);
			this.disconnectFromAll();
			return true;
		}

		if (Planes.on(r.get(), h)<1 || Planes.on(r.get(), t)<1)
		{
			Drawing.dirty(this);
//			Callbacks.delete(this);
			this.disconnectFromAll();
			return true;
		}


		Set<Box> hc = h.children();
		if (!hc.contains(t)) {
			Drawing.dirty(this);
//			Callbacks.delete(this);
			this.disconnectFromAll();
			return true;
		}

		Set<Box> hp = h.parents();
		Set<Box> tp = t.parents();
		if (hp.size() == 0 || tp.size() == 0) {

			// now we need to prove the connectedness of either hp or tp, this is slightly expensive
			if (!h.breadthFirstAll(h.both()).filter(x -> x.properties.has(Boxes.root)).findAny().isPresent())
			{
				Drawing.dirty(this);
				Callbacks.delete(this);
				this.disconnectFromAll();
			}

			return true;
		}

		return true;
	}

	protected boolean updateFrameToMiddle() {
		Box hb = head();
		Box tb = tail();
		if (hb == null) return false;
		if (tb == null) return false;


		Rect h = hb.properties.get(frame);
		Rect t = tb.properties.get(frame);

		if (h == null || t == null) return false;

		if (Util.safeEq(h, cache_h) && Util.safeEq(t, cache_t)) return true;

		Vec2 m = arc(h, t, false).second;

		cache_h = h.duplicate();
		cache_t = t.duplicate();


		Rect frame1 = this.properties.get(frame);
		float w = 10;
		Rect frame2 = new Rect(m.x - w, m.y - w, w * 2, w * 2);

		if (!frame1.equals(frame2)) {
			this.properties.put(frame, frame2);
			Drawing.dirty(this);
		}

		return true;
	}

	protected Box head() {

		Box h = properties.get(head)
				  .get(this);
		if (h == null) {
			Log.log("huh", ()->"failed to find head");
		}
		return h;
	}

	protected Box tail() {

		Box h = properties.get(tail)
				  .get(this);

		if (h == null) {
			Log.log("huh", ()->"failed to find tail");
		}
		return h;
	}

}
