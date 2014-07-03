package fieldbox.boxes;

import field.graphics.RunLoop;
import field.graphics.Scene;
import field.utility.Dict;
import fieldbox.ui.FieldBoxWindow;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Acts as a container for the root of the Box graph. Declares Dict.Prop that pertain to the graph as a whole. Provides
 * an entry point for the Runloop into the graph for animation updates.
 */
public class Boxes {

	static public final Dict.Prop<Box> root = new Dict.Prop<>("root").type().toCannon().doc("the root of the box graph");
	static public final Dict.Prop<FieldBoxWindow> window = new Dict.Prop<>("window").doc("the FieldBoxWindow that this graph is currently in");
	static public final Dict.Prop<Map<String, Supplier<Boolean>>> insideRunLoop = new Dict.Prop<>("_insideRunLoop");
	static public final Dict.Prop<Boolean> dontSave = new Dict.Prop<>("dontSave").type().toCannon().doc("set this to true to cause this box to not be saved with the box graph");

	Box origin;

	public Boxes() {
		origin = new Box();
		origin.properties.put(root, origin);
		origin.properties.put(Box.name, "<<root>>");
	}

	protected Set<Box> population = Collections.emptySet();

	protected Scene.Perform updater = new Scene.Perform() {
		@Override
		public boolean perform(int pass) {
			origin.find(insideRunLoop, origin.both()).forEach(x -> {

				Iterator<Map.Entry<String, Supplier<Boolean>>> r = x.entrySet().iterator();
				while (r.hasNext()) {
					Map.Entry<String, Supplier<Boolean>> n = r.next();
					if (n.getKey().startsWith("main."))
					try {
						if (!n.getValue().get()) r.remove();
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
		RunLoop.main.getLoop().connect(updater);
	}

	public void stop() {
		RunLoop.main.getLoop().disconnect(updater);
	}

	public Box root() {
		return origin;
	}

}
