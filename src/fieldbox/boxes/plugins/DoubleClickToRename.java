package fieldbox.boxes.plugins;

import fieldbox.boxes.Box;
import fieldbox.boxes.FrameManipulation;
import fieldbox.boxes.Intersects;
import fieldbox.boxes.Mouse;
import fieldcef.plugins.GlassBrowser;

import java.util.Collections;
import java.util.stream.Stream;

/**
 * Created by marc on 7/11/16.
 */
public class DoubleClickToRename extends Box {

	public DoubleClickToRename(Box root) {
		this.properties.putToMap(Mouse.onDoubleClick, "__doubleClickToRename__", e -> {
			if (e.after.keyboardState.isSuperDown()) {

				Box o = Intersects.startAt(e.after, root);
				if (o==root) return;
				FrameManipulation.setSelectionTo(root, Collections.singleton(o));


				this.find(GlassBrowser.glassBrowser, both()).findFirst().ifPresent(x -> {
					x.joinCommands("Rename Box");
				});

			}
		});
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}
}
