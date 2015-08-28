package field.graphics;

import field.linalg.Vec3;
import field.utility.Dict;

import java.nio.FloatBuffer;

/**
 * Used for deconstructing and reconstructing FLines
 */
public class TaggedVec3 extends Vec3 {

	final char tag;
	protected Dict d;

	public TaggedVec3(char t) {
		this.tag = t;
	}

	public TaggedVec3(char t, double x, double y, double z) {
		super(x, y, z);
		this.tag = t;
	}

	public TaggedVec3(char t, Vec3 to) {
		super(to);
		this.tag = t;
	}

	public TaggedVec3(char t, FloatBuffer f) {
		super(f);
		this.tag = t;
	}

	public TaggedVec3(char t, FloatBuffer f, int index) {
		super(index, f);
		this.tag = t;
	}

	@Override
	public TaggedVec3 duplicate() {
		return new TaggedVec3(tag, this);
	}

	public Dict d() {
		return d;
	}
}
