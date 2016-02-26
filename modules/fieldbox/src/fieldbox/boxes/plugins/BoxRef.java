package fieldbox.boxes.plugins;

import field.utility.Util;
import fieldbox.boxes.Box;
import fieldbox.io.IO;

import java.util.Optional;

/**
 * A serializeable, cached, immutable reference to a box.
 */
public class BoxRef {

	protected transient Box ref;
	public String uuid;

	public BoxRef(Box b) {
		ref = b;
		uuid = b.properties.get(IO.id);
	}

	public BoxRef() {

	}

	public Box get(Box from) {
		if (ref != null) {
			if (ref.disconnected) return ref=null;
			return ref;
		}
		Optional<Box> b = from.breadthFirst(from.both()).filter(x -> Util.safeEq(x.properties.get(IO.id), uuid)).findFirst();
		if (b.isPresent()) ref = b.get();
		return ref;
	}

}
