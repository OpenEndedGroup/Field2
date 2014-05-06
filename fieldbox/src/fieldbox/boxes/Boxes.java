package fieldbox.boxes;

import field.graphics.Scene;
import field.utility.Dict;
import field.graphics.RunLoop;
import fieldbox.ui.FieldBoxWindow;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created by marc on 3/21/14.
 */
public class Boxes {

	static public final Dict.Prop<Box> root = new Dict.Prop<>("root");
	static public final Dict.Prop<FieldBoxWindow> window = new Dict.Prop<>("window");
	static public final Dict.Prop<Map<String, Supplier<Boolean>>> insideRunLoop = new Dict.Prop<>("_insideRunLoop");

	Box origin;

	public Boxes() {
		origin = new Box();
		origin.properties.put(root, origin);
		origin.properties.put(Box.name, "<<root>>");
	}

	protected Scene.Perform updateor = new Scene.Perform() {
		@Override
		public boolean perform(int pass) {
			origin.find(insideRunLoop, origin.both()).forEach(x -> {
				Iterator<Map.Entry<String, Supplier<Boolean>>> r = x.entrySet().iterator();
				while (r.hasNext()) {
					try {
						if (!r.next().getValue().get()) r.remove();
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			});
			return true;
		}

		@Override
		public int[] getPasses() {
			return new int[]{10};
		}
	};

	public void start() {
		RunLoop.main.getLoop().connect(updateor);
	}

	public void stop() {
		RunLoop.main.getLoop().disconnect(updateor);
	}

	public Box root() {
		return origin;
	}

	static public Box root(Box from) {
		Optional<Box> o = from.find(root, from.upwards()).findFirst();
		return o.orElseThrow(() -> new IllegalArgumentException(" rootless box? "));
	}
}
