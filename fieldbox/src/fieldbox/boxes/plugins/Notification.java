package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.linalg.Vec4;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.FrameDrawer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marc on 5/16/14.
 */
public class Notification {

	static public void notify(String text, Box from, int dur)
	{
		from.properties.putToMap(FrameDrawer.frameDrawing, "__notificationText__", FrameDrawer.expires(box -> {

			Drawing d = from.first(Drawing.drawing).get();
			Rect view = d.getCurrentViewBounds(from);
			FLine f = new FLine();
			f.moveTo(view.x+view.w/2, view.y+view.h/2);
			f.nodes.get(0).attributes.put(FrameDrawer.text, text);
			f.nodes.get(0).attributes.put(FrameDrawer.textScale, 4);
			f.attributes.put(FrameDrawer.color, new Vec4(1,1,1,1f));
			f.attributes.put(FrameDrawer.hasText, true);
			f.attributes.put(FrameDrawer.layer, "glass");

			return f;
		}, (int)(dur),0.05f));
		from.properties.putToMap(FrameDrawer.frameDrawing, "__notificationMask__", FrameDrawer.expires(box -> {

			Drawing d = from.first(Drawing.drawing).get();
			Rect view = d.getCurrentViewBounds(from);
			FLine f = new FLine();
			int w = 20;
			int h = 60;
			f.rect(view.x-w, view.y+view.h/2-h, view.w+w*2, h+10);
			f.attributes.put(FrameDrawer.color, new Vec4(0,0,0,-0.8f));
			f.attributes.put(FrameDrawer.layer, "glass");
			f.attributes.put(FrameDrawer.filled, true);
			return f;
		}, dur, 0.05f));
	}

}
