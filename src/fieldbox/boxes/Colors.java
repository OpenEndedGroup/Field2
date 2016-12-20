package fieldbox.boxes;

import field.linalg.Vec3;
import field.linalg.Vec4;

/**
 * Default colors for the "theme" of Field's canvas
 */
public class Colors {
	static public Vec3 backgroundColor = new Vec3(0.23f*0.8f, 0.24f*0.8f, 0.245f*0.8f);

	static public Vec4 boxBackground1Selected = new Vec4(1,1,1,0.6f);
	static public Vec4 boxBackground2Selected = new Vec4(0.8f,0.8f,0.8f,0.6f);
	static public Vec4 boxBackground1 = new Vec4(1f,1f,1f,0.5f);
	static public Vec4 boxBackground2= new Vec4(0.8f,0.8f,0.8f,0.5f);

	static public Vec3 executionColor = new Vec3(0.2f, 0.5f, 0.3f);

	static public Vec4 boxTextBackground1=new Vec4(0.55, 0.55, 0.55, 0.8);
	static public Vec4 boxTextBackground2=new Vec4(0.685,0.685,0.685,0.8);

	static public Vec4 boxStroke = new Vec4(0, 0, 0, 0.25f);
	static public Vec4 boxStrokeSelected = new Vec4(0, 0, 0, -0.25f);

	public static Vec4 statusBarBackground = new Vec4(0.32f*0.09f, 0.28f*0.09f, 0.1f*0.09f, 0.2f);


	public static Vec4 focusRing1 = new Vec4(0.1f, 0.6, 1.0, 0.2f);
	public static Vec4 focusRing2 = new Vec4(0.1f, 0.6, 1.0, 0.4f);
}
