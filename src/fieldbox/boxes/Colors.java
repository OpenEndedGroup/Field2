package fieldbox.boxes;

import field.linalg.Vec3;
import field.linalg.Vec4;

/**
 * Default colors for the "theme" of Field's canvas
 */
public class Colors {
	static public Vec3 backgroundColor = new Vec3(0.23f*0.8f*1.48, 0.24f*0.8f*1.48, 0.245f*0.8f*1.48);

	//40 55 60


	static public Vec4 boxStrokeSelected = new Vec4(0.,0.2,0.4, -0.5);
	static public Vec4 boxBackground1 = new Vec4(1, 0.95, 0.9, 0.75);
	static public Vec4 boxBackground2 = new Vec4(1, 0.95, 0.9, 0.5);
	static public Vec4 boxTextBackground1 =new Vec4(0.9, 0.95, 1, 0.95);
	static public Vec4 boxTextBackground2 =new Vec4(0.9, 0.95, 1, 0.95);
	static public Vec4 boxStroke = new Vec4(0, 0, 0.3, 0.25f);

//
//	static public Vec4 boxBackground1Selected = new Vec4(0.85f,0.9f,1f,0.6f);
//	static public Vec4 boxBackground2Selected = new Vec4(0.8f,0.82f,0.84f,0.6f);
//	static public Vec4 boxBackground1 = new Vec4(0.85f,0.9f,1f,0.5f);
//	static public Vec4 boxBackground2= new Vec4(0.8f,0.82f,0.84f,0.5f);

	static public Vec3 executionColor = new Vec3(0.2f, 0.5f, 0.3f);

//	static public Vec4 boxTextBackground1=new Vec4(0.55, 0.55, 0.55, 0.8);
//	static public Vec4 boxTextBackground2=new Vec4(0.685,0.685,0.685,0.8);

//	static public Vec4 boxStrokeSelected = new Vec4(0, 0, 0, -0.25f);
//
	public static Vec4 statusBarBackground = new Vec4(0f, 0.05f, 0.1f, 0.2f);

	public static Vec4 focusRing1 = new Vec4(0.1f, 0.6, 1.0, 0.2f);
	public static Vec4 focusRing2 = new Vec4(0.1f, 0.6, 1.0, 0.4f);
}
