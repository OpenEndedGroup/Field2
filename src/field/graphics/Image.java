package field.graphics;

import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL42.glBindImageTexture;

/**
 * This is an OpenGL Image class (which is not to be confused with an OpenGL Texture).
 * <p>
 * An OpenGL Image is a very recent addition to OpenGL. Unlike Textures, Images can be writeable in random access ways from shaders.
 */
public class Image implements Scene.Perform {

	private final Texture texture;

	public Image(Texture texture) {
		this.texture = texture;
	}

	@Override
	public boolean perform(int pass) {
		if (pass == -1) return perform();
		return true;
	}

	private boolean perform() {
		texture.perform(-1);
		glBindImageTexture(texture.specification.unit, ((Texture.State) GraphicsContext
			    .get(texture)).name, 0, false, 0, GL_READ_WRITE, texture.specification.format);
		return true;
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1};
	}
}
