package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fielded.Commands;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A Box that draws itself in a particular way, and drags it's children around with them
 */
public class BoxGroup extends Box implements IO.Loaded{

	static public Dict.Prop<Boolean> _collapsed = new Dict.Prop<>("collapsed").type().doc("is this group collapsed?");

	static
	{
		IO.persist(_collapsed);
	}

	public BoxGroup() {

		this.properties.putToMap(Boxes.insideRunLoop, "main.__checkforcollapsed__", () -> {
			if (this.properties.isTrue(_collapsed, false)!=collapsed)
			{
				if (this.properties.isTrue(_collapsed, false)) collapse();
				else expand();
			}
			return true;
		});

		this.properties.putToMap(FLineDrawing.frameDrawing, "__outline__", FrameChangedHash.getCached(this, (a, b) -> {
			Rect r = computeFrame();

			if (r == null) return new FLine();

			return drawFrame(r);
		}, () -> {

			long l = 0;
			for (Box c : children()) {
				l = l * 31 + c.hashCode() + 17 * (c.disconnected ? 1 : 0);
			}
			return l;
		}));

		this.properties.putToMap(FLineDrawing.frameDrawing, "__outline2__", FrameChangedHash.getCached(this, (a, b) -> {
			Rect r = computeFrame();

			if (r == null) return new FLine();

			return drawName(r);

		}, () -> {

			long l = 0;
			for (Box c : children) {
				l = l * 31 + c.hashCode() + 17 * (c.disconnected ? 1 : 0);
			}
			return l;
		}));

		this.properties.putToMap(Commands.command, "Isolate contents of group", (k) -> {
			isolateContents();
			return null;
		});
		this.properties.putToMap(Commands.commandDoc, "Isolate contents of group",
					 "Make sure that the contents of this group are not connected to the root of the box graph directly. This will then allow you to collapse and expand this this group. All connections from the contents of the group will go through this group box. ");

		this.properties.putToMap(Commands.commandGuard, "Isolate contents of group", (k) -> !isContentsIsolated());

		this.properties.putToMap(Commands.command, "Collapse group", (k) -> {
			collapse();
			return null;
		});


		this.properties.putToMap(Commands.command, "Integrate contents of group", (k) -> {
			integrateContents();
			return null;
		});
		this.properties.putToMap(Commands.commandDoc, "Integrate contents of group",
					 "Make sure that the contents of this group are also directly connected to the root of the box graph. This means, if you delete the group, the boxes remain connected. ");

		this.properties.putToMap(Commands.commandGuard, "Integrate contents of group", (k) -> isContentsIsolated());

		this.properties.putToMap(Commands.command, "Collapse group", (k) -> {
			collapse();
			return null;
		});

		this.properties.putToMap(Commands.commandDoc, "Collapse group", "Hides the contents of this group");

		this.properties.putToMap(Commands.commandGuard, "Collapse group", (k) -> isContentsIsolated() && !isCollapsed());

		this.properties.putToMap(Commands.command, "Expand group", (k) -> {
			expand();
			return null;
		});

		this.properties.putToMap(Commands.commandDoc, "Expand group", "Shows the contents of this group");

		this.properties.putToMap(Commands.commandGuard, "Expand group", (k) -> isCollapsed());

		this.properties.putToMap(Callbacks.onFrameChanged, "__drageverything__", (on, next) -> {

			System.out.println(" on frame change :"+on+" / "+this);

			if (on == this) {
				Rect from = on.properties.get(Box.frame);

				System.out.println("   on :"+from+" -> "+next);

				if (from.w!=next.w && collapsed) return next;

				if (from.x == next.x) {
					if (from.w != next.w) {
						changeChildren(x -> {
							x.x = (x.x - from.x) * next.w / from.w + from.x;
						});
					}
				} else {
					if (from.w + from.x == next.w + next.x) {
						changeChildren(x -> {
							x.x = (x.x - (from.x + from.w)) * next.w / from.w + from.x + from.w;
						});
					} else {
						changeChildren(x -> {
							x.x += next.x - from.x;
						});
					}
				}
				if (from.y == next.y) {
					if (from.h != next.h) {
						changeChildren(x -> {
							x.y = (x.y - from.y) * next.h / from.h + from.y;
						});
					}
				} else {
					if (from.h + from.y == next.h + next.y) {
						changeChildren(x -> {
							x.y = (x.y - (from.y + from.h)) * next.h / from.h + from.y + from.h;
						});
					} else {
						changeChildren(x -> {
							x.y += next.y - from.y;
						});
					}
				}
			}
			return next;
		});
	}

	private Rect computeFrame() {
		if (collapsed) {
			Rect r = this.properties.get(Box.frame);
			Rect rr = new Rect(r.x, r.y, 50, 50);
			if (!rr.equals(r)) {
				rr = Callbacks.frameChange(this, rr);
				this.properties.put(Box.frame, rr);
			}
			return rr;
		}

		Rect r = null;
		for (Box c : children()) {
			if (c.disconnected) continue;
			r = Rect.union(r, c.properties.get(Box.frame));
		}
		if (r == null) {
			return null;
		} else {
			r = r.inset(-10);
			this.properties.put(Box.frame, r);
			return r;
		}
	}

	Map<String, Boolean> collapsedState = new LinkedHashMap<>();

	private void collapse() {
		this.properties.put(_collapsed, true);
		collapsedAt = this.properties.get(Box.frame);
		collapsed = true;
		this.breadthFirst(this.downwards())
		    .filter(x -> x != this)
		    .forEach(x -> {
			    collapsedState.put(x.properties.getOrConstruct(IO.id), x.disconnected);
			    x.disconnected = true;
		    });
	}

	private void expand() {
		this.properties.put(_collapsed, false);
		collapsed = false;
		this.breadthFirstAll(this.downwards())
		    .filter(x -> x != this)
		    .forEach(x -> {
			    Boolean m = collapsedState.get(x.properties.getOrConstruct(IO.id));
			    x.disconnected = m!=null ? m.booleanValue() : false;
		    });
	}

	boolean collapsed = false;
	Rect collapsedAt = null;

	public boolean isCollapsed() {
		return collapsed;
	}

	protected void isolateContents() {
		Box root = this.find(Boxes.root, both())
			       .findFirst()
			       .get();
		this.breadthFirst(this.downwards())
		    .filter(x -> x != this)
		    .forEach(x -> {
			    root.disconnect(x);
		    });

		root.connect(this);
	}

	protected void integrateContents() {
		Box root = this.find(Boxes.root, both())
			       .findFirst()
			       .get();
		this.breadthFirst(this.downwards())
		    .filter(x -> x != this)
		    .forEach(x -> {
			    root.connect(x);
		    });

		root.disconnect(this);
	}
	public boolean isContentsIsolated() {
		Box root = this.find(Boxes.root, both())
			       .findFirst()
			       .get();
		return !this.breadthFirst(this.downwards())
			    .filter(x -> x != this)
			    .filter(x -> root.children()
					     .contains(x))
			    .findAny()
			    .isPresent();
	}

	protected void changeChildren(Consumer<Vec2> p) {
		for (Box c : children()) {
			Rect f = c.properties.get(Box.frame);
			Vec2 v0 = new Vec2(f.x, f.y);
			Vec2 v1 = new Vec2(f.x + f.w, f.y + f.h);

			p.accept(v0);
			p.accept(v1);

			c.properties.put(Box.frame, Callbacks.frameChange(c, new Rect(v0.x, v0.y, v1.x - v0.x, v1.y - v0.y)));
		}
	}

	private FLine drawFrame(Rect r) {
		FLine f = new FLine().rect(r);

		if (this.properties.isTrue(Mouse.isSelected, false)) {
			f.attributes.put(StandardFLineDrawing.strokeColor, new Vec4(0, 0, 0, -0.15f));
			f.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(10));
			f.attributes.put(StandardFLineDrawing.fillColor, new Vec4(0, 0, 0, 0.15f));
			f.attributes.put(StandardFLineDrawing.filled, true);
		} else {
			f.attributes.put(StandardFLineDrawing.strokeColor, new Vec4(0, 0, 0, 0.5f));
			f.attributes.put(StandardFLineDrawing.fillColor, new Vec4(0, 0, 0, 0.15f));
			f.attributes.put(StandardFLineDrawing.filled, true);
		}
		return f;
	}

	private FLine drawName(Rect r) {
		FLine f = new FLine().moveTo(r.x + r.w / 2, r.y -4);
		f.attributes.put(StandardFLineDrawing.hasText, true);
		f.last().attributes.put(StandardFLineDrawing.textAlign, 0.5f);
		if (collapsed)
		{
			f.last().attributes.put(StandardFLineDrawing.text, this.properties.get(Box.name)+" (contains "+collapsedState.size()+" box"+(collapsedState.size()==1 ? "" : "es)"));
		}
		else
		f.last().attributes.put(StandardFLineDrawing.text, this.properties.get(Box.name));
		return f;
	}


	@Override
	public void loaded() {
	}
}
