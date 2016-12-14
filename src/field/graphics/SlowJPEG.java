package field.graphics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * We've been holding onto FastJPEG and it's our only Created by marc on 2/19/16.
 */
public class SlowJPEG implements JPEGLoader{

	@Override
	public void decompress(String filename, Buffer dest, int width, int height) {
		byte[] f = bytes(filename);

		if(f==null)
		{
			throw new IllegalArgumentException(" failed to load :"+filename);
		}
		// 4 or 3?
		dest.rewind();
		((ByteBuffer)dest).put(f);
		dest.rewind();

	}

	protected byte[] bytes(String filename) {
		try {
			BufferedImage originalImage = ImageIO.read(new File(filename));

			byte[] pixels = ((DataBufferByte) originalImage.getRaster().getDataBuffer()).getData();

			return pixels;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void decompressFlipped(String filename, Buffer dest, int width, int height)
	{
		throw new IllegalArgumentException("not implemented");
	}

	public void decompressGrey(String filename, Buffer dest, int width, int height)
	{
		throw new IllegalArgumentException("not implemented");
	}


	public void compress(String filename, Buffer dest, int width, int height)
	{
		throw new IllegalArgumentException("not implemented");
	}



	@Override
	public int[] dimensions(String filename) {
		try {
			BufferedImage originalImage = ImageIO.read(new File(filename));
			return new int[]{originalImage.getWidth(), originalImage.getHeight()};
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Texture.TextureSpecification loadTexture(int unit, boolean mips, String filename) {

		int[] d = dimensions(filename);
		if (d == null) return null;

		ByteBuffer b = ByteBuffer.allocateDirect(3 * d[0] * d[1]);
		decompress(filename, b, d[0], d[1]);
		b.rewind();

		return Texture.TextureSpecification.byte3(unit, d[0], d[1], b, mips);
	}

}
