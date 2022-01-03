package field.graphics;

import java.nio.Buffer;

/**
 * Created by marc on 2/19/16.
 */
public interface JPEGLoader {
	void decompress(String filename, Buffer dest, int width, int height);

	int[] dimensions(String filename);

	Texture.TextureSpecification loadTexture(int unit, boolean mips, String filename);
}
