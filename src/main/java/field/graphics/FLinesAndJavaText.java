package field.graphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;

/**
 * utils for FLines and text outlines
 */
public class FLinesAndJavaText {

	private final FontRenderContext frc;

	public FLinesAndJavaText()
	{
//		GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment()
//							.getDefaultScreenDevice();
//		BufferedImage image = dev.getDefaultConfiguration()
//					 .createCompatibleImage(1, 1, Transparency.TRANSLUCENT);
//		Graphics2D g2 = (Graphics2D) image.getGraphics();
//
//		frc= g2.getFontRenderContext();

		var bi = new BufferedImage(1,1,BufferedImage.TYPE_3BYTE_BGR);
		frc =GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(bi).getFontRenderContext();
		
	}

	public FLine flineForText(String text, String font, int size, int style)
	{
		Font f = new Font(font, style, size);

		return FLinesAndJavaShapes.javaShapeToFLine(f.createGlyphVector(frc, text).getOutline());
	}


}
