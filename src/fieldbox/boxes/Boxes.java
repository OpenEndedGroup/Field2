package fieldbox.boxes;

import field.app.RunLoop;
import field.graphics.Scene;
import field.utility.Dict;
import fieldbox.boxes.plugins.Planes;
import fieldbox.execution.InverseDebugMapping;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fielded.Commands;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Acts as a container for the root of the Box graph. Declares Dict.Prop that pertain to the graph as a whole. Provides an entry point for the Runloop into the graph for animation updates.
 */
public class Boxes {

	static public final Dict.Prop<Box> root = new Dict.Prop<>("root").type()
									 .toCannon()
									 .doc("the root of the box graph");
	static public final Dict.Prop<FieldBoxWindow> window = new Dict.Prop<>("window").doc("the FieldBoxWindow that this graph is currently in");
	static public final Dict.Prop<Map<String, Supplier<Boolean>>> insideRunLoop = new Dict.Prop<>("_insideRunLoop");
	static public final Dict.Prop<Boolean> dontSave = new Dict.Prop<>("dontSave").type()
										     .toCannon()
										     .doc("set this to true to cause this box to not be saved with the box graph");
	static public final Dict.Prop<String> tag = new Dict.Prop<>("tag").type()
									  .toCannon()
									  .doc("Facilitates box creation in an idempotent style'internal name' for boxes. <code>new _('tag', {})</code> will either create a box with tag <code>'tag'</code> (as a child of <code>_</code> or return an existing box with this tag ");

	static {
		IO.persist(tag);
	}


	Box origin;

	public Boxes() {
		origin = new Box();
		origin.properties.put(root, origin);
		origin.properties.put(Box.name, "<<root>>");

		origin.properties.put(Planes.plane, "__root__");


		InverseDebugMapping.defaultRoot = origin;

		// set these up so that they will appear in autocomplete

		origin.properties.getOrConstruct(Callbacks.onDelete);
		origin.properties.getOrConstruct(Callbacks.onLoad);
		origin.properties.getOrConstruct(Callbacks.onFrameChanged);

		origin.properties.getOrConstruct(Commands.command);
		origin.properties.getOrConstruct(Commands.commandDoc);
	}

	protected Set<Box> population = Collections.emptySet();


	protected Scene.Perform updater = new Scene.Perform() {
		@Override
		public boolean perform(int pass) {

			origin.forEach(
   // turns out, this is something on the order of 20mb a second of garbage at full framerate.
//			origin.find(insideRunLoop, origin.both())
//			      .forEach(
				    y -> {

					    Map<String, Supplier<Boolean>> x = y.properties.get(insideRunLoop);
					    if (x == null || x.size() == 0) return;

					    Iterator<Map.Entry<String, Supplier<Boolean>>> r = x.entrySet()
												.iterator();
					    while (r.hasNext()) {
						    Map.Entry<String, Supplier<Boolean>> n = r.next();
						    try {
							    if (n.getKey()
								 .startsWith("main.")) try {
								    if (!n.getValue()
									  .get()) r.remove();
							    } catch (Throwable t) {
								    t.printStackTrace();
							    }
						    } catch (Throwable t) {
							    t.printStackTrace();
							    try {
								    r.remove();
							    } catch (Throwable tt) {
							    }
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
		RunLoop.main.getLoop()
			    .attach(updater);
	}

	public void stop() {
		RunLoop.main.getLoop()
			    .detach(updater);
	}

	public Box root() {
		return origin;
	}

	static public String debugPrintBoxGraph(Box origin) {
		String r = "";
		r += "children\n" + _debugPrintBoxGraphChildren(origin, 0);
		r += "\nparents\n" + _debugPrintBoxGraphParents(origin, 0);
		return r;
	}

	static private String indent(int x) {
		String q = "";
		while (q.length() < x) q = ":" + q;
		return q;
	}

	static private String _debugPrintBoxGraphChildren(Box root, int indent) {
		return indent(indent) + root + "\n" + root.children()
							  .stream()
							  .map(x -> _debugPrintBoxGraphChildren(x, indent + 2))
							  .reduce((a, b) -> a + "\n" + b)
							  .orElseGet(() -> "--");
	}

	static private String _debugPrintBoxGraphParents(Box root, int indent) {
		return indent(indent) + root + "\n" + root.children()
							  .stream()
							  .map(x -> _debugPrintBoxGraphParents(x, indent + 2))
							  .reduce((a, b) -> a + "\n" + b)
							  .orElseGet(() -> "--");
	}

}
