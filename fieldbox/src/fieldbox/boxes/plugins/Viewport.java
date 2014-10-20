package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.GraphicsContext;
import field.graphics.Scene;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;

import java.awt.*;

import static field.graphics.StandardFLineDrawing.*;

/**
 * A box that has a 3d drawing canvas inside it
 */
public class Viewport extends Box implements IO.Loaded{

	static public final Dict.Prop<Scene> scene = new Dict.Prop<Scene>("scene").type().toCannon().doc("The Scene that's inside this viewport");

	public Viewport()
	{
		this.properties.putToList(Drawing.lateDrawers, this::drawNow);
		this.properties.put(scene, new Scene());

		this.properties.putToMap(FLineDrawing.frameDrawing, "__outline__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();
			if (selected) rect = rect.inset(-8f);
			else rect = rect.inset(-0.5f);

			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x + rect.w, rect.y);
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.lineTo(rect.x, rect.y + rect.h);
			f.lineTo(rect.x, rect.y);

			f.attributes.put(strokeColor, selected ? new Vec4(0, 0, 0, -1.0f) : new Vec4(0, 0, 0, 0.5f));

			f.attributes.put(thicken, new BasicStroke(selected ? 16 : 1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));

		this.properties.putToMap(FLineDrawing.frameDrawing, "__outlineFill__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();
			if (selected) rect = rect.inset(-8f);
			else rect = rect.inset(-0.5f);

			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x + rect.w, rect.y);
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.lineTo(rect.x, rect.y + rect.h);
			f.lineTo(rect.x, rect.y);

			f.attributes.put(stroked, false);
			f.attributes.put(fillColor, new Vec4(0,0,0,0.2f));
			f.attributes.put(filled, true);

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));
	}


	@Override
	public void loaded() {
		Drawing d = this.first(Drawing.drawing)
				      .get();

	}

	protected void drawNow(Drawing context)
	{
		try(Util.ExceptionlessAutoCloasable s = GraphicsContext.stateTracker.save())
		{

			Rect f = this.properties.get(Box.frame);

			Drawing d = this.first(Drawing.drawing)
					      .get();

			Vec2 tl = d.drawingSystemToWindowSystem(new Vec2(f.x, f.y));
			Vec2 bl = d.drawingSystemToWindowSystem(new Vec2(f.x+f.w, f.y+f.h));

			FieldBoxWindow window = this.first(Boxes.window)
							    .get();
			int h = window.getHeight();
			int w = window.getWidth();

			int[] v = new int[]{(int)tl.x, (int)(h-bl.y), (int)(bl.x-tl.x+2), (int)(bl.y-tl.y+2)};

			Log.log("viewport", "Drawing! "+tl+" "+bl);

			GraphicsContext.stateTracker.scissor.set(v);

			Scene scene = this.properties.get(Viewport.scene);
			scene.updateAll();

//			glClearColor(0,0,0,0.2f);
//			glClear(GL_COLOR_BUFFER_BIT);

			Log.log("viewport", "complete "+s);
		}
		Log.log("viewport", "working? ");

	}

	@Override
	public String toString() {
		return super.toString()+"/viewport";
	}
}
