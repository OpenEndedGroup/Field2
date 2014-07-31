package fieldprocessing;

import field.utility.Log;
import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.data.Table;
import processing.data.XML;
import processing.event.MouseEvent;
import processing.opengl.PGL;
import processing.opengl.PShader;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Map;

/**
 * A delegate for (Field/Processing)Applet. This allows us to de-static, de-protected and accessorify this class, which gets us better integration
 * with Nashorn.
 */
public class FieldProcessingAppletDelgate {

	public final  int X = 0;
	public final  int Y = 1;
	public final  int Z = 2;


	// renderers known to processing.core

	public final  String JAVA2D = "processing.core.PGraphicsJava2D";
	public final  String P2D = "processing.opengl.PGraphics2D";
	public final  String P3D = "processing.opengl.PGraphics3D";
	public final  String OPENGL = P3D;

	public final  String PDF = "processing.pdf.PGraphicsPDF";
	public final  String DXF = "processing.dxf.RawDXF";

	// platform IDs for PApplet.platform

	public final  int OTHER = 0;
	public final  int WINDOWS = 1;
	public final  int MACOSX = 2;
	public final  int LINUX = 3;

	public final  String[] platformNames = {"other", "windows", "macosx", "linux"};


	public final  float EPSILON = 0.0001f;


	// max/min values for numbers

	/**
	 * Same as Float.MAX_VALUE, but included for parity with MIN_VALUE, and to avoid teaching methods on the first day.
	 */
	public final  float MAX_FLOAT = Float.MAX_VALUE;
	/**
	 * Note that Float.MIN_VALUE is the smallest <EM>positive</EM> value for a floating point number, not actually the minimum (negative) value
	 * for a float. This constant equals 0xFF7FFFFF, the smallest (farthest negative) value a float can have before it hits NaN.
	 */
	public final  float MIN_FLOAT = -Float.MAX_VALUE;
	/**
	 * Largest possible (positive) integer value
	 */
	public final  int MAX_INT = Integer.MAX_VALUE;
	/**
	 * Smallest possible (negative) integer value
	 */
	public final  int MIN_INT = Integer.MIN_VALUE;

	// shapes

	public final  int VERTEX = 0;
	public final  int BEZIER_VERTEX = 1;
	public final  int QUADRATIC_VERTEX = 2;
	public final  int CURVE_VERTEX = 3;
	public final  int BREAK = 4;

	@Deprecated
	public final  int QUAD_BEZIER_VERTEX = 2;  // should not have been exposed

	// useful goodness

	/**
	 * ( begin auto-generated from PI.xml )
	 * <p>
	 * PI is a mathematical constant with the value 3.14159265358979323846. It is the ratio of the circumference of a circle to its diameter. It
	 * is useful in combination with the trigonometric functions <b>sin()</b> and <b>cos()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref constants
	 * @see PConstants#TWO_PI
	 * @see PConstants#TAU
	 * @see PConstants#HALF_PI
	 * @see PConstants#QUARTER_PI
	 */
	public final  float PI = (float) Math.PI;
	/**
	 * ( begin auto-generated from HALF_PI.xml )
	 * <p>
	 * HALF_PI is a mathematical constant with the value 1.57079632679489661923. It is half the ratio of the circumference of a circle to its
	 * diameter. It is useful in combination with the trigonometric functions <b>sin()</b> and <b>cos()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref constants
	 * @see PConstants#PI
	 * @see PConstants#TWO_PI
	 * @see PConstants#TAU
	 * @see PConstants#QUARTER_PI
	 */
	public final  float HALF_PI = (float) (Math.PI / 2.0);
	public final  float THIRD_PI = (float) (Math.PI / 3.0);
	/**
	 * ( begin auto-generated from QUARTER_PI.xml )
	 * <p>
	 * QUARTER_PI is a mathematical constant with the value 0.7853982. It is one quarter the ratio of the circumference of a circle to its
	 * diameter. It is useful in combination with the trigonometric functions <b>sin()</b> and <b>cos()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref constants
	 * @see PConstants#PI
	 * @see PConstants#TWO_PI
	 * @see PConstants#TAU
	 * @see PConstants#HALF_PI
	 */
	public final  float QUARTER_PI = (float) (Math.PI / 4.0);
	/**
	 * ( begin auto-generated from TWO_PI.xml )
	 * <p>
	 * TWO_PI is a mathematical constant with the value 6.28318530717958647693. It is twice the ratio of the circumference of a circle to its
	 * diameter. It is useful in combination with the trigonometric functions <b>sin()</b> and <b>cos()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref constants
	 * @see PConstants#PI
	 * @see PConstants#TAU
	 * @see PConstants#HALF_PI
	 * @see PConstants#QUARTER_PI
	 */
	public final  float TWO_PI = (float) (2.0 * Math.PI);
	/**
	 * ( begin auto-generated from TAU.xml )
	 * <p>
	 * TAU is an alias for TWO_PI, a mathematical constant with the value 6.28318530717958647693. It is twice the ratio of the circumference of a
	 * circle to its diameter. It is useful in combination with the trigonometric functions <b>sin()</b> and <b>cos()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref constants
	 * @see PConstants#PI
	 * @see PConstants#TWO_PI
	 * @see PConstants#HALF_PI
	 * @see PConstants#QUARTER_PI
	 */
	public final  float TAU = (float) (2.0 * Math.PI);

	public final  float DEG_TO_RAD = PI / 180.0f;
	public final  float RAD_TO_DEG = 180.0f / PI;


	// angle modes

	//public final  int RADIANS = 0;
	//public final  int DEGREES = 1;


	// used by split, all the standard whitespace chars
	// (also includes unicode nbsp, that little bostage)

	public final  String WHITESPACE = " \t\n\r\f\u00A0";


	// for colors and/or images

	public final  int RGB = 1;  // image & color
	public final  int ARGB = 2;  // image
	public final  int HSB = 3;  // color
	public final  int ALPHA = 4;  // image
//  public final  int CMYK  = 5;  // image & color (someday)


	// image file types

	public final  int TIFF = 0;
	public final  int TARGA = 1;
	public final  int JPEG = 2;
	public final  int GIF = 3;


	// filter/convert types

	public final  int BLUR = 11;
	public final  int GRAY = 12;
	public final  int INVERT = 13;
	public final  int OPAQUE = 14;
	public final  int POSTERIZE = 15;
	public final  int THRESHOLD = 16;
	public final  int ERODE = 17;
	public final  int DILATE = 18;


	// blend mode keyword definitions
	// @see processing.core.PImage#blendColor(int,int,int)

	public final  int REPLACE = 0;
	public final  int BLEND = 1 << 0;
	public final  int ADD = 1 << 1;
	public final  int SUBTRACT = 1 << 2;
	public final  int LIGHTEST = 1 << 3;
	public final  int DARKEST = 1 << 4;
	public final  int DIFFERENCE = 1 << 5;
	public final  int EXCLUSION = 1 << 6;
	public final  int MULTIPLY = 1 << 7;
	public final  int SCREEN = 1 << 8;
	public final  int OVERLAY = 1 << 9;
	public final  int HARD_LIGHT = 1 << 10;
	public final  int SOFT_LIGHT = 1 << 11;
	public final  int DODGE = 1 << 12;
	public final  int BURN = 1 << 13;

	// for messages

	public final  int CHATTER = 0;
	public final  int COMPLAINT = 1;
	public final  int PROBLEM = 2;


	// types of transformation matrices

	public final  int PROJECTION = 0;
	public final  int MODELVIEW = 1;

	// types of projection matrices

	public final  int CUSTOM = 0; // user-specified fanciness
	public final  int ORTHOGRAPHIC = 2; // 2D isometric projection
	public final  int PERSPECTIVE = 3; // perspective matrix


	// shapes

	// the low four bits set the variety,
	// higher bits set the specific shape type

	public final  int GROUP = 0;   // createShape()

	public final  int POINT = 2;   // primitive
	public final  int POINTS = 3;   // vertices

	public final  int LINE = 4;   // primitive
	public final  int LINES = 5;   // beginShape(), createShape()
	public final  int LINE_STRIP = 50;  // beginShape()
	public final  int LINE_LOOP = 51;

	public final  int TRIANGLE = 8;   // primitive
	public final  int TRIANGLES = 9;   // vertices
	public final  int TRIANGLE_STRIP = 10;  // vertices
	public final  int TRIANGLE_FAN = 11;  // vertices

	public final  int QUAD = 16;  // primitive
	public final  int QUADS = 17;  // vertices
	public final  int QUAD_STRIP = 18;  // vertices

	public final  int POLYGON = 20;  // in the end, probably cannot
	public final  int PATH = 21;  // separate these two

	public final  int RECT = 30;  // primitive
	public final  int ELLIPSE = 31;  // primitive
	public final  int ARC = 32;  // primitive

	public final  int SPHERE = 40;  // primitive
	public final  int BOX = 41;  // primitive

//  public final  int POINT_SPRITES = 52;
//  public final  int NON_STROKED_SHAPE = 60;
//  public final  int STROKED_SHAPE     = 61;


	// shape closing modes

	public final  int OPEN = 1;
	public final  int CLOSE = 2;


	// shape drawing modes

	/**
	 * Draw mode convention to use (x, y) to (width, height)
	 */
	public final  int CORNER = 0;
	/**
	 * Draw mode convention to use (x1, y1) to (x2, y2) coordinates
	 */
	public final  int CORNERS = 1;
	/**
	 * Draw mode from the center, and using the radius
	 */
	public final  int RADIUS = 2;
	/**
	 * Draw from the center, using second pair of values as the diameter. Formerly called CENTER_DIAMETER in alpha releases.
	 */
	public final  int CENTER = 3;
	/**
	 * Synonym for the CENTER constant. Draw from the center, using second pair of values as the diameter.
	 */
	public final  int DIAMETER = 3;


	// arc drawing modes

	//public final  int OPEN = 1;  // shared
	public final  int CHORD = 2;
	public final  int PIE = 3;


	// vertically alignment modes for text

	/**
	 * Default vertical alignment for text placement
	 */
	public final  int BASELINE = 0;
	/**
	 * Align text to the top
	 */
	public final  int TOP = 101;
	/**
	 * Align text from the bottom, using the baseline.
	 */
	public final  int BOTTOM = 102;


	// uv texture orientation modes

	/**
	 * texture coordinates in 0..1 range
	 */
	public final  int NORMAL = 1;
	/**
	 * texture coordinates based on image width/height
	 */
	public final  int IMAGE = 2;


	// texture wrapping modes

	/**
	 * textures are clamped to their edges
	 */
	public final  int CLAMP = 0;
	/**
	 * textures wrap around when uv values go outside 0..1 range
	 */
	public final  int REPEAT = 1;


	// text placement modes

	/**
	 * textMode(MODEL) is the default, meaning that characters will be affected by transformations like any other shapes.
	 * <p>
	 * Changed value in 0093 to not interfere with LEFT, CENTER, and RIGHT.
	 */
	public final  int MODEL = 4;

	/**
	 * textMode(SHAPE) draws text using the the glyph outlines of individual characters rather than as textures. If the outlines are not
	 * available, then textMode(SHAPE) will be ignored and textMode(MODEL) will be used instead. For this reason, be sure to call textMode()
	 * <EM>after</EM> calling textFont().
	 * <p>
	 * Currently, textMode(SHAPE) is only supported by OPENGL mode. It also requires Java 1.2 or higher (OPENGL requires 1.4 anyway)
	 */
	public final  int SHAPE = 5;


	// text alignment modes
	// are inherited from LEFT, CENTER, RIGHT

	// stroke modes

	public final  int SQUARE = 1 << 0;  // called 'butt' in the svg spec
	public final  int ROUND = 1 << 1;
	public final  int PROJECT = 1 << 2;  // called 'square' in the svg spec
	public final  int MITER = 1 << 3;
	public final  int BEVEL = 1 << 5;


	// lighting

	public final  int AMBIENT = 0;
	public final  int DIRECTIONAL = 1;
	//public final  int POINT  = 2;  // shared with shape feature
	public final  int SPOT = 3;


	// key constants

	// only including the most-used of these guys
	// if people need more esoteric keys, they can learn about
	// the esoteric java KeyEvent api and of virtual keys

	// both key and keyCode will equal these values
	// for 0125, these were changed to 'char' values, because they
	// can be upgraded to ints automatically by Java, but having them
	// as ints prevented split(blah, TAB) from working
	public final  char BACKSPACE = 8;
	public final  char TAB = 9;
	public final  char ENTER = 10;
	public final  char RETURN = 13;
	public final  char ESC = 27;
	public final  char DELETE = 127;

	// i.e. if ((key == CODED) && (keyCode == UP))
	public final  int CODED = 0xffff;

	// key will be CODED and keyCode will be this value
	public final  int UP = KeyEvent.VK_UP;
	public final  int DOWN = KeyEvent.VK_DOWN;
	public final  int LEFT = KeyEvent.VK_LEFT;
	public final  int RIGHT = KeyEvent.VK_RIGHT;

	// key will be CODED and keyCode will be this value
	public final  int ALT = KeyEvent.VK_ALT;
	public final  int CONTROL = KeyEvent.VK_CONTROL;
	public final  int SHIFT = KeyEvent.VK_SHIFT;


	// orientations (only used on Android, ignored on desktop)

	/**
	 * Screen orientation constant for portrait (the hamburger way).
	 */
	public final  int PORTRAIT = 1;
	/**
	 * Screen orientation constant for landscape (the hot dog way).
	 */
	public final  int LANDSCAPE = 2;


	// cursor types

	public final  int ARROW = Cursor.DEFAULT_CURSOR;
	public final  int CROSS = Cursor.CROSSHAIR_CURSOR;
	public final  int HAND = Cursor.HAND_CURSOR;
	public final  int MOVE = Cursor.MOVE_CURSOR;
	public final  int TEXT = Cursor.TEXT_CURSOR;
	public final  int WAIT = Cursor.WAIT_CURSOR;


	// hints - hint values are positive for the alternate version,
	// negative of the same value returns to the normal/default state

	@Deprecated
	public final  int ENABLE_NATIVE_FONTS = 1;
	@Deprecated
	public final  int DISABLE_NATIVE_FONTS = -1;

	public final  int DISABLE_DEPTH_TEST = 2;
	public final  int ENABLE_DEPTH_TEST = -2;

	public final  int ENABLE_DEPTH_SORT = 3;
	public final  int DISABLE_DEPTH_SORT = -3;

	public final  int DISABLE_OPENGL_ERRORS = 4;
	public final  int ENABLE_OPENGL_ERRORS = -4;

	public final  int DISABLE_DEPTH_MASK = 5;
	public final  int ENABLE_DEPTH_MASK = -5;

	public final  int DISABLE_OPTIMIZED_STROKE = 6;
	public final  int ENABLE_OPTIMIZED_STROKE = -6;

	public final  int ENABLE_STROKE_PERSPECTIVE = 7;
	public final  int DISABLE_STROKE_PERSPECTIVE = -7;

	public final  int DISABLE_TEXTURE_MIPMAPS = 8;
	public final  int ENABLE_TEXTURE_MIPMAPS = -8;

	public final  int ENABLE_STROKE_PURE = 9;
	public final  int DISABLE_STROKE_PURE = -9;

	public final  int ENABLE_RETINA_PIXELS = 10;
	public final  int DISABLE_RETINA_PIXELS = -10;

	public final  int HINT_COUNT = 11;

	public final  FieldProcessingApplet applet;

	protected FieldProcessingAppletDelgate(FieldProcessingApplet applet) {
		this.applet = applet;
		this.onMouseClicked = applet.onMouseClicked;
		this.onMouseMoved = applet.onMouseMoved;
		this.onMousePressed = applet.onMousePressed;
		this.onMouseDragged = applet.onMouseDragged;
		this.onMouseEntered = applet.onMouseEntered;
		this.onMouseExited = applet.onMouseExited;
		this.onMouseReleased = applet.onMouseReleased;
		this.onMouseWheel = applet.onMouseWheel;
		this.onMouseWheelMoved = applet.onMouseWheelMoved;
		this.onKeyPressed = applet.onKeyPressed;
		this.onKeyReleased = applet.onKeyReleased;
	}

	/**
	 * A Map containing handlers to be called when the mouse is clicked with (Applet, MouseEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMouseClicked;

	/**
	 * A Map containing handlers to be called when the mouse is moved with (Applet, MouseEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMouseMoved;

	/**
	 * A Map containing handlers to be called when the mouse is pressed with (Applet, MouseEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMousePressed;

	/**
	 * A Map containing handlers to be called when the mouse is dragged with (Applet, MouseEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMouseDragged;

	/**
	 * A Map containing handlers to be called when the mouse enters with (Applet, MouseEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMouseEntered;

	/**
	 * A Map containing handlers to be called when the mouse exits with (Applet, MouseEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMouseExited;

	/**
	 * A Map containing handlers to be called when the mouse is released with (Applet, MouseEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMouseReleased;

	/**
	 * A Map containing handlers to be called when the mouse wheel is pressed with (Applet, MouseEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMouseWheel;

	/**
	 * A Map containing handlers to be called when the mouse wheel is moved with (Applet, MouseWheelEvent)
	 */
	public final  Map<String, Processing.MouseHandler> onMouseWheelMoved;

	/**
	 * A Map containing handlers to be called when the mouse wheel is moved with (Applet, MouseWheelEvent)
	 */
	public final  Map<String, Processing.KeyHandler> onKeyPressed;

	/**
	 * A Map containing handlers to be called when the mouse wheel is moved with (Applet, MouseWheelEvent)
	 */
	public final  Map<String, Processing.KeyHandler> onKeyReleased;

	public int sketchQuality() {
		return applet.sketchQuality();
	}

	public int sketchWidth() {
		return applet.sketchWidth();
	}

	public int sketchHeight() {
		return applet.sketchHeight();
	}

	public String sketchRenderer() {
		return applet.sketchRenderer();
	}

	public boolean sketchFullScreen() {
		return applet.sketchFullScreen();
	}

	public void orientation(int which) {
		applet.orientation(which);
	}

	/**
	 * Called by the browser or __applet viewer to inform this __applet that it should start its execution. It is called after the init method and
	 * each time the __applet is revisited in a Web page.
	 * <p>
	 * Called explicitly via the first call to PApplet.paint(), because PAppletGL needs to have a usable screen before getting things rolling.
	 */
	public void start() {
		applet.start();
	}

	/**
	 * Called by the browser or __applet viewer to inform this __applet that it should stop its execution.
	 * <p>
	 * Unfortunately, there are no guarantees from the Java spec when or if stop() will be called (i.e. on browser quit, or when moving between
	 * web pages), and it's not always called.
	 */
	public void stop() {
		applet.stop();
	}

	/**
	 * Sketch has been paused. Called when switching tabs in a browser or swapping to a different application on Android. Also called just before
	 * quitting. Use to safely disable things like serial, sound, or sensors.
	 */
	public void pause() {
		applet.pause();
	}

	/**
	 * Sketch has resumed. Called when switching tabs in a browser or swapping to this application on Android. Also called on startup. Use this to
	 * safely disable things like serial, sound, or sensors.
	 */
	public void resume() {
		applet.resume();
	}

	/**
	 * Called by the browser or __applet viewer to inform this __applet that it is being reclaimed and that it should destroy any resources that it
	 * has allocated.
	 * <p>
	 * destroy() supposedly gets called as the __applet viewer is shutting down the __applet. stop() is called first, and then destroy() to really get
	 * rid of things. no guarantees on when they're run (on browser quit, or when moving between pages), though.
	 */
	public void destroy() {
		applet.destroy();
	}

	/**
	 * ( begin auto-generated from size.xml )
	 * <p>
	 * Defines the dimension of the display window in units of pixels. The <b>size()</b> function must be the first line in <b>setup()</b>. If
	 * <b>size()</b> is not used, the default size of the window is 100x100 pixels. The system variables <b>width</b> and <b>height</b> are set by
	 * the parameters passed to this function.<br /> <br /> Do not use variables as the parameters to <b>size()</b> function, because it will
	 * cause problems when exporting your sketch. When variables are used, the dimensions of your sketch cannot be determined during export.
	 * Instead, employ numeric values in the <b>size()</b> statement, and then use the built-in <b>width</b> and <b>height</b> variables inside
	 * your program when the dimensions of the display window are needed.<br /> <br /> The <b>size()</b> function can only be used once inside a
	 * sketch, and cannot be used for resizing.<br/> <br/> <b>renderer</b> parameter selects which rendering engine to use. For example, if you
	 * will be drawing 3D shapes, use <b>P3D</b>, if you want to export images from a program as a PDF file use <b>PDF</b>. A brief description of
	 * the three primary renderers follows:<br /> <br /> <b>P2D</b> (Processing 2D) - The default renderer that supports two dimensional
	 * drawing.<br /> <br /> <b>P3D</b> (Processing 3D) - 3D graphics renderer that makes use of OpenGL-compatible graphics hardware.<br /> <br />
	 * <b>PDF</b> - The PDF renderer draws 2D graphics directly to an Acrobat PDF file. This produces excellent results when you need vector
	 * shapes for high resolution output or printing. You must first use Import Library &rarr; PDF to make use of the library. More information
	 * can be found in the PDF library reference.<br /> <br /> The P3D renderer doesn't support <b>strokeCap()</b> or <b>strokeJoin()</b>, which
	 * can lead to ugly results when using <b>strokeWeight()</b>. (<a href="http://code.google.com/p/processing/issues/detail?id=123">Issue
	 * 123</a>) <br /> <br /> The maximum width and height is limited by your operating system, and is usually the width and height of your actual
	 * screen. On some machines it may simply be the number of pixels on your current screen, meaning that a screen of 800x600 could support
	 * <b>size(1600, 300)</b>, since it's the same number of pixels. This varies widely so you'll have to try different rendering modes and sizes
	 * until you get what you're looking for. If you need something larger, use <b>createGraphics</b> to create a non-visible drawing surface.<br
	 * /> <br /> Again, the <b>size()</b> function must be the first line of the code (or first item inside setup). Any code that appears before
	 * the <b>size()</b> command may run more than once, which can lead to confusing results.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> If using Java 1.3 or later, this will default to using PGraphics2, the Java2D-based renderer. If using Java 1.1, or if
	 * PGraphics2 is not available, then PGraphics will be used. To set your own renderer, use the other version of the size() method that takes a
	 * renderer as its last parameter.
	 * <p>
	 * If called once a renderer has already been set, this will use the previous renderer and simply resize it.
	 *
	 * @param w width of the display window in units of pixels
	 * @param h height of the display window in units of pixels
	 * @webref environment
	 * @see PApplet#width
	 * @see PApplet#height
	 */
	public void size(int w, int h) {
		applet.size(w, h);
	}

	/**
	 * @param w
	 * @param h
	 * @param renderer Either P2D, P3D, or PDF
	 */
	public void size(int w, int h, String renderer) {
		applet.size(w, h, renderer);
	}

	/**
	 * @param w
	 * @param h
	 * @param renderer
	 * @param path
	 * @nowebref
	 */
	public void size(int w, int h, String renderer, String path) {
		applet.size(w, h, renderer, path);
	}

	public PGraphics createGraphics(int w, int h) {
		return applet.createGraphics(w, h);
	}

	/**
	 * ( begin auto-generated from createGraphics.xml )
	 * <p>
	 * Creates and returns a new <b>PGraphics</b> object of the types P2D or P3D. Use this class if you need to draw into an off-screen graphics
	 * buffer. The PDF renderer requires the filename parameter. The DXF renderer should not be used with <b>createGraphics()</b>, it's only built
	 * for use with <b>beginRaw()</b> and <b>endRaw()</b>.<br /> <br /> It's important to call any drawing functions between <b>beginDraw()</b>
	 * and <b>endDraw()</b> statements. This is also true for any functions that affect drawing, such as <b>smooth()</b> or
	 * <b>colorMode()</b>.<br/> <br/> the main drawing surface which is completely opaque, surfaces created with <b>createGraphics()</b> can have
	 * transparency. This makes it possible to draw into a graphics and maintain the alpha channel. By using <b>save()</b> to write a PNG or TGA
	 * file, the transparency of the graphics object will be honored. Note that transparency levels are binary: pixels are either complete opaque
	 * or transparent. For the time being, this means that text characters will be opaque blocks. This will be fixed in a future release (<a
	 * href="http://code.google.com/p/processing/issues/detail?id=80">Issue 80</a>).
	 * <p>
	 * ( end auto-generated ) <h3>Advanced</h3> Create an offscreen PGraphics object for drawing. This can be used for bitmap or vector images
	 * drawing or rendering. <UL> <LI>Do not use "new PGraphicsXxxx()", use this method. This method ensures that internal variables are set up
	 * properly that tie the new graphics context back to its parent PApplet. <LI>The basic way to create bitmap images is to use the <A
	 * HREF="http://processing.org/reference/saveFrame_.html">saveFrame()</A> function. <LI>If you want to create a really large scene and write
	 * that, first make sure that you've allocated a lot of memory in the Preferences. <LI>If you want to create images that are larger than the
	 * screen, you should create your own PGraphics object, draw to that, and use <A HREF="http://processing.org/reference/save_.html">save()</A>.
	 * <PRE>
	 * <p>
	 * PGraphics big;
	 * <p>
	 * void setup() { big = createGraphics(3000, 3000);
	 * <p>
	 * big.beginDraw(); big.background(128); big.line(20, 1800, 1800, 900); // etc.. big.endDraw();
	 * <p>
	 * // make sure the file is written to the sketch folder big.save("big.tif"); }
	 * <p>
	 * </PRE> <LI>It's important to always wrap drawing to createGraphics() with beginDraw() and endDraw() (beginFrame() and endFrame() prior to
	 * revision 0115). The reason is that the renderer needs to know when drawing has stopped, so that it can update itself internally. This also
	 * handles calling the defaults() method, for people familiar with that. <LI>With Processing 0115 and later, it's possible to write images in
	 * formats other than the default .tga and .tiff. The exact formats and background information can be found in the developer's reference for
	 * <A HREF="http://dev.processing.org/reference/core/javadoc/processing/core/PImage.html#save(java.lang.String)">PImage.save()</A>. </UL>
	 *
	 * @param w        width in pixels
	 * @param h        height in pixels
	 * @param renderer Either P2D, P3D, or PDF
	 * @webref rendering
	 * @see processing.core.PGraphics#PGraphics
	 */
	public PGraphics createGraphics(int w, int h, String renderer) {
		return applet.createGraphics(w, h, renderer);
	}

	/**
	 * Create an offscreen graphics surface for drawing, in this case for a renderer that writes to a file (such as PDF or DXF).
	 *
	 * @param w
	 * @param h
	 * @param renderer
	 * @param path     the name of the file (can be an absolute or relative path)
	 */
	public PGraphics createGraphics(int w, int h, String renderer, String path) {
		return applet.createGraphics(w, h, renderer, path);
	}

	/**
	 * ( begin auto-generated from createImage.xml )
	 * <p>
	 * Creates a new PImage (the datatype for storing images). This provides a fresh buffer of pixels to play with. Set the size of the buffer
	 * with the <b>width</b> and <b>height</b> parameters. The <b>format</b> parameter defines how the pixels are stored. See the PImage reference
	 * for more information. <br/> <br/> Be sure to include all three parameters, specifying only the width and height (but no format) will
	 * produce a strange error. <br/> <br/> Advanced users please note that createImage() should be used instead of the syntax <tt>new
	 * PImage()</tt>.
	 * <p>
	 * ( end auto-generated ) <h3>Advanced</h3> Preferred method of creating new PImage objects, ensures that a reference to the parent PApplet is
	 * included, which makes save() work without needing an absolute path.
	 *
	 * @param w      width in pixels
	 * @param h      height in pixels
	 * @param format Either RGB, ARGB, ALPHA (grayscale alpha channel)
	 * @webref image
	 * @see PImage
	 * @see processing.core.PGraphics
	 */
	public PImage createImage(int w, int h, int format) {
		return applet.createImage(w, h, format);
	}

	/**
	 * ( begin auto-generated from loop.xml )
	 * <p>
	 * Causes Processing to continuously execute the code within <b>draw()</b>. If <b>noLoop()</b> is called, the code in <b>draw()</b> stops
	 * executing.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref structure
	 * @usage web_application
	 * @see PApplet#noLoop()
	 * @see PApplet#redraw()
	 * @see PApplet#draw()
	 */
	public void loop() {
		applet.loop();
	}

	/**
	 * ( begin auto-generated from noLoop.xml )
	 * <p>
	 * Stops Processing from continuously executing the code within <b>draw()</b>. If <b>loop()</b> is called, the code in <b>draw()</b> begin to
	 * run continuously again. If using <b>noLoop()</b> in <b>setup()</b>, it should be the last line inside the block. <br/> <br/> When
	 * <b>noLoop()</b> is used, it's not possible to manipulate or access the screen inside event handling functions such as <b>mousePressed()</b>
	 * or <b>keyPressed()</b>. Instead, use those functions to call <b>redraw()</b> or <b>loop()</b>, which will run <b>draw()</b>, which can
	 * update the screen properly. This means that when noLoop() has been called, no drawing can happen, and functions like saveFrame() or
	 * loadPixels() may not be used. <br/> <br/> Note that if the sketch is resized, <b>redraw()</b> will be called to update the sketch, even
	 * after <b>noLoop()</b> has been specified. Otherwise, the sketch would enter an odd state until <b>loop()</b> was called.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref structure
	 * @usage web_application
	 * @see PApplet#loop()
	 * @see PApplet#redraw()
	 * @see PApplet#draw()
	 */
	public void noLoop() {
		applet.noLoop();
	}

	/**
	 * ( begin auto-generated from millis.xml )
	 * <p>
	 * Returns the number of milliseconds (thousandths of a second) since starting an __applet. This information is often used for timing animation
	 * sequences.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3>
	 * <p>
	 * This is a function, rather than a variable, because it may change multiple times per frame.
	 *
	 * @webref input:time_date
	 * @see PApplet#second()
	 * @see PApplet#minute()
	 * @see PApplet#hour()
	 * @see PApplet#day()
	 * @see PApplet#month()
	 * @see PApplet#year()
	 */
	public int millis() {
		return applet.millis();
	}

	/**
	 * ( begin auto-generated from second.xml )
	 * <p>
	 * Processing communicates with the clock on your computer. The <b>second()</b> function returns the current second as a value from 0 - 59.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:time_date
	 * @see PApplet#millis()
	 * @see PApplet#minute()
	 * @see PApplet#hour()
	 * @see PApplet#day()
	 * @see PApplet#month()
	 * @see PApplet#year()
	 */
	public int second() {
		return PApplet.second();
	}

	/**
	 * ( begin auto-generated from minute.xml )
	 * <p>
	 * Processing communicates with the clock on your computer. The <b>minute()</b> function returns the current minute as a value from 0 - 59.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:time_date
	 * @see processing.core.PApplet#millis()
	 * @see processing.core.PApplet#second()
	 * @see processing.core.PApplet#hour()
	 * @see processing.core.PApplet#day()
	 * @see processing.core.PApplet#month()
	 * @see processing.core.PApplet#year()
	 */
	public int minute() {
		return PApplet.minute();
	}

	/**
	 * ( begin auto-generated from hour.xml )
	 * <p>
	 * Processing communicates with the clock on your computer. The <b>hour()</b> function returns the current hour as a value from 0 - 23.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:time_date
	 * @see processing.core.PApplet#millis()
	 * @see processing.core.PApplet#second()
	 * @see processing.core.PApplet#minute()
	 * @see processing.core.PApplet#day()
	 * @see processing.core.PApplet#month()
	 * @see processing.core.PApplet#year()
	 */
	public int hour() {
		return PApplet.hour();
	}

	/**
	 * ( begin auto-generated from day.xml )
	 * <p>
	 * Processing communicates with the clock on your computer. The <b>day()</b> function returns the current day as a value from 1 - 31.
	 * <p>
	 * ( end auto-generated ) <h3>Advanced</h3> Get the current day of the month (1 through 31).
	 * <p>
	 * If you're looking for the day of the week (M-F or whatever) or day of the year (1..365) then use java's Calendar.get()
	 *
	 * @webref input:time_date
	 * @see processing.core.PApplet#millis()
	 * @see processing.core.PApplet#second()
	 * @see processing.core.PApplet#minute()
	 * @see processing.core.PApplet#hour()
	 * @see processing.core.PApplet#month()
	 * @see processing.core.PApplet#year()
	 */
	public int day() {
		return PApplet.day();
	}

	/**
	 * ( begin auto-generated from month.xml )
	 * <p>
	 * Processing communicates with the clock on your computer. The <b>month()</b> function returns the current month as a value from 1 - 12.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:time_date
	 * @see processing.core.PApplet#millis()
	 * @see processing.core.PApplet#second()
	 * @see processing.core.PApplet#minute()
	 * @see processing.core.PApplet#hour()
	 * @see processing.core.PApplet#day()
	 * @see processing.core.PApplet#year()
	 */
	public int month() {
		return PApplet.month();
	}

	/**
	 * ( begin auto-generated from year.xml )
	 * <p>
	 * Processing communicates with the clock on your computer. The <b>year()</b> function returns the current year as an integer (2003, 2004,
	 * 2005, etc).
	 * <p>
	 * ( end auto-generated ) The <b>year()</b> function returns the current year as an integer (2003, 2004, 2005, etc).
	 *
	 * @webref input:time_date
	 * @see processing.core.PApplet#millis()
	 * @see processing.core.PApplet#second()
	 * @see processing.core.PApplet#minute()
	 * @see processing.core.PApplet#hour()
	 * @see processing.core.PApplet#day()
	 * @see processing.core.PApplet#month()
	 */
	public int year() {
		return PApplet.year();
	}

	/**
	 * The delay() function causes the program to halt for a specified time. Delay times are specified in thousandths of a second. For example,
	 * running delay(3000) will stop the program for three seconds and delay(500) will stop the program for a half-second.
	 * <p>
	 * The screen only updates when the end of draw() is reached, so delay() cannot be used to slow down drawing. For instance, you cannot use
	 * delay() to control the timing of an animation.
	 * <p>
	 * The delay() function should only be used for pausing scripts (i.e. a script that needs to pause a few seconds before attempting a download,
	 * or a sketch that needs to wait a few milliseconds before reading from the serial port).
	 *
	 * @param napTime
	 */
	public void delay(int napTime) {
		applet.delay(napTime);
	}

	/**
	 * ( begin auto-generated from frameRate.xml )
	 * <p>
	 * Specifies the number of frames to be displayed every second. If the processor is not fast enough to maintain the specified rate, it will
	 * not be achieved. For example, the function call <b>frameRate(30)</b> will attempt to refresh 30 times a second. It is recommended to set
	 * the frame rate within <b>setup()</b>. The default rate is 60 frames per second.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param fps number of desired frames per second
	 * @webref environment
	 * @see processing.core.PApplet#frameRate
	 * @see processing.core.PApplet#frameCount
	 * @see processing.core.PApplet#setup()
	 * @see processing.core.PApplet#draw()
	 * @see processing.core.PApplet#loop()
	 * @see processing.core.PApplet#noLoop()
	 * @see processing.core.PApplet#redraw()
	 */
	public void frameRate(float fps) {
		applet.frameRate(fps);
	}

	/**
	 * ( begin auto-generated from open.xml )
	 * <p>
	 * Attempts to open an application or file using your platform's launcher. The <b>file</b> parameter is a String specifying the file name and
	 * location. The location parameter must be a full path name, or the name of an executable in the system's PATH. In most cases, using a full
	 * path is the best option, rather than relying on the system PATH. Be sure to make the file executable before attempting to open it (chmod
	 * +x). <br/> <br/> The <b>args</b> parameter is a String or String array which is passed to the command line. If you have multiple
	 * parameters, e.g. an application and a document, or a command with multiple switches, use the version that takes a String array, and place
	 * each individual item in a separate element. <br/> <br/> If args is a String (not an array), then it can only be a single file or
	 * application with no parameters. It's not the same as executing that String using a shell. For instance, open("jikes -help") will not work
	 * properly. <br/> <br/> This function behaves differently on each platform. On Windows, the parameters are sent to the Windows shell via "cmd
	 * /c". On Mac OS X, the "open" command is used (type "man open" in Terminal.app for documentation). On Linux, it first tries gnome-open, then
	 * kde-open, but if neither are available, it sends the command to the shell without any alterations. <br/> <br/> For users familiar with
	 * Java, this is not quite the same as Runtime.exec(), because the launcher command is prepended. Instead, the <b>exec(String[])</b> function
	 * is a shortcut for Runtime.getRuntime.exec(String[]).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename name of the file
	 * @webref input:files
	 * @usage Application
	 */
	public void open(String filename) {
		PApplet.open(filename);
	}

	/**
	 * Launch a process using a platforms shell. This version uses an array to make it easier to deal with spaces in the individual elements.
	 * (This avoids the situation of trying to put single or double quotes around different bits).
	 *
	 * @param argv list of commands passed to the command line
	 */
	public Process open(String[] argv) {
		return PApplet.open(argv);
	}

	public Process exec(String[] argv) {
		return PApplet.exec(argv);
	}

	/**
	 * Function for an __applet/application to kill itself and display an error. Mostly this is here to be improved later.
	 *
	 * @param what
	 */
	public void die(String what) {
		applet.die(what);
	}

	/**
	 * Same as above but with an exception. Also needs work.
	 *
	 * @param what
	 * @param e
	 */
	public void die(String what, Exception e) {
		applet.die(what, e);
	}

	/**
	 * ( begin auto-generated from exit.xml )
	 * <p>
	 * Quits/stops/exits the program. Programs without a <b>draw()</b> function exit automatically after the last line has run, but programs with
	 * <b>draw()</b> run continuously until the program is manually stopped or <b>exit()</b> is run.<br /> <br /> Rather than terminating
	 * immediately, <b>exit()</b> will cause the sketch to exit after <b>draw()</b> has completed (or after <b>setup()</b> completes if called
	 * during the <b>setup()</b> function).<br /> <br /> For Java programmers, this is <em>not</em> the same as System.exit(). Further,
	 * System.exit() should not be used because closing out an application while <b>draw()</b> is running may cause a crash (particularly with
	 * P3D).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref structure
	 */
	public void exit() {
		applet.exit();
	}

	/**
	 * Called to dispose of resources and shut down the sketch. Destroys the thread, dispose the renderer,and notify listeners.
	 * <p>
	 * Not to be called or overriden by users. If called multiple times, will only notify listeners once. Register a dispose listener instead.
	 */
	public void dispose() {
		applet.dispose();
	}

	/**
	 * Call a method in the current class based on its name.
	 * <p>
	 * Note that the function being called must be public. Inside the PDE, 'public' is automatically added, but when used without the
	 * preprocessor, (like from Eclipse) you'll have to do it yourself.
	 *
	 * @param name
	 */
	public void method(String name) {
		applet.method(name);
	}

	/**
	 * Launch a new thread and call the specified function from that new thread. This is a very simple way to do a thread without needing to get
	 * into classes, runnables, etc.
	 * <p>
	 * Note that the function being called must be public. Inside the PDE, 'public' is automatically added, but when used without the
	 * preprocessor, (like from Eclipse) you'll have to do it yourself.
	 *
	 * @param name
	 */
	public void thread(String name) {
		applet.thread(name);
	}

	/**
	 * ( begin auto-generated from save.xml )
	 * <p>
	 * Saves an image from the display window. Images are saved in TIFF, TARGA, JPEG, and PNG format depending on the extension within the
	 * <b>filename</b> parameter. For example, "image.tif" will have a TIFF image and "image.png" will save a PNG image. If no extension is
	 * included in the filename, the image will save in TIFF format and <b>.tif</b> will be added to the name. These files are saved to the
	 * sketch's folder, which may be opened by selecting "Show sketch folder" from the "Sketch" menu. It is not possible to use <b>save()</b>
	 * while running the program in a web browser. <br/> images saved from the main drawing window will be opaque. To save images without a
	 * background, use <b>createGraphics()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename any sequence of letters and numbers
	 * @webref output:image
	 * @see processing.core.PApplet#saveFrame()
	 * @see processing.core.PApplet#createGraphics(int, int, String)
	 */
	public void save(String filename) {
		applet.save(filename);
	}

	/**
	 */
	public void saveFrame() {
		applet.saveFrame();
	}

	/**
	 * ( begin auto-generated from saveFrame.xml )
	 * <p>
	 * Saves a numbered sequence of images, one image each time the function is run. To save an image that is identical to the display window, run
	 * the function at the end of <b>draw()</b> or within mouse and key events such as <b>mousePressed()</b> and <b>keyPressed()</b>. If
	 * <b>saveFrame()</b> is called without parameters, it will save the files as screen-0000.tif, screen-0001.tif, etc. It is possible to specify
	 * the name of the sequence with the <b>filename</b> parameter and make the choice of saving TIFF, TARGA, PNG, or JPEG files with the
	 * <b>ext</b> parameter. These image sequences can be loaded into programs such as Apple's QuickTime software and made into movies. These
	 * files are saved to the sketch's folder, which may be opened by selecting "Show sketch folder" from the "Sketch" menu.<br /> <br /> It is
	 * not possible to use saveXxxxx() functions inside a web browser unless the sketch is <a href="http://wiki.processing.org/w/Sign_an_Applet">signed
	 * __applet</A>. To save a file back to a server, see the <a href="http://wiki.processing.org/w/Saving_files_to_a_web-server">save to web</A>
	 * code snippet on the Processing Wiki.<br/> <br/ > All images saved from the main drawing window will be opaque. To save images without a
	 * background, use <b>createGraphics()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename any sequence of letters or numbers that ends with either ".tif", ".tga", ".jpg", or ".png"
	 * @webref output:image
	 * @see processing.core.PApplet#save(String)
	 * @see processing.core.PApplet#createGraphics(int, int, String, String)
	 * @see processing.core.PApplet#frameCount
	 */
	public void saveFrame(String filename) {
		applet.saveFrame(filename);
	}

	/**
	 * Check a string for #### signs to see if the frame number should be inserted. Used for functions like saveFrame() and beginRecord() to
	 * replace the # marks with the frame number. If only one # is used, it will be ignored, under the assumption that it's probably not intended
	 * to be the frame number.
	 *
	 * @param what
	 */
	public String insertFrame(String what) {
		return applet.insertFrame(what);
	}

	/**
	 * Set the cursor type
	 *
	 * @param kind either ARROW, CROSS, HAND, MOVE, TEXT, or WAIT
	 */
	public void cursor(int kind) {
		applet.cursor(kind);
	}

	/**
	 * Replace the cursor with the specified PImage. The x- and y- coordinate of the center will be the center of the image.
	 *
	 * @param img
	 */
	public void cursor(PImage img) {
		applet.cursor(img);
	}

	/**
	 * ( begin auto-generated from cursor.xml )
	 * <p>
	 * Sets the cursor to a predefined symbol, an image, or makes it visible if already hidden. If you are trying to set an image as the cursor,
	 * it is recommended to make the size 16x16 or 32x32 pixels. It is not possible to load an image as the cursor if you are exporting your
	 * program for the Web and not all MODES work with all Web browsers. The values for parameters <b>x</b> and <b>y</b> must be less than the
	 * dimensions of the image. <br /> <br /> Setting or hiding the cursor generally does not work with "Present" mode (when running
	 * full-screen).
	 * <p>
	 * ( end auto-generated ) <h3>Advanced</h3> Set a custom cursor to an image with a specific hotspot. Only works with JDK 1.2 and later.
	 * Currently seems to be broken on Java 1.4 for Mac OS X
	 * <p>
	 * Based on code contributed by Amit Pitaru, plus additional code to handle Java versions via reflection by Jonathan Feinberg. Reflection
	 * removed for release 0128 and later.
	 *
	 * @param img any variable of type PImage
	 * @param x   the horizontal active spot of the cursor
	 * @param y   the vertical active spot of the cursor
	 * @webref environment
	 * @see processing.core.PApplet#noCursor()
	 */
	public void cursor(PImage img, int x, int y) {
		applet.cursor(img, x, y);
	}

	/**
	 * Show the cursor after noCursor() was called. Notice that the program remembers the last set cursor type
	 */
	public void cursor() {
		applet.cursor();
	}

	/**
	 * ( begin auto-generated from noCursor.xml )
	 * <p>
	 * Hides the cursor from view. Will not work when running the program in a web browser or when running in full screen (Present) mode.
	 * <p>
	 * ( end auto-generated ) <h3>Advanced</h3> Hide the cursor by creating a transparent image and using it as a custom cursor.
	 *
	 * @webref environment
	 * @usage Application
	 * @see processing.core.PApplet#cursor()
	 */
	public void noCursor() {
		applet.noCursor();
	}

	/**
	 * ( begin auto-generated from print.xml )
	 * <p>
	 * Writes to the console area of the Processing environment. This is often helpful for looking at the data a program is producing. The
	 * companion function <b>println()</b> works like <b>print()</b>, but creates a new line of text for each call to the function. Individual
	 * elements can be separated with quotes ("") and joined with the addition operator (+).<br /> <br /> Beginning with release 0125, to print
	 * the contents of an array, use println(). There's no sensible way to do a <b>print()</b> of an array, because there are too many
	 * possibilities for how to separate the data (spaces, commas, etc). If you want to print an array as a single line, use <b>join()</b>. With
	 * <b>join()</b>, you can choose any delimiter you like and <b>print()</b> the result.<br /> <br /> Using <b>print()</b> on an object will
	 * output <b>null</b>, a memory location that may look like "@10be08," or the result of the <b>toString()</b> method from the object that's
	 * being printed. Advanced users who want more useful output when calling <b>print()</b> on their own classes can add a <b>toString()</b>
	 * method to the class that returns a String.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param what data to print to console
	 * @webref output:text_area
	 * @usage IDE
	 * @see processing.core.PApplet#println()
	 * @see processing.core.PApplet#printArray(Object)
	 * @see processing.core.PApplet#join(String[], char)
	 */
	public void print(byte what) {
		PApplet.print(what);
	}

	public void print(boolean what) {
		PApplet.print(what);
	}

	public void print(char what) {
		PApplet.print(what);
	}

	public void print(int what) {
		PApplet.print(what);
	}

	public void print(long what) {
		PApplet.print(what);
	}

	public void print(float what) {
		PApplet.print(what);
	}

	public void print(double what) {
		PApplet.print(what);
	}

	public void print(String what) {
		PApplet.print(what);
	}

	/**
	 * @param variables list of data, separated by commas
	 */
	public void print(Object... variables) {
		PApplet.print(variables);
	}

	/**
	 * ( begin auto-generated from println.xml )
	 * <p>
	 * Writes to the text area of the Processing environment's console. This is often helpful for looking at the data a program is producing. Each
	 * call to this function creates a new line of output. Individual elements can be separated with quotes ("") and joined with the string
	 * concatenation operator (+). See <b>print()</b> for more about what to expect in the output. <br/><br/> <b>println()</b> on an array (by
	 * itself) will write the contents of the array to the console. This is often helpful for looking at the data a program is producing. A new
	 * line is put between each element of the array. This function can only print one dimensional arrays. For arrays with higher dimensions, the
	 * result will be closer to that of <b>print()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref output:text_area
	 * @usage IDE
	 * @see processing.core.PApplet#print(byte)
	 * @see processing.core.PApplet#printArray(Object)
	 */
	public void println() {
		PApplet.println();
	}

	/**
	 * @param what data to print to console
	 */
	public void println(byte what) {
		PApplet.println(what);
	}

	public void println(boolean what) {
		PApplet.println(what);
	}

	public void println(char what) {
		PApplet.println(what);
	}

	public void println(int what) {
		PApplet.println(what);
	}

	public void println(long what) {
		PApplet.println(what);
	}

	public void println(float what) {
		PApplet.println(what);
	}

	public void println(double what) {
		PApplet.println(what);
	}

	public void println(String what) {
		PApplet.println(what);
	}

	/**
	 * @param variables list of data, separated by commas
	 */
	public void println(Object... variables) {
		PApplet.println(variables);
	}

	/**
	 * For arrays, use printArray() instead. This function causes a warning because the new print(Object...) and println(Object...) functions
	 * can't be reliably bound by the compiler.
	 *
	 * @param what
	 */
	public void println(Object what) {
		PApplet.println(what);
	}

	/**
	 * ( begin auto-generated from printArray.xml )
	 * <p>
	 * To come...
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param what one-dimensional array
	 * @webref output:text_area
	 * @usage IDE
	 * @see processing.core.PApplet#print(byte)
	 * @see processing.core.PApplet#println()
	 */
	public void printArray(Object what) {
		PApplet.printArray(what);
	}

	public void debug(String msg) {
		PApplet.debug(msg);
	}

	/**
	 * ( begin auto-generated from abs.xml )
	 * <p>
	 * Calculates the absolute value (magnitude) of a number. The absolute value of a number is always positive.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n number to compute
	 * @webref math:calculation
	 */
	public float abs(float n) {
		return PApplet.abs(n);
	}

	public int abs(int n) {
		return PApplet.abs(n);
	}

	/**
	 * ( begin auto-generated from sq.xml )
	 * <p>
	 * Squares a number (multiplies a number by itself). The result is always a positive number, as multiplying two negative numbers always yields
	 * a positive result. For example, -1 * -1 = 1.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n number to square
	 * @webref math:calculation
	 * @see processing.core.PApplet#sqrt(float)
	 */
	public float sq(float n) {
		return PApplet.sq(n);
	}

	/**
	 * ( begin auto-generated from sqrt.xml )
	 * <p>
	 * Calculates the square root of a number. The square root of a number is always positive, even though there may be a valid negative root. The
	 * square root <b>s</b> of number <b>a</b> is such that <b>s*s = a</b>. It is the opposite of squaring.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n non-negative number
	 * @webref math:calculation
	 * @see processing.core.PApplet#pow(float, float)
	 * @see processing.core.PApplet#sq(float)
	 */
	public float sqrt(float n) {
		return PApplet.sqrt(n);
	}

	/**
	 * ( begin auto-generated from log.xml )
	 * <p>
	 * Calculates the natural logarithm (the base-<i>e</i> logarithm) of a number. This function expects the values greater than 0.0.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n number greater than 0.0
	 * @webref math:calculation
	 */
	public float log(float n) {
		return PApplet.log(n);
	}

	/**
	 * ( begin auto-generated from exp.xml )
	 * <p>
	 * Returns Euler's number <i>e</i> (2.71828...) raised to the power of the <b>value</b> parameter.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n exponent to raise
	 * @webref math:calculation
	 */
	public float exp(float n) {
		return PApplet.exp(n);
	}

	/**
	 * ( begin auto-generated from pow.xml )
	 * <p>
	 * Facilitates exponential expressions. The <b>pow()</b> function is an efficient way of multiplying numbers by themselves (or their
	 * reciprocal) in large quantities. For example, <b>pow(3, 5)</b> is equivalent to the expression 3*3*3*3*3 and <b>pow(3, -5)</b> is
	 * equivalent to 1 / 3*3*3*3*3.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n base of the exponential expression
	 * @param e power by which to raise the base
	 * @webref math:calculation
	 * @see processing.core.PApplet#sqrt(float)
	 */
	public float pow(float n, float e) {
		return PApplet.pow(n, e);
	}

	/**
	 * ( begin auto-generated from max.xml )
	 * <p>
	 * Determines the largest value in a sequence of numbers.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param a first number to compare
	 * @param b second number to compare
	 * @webref math:calculation
	 * @see processing.core.PApplet#min(float, float, float)
	 */
	public int max(int a, int b) {
		return PApplet.max(a, b);
	}

	public float max(float a, float b) {
		return PApplet.max(a, b);
	}

	/**
	 * @param a
	 * @param b
	 * @param c third number to compare
	 */
	public int max(int a, int b, int c) {
		return PApplet.max(a, b, c);
	}

	public float max(float a, float b, float c) {
		return PApplet.max(a, b, c);
	}

	/**
	 * @param list array of numbers to compare
	 */
	public int max(int[] list) {
		return PApplet.max(list);
	}

	public float max(float[] list) {
		return PApplet.max(list);
	}

	/**
	 * Find the maximum value in an array. Throws an ArrayIndexOutOfBoundsException if the array is length 0.
	 *
	 * @param a
	 * @param b
	 * @return The maximum value
	 */
	public int min(int a, int b) {
		return PApplet.min(a, b);
	}

	public float min(float a, float b) {
		return PApplet.min(a, b);
	}

	public int min(int a, int b, int c) {
		return PApplet.min(a, b, c);
	}

	/**
	 * ( begin auto-generated from min.xml )
	 * <p>
	 * Determines the smallest value in a sequence of numbers.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param a first number
	 * @param b second number
	 * @param c third number
	 * @webref math:calculation
	 * @see processing.core.PApplet#max(float, float, float)
	 */
	public float min(float a, float b, float c) {
		return PApplet.min(a, b, c);
	}

	/**
	 * @param list array of numbers to compare
	 */
	public int min(int[] list) {
		return PApplet.min(list);
	}

	public float min(float[] list) {
		return PApplet.min(list);
	}

	/**
	 * Find the minimum value in an array. Throws an ArrayIndexOutOfBoundsException if the array is length 0.
	 *
	 * @param amt
	 * @param low
	 * @param high @return The minimum value
	 */
	public int constrain(int amt, int low, int high) {
		return PApplet.constrain(amt, low, high);
	}

	/**
	 * ( begin auto-generated from constrain.xml )
	 * <p>
	 * Constrains a value to not exceed a maximum and minimum value.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param amt  the value to constrain
	 * @param low  minimum limit
	 * @param high maximum limit
	 * @webref math:calculation
	 * @see processing.core.PApplet#max(float, float, float)
	 * @see processing.core.PApplet#min(float, float, float)
	 */
	public float constrain(float amt, float low, float high) {
		return PApplet.constrain(amt, low, high);
	}

	/**
	 * ( begin auto-generated from sin.xml )
	 * <p>
	 * Calculates the sine of an angle. This function expects the values of the <b>angle</b> parameter to be provided in radians (values from 0 to
	 * 6.28). Values are returned in the range -1 to 1.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle an angle in radians
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#cos(float)
	 * @see processing.core.PApplet#tan(float)
	 * @see processing.core.PApplet#radians(float)
	 */
	public float sin(float angle) {
		return PApplet.sin(angle);
	}

	/**
	 * ( begin auto-generated from cos.xml )
	 * <p>
	 * Calculates the cosine of an angle. This function expects the values of the <b>angle</b> parameter to be provided in radians (values from 0
	 * to PI*2). Values are returned in the range -1 to 1.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle an angle in radians
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#sin(float)
	 * @see processing.core.PApplet#tan(float)
	 * @see processing.core.PApplet#radians(float)
	 */
	public float cos(float angle) {
		return PApplet.cos(angle);
	}

	/**
	 * ( begin auto-generated from tan.xml )
	 * <p>
	 * Calculates the ratio of the sine and cosine of an angle. This function expects the values of the <b>angle</b> parameter to be provided in
	 * radians (values from 0 to PI*2). Values are returned in the range <b>infinity</b> to <b>-infinity</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle an angle in radians
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#cos(float)
	 * @see processing.core.PApplet#sin(float)
	 * @see processing.core.PApplet#radians(float)
	 */
	public float tan(float angle) {
		return PApplet.tan(angle);
	}

	/**
	 * ( begin auto-generated from asin.xml )
	 * <p>
	 * The inverse of <b>sin()</b>, returns the arc sine of a value. This function expects the values in the range of -1 to 1 and values are
	 * returned in the range <b>-PI/2</b> to <b>PI/2</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value the value whose arc sine is to be returned
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#sin(float)
	 * @see processing.core.PApplet#acos(float)
	 * @see processing.core.PApplet#atan(float)
	 */
	public float asin(float value) {
		return PApplet.asin(value);
	}

	/**
	 * ( begin auto-generated from acos.xml )
	 * <p>
	 * The inverse of <b>cos()</b>, returns the arc cosine of a value. This function expects the values in the range of -1 to 1 and values are
	 * returned in the range <b>0</b> to <b>PI (3.1415927)</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value the value whose arc cosine is to be returned
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#cos(float)
	 * @see processing.core.PApplet#asin(float)
	 * @see processing.core.PApplet#atan(float)
	 */
	public float acos(float value) {
		return PApplet.acos(value);
	}

	/**
	 * ( begin auto-generated from atan.xml )
	 * <p>
	 * The inverse of <b>tan()</b>, returns the arc tangent of a value. This function expects the values in the range of -Infinity to Infinity
	 * (exclusive) and values are returned in the range <b>-PI/2</b> to <b>PI/2 </b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value -Infinity to Infinity (exclusive)
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#tan(float)
	 * @see processing.core.PApplet#asin(float)
	 * @see processing.core.PApplet#acos(float)
	 */
	public float atan(float value) {
		return PApplet.atan(value);
	}

	/**
	 * ( begin auto-generated from atan2.xml )
	 * <p>
	 * Calculates the angle (in radians) from a specified point to the coordinate origin as measured from the positive x-axis. Values are returned
	 * as a <b>float</b> in the range from <b>PI</b> to <b>-PI</b>. The <b>atan2()</b> function is most often used for orienting geometry to the
	 * position of the cursor.  Note: The y-coordinate of the point is the first parameter and the x-coordinate is the second due the the
	 * structure of calculating the tangent.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param y y-coordinate of the point
	 * @param x x-coordinate of the point
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#tan(float)
	 */
	public float atan2(float y, float x) {
		return PApplet.atan2(y, x);
	}

	/**
	 * ( begin auto-generated from degrees.xml )
	 * <p>
	 * Converts a radian measurement to its corresponding value in degrees. Radians and degrees are two ways of measuring the same thing. There
	 * are 360 degrees in a circle and 2*PI radians in a circle. For example, 90&deg; = PI/2 = 1.5707964. All trigonometric functions in
	 * Processing require their parameters to be specified in radians.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param radians radian value to convert to degrees
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#radians(float)
	 */
	public float degrees(float radians) {
		return PApplet.degrees(radians);
	}

	/**
	 * ( begin auto-generated from radians.xml )
	 * <p>
	 * Converts a degree measurement to its corresponding value in radians. Radians and degrees are two ways of measuring the same thing. There
	 * are 360 degrees in a circle and 2*PI radians in a circle. For example, 90&deg; = PI/2 = 1.5707964. All trigonometric functions in
	 * Processing require their parameters to be specified in radians.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param degrees degree value to convert to radians
	 * @webref math:trigonometry
	 * @see processing.core.PApplet#degrees(float)
	 */
	public float radians(float degrees) {
		return PApplet.radians(degrees);
	}

	/**
	 * ( begin auto-generated from ceil.xml )
	 * <p>
	 * Calculates the closest int value that is greater than or equal to the value of the parameter. For example, <b>ceil(9.03)</b> returns the
	 * value 10.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n number to round up
	 * @webref math:calculation
	 * @see processing.core.PApplet#floor(float)
	 * @see processing.core.PApplet#round(float)
	 */
	public int ceil(float n) {
		return PApplet.ceil(n);
	}

	/**
	 * ( begin auto-generated from floor.xml )
	 * <p>
	 * Calculates the closest int value that is less than or equal to the value of the parameter.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n number to round down
	 * @webref math:calculation
	 * @see processing.core.PApplet#ceil(float)
	 * @see processing.core.PApplet#round(float)
	 */
	public int floor(float n) {
		return PApplet.floor(n);
	}

	/**
	 * ( begin auto-generated from round.xml )
	 * <p>
	 * Calculates the integer closest to the <b>value</b> parameter. For example, <b>round(9.2)</b> returns the value 9.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param n number to round
	 * @webref math:calculation
	 * @see processing.core.PApplet#floor(float)
	 * @see processing.core.PApplet#ceil(float)
	 */
	public int round(float n) {
		return PApplet.round(n);
	}

	public float mag(float a, float b) {
		return PApplet.mag(a, b);
	}

	/**
	 * ( begin auto-generated from mag.xml )
	 * <p>
	 * Calculates the magnitude (or length) of a vector. A vector is a direction in space commonly used in computer graphics and linear algebra.
	 * Because it has no "start" position, the magnitude of a vector can be thought of as the distance from coordinate (0,0) to its (x,y) value.
	 * Therefore, mag() is a shortcut for writing "dist(0, 0, x, y)".
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param a first value
	 * @param b second value
	 * @param c third value
	 * @webref math:calculation
	 * @see processing.core.PApplet#dist(float, float, float, float)
	 */
	public float mag(float a, float b, float c) {
		return PApplet.mag(a, b, c);
	}

	public float dist(float x1, float y1, float x2, float y2) {
		return PApplet.dist(x1, y1, x2, y2);
	}

	/**
	 * ( begin auto-generated from dist.xml )
	 * <p>
	 * Calculates the distance between two points.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x1 x-coordinate of the first point
	 * @param y1 y-coordinate of the first point
	 * @param z1 z-coordinate of the first point
	 * @param x2 x-coordinate of the second point
	 * @param y2 y-coordinate of the second point
	 * @param z2 z-coordinate of the second point
	 * @webref math:calculation
	 */
	public float dist(float x1, float y1, float z1, float x2, float y2, float z2) {
		return PApplet.dist(x1, y1, z1, x2, y2, z2);
	}

	/**
	 * ( begin auto-generated from lerp.xml )
	 * <p>
	 * Calculates a number between two numbers at a specific increment. The <b>amt</b> parameter is the amount to interpolate between the two
	 * values where 0.0 equal to the first point, 0.1 is very near the first point, 0.5 is half-way in between, etc. The lerp function is
	 * convenient for creating motion along a straight path and for drawing dotted lines.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param start first value
	 * @param stop  second value
	 * @param amt   float between 0.0 and 1.0
	 * @webref math:calculation
	 * @see processing.core.PGraphics#curvePoint(float, float, float, float, float)
	 * @see processing.core.PGraphics#bezierPoint(float, float, float, float, float)
	 * @see PVector#lerp(PVector, float)
	 * @see processing.core.PGraphics#lerpColor(int, int, float)
	 */
	public float lerp(float start, float stop, float amt) {
		return PApplet.lerp(start, stop, amt);
	}

	/**
	 * ( begin auto-generated from norm.xml )
	 * <p>
	 * Normalizes a number from another range into a value between 0 and 1. <br/> <br/> Identical to map(value, low, high, 0, 1); <br/> <br/>
	 * Numbers outside the range are not clamped to 0 and 1, because out-of-range values are often intentional and useful.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value the incoming value to be converted
	 * @param start lower bound of the value's current range
	 * @param stop  upper bound of the value's current range
	 * @webref math:calculation
	 * @see processing.core.PApplet#map(float, float, float, float, float)
	 * @see processing.core.PApplet#lerp(float, float, float)
	 */
	public float norm(float value, float start, float stop) {
		return PApplet.norm(value, start, stop);
	}

	/**
	 * ( begin auto-generated from map.xml )
	 * <p>
	 * Re-maps a number from one range to another. In the example above, the number '25' is converted from a value in the range 0..100 into a
	 * value that ranges from the left edge (0) to the right edge (width) of the screen. <br/> <br/> Numbers outside the range are not clamped to
	 * 0 and 1, because out-of-range values are often intentional and useful.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value  the incoming value to be converted
	 * @param start1 lower bound of the value's current range
	 * @param stop1  upper bound of the value's current range
	 * @param start2 lower bound of the value's target range
	 * @param stop2  upper bound of the value's target range
	 * @webref math:calculation
	 * @see processing.core.PApplet#norm(float, float, float)
	 * @see processing.core.PApplet#lerp(float, float, float)
	 */
	public float map(float value, float start1, float stop1, float start2, float stop2) {
		return PApplet.map(value, start1, stop1, start2, stop2);
	}

	/**
	 * @param high
	 */
	public float random(float high) {
		return applet.random(high);
	}

	/**
	 * ( begin auto-generated from randomGaussian.xml )
	 * <p>
	 * Returns a float from a random series of numbers having a mean of 0 and standard deviation of 1. Each time the <b>randomGaussian()</b>
	 * function is called, it returns a number fitting a Gaussian, or normal, distribution. There is theoretically no minimum or maximum value
	 * that <b>randomGaussian()</b> might return. Rather, there is just a very low probability that values far from the mean will be returned; and
	 * a higher probability that numbers near the mean will be returned.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref math:random
	 * @see processing.core.PApplet#random(float, float)
	 * @see processing.core.PApplet#noise(float, float, float)
	 */
	public float randomGaussian() {
		return applet.randomGaussian();
	}

	/**
	 * ( begin auto-generated from random.xml )
	 * <p>
	 * Generates random numbers. Each time the <b>random()</b> function is called, it returns an unexpected value within the specified range. If
	 * one parameter is passed to the function it will return a <b>float</b> between zero and the value of the <b>high</b> parameter. The function
	 * call <b>random(5)</b> returns values between 0 and 5 (starting at zero, up to but not including 5). If two parameters are passed, it will
	 * return a <b>float</b> with a value between the the parameters. The function call <b>random(-5, 10.2)</b> returns values starting at -5 up
	 * to (but not including) 10.2. To convert a floating-point random number to an integer, use the <b>int()</b> function.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param low  lower limit
	 * @param high upper limit
	 * @webref math:random
	 * @see processing.core.PApplet#randomSeed(long)
	 * @see processing.core.PApplet#noise(float, float, float)
	 */
	public float random(float low, float high) {
		return applet.random(low, high);
	}

	/**
	 * ( begin auto-generated from randomSeed.xml )
	 * <p>
	 * Sets the seed value for <b>random()</b>. By default, <b>random()</b> produces different results each time the program is run. Set the
	 * <b>value</b> parameter to a constant to return the same pseudo-random numbers each time the software is run.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param seed seed value
	 * @webref math:random
	 * @see processing.core.PApplet#random(float, float)
	 * @see processing.core.PApplet#noise(float, float, float)
	 * @see processing.core.PApplet#noiseSeed(long)
	 */
	public void randomSeed(long seed) {
		applet.randomSeed(seed);
	}

	/**
	 * @param x
	 */
	public float noise(float x) {
		return applet.noise(x);
	}

	/**
	 * @param x
	 * @param y
	 */
	public float noise(float x, float y) {
		return applet.noise(x, y);
	}

	/**
	 * ( begin auto-generated from noise.xml )
	 * <p>
	 * Returns the Perlin noise value at specified coordinates. Perlin noise is a random sequence generator producing a more natural ordered,
	 * harmonic succession of numbers compared to the standard <b>random()</b> function. It was invented by Ken Perlin in the 1980s and been used
	 * since in graphical applications to produce procedural textures, natural motion, shapes, terrains etc.<br /><br /> The main difference to
	 * the <b>random()</b> function is that Perlin noise is defined in an infinite n-dimensional space where each pair of coordinates corresponds
	 * to a fixed semi-random value (fixed only for the lifespan of the program). The resulting value will always be between 0.0 and 1.0.
	 * Processing can compute 1D, 2D and 3D noise, depending on the number of coordinates given. The noise value can be animated by moving through
	 * the noise space as demonstrated in the example above. The 2nd and 3rd dimension can also be interpreted as time.<br /><br />The actual
	 * noise is structured similar to an audio signal, in respect to the function's use of frequencies. Similar to the concept of harmonics in
	 * physics, perlin noise is computed over several octaves which are added together for the public final  result. <br /><br />Another way to adjust the
	 * character of the resulting sequence is the scale of the input coordinates. As the function works within an infinite space the value of the
	 * coordinates doesn't matter as such, only the distance between successive coordinates does (eg. when using <b>noise()</b> within a loop). As
	 * a general rule the smaller the difference between coordinates, the smoother the resulting noise sequence will be. Steps of 0.005-0.03 work
	 * best for most applications, but this will differ depending on use.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x x-coordinate in noise space
	 * @param y y-coordinate in noise space
	 * @param z z-coordinate in noise space
	 * @webref math:random
	 * @see processing.core.PApplet#noiseSeed(long)
	 * @see processing.core.PApplet#noiseDetail(int, float)
	 * @see processing.core.PApplet#random(float, float)
	 */
	public float noise(float x, float y, float z) {
		return applet.noise(x, y, z);
	}

	/**
	 * ( begin auto-generated from noiseDetail.xml )
	 * <p>
	 * Adjusts the character and level of detail produced by the Perlin noise function. Similar to harmonics in physics, noise is computed over
	 * several octaves. Lower octaves contribute more to the output signal and as such define the overal intensity of the noise, whereas higher
	 * octaves create finer grained details in the noise sequence. By default, noise is computed over 4 octaves with each octave contributing
	 * exactly half than its predecessor, starting at 50% strength for the 1st octave. This falloff amount can be changed by adding an additional
	 * function parameter. Eg. a falloff factor of 0.75 means each octave will now have 75% impact (25% less) of the previous lower octave. Any
	 * value between 0.0 and 1.0 is valid, however note that values greater than 0.5 might result in greater than 1.0 values returned by
	 * <b>noise()</b>.<br /><br />By changing these parameters, the signal created by the <b>noise()</b> function can be adapted to fit very
	 * specific needs and characteristics.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param lod number of octaves to be used by the noise
	 * @webref math:random
	 * @see processing.core.PApplet#noise(float, float, float)
	 */
	public void noiseDetail(int lod) {
		applet.noiseDetail(lod);
	}

	/**
	 * @param lod
	 * @param falloff falloff factor for each octave
	 */
	public void noiseDetail(int lod, float falloff) {
		applet.noiseDetail(lod, falloff);
	}

	/**
	 * ( begin auto-generated from noiseSeed.xml )
	 * <p>
	 * Sets the seed value for <b>noise()</b>. By default, <b>noise()</b> produces different results each time the program is run. Set the
	 * <b>value</b> parameter to a constant to return the same pseudo-random numbers each time the software is run.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param seed seed value
	 * @webref math:random
	 * @see processing.core.PApplet#noise(float, float, float)
	 * @see processing.core.PApplet#noiseDetail(int, float)
	 * @see processing.core.PApplet#random(float, float)
	 * @see processing.core.PApplet#randomSeed(long)
	 */
	public void noiseSeed(long seed) {
		applet.noiseSeed(seed);
	}

	/**
	 * ( begin auto-generated from loadImage.xml )
	 * <p>
	 * Loads an image into a variable of type <b>PImage</b>. Four types of images ( <b>.gif</b>, <b>.jpg</b>, <b>.tga</b>, <b>.png</b>) images may
	 * be loaded. To load correctly, images must be located in the data directory of the current sketch. In most cases, load all images in
	 * <b>setup()</b> to preload them at the start of the program. Loading images inside <b>draw()</b> will reduce the speed of a program.<br/>
	 * <br/> <b>filename</b> parameter can also be a URL to a file found online. For security reasons, a Processing sketch found online can only
	 * download files from the same server from which it came. Getting around this restriction requires a <a
	 * href="http://wiki.processing.org/w/Sign_an_Applet">signed __applet</a>.<br/> <br/> <b>extension</b> parameter is used to determine the image
	 * type in cases where the image filename does not end with a proper extension. Specify the extension as the second parameter to
	 * <b>loadImage()</b>, as shown in the third example on this page.<br/> <br/> an image is not loaded successfully, the <b>null</b> value is
	 * returned and an error message will be printed to the console. The error message does not halt the program, however the null value may cause
	 * a NullPointerException if your code does not check whether the value returned from <b>loadImage()</b> is null.<br/> <br/> on the type of
	 * error, a <b>PImage</b> object may still be returned, but the width and height of the image will be set to -1. This happens if bad image
	 * data is returned or cannot be decoded properly. Sometimes this happens with image URLs that produce a 403 error or that redirect to a
	 * password prompt, because <b>loadImage()</b> will attempt to interpret the HTML as image data.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename name of file to load, can be .gif, .jpg, .tga, or a handful of other image types depending on your platform
	 * @webref image:loading_displaying
	 * @see processing.core.PImage
	 * @see processing.core.PGraphics#image(processing.core.PImage, float, float, float, float)
	 * @see processing.core.PGraphics#imageMode(int)
	 * @see processing.core.PGraphics#background(float, float, float, float)
	 */
	public PImage loadImage(String filename) {
		return applet.loadImage(filename);
	}

	/**
	 * @param filename
	 * @param extension type of image to load, for example "png", "gif", "jpg"
	 */
	public PImage loadImage(String filename, String extension) {
		return applet.loadImage(filename, extension);
	}

	public PImage requestImage(String filename) {
		return applet.requestImage(filename);
	}

	/**
	 * ( begin auto-generated from requestImage.xml )
	 * <p>
	 * This function load images on a separate thread so that your sketch does not freeze while images load during <b>setup()</b>. While the image
	 * is loading, its width and height will be 0. If an error occurs while loading the image, its width and height will be set to -1. You'll know
	 * when the image has loaded properly because its width and height will be greater than 0. Asynchronous image loading (particularly when
	 * downloading from a server) can dramatically improve performance.<br /> <br/> <b>extension</b> parameter is used to determine the image type
	 * in cases where the image filename does not end with a proper extension. Specify the extension as the second parameter to
	 * <b>requestImage()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename  name of the file to load, can be .gif, .jpg, .tga, or a handful of other image types depending on your platform
	 * @param extension the type of image to load, for example "png", "gif", "jpg"
	 * @webref image:loading_displaying
	 * @see processing.core.PImage
	 * @see processing.core.PApplet#loadImage(String, String)
	 */
	public PImage requestImage(String filename, String extension) {
		return applet.requestImage(filename, extension);
	}

	/**
	 * @param filename name of a file in the data folder or a URL.
	 * @webref input:files
	 * @see XML
	 * @see processing.core.PApplet#parseXML(String)
	 * @see processing.core.PApplet#saveXML(XML, String)
	 * @see processing.core.PApplet#loadBytes(String)
	 * @see processing.core.PApplet#loadStrings(String)
	 * @see processing.core.PApplet#loadTable(String)
	 */
	public XML loadXML(String filename) {
		return applet.loadXML(filename);
	}

	/**
	 * @param filename
	 * @param options
	 * @nowebref
	 */
	public XML loadXML(String filename, String options) {
		return applet.loadXML(filename, options);
	}

	/**
	 * @param xmlString
	 * @return an XML object, or null
	 * @webref input:files
	 * @brief Converts String content to an XML object
	 * @see processing.data.XML
	 * @see processing.core.PApplet#loadXML(String)
	 * @see processing.core.PApplet#saveXML(processing.data.XML, String)
	 */
	public XML parseXML(String xmlString) {
		return applet.parseXML(xmlString);
	}

	public XML parseXML(String xmlString, String options) {
		return applet.parseXML(xmlString, options);
	}

	/**
	 * @param xml      the XML object to save to disk
	 * @param filename name of the file to write to
	 * @webref output:files
	 * @see processing.data.XML
	 * @see processing.core.PApplet#loadXML(String)
	 * @see processing.core.PApplet#parseXML(String)
	 */
	public boolean saveXML(XML xml, String filename) {
		return applet.saveXML(xml, filename);
	}

	public boolean saveXML(XML xml, String filename, String options) {
		return applet.saveXML(xml, filename, options);
	}

	public JSONObject parseJSONObject(String input) {
		return applet.parseJSONObject(input);
	}

	/**
	 * @param filename name of a file in the data folder or a URL
	 * @webref input:files
	 * @see processing.data.JSONObject
	 * @see JSONArray
	 * @see processing.core.PApplet#loadJSONArray(String)
	 * @see processing.core.PApplet#saveJSONObject(processing.data.JSONObject, String)
	 * @see processing.core.PApplet#saveJSONArray(JSONArray, String)
	 */
	public JSONObject loadJSONObject(String filename) {
		return applet.loadJSONObject(filename);
	}

	public JSONObject loadJSONObject(File file) {
		return PApplet.loadJSONObject(file);
	}

	/**
	 * @param json
	 * @param filename
	 * @webref output:files
	 * @see processing.data.JSONObject
	 * @see JSONArray
	 * @see processing.core.PApplet#loadJSONObject(String)
	 * @see processing.core.PApplet#loadJSONArray(String)
	 * @see processing.core.PApplet#saveJSONArray(JSONArray, String)
	 */
	public boolean saveJSONObject(JSONObject json, String filename) {
		return applet.saveJSONObject(json, filename);
	}

	public boolean saveJSONObject(JSONObject json, String filename, String options) {
		return applet.saveJSONObject(json, filename, options);
	}

	public JSONArray parseJSONArray(String input) {
		return applet.parseJSONArray(input);
	}

	/**
	 * @param filename name of a file in the data folder or a URL
	 * @webref input:files
	 * @see processing.data.JSONObject
	 * @see processing.data.JSONArray
	 * @see processing.core.PApplet#loadJSONObject(String)
	 * @see processing.core.PApplet#saveJSONObject(processing.data.JSONObject, String)
	 * @see processing.core.PApplet#saveJSONArray(processing.data.JSONArray, String)
	 */
	public JSONArray loadJSONArray(String filename) {
		return applet.loadJSONArray(filename);
	}

	public JSONArray loadJSONArray(File file) {
		return PApplet.loadJSONArray(file);
	}

	/**
	 * @param json
	 * @param filename
	 * @webref output:files
	 * @see processing.data.JSONObject
	 * @see processing.data.JSONArray
	 * @see processing.core.PApplet#loadJSONObject(String)
	 * @see processing.core.PApplet#loadJSONArray(String)
	 * @see processing.core.PApplet#saveJSONObject(processing.data.JSONObject, String)
	 */
	public boolean saveJSONArray(JSONArray json, String filename) {
		return applet.saveJSONArray(json, filename);
	}

	public boolean saveJSONArray(JSONArray json, String filename, String options) {
		return applet.saveJSONArray(json, filename, options);
	}

	/**
	 * @param filename name of a file in the data folder or a URL.
	 * @webref input:files
	 * @see Table
	 * @see processing.core.PApplet#saveTable(Table, String)
	 * @see processing.core.PApplet#loadBytes(String)
	 * @see processing.core.PApplet#loadStrings(String)
	 * @see processing.core.PApplet#loadXML(String)
	 */
	public Table loadTable(String filename) {
		return applet.loadTable(filename);
	}

	/**
	 * Options may contain "header", "tsv", "csv", or "bin" separated by commas.
	 * <p>
	 * Another option is "dictionary=filename.tsv", which allows users to specify a "dictionary" file that contains a mapping of the column titles
	 * and the data types used in the table file. This can be far more efficient (in terms of speed and memory usage) for loading and parsing
	 * tables. The dictionary file can only be tab separated values (.tsv) and its extension will be ignored. This option was added in Processing
	 * 2.0.2.
	 *
	 * @param filename
	 * @param options
	 */
	public Table loadTable(String filename, String options) {
		return applet.loadTable(filename, options);
	}

	/**
	 * @param table    the Table object to save to a file
	 * @param filename the filename to which the Table should be saved
	 * @webref output:files
	 * @see processing.data.Table
	 * @see processing.core.PApplet#loadTable(String)
	 */
	public boolean saveTable(Table table, String filename) {
		return applet.saveTable(table, filename);
	}

	/**
	 * @param table
	 * @param filename
	 * @param options  can be one of "tsv", "csv", "bin", or "html"
	 */
	public boolean saveTable(Table table, String filename, String options) {
		return applet.saveTable(table, filename, options);
	}

	/**
	 * ( begin auto-generated from loadFont.xml )
	 * <p>
	 * Loads a font into a variable of type <b>PFont</b>. To load correctly, fonts must be located in the data directory of the current sketch. To
	 * create a font to use with Processing, select "Create Font..." from the Tools menu. This will create a font in the format Processing
	 * requires and also adds it to the current sketch's data directory.<br /> <br /> Like <b>loadImage()</b> and other functions that load data,
	 * the <b>loadFont()</b> function should not be used inside <b>draw()</b>, because it will slow down the sketch considerably, as the font will
	 * be re-loaded from the disk (or network) on each frame.<br /> <br /> For most renderers, Processing displays fonts using the .vlw font
	 * format, which uses images for each letter, rather than defining them through vector data. When <b>hint(ENABLE_NATIVE_FONTS)</b> is used
	 * with the JAVA2D renderer, the native version of a font will be used if it is installed on the user's machine.<br /> <br /> Using
	 * <b>createFont()</b> (instead of loadFont) enables vector data to be used with the JAVA2D (default) renderer setting. This can be helpful
	 * when many font sizes are needed, or when using any renderer based on JAVA2D, such as the PDF library.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename name of the font to load
	 * @webref typography:loading_displaying
	 * @see PFont
	 * @see processing.core.PGraphics#textFont(PFont, float)
	 * @see processing.core.PApplet#createFont(String, float, boolean, char[])
	 */
	public PFont loadFont(String filename) {
		return applet.loadFont(filename);
	}

	public PFont createFont(String name, float size) {
		return applet.createFont(name, size);
	}

	public PFont createFont(String name, float size, boolean smooth) {
		return applet.createFont(name, size, smooth);
	}

	/**
	 * ( begin auto-generated from createFont.xml )
	 * <p>
	 * Dynamically converts a font to the format used by Processing from either a font name that's installed on the computer, or from a .ttf or
	 * .otf file inside the sketches "data" folder. This function is an advanced feature for precise control. On most occasions you should create
	 * fonts through selecting "Create Font..." from the Tools menu. <br /><br /> Use the <b>PFont.list()</b> method to first determine the names
	 * for the fonts recognized by the computer and are compatible with this function. Because of limitations in Java, not all fonts can be used
	 * and some might work with one operating system and not others. When sharing a sketch with other people or posting it on the web, you may
	 * need to include a .ttf or .otf version of your font in the data directory of the sketch because other people might not have the font
	 * installed on their computer. Only fonts that can legally be distributed should be included with a sketch. <br /><br /> The <b>size</b>
	 * parameter states the font size you want to generate. The <b>smooth</b> parameter specifies if the font should be antialiased or not, and
	 * the <b>charset</b> parameter is an array of chars that specifies the characters to generate. <br /><br /> This function creates a bitmapped
	 * version of a font in the same manner as the Create Font tool. It loads a font by name, and converts it to a series of images based on the
	 * size of the font. When possible, the <b>text()</b> function will use a native font rather than the bitmapped version created behind the
	 * scenes with <b>createFont()</b>. For instance, when using P2D, the actual native version of the font will be employed by the sketch,
	 * improving drawing quality and performance. With the P3D renderer, the bitmapped version will be used. While this can drastically improve
	 * speed and appearance, results are poor when exporting if the sketch does not include the .otf or .ttf file, and the requested font is not
	 * available on the machine running the sketch.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param name    name of the font to load
	 * @param size    point size of the font
	 * @param smooth  true for an antialiased font, false for aliased
	 * @param charset array containing characters to be generated
	 * @webref typography:loading_displaying
	 * @see processing.core.PFont
	 * @see processing.core.PGraphics#textFont(processing.core.PFont, float)
	 * @see processing.core.PGraphics#text(String, float, float, float, float, float)
	 * @see processing.core.PApplet#loadFont(String)
	 */
	public PFont createFont(String name, float size, boolean smooth, char[] charset) {
		return applet.createFont(name, size, smooth, charset);
	}

	/**
	 * Open a platform-specific file chooser dialog to select a file for input. After the selection is made, the selected File will be passed to
	 * the 'callback' function. If the dialog is closed or canceled, null will be sent to the function, so that the program is not waiting for
	 * additional input. The callback is necessary because of how threading works.
	 * <p>
	 * <pre>
	 * void setup() {
	 *   selectInput("Select a file to process:", "fileSelected");
	 * }
	 *
	 * void fileSelected(File selection) {
	 *   if (selection == null) {
	 *     println("Window was closed or the user hit cancel.");
	 *   } else {
	 *     println("User selected " + fileSeleted.getAbsolutePath());
	 *   }
	 * }
	 * </pre>
	 * <p>
	 * For advanced users, the method must be 'public', which is true for all methods inside a sketch when run from the PDE, but must explicitly
	 * be set when using Eclipse or other development environments.
	 *
	 * @param prompt   message to the user
	 * @param callback name of the method to be called when the selection is made
	 * @webref input:files
	 */
	public void selectInput(String prompt, String callback) {
		applet.selectInput(prompt, callback);
	}

	public void selectInput(String prompt, String callback, File file) {
		applet.selectInput(prompt, callback, file);
	}

	public void selectInput(String prompt, String callback, File file, Object callbackObject) {
		applet.selectInput(prompt, callback, file, callbackObject);
	}

	public void selectInput(String prompt, String callbackMethod, File file, Object callbackObject, Frame parent) {
		PApplet.selectInput(prompt, callbackMethod, file, callbackObject, parent);
	}

	/**
	 * See selectInput() for details.
	 *
	 * @param prompt   message to the user
	 * @param callback name of the method to be called when the selection is made
	 * @webref output:files
	 */
	public void selectOutput(String prompt, String callback) {
		applet.selectOutput(prompt, callback);
	}

	public void selectOutput(String prompt, String callback, File file) {
		applet.selectOutput(prompt, callback, file);
	}

	public void selectOutput(String prompt, String callback, File file, Object callbackObject) {
		applet.selectOutput(prompt, callback, file, callbackObject);
	}

	public void selectOutput(String prompt, String callbackMethod, File file, Object callbackObject, Frame parent) {
		PApplet.selectOutput(prompt, callbackMethod, file, callbackObject, parent);
	}

	/**
	 * See selectInput() for details.
	 *
	 * @param prompt   message to the user
	 * @param callback name of the method to be called when the selection is made
	 * @webref input:files
	 */
	public void selectFolder(String prompt, String callback) {
		applet.selectFolder(prompt, callback);
	}

	public void selectFolder(String prompt, String callback, File file) {
		applet.selectFolder(prompt, callback, file);
	}

	public void selectFolder(String prompt, String callback, File file, Object callbackObject) {
		applet.selectFolder(prompt, callback, file, callbackObject);
	}

	public void selectFolder(String prompt, String callbackMethod, File defaultSelection, Object callbackObject, Frame parentFrame) {
		PApplet.selectFolder(prompt, callbackMethod, defaultSelection, callbackObject, parentFrame);
	}

	/**
	 * Get the compression-free extension for this filename.
	 *
	 * @param filename The filename to check
	 * @return an extension, skipping past .gz if it's present
	 */
	public String checkExtension(String filename) {
		return PApplet.checkExtension(filename);
	}

	/**
	 * ( begin auto-generated from createReader.xml )
	 * <p>
	 * Creates a <b>BufferedReader</b> object that can be used to read files line-by-line as individual <b>String</b> objects. This is the
	 * complement to the <b>createWriter()</b> function. <br/> <br/> Starting with Processing release 0134, all files loaded and saved by the
	 * Processing API use UTF-8 encoding. In previous releases, the default encoding for your platform was used, which causes problems when files
	 * are moved to other platforms.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename name of the file to be opened
	 * @webref input:files
	 * @see BufferedReader
	 * @see processing.core.PApplet#createWriter(String)
	 * @see PrintWriter
	 */
	public BufferedReader createReader(String filename) {
		return applet.createReader(filename);
	}

	/**
	 * @param file
	 * @nowebref
	 */
	public BufferedReader createReader(File file) {
		return PApplet.createReader(file);
	}

	/**
	 * @param input
	 * @nowebref I want to read lines from a stream. If I have to type the following lines any more I'm gonna send Sun my medical bills.
	 */
	public BufferedReader createReader(InputStream input) {
		return PApplet.createReader(input);
	}

	/**
	 * ( begin auto-generated from createWriter.xml )
	 * <p>
	 * Creates a new file in the sketch folder, and a <b>PrintWriter</b> object to write to it. For the file to be made correctly, it should be
	 * flushed and must be closed with its <b>flush()</b> and <b>close()</b> methods (see above example). <br/> <br/> Starting with Processing
	 * release 0134, all files loaded and saved by the Processing API use UTF-8 encoding. In previous releases, the default encoding for your
	 * platform was used, which causes problems when files are moved to other platforms.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename name of the file to be created
	 * @webref output:files
	 * @see PrintWriter
	 * @see processing.core.PApplet#createReader
	 * @see java.io.BufferedReader
	 */
	public PrintWriter createWriter(String filename) {
		return applet.createWriter(filename);
	}

	/**
	 * @param file
	 * @nowebref I want to print lines to a file. I have RSI from typing these eight lines of code so many times.
	 */
	public PrintWriter createWriter(File file) {
		return PApplet.createWriter(file);
	}

	/**
	 * @param output
	 * @nowebref I want to print lines to a file. Why am I always explaining myself? It's the JavaSoft API engineers who need to explain
	 * themselves.
	 */
	public PrintWriter createWriter(OutputStream output) {
		return PApplet.createWriter(output);
	}

	/**
	 * @param filename
	 * @deprecated As of release 0136, use createInput() instead.
	 */
	public InputStream openStream(String filename) {
		return applet.openStream(filename);
	}

	/**
	 * ( begin auto-generated from createInput.xml )
	 * <p>
	 * This is a function for advanced programmers to open a Java InputStream. It's useful if you want to use the facilities provided by PApplet
	 * to easily open files from the data folder or from a URL, but want an InputStream object so that you can use other parts of Java to take
	 * more control of how the stream is read.<br /> <br /> The filename passed in can be:<br /> - A URL, for instance
	 * <b>openStream("http://processing.org/")</b><br /> - A file in the sketch's <b>data</b> folder<br /> - The full path to a file to be opened
	 * locally (when running as an application)<br /> <br /> If the requested item doesn't exist, null is returned. If not online, this will also
	 * check to see if the user is asking for a file whose name isn't properly capitalized. If capitalization is different, an error will be
	 * printed to the console. This helps prevent issues that appear when a sketch is exported to the web, where case sensitivity matters, as
	 * opposed to running from inside the Processing Development Environment on Windows or Mac OS, where case sensitivity is preserved but
	 * ignored.<br /> <br /> If the file ends with <b>.gz</b>, the stream will automatically be gzip decompressed. If you don't want the automatic
	 * decompression, use the related function <b>createInputRaw()</b>. <br /> In earlier releases, this function was called
	 * <b>openStream()</b>.<br /> <br />
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Simplified method to open a Java InputStream.
	 * <p>
	 * This method is useful if you want to use the facilities provided by PApplet to easily open things from the data folder or from a URL, but
	 * want an InputStream object so that you can use other Java methods to take more control of how the stream is read.
	 * <p>
	 * If the requested item doesn't exist, null is returned. (Prior to 0096, die() would be called, killing the __applet)
	 * <p>
	 * For 0096+, the "data" folder is exported intact with subfolders, and openStream() properly handles subdirectories from the data folder
	 * <p>
	 * If not online, this will also check to see if the user is asking for a file whose name isn't properly capitalized. This helps prevent
	 * issues when a sketch is exported to the web, where case sensitivity matters, as opposed to Windows and the Mac OS default where case
	 * sensitivity is preserved but ignored.
	 * <p>
	 * It is strongly recommended that libraries use this method to open data files, so that the loading sequence is handled in the same way as
	 * functions like loadBytes(), loadImage(), etc.
	 * <p>
	 * The filename passed in can be: <UL> <LI>A URL, for instance openStream("http://processing.org/"); <LI>A file in the sketch's data folder
	 * <LI>Another file to be opened locally (when running as an application) </UL>
	 *
	 * @param filename the name of the file to use as input
	 * @webref input:files
	 * @see processing.core.PApplet#createOutput(String)
	 * @see processing.core.PApplet#selectOutput(String)
	 * @see processing.core.PApplet#selectInput(String)
	 */
	public InputStream createInput(String filename) {
		return applet.createInput(filename);
	}

	/**
	 * Call openStream() without automatic gzip decompression.
	 *
	 * @param filename
	 */
	public InputStream createInputRaw(String filename) {
		return applet.createInputRaw(filename);
	}

	/**
	 * @param file
	 * @nowebref
	 */
	public InputStream createInput(File file) {
		return PApplet.createInput(file);
	}

	/**
	 * ( begin auto-generated from loadBytes.xml )
	 * <p>
	 * Reads the contents of a file or url and places it in a byte array. If a file is specified, it must be located in the sketch's "data"
	 * directory/folder.<br /> <br /> The filename parameter can also be a URL to a file found online. For security reasons, a Processing sketch
	 * found online can only download files from the same server from which it came. Getting around this restriction requires a <a
	 * href="http://wiki.processing.org/w/Sign_an_Applet">signed __applet</a>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename name of a file in the data folder or a URL.
	 * @webref input:files
	 * @see processing.core.PApplet#loadStrings(String)
	 * @see processing.core.PApplet#saveStrings(String, String[])
	 * @see processing.core.PApplet#saveBytes(String, byte[])
	 */
	public byte[] loadBytes(String filename) {
		return applet.loadBytes(filename);
	}

	/**
	 * @param input
	 * @nowebref
	 */
	public byte[] loadBytes(InputStream input) {
		return PApplet.loadBytes(input);
	}

	/**
	 * @param file
	 * @nowebref
	 */
	public byte[] loadBytes(File file) {
		return PApplet.loadBytes(file);
	}

	/**
	 * @param file
	 * @nowebref
	 */
	public String[] loadStrings(File file) {
		return PApplet.loadStrings(file);
	}

	/**
	 * ( begin auto-generated from loadStrings.xml )
	 * <p>
	 * Reads the contents of a file or url and creates a String array of its individual lines. If a file is specified, it must be located in the
	 * sketch's "data" directory/folder.<br /> <br /> The filename parameter can also be a URL to a file found online. For security reasons, a
	 * Processing sketch found online can only download files from the same server from which it came. Getting around this restriction requires a
	 * <a href="http://wiki.processing.org/w/Sign_an_Applet">signed __applet</a>. <br /> If the file is not available or an error occurs,
	 * <b>null</b> will be returned and an error message will be printed to the console. The error message does not halt the program, however the
	 * null value may cause a NullPointerException if your code does not check whether the value returned is null. <br/> <br/> Starting with
	 * Processing release 0134, all files loaded and saved by the Processing API use UTF-8 encoding. In previous releases, the default encoding
	 * for your platform was used, which causes problems when files are moved to other platforms.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Load data from a file and shove it into a String array.
	 * <p>
	 * Exceptions are handled internally, when an error, occurs, an exception is printed to the console and 'null' is returned, but the program
	 * continues running. This is a tradeoff between 1) showing the user that there was a problem but 2) not requiring that all i/o code is
	 * contained in try/catch blocks, for the sake of new users (or people who are just trying to get things done in a "scripting" fashion. If you
	 * want to handle exceptions, use Java methods for I/O.
	 *
	 * @param filename name of the file or url to load
	 * @webref input:files
	 * @see processing.core.PApplet#loadBytes(String)
	 * @see processing.core.PApplet#saveStrings(String, String[])
	 * @see processing.core.PApplet#saveBytes(String, byte[])
	 */
	public String[] loadStrings(String filename) {
		return applet.loadStrings(filename);
	}

	/**
	 * @param input
	 * @nowebref
	 */
	public String[] loadStrings(InputStream input) {
		return PApplet.loadStrings(input);
	}

	public String[] loadStrings(BufferedReader reader) {
		return PApplet.loadStrings(reader);
	}

	/**
	 * ( begin auto-generated from createOutput.xml )
	 * <p>
	 * Similar to <b>createInput()</b>, this creates a Java <b>OutputStream</b> for a given filename or path. The file will be created in the
	 * sketch folder, or in the same folder as an exported application. <br /><br /> If the path does not exist, intermediate folders will be
	 * created. If an exception occurs, it will be printed to the console, and <b>null</b> will be returned. <br /><br /> This function is a
	 * convenience over the Java approach that requires you to 1) create a FileOutputStream object, 2) determine the exact file location, and 3)
	 * handle exceptions. Exceptions are handled internally by the function, which is more appropriate for "sketch" projects. <br /><br /> If the
	 * output filename ends with <b>.gz</b>, the output will be automatically GZIP compressed as it is written.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename name of the file to open
	 * @webref output:files
	 * @see processing.core.PApplet#createInput(String)
	 * @see processing.core.PApplet#selectOutput()
	 */
	public OutputStream createOutput(String filename) {
		return applet.createOutput(filename);
	}

	/**
	 * @param file
	 * @nowebref
	 */
	public OutputStream createOutput(File file) {
		return PApplet.createOutput(file);
	}

	/**
	 * ( begin auto-generated from saveStream.xml )
	 * <p>
	 * Save the contents of a stream to a file in the sketch folder. This is basically <b>saveBytes(blah, loadBytes())</b>, but done more
	 * efficiently (and with less confusing syntax).<br /> <br /> When using the <b>targetFile</b> parameter, it writes to a <b>File</b> object
	 * for greater control over the file location. (Note that unlike some other functions, this will not automatically compress or uncompress gzip
	 * files.)
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param target name of the file to write to
	 * @param source location to read from (a filename, path, or URL)
	 * @webref output:files
	 * @see processing.core.PApplet#createOutput(String)
	 */
	public boolean saveStream(String target, String source) {
		return applet.saveStream(target, source);
	}

	/**
	 * Identical to the other saveStream(), but writes to a File object, for greater control over the file location.
	 * <p>
	 * Note that unlike other api methods, this will not automatically compress or uncompress gzip files.
	 *
	 * @param target
	 * @param source
	 */
	public boolean saveStream(File target, String source) {
		return applet.saveStream(target, source);
	}

	/**
	 * @param target
	 * @param source
	 * @nowebref
	 */
	public boolean saveStream(String target, InputStream source) {
		return applet.saveStream(target, source);
	}

	/**
	 * @param target
	 * @param source
	 * @nowebref
	 */
	public boolean saveStream(File target, InputStream source) {
		return PApplet.saveStream(target, source);
	}

	/**
	 * @param target
	 * @param source
	 * @nowebref
	 */
	public void saveStream(OutputStream target, InputStream source) throws IOException {
		PApplet.saveStream(target, source);
	}

	/**
	 * ( begin auto-generated from saveBytes.xml )
	 * <p>
	 * Opposite of <b>loadBytes()</b>, will write an entire array of bytes to a file. The data is saved in binary format. This file is saved to
	 * the sketch's folder, which is opened by selecting "Show sketch folder" from the "Sketch" menu.<br /> <br /> It is not possible to use
	 * saveXxxxx() functions inside a web browser unless the sketch is <a href="http://wiki.processing.org/w/Sign_an_Applet">signed __applet</A>. To
	 * save a file back to a server, see the <a href="http://wiki.processing.org/w/Saving_files_to_a_web-server">save to web</A> code snippet on
	 * the Processing Wiki.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename name of the file to write to
	 * @param data     array of bytes to be written
	 * @webref output:files
	 * @see processing.core.PApplet#loadStrings(String)
	 * @see processing.core.PApplet#loadBytes(String)
	 * @see processing.core.PApplet#saveStrings(String, String[])
	 */
	public void saveBytes(String filename, byte[] data) {
		applet.saveBytes(filename, data);
	}

	/**
	 * @param file
	 * @param data
	 * @nowebref Saves bytes to a specific File location specified by the user.
	 */
	public void saveBytes(File file, byte[] data) {
		PApplet.saveBytes(file, data);
	}

	/**
	 * @param output
	 * @param data
	 * @nowebref Spews a buffer of bytes to an OutputStream.
	 */
	public void saveBytes(OutputStream output, byte[] data) {
		PApplet.saveBytes(output, data);
	}

	/**
	 * ( begin auto-generated from saveStrings.xml )
	 * <p>
	 * Writes an array of strings to a file, one line per string. This file is saved to the sketch's folder, which is opened by selecting "Show
	 * sketch folder" from the "Sketch" menu.<br /> <br /> It is not possible to use saveXxxxx() functions inside a web browser unless the sketch
	 * is <a href="http://wiki.processing.org/w/Sign_an_Applet">signed __applet</A>. To save a file back to a server, see the <a
	 * href="http://wiki.processing.org/w/Saving_files_to_a_web-server">save to web</A> code snippet on the Processing Wiki.<br/> <br/ > Starting
	 * with Processing 1.0, all files loaded and saved by the Processing API use UTF-8 encoding. In previous releases, the default encoding for
	 * your platform was used, which causes problems when files are moved to other platforms.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param filename filename for output
	 * @param data     string array to be written
	 * @webref output:files
	 * @see processing.core.PApplet#loadStrings(String)
	 * @see processing.core.PApplet#loadBytes(String)
	 * @see processing.core.PApplet#saveBytes(String, byte[])
	 */
	public void saveStrings(String filename, String[] data) {
		applet.saveStrings(filename, data);
	}

	/**
	 * @param file
	 * @param data
	 * @nowebref
	 */
	public void saveStrings(File file, String[] data) {
		PApplet.saveStrings(file, data);
	}

	/**
	 * @param output
	 * @param data
	 * @nowebref
	 */
	public void saveStrings(OutputStream output, String[] data) {
		PApplet.saveStrings(output, data);
	}

	/**
	 * Prepend the sketch folder path to the filename (or path) that is passed in. External libraries should use this function to save to the
	 * sketch folder.
	 * <p>
	 * Note that when running as an __applet inside a web browser, the sketchPath will be set to null, because security restrictions prevent applets
	 * from accessing that information.
	 * <p>
	 * This will also cause an error if the sketch is not inited properly, meaning that init() was never called on the PApplet when hosted my some
	 * other main() or by other code. For proper use of init(), see the examples in the main description text for PApplet.
	 *
	 * @param where
	 */
	public String sketchPath(String where) {
		return applet.sketchPath(where);
	}

	public File sketchFile(String where) {
		return applet.sketchFile(where);
	}

	/**
	 * Returns a path inside the __applet folder to save to. Like sketchPath(), but creates any in-between folders so that things save properly.
	 * <p>
	 * All saveXxxx() functions use the path to the sketch folder, rather than its data folder. Once exported, the data folder will be found
	 * inside the jar file of the exported application or __applet. In this case, it's not possible to save data into the jar file, because it will
	 * often be running from a server, or marked in-use if running from a local file system. With this in mind, saving to the data path doesn't
	 * make sense anyway. If you know you're running locally, and want to save to the data folder, use <TT>saveXxxx("data/blah.dat")</TT>.
	 *
	 * @param where
	 */
	public String savePath(String where) {
		return applet.savePath(where);
	}

	/**
	 * Identical to savePath(), but returns a File object.
	 *
	 * @param where
	 */
	public File saveFile(String where) {
		return applet.saveFile(where);
	}

	/**
	 * Not a supported function. For testing use only.
	 *
	 * @param what
	 */
	public File desktopFile(String what) {
		return PApplet.desktopFile(what);
	}

	/**
	 * Not a supported function. For testing use only.
	 *
	 * @param what
	 */
	public String desktopPath(String what) {
		return PApplet.desktopPath(what);
	}

	/**
	 * Return a full path to an item in the data folder.
	 * <p>
	 * This is only available with applications, not applets or Android. On Windows and Linux, this is simply the data folder, which is located in
	 * the same directory as the EXE file and lib folders. On Mac OS X, this is a path to the data folder buried inside Contents/Java. For the
	 * latter point, that also means that the data folder should not be considered writable. Use sketchPath() for now, or inputPath() and
	 * outputPath() once they're available in the 2.0 release.
	 * <p>
	 * dataPath() is not supported with applets because applets have their data folder wrapped into the JAR file. To read data from the data
	 * folder that works with an __applet, you should use other methods such as createInput(), createReader(), or loadStrings().
	 *
	 * @param where
	 */
	public String dataPath(String where) {
		return applet.dataPath(where);
	}

	/**
	 * Return a full path to an item in the data folder as a File object. See the dataPath() method for more information.
	 *
	 * @param where
	 */
	public File dataFile(String where) {
		return applet.dataFile(where);
	}

	/**
	 * Takes a path and creates any in-between folders if they don't already exist. Useful when trying to save to a subfolder that may not
	 * actually exist.
	 *
	 * @param path
	 */
	public void createPath(String path) {
		PApplet.createPath(path);
	}

	public void createPath(File file) {
		PApplet.createPath(file);
	}

	public String getExtension(String filename) {
		return PApplet.getExtension(filename);
	}

	public String urlEncode(String str) {
		return PApplet.urlEncode(str);
	}

	public String urlDecode(String str) {
		return PApplet.urlDecode(str);
	}

	/**
	 * ( begin auto-generated from sort.xml )
	 * <p>
	 * Sorts an array of numbers from smallest to largest and puts an array of words in alphabetical order. The original array is not modified, a
	 * re-ordered array is returned. The <b>count</b> parameter states the number of elements to sort. For example if there are 12 elements in an
	 * array and if count is the value 5, only the first five elements on the array will be sorted. <!--As of release 0126, the alphabetical
	 * ordering is case insensitive.-->
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param list array to sort
	 * @webref data:array_functions
	 * @see processing.core.PApplet#reverse(boolean[])
	 */
	public byte[] sort(byte[] list) {
		return PApplet.sort(list);
	}

	/**
	 * @param list
	 * @param count number of elements to sort, starting from 0
	 */
	public byte[] sort(byte[] list, int count) {
		return PApplet.sort(list, count);
	}

	public char[] sort(char[] list) {
		return PApplet.sort(list);
	}

	public char[] sort(char[] list, int count) {
		return PApplet.sort(list, count);
	}

	public int[] sort(int[] list) {
		return PApplet.sort(list);
	}

	public int[] sort(int[] list, int count) {
		return PApplet.sort(list, count);
	}

	public float[] sort(float[] list) {
		return PApplet.sort(list);
	}

	public float[] sort(float[] list, int count) {
		return PApplet.sort(list, count);
	}

	public String[] sort(String[] list) {
		return PApplet.sort(list);
	}

	public String[] sort(String[] list, int count) {
		return PApplet.sort(list, count);
	}

	/**
	 * ( begin auto-generated from arrayCopy.xml )
	 * <p>
	 * Copies an array (or part of an array) to another array. The <b>src</b> array is copied to the <b>dst</b> array, beginning at the position
	 * specified by <b>srcPos</b> and into the position specified by <b>dstPos</b>. The number of elements to copy is determined by <b>length</b>.
	 * The simplified version with two arguments copies an entire array to another of the same size. It is equivalent to "arrayCopy(src, 0, dst,
	 * 0, src.length)". This function is far more efficient for copying array data than iterating through a <b>for</b> and copying each element.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param src         the source array
	 * @param srcPosition starting position in the source array
	 * @param dst         the destination array of the same data type as the source array
	 * @param dstPosition starting position in the destination array
	 * @param length      number of array elements to be copied
	 * @webref data:array_functions
	 * @see processing.core.PApplet#concat(boolean[], boolean[])
	 */
	public void arrayCopy(Object src, int srcPosition, Object dst, int dstPosition, int length) {
		PApplet.arrayCopy(src, srcPosition, dst, dstPosition, length);
	}

	/**
	 * Convenience method for arraycopy(). Identical to <CODE>arraycopy(src, 0, dst, 0, length);</CODE>
	 *
	 * @param src
	 * @param dst
	 * @param length
	 */
	public void arrayCopy(Object src, Object dst, int length) {
		PApplet.arrayCopy(src, dst, length);
	}

	/**
	 * Shortcut to copy the entire contents of the source into the destination array. Identical to <CODE>arraycopy(src, 0, dst, 0,
	 * src.length);</CODE>
	 *
	 * @param src
	 * @param dst
	 */
	public void arrayCopy(Object src, Object dst) {
		PApplet.arrayCopy(src, dst);
	}

	/**
	 * @param src
	 * @param srcPosition
	 * @param dst
	 * @param dstPosition
	 * @param length
	 * @deprecated Use arrayCopy() instead.
	 */
	public void arraycopy(Object src, int srcPosition, Object dst, int dstPosition, int length) {
		PApplet.arraycopy(src, srcPosition, dst, dstPosition, length);
	}

	/**
	 * @param src
	 * @param dst
	 * @param length
	 * @deprecated Use arrayCopy() instead.
	 */
	public void arraycopy(Object src, Object dst, int length) {
		PApplet.arraycopy(src, dst, length);
	}

	/**
	 * @param src
	 * @param dst
	 * @deprecated Use arrayCopy() instead.
	 */
	public void arraycopy(Object src, Object dst) {
		PApplet.arraycopy(src, dst);
	}

	/**
	 * ( begin auto-generated from expand.xml )
	 * <p>
	 * Increases the size of an array. By default, this function doubles the size of the array, but the optional <b>newSize</b> parameter provides
	 * precise control over the increase in size. <br/> <br/> When using an array of objects, the data returned from the function must be cast to
	 * the object array's data type. For example: <em>SomeClass[] items = (SomeClass[]) expand(originalArray)</em>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param list the array to expand
	 * @webref data:array_functions
	 * @see processing.core.PApplet#shorten(boolean[])
	 */
	public boolean[] expand(boolean[] list) {
		return PApplet.expand(list);
	}

	/**
	 * @param list
	 * @param newSize new size for the array
	 */
	public boolean[] expand(boolean[] list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	public byte[] expand(byte[] list) {
		return PApplet.expand(list);
	}

	public byte[] expand(byte[] list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	public char[] expand(char[] list) {
		return PApplet.expand(list);
	}

	public char[] expand(char[] list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	public int[] expand(int[] list) {
		return PApplet.expand(list);
	}

	public int[] expand(int[] list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	public long[] expand(long[] list) {
		return PApplet.expand(list);
	}

	public long[] expand(long[] list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	public float[] expand(float[] list) {
		return PApplet.expand(list);
	}

	public float[] expand(float[] list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	public double[] expand(double[] list) {
		return PApplet.expand(list);
	}

	public double[] expand(double[] list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	public String[] expand(String[] list) {
		return PApplet.expand(list);
	}

	public String[] expand(String[] list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	/**
	 * @param array
	 * @nowebref
	 */
	public Object expand(Object array) {
		return PApplet.expand(array);
	}

	public Object expand(Object list, int newSize) {
		return PApplet.expand(list, newSize);
	}

	/**
	 * ( begin auto-generated from append.xml )
	 * <p>
	 * Expands an array by one element and adds data to the new position. The datatype of the <b>element</b> parameter must be the same as the
	 * datatype of the array. <br/> <br/> When using an array of objects, the data returned from the function must be cast to the object array's
	 * data type. For example: <em>SomeClass[] items = (SomeClass[]) append(originalArray, element)</em>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param array array to append
	 * @param value new data for the array
	 * @webref data:array_functions
	 * @see processing.core.PApplet#shorten(boolean[])
	 * @see processing.core.PApplet#expand(boolean[])
	 */
	public byte[] append(byte[] array, byte value) {
		return PApplet.append(array, value);
	}

	public char[] append(char[] array, char value) {
		return PApplet.append(array, value);
	}

	public int[] append(int[] array, int value) {
		return PApplet.append(array, value);
	}

	public float[] append(float[] array, float value) {
		return PApplet.append(array, value);
	}

	public String[] append(String[] array, String value) {
		return PApplet.append(array, value);
	}

	public Object append(Object array, Object value) {
		return PApplet.append(array, value);
	}

	/**
	 * ( begin auto-generated from shorten.xml )
	 * <p>
	 * Decreases an array by one element and returns the shortened array. <br/> <br/> When using an array of objects, the data returned from the
	 * function must be cast to the object array's data type. For example: <em>SomeClass[] items = (SomeClass[]) shorten(originalArray)</em>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param list array to shorten
	 * @webref data:array_functions
	 * @see processing.core.PApplet#append(byte[], byte)
	 * @see processing.core.PApplet#expand(boolean[])
	 */
	public boolean[] shorten(boolean[] list) {
		return PApplet.shorten(list);
	}

	public byte[] shorten(byte[] list) {
		return PApplet.shorten(list);
	}

	public char[] shorten(char[] list) {
		return PApplet.shorten(list);
	}

	public int[] shorten(int[] list) {
		return PApplet.shorten(list);
	}

	public float[] shorten(float[] list) {
		return PApplet.shorten(list);
	}

	public String[] shorten(String[] list) {
		return PApplet.shorten(list);
	}

	public Object shorten(Object list) {
		return PApplet.shorten(list);
	}

	/**
	 * ( begin auto-generated from splice.xml )
	 * <p>
	 * Inserts a value or array of values into an existing array. The first two parameters must be of the same datatype. The <b>array</b>
	 * parameter defines the array which will be modified and the second parameter defines the data which will be inserted. <br/> <br/> When using
	 * an array of objects, the data returned from the function must be cast to the object array's data type. For example: <em>SomeClass[] items =
	 * (SomeClass[]) splice(array1, array2, index)</em>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param list  array to splice into
	 * @param value value to be spliced in
	 * @param index position in the array from which to insert data
	 * @webref data:array_functions
	 * @see processing.core.PApplet#concat(boolean[], boolean[])
	 * @see processing.core.PApplet#subset(boolean[], int, int)
	 */
	public boolean[] splice(boolean[] list, boolean value, int index) {
		return PApplet.splice(list, value, index);
	}

	public boolean[] splice(boolean[] list, boolean[] value, int index) {
		return PApplet.splice(list, value, index);
	}

	public byte[] splice(byte[] list, byte value, int index) {
		return PApplet.splice(list, value, index);
	}

	public byte[] splice(byte[] list, byte[] value, int index) {
		return PApplet.splice(list, value, index);
	}

	public char[] splice(char[] list, char value, int index) {
		return PApplet.splice(list, value, index);
	}

	public char[] splice(char[] list, char[] value, int index) {
		return PApplet.splice(list, value, index);
	}

	public int[] splice(int[] list, int value, int index) {
		return PApplet.splice(list, value, index);
	}

	public int[] splice(int[] list, int[] value, int index) {
		return PApplet.splice(list, value, index);
	}

	public float[] splice(float[] list, float value, int index) {
		return PApplet.splice(list, value, index);
	}

	public float[] splice(float[] list, float[] value, int index) {
		return PApplet.splice(list, value, index);
	}

	public String[] splice(String[] list, String value, int index) {
		return PApplet.splice(list, value, index);
	}

	public String[] splice(String[] list, String[] value, int index) {
		return PApplet.splice(list, value, index);
	}

	public Object splice(Object list, Object value, int index) {
		return PApplet.splice(list, value, index);
	}

	public boolean[] subset(boolean[] list, int start) {
		return PApplet.subset(list, start);
	}

	/**
	 * ( begin auto-generated from subset.xml )
	 * <p>
	 * Extracts an array of elements from an existing array. The <b>array</b> parameter defines the array from which the elements will be copied
	 * and the <b>offset</b> and <b>length</b> parameters determine which elements to extract. If no <b>length</b> is given, elements will be
	 * extracted from the <b>offset</b> to the end of the array. When specifying the <b>offset</b> remember the first array element is 0. This
	 * function does not change the source array. <br/> <br/> When using an array of objects, the data returned from the function must be cast to
	 * the object array's data type. For example: <em>SomeClass[] items = (SomeClass[]) subset(originalArray, 0, 4)</em>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param list  array to extract from
	 * @param start position to begin
	 * @param count number of values to extract
	 * @webref data:array_functions
	 * @see processing.core.PApplet#splice(boolean[], boolean, int)
	 */
	public boolean[] subset(boolean[] list, int start, int count) {
		return PApplet.subset(list, start, count);
	}

	public byte[] subset(byte[] list, int start) {
		return PApplet.subset(list, start);
	}

	public byte[] subset(byte[] list, int start, int count) {
		return PApplet.subset(list, start, count);
	}

	public char[] subset(char[] list, int start) {
		return PApplet.subset(list, start);
	}

	public char[] subset(char[] list, int start, int count) {
		return PApplet.subset(list, start, count);
	}

	public int[] subset(int[] list, int start) {
		return PApplet.subset(list, start);
	}

	public int[] subset(int[] list, int start, int count) {
		return PApplet.subset(list, start, count);
	}

	public float[] subset(float[] list, int start) {
		return PApplet.subset(list, start);
	}

	public float[] subset(float[] list, int start, int count) {
		return PApplet.subset(list, start, count);
	}

	public String[] subset(String[] list, int start) {
		return PApplet.subset(list, start);
	}

	public String[] subset(String[] list, int start, int count) {
		return PApplet.subset(list, start, count);
	}

	public Object subset(Object list, int start) {
		return PApplet.subset(list, start);
	}

	public Object subset(Object list, int start, int count) {
		return PApplet.subset(list, start, count);
	}

	/**
	 * ( begin auto-generated from concat.xml )
	 * <p>
	 * Concatenates two arrays. For example, concatenating the array { 1, 2, 3 } and the array { 4, 5, 6 } yields { 1, 2, 3, 4, 5, 6 }. Both
	 * parameters must be arrays of the same datatype. <br/> <br/> When using an array of objects, the data returned from the function must be
	 * cast to the object array's data type. For example: <em>SomeClass[] items = (SomeClass[]) concat(array1, array2)</em>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param a first array to concatenate
	 * @param b second array to concatenate
	 * @webref data:array_functions
	 * @see processing.core.PApplet#splice(boolean[], boolean, int)
	 * @see processing.core.PApplet#arrayCopy(Object, int, Object, int, int)
	 */
	public boolean[] concat(boolean[] a, boolean[] b) {
		return PApplet.concat(a, b);
	}

	public byte[] concat(byte[] a, byte[] b) {
		return PApplet.concat(a, b);
	}

	public char[] concat(char[] a, char[] b) {
		return PApplet.concat(a, b);
	}

	public int[] concat(int[] a, int[] b) {
		return PApplet.concat(a, b);
	}

	public float[] concat(float[] a, float[] b) {
		return PApplet.concat(a, b);
	}

	public String[] concat(String[] a, String[] b) {
		return PApplet.concat(a, b);
	}

	public Object concat(Object a, Object b) {
		return PApplet.concat(a, b);
	}

	/**
	 * ( begin auto-generated from reverse.xml )
	 * <p>
	 * Reverses the order of an array.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param list booleans[], bytes[], chars[], ints[], floats[], or Strings[]
	 * @webref data:array_functions
	 * @see processing.core.PApplet#sort(String[], int)
	 */
	public boolean[] reverse(boolean[] list) {
		return PApplet.reverse(list);
	}

	public byte[] reverse(byte[] list) {
		return PApplet.reverse(list);
	}

	public char[] reverse(char[] list) {
		return PApplet.reverse(list);
	}

	public int[] reverse(int[] list) {
		return PApplet.reverse(list);
	}

	public float[] reverse(float[] list) {
		return PApplet.reverse(list);
	}

	public String[] reverse(String[] list) {
		return PApplet.reverse(list);
	}

	public Object reverse(Object list) {
		return PApplet.reverse(list);
	}

	/**
	 * ( begin auto-generated from trim.xml )
	 * <p>
	 * Removes whitespace characters from the beginning and end of a String. In addition to standard whitespace characters such as space, carriage
	 * return, and tab, this function also removes the Unicode "nbsp" character.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param str any string
	 * @webref data:string_functions
	 * @see processing.core.PApplet#split(String, String)
	 * @see processing.core.PApplet#join(String[], char)
	 */
	public String trim(String str) {
		return PApplet.trim(str);
	}

	/**
	 * @param array a String array
	 */
	public String[] trim(String[] array) {
		return PApplet.trim(array);
	}

	/**
	 * ( begin auto-generated from join.xml )
	 * <p>
	 * Combines an array of Strings into one String, each separated by the character(s) used for the <b>separator</b> parameter. To join arrays of
	 * ints or floats, it's necessary to first convert them to strings using <b>nf()</b> or <b>nfs()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param list      array of Strings
	 * @param separator char or String to be placed between each item
	 * @webref data:string_functions
	 * @see processing.core.PApplet#split(String, String)
	 * @see processing.core.PApplet#trim(String)
	 * @see processing.core.PApplet#nf(float, int, int)
	 * @see processing.core.PApplet#nfs(float, int, int)
	 */
	public String join(String[] list, char separator) {
		return PApplet.join(list, separator);
	}

	public String join(String[] list, String separator) {
		return PApplet.join(list, separator);
	}

	public String[] splitTokens(String value) {
		return PApplet.splitTokens(value);
	}

	/**
	 * ( begin auto-generated from splitTokens.xml )
	 * <p>
	 * The splitTokens() function splits a String at one or many character "tokens." The <b>tokens</b> parameter specifies the character or
	 * characters to be used as a boundary. <br/> <br/> If no <b>tokens</b> character is specified, any whitespace character is used to split.
	 * Whitespace characters include tab (\\t), line feed (\\n), carriage return (\\r), form feed (\\f), and space. To convert a String to an
	 * array of integers or floats, use the datatype conversion functions <b>int()</b> and <b>float()</b> to convert the array of Strings.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value the String to be split
	 * @param delim list of individual characters that will be used as separators
	 * @webref data:string_functions
	 * @see processing.core.PApplet#split(String, String)
	 * @see processing.core.PApplet#join(String[], String)
	 * @see processing.core.PApplet#trim(String)
	 */
	public String[] splitTokens(String value, String delim) {
		return PApplet.splitTokens(value, delim);
	}

	/**
	 * ( begin auto-generated from split.xml )
	 * <p>
	 * The split() function breaks a string into pieces using a character or string as the divider. The <b>delim</b> parameter specifies the
	 * character or characters that mark the boundaries between each piece. A String[] array is returned that contains each of the pieces. <br/>
	 * <br/> If the result is a set of numbers, you can convert the String[] array to to a float[] or int[] array using the datatype conversion
	 * functions <b>int()</b> and <b>float()</b> (see example above). <br/> <br/> The <b>splitTokens()</b> function works in a similar fashion,
	 * except that it splits using a range of characters instead of a specific character or sequence. <!-- /><br /> This function uses regular
	 * expressions to determine how the <b>delim</b> parameter divides the <b>str</b> parameter. Therefore, if you use characters such parentheses
	 * and brackets that are used with regular expressions as a part of the <b>delim</b> parameter, you'll need to put two blackslashes (\\\\) in
	 * front of the character (see example above). You can read more about <a href="http://en.wikipedia.org/wiki/Regular_expression">regular
	 * expressions</a> and <a href="http://en.wikipedia.org/wiki/Escape_character">escape characters</a> on Wikipedia. -->
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value the String to be split
	 * @param delim the character or String used to separate the data
	 * @webref data:string_functions
	 * @usage web_application
	 */
	public String[] split(String value, char delim) {
		return PApplet.split(value, delim);
	}

	public String[] split(String value, String delim) {
		return PApplet.split(value, delim);
	}

	/**
	 * ( begin auto-generated from match.xml )
	 * <p>
	 * The match() function is used to apply a regular expression to a piece of text, and return matching groups (elements found inside
	 * parentheses) as a String array. No match will return null. If no groups are specified in the regexp, but the sequence matches, an array of
	 * length one (with the matched text as the first element of the array) will be returned.<br /> <br /> To use the function, first check to see
	 * if the result is null. If the result is null, then the sequence did not match. If the sequence did match, an array is returned. If there
	 * are groups (specified by sets of parentheses) in the regexp, then the contents of each will be returned in the array. Element [0] of a
	 * regexp match returns the entire matching string, and the match groups start at element [1] (the first group is [1], the second [2], and so
	 * on).<br /> <br /> The syntax can be found in the reference for Java's <a href="http://download.oracle.com/javase/6/docs/api/">Pattern</a>
	 * class. For regular expression syntax, read the <a href="http://download.oracle.com/javase/tutorial/essential/regex/">Java Tutorial</a> on
	 * the topic.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param str    the String to be searched
	 * @param regexp the regexp to be used for matching
	 * @webref data:string_functions
	 * @see processing.core.PApplet#matchAll(String, String)
	 * @see processing.core.PApplet#split(String, String)
	 * @see processing.core.PApplet#splitTokens(String, String)
	 * @see processing.core.PApplet#join(String[], String)
	 * @see processing.core.PApplet#trim(String)
	 */
	public String[] match(String str, String regexp) {
		return PApplet.match(str, regexp);
	}

	/**
	 * ( begin auto-generated from matchAll.xml )
	 * <p>
	 * This function is used to apply a regular expression to a piece of text, and return a list of matching groups (elements found inside
	 * parentheses) as a two-dimensional String array. No matches will return null. If no groups are specified in the regexp, but the sequence
	 * matches, a two dimensional array is still returned, but the second dimension is only of length one.<br /> <br /> To use the function, first
	 * check to see if the result is null. If the result is null, then the sequence did not match at all. If the sequence did match, a 2D array is
	 * returned. If there are groups (specified by sets of parentheses) in the regexp, then the contents of each will be returned in the array.
	 * Assuming, a loop with counter variable i, element [i][0] of a regexp match returns the entire matching string, and the match groups start
	 * at element [i][1] (the first group is [i][1], the second [i][2], and so on).<br /> <br /> The syntax can be found in the reference for
	 * Java's <a href="http://download.oracle.com/javase/6/docs/api/">Pattern</a> class. For regular expression syntax, read the <a
	 * href="http://download.oracle.com/javase/tutorial/essential/regex/">Java Tutorial</a> on the topic.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param str    the String to be searched
	 * @param regexp the regexp to be used for matching
	 * @webref data:string_functions
	 * @see processing.core.PApplet#match(String, String)
	 * @see processing.core.PApplet#split(String, String)
	 * @see processing.core.PApplet#splitTokens(String, String)
	 * @see processing.core.PApplet#join(String[], String)
	 * @see processing.core.PApplet#trim(String)
	 */
	public String[][] matchAll(String str, String regexp) {
		return PApplet.matchAll(str, regexp);
	}

	/**
	 * <p>Convert an integer to a boolean. Because of how Java handles upgrading numbers, this will also cover byte and char (as they will upgrade
	 * to an int without any sort of explicit cast).</p> <p>The preprocessor will convert boolean(what) to parseBoolean(what).</p>
	 *
	 * @param what
	 * @return false if 0, true if any other number
	 */
	public boolean parseBoolean(int what) {
		return PApplet.parseBoolean(what);
	}

	/**
	 * Convert the string "true" or "false" to a boolean.
	 *
	 * @param what
	 * @return true if 'what' is "true" or "TRUE", false otherwise
	 */
	public boolean parseBoolean(String what) {
		return PApplet.parseBoolean(what);
	}

	/**
	 * Convert an int array to a boolean array. An int equal to zero will return false, and any other value will return true.
	 *
	 * @param what
	 * @return array of boolean elements
	 */
	public boolean[] parseBoolean(int[] what) {
		return PApplet.parseBoolean(what);
	}

	public boolean[] parseBoolean(String[] what) {
		return PApplet.parseBoolean(what);
	}

	public byte parseByte(boolean what) {
		return PApplet.parseByte(what);
	}

	public byte parseByte(char what) {
		return PApplet.parseByte(what);
	}

	public byte parseByte(int what) {
		return PApplet.parseByte(what);
	}

	public byte parseByte(float what) {
		return PApplet.parseByte(what);
	}

	public byte[] parseByte(boolean[] what) {
		return PApplet.parseByte(what);
	}

	public byte[] parseByte(char[] what) {
		return PApplet.parseByte(what);
	}

	public byte[] parseByte(int[] what) {
		return PApplet.parseByte(what);
	}

	public byte[] parseByte(float[] what) {
		return PApplet.parseByte(what);
	}

	public char parseChar(byte what) {
		return PApplet.parseChar(what);
	}

	public char parseChar(int what) {
		return PApplet.parseChar(what);
	}

	public char[] parseChar(byte[] what) {
		return PApplet.parseChar(what);
	}

	public char[] parseChar(int[] what) {
		return PApplet.parseChar(what);
	}

	public int parseInt(boolean what) {
		return PApplet.parseInt(what);
	}

	/**
	 * Note that parseInt() will un-sign a signed byte value.
	 *
	 * @param what
	 */
	public int parseInt(byte what) {
		return PApplet.parseInt(what);
	}

	/**
	 * Note that parseInt('5') is unlike String in the sense that it won't return 5, but the ascii value. This is because ((int) someChar) returns
	 * the ascii value, and parseInt() is just longhand for the cast.
	 *
	 * @param what
	 */
	public int parseInt(char what) {
		return PApplet.parseInt(what);
	}

	/**
	 * Same as floor(), or an (int) cast.
	 *
	 * @param what
	 */
	public int parseInt(float what) {
		return PApplet.parseInt(what);
	}

	/**
	 * Parse a String into an int value. Returns 0 if the value is bad.
	 *
	 * @param what
	 */
	public int parseInt(String what) {
		return PApplet.parseInt(what);
	}

	/**
	 * Parse a String to an int, and provide an alternate value that should be used when the number is invalid.
	 *
	 * @param what
	 * @param otherwise
	 */
	public int parseInt(String what, int otherwise) {
		return PApplet.parseInt(what, otherwise);
	}

	public int[] parseInt(boolean[] what) {
		return PApplet.parseInt(what);
	}

	public int[] parseInt(byte[] what) {
		return PApplet.parseInt(what);
	}

	public int[] parseInt(char[] what) {
		return PApplet.parseInt(what);
	}

	public int[] parseInt(float[] what) {
		return PApplet.parseInt(what);
	}

	/**
	 * Make an array of int elements from an array of String objects. If the String can't be parsed as a number, it will be set to zero.
	 * <p>
	 * String s[] = { "1", "300", "44" }; int numbers[] = parseInt(s);
	 * <p>
	 * numbers will contain { 1, 300, 44 }
	 *
	 * @param what
	 */
	public int[] parseInt(String[] what) {
		return PApplet.parseInt(what);
	}

	/**
	 * Make an array of int elements from an array of String objects. If the String can't be parsed as a number, its entry in the array will be
	 * set to the value of the "missing" parameter.
	 * <p>
	 * String s[] = { "1", "300", "apple", "44" }; int numbers[] = parseInt(s, 9999);
	 * <p>
	 * numbers will contain { 1, 300, 9999, 44 }
	 *
	 * @param what
	 * @param missing
	 */
	public int[] parseInt(String[] what, int missing) {
		return PApplet.parseInt(what, missing);
	}

	/**
	 * Convert an int to a float value. Also handles bytes because of Java's rules for upgrading values.
	 *
	 * @param what
	 */
	public float parseFloat(int what) {
		return PApplet.parseFloat(what);
	}

	public float parseFloat(String what) {
		return PApplet.parseFloat(what);
	}

	public float parseFloat(String what, float otherwise) {
		return PApplet.parseFloat(what, otherwise);
	}

	public float[] parseByte(byte[] what) {
		return PApplet.parseByte(what);
	}

	public float[] parseFloat(int[] what) {
		return PApplet.parseFloat(what);
	}

	public float[] parseFloat(String[] what) {
		return PApplet.parseFloat(what);
	}

	public float[] parseFloat(String[] what, float missing) {
		return PApplet.parseFloat(what, missing);
	}

	public String str(boolean x) {
		return PApplet.str(x);
	}

	public String str(byte x) {
		return PApplet.str(x);
	}

	public String str(char x) {
		return PApplet.str(x);
	}

	public String str(int x) {
		return PApplet.str(x);
	}

	public String str(float x) {
		return PApplet.str(x);
	}

	public String[] str(boolean[] x) {
		return PApplet.str(x);
	}

	public String[] str(byte[] x) {
		return PApplet.str(x);
	}

	public String[] str(char[] x) {
		return PApplet.str(x);
	}

	public String[] str(int[] x) {
		return PApplet.str(x);
	}

	public String[] str(float[] x) {
		return PApplet.str(x);
	}

	public String[] nf(int[] num, int digits) {
		return PApplet.nf(num, digits);
	}

	/**
	 * ( begin auto-generated from nf.xml )
	 * <p>
	 * Utility function for formatting numbers into strings. There are two versions, one for formatting floats and one for formatting ints. The
	 * values for the <b>digits</b>, <b>left</b>, and <b>right</b> parameters should always be positive integers.<br /><br />As shown in the above
	 * example, <b>nf()</b> is used to add zeros to the left and/or right of a number. This is typically for aligning a list of numbers. To
	 * <em>remove</em> digits from a floating-point number, use the <b>int()</b>, <b>ceil()</b>, <b>floor()</b>, or <b>round()</b> functions.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param num    the number(s) to format
	 * @param digits number of digits to pad with zero
	 * @webref data:string_functions
	 * @see processing.core.PApplet#nfs(float, int, int)
	 * @see processing.core.PApplet#nfp(float, int, int)
	 * @see processing.core.PApplet#nfc(float, int)
	 * @see processing.core.PApplet#int(float)
	 */
	public String nf(int num, int digits) {
		return PApplet.nf(num, digits);
	}

	/**
	 * ( begin auto-generated from nfc.xml )
	 * <p>
	 * Utility function for formatting numbers into strings and placing appropriate commas to mark units of 1000. There are two versions, one for
	 * formatting ints and one for formatting an array of ints. The value for the <b>digits</b> parameter should always be a positive integer.
	 * <br/> <br/> For a non-US locale, this will insert periods instead of commas, or whatever is apprioriate for that region.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param num the number(s) to format
	 * @webref data:string_functions
	 * @see processing.core.PApplet#nf(float, int, int)
	 * @see processing.core.PApplet#nfp(float, int, int)
	 * @see processing.core.PApplet#nfc(float, int)
	 */
	public String[] nfc(int[] num) {
		return PApplet.nfc(num);
	}

	/**
	 * nfc() or "number format with commas". This is an unfortunate misnomer because in locales where a comma is not the separator for numbers, it
	 * won't actually be outputting a comma, it'll use whatever makes sense for the locale.
	 *
	 * @param num
	 */
	public String nfc(int num) {
		return PApplet.nfc(num);
	}

	/**
	 * ( begin auto-generated from nfs.xml )
	 * <p>
	 * Utility function for formatting numbers into strings. Similar to <b>nf()</b> but leaves a blank space in front of positive numbers so they
	 * align with negative numbers in spite of the minus symbol. There are two versions, one for formatting floats and one for formatting ints.
	 * The values for the <b>digits</b>, <b>left</b>, and <b>right</b> parameters should always be positive integers.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param num    the number(s) to format
	 * @param digits number of digits to pad with zeroes
	 * @webref data:string_functions
	 * @see processing.core.PApplet#nf(float, int, int)
	 * @see processing.core.PApplet#nfp(float, int, int)
	 * @see processing.core.PApplet#nfc(float, int)
	 */
	public String nfs(int num, int digits) {
		return PApplet.nfs(num, digits);
	}

	public String[] nfs(int[] num, int digits) {
		return PApplet.nfs(num, digits);
	}

	/**
	 * ( begin auto-generated from nfp.xml )
	 * <p>
	 * Utility function for formatting numbers into strings. Similar to <b>nf()</b> but puts a "+" in front of positive numbers and a "-" in front
	 * of negative numbers. There are two versions, one for formatting floats and one for formatting ints. The values for the <b>digits</b>,
	 * <b>left</b>, and <b>right</b> parameters should always be positive integers.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param num    the number(s) to format
	 * @param digits number of digits to pad with zeroes
	 * @webref data:string_functions
	 * @see processing.core.PApplet#nf(float, int, int)
	 * @see processing.core.PApplet#nfs(float, int, int)
	 * @see processing.core.PApplet#nfc(float, int)
	 */
	public String nfp(int num, int digits) {
		return PApplet.nfp(num, digits);
	}

	public String[] nfp(int[] num, int digits) {
		return PApplet.nfp(num, digits);
	}

	public String[] nf(float[] num, int left, int right) {
		return PApplet.nf(num, left, right);
	}

	/**
	 * @param num
	 * @param left  number of digits to the left of the decimal point
	 * @param right number of digits to the right of the decimal point
	 */
	public String nf(float num, int left, int right) {
		return PApplet.nf(num, left, right);
	}

	/**
	 * @param num
	 * @param right number of digits to the right of the decimal point
	 */
	public String[] nfc(float[] num, int right) {
		return PApplet.nfc(num, right);
	}

	public String nfc(float num, int right) {
		return PApplet.nfc(num, right);
	}

	/**
	 * @param num
	 * @param left  the number of digits to the left of the decimal point
	 * @param right the number of digits to the right of the decimal point
	 */
	public String[] nfs(float[] num, int left, int right) {
		return PApplet.nfs(num, left, right);
	}

	public String nfs(float num, int left, int right) {
		return PApplet.nfs(num, left, right);
	}

	/**
	 * @param num
	 * @param left  the number of digits to the left of the decimal point
	 * @param right the number of digits to the right of the decimal point
	 */
	public String[] nfp(float[] num, int left, int right) {
		return PApplet.nfp(num, left, right);
	}

	public String nfp(float num, int left, int right) {
		return PApplet.nfp(num, left, right);
	}

	/**
	 * ( begin auto-generated from hex.xml )
	 * <p>
	 * Converts a byte, char, int, or color to a String containing the equivalent hexadecimal notation. For example color(0, 102, 153) will
	 * convert to the String "FF006699". This function can help make your geeky debugging sessions much happier. <br/> <br/> Note that the maximum
	 * number of digits is 8, because an int value can only represent up to 32 bits. Specifying more than eight digits will simply shorten the
	 * string to eight anyway.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value the value to convert
	 * @webref data:conversion
	 * @see processing.core.PApplet#unhex(String)
	 * @see processing.core.PApplet#binary(byte)
	 * @see processing.core.PApplet#unbinary(String)
	 */
	public String hex(byte value) {
		return PApplet.hex(value);
	}

	public String hex(char value) {
		return PApplet.hex(value);
	}

	public String hex(int value) {
		return PApplet.hex(value);
	}

	/**
	 * @param value
	 * @param digits the number of digits (maximum 8)
	 */
	public String hex(int value, int digits) {
		return PApplet.hex(value, digits);
	}

	/**
	 * ( begin auto-generated from unhex.xml )
	 * <p>
	 * Converts a String representation of a hexadecimal number to its equivalent integer value.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value String to convert to an integer
	 * @webref data:conversion
	 * @see processing.core.PApplet#hex(int, int)
	 * @see processing.core.PApplet#binary(byte)
	 * @see processing.core.PApplet#unbinary(String)
	 */
	public int unhex(String value) {
		return PApplet.unhex(value);
	}

	/**
	 * Returns a String that contains the binary value of a byte. The returned value will always have 8 digits.
	 *
	 * @param value
	 */
	public String binary(byte value) {
		return PApplet.binary(value);
	}

	/**
	 * Returns a String that contains the binary value of a char. The returned value will always have 16 digits because chars are two bytes long.
	 *
	 * @param value
	 */
	public String binary(char value) {
		return PApplet.binary(value);
	}

	/**
	 * Returns a String that contains the binary value of an int. The length depends on the size of the number itself. If you want a specific
	 * number of digits use binary(int what, int digits) to specify how many.
	 *
	 * @param value
	 */
	public String binary(int value) {
		return PApplet.binary(value);
	}

	/**
	 * ( begin auto-generated from binary.xml )
	 * <p>
	 * Converts a byte, char, int, or color to a String containing the equivalent binary notation. For example color(0, 102, 153, 255) will
	 * convert to the String "11111111000000000110011010011001". This function can help make your geeky debugging sessions much happier. <br/>
	 * <br/> Note that the maximum number of digits is 32, because an int value can only represent up to 32 bits. Specifying more than 32 digits
	 * will simply shorten the string to 32 anyway.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value  value to convert
	 * @param digits number of digits to return
	 * @webref data:conversion
	 * @see processing.core.PApplet#unbinary(String)
	 * @see processing.core.PApplet#hex(int, int)
	 * @see processing.core.PApplet#unhex(String)
	 */
	public String binary(int value, int digits) {
		return PApplet.binary(value, digits);
	}

	/**
	 * ( begin auto-generated from unbinary.xml )
	 * <p>
	 * Converts a String representation of a binary number to its equivalent integer value. For example, unbinary("00001000") will return 8.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param value String to convert to an integer
	 * @webref data:conversion
	 * @see processing.core.PApplet#binary(byte)
	 * @see processing.core.PApplet#hex(int, int)
	 * @see processing.core.PApplet#unhex(String)
	 */
	public int unbinary(String value) {
		return PApplet.unbinary(value);
	}

	/**
	 * ( begin auto-generated from color.xml )
	 * <p>
	 * Creates colors for storing in variables of the <b>color</b> datatype. The parameters are interpreted as RGB or HSB values depending on the
	 * current <b>colorMode()</b>. The default mode is RGB values from 0 to 255 and therefore, the function call <b>color(255, 204, 0)</b> will
	 * return a bright yellow color. More about how colors are stored can be found in the reference for the <a
	 * href="color_datatype.html">color</a> datatype.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param gray number specifying value between white and black
	 * @webref color:creating_reading
	 * @see processing.core.PApplet#colorMode(int)
	 */
	public int color(int gray) {
		return applet.color(gray);
	}

	/**
	 * @param fgray number specifying value between white and black
	 * @nowebref
	 */
	public int color(float fgray) {
		return applet.color(fgray);
	}

	/**
	 * As of 0116 this also takes color(#FF8800, alpha)
	 *
	 * @param gray
	 * @param alpha relative to current color range
	 */
	public int color(int gray, int alpha) {
		return applet.color(gray, alpha);
	}

	/**
	 * @param fgray
	 * @param falpha
	 * @nowebref
	 */
	public int color(float fgray, float falpha) {
		return applet.color(fgray, falpha);
	}

	/**
	 * @param v1 red or hue values relative to the current color range
	 * @param v2 green or saturation values relative to the current color range
	 * @param v3 blue or brightness values relative to the current color range
	 */
	public int color(int v1, int v2, int v3) {
		return applet.color(v1, v2, v3);
	}

	public int color(int v1, int v2, int v3, int alpha) {
		return applet.color(v1, v2, v3, alpha);
	}

	public int color(float v1, float v2, float v3) {
		return applet.color(v1, v2, v3);
	}

	public int color(float v1, float v2, float v3, float alpha) {
		return applet.color(v1, v2, v3, alpha);
	}

	public int blendColor(int c1, int c2, int mode) {
		return PApplet.blendColor(c1, c2, mode);
	}

	/**
	 * Set this sketch to communicate its state back to the PDE.
	 * <p>
	 * This uses the stderr stream to write positions of the window (so that it will be saved by the PDE for the next run) and notify on quit. See
	 * more notes in the Worker class.
	 */
	public void setupExternalMessages() {
		applet.setupExternalMessages();
	}

	/**
	 * Set up a listener that will fire proper component resize events in cases where frame.setResizable(true) is called.
	 */
	public void setupFrameResizeListener() {
		applet.setupFrameResizeListener();
	}

	/**
	 * main() method for running this class from the command line.
	 * <p>
	 * <B>The options shown here are not yet public final ized and will be changing over the next several releases.</B>
	 * <p>
	 * The simplest way to turn and __applet into an application is to add the following code to your program: <PRE> public void main(String args[])
	 * { PApplet.main("YourSketchName", args); }</PRE> This will properly launch your __applet from a double-clickable .jar or from the command
	 * line. <PRE> Parameters useful for launching or also used by the PDE:
	 * <p>
	 * --location=x,y        upper-lefthand corner of where the __applet should appear on screen. if not used, the default is to center on the main
	 * screen.
	 * <p>
	 * --full-screen         put the __applet into full screen "present" mode.
	 * <p>
	 * --hide-stop           use to hide the stop button in situations where you don't want to allow users to exit. also see the FAQ on
	 * information for capturing the ESC key when running in presentation mode.
	 * <p>
	 * --stop-color=#xxxxxx  color of the 'stop' text used to quit an sketch when it's in present mode.
	 * <p>
	 * --bgcolor=#xxxxxx     background color of the window.
	 * <p>
	 * --sketch-path         location of where to save files from functions like saveStrings() or saveFrame(). defaults to the folder that the
	 * java application was launched from, which means if this isn't set by the pde, everything goes into the same folder as processing.exe.
	 * <p>
	 * --display=n           set what display should be used by this sketch. displays are numbered starting from 0.
	 * <p>
	 * Parameters used by Processing when running via the PDE
	 * <p>
	 * --external            set when the __applet is being used by the PDE
	 * <p>
	 * --editor-location=x,y position of the upper-lefthand corner of the editor window, for placement of __applet window </PRE>
	 *
	 * @param args
	 */
	public void main(String[] args) {
		PApplet.main(args);
	}

	/**
	 * Convenience method so that PApplet.main("YourSketch") launches a sketch, rather than having to wrap it into a String array.
	 *
	 * @param mainClass name of the class to load (with package if any)
	 */
	public void main(String mainClass) {
		PApplet.main(mainClass);
	}

	/**
	 * Convenience method so that PApplet.main("YourSketch", args) launches a sketch, rather than having to wrap it into a String array, and
	 * appending the 'args' array when not null.
	 *
	 * @param mainClass  name of the class to load (with package if any)
	 * @param passedArgs
	 */
	public void main(String mainClass, String[] passedArgs) {
		PApplet.main(mainClass, passedArgs);
	}

	public void runSketch(String[] args, PApplet constructedApplet) {
		PApplet.runSketch(args, constructedApplet);
	}

	/**
	 * ( begin auto-generated from beginRecord.xml )
	 * <p>
	 * Opens a new file and all subsequent drawing functions are echoed to this file as well as the display window. The <b>beginRecord()</b>
	 * function requires two parameters, the first is the renderer and the second is the file name. This function is always used with
	 * <b>endRecord()</b> to stop the recording process and close the file. <br /> <br /> Note that beginRecord() will only pick up any settings
	 * that happen after it has been called. For instance, if you call textFont() before beginRecord(), then that font will not be set for the
	 * file that you're recording to.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param renderer for example, PDF
	 * @param filename filename for output
	 * @webref output:files
	 * @see processing.core.PApplet#endRecord()
	 */
	public PGraphics beginRecord(String renderer, String filename) {
		return applet.beginRecord(renderer, filename);
	}

	/**
	 * @param recorder
	 * @nowebref Begin recording (echoing) commands to the specified PGraphics object.
	 */
	public void beginRecord(PGraphics recorder) {
		applet.beginRecord(recorder);
	}

	/**
	 * ( begin auto-generated from endRecord.xml )
	 * <p>
	 * Stops the recording process started by <b>beginRecord()</b> and closes the file.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref output:files
	 * @see processing.core.PApplet#beginRecord(String, String)
	 */
	public void endRecord() {
		applet.endRecord();
	}

	/**
	 * ( begin auto-generated from beginRaw.xml )
	 * <p>
	 * To create vectors from 3D data, use the <b>beginRaw()</b> and <b>endRaw()</b> commands. These commands will grab the shape data just before
	 * it is rendered to the screen. At this stage, your entire scene is nothing but a long list of individual lines and triangles. This means
	 * that a shape created with <b>sphere()</b> function will be made up of hundreds of triangles, rather than a single object. Or that a
	 * multi-segment line shape (such as a curve) will be rendered as individual segments. <br /><br /> When using <b>beginRaw()</b> and
	 * <b>endRaw()</b>, it's possible to write to either a 2D or 3D renderer. For instance, <b>beginRaw()</b> with the PDF library will write the
	 * geometry as flattened triangles and lines, even if recording from the <b>P3D</b> renderer. <br /><br /> If you want a background to show up
	 * in your files, use <b>rect(0, 0, width, height)</b> after setting the <b>fill()</b> to the background color. Otherwise the background will
	 * not be rendered to the file because the background is not shape. <br /><br /> Using <b>hint(ENABLE_DEPTH_SORT)</b> can improve the
	 * appearance of 3D geometry drawn to 2D file formats. See the <b>hint()</b> reference for more details. <br /><br /> See examples in the
	 * reference for the <b>PDF</b> and <b>DXF</b> libraries for more information.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param renderer for example, PDF or DXF
	 * @param filename filename for output
	 * @webref output:files
	 * @see processing.core.PApplet#endRaw()
	 * @see processing.core.PApplet#hint(int)
	 */
	public PGraphics beginRaw(String renderer, String filename) {
		return applet.beginRaw(renderer, filename);
	}

	/**
	 * @param rawGraphics ???
	 * @nowebref Begin recording raw shape data to the specified renderer.
	 * <p>
	 * This simply echoes to g.beginRaw(), but since is placed here (rather than generated by preproc.pl) for clarity and so that it doesn't echo
	 * the command should beginRecord() be in use.
	 */
	public void beginRaw(PGraphics rawGraphics) {
		applet.beginRaw(rawGraphics);
	}

	/**
	 * ( begin auto-generated from endRaw.xml )
	 * <p>
	 * Complement to <b>beginRaw()</b>; they must always be used together. See the <b>beginRaw()</b> reference for details.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref output:files
	 * @see processing.core.PApplet#beginRaw(String, String)
	 */
	public void endRaw() {
		applet.endRaw();
	}

	/**
	 * ( begin auto-generated from loadPixels.xml )
	 * <p>
	 * Loads the pixel data for the display window into the <b>pixels[]</b> array. This function must always be called before reading from or
	 * writing to <b>pixels[]</b>. <br/><br/> renderers may or may not seem to require <b>loadPixels()</b> or <b>updatePixels()</b>. However, the
	 * rule is that any time you want to manipulate the <b>pixels[]</b> array, you must first call <b>loadPixels()</b>, and after changes have
	 * been made, call <b>updatePixels()</b>. Even if the renderer may not seem to use this function in the current Processing release, this will
	 * always be subject to change.
	 * <p>
	 * ( end auto-generated ) <h3>Advanced</h3> Override the g.pixels[] function to set the pixels[] array that's part of the PApplet object.
	 * Allows the use of pixels[] in the code, rather than g.pixels[].
	 *
	 * @webref image:pixels
	 * @see processing.core.PApplet#pixels
	 * @see processing.core.PApplet#updatePixels()
	 */
	public void loadPixels() {
		applet.loadPixels();
	}

	/**
	 * ( begin auto-generated from updatePixels.xml )
	 * <p>
	 * Updates the display window with the data in the <b>pixels[]</b> array. Use in conjunction with <b>loadPixels()</b>. If you're only reading
	 * pixels from the array, there's no need to call <b>updatePixels()</b> unless there are changes. <br/><br/> renderers may or may not seem to
	 * require <b>loadPixels()</b> or <b>updatePixels()</b>. However, the rule is that any time you want to manipulate the <b>pixels[]</b> array,
	 * you must first call <b>loadPixels()</b>, and after changes have been made, call <b>updatePixels()</b>. Even if the renderer may not seem to
	 * use this function in the current Processing release, this will always be subject to change. <br/> <br/> Currently, none of the renderers
	 * use the additional parameters to <b>updatePixels()</b>, however this may be implemented in the future.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref image:pixels
	 * @see processing.core.PApplet#loadPixels()
	 * @see processing.core.PApplet#pixels
	 */
	public void updatePixels() {
		applet.updatePixels();
	}

	/**
	 * @param x1 x-coordinate of the upper-left corner
	 * @param y1 y-coordinate of the upper-left corner
	 * @param x2 width of the region
	 * @param y2 height of the region
	 * @nowebref
	 */
	public void updatePixels(int x1, int y1, int x2, int y2) {
		applet.updatePixels(x1, y1, x2, y2);
	}

	/**
	 * Store data of some kind for the renderer that requires extra metadata of some kind. Usually this is a renderer-specific representation of
	 * the image data, for instance a BufferedImage with tint() settings applied for PGraphicsJava2D, or resized image data and OpenGL texture
	 * indices for PGraphicsOpenGL.
	 *
	 * @param image
	 * @param storage The metadata required by the renderer
	 */
	public void setCache(PImage image, Object storage) {
		applet.setCache(image, storage);
	}

	/**
	 * Get cache storage data for the specified renderer. Because each renderer will cache data in different formats, it's necessary to store
	 * cache data keyed by the renderer object. Otherwise, attempting to draw the same image to both a PGraphicsJava2D and a PGraphicsOpenGL will
	 * cause errors.
	 *
	 * @param image@return metadata stored for the specified renderer
	 */
	public Object getCache(PImage image) {
		return applet.getCache(image);
	}

	/**
	 * Remove information associated with this renderer from the cache, if any.
	 *
	 * @param image
	 */
	public void removeCache(PImage image) {
		applet.removeCache(image);
	}

	public PGL beginPGL() {
		return applet.beginPGL();
	}

	public void endPGL() {
		applet.endPGL();
	}

	public void flush() {
		applet.flush();
	}

	public void hint(int which) {
		applet.hint(which);
	}

	/**
	 * Start a new shape of type POLYGON
	 */
	public void beginShape() {
		applet.beginShape();
	}

	/**
	 * ( begin auto-generated from beginShape.xml )
	 * <p>
	 * Using the <b>beginShape()</b> and <b>endShape()</b> functions allow creating more complex forms. <b>beginShape()</b> begins recording
	 * vertices for a shape and <b>endShape()</b> stops recording. The value of the <b>MODE</b> parameter tells it which types of shapes to create
	 * from the provided vertices. With no mode specified, the shape can be any irregular polygon. The parameters available for beginShape() are
	 * POINTS, LINES, TRIANGLES, TRIANGLE_FAN, TRIANGLE_STRIP, QUADS, and QUAD_STRIP. After calling the <b>beginShape()</b> function, a series of
	 * <b>vertex()</b> commands must follow. To stop drawing the shape, call <b>endShape()</b>. The <b>vertex()</b> function with two parameters
	 * specifies a position in 2D and the <b>vertex()</b> function with three parameters specifies a position in 3D. Each shape will be outlined
	 * with the current stroke color and filled with the fill color. <br/> <br/> Transformations such as <b>translate()</b>, <b>rotate()</b>, and
	 * <b>scale()</b> do not work within <b>beginShape()</b>. It is also not possible to use other shapes, such as <b>ellipse()</b> or
	 * <b>rect()</b> within <b>beginShape()</b>. <br/> <br/> The P3D renderer settings allow <b>stroke()</b> and <b>fill()</b> settings to be
	 * altered per-vertex, however the default P2D renderer does not. Settings such as <b>strokeWeight()</b>, <b>strokeCap()</b>, and
	 * <b>strokeJoin()</b> cannot be changed while inside a <b>beginShape()</b>/<b>endShape()</b> block with any renderer.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param kind Either POINTS, LINES, TRIANGLES, TRIANGLE_FAN, TRIANGLE_STRIP, QUADS, or QUAD_STRIP
	 * @webref shape:vertex
	 * @see PShape
	 * @see processing.core.PGraphics#endShape()
	 * @see processing.core.PGraphics#vertex(float, float, float, float, float)
	 * @see processing.core.PGraphics#curveVertex(float, float, float)
	 * @see processing.core.PGraphics#bezierVertex(float, float, float, float, float, float, float, float, float)
	 */
	public void beginShape(int kind) {
		applet.beginShape(kind);
	}

	/**
	 * Sets whether the upcoming vertex is part of an edge. Equivalent to glEdgeFlag(), for people familiar with OpenGL.
	 *
	 * @param edge
	 */
	public void edge(boolean edge) {
		applet.edge(edge);
	}

	/**
	 * ( begin auto-generated from normal.xml )
	 * <p>
	 * Sets the current normal vector. This is for drawing three dimensional shapes and surfaces and specifies a vector perpendicular to the
	 * surface of the shape which determines how lighting affects it. Processing attempts to automatically assign normals to shapes, but since
	 * that's imperfect, this is a better option when you want more control. This function is identical to glNormal3f() in OpenGL.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param nx x direction
	 * @param ny y direction
	 * @param nz z direction
	 * @webref lights_camera:lights
	 * @see processing.core.PGraphics#beginShape(int)
	 * @see processing.core.PGraphics#endShape(int)
	 * @see processing.core.PGraphics#lights()
	 */
	public void normal(float nx, float ny, float nz) {
		applet.normal(nx, ny, nz);
	}

	/**
	 * ( begin auto-generated from textureMode.xml )
	 * <p>
	 * Sets the coordinate space for texture mapping. There are two options, IMAGE, which refers to the actual coordinates of the image, and
	 * NORMAL, which refers to a normalized space of values ranging from 0 to 1. The default mode is IMAGE. In IMAGE, if an image is 100 x 200
	 * pixels, mapping the image onto the entire size of a quad would require the points (0,0) (0,100) (100,200) (0,200). The same mapping in
	 * NORMAL_SPACE is (0,0) (0,1) (1,1) (0,1).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode either IMAGE or NORMAL
	 * @webref image:textures
	 * @see processing.core.PGraphics#texture(processing.core.PImage)
	 * @see processing.core.PGraphics#textureWrap(int)
	 */
	public void textureMode(int mode) {
		applet.textureMode(mode);
	}

	/**
	 * ( begin auto-generated from textureWrap.xml )
	 * <p>
	 * Description to come...
	 * <p>
	 * ( end auto-generated from textureWrap.xml )
	 *
	 * @param wrap Either CLAMP (default) or REPEAT
	 * @webref image:textures
	 * @see processing.core.PGraphics#texture(processing.core.PImage)
	 * @see processing.core.PGraphics#textureMode(int)
	 */
	public void textureWrap(int wrap) {
		applet.textureWrap(wrap);
	}

	/**
	 * ( begin auto-generated from texture.xml )
	 * <p>
	 * Sets a texture to be applied to vertex points. The <b>texture()</b> function must be called between <b>beginShape()</b> and
	 * <b>endShape()</b> and before any calls to <b>vertex()</b>. <br/> <br/> When textures are in use, the fill color is ignored. Instead, use
	 * tint() to specify the color of the texture as it is applied to the shape.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param image reference to a PImage object
	 * @webref image:textures
	 * @see processing.core.PGraphics#textureMode(int)
	 * @see processing.core.PGraphics#textureWrap(int)
	 * @see processing.core.PGraphics#beginShape(int)
	 * @see processing.core.PGraphics#endShape(int)
	 * @see processing.core.PGraphics#vertex(float, float, float, float, float)
	 */
	public void texture(PImage image) {
		applet.texture(image);
	}

	/**
	 * Removes texture image for current shape. Needs to be called between beginShape and endShape
	 */
	public void noTexture() {
		applet.noTexture();
	}

	public void vertex(float x, float y) {
		applet.vertex(x, y);
	}

	public void vertex(float x, float y, float z) {
		applet.vertex(x, y, z);
	}

	/**
	 * Used by renderer subclasses or PShape to efficiently pass in already formatted vertex information.
	 *
	 * @param v vertex parameters, as a float array of length VERTEX_FIELD_COUNT
	 */
	public void vertex(float[] v) {
		applet.vertex(v);
	}

	public void vertex(float x, float y, float u, float v) {
		applet.vertex(x, y, u, v);
	}

	/**
	 * ( begin auto-generated from vertex.xml )
	 * <p>
	 * All shapes are constructed by connecting a series of vertices. <b>vertex()</b> is used to specify the vertex coordinates for points, lines,
	 * triangles, quads, and polygons and is used exclusively within the <b>beginShape()</b> and <b>endShape()</b> function.<br /> <br /> Drawing
	 * a vertex in 3D using the <b>z</b> parameter requires the P3D parameter in combination with size as shown in the above example.<br /> <br />
	 * This function is also used to map a texture onto the geometry. The <b>texture()</b> function declares the texture to apply to the geometry
	 * and the <b>u</b> and <b>v</b> coordinates set define the mapping of this texture to the form. By default, the coordinates used for <b>u</b>
	 * and <b>v</b> are specified in relation to the image's size in pixels, but this relation can be changed with <b>textureMode()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x x-coordinate of the vertex
	 * @param y y-coordinate of the vertex
	 * @param z z-coordinate of the vertex
	 * @param u horizontal coordinate for the texture mapping
	 * @param v vertical coordinate for the texture mapping
	 * @webref shape:vertex
	 * @see processing.core.PGraphics#beginShape(int)
	 * @see processing.core.PGraphics#endShape(int)
	 * @see processing.core.PGraphics#bezierVertex(float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#quadraticVertex(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curveVertex(float, float, float)
	 * @see processing.core.PGraphics#texture(processing.core.PImage)
	 */
	public void vertex(float x, float y, float z, float u, float v) {
		applet.vertex(x, y, z, u, v);
	}

	/**
	 * @webref shape:vertex
	 */
	public void beginContour() {
		applet.beginContour();
	}

	/**
	 * @webref shape:vertex
	 */
	public void endContour() {
		applet.endContour();
	}

	public void endShape() {
		applet.endShape();
	}

	/**
	 * ( begin auto-generated from endShape.xml )
	 * <p>
	 * The <b>endShape()</b> function is the companion to <b>beginShape()</b> and may only be called after <b>beginShape()</b>. When
	 * <b>endshape()</b> is called, all of image data defined since the previous call to <b>beginShape()</b> is written into the image buffer. The
	 * constant CLOSE as the value for the MODE parameter to close the shape (to connect the beginning and the end).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode use CLOSE to close the shape
	 * @webref shape:vertex
	 * @see PShape
	 * @see processing.core.PGraphics#beginShape(int)
	 */
	public void endShape(int mode) {
		applet.endShape(mode);
	}

	/**
	 * @param filename name of file to load, can be .svg or .obj
	 * @webref shape
	 * @see PShape
	 * @see processing.core.PApplet#createShape()
	 */
	public PShape loadShape(String filename) {
		return applet.loadShape(filename);
	}

	public PShape loadShape(String filename, String options) {
		return applet.loadShape(filename, options);
	}

	/**
	 * @webref shape
	 * @see processing.core.PShape
	 * @see processing.core.PShape#endShape()
	 * @see processing.core.PApplet#loadShape(String)
	 */
	public PShape createShape() {
		return applet.createShape();
	}

	public PShape createShape(PShape source) {
		return applet.createShape(source);
	}

	/**
	 * @param type either POINTS, LINES, TRIANGLES, TRIANGLE_FAN, TRIANGLE_STRIP, QUADS, QUAD_STRIP
	 */
	public PShape createShape(int type) {
		return applet.createShape(type);
	}

	/**
	 * @param kind either LINE, TRIANGLE, RECT, ELLIPSE, ARC, SPHERE, BOX
	 * @param p    parameters that match the kind of shape
	 */
	public PShape createShape(int kind, float... p) {
		return applet.createShape(kind, p);
	}

	/**
	 * ( begin auto-generated from loadShader.xml )
	 * <p>
	 * This is a new reference entry for Processing 2.0. It will be updated shortly.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param fragFilename name of fragment shader file
	 * @webref rendering:shaders
	 */
	public PShader loadShader(String fragFilename) {
		return applet.loadShader(fragFilename);
	}

	/**
	 * @param fragFilename
	 * @param vertFilename name of vertex shader file
	 */
	public PShader loadShader(String fragFilename, String vertFilename) {
		return applet.loadShader(fragFilename, vertFilename);
	}

	/**
	 * ( begin auto-generated from shader.xml )
	 * <p>
	 * This is a new reference entry for Processing 2.0. It will be updated shortly.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param shader name of shader file
	 * @webref rendering:shaders
	 */
	public void shader(PShader shader) {
		applet.shader(shader);
	}

	/**
	 * @param shader
	 * @param kind   type of shader, either POINTS, LINES, or TRIANGLES
	 */
	public void shader(PShader shader, int kind) {
		applet.shader(shader, kind);
	}

	/**
	 * ( begin auto-generated from resetShader.xml )
	 * <p>
	 * This is a new reference entry for Processing 2.0. It will be updated shortly.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref rendering:shaders
	 */
	public void resetShader() {
		applet.resetShader();
	}

	/**
	 * @param kind type of shader, either POINTS, LINES, or TRIANGLES
	 */
	public void resetShader(int kind) {
		applet.resetShader(kind);
	}

	/**
	 * @param shader the fragment shader to apply
	 */
	public void filter(PShader shader) {
		applet.filter(shader);
	}

	public void clip(float a, float b, float c, float d) {
		applet.clip(a, b, c, d);
	}

	public void noClip() {
		applet.noClip();
	}

	/**
	 * ( begin auto-generated from blendMode.xml )
	 * <p>
	 * This is a new reference entry for Processing 2.0. It will be updated shortly.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode the blending mode to use
	 * @webref Rendering
	 */
	public void blendMode(int mode) {
		applet.blendMode(mode);
	}

	public void bezierVertex(float x2, float y2, float x3, float y3, float x4, float y4) {
		applet.bezierVertex(x2, y2, x3, y3, x4, y4);
	}

	/**
	 * ( begin auto-generated from bezierVertex.xml )
	 * <p>
	 * Specifies vertex coordinates for Bezier curves. Each call to <b>bezierVertex()</b> defines the position of two control points and one
	 * anchor point of a Bezier curve, adding a new segment to a line or shape. The first time <b>bezierVertex()</b> is used within a
	 * <b>beginShape()</b> call, it must be prefaced with a call to <b>vertex()</b> to set the first anchor point. This function must be used
	 * between <b>beginShape()</b> and <b>endShape()</b> and only when there is no MODE parameter specified to <b>beginShape()</b>. Using the 3D
	 * version requires rendering with P3D (see the Environment reference for more information).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x2 the x-coordinate of the 1st control point
	 * @param y2 the y-coordinate of the 1st control point
	 * @param z2 the z-coordinate of the 1st control point
	 * @param x3 the x-coordinate of the 2nd control point
	 * @param y3 the y-coordinate of the 2nd control point
	 * @param z3 the z-coordinate of the 2nd control point
	 * @param x4 the x-coordinate of the anchor point
	 * @param y4 the y-coordinate of the anchor point
	 * @param z4 the z-coordinate of the anchor point
	 * @webref shape:vertex
	 * @see processing.core.PGraphics#curveVertex(float, float, float)
	 * @see processing.core.PGraphics#vertex(float, float, float, float, float)
	 * @see processing.core.PGraphics#quadraticVertex(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
	 */
	public void bezierVertex(float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
		applet.bezierVertex(x2, y2, z2, x3, y3, z3, x4, y4, z4);
	}

	/**
	 * @param cx the x-coordinate of the control point
	 * @param cy the y-coordinate of the control point
	 * @param x3 the x-coordinate of the anchor point
	 * @param y3 the y-coordinate of the anchor point
	 * @webref shape:vertex
	 * @see processing.core.PGraphics#curveVertex(float, float, float)
	 * @see processing.core.PGraphics#vertex(float, float, float, float, float)
	 * @see processing.core.PGraphics#bezierVertex(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
	 */
	public void quadraticVertex(float cx, float cy, float x3, float y3) {
		applet.quadraticVertex(cx, cy, x3, y3);
	}

	/**
	 * @param cx
	 * @param cy
	 * @param cz the z-coordinate of the control point
	 * @param x3
	 * @param y3
	 * @param z3 the z-coordinate of the anchor point
	 */
	public void quadraticVertex(float cx, float cy, float cz, float x3, float y3, float z3) {
		applet.quadraticVertex(cx, cy, cz, x3, y3, z3);
	}

	/**
	 * ( begin auto-generated from curveVertex.xml )
	 * <p>
	 * Specifies vertex coordinates for curves. This function may only be used between <b>beginShape()</b> and <b>endShape()</b> and only when
	 * there is no MODE parameter specified to <b>beginShape()</b>. The first and last points in a series of <b>curveVertex()</b> lines will be
	 * used to guide the beginning and end of a the curve. A minimum of four points is required to draw a tiny curve between the second and third
	 * points. Adding a fifth point with <b>curveVertex()</b> will draw the curve between the second, third, and fourth points. The
	 * <b>curveVertex()</b> function is an implementation of Catmull-Rom splines. Using the 3D version requires rendering with P3D (see the
	 * Environment reference for more information).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x the x-coordinate of the vertex
	 * @param y the y-coordinate of the vertex
	 * @webref shape:vertex
	 * @see processing.core.PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#beginShape(int)
	 * @see processing.core.PGraphics#endShape(int)
	 * @see processing.core.PGraphics#vertex(float, float, float, float, float)
	 * @see processing.core.PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#quadraticVertex(float, float, float, float, float, float)
	 */
	public void curveVertex(float x, float y) {
		applet.curveVertex(x, y);
	}

	/**
	 * @param x
	 * @param y
	 * @param z the z-coordinate of the vertex
	 */
	public void curveVertex(float x, float y, float z) {
		applet.curveVertex(x, y, z);
	}

	/**
	 * ( begin auto-generated from point.xml )
	 * <p>
	 * Draws a point, a coordinate in space at the dimension of one pixel. The first parameter is the horizontal value for the point, the second
	 * value is the vertical value for the point, and the optional third value is the depth value. Drawing this shape in 3D with the <b>z</b>
	 * parameter requires the P3D parameter in combination with <b>size()</b> as shown in the above example.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x x-coordinate of the point
	 * @param y y-coordinate of the point
	 * @webref shape:2d_primitives
	 */
	public void point(float x, float y) {
		applet.point(x, y);
	}

	/**
	 * @param x
	 * @param y
	 * @param z z-coordinate of the point
	 */
	public void point(float x, float y, float z) {
		applet.point(x, y, z);
	}

	/**
	 * ( begin auto-generated from line.xml )
	 * <p>
	 * Draws a line (a direct path between two points) to the screen. The version of <b>line()</b> with four parameters draws the line in 2D.  To
	 * color a line, use the <b>stroke()</b> function. A line cannot be filled, therefore the <b>fill()</b> function will not affect the color of
	 * a line. 2D lines are drawn with a width of one pixel by default, but this can be changed with the <b>strokeWeight()</b> function. The
	 * version with six parameters allows the line to be placed anywhere within XYZ space. Drawing this shape in 3D with the <b>z</b> parameter
	 * requires the P3D parameter in combination with <b>size()</b> as shown in the above example.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x1 x-coordinate of the first point
	 * @param y1 y-coordinate of the first point
	 * @param x2 x-coordinate of the second point
	 * @param y2 y-coordinate of the second point
	 * @webref shape:2d_primitives
	 * @see processing.core.PGraphics#strokeWeight(float)
	 * @see processing.core.PGraphics#strokeJoin(int)
	 * @see processing.core.PGraphics#strokeCap(int)
	 * @see processing.core.PGraphics#beginShape()
	 */
	public void line(float x1, float y1, float x2, float y2) {
		applet.line(x1, y1, x2, y2);
	}

	/**
	 * @param x1
	 * @param y1
	 * @param z1 z-coordinate of the first point
	 * @param x2
	 * @param y2
	 * @param z2 z-coordinate of the second point
	 */
	public void line(float x1, float y1, float z1, float x2, float y2, float z2) {
		applet.line(x1, y1, z1, x2, y2, z2);
	}

	/**
	 * ( begin auto-generated from triangle.xml )
	 * <p>
	 * A triangle is a plane created by connecting three points. The first two arguments specify the first point, the middle two arguments specify
	 * the second point, and the last two arguments specify the third point.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x1 x-coordinate of the first point
	 * @param y1 y-coordinate of the first point
	 * @param x2 x-coordinate of the second point
	 * @param y2 y-coordinate of the second point
	 * @param x3 x-coordinate of the third point
	 * @param y3 y-coordinate of the third point
	 * @webref shape:2d_primitives
	 * @see processing.core.PApplet#beginShape()
	 */
	public void triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
		applet.triangle(x1, y1, x2, y2, x3, y3);
	}

	/**
	 * ( begin auto-generated from quad.xml )
	 * <p>
	 * A quad is a quadrilateral, a four sided polygon. It is similar to a rectangle, but the angles between its edges are not constrained to
	 * ninety degrees. The first pair of parameters (x1,y1) sets the first vertex and the subsequent pairs should proceed clockwise or
	 * counter-clockwise around the defined shape.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x1 x-coordinate of the first corner
	 * @param y1 y-coordinate of the first corner
	 * @param x2 x-coordinate of the second corner
	 * @param y2 y-coordinate of the second corner
	 * @param x3 x-coordinate of the third corner
	 * @param y3 y-coordinate of the third corner
	 * @param x4 x-coordinate of the fourth corner
	 * @param y4 y-coordinate of the fourth corner
	 * @webref shape:2d_primitives
	 */
	public void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		applet.quad(x1, y1, x2, y2, x3, y3, x4, y4);
	}

	/**
	 * ( begin auto-generated from rectMode.xml )
	 * <p>
	 * Modifies the location from which rectangles draw. The default mode is <b>rectMode(CORNER)</b>, which specifies the location to be the upper
	 * left corner of the shape and uses the third and fourth parameters of <b>rect()</b> to specify the width and height. The syntax
	 * <b>rectMode(CORNERS)</b> uses the first and second parameters of <b>rect()</b> to set the location of one corner and uses the third and
	 * fourth parameters to set the opposite corner. The syntax <b>rectMode(CENTER)</b> draws the image from its center point and uses the third
	 * and forth parameters of <b>rect()</b> to specify the image's width and height. The syntax <b>rectMode(RADIUS)</b> draws the image from its
	 * center point and uses the third and forth parameters of <b>rect()</b> to specify half of the image's width and height. The parameter must
	 * be written in ALL CAPS because Processing is a case sensitive language. Note: In version 125, the mode named CENTER_RADIUS was shortened to
	 * RADIUS.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode either CORNER, CORNERS, CENTER, or RADIUS
	 * @webref shape:attributes
	 * @see processing.core.PGraphics#rect(float, float, float, float)
	 */
	public void rectMode(int mode) {
		applet.rectMode(mode);
	}

	/**
	 * ( begin auto-generated from rect.xml )
	 * <p>
	 * Draws a rectangle to the screen. A rectangle is a four-sided shape with every angle at ninety degrees. By default, the first two parameters
	 * set the location of the upper-left corner, the third sets the width, and the fourth sets the height. These parameters may be changed with
	 * the <b>rectMode()</b> function.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param a x-coordinate of the rectangle by default
	 * @param b y-coordinate of the rectangle by default
	 * @param c width of the rectangle by default
	 * @param d height of the rectangle by default
	 * @webref shape:2d_primitives
	 * @see processing.core.PGraphics#rectMode(int)
	 * @see processing.core.PGraphics#quad(float, float, float, float, float, float, float, float)
	 */
	public void rect(float a, float b, float c, float d) {
		applet.rect(a, b, c, d);
	}

	/**
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @param r radii for all four corners
	 */
	public void rect(float a, float b, float c, float d, float r) {
		applet.rect(a, b, c, d, r);
	}

	/**
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @param tl radius for top-left corner
	 * @param tr radius for top-right corner
	 * @param br radius for bottom-right corner
	 * @param bl radius for bottom-left corner
	 */
	public void rect(float a, float b, float c, float d, float tl, float tr, float br, float bl) {
		applet.rect(a, b, c, d, tl, tr, br, bl);
	}

	/**
	 * ( begin auto-generated from ellipseMode.xml )
	 * <p>
	 * The origin of the ellipse is modified by the <b>ellipseMode()</b> function. The default configuration is <b>ellipseMode(CENTER)</b>, which
	 * specifies the location of the ellipse as the center of the shape. The <b>RADIUS</b> mode is the same, but the width and height parameters
	 * to <b>ellipse()</b> specify the radius of the ellipse, rather than the diameter. The <b>CORNER</b> mode draws the shape from the upper-left
	 * corner of its bounding box. The <b>CORNERS</b> mode uses the four parameters to <b>ellipse()</b> to set two opposing corners of the
	 * ellipse's bounding box. The parameter must be written in ALL CAPS because Processing is a case-sensitive language.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode either CENTER, RADIUS, CORNER, or CORNERS
	 * @webref shape:attributes
	 * @see processing.core.PApplet#ellipse(float, float, float, float)
	 * @see processing.core.PApplet#arc(float, float, float, float, float, float)
	 */
	public void ellipseMode(int mode) {
		applet.ellipseMode(mode);
	}

	/**
	 * ( begin auto-generated from ellipse.xml )
	 * <p>
	 * Draws an ellipse (oval) in the display window. An ellipse with an equal <b>width</b> and <b>height</b> is a circle. The first two
	 * parameters set the location, the third sets the width, and the fourth sets the height. The origin may be changed with the
	 * <b>ellipseMode()</b> function.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param a x-coordinate of the ellipse
	 * @param b y-coordinate of the ellipse
	 * @param c width of the ellipse by default
	 * @param d height of the ellipse by default
	 * @webref shape:2d_primitives
	 * @see processing.core.PApplet#ellipseMode(int)
	 * @see processing.core.PApplet#arc(float, float, float, float, float, float)
	 */
	public void ellipse(float a, float b, float c, float d) {
		applet.ellipse(a, b, c, d);
	}

	/**
	 * ( begin auto-generated from arc.xml )
	 * <p>
	 * Draws an arc in the display window. Arcs are drawn along the outer edge of an ellipse defined by the <b>x</b>, <b>y</b>, <b>width</b> and
	 * <b>height</b> parameters. The origin or the arc's ellipse may be changed with the <b>ellipseMode()</b> function. The <b>start</b> and
	 * <b>stop</b> parameters specify the angles at which to draw the arc.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param a     x-coordinate of the arc's ellipse
	 * @param b     y-coordinate of the arc's ellipse
	 * @param c     width of the arc's ellipse by default
	 * @param d     height of the arc's ellipse by default
	 * @param start angle to start the arc, specified in radians
	 * @param stop  angle to stop the arc, specified in radians
	 * @webref shape:2d_primitives
	 * @see processing.core.PApplet#ellipse(float, float, float, float)
	 * @see processing.core.PApplet#ellipseMode(int)
	 * @see processing.core.PApplet#radians(float)
	 * @see processing.core.PApplet#degrees(float)
	 */
	public void arc(float a, float b, float c, float d, float start, float stop) {
		applet.arc(a, b, c, d, start, stop);
	}

	public void arc(float a, float b, float c, float d, float start, float stop, int mode) {
		applet.arc(a, b, c, d, start, stop, mode);
	}

	/**
	 * ( begin auto-generated from box.xml )
	 * <p>
	 * A box is an extruded rectangle. A box with equal dimension on all sides is a cube.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param size dimension of the box in all dimensions (creates a cube)
	 * @webref shape:3d_primitives
	 * @see processing.core.PGraphics#sphere(float)
	 */
	public void box(float size) {
		applet.box(size);
	}

	/**
	 * @param w dimension of the box in the x-dimension
	 * @param h dimension of the box in the y-dimension
	 * @param d dimension of the box in the z-dimension
	 */
	public void box(float w, float h, float d) {
		applet.box(w, h, d);
	}

	/**
	 * ( begin auto-generated from sphereDetail.xml )
	 * <p>
	 * Controls the detail used to render a sphere by adjusting the number of vertices of the sphere mesh. The default resolution is 30, which
	 * creates a fairly detailed sphere definition with vertices every 360/30 = 12 degrees. If you're going to render a great number of spheres
	 * per frame, it is advised to reduce the level of detail using this function. The setting stays active until <b>sphereDetail()</b> is called
	 * again with a new parameter and so should <i>not</i> be called prior to every <b>sphere()</b> statement, unless you wish to render spheres
	 * with different settings, e.g. using less detail for smaller spheres or ones further away from the camera. To control the detail of the
	 * horizontal and vertical resolution independently, use the version of the functions with two parameters.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Code for sphereDetail() submitted by toxi [031031]. Code for enhanced u/v version from davbol [080801].
	 *
	 * @param res number of segments (minimum 3) used per full circle revolution
	 * @webref shape:3d_primitives
	 * @see processing.core.PGraphics#sphere(float)
	 */
	public void sphereDetail(int res) {
		applet.sphereDetail(res);
	}

	/**
	 * @param ures number of segments used longitudinally per full circle revolutoin
	 * @param vres number of segments used latitudinally from top to bottom
	 */
	public void sphereDetail(int ures, int vres) {
		applet.sphereDetail(ures, vres);
	}

	/**
	 * ( begin auto-generated from sphere.xml )
	 * <p>
	 * A sphere is a hollow ball made from tessellated triangles.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3>
	 * <p>
	 * Implementation notes:
	 * <p>
	 * cache all the points of the sphere in a  array top and bottom are just a bunch of triangles that land in the center point
	 * <p>
	 * sphere is a series of concentric circles who radii vary along the shape, based on, er.. cos or something <PRE> [toxi 031031] new sphere
	 * code. removed all multiplies with radius, as scale() will take care of that anyway
	 * <p>
	 * [toxi 031223] updated sphere code (removed modulos) and introduced sphereAt(x,y,z,r) to avoid additional translate()'s on the user/sketch
	 * side
	 * <p>
	 * [davbol 080801] now using separate sphereDetailU/V </PRE>
	 *
	 * @param r the radius of the sphere
	 * @webref shape:3d_primitives
	 * @see processing.core.PGraphics#sphereDetail(int)
	 */
	public void sphere(float r) {
		applet.sphere(r);
	}

	/**
	 * ( begin auto-generated from bezierPoint.xml )
	 * <p>
	 * Evaluates the Bezier at point t for points a, b, c, d. The parameter t varies between 0 and 1, a and d are points on the curve, and b and c
	 * are the control points. This can be done once with the x coordinates and a second time with the y coordinates to get the location of a
	 * bezier curve at t.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> For instance, to convert the following example:<PRE> stroke(255, 102, 0); line(85, 20, 10, 10); line(90, 90, 15, 80);
	 * stroke(0, 0, 0); bezier(85, 20, 10, 10, 90, 90, 15, 80);
	 * <p>
	 * // draw it in gray, using 10 steps instead of the default 20 // this is a slower way to do it, but useful if you need // to do things with
	 * the coordinates at each step stroke(128); beginShape(LINE_STRIP); for (int i = 0; i <= 10; i++) { float t = i / 10.0f; float x =
	 * bezierPoint(85, 10, 90, 15, t); float y = bezierPoint(20, 10, 90, 80, t); vertex(x, y); } endShape();</PRE>
	 *
	 * @param a coordinate of first point on the curve
	 * @param b coordinate of first control point
	 * @param c coordinate of second control point
	 * @param d coordinate of second point on the curve
	 * @param t value between 0 and 1
	 * @webref shape:curves
	 * @see processing.core.PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#bezierVertex(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curvePoint(float, float, float, float, float)
	 */
	public float bezierPoint(float a, float b, float c, float d, float t) {
		return applet.bezierPoint(a, b, c, d, t);
	}

	/**
	 * ( begin auto-generated from bezierTangent.xml )
	 * <p>
	 * Calculates the tangent of a point on a Bezier curve. There is a good definition of <a href="http://en.wikipedia.org/wiki/Tangent"
	 * target="new"><em>tangent</em> on Wikipedia</a>.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Code submitted by Dave Bollinger (davol) for release 0136.
	 *
	 * @param a coordinate of first point on the curve
	 * @param b coordinate of first control point
	 * @param c coordinate of second control point
	 * @param d coordinate of second point on the curve
	 * @param t value between 0 and 1
	 * @webref shape:curves
	 * @see processing.core.PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#bezierVertex(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curvePoint(float, float, float, float, float)
	 */
	public float bezierTangent(float a, float b, float c, float d, float t) {
		return applet.bezierTangent(a, b, c, d, t);
	}

	/**
	 * ( begin auto-generated from bezierDetail.xml )
	 * <p>
	 * Sets the resolution at which Beziers display. The default value is 20. This function is only useful when using the P3D renderer as the
	 * default P2D renderer does not use this information.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param detail resolution of the curves
	 * @webref shape:curves
	 * @see processing.core.PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curveVertex(float, float, float)
	 * @see processing.core.PGraphics#curveTightness(float)
	 */
	public void bezierDetail(int detail) {
		applet.bezierDetail(detail);
	}

	public void bezier(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		applet.bezier(x1, y1, x2, y2, x3, y3, x4, y4);
	}

	/**
	 * ( begin auto-generated from bezier.xml )
	 * <p>
	 * Draws a Bezier curve on the screen. These curves are defined by a series of anchor and control points. The first two parameters specify the
	 * first anchor point and the last two parameters specify the other anchor point. The middle parameters specify the control points which
	 * define the shape of the curve. Bezier curves were developed by French engineer Pierre Bezier. Using the 3D version requires rendering with
	 * P3D (see the Environment reference for more information).
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Draw a cubic bezier curve. The first and last points are the on-curve points. The middle two are the 'control' points, or
	 * 'handles' in an application like Illustrator.
	 * <p>
	 * Identical to typing: <PRE>beginShape(); vertex(x1, y1); bezierVertex(x2, y2, x3, y3, x4, y4); endShape(); </PRE> In Postscript-speak, this
	 * would be: <PRE>moveto(x1, y1); curveto(x2, y2, x3, y3, x4, y4);</PRE> If you were to try and continue that curve like so: <PRE>curveto(x5,
	 * y5, x6, y6, x7, y7);</PRE> This would be done in processing by adding these statements: <PRE>bezierVertex(x5, y5, x6, y6, x7, y7) </PRE> To
	 * draw a quadratic (instead of cubic) curve, use the control point twice by doubling it: <PRE>bezier(x1, y1, cx, cy, cx, cy, x2, y2);</PRE>
	 *
	 * @param x1 coordinates for the first anchor point
	 * @param y1 coordinates for the first anchor point
	 * @param z1 coordinates for the first anchor point
	 * @param x2 coordinates for the first control point
	 * @param y2 coordinates for the first control point
	 * @param z2 coordinates for the first control point
	 * @param x3 coordinates for the second control point
	 * @param y3 coordinates for the second control point
	 * @param z3 coordinates for the second control point
	 * @param x4 coordinates for the second anchor point
	 * @param y4 coordinates for the second anchor point
	 * @param z4 coordinates for the second anchor point
	 * @webref shape:curves
	 * @see processing.core.PGraphics#bezierVertex(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
	 */
	public void bezier(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
		applet.bezier(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
	}

	/**
	 * ( begin auto-generated from curvePoint.xml )
	 * <p>
	 * Evalutes the curve at point t for points a, b, c, d. The parameter t varies between 0 and 1, a and d are points on the curve, and b and c
	 * are the control points. This can be done once with the x coordinates and a second time with the y coordinates to get the location of a
	 * curve at t.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param a coordinate of first point on the curve
	 * @param b coordinate of second point on the curve
	 * @param c coordinate of third point on the curve
	 * @param d coordinate of fourth point on the curve
	 * @param t value between 0 and 1
	 * @webref shape:curves
	 * @see processing.core.PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curveVertex(float, float)
	 * @see processing.core.PGraphics#bezierPoint(float, float, float, float, float)
	 */
	public float curvePoint(float a, float b, float c, float d, float t) {
		return applet.curvePoint(a, b, c, d, t);
	}

	/**
	 * ( begin auto-generated from curveTangent.xml )
	 * <p>
	 * Calculates the tangent of a point on a curve. There's a good definition of <em><a href="http://en.wikipedia.org/wiki/Tangent"
	 * target="new">tangent</em> on Wikipedia</a>.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Code thanks to Dave Bollinger (Bug #715)
	 *
	 * @param a coordinate of first point on the curve
	 * @param b coordinate of first control point
	 * @param c coordinate of second control point
	 * @param d coordinate of second point on the curve
	 * @param t value between 0 and 1
	 * @webref shape:curves
	 * @see processing.core.PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curveVertex(float, float)
	 * @see processing.core.PGraphics#curvePoint(float, float, float, float, float)
	 * @see processing.core.PGraphics#bezierTangent(float, float, float, float, float)
	 */
	public float curveTangent(float a, float b, float c, float d, float t) {
		return applet.curveTangent(a, b, c, d, t);
	}

	/**
	 * ( begin auto-generated from curveDetail.xml )
	 * <p>
	 * Sets the resolution at which curves display. The default value is 20. This function is only useful when using the P3D renderer as the
	 * default P2D renderer does not use this information.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param detail resolution of the curves
	 * @webref shape:curves
	 * @see processing.core.PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curveVertex(float, float)
	 * @see processing.core.PGraphics#curveTightness(float)
	 */
	public void curveDetail(int detail) {
		applet.curveDetail(detail);
	}

	/**
	 * ( begin auto-generated from curveTightness.xml )
	 * <p>
	 * Modifies the quality of forms created with <b>curve()</b> and <b>curveVertex()</b>. The parameter <b>squishy</b> determines how the curve
	 * fits to the vertex points. The value 0.0 is the default value for <b>squishy</b> (this value defines the curves to be Catmull-Rom splines)
	 * and the value 1.0 connects all the points with straight lines. Values within the range -5.0 and 5.0 will deform the curves but will leave
	 * them recognizable and as values increase in magnitude, they will continue to deform.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param tightness amount of deformation from the original vertices
	 * @webref shape:curves
	 * @see processing.core.PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#curveVertex(float, float)
	 */
	public void curveTightness(float tightness) {
		applet.curveTightness(tightness);
	}

	/**
	 * ( begin auto-generated from curve.xml )
	 * <p>
	 * Draws a curved line on the screen. The first and second parameters specify the beginning control point and the last two parameters specify
	 * the ending control point. The middle parameters specify the start and stop of the curve. Longer curves can be created by putting a series
	 * of <b>curve()</b> functions together or using <b>curveVertex()</b>. An additional function called <b>curveTightness()</b> provides control
	 * for the visual quality of the curve. The <b>curve()</b> function is an implementation of Catmull-Rom splines. Using the 3D version requires
	 * rendering with P3D (see the Environment reference for more information).
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> As of revision 0070, this function no longer doubles the first and last points. The curves are a bit more boring, but
	 * it's more mathematically correct, and properly mirrored in curvePoint().
	 * <p>
	 * Identical to typing out:<PRE> beginShape(); curveVertex(x1, y1); curveVertex(x2, y2); curveVertex(x3, y3); curveVertex(x4, y4); endShape();
	 * </PRE>
	 *
	 * @param x1 coordinates for the beginning control point
	 * @param y1 coordinates for the beginning control point
	 * @param x2 coordinates for the first point
	 * @param y2 coordinates for the first point
	 * @param x3 coordinates for the second point
	 * @param y3 coordinates for the second point
	 * @param x4 coordinates for the ending control point
	 * @param y4 coordinates for the ending control point
	 * @webref shape:curves
	 * @see processing.core.PGraphics#curveVertex(float, float)
	 * @see processing.core.PGraphics#curveTightness(float)
	 * @see processing.core.PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
	 */
	public void curve(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		applet.curve(x1, y1, x2, y2, x3, y3, x4, y4);
	}

	/**
	 * @param x1
	 * @param y1
	 * @param z1 coordinates for the beginning control point
	 * @param x2
	 * @param y2
	 * @param z2 coordinates for the first point
	 * @param x3
	 * @param y3
	 * @param z3 coordinates for the second point
	 * @param x4
	 * @param y4
	 * @param z4 coordinates for the ending control point
	 */
	public void curve(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
		applet.curve(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
	}

	/**
	 * ( begin auto-generated from smooth.xml )
	 * <p>
	 * Draws all geometry with smooth (anti-aliased) edges. This will sometimes slow down the frame rate of the application, but will enhance the
	 * visual refinement. Note that <b>smooth()</b> will also improve image quality of resized images, and <b>noSmooth()</b> will disable image
	 * (and font) smoothing altogether.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref shape:attributes
	 * @see processing.core.PGraphics#noSmooth()
	 * @see processing.core.PGraphics#hint(int)
	 * @see processing.core.PApplet#size(int, int, String)
	 */
	public void smooth() {
		applet.smooth();
	}

	/**
	 * @param level either 2, 4, or 8
	 */
	public void smooth(int level) {
		applet.smooth(level);
	}

	/**
	 * ( begin auto-generated from noSmooth.xml )
	 * <p>
	 * Draws all geometry with jagged (aliased) edges.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref shape:attributes
	 * @see processing.core.PGraphics#smooth()
	 */
	public void noSmooth() {
		applet.noSmooth();
	}

	/**
	 * ( begin auto-generated from imageMode.xml )
	 * <p>
	 * Modifies the location from which images draw. The default mode is <b>imageMode(CORNER)</b>, which specifies the location to be the upper
	 * left corner and uses the fourth and fifth parameters of <b>image()</b> to set the image's width and height. The syntax
	 * <b>imageMode(CORNERS)</b> uses the second and third parameters of <b>image()</b> to set the location of one corner of the image and uses
	 * the fourth and fifth parameters to set the opposite corner. Use <b>imageMode(CENTER)</b> to draw images centered at the given x and y
	 * position.<br /> <br /> The parameter to <b>imageMode()</b> must be written in ALL CAPS because Processing is a case-sensitive language.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode either CORNER, CORNERS, or CENTER
	 * @webref image:loading_displaying
	 * @see processing.core.PApplet#loadImage(String, String)
	 * @see processing.core.PImage
	 * @see processing.core.PGraphics#image(processing.core.PImage, float, float, float, float)
	 * @see processing.core.PGraphics#background(float, float, float, float)
	 */
	public void imageMode(int mode) {
		applet.imageMode(mode);
	}

	/**
	 * ( begin auto-generated from image.xml )
	 * <p>
	 * Displays images to the screen. The images must be in the sketch's "data" directory to load correctly. Select "Add file..." from the
	 * "Sketch" menu to add the image. Processing currently works with GIF, JPEG, and Targa images. The <b>img</b> parameter specifies the image
	 * to display and the <b>x</b> and <b>y</b> parameters define the location of the image from its upper-left corner. The image is displayed at
	 * its original size unless the <b>width</b> and <b>height</b> parameters specify a different size.<br /> <br /> The <b>imageMode()</b>
	 * function changes the way the parameters work. For example, a call to <b>imageMode(CORNERS)</b> will change the <b>width</b> and
	 * <b>height</b> parameters to define the x and y values of the opposite corner of the image.<br /> <br /> The color of an image may be
	 * modified with the <b>tint()</b> function. This function will maintain transparency for GIF and PNG images.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Starting with release 0124, when using the default (JAVA2D) renderer, smooth() will also improve image quality of resized
	 * images.
	 *
	 * @param img the image to display
	 * @param a   x-coordinate of the image
	 * @param b   y-coordinate of the image
	 * @webref image:loading_displaying
	 * @see processing.core.PApplet#loadImage(String, String)
	 * @see processing.core.PImage
	 * @see processing.core.PGraphics#imageMode(int)
	 * @see processing.core.PGraphics#tint(float)
	 * @see processing.core.PGraphics#background(float, float, float, float)
	 * @see processing.core.PGraphics#alpha(int)
	 */
	public void image(PImage img, float a, float b) {
		applet.image(img, a, b);
	}

	/**
	 * @param img
	 * @param a
	 * @param b
	 * @param c   width to display the image
	 * @param d   height to display the image
	 */
	public void image(PImage img, float a, float b, float c, float d) {
		applet.image(img, a, b, c, d);
	}

	/**
	 * Draw an image(), also specifying u/v coordinates. In this method, the  u, v coordinates are always based on image space location,
	 * regardless of the current textureMode().
	 *
	 * @param img
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @param u1
	 * @param v1
	 * @param u2
	 * @param v2
	 * @nowebref
	 */
	public void image(PImage img, float a, float b, float c, float d, int u1, int v1, int u2, int v2) {
		applet.image(img, a, b, c, d, u1, v1, u2, v2);
	}

	/**
	 * ( begin auto-generated from shapeMode.xml )
	 * <p>
	 * Modifies the location from which shapes draw. The default mode is <b>shapeMode(CORNER)</b>, which specifies the location to be the upper
	 * left corner of the shape and uses the third and fourth parameters of <b>shape()</b> to specify the width and height. The syntax
	 * <b>shapeMode(CORNERS)</b> uses the first and second parameters of <b>shape()</b> to set the location of one corner and uses the third and
	 * fourth parameters to set the opposite corner. The syntax <b>shapeMode(CENTER)</b> draws the shape from its center point and uses the third
	 * and forth parameters of <b>shape()</b> to specify the width and height. The parameter must be written in "ALL CAPS" because Processing is a
	 * case sensitive language.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode either CORNER, CORNERS, CENTER
	 * @webref shape:loading_displaying
	 * @see processing.core.PShape
	 * @see processing.core.PGraphics#shape(processing.core.PShape)
	 * @see processing.core.PGraphics#rectMode(int)
	 */
	public void shapeMode(int mode) {
		applet.shapeMode(mode);
	}

	public void shape(PShape shape) {
		applet.shape(shape);
	}

	/**
	 * ( begin auto-generated from shape.xml )
	 * <p>
	 * Displays shapes to the screen. The shapes must be in the sketch's "data" directory to load correctly. Select "Add file..." from the
	 * "Sketch" menu to add the shape. Processing currently works with SVG shapes only. The <b>sh</b> parameter specifies the shape to display and
	 * the <b>x</b> and <b>y</b> parameters define the location of the shape from its upper-left corner. The shape is displayed at its original
	 * size unless the <b>width</b> and <b>height</b> parameters specify a different size. The <b>shapeMode()</b> function changes the way the
	 * parameters work. A call to <b>shapeMode(CORNERS)</b>, for example, will change the width and height parameters to define the x and y values
	 * of the opposite corner of the shape. <br /><br /> Note complex shapes may draw awkwardly with P3D. This renderer does not yet support
	 * shapes that have holes or complicated breaks.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param shape the shape to display
	 * @param x     x-coordinate of the shape
	 * @param y     y-coordinate of the shape
	 * @webref shape:loading_displaying
	 * @see processing.core.PShape
	 * @see processing.core.PApplet#loadShape(String)
	 * @see processing.core.PGraphics#shapeMode(int)
	 * <p>
	 * Convenience method to draw at a particular location.
	 */
	public void shape(PShape shape, float x, float y) {
		applet.shape(shape, x, y);
	}

	/**
	 * @param shape
	 * @param a     x-coordinate of the shape
	 * @param b     y-coordinate of the shape
	 * @param c     width to display the shape
	 * @param d     height to display the shape
	 */
	public void shape(PShape shape, float a, float b, float c, float d) {
		applet.shape(shape, a, b, c, d);
	}

	public void textAlign(int alignX) {
		applet.textAlign(alignX);
	}

	/**
	 * ( begin auto-generated from textAlign.xml )
	 * <p>
	 * Sets the current alignment for drawing text. The parameters LEFT, CENTER, and RIGHT set the display characteristics of the letters in
	 * relation to the values for the <b>x</b> and <b>y</b> parameters of the <b>text()</b> function. <br/> <br/> In Processing 0125 and later, an
	 * optional second parameter can be used to vertically align the text. BASELINE is the default, and the vertical alignment will be reset to
	 * BASELINE if the second parameter is not used. The TOP and CENTER parameters are straightforward. The BOTTOM parameter offsets the line
	 * based on the current <b>textDescent()</b>. For multiple lines, the public final  line will be aligned to the bottom, with the previous lines
	 * appearing above it. <br/> <br/> When using <b>text()</b> with width and height parameters, BASELINE is ignored, and treated as TOP.
	 * (Otherwise, text would by default draw outside the box, since BASELINE is the default setting. BASELINE is not a useful drawing mode for
	 * text drawn in a rectangle.) <br/> <br/> The vertical alignment is based on the value of <b>textAscent()</b>, which many fonts do not
	 * specify correctly. It may be necessary to use a hack and offset by a few pixels by hand so that the offset looks correct. To do this as
	 * less of a hack, use some percentage of <b>textAscent()</b> or <b>textDescent()</b> so that the hack works even if you change the size of
	 * the font.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param alignX horizontal alignment, either LEFT, CENTER, or RIGHT
	 * @param alignY vertical alignment, either TOP, BOTTOM, CENTER, or BASELINE
	 * @webref typography:attributes
	 * @see processing.core.PApplet#loadFont(String)
	 * @see processing.core.PFont
	 * @see processing.core.PGraphics#text(String, float, float)
	 */
	public void textAlign(int alignX, int alignY) {
		applet.textAlign(alignX, alignY);
	}

	/**
	 * ( begin auto-generated from textAscent.xml )
	 * <p>
	 * Returns ascent of the current font at its current size. This information is useful for determining the height of the font above the
	 * baseline. For example, adding the <b>textAscent()</b> and <b>textDescent()</b> values will give you the total height of the line.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref typography:metrics
	 * @see processing.core.PGraphics#textDescent()
	 */
	public float textAscent() {
		return applet.textAscent();
	}

	/**
	 * ( begin auto-generated from textDescent.xml )
	 * <p>
	 * Returns descent of the current font at its current size. This information is useful for determining the height of the font below the
	 * baseline. For example, adding the <b>textAscent()</b> and <b>textDescent()</b> values will give you the total height of the line.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref typography:metrics
	 * @see processing.core.PGraphics#textAscent()
	 */
	public float textDescent() {
		return applet.textDescent();
	}

	/**
	 * ( begin auto-generated from textFont.xml )
	 * <p>
	 * Sets the current font that will be drawn with the <b>text()</b> function. Fonts must be loaded with <b>loadFont()</b> before it can be
	 * used. This font will be used in all subsequent calls to the <b>text()</b> function. If no <b>size</b> parameter is input, the font will
	 * appear at its original size (the size it was created at with the "Create Font..." tool) until it is changed with <b>textSize()</b>. <br />
	 * <br /> Because fonts are usually bitmaped, you should create fonts at the sizes that will be used most commonly. Using <b>textFont()</b>
	 * without the size parameter will result in the cleanest-looking text. <br /><br /> With the default (JAVA2D) and PDF renderers, it's also
	 * possible to enable the use of native fonts via the command <b>hint(ENABLE_NATIVE_FONTS)</b>. This will produce vector text in JAVA2D
	 * sketches and PDF output in cases where the vector data is available: when the font is still installed, or the font is created via the
	 * <b>createFont()</b> function (rather than the Create Font tool).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param which any variable of the type PFont
	 * @webref typography:loading_displaying
	 * @see processing.core.PApplet#createFont(String, float, boolean)
	 * @see processing.core.PApplet#loadFont(String)
	 * @see processing.core.PFont
	 * @see processing.core.PGraphics#text(String, float, float)
	 */
	public void textFont(PFont which) {
		applet.textFont(which);
	}

	/**
	 * @param which
	 * @param size  the size of the letters in units of pixels
	 */
	public void textFont(PFont which, float size) {
		applet.textFont(which, size);
	}

	/**
	 * ( begin auto-generated from textLeading.xml )
	 * <p>
	 * Sets the spacing between lines of text in units of pixels. This setting will be used in all subsequent calls to the <b>text()</b>
	 * function.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param leading the size in pixels for spacing between lines
	 * @webref typography:attributes
	 * @see processing.core.PApplet#loadFont(String)
	 * @see processing.core.PFont#PFont
	 * @see processing.core.PGraphics#text(String, float, float)
	 * @see processing.core.PGraphics#textFont(processing.core.PFont)
	 */
	public void textLeading(float leading) {
		applet.textLeading(leading);
	}

	/**
	 * ( begin auto-generated from textMode.xml )
	 * <p>
	 * Sets the way text draws to the screen. In the default configuration, the <b>MODEL</b> mode, it's possible to rotate, scale, and place
	 * letters in two and three dimensional space.<br /> <br /> The <b>SHAPE</b> mode draws text using the the glyph outlines of individual
	 * characters rather than as textures. This mode is only supported with the <b>PDF</b> and <b>P3D</b> renderer settings. With the <b>PDF</b>
	 * renderer, you must call <b>textMode(SHAPE)</b> before any other drawing occurs. If the outlines are not available, then
	 * <b>textMode(SHAPE)</b> will be ignored and <b>textMode(MODEL)</b> will be used instead.<br /> <br /> The <b>textMode(SHAPE)</b> option in
	 * <b>P3D</b> can be combined with <b>beginRaw()</b> to write vector-accurate text to 2D and 3D output files, for instance <b>DXF</b> or
	 * <b>PDF</b>. The <b>SHAPE</b> mode is not currently optimized for <b>P3D</b>, so if recording shape data, use <b>textMode(MODEL)</b> until
	 * you're ready to capture the geometry with <b>beginRaw()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode either MODEL or SHAPE
	 * @webref typography:attributes
	 * @see processing.core.PApplet#loadFont(String)
	 * @see processing.core.PFont#PFont
	 * @see processing.core.PGraphics#text(String, float, float)
	 * @see processing.core.PGraphics#textFont(processing.core.PFont)
	 * @see processing.core.PGraphics#beginRaw(processing.core.PGraphics)
	 * @see processing.core.PApplet#createFont(String, float, boolean)
	 */
	public void textMode(int mode) {
		applet.textMode(mode);
	}

	/**
	 * ( begin auto-generated from textSize.xml )
	 * <p>
	 * Sets the current font size. This size will be used in all subsequent calls to the <b>text()</b> function. Font size is measured in units of
	 * pixels.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param size the size of the letters in units of pixels
	 * @webref typography:attributes
	 * @see processing.core.PApplet#loadFont(String)
	 * @see processing.core.PFont#PFont
	 * @see processing.core.PGraphics#text(String, float, float)
	 * @see processing.core.PGraphics#textFont(processing.core.PFont)
	 */
	public void textSize(float size) {
		applet.textSize(size);
	}

	/**
	 * @param c the character to measure
	 */
	public float textWidth(char c) {
		return applet.textWidth(c);
	}

	/**
	 * ( begin auto-generated from textWidth.xml )
	 * <p>
	 * Calculates and returns the width of any character or text string.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param str the String of characters to measure
	 * @webref typography:attributes
	 * @see processing.core.PApplet#loadFont(String)
	 * @see processing.core.PFont#PFont
	 * @see processing.core.PGraphics#text(String, float, float)
	 * @see processing.core.PGraphics#textFont(processing.core.PFont)
	 */
	public float textWidth(String str) {
		return applet.textWidth(str);
	}

	/**
	 * @param chars
	 * @param start
	 * @param length
	 * @nowebref
	 */
	public float textWidth(char[] chars, int start, int length) {
		return applet.textWidth(chars, start, length);
	}

	/**
	 * ( begin auto-generated from text.xml )
	 * <p>
	 * Draws text to the screen. Displays the information specified in the <b>data</b> or <b>stringdata</b> parameters on the screen in the
	 * position specified by the <b>x</b> and <b>y</b> parameters and the optional <b>z</b> parameter. A default font will be used unless a font
	 * is set with the <b>textFont()</b> function. Change the color of the text with the <b>fill()</b> function. The text displays in relation to
	 * the <b>textAlign()</b> function, which gives the option to draw to the left, right, and center of the coordinates. <br /><br /> The
	 * <b>x2</b> and <b>y2</b> parameters define a rectangular area to display within and may only be used with string data. For text drawn inside
	 * a rectangle, the coordinates are interpreted based on the current <b>rectMode()</b> setting.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param c the alphanumeric character to be displayed
	 * @param x x-coordinate of text
	 * @param y y-coordinate of text
	 * @webref typography:loading_displaying
	 * @see_external String
	 * @see processing.core.PGraphics#textAlign(int, int)
	 * @see processing.core.PGraphics#textFont(processing.core.PFont)
	 * @see processing.core.PGraphics#textMode(int)
	 * @see processing.core.PGraphics#textSize(float)
	 * @see processing.core.PGraphics#rectMode(int)
	 * @see processing.core.PGraphics#fill(int, float)
	 */
	public void text(char c, float x, float y) {
		applet.text(c, x, y);
	}

	/**
	 * @param c
	 * @param x
	 * @param y
	 * @param z z-coordinate of text
	 */
	public void text(char c, float x, float y, float z) {
		applet.text(c, x, y, z);
	}

	/**
	 * <h3>Advanced</h3> Draw a chunk of text. Newlines that are \n (Unix newline or linefeed char, ascii 10) are honored, but \r (carriage
	 * return, Windows and Mac OS) are ignored.
	 *
	 * @param str
	 * @param x
	 * @param y
	 */
	public void text(String str, float x, float y) {
		applet.text(str, x, y);
	}

	/**
	 * <h3>Advanced</h3> Method to draw text from an array of chars. This method will usually be more efficient than drawing from a String object,
	 * because the String will not be converted to a char array before drawing.
	 *
	 * @param chars the alphanumberic symbols to be displayed
	 * @param start array index at which to start writing characters
	 * @param stop  array index at which to stop writing characters
	 * @param x
	 * @param y
	 */
	public void text(char[] chars, int start, int stop, float x, float y) {
		applet.text(chars, start, stop, x, y);
	}

	/**
	 * Same as above but with a z coordinate.
	 *
	 * @param str
	 * @param x
	 * @param y
	 * @param z
	 */
	public void text(String str, float x, float y, float z) {
		applet.text(str, x, y, z);
	}

	public void text(char[] chars, int start, int stop, float x, float y, float z) {
		applet.text(chars, start, stop, x, y, z);
	}

	/**
	 * <h3>Advanced</h3> Draw text in a box that is constrained to a particular size. The current rectMode() determines what the coordinates mean
	 * (whether x1/y1/x2/y2 or x/y/w/h).
	 * <p>
	 * Note that the x,y coords of the start of the box will align with the *ascent* of the text, not the baseline, as is the case for the other
	 * text() functions.
	 * <p>
	 * Newlines that are \n (Unix newline or linefeed char, ascii 10) are honored, and \r (carriage return, Windows and Mac OS) are ignored.
	 *
	 * @param str
	 * @param x1  by default, the x-coordinate of text, see rectMode() for more info
	 * @param y1  by default, the x-coordinate of text, see rectMode() for more info
	 * @param x2  by default, the width of the text box, see rectMode() for more info
	 * @param y2  by default, the height of the text box, see rectMode() for more info
	 */
	public void text(String str, float x1, float y1, float x2, float y2) {
		applet.text(str, x1, y1, x2, y2);
	}

	public void text(int num, float x, float y) {
		applet.text(num, x, y);
	}

	public void text(int num, float x, float y, float z) {
		applet.text(num, x, y, z);
	}

	/**
	 * This does a basic number formatting, to avoid the generally ugly appearance of printing floats. Users who want more control should use
	 * their own nf() cmmand, or if they want the long, ugly version of float, use String.valueOf() to convert the float to a String first.
	 *
	 * @param num the numeric value to be displayed
	 * @param x
	 * @param y
	 */
	public void text(float num, float x, float y) {
		applet.text(num, x, y);
	}

	public void text(float num, float x, float y, float z) {
		applet.text(num, x, y, z);
	}

	int p = 0;

	/**
	 * ( begin auto-generated from pushMatrix.xml )
	 * <p>
	 * Pushes the current transformation matrix onto the matrix stack. Understanding <b>pushMatrix()</b> and <b>popMatrix()</b> requires
	 * understanding the concept of a matrix stack. The <b>pushMatrix()</b> function saves the current coordinate system to the stack and
	 * <b>popMatrix()</b> restores the prior coordinate system. <b>pushMatrix()</b> and <b>popMatrix()</b> are used in conjuction with the other
	 * transformation functions and may be embedded to control the scope of the transformations.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref transform
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#translate(float, float, float)
	 * @see processing.core.PGraphics#scale(float)
	 * @see processing.core.PGraphics#rotate(float)
	 * @see processing.core.PGraphics#rotateX(float)
	 * @see processing.core.PGraphics#rotateY(float)
	 * @see processing.core.PGraphics#rotateZ(float)
	 */
	public void pushMatrix() {
//		p++;
//		Log.log("processing.push", "push :" + p);
		applet.pushMatrix();
	}

	/**
	 * ( begin auto-generated from popMatrix.xml )
	 * <p>
	 * Pops the current transformation matrix off the matrix stack. Understanding pushing and popping requires understanding the concept of a
	 * matrix stack. The <b>pushMatrix()</b> function saves the current coordinate system to the stack and <b>popMatrix()</b> restores the prior
	 * coordinate system. <b>pushMatrix()</b> and <b>popMatrix()</b> are used in conjuction with the other transformation functions and may be
	 * embedded to control the scope of the transformations.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref transform
	 * @see processing.core.PGraphics#pushMatrix()
	 */
	public void popMatrix() {
//		p--;
//		Log.log("processing.push", "pop :" + p);
		applet.popMatrix();
	}

	/**
	 * ( begin auto-generated from translate.xml )
	 * <p>
	 * Specifies an amount to displace objects within the display window. The <b>x</b> parameter specifies left/right translation, the <b>y</b>
	 * parameter specifies up/down translation, and the <b>z</b> parameter specifies translations toward/away from the screen. Using this function
	 * with the <b>z</b> parameter requires using P3D as a parameter in combination with size as shown in the above example. Transformations apply
	 * to everything that happens after and subsequent calls to the function accumulates the effect. For example, calling <b>translate(50, 0)</b>
	 * and then <b>translate(20, 0)</b> is the same as <b>translate(70, 0)</b>. If <b>translate()</b> is called within <b>draw()</b>, the
	 * transformation is reset when the loop begins again. This function can be further controlled by the <b>pushMatrix()</b> and
	 * <b>popMatrix()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x left/right translation
	 * @param y up/down translation
	 * @webref transform
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#rotate(float)
	 * @see processing.core.PGraphics#rotateX(float)
	 * @see processing.core.PGraphics#rotateY(float)
	 * @see processing.core.PGraphics#rotateZ(float)
	 * @see processing.core.PGraphics#scale(float, float, float)
	 */
	public void translate(float x, float y) {
		applet.translate(x, y);
	}

	/**
	 * @param x
	 * @param y
	 * @param z forward/backward translation
	 */
	public void translate(float x, float y, float z) {
		applet.translate(x, y, z);
	}

	/**
	 * ( begin auto-generated from rotate.xml )
	 * <p>
	 * Rotates a shape the amount specified by the <b>angle</b> parameter. Angles should be specified in radians (values from 0 to TWO_PI) or
	 * converted to radians with the <b>radians()</b> function. <br/> <br/> Objects are always rotated around their relative position to the
	 * origin and positive numbers rotate objects in a clockwise direction. Transformations apply to everything that happens after and subsequent
	 * calls to the function accumulates the effect. For example, calling <b>rotate(HALF_PI)</b> and then <b>rotate(HALF_PI)</b> is the same as
	 * <b>rotate(PI)</b>. All tranformations are reset when <b>draw()</b> begins again. <br/> <br/> Technically, <b>rotate()</b> multiplies the
	 * current transformation matrix by a rotation matrix. This function can be further controlled by the <b>pushMatrix()</b> and
	 * <b>popMatrix()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle angle of rotation specified in radians
	 * @webref transform
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#rotateX(float)
	 * @see processing.core.PGraphics#rotateY(float)
	 * @see processing.core.PGraphics#rotateZ(float)
	 * @see processing.core.PGraphics#scale(float, float, float)
	 * @see processing.core.PApplet#radians(float)
	 */
	public void rotate(float angle) {
		applet.rotate(angle);
	}

	/**
	 * ( begin auto-generated from rotateX.xml )
	 * <p>
	 * Rotates a shape around the x-axis the amount specified by the <b>angle</b> parameter. Angles should be specified in radians (values from 0
	 * to PI*2) or converted to radians with the <b>radians()</b> function. Objects are always rotated around their relative position to the
	 * origin and positive numbers rotate objects in a counterclockwise direction. Transformations apply to everything that happens after and
	 * subsequent calls to the function accumulates the effect. For example, calling <b>rotateX(PI/2)</b> and then <b>rotateX(PI/2)</b> is the
	 * same as <b>rotateX(PI)</b>. If <b>rotateX()</b> is called within the <b>draw()</b>, the transformation is reset when the loop begins again.
	 * This function requires using P3D as a third parameter to <b>size()</b> as shown in the example above.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle angle of rotation specified in radians
	 * @webref transform
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#rotate(float)
	 * @see processing.core.PGraphics#rotateY(float)
	 * @see processing.core.PGraphics#rotateZ(float)
	 * @see processing.core.PGraphics#scale(float, float, float)
	 * @see processing.core.PGraphics#translate(float, float, float)
	 */
	public void rotateX(float angle) {
		applet.rotateX(angle);
	}

	/**
	 * ( begin auto-generated from rotateY.xml )
	 * <p>
	 * Rotates a shape around the y-axis the amount specified by the <b>angle</b> parameter. Angles should be specified in radians (values from 0
	 * to PI*2) or converted to radians with the <b>radians()</b> function. Objects are always rotated around their relative position to the
	 * origin and positive numbers rotate objects in a counterclockwise direction. Transformations apply to everything that happens after and
	 * subsequent calls to the function accumulates the effect. For example, calling <b>rotateY(PI/2)</b> and then <b>rotateY(PI/2)</b> is the
	 * same as <b>rotateY(PI)</b>. If <b>rotateY()</b> is called within the <b>draw()</b>, the transformation is reset when the loop begins again.
	 * This function requires using P3D as a third parameter to <b>size()</b> as shown in the examples above.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle angle of rotation specified in radians
	 * @webref transform
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#rotate(float)
	 * @see processing.core.PGraphics#rotateX(float)
	 * @see processing.core.PGraphics#rotateZ(float)
	 * @see processing.core.PGraphics#scale(float, float, float)
	 * @see processing.core.PGraphics#translate(float, float, float)
	 */
	public void rotateY(float angle) {
		applet.rotateY(angle);
	}

	/**
	 * ( begin auto-generated from rotateZ.xml )
	 * <p>
	 * Rotates a shape around the z-axis the amount specified by the <b>angle</b> parameter. Angles should be specified in radians (values from 0
	 * to PI*2) or converted to radians with the <b>radians()</b> function. Objects are always rotated around their relative position to the
	 * origin and positive numbers rotate objects in a counterclockwise direction. Transformations apply to everything that happens after and
	 * subsequent calls to the function accumulates the effect. For example, calling <b>rotateZ(PI/2)</b> and then <b>rotateZ(PI/2)</b> is the
	 * same as <b>rotateZ(PI)</b>. If <b>rotateZ()</b> is called within the <b>draw()</b>, the transformation is reset when the loop begins again.
	 * This function requires using P3D as a third parameter to <b>size()</b> as shown in the examples above.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle angle of rotation specified in radians
	 * @webref transform
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#rotate(float)
	 * @see processing.core.PGraphics#rotateX(float)
	 * @see processing.core.PGraphics#rotateY(float)
	 * @see processing.core.PGraphics#scale(float, float, float)
	 * @see processing.core.PGraphics#translate(float, float, float)
	 */
	public void rotateZ(float angle) {
		applet.rotateZ(angle);
	}

	/**
	 * <h3>Advanced</h3> Rotate about a vector in space. Same as the glRotatef() function.
	 *
	 * @param angle
	 * @param x
	 * @param y
	 * @param z
	 */
	public void rotate(float angle, float x, float y, float z) {
		applet.rotate(angle, x, y, z);
	}

	/**
	 * ( begin auto-generated from scale.xml )
	 * <p>
	 * Increases or decreases the size of a shape by expanding and contracting vertices. Objects always scale from their relative origin to the
	 * coordinate system. Scale values are specified as decimal percentages. For example, the function call <b>scale(2.0)</b> increases the
	 * dimension of a shape by 200%. Transformations apply to everything that happens after and subsequent calls to the function multiply the
	 * effect. For example, calling <b>scale(2.0)</b> and then <b>scale(1.5)</b> is the same as <b>scale(3.0)</b>. If <b>scale()</b> is called
	 * within <b>draw()</b>, the transformation is reset when the loop begins again. Using this fuction with the <b>z</b> parameter requires using
	 * P3D as a parameter for <b>size()</b> as shown in the example above. This function can be further controlled by <b>pushMatrix()</b> and
	 * <b>popMatrix()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param s percentage to scale the object
	 * @webref transform
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#translate(float, float, float)
	 * @see processing.core.PGraphics#rotate(float)
	 * @see processing.core.PGraphics#rotateX(float)
	 * @see processing.core.PGraphics#rotateY(float)
	 * @see processing.core.PGraphics#rotateZ(float)
	 */
	public void scale(float s) {
		applet.scale(s);
	}

	/**
	 * <h3>Advanced</h3> Scale in X and Y. Equivalent to scale(sx, sy, 1).
	 * <p>
	 * Not recommended for use in 3D, because the z-dimension is just scaled by 1, since there's no way to know what else to scale it by.
	 *
	 * @param x percentage to scale the object in the x-axis
	 * @param y percentage to scale the object in the y-axis
	 */
	public void scale(float x, float y) {
		applet.scale(x, y);
	}

	/**
	 * @param x
	 * @param y
	 * @param z percentage to scale the object in the z-axis
	 */
	public void scale(float x, float y, float z) {
		applet.scale(x, y, z);
	}

	/**
	 * ( begin auto-generated from shearX.xml )
	 * <p>
	 * Shears a shape around the x-axis the amount specified by the <b>angle</b> parameter. Angles should be specified in radians (values from 0
	 * to PI*2) or converted to radians with the <b>radians()</b> function. Objects are always sheared around their relative position to the
	 * origin and positive numbers shear objects in a clockwise direction. Transformations apply to everything that happens after and subsequent
	 * calls to the function accumulates the effect. For example, calling <b>shearX(PI/2)</b> and then <b>shearX(PI/2)</b> is the same as
	 * <b>shearX(PI)</b>. If <b>shearX()</b> is called within the <b>draw()</b>, the transformation is reset when the loop begins again. <br/>
	 * <br/> Technically, <b>shearX()</b> multiplies the current transformation matrix by a rotation matrix. This function can be further
	 * controlled by the <b>pushMatrix()</b> and <b>popMatrix()</b> functions.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle angle of shear specified in radians
	 * @webref transform
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#shearY(float)
	 * @see processing.core.PGraphics#scale(float, float, float)
	 * @see processing.core.PGraphics#translate(float, float, float)
	 * @see processing.core.PApplet#radians(float)
	 */
	public void shearX(float angle) {
		applet.shearX(angle);
	}

	/**
	 * ( begin auto-generated from shearY.xml )
	 * <p>
	 * Shears a shape around the y-axis the amount specified by the <b>angle</b> parameter. Angles should be specified in radians (values from 0
	 * to PI*2) or converted to radians with the <b>radians()</b> function. Objects are always sheared around their relative position to the
	 * origin and positive numbers shear objects in a clockwise direction. Transformations apply to everything that happens after and subsequent
	 * calls to the function accumulates the effect. For example, calling <b>shearY(PI/2)</b> and then <b>shearY(PI/2)</b> is the same as
	 * <b>shearY(PI)</b>. If <b>shearY()</b> is called within the <b>draw()</b>, the transformation is reset when the loop begins again. <br/>
	 * <br/> Technically, <b>shearY()</b> multiplies the current transformation matrix by a rotation matrix. This function can be further
	 * controlled by the <b>pushMatrix()</b> and <b>popMatrix()</b> functions.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param angle angle of shear specified in radians
	 * @webref transform
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#shearX(float)
	 * @see processing.core.PGraphics#scale(float, float, float)
	 * @see processing.core.PGraphics#translate(float, float, float)
	 * @see processing.core.PApplet#radians(float)
	 */
	public void shearY(float angle) {
		applet.shearY(angle);
	}

	/**
	 * ( begin auto-generated from resetMatrix.xml )
	 * <p>
	 * Replaces the current matrix with the identity matrix. The equivalent function in OpenGL is glLoadIdentity().
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref transform
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#applyMatrix(processing.core.PMatrix)
	 * @see processing.core.PGraphics#printMatrix()
	 */
	public void resetMatrix() {
		applet.resetMatrix();
	}

	/**
	 * ( begin auto-generated from applyMatrix.xml )
	 * <p>
	 * Multiplies the current matrix by the one specified through the parameters. This is very slow because it will try to calculate the inverse
	 * of the transform, so avoid it whenever possible. The equivalent function in OpenGL is glMultMatrix().
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param source
	 * @webref transform
	 * @source
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#resetMatrix()
	 * @see processing.core.PGraphics#printMatrix()
	 */
	public void applyMatrix(PMatrix source) {
		applet.applyMatrix(source);
	}

	public void applyMatrix(PMatrix2D source) {
		applet.applyMatrix(source);
	}

	/**
	 * @param n00 numbers which define the 4x4 matrix to be multiplied
	 * @param n01 numbers which define the 4x4 matrix to be multiplied
	 * @param n02 numbers which define the 4x4 matrix to be multiplied
	 * @param n10 numbers which define the 4x4 matrix to be multiplied
	 * @param n11 numbers which define the 4x4 matrix to be multiplied
	 * @param n12 numbers which define the 4x4 matrix to be multiplied
	 */
	public void applyMatrix(float n00, float n01, float n02, float n10, float n11, float n12) {
		applet.applyMatrix(n00, n01, n02, n10, n11, n12);
	}

	public void applyMatrix(PMatrix3D source) {
		applet.applyMatrix(source);
	}

	/**
	 * @param n00
	 * @param n01
	 * @param n02
	 * @param n03 numbers which define the 4x4 matrix to be multiplied
	 * @param n10
	 * @param n11
	 * @param n12
	 * @param n13 numbers which define the 4x4 matrix to be multiplied
	 * @param n20 numbers which define the 4x4 matrix to be multiplied
	 * @param n21 numbers which define the 4x4 matrix to be multiplied
	 * @param n22 numbers which define the 4x4 matrix to be multiplied
	 * @param n23 numbers which define the 4x4 matrix to be multiplied
	 * @param n30 numbers which define the 4x4 matrix to be multiplied
	 * @param n31 numbers which define the 4x4 matrix to be multiplied
	 * @param n32 numbers which define the 4x4 matrix to be multiplied
	 * @param n33 numbers which define the 4x4 matrix to be multiplied
	 */
	public void applyMatrix(float n00, float n01, float n02, float n03, float n10, float n11, float n12, float n13, float n20, float n21, float n22, float n23, float n30, float n31, float n32, float n33) {
		applet.applyMatrix(n00, n01, n02, n03, n10, n11, n12, n13, n20, n21, n22, n23, n30, n31, n32, n33);
	}

	public PMatrix getMatrix() {
		return applet.getMatrix();
	}

	/**
	 * Copy the current transformation matrix into the specified target. Pass in null to create a new matrix.
	 *
	 * @param target
	 */
	public PMatrix2D getMatrix(PMatrix2D target) {
		return applet.getMatrix(target);
	}

	/**
	 * Copy the current transformation matrix into the specified target. Pass in null to create a new matrix.
	 *
	 * @param target
	 */
	public PMatrix3D getMatrix(PMatrix3D target) {
		return applet.getMatrix(target);
	}

	/**
	 * Set the current transformation matrix to the contents of another.
	 *
	 * @param source
	 */
	public void setMatrix(PMatrix source) {
		applet.setMatrix(source);
	}

	/**
	 * Set the current transformation to the contents of the specified source.
	 *
	 * @param source
	 */
	public void setMatrix(PMatrix2D source) {
		applet.setMatrix(source);
	}

	/**
	 * Set the current transformation to the contents of the specified source.
	 *
	 * @param source
	 */
	public void setMatrix(PMatrix3D source) {
		applet.setMatrix(source);
	}

	/**
	 * ( begin auto-generated from printMatrix.xml )
	 * <p>
	 * Prints the current matrix to the Console (the text window at the bottom of Processing).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref transform
	 * @see processing.core.PGraphics#pushMatrix()
	 * @see processing.core.PGraphics#popMatrix()
	 * @see processing.core.PGraphics#resetMatrix()
	 * @see processing.core.PGraphics#applyMatrix(processing.core.PMatrix)
	 */
	public void printMatrix() {
		applet.printMatrix();
	}

	/**
	 * ( begin auto-generated from beginCamera.xml )
	 * <p>
	 * The <b>beginCamera()</b> and <b>endCamera()</b> functions enable advanced customization of the camera space. The functions are useful if
	 * you want to more control over camera movement, however for most users, the <b>camera()</b> function will be sufficient.<br /><br />The
	 * camera functions will replace any transformations (such as <b>rotate()</b> or <b>translate()</b>) that occur before them in <b>draw()</b>,
	 * but they will not automatically replace the camera transform itself. For this reason, camera functions should be placed at the beginning of
	 * <b>draw()</b> (so that transformations happen afterwards), and the <b>camera()</b> function can be used after <b>beginCamera()</b> if you
	 * want to reset the camera before applying transformations.<br /><br />This function sets the matrix mode to the camera matrix so calls such
	 * as <b>translate()</b>, <b>rotate()</b>, applyMatrix() and resetMatrix() affect the camera. <b>beginCamera()</b> should always be used with
	 * a following <b>endCamera()</b> and pairs of <b>beginCamera()</b> and <b>endCamera()</b> cannot be nested.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:camera
	 * @see processing.core.PGraphics#camera()
	 * @see processing.core.PGraphics#endCamera()
	 * @see processing.core.PGraphics#applyMatrix(processing.core.PMatrix)
	 * @see processing.core.PGraphics#resetMatrix()
	 * @see processing.core.PGraphics#translate(float, float, float)
	 * @see processing.core.PGraphics#scale(float, float, float)
	 */
	public void beginCamera() {
		applet.beginCamera();
	}

	/**
	 * ( begin auto-generated from endCamera.xml )
	 * <p>
	 * The <b>beginCamera()</b> and <b>endCamera()</b> functions enable advanced customization of the camera space. Please see the reference for
	 * <b>beginCamera()</b> for a description of how the functions are used.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:camera
	 * @see processing.core.PGraphics#camera(float, float, float, float, float, float, float, float, float)
	 */
	public void endCamera() {
		applet.endCamera();
	}

	/**
	 * ( begin auto-generated from camera.xml )
	 * <p>
	 * Sets the position of the camera through setting the eye position, the center of the scene, and which axis is facing upward. Moving the eye
	 * position and the direction it is pointing (the center of the scene) allows the images to be seen from different angles. The version without
	 * any parameters sets the camera to the default position, pointing to the center of the display window with the Y axis as up. The default
	 * values are <b>camera(width/2.0, height/2.0, (height/2.0) / tan(PI*30.0 / 180.0), width/2.0, height/2.0, 0, 0, 1, 0)</b>. This function is
	 * similar to <b>gluLookAt()</b> in OpenGL, but it first clears the current camera settings.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:camera
	 * @see processing.core.PGraphics#endCamera()
	 * @see processing.core.PGraphics#frustum(float, float, float, float, float, float)
	 */
	public void camera() {
		applet.camera();
	}

	/**
	 * @param eyeX    x-coordinate for the eye
	 * @param eyeY    y-coordinate for the eye
	 * @param eyeZ    z-coordinate for the eye
	 * @param centerX x-coordinate for the center of the scene
	 * @param centerY y-coordinate for the center of the scene
	 * @param centerZ z-coordinate for the center of the scene
	 * @param upX     usually 0.0, 1.0, or -1.0
	 * @param upY     usually 0.0, 1.0, or -1.0
	 * @param upZ     usually 0.0, 1.0, or -1.0
	 */
	public void camera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
		applet.camera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
	}

	/**
	 * ( begin auto-generated from printCamera.xml )
	 * <p>
	 * Prints the current camera matrix to the Console (the text window at the bottom of Processing).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:camera
	 * @see processing.core.PGraphics#camera(float, float, float, float, float, float, float, float, float)
	 */
	public void printCamera() {
		applet.printCamera();
	}

	/**
	 * ( begin auto-generated from ortho.xml )
	 * <p>
	 * Sets an orthographic projection and defines a parallel clipping volume. All objects with the same dimension appear the same size,
	 * regardless of whether they are near or far from the camera. The parameters to this function specify the clipping volume where left and
	 * right are the minimum and maximum x values, top and bottom are the minimum and maximum y values, and near and far are the minimum and
	 * maximum z values. If no parameters are given, the default is used: ortho(0, width, 0, height, -10, 10).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:camera
	 */
	public void ortho() {
		applet.ortho();
	}

	/**
	 * @param left   left plane of the clipping volume
	 * @param right  right plane of the clipping volume
	 * @param bottom bottom plane of the clipping volume
	 * @param top    top plane of the clipping volume
	 */
	public void ortho(float left, float right, float bottom, float top) {
		applet.ortho(left, right, bottom, top);
	}

	/**
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 * @param near   maximum distance from the origin to the viewer
	 * @param far    maximum distance from the origin away from the viewer
	 */
	public void ortho(float left, float right, float bottom, float top, float near, float far) {
		applet.ortho(left, right, bottom, top, near, far);
	}

	/**
	 * ( begin auto-generated from perspective.xml )
	 * <p>
	 * Sets a perspective projection applying foreshortening, making distant objects appear smaller than closer ones. The parameters define a
	 * viewing volume with the shape of truncated pyramid. Objects near to the front of the volume appear their actual size, while farther objects
	 * appear smaller. This projection simulates the perspective of the world more accurately than orthographic projection. The version of
	 * perspective without parameters sets the default perspective and the version with four parameters allows the programmer to set the area
	 * precisely. The default values are: perspective(PI/3.0, width/height, cameraZ/10.0, cameraZ*10.0) where cameraZ is ((height/2.0) /
	 * tan(PI*60.0/360.0));
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:camera
	 */
	public void perspective() {
		applet.perspective();
	}

	/**
	 * @param fovy   field-of-view angle (in radians) for vertical direction
	 * @param aspect ratio of width to height
	 * @param zNear  z-position of nearest clipping plane
	 * @param zFar   z-position of farthest clipping plane
	 */
	public void perspective(float fovy, float aspect, float zNear, float zFar) {
		applet.perspective(fovy, aspect, zNear, zFar);
	}

	/**
	 * ( begin auto-generated from frustum.xml )
	 * <p>
	 * Sets a perspective matrix defined through the parameters. Works like glFrustum, except it wipes out the current perspective matrix rather
	 * than muliplying itself with it.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param left   left coordinate of the clipping plane
	 * @param right  right coordinate of the clipping plane
	 * @param bottom bottom coordinate of the clipping plane
	 * @param top    top coordinate of the clipping plane
	 * @param near   near component of the clipping plane; must be greater than zero
	 * @param far    far component of the clipping plane; must be greater than the near value
	 * @webref lights_camera:camera
	 * @see processing.core.PGraphics#camera(float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#endCamera()
	 * @see processing.core.PGraphics#perspective(float, float, float, float)
	 */
	public void frustum(float left, float right, float bottom, float top, float near, float far) {
		applet.frustum(left, right, bottom, top, near, far);
	}

	/**
	 * ( begin auto-generated from printProjection.xml )
	 * <p>
	 * Prints the current projection matrix to the Console (the text window at the bottom of Processing).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:camera
	 * @see processing.core.PGraphics#camera(float, float, float, float, float, float, float, float, float)
	 */
	public void printProjection() {
		applet.printProjection();
	}

	/**
	 * ( begin auto-generated from screenX.xml )
	 * <p>
	 * Takes a three-dimensional X, Y, Z position and returns the X value for where it will appear on a (two-dimensional) screen.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x 3D x-coordinate to be mapped
	 * @param y 3D y-coordinate to be mapped
	 * @webref lights_camera:coordinates
	 * @see processing.core.PGraphics#screenY(float, float, float)
	 * @see processing.core.PGraphics#screenZ(float, float, float)
	 */
	public float screenX(float x, float y) {
		return applet.screenX(x, y);
	}

	/**
	 * ( begin auto-generated from screenY.xml )
	 * <p>
	 * Takes a three-dimensional X, Y, Z position and returns the Y value for where it will appear on a (two-dimensional) screen.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x 3D x-coordinate to be mapped
	 * @param y 3D y-coordinate to be mapped
	 * @webref lights_camera:coordinates
	 * @see processing.core.PGraphics#screenX(float, float, float)
	 * @see processing.core.PGraphics#screenZ(float, float, float)
	 */
	public float screenY(float x, float y) {
		return applet.screenY(x, y);
	}

	/**
	 * @param x
	 * @param y
	 * @param z 3D z-coordinate to be mapped
	 */
	public float screenX(float x, float y, float z) {
		return applet.screenX(x, y, z);
	}

	/**
	 * @param x
	 * @param y
	 * @param z 3D z-coordinate to be mapped
	 */
	public float screenY(float x, float y, float z) {
		return applet.screenY(x, y, z);
	}

	/**
	 * ( begin auto-generated from screenZ.xml )
	 * <p>
	 * Takes a three-dimensional X, Y, Z position and returns the Z value for where it will appear on a (two-dimensional) screen.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x 3D x-coordinate to be mapped
	 * @param y 3D y-coordinate to be mapped
	 * @param z 3D z-coordinate to be mapped
	 * @webref lights_camera:coordinates
	 * @see processing.core.PGraphics#screenX(float, float, float)
	 * @see processing.core.PGraphics#screenY(float, float, float)
	 */
	public float screenZ(float x, float y, float z) {
		return applet.screenZ(x, y, z);
	}

	/**
	 * ( begin auto-generated from modelX.xml )
	 * <p>
	 * Returns the three-dimensional X, Y, Z position in model space. This returns the X value for a given coordinate based on the current set of
	 * transformations (scale, rotate, translate, etc.) The X value can be used to place an object in space relative to the location of the
	 * original point once the transformations are no longer in use. <br/> <br/> In the example, the <b>modelX()</b>, <b>modelY()</b>, and
	 * <b>modelZ()</b> functions record the location of a box in space after being placed using a series of translate and rotate commands. After
	 * popMatrix() is called, those transformations no longer apply, but the (x, y, z) coordinate returned by the model functions is used to place
	 * another box in the same location.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x 3D x-coordinate to be mapped
	 * @param y 3D y-coordinate to be mapped
	 * @param z 3D z-coordinate to be mapped
	 * @webref lights_camera:coordinates
	 * @see processing.core.PGraphics#modelY(float, float, float)
	 * @see processing.core.PGraphics#modelZ(float, float, float)
	 */
	public float modelX(float x, float y, float z) {
		return applet.modelX(x, y, z);
	}

	/**
	 * ( begin auto-generated from modelY.xml )
	 * <p>
	 * Returns the three-dimensional X, Y, Z position in model space. This returns the Y value for a given coordinate based on the current set of
	 * transformations (scale, rotate, translate, etc.) The Y value can be used to place an object in space relative to the location of the
	 * original point once the transformations are no longer in use.<br /> <br /> In the example, the <b>modelX()</b>, <b>modelY()</b>, and
	 * <b>modelZ()</b> functions record the location of a box in space after being placed using a series of translate and rotate commands. After
	 * popMatrix() is called, those transformations no longer apply, but the (x, y, z) coordinate returned by the model functions is used to place
	 * another box in the same location.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x 3D x-coordinate to be mapped
	 * @param y 3D y-coordinate to be mapped
	 * @param z 3D z-coordinate to be mapped
	 * @webref lights_camera:coordinates
	 * @see processing.core.PGraphics#modelX(float, float, float)
	 * @see processing.core.PGraphics#modelZ(float, float, float)
	 */
	public float modelY(float x, float y, float z) {
		return applet.modelY(x, y, z);
	}

	/**
	 * ( begin auto-generated from modelZ.xml )
	 * <p>
	 * Returns the three-dimensional X, Y, Z position in model space. This returns the Z value for a given coordinate based on the current set of
	 * transformations (scale, rotate, translate, etc.) The Z value can be used to place an object in space relative to the location of the
	 * original point once the transformations are no longer in use.<br /> <br /> In the example, the <b>modelX()</b>, <b>modelY()</b>, and
	 * <b>modelZ()</b> functions record the location of a box in space after being placed using a series of translate and rotate commands. After
	 * popMatrix() is called, those transformations no longer apply, but the (x, y, z) coordinate returned by the model functions is used to place
	 * another box in the same location.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x 3D x-coordinate to be mapped
	 * @param y 3D y-coordinate to be mapped
	 * @param z 3D z-coordinate to be mapped
	 * @webref lights_camera:coordinates
	 * @see processing.core.PGraphics#modelX(float, float, float)
	 * @see processing.core.PGraphics#modelY(float, float, float)
	 */
	public float modelZ(float x, float y, float z) {
		return applet.modelZ(x, y, z);
	}

	/**
	 * ( begin auto-generated from pushStyle.xml )
	 * <p>
	 * The <b>pushStyle()</b> function saves the current style settings and <b>popStyle()</b> restores the prior settings. Note that these
	 * functions are always used together. They allow you to change the style settings and later return to what you had. When a new style is
	 * started with <b>pushStyle()</b>, it builds on the current style information. The <b>pushStyle()</b> and <b>popStyle()</b> functions can be
	 * embedded to provide more control (see the second example above for a demonstration.) <br /><br /> The style information controlled by the
	 * following functions are included in the style: fill(), stroke(), tint(), strokeWeight(), strokeCap(), strokeJoin(), imageMode(),
	 * rectMode(), ellipseMode(), shapeMode(), colorMode(), textAlign(), textFont(), textMode(), textSize(), textLeading(), emissive(),
	 * specular(), shininess(), ambient()
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref structure
	 * @see processing.core.PGraphics#popStyle()
	 */
	public void pushStyle() {
		applet.pushStyle();
	}

	/**
	 * ( begin auto-generated from popStyle.xml )
	 * <p>
	 * The <b>pushStyle()</b> function saves the current style settings and <b>popStyle()</b> restores the prior settings; these functions are
	 * always used together. They allow you to change the style settings and later return to what you had. When a new style is started with
	 * <b>pushStyle()</b>, it builds on the current style information. The <b>pushStyle()</b> and <b>popStyle()</b> functions can be embedded to
	 * provide more control (see the second example above for a demonstration.)
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref structure
	 * @see processing.core.PGraphics#pushStyle()
	 */
	public void popStyle() {
		applet.popStyle();
	}

	public void style(PStyle s) {
		applet.style(s);
	}

	/**
	 * ( begin auto-generated from strokeWeight.xml )
	 * <p>
	 * Sets the width of the stroke used for lines, points, and the border around shapes. All widths are set in units of pixels. <br/> <br/> When
	 * drawing with P3D, series of connected lines (such as the stroke around a polygon, triangle, or ellipse) produce unattractive results when a
	 * thick stroke weight is set (<a href="http://code.google.com/p/processing/issues/detail?id=123">see Issue 123</a>). With P3D, the minimum
	 * and maximum values for <b>strokeWeight()</b> are controlled by the graphics card and the operating system's OpenGL implementation. For
	 * instance, the thickness may not go higher than 10 pixels.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param weight the weight (in pixels) of the stroke
	 * @webref shape:attributes
	 * @see processing.core.PGraphics#stroke(int, float)
	 * @see processing.core.PGraphics#strokeJoin(int)
	 * @see processing.core.PGraphics#strokeCap(int)
	 */
	public void strokeWeight(float weight) {
		applet.strokeWeight(weight);
	}

	/**
	 * ( begin auto-generated from strokeJoin.xml )
	 * <p>
	 * Sets the style of the joints which connect line segments. These joints are either mitered, beveled, or rounded and specified with the
	 * corresponding parameters MITER, BEVEL, and ROUND. The default joint is MITER. <br/> <br/> This function is not available with the P3D
	 * renderer, (<a href="http://code.google.com/p/processing/issues/detail?id=123">see Issue 123</a>). More information about the renderers can
	 * be found in the <b>size()</b> reference.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param join either MITER, BEVEL, ROUND
	 * @webref shape:attributes
	 * @see processing.core.PGraphics#stroke(int, float)
	 * @see processing.core.PGraphics#strokeWeight(float)
	 * @see processing.core.PGraphics#strokeCap(int)
	 */
	public void strokeJoin(int join) {
		applet.strokeJoin(join);
	}

	/**
	 * ( begin auto-generated from strokeCap.xml )
	 * <p>
	 * Sets the style for rendering line endings. These ends are either squared, extended, or rounded and specified with the corresponding
	 * parameters SQUARE, PROJECT, and ROUND. The default cap is ROUND. <br/> <br/> This function is not available with the P3D renderer (<a
	 * href="http://code.google.com/p/processing/issues/detail?id=123">see Issue 123</a>). More information about the renderers can be found in
	 * the <b>size()</b> reference.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param cap either SQUARE, PROJECT, or ROUND
	 * @webref shape:attributes
	 * @see processing.core.PGraphics#stroke(int, float)
	 * @see processing.core.PGraphics#strokeWeight(float)
	 * @see processing.core.PGraphics#strokeJoin(int)
	 * @see processing.core.PApplet#size(int, int, String, String)
	 */
	public void strokeCap(int cap) {
		applet.strokeCap(cap);
	}

	/**
	 * ( begin auto-generated from noStroke.xml )
	 * <p>
	 * Disables drawing the stroke (outline). If both <b>noStroke()</b> and <b>noFill()</b> are called, nothing will be drawn to the screen.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref color:setting
	 * @see processing.core.PGraphics#stroke(int, float)
	 * @see processing.core.PGraphics#fill(float, float, float, float)
	 * @see processing.core.PGraphics#noFill()
	 */
	public void noStroke() {
		applet.noStroke();
	}

	/**
	 * ( begin auto-generated from stroke.xml )
	 * <p>
	 * Sets the color used to draw lines and borders around shapes. This color is either specified in terms of the RGB or HSB color depending on
	 * the current <b>colorMode()</b> (the default color space is RGB, with each value in the range from 0 to 255). <br/> <br/> When using
	 * hexadecimal notation to specify a color, use "#" or "0x" before the values (e.g. #CCFFAA, 0xFFCCFFAA). The # syntax uses six digits to
	 * specify a color (the way colors are specified in HTML and CSS). When using the hexadecimal notation starting with "0x", the hexadecimal
	 * value must be specified with eight characters; the first two characters define the alpha component and the remainder the red, green, and
	 * blue components. <br/> <br/> The value for the parameter "gray" must be less than or equal to the current maximum value as specified by
	 * <b>colorMode()</b>. The default maximum value is 255.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb color value in hexadecimal notation
	 * @see processing.core.PGraphics#noStroke()
	 * @see processing.core.PGraphics#strokeWeight(float)
	 * @see processing.core.PGraphics#strokeJoin(int)
	 * @see processing.core.PGraphics#strokeCap(int)
	 * @see processing.core.PGraphics#fill(int, float)
	 * @see processing.core.PGraphics#noFill()
	 * @see processing.core.PGraphics#tint(int, float)
	 * @see processing.core.PGraphics#background(float, float, float, float)
	 * @see processing.core.PGraphics#colorMode(int, float, float, float, float)
	 */
	public void stroke(int rgb) {
		applet.stroke(rgb);
	}

	/**
	 * @param rgb
	 * @param alpha opacity of the stroke
	 */
	public void stroke(int rgb, float alpha) {
		applet.stroke(rgb, alpha);
	}

	/**
	 * @param gray specifies a value between white and black
	 */
	public void stroke(float gray) {
		applet.stroke(gray);
	}

	public void stroke(float gray, float alpha) {
		applet.stroke(gray, alpha);
	}

	/**
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 * @webref color:setting
	 */
	public void stroke(float v1, float v2, float v3) {
		applet.stroke(v1, v2, v3);
	}

	public void stroke(float v1, float v2, float v3, float alpha) {
		applet.stroke(v1, v2, v3, alpha);
	}

	/**
	 * ( begin auto-generated from noTint.xml )
	 * <p>
	 * Removes the current fill value for displaying images and reverts to displaying images with their original hues.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref image:loading_displaying
	 * @usage web_application
	 * @see processing.core.PGraphics#tint(float, float, float, float)
	 * @see processing.core.PGraphics#image(processing.core.PImage, float, float, float, float)
	 */
	public void noTint() {
		applet.noTint();
	}

	/**
	 * ( begin auto-generated from tint.xml )
	 * <p>
	 * Sets the fill value for displaying images. Images can be tinted to specified colors or made transparent by setting the alpha.<br /> <br />
	 * To make an image transparent, but not change it's color, use white as the tint color and specify an alpha value. For instance, tint(255,
	 * 128) will make an image 50% transparent (unless <b>colorMode()</b> has been used).<br /> <br /> When using hexadecimal notation to specify
	 * a color, use "#" or "0x" before the values (e.g. #CCFFAA, 0xFFCCFFAA). The # syntax uses six digits to specify a color (the way colors are
	 * specified in HTML and CSS). When using the hexadecimal notation starting with "0x", the hexadecimal value must be specified with eight
	 * characters; the first two characters define the alpha component and the remainder the red, green, and blue components.<br /> <br /> The
	 * value for the parameter "gray" must be less than or equal to the current maximum value as specified by <b>colorMode()</b>. The default
	 * maximum value is 255.<br /> <br /> The <b>tint()</b> function is also used to control the coloring of textures in 3D.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb color value in hexadecimal notation
	 * @webref image:loading_displaying
	 * @usage web_application
	 * @see processing.core.PGraphics#noTint()
	 * @see processing.core.PGraphics#image(processing.core.PImage, float, float, float, float)
	 */
	public void tint(int rgb) {
		applet.tint(rgb);
	}

	/**
	 * @param rgb
	 * @param alpha opacity of the image
	 */
	public void tint(int rgb, float alpha) {
		applet.tint(rgb, alpha);
	}

	/**
	 * @param gray specifies a value between white and black
	 */
	public void tint(float gray) {
		applet.tint(gray);
	}

	public void tint(float gray, float alpha) {
		applet.tint(gray, alpha);
	}

	/**
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 */
	public void tint(float v1, float v2, float v3) {
		applet.tint(v1, v2, v3);
	}

	public void tint(float v1, float v2, float v3, float alpha) {
		applet.tint(v1, v2, v3, alpha);
	}

	/**
	 * ( begin auto-generated from noFill.xml )
	 * <p>
	 * Disables filling geometry. If both <b>noStroke()</b> and <b>noFill()</b> are called, nothing will be drawn to the screen.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref color:setting
	 * @usage web_application
	 * @see processing.core.PGraphics#fill(float, float, float, float)
	 * @see processing.core.PGraphics#stroke(int, float)
	 * @see processing.core.PGraphics#noStroke()
	 */
	public void noFill() {
		applet.noFill();
	}

	/**
	 * ( begin auto-generated from fill.xml )
	 * <p>
	 * Sets the color used to fill shapes. For example, if you run <b>fill(204, 102, 0)</b>, all subsequent shapes will be filled with orange.
	 * This color is either specified in terms of the RGB or HSB color depending on the current <b>colorMode()</b> (the default color space is
	 * RGB, with each value in the range from 0 to 255). <br/> <br/> When using hexadecimal notation to specify a color, use "#" or "0x" before
	 * the values (e.g. #CCFFAA, 0xFFCCFFAA). The # syntax uses six digits to specify a color (the way colors are specified in HTML and CSS). When
	 * using the hexadecimal notation starting with "0x", the hexadecimal value must be specified with eight characters; the first two characters
	 * define the alpha component and the remainder the red, green, and blue components. <br/> <br/> The value for the parameter "gray" must be
	 * less than or equal to the current maximum value as specified by <b>colorMode()</b>. The default maximum value is 255. <br/> <br/> To change
	 * the color of an image (or a texture), use tint().
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb color variable or hex value
	 * @webref color:setting
	 * @usage web_application
	 * @see processing.core.PGraphics#noFill()
	 * @see processing.core.PGraphics#stroke(int, float)
	 * @see processing.core.PGraphics#noStroke()
	 * @see processing.core.PGraphics#tint(int, float)
	 * @see processing.core.PGraphics#background(float, float, float, float)
	 * @see processing.core.PGraphics#colorMode(int, float, float, float, float)
	 */
	public void fill(int rgb) {
		applet.fill(rgb);
	}

	/**
	 * @param rgb
	 * @param alpha opacity of the fill
	 */
	public void fill(int rgb, float alpha) {
		applet.fill(rgb, alpha);
	}

	/**
	 * @param gray number specifying value between white and black
	 */
	public void fill(float gray) {
		applet.fill(gray);
	}

	public void fill(float gray, float alpha) {
		applet.fill(gray, alpha);
	}

	/**
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 */
	public void fill(float v1, float v2, float v3) {
		applet.fill(v1, v2, v3);
	}

	public void fill(float v1, float v2, float v3, float alpha) {
		applet.fill(v1, v2, v3, alpha);
	}

	/**
	 * ( begin auto-generated from ambient.xml )
	 * <p>
	 * Sets the ambient reflectance for shapes drawn to the screen. This is combined with the ambient light component of environment. The color
	 * components set through the parameters define the reflectance. For example in the default color mode, setting v1=255, v2=126, v3=0, would
	 * cause all the red light to reflect and half of the green light to reflect. Used in combination with <b>emissive()</b>, <b>specular()</b>,
	 * and <b>shininess()</b> in setting the material properties of shapes.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb any value of the color datatype
	 * @webref lights_camera:material_properties
	 * @usage web_application
	 * @see processing.core.PGraphics#emissive(float, float, float)
	 * @see processing.core.PGraphics#specular(float, float, float)
	 * @see processing.core.PGraphics#shininess(float)
	 */
	public void ambient(int rgb) {
		applet.ambient(rgb);
	}

	/**
	 * @param gray number specifying value between white and black
	 */
	public void ambient(float gray) {
		applet.ambient(gray);
	}

	/**
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 */
	public void ambient(float v1, float v2, float v3) {
		applet.ambient(v1, v2, v3);
	}

	/**
	 * ( begin auto-generated from specular.xml )
	 * <p>
	 * Sets the specular color of the materials used for shapes drawn to the screen, which sets the color of hightlights. Specular refers to light
	 * which bounces off a surface in a perferred direction (rather than bouncing in all directions like a diffuse light). Used in combination
	 * with <b>emissive()</b>, <b>ambient()</b>, and <b>shininess()</b> in setting the material properties of shapes.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb color to set
	 * @webref lights_camera:material_properties
	 * @usage web_application
	 * @see processing.core.PGraphics#lightSpecular(float, float, float)
	 * @see processing.core.PGraphics#ambient(float, float, float)
	 * @see processing.core.PGraphics#emissive(float, float, float)
	 * @see processing.core.PGraphics#shininess(float)
	 */
	public void specular(int rgb) {
		applet.specular(rgb);
	}

	/**
	 * gray number specifying value between white and black
	 *
	 * @param gray
	 */
	public void specular(float gray) {
		applet.specular(gray);
	}

	/**
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 */
	public void specular(float v1, float v2, float v3) {
		applet.specular(v1, v2, v3);
	}

	/**
	 * ( begin auto-generated from shininess.xml )
	 * <p>
	 * Sets the amount of gloss in the surface of shapes. Used in combination with <b>ambient()</b>, <b>specular()</b>, and <b>emissive()</b> in
	 * setting the material properties of shapes.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param shine degree of shininess
	 * @webref lights_camera:material_properties
	 * @usage web_application
	 * @see processing.core.PGraphics#emissive(float, float, float)
	 * @see processing.core.PGraphics#ambient(float, float, float)
	 * @see processing.core.PGraphics#specular(float, float, float)
	 */
	public void shininess(float shine) {
		applet.shininess(shine);
	}

	/**
	 * ( begin auto-generated from emissive.xml )
	 * <p>
	 * Sets the emissive color of the material used for drawing shapes drawn to the screen. Used in combination with <b>ambient()</b>,
	 * <b>specular()</b>, and <b>shininess()</b> in setting the material properties of shapes.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb color to set
	 * @webref lights_camera:material_properties
	 * @usage web_application
	 * @see processing.core.PGraphics#ambient(float, float, float)
	 * @see processing.core.PGraphics#specular(float, float, float)
	 * @see processing.core.PGraphics#shininess(float)
	 */
	public void emissive(int rgb) {
		applet.emissive(rgb);
	}

	/**
	 * gray number specifying value between white and black
	 *
	 * @param gray
	 */
	public void emissive(float gray) {
		applet.emissive(gray);
	}

	/**
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 */
	public void emissive(float v1, float v2, float v3) {
		applet.emissive(v1, v2, v3);
	}

	/**
	 * ( begin auto-generated from lights.xml )
	 * <p>
	 * Sets the default ambient light, directional light, falloff, and specular values. The defaults are ambientLight(128, 128, 128) and
	 * directionalLight(128, 128, 128, 0, 0, -1), lightFalloff(1, 0, 0), and lightSpecular(0, 0, 0). Lights need to be included in the draw() to
	 * remain persistent in a looping program. Placing them in the setup() of a looping program will cause them to only have an effect the first
	 * time through the loop.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:lights
	 * @usage web_application
	 * @see processing.core.PGraphics#ambientLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#directionalLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#pointLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#noLights()
	 */
	public void lights() {
		applet.lights();
	}

	/**
	 * ( begin auto-generated from noLights.xml )
	 * <p>
	 * Disable all lighting. Lighting is turned off by default and enabled with the <b>lights()</b> function. This function can be used to disable
	 * lighting so that 2D geometry (which does not require lighting) can be drawn after a set of lighted 3D geometry.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref lights_camera:lights
	 * @usage web_application
	 * @see processing.core.PGraphics#lights()
	 */
	public void noLights() {
		applet.noLights();
	}

	/**
	 * ( begin auto-generated from ambientLight.xml )
	 * <p>
	 * Adds an ambient light. Ambient light doesn't come from a specific direction, the rays have light have bounced around so much that objects
	 * are evenly lit from all sides. Ambient lights are almost always used in combination with other types of lights. Lights need to be included
	 * in the <b>draw()</b> to remain persistent in a looping program. Placing them in the <b>setup()</b> of a looping program will cause them to
	 * only have an effect the first time through the loop. The effect of the parameters is determined by the current color mode.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 * @webref lights_camera:lights
	 * @usage web_application
	 * @see processing.core.PGraphics#lights()
	 * @see processing.core.PGraphics#directionalLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#pointLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
	 */
	public void ambientLight(float v1, float v2, float v3) {
		applet.ambientLight(v1, v2, v3);
	}

	/**
	 * @param v1
	 * @param v2
	 * @param v3
	 * @param x  x-coordinate of the light
	 * @param y  y-coordinate of the light
	 * @param z  z-coordinate of the light
	 */
	public void ambientLight(float v1, float v2, float v3, float x, float y, float z) {
		applet.ambientLight(v1, v2, v3, x, y, z);
	}

	/**
	 * ( begin auto-generated from directionalLight.xml )
	 * <p>
	 * Adds a directional light. Directional light comes from one direction and is stronger when hitting a surface squarely and weaker if it hits
	 * at a a gentle angle. After hitting a surface, a directional lights scatters in all directions. Lights need to be included in the
	 * <b>draw()</b> to remain persistent in a looping program. Placing them in the <b>setup()</b> of a looping program will cause them to only
	 * have an effect the first time through the loop. The affect of the <b>v1</b>, <b>v2</b>, and <b>v3</b> parameters is determined by the
	 * current color mode. The <b>nx</b>, <b>ny</b>, and <b>nz</b> parameters specify the direction the light is facing. For example, setting
	 * <b>ny</b> to -1 will cause the geometry to be lit from below (the light is facing directly upward).
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 * @param nx direction along the x-axis
	 * @param ny direction along the y-axis
	 * @param nz direction along the z-axis
	 * @webref lights_camera:lights
	 * @usage web_application
	 * @see processing.core.PGraphics#lights()
	 * @see processing.core.PGraphics#ambientLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#pointLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
	 */
	public void directionalLight(float v1, float v2, float v3, float nx, float ny, float nz) {
		applet.directionalLight(v1, v2, v3, nx, ny, nz);
	}

	/**
	 * ( begin auto-generated from pointLight.xml )
	 * <p>
	 * Adds a point light. Lights need to be included in the <b>draw()</b> to remain persistent in a looping program. Placing them in the
	 * <b>setup()</b> of a looping program will cause them to only have an effect the first time through the loop. The affect of the <b>v1</b>,
	 * <b>v2</b>, and <b>v3</b> parameters is determined by the current color mode. The <b>x</b>, <b>y</b>, and <b>z</b> parameters set the
	 * position of the light.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 * @param x  x-coordinate of the light
	 * @param y  y-coordinate of the light
	 * @param z  z-coordinate of the light
	 * @webref lights_camera:lights
	 * @usage web_application
	 * @see processing.core.PGraphics#lights()
	 * @see processing.core.PGraphics#directionalLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#ambientLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
	 */
	public void pointLight(float v1, float v2, float v3, float x, float y, float z) {
		applet.pointLight(v1, v2, v3, x, y, z);
	}

	/**
	 * ( begin auto-generated from spotLight.xml )
	 * <p>
	 * Adds a spot light. Lights need to be included in the <b>draw()</b> to remain persistent in a looping program. Placing them in the
	 * <b>setup()</b> of a looping program will cause them to only have an effect the first time through the loop. The affect of the <b>v1</b>,
	 * <b>v2</b>, and <b>v3</b> parameters is determined by the current color mode. The <b>x</b>, <b>y</b>, and <b>z</b> parameters specify the
	 * position of the light and <b>nx</b>, <b>ny</b>, <b>nz</b> specify the direction or light. The <b>angle</b> parameter affects angle of the
	 * spotlight cone.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param v1            red or hue value (depending on current color mode)
	 * @param v2            green or saturation value (depending on current color mode)
	 * @param v3            blue or brightness value (depending on current color mode)
	 * @param x             x-coordinate of the light
	 * @param y             y-coordinate of the light
	 * @param z             z-coordinate of the light
	 * @param nx            direction along the x axis
	 * @param ny            direction along the y axis
	 * @param nz            direction along the z axis
	 * @param angle         angle of the spotlight cone
	 * @param concentration exponent determining the center bias of the cone
	 * @webref lights_camera:lights
	 * @usage web_application
	 * @see processing.core.PGraphics#lights()
	 * @see processing.core.PGraphics#directionalLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#pointLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#ambientLight(float, float, float, float, float, float)
	 */
	public void spotLight(float v1, float v2, float v3, float x, float y, float z, float nx, float ny, float nz, float angle, float concentration) {
		applet.spotLight(v1, v2, v3, x, y, z, nx, ny, nz, angle, concentration);
	}

	/**
	 * ( begin auto-generated from lightFalloff.xml )
	 * <p>
	 * Sets the falloff rates for point lights, spot lights, and ambient lights. The parameters are used to determine the falloff with the
	 * following equation:<br /><br />d = distance from light position to vertex position<br />falloff = 1 / (CONSTANT + d * LINEAR + (d*d) *
	 * QUADRATIC)<br /><br />Like <b>fill()</b>, it affects only the elements which are created after it in the code. The default value if
	 * <b>LightFalloff(1.0, 0.0, 0.0)</b>. Thinking about an ambient light with a falloff can be tricky. It is used, for example, if you wanted a
	 * region of your scene to be lit ambiently one color and another region to be lit ambiently by another color, you would use an ambient light
	 * with location and falloff. You can think of it as a point light that doesn't care which direction a surface is facing.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param constant  constant value or determining falloff
	 * @param linear    linear value for determining falloff
	 * @param quadratic quadratic value for determining falloff
	 * @webref lights_camera:lights
	 * @usage web_application
	 * @see processing.core.PGraphics#lights()
	 * @see processing.core.PGraphics#ambientLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#pointLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
	 * @see processing.core.PGraphics#lightSpecular(float, float, float)
	 */
	public void lightFalloff(float constant, float linear, float quadratic) {
		applet.lightFalloff(constant, linear, quadratic);
	}

	/**
	 * ( begin auto-generated from lightSpecular.xml )
	 * <p>
	 * Sets the specular color for lights. Like <b>fill()</b>, it affects only the elements which are created after it in the code. Specular
	 * refers to light which bounces off a surface in a perferred direction (rather than bouncing in all directions like a diffuse light) and is
	 * used for creating highlights. The specular quality of a light interacts with the specular material qualities set through the
	 * <b>specular()</b> and <b>shininess()</b> functions.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param v1 red or hue value (depending on current color mode)
	 * @param v2 green or saturation value (depending on current color mode)
	 * @param v3 blue or brightness value (depending on current color mode)
	 * @webref lights_camera:lights
	 * @usage web_application
	 * @see processing.core.PGraphics#specular(float, float, float)
	 * @see processing.core.PGraphics#lights()
	 * @see processing.core.PGraphics#ambientLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#pointLight(float, float, float, float, float, float)
	 * @see processing.core.PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
	 */
	public void lightSpecular(float v1, float v2, float v3) {
		applet.lightSpecular(v1, v2, v3);
	}

	/**
	 * ( begin auto-generated from background.xml ) <p> The <b>background()</b> function sets the color used for the background of the Processing
	 * window. The default background is light gray. In the <b>draw()</b> function, the background color is used to clear the display window at
	 * the beginning of each frame. <br/> <br/> An image can also be used as the background for a sketch, however its width and height must be the
	 * same size as the sketch window. To resize an image 'b' to the size of the sketch window, use b.resize(width, height). <br/> <br/> Images
	 * used as background will ignore the current <b>tint()</b> setting. <br/> <br/> It is not possible to use transparency (alpha) in background
	 * colors with the main drawing surface, however they will work properly with <b>createGraphics()</b>. <p> ( end auto-generated ) <p>
	 * <h3>Advanced</h3> <p>Clear the background with a color that includes an alpha value. This can only be used with objects created by
	 * createGraphics(), because the main drawing surface cannot be set transparent.</p> <p>It might be tempting to use this function to partially
	 * clear the screen on each frame, however that's not how this function works. When calling background(), the pixels will be replaced with
	 * pixels that have that level of transparency. To do a semi-transparent overlay, use fill() with alpha and draw a rectangle.</p>
	 *
	 * @param rgb any value of the color datatype
	 * @webref color:setting
	 * @usage web_application
	 * @see processing.core.PGraphics#stroke(float)
	 * @see processing.core.PGraphics#fill(float)
	 * @see processing.core.PGraphics#tint(float)
	 * @see processing.core.PGraphics#colorMode(int)
	 */
	public void background(int rgb) {
		applet.background(rgb);
	}

	/**
	 * @param rgb
	 * @param alpha opacity of the background
	 */
	public void background(int rgb, float alpha) {
		applet.background(rgb, alpha);
	}

	/**
	 * @param gray specifies a value between white and black
	 */
	public void background(float gray) {
		applet.background(gray);
	}

	public void background(float gray, float alpha) {
		applet.background(gray, alpha);
	}

	/**
	 * @param v1 red or hue value (depending on the current color mode)
	 * @param v2 green or saturation value (depending on the current color mode)
	 * @param v3 blue or brightness value (depending on the current color mode)
	 */
	public void background(float v1, float v2, float v3) {
		applet.background(v1, v2, v3);
	}

	public void background(float v1, float v2, float v3, float alpha) {
		applet.background(v1, v2, v3, alpha);
	}

	/**
	 * @webref color:setting
	 */
	public void clear() {
		applet.clear();
	}

	/**
	 * Takes an RGB or ARGB image and sets it as the background. The width and height of the image must be the same size as the sketch. Use
	 * image.resize(width, height) to make short work of such a task.<br/> <br/> Note that even if the image is set as RGB, the high 8 bits of
	 * each pixel should be set opaque (0xFF000000) because the image data will be copied directly to the screen, and non-opaque background images
	 * may have strange behavior. Use image.filter(OPAQUE) to handle this easily.<br/> <br/> When using 3D, this will also clear the zbuffer (if
	 * it exists).
	 *
	 * @param image PImage to set as background (must be same size as the sketch window)
	 */
	public void background(PImage image) {
		applet.background(image);
	}

	/**
	 * ( begin auto-generated from colorMode.xml )
	 * <p>
	 * Changes the way Processing interprets color data. By default, the parameters for <b>fill()</b>, <b>stroke()</b>, <b>background()</b>, and
	 * <b>color()</b> are defined by values between 0 and 255 using the RGB color model. The <b>colorMode()</b> function is used to change the
	 * numerical range used for specifying colors and to switch color systems. For example, calling <b>colorMode(RGB, 1.0)</b> will specify that
	 * values are specified between 0 and 1. The limits for defining colors are altered by setting the parameters range1, range2, range3, and
	 * range 4.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param mode Either RGB or HSB, corresponding to Red/Green/Blue and Hue/Saturation/Brightness
	 * @webref color:setting
	 * @usage web_application
	 * @see processing.core.PGraphics#background(float)
	 * @see processing.core.PGraphics#fill(float)
	 * @see processing.core.PGraphics#stroke(float)
	 */
	public void colorMode(int mode) {
		applet.colorMode(mode);
	}

	/**
	 * @param mode
	 * @param max  range for all color elements
	 */
	public void colorMode(int mode, float max) {
		applet.colorMode(mode, max);
	}

	/**
	 * @param mode
	 * @param max1 range for the red or hue depending on the current color mode
	 * @param max2 range for the green or saturation depending on the current color mode
	 * @param max3 range for the blue or brightness depending on the current color mode
	 */
	public void colorMode(int mode, float max1, float max2, float max3) {
		applet.colorMode(mode, max1, max2, max3);
	}

	/**
	 * @param mode
	 * @param max1
	 * @param max2
	 * @param max3
	 * @param maxA range for the alpha
	 */
	public void colorMode(int mode, float max1, float max2, float max3, float maxA) {
		applet.colorMode(mode, max1, max2, max3, maxA);
	}

	/**
	 * ( begin auto-generated from alpha.xml )
	 * <p>
	 * Extracts the alpha value from a color.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb any value of the color datatype
	 * @webref color:creating_reading
	 * @usage web_application
	 * @see processing.core.PGraphics#red(int)
	 * @see processing.core.PGraphics#green(int)
	 * @see processing.core.PGraphics#blue(int)
	 * @see processing.core.PGraphics#hue(int)
	 * @see processing.core.PGraphics#saturation(int)
	 * @see processing.core.PGraphics#brightness(int)
	 */
	public float alpha(int rgb) {
		return applet.alpha(rgb);
	}

	/**
	 * ( begin auto-generated from red.xml )
	 * <p>
	 * Extracts the red value from a color, scaled to match current <b>colorMode()</b>. This value is always returned as a  float so be careful
	 * not to assign it to an int value.<br /><br />The red() function is easy to use and undestand, but is slower than another technique. To
	 * achieve the same results when working in <b>colorMode(RGB, 255)</b>, but with greater speed, use the &gt;&gt; (right shift) operator with a
	 * bit mask. For example, the following two lines of code are equivalent:<br
	 * /><pre>float r1 = red(myColor);<br />float r2 = myColor &gt;&gt; 16
	 * &amp; 0xFF;</pre>
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb any value of the color datatype
	 * @webref color:creating_reading
	 * @usage web_application
	 * @see_external rightshift
	 * @see processing.core.PGraphics#green(int)
	 * @see processing.core.PGraphics#blue(int)
	 * @see processing.core.PGraphics#alpha(int)
	 * @see processing.core.PGraphics#hue(int)
	 * @see processing.core.PGraphics#saturation(int)
	 * @see processing.core.PGraphics#brightness(int)
	 */
	public float red(int rgb) {
		return applet.red(rgb);
	}

	/**
	 * ( begin auto-generated from green.xml )
	 * <p>
	 * Extracts the green value from a color, scaled to match current <b>colorMode()</b>. This value is always returned as a  float so be careful
	 * not to assign it to an int value.<br /><br />The <b>green()</b> function is easy to use and undestand, but is slower than another
	 * technique. To achieve the same results when working in <b>colorMode(RGB, 255)</b>, but with greater speed, use the &gt;&gt; (right shift)
	 * operator with a bit mask. For example, the following two lines of code
	 * are equivalent:<br /><pre>float r1 = green(myColor);<br />float r2 =
	 * myColor &gt;&gt; 8 &amp; 0xFF;</pre>
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb any value of the color datatype
	 * @webref color:creating_reading
	 * @usage web_application
	 * @see_external rightshift
	 * @see processing.core.PGraphics#red(int)
	 * @see processing.core.PGraphics#blue(int)
	 * @see processing.core.PGraphics#alpha(int)
	 * @see processing.core.PGraphics#hue(int)
	 * @see processing.core.PGraphics#saturation(int)
	 * @see processing.core.PGraphics#brightness(int)
	 */
	public float green(int rgb) {
		return applet.green(rgb);
	}

	/**
	 * ( begin auto-generated from blue.xml )
	 * <p>
	 * Extracts the blue value from a color, scaled to match current <b>colorMode()</b>. This value is always returned as a  float so be careful
	 * not to assign it to an int value.<br /><br />The <b>blue()</b> function is easy to use and undestand, but is slower than another technique.
	 * To achieve the same results when working in <b>colorMode(RGB, 255)</b>, but with greater speed, use a bit mask to remove the other color
	 * components. For example, the following two lines of code are
	 * equivalent:<br /><pre>float r1 = blue(myColor);<br />float r2 = myColor
	 * &amp; 0xFF;</pre>
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb any value of the color datatype
	 * @webref color:creating_reading
	 * @usage web_application
	 * @see_external rightshift
	 * @see processing.core.PGraphics#red(int)
	 * @see processing.core.PGraphics#green(int)
	 * @see processing.core.PGraphics#alpha(int)
	 * @see processing.core.PGraphics#hue(int)
	 * @see processing.core.PGraphics#saturation(int)
	 * @see processing.core.PGraphics#brightness(int)
	 */
	public float blue(int rgb) {
		return applet.blue(rgb);
	}

	/**
	 * ( begin auto-generated from hue.xml )
	 * <p>
	 * Extracts the hue value from a color.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb any value of the color datatype
	 * @webref color:creating_reading
	 * @usage web_application
	 * @see processing.core.PGraphics#red(int)
	 * @see processing.core.PGraphics#green(int)
	 * @see processing.core.PGraphics#blue(int)
	 * @see processing.core.PGraphics#alpha(int)
	 * @see processing.core.PGraphics#saturation(int)
	 * @see processing.core.PGraphics#brightness(int)
	 */
	public float hue(int rgb) {
		return applet.hue(rgb);
	}

	/**
	 * ( begin auto-generated from saturation.xml )
	 * <p>
	 * Extracts the saturation value from a color.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb any value of the color datatype
	 * @webref color:creating_reading
	 * @usage web_application
	 * @see processing.core.PGraphics#red(int)
	 * @see processing.core.PGraphics#green(int)
	 * @see processing.core.PGraphics#blue(int)
	 * @see processing.core.PGraphics#alpha(int)
	 * @see processing.core.PGraphics#hue(int)
	 * @see processing.core.PGraphics#brightness(int)
	 */
	public float saturation(int rgb) {
		return applet.saturation(rgb);
	}

	/**
	 * ( begin auto-generated from brightness.xml )
	 * <p>
	 * Extracts the brightness value from a color.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param rgb any value of the color datatype
	 * @webref color:creating_reading
	 * @usage web_application
	 * @see processing.core.PGraphics#red(int)
	 * @see processing.core.PGraphics#green(int)
	 * @see processing.core.PGraphics#blue(int)
	 * @see processing.core.PGraphics#alpha(int)
	 * @see processing.core.PGraphics#hue(int)
	 * @see processing.core.PGraphics#saturation(int)
	 */
	public float brightness(int rgb) {
		return applet.brightness(rgb);
	}

	/**
	 * ( begin auto-generated from lerpColor.xml )
	 * <p>
	 * Calculates a color or colors between two color at a specific increment. The <b>amt</b> parameter is the amount to interpolate between the
	 * two values where 0.0 equal to the first point, 0.1 is very near the first point, 0.5 is half-way in between, etc.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param c1  interpolate from this color
	 * @param c2  interpolate to this color
	 * @param amt between 0.0 and 1.0
	 * @webref color:creating_reading
	 * @usage web_application
	 * @see processing.core.PImage#blendColor(int, int, int)
	 * @see processing.core.PGraphics#color(float, float, float, float)
	 * @see processing.core.PApplet#lerp(float, float, float)
	 */
	public int lerpColor(int c1, int c2, float amt) {
		return applet.lerpColor(c1, c2, amt);
	}

	/**
	 * @param c1
	 * @param c2
	 * @param amt
	 * @param mode
	 * @nowebref Interpolate between two colors. Like lerp(), but for the individual color components of a color supplied as an int value.
	 */
	public int lerpColor(int c1, int c2, float amt, int mode) {
		return PApplet.lerpColor(c1, c2, amt, mode);
	}

	/**
	 * Display a warning that the specified method is only available with 3D.
	 *
	 * @param method The method name (no parentheses)
	 */
	public void showDepthWarning(String method) {
		PApplet.showDepthWarning(method);
	}

	/**
	 * Display a warning that the specified method that takes x, y, z parameters can only be used with x and y parameters in this renderer.
	 *
	 * @param method The method name (no parentheses)
	 */
	public void showDepthWarningXYZ(String method) {
		PApplet.showDepthWarningXYZ(method);
	}

	/**
	 * Display a warning that the specified method is simply unavailable.
	 *
	 * @param method
	 */
	public void showMethodWarning(String method) {
		PApplet.showMethodWarning(method);
	}

	/**
	 * Error that a particular variation of a method is unavailable (even though other variations are). For instance, if vertex(x, y, u, v) is not
	 * available, but vertex(x, y) is just fine.
	 *
	 * @param str
	 */
	public void showVariationWarning(String str) {
		PApplet.showVariationWarning(str);
	}

	/**
	 * Display a warning that the specified method is not implemented, meaning that it could be either a completely missing function, although
	 * other variations of it may still work properly.
	 *
	 * @param method
	 */
	public void showMissingWarning(String method) {
		PApplet.showMissingWarning(method);
	}

	/**
	 * Return true if this renderer should be drawn to the screen. Defaults to returning true, since nearly all renderers are on-screen beasts.
	 * But can be overridden for subclasses like PDF so that a window doesn't open up. <br/> <br/> A better name? showFrame, displayable,
	 * isVisible, visible, shouldDisplay, what to call this?
	 */
	public boolean displayable() {
		return applet.displayable();
	}

	/**
	 * Return true if this renderer does rendering through OpenGL. Defaults to false.
	 */
	public boolean isGL() {
		return applet.isGL();
	}

	/**
	 * ( begin auto-generated from PImage_get.xml )
	 * <p>
	 * Reads the color of any pixel or grabs a section of an image. If no parameters are specified, the entire image is returned. Use the <b>x</b>
	 * and <b>y</b> parameters to get the value of one pixel. Get a section of the display window by specifying an additional <b>width</b> and
	 * <b>height</b> parameter. When getting an image, the <b>x</b> and <b>y</b> parameters define the coordinates for the upper-left corner of
	 * the image, regardless of the current <b>imageMode()</b>.<br /> <br /> If the pixel requested is outside of the image window, black is
	 * returned. The numbers returned are scaled according to the current color ranges, but only RGB values are returned by this function. For
	 * example, even though you may have drawn a shape with <b>colorMode(HSB)</b>, the numbers returned will be in RGB format.<br /> <br />
	 * Getting the color of a single pixel with <b>get(x, y)</b> is easy, but not as fast as grabbing the data directly from <b>pixels[]</b>. The
	 * equivalent statement to <b>get(x, y)</b> using <b>pixels[]</b> is <b>pixels[y*width+x]</b>. See the reference for <b>pixels[]</b> for more
	 * information.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Returns an ARGB "color" type (a packed 32 bit int with the color. If the coordinate is outside the image, zero is
	 * returned (black, but completely transparent).
	 * <p>
	 * If the image is in RGB format (i.e. on a PVideo object), the value will get its high bits set, just to avoid cases where they haven't been
	 * set already.
	 * <p>
	 * If the image is in ALPHA format, this returns a white with its alpha value set.
	 * <p>
	 * This function is included primarily for beginners. It is quite slow because it has to check to see if the x, y that was provided is inside
	 * the bounds, and then has to check to see what image type it is. If you want things to be more efficient, access the pixels[] array
	 * directly.
	 *
	 * @param x x-coordinate of the pixel
	 * @param y y-coordinate of the pixel
	 * @webref image:pixels
	 * @brief Reads the color of any pixel or grabs a rectangle of pixels
	 * @usage web_application
	 * @see processing.core.PApplet#set(int, int, int)
	 * @see processing.core.PApplet#pixels
	 * @see processing.core.PApplet#copy(processing.core.PImage, int, int, int, int, int, int, int, int)
	 */
	public int get(int x, int y) {
		return applet.get(x, y);
	}

	/**
	 * @param x
	 * @param y
	 * @param w width of pixel rectangle to get
	 * @param h height of pixel rectangle to get
	 */
	public PImage get(int x, int y, int w, int h) {
		return applet.get(x, y, w, h);
	}

	/**
	 * Returns a copy of this PImage. Equivalent to get(0, 0, width, height).
	 */
	public PImage get() {
		return applet.get();
	}

	/**
	 * ( begin auto-generated from PImage_set.xml )
	 * <p>
	 * Changes the color of any pixel or writes an image directly into the display window.<br /> <br /> The <b>x</b> and <b>y</b> parameters
	 * specify the pixel to change and the <b>color</b> parameter specifies the color value. The color parameter is affected by the current color
	 * mode (the default is RGB values from 0 to 255). When setting an image, the <b>x</b> and <b>y</b> parameters define the coordinates for the
	 * upper-left corner of the image, regardless of the current <b>imageMode()</b>. <br /><br /> Setting the color of a single pixel with
	 * <b>set(x, y)</b> is easy, but not as fast as putting the data directly into <b>pixels[]</b>. The equivalent statement to <b>set(x, y,
	 * #000000)</b> using <b>pixels[]</b> is <b>pixels[y*width+x] = #000000</b>. See the reference for <b>pixels[]</b> for more information.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param x x-coordinate of the pixel
	 * @param y y-coordinate of the pixel
	 * @param c any value of the color datatype
	 * @webref image:pixels
	 * @brief writes a color to any pixel or writes an image into another
	 * @usage web_application
	 * @see processing.core.PImage#get(int, int, int, int)
	 * @see processing.core.PImage#pixels
	 * @see processing.core.PImage#copy(processing.core.PImage, int, int, int, int, int, int, int, int)
	 */
	public void set(int x, int y, int c) {
		applet.set(x, y, c);
	}

	/**
	 * <h3>Advanced</h3> Efficient method of drawing an image's pixels directly to this surface. No variations are employed, meaning that any
	 * scale, tint, or imageMode settings will be ignored.
	 *
	 * @param x
	 * @param y
	 * @param img image to copy into the original image
	 */
	public void set(int x, int y, PImage img) {
		applet.set(x, y, img);
	}

	/**
	 * ( begin auto-generated from PImage_mask.xml )
	 * <p>
	 * Masks part of an image from displaying by loading another image and using it as an alpha channel. This mask image should only contain
	 * grayscale data, but only the blue color channel is used. The mask image needs to be the same size as the image to which it is applied.<br
	 * /> <br /> In addition to using a mask image, an integer array containing the alpha channel data can be specified directly. This method is
	 * useful for creating dynamically generated alpha masks. This array must be of the same length as the target image's pixels array and should
	 * contain only grayscale data of values between 0-255.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3>
	 * <p>
	 * Set alpha channel for an image. Black colors in the source image will make the destination image completely transparent, and white will
	 * make things fully opaque. Gray values will be in-between steps.
	 * <p>
	 * Strictly speaking the "blue" value from the source image is used as the alpha color. For a fully grayscale image, this is correct, but for
	 * a color image it's not 100% accurate. For a more accurate conversion, first use filter(GRAY) which will make the image into a "correct"
	 * grayscale by performing a proper luminance-based conversion.
	 *
	 * @param img
	 * @webref pimage:method
	 * @usage web_application
	 * @brief Masks part of an image with another image as an alpha channel
	 */
	public void mask(PImage img) {
		applet.mask(img);
	}

	public void filter(int kind) {
		applet.filter(kind);
	}

	/**
	 * ( begin auto-generated from PImage_filter.xml )
	 * <p>
	 * Filters an image as defined by one of the following modes:<br /><br />THRESHOLD - converts the image to black and white pixels depending if
	 * they are above or below the threshold defined by the level parameter. The level must be between 0.0 (black) and 1.0(white). If no level is
	 * specified, 0.5 is used.<br /> <br /> GRAY - converts any colors in the image to grayscale equivalents<br /> <br /> INVERT - sets each pixel
	 * to its inverse value<br /> <br /> POSTERIZE - limits each channel of the image to the number of colors specified as the level parameter<br
	 * /> <br /> BLUR - executes a Guassian blur with the level parameter specifying the extent of the blurring. If no level parameter is used,
	 * the blur is equivalent to Guassian blur of radius 1<br /> <br /> OPAQUE - sets the alpha channel to entirely opaque<br /> <br /> ERODE -
	 * reduces the light areas with the amount defined by the level parameter<br /> <br /> DILATE - increases the light areas with the amount
	 * defined by the level parameter
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> Method to apply a variety of basic filters to this image.
	 * <p>
	 * <UL> <LI>filter(BLUR) provides a basic blur. <LI>filter(GRAY) converts the image to grayscale based on luminance. <LI>filter(INVERT) will
	 * invert the color components in the image. <LI>filter(OPAQUE) set all the high bits in the image to opaque <LI>filter(THRESHOLD) converts
	 * the image to black and white. <LI>filter(DILATE) grow white/light areas <LI>filter(ERODE) shrink white/light areas </UL> Luminance
	 * conversion code contributed by <A HREF="http://www.toxi.co.uk">toxi</A>
	 * <p>
	 * Gaussian blur code contributed by <A HREF="http://incubator.quasimondo.com">Mario Klingemann</A>
	 *
	 * @param kind  Either THRESHOLD, GRAY, OPAQUE, INVERT, POSTERIZE, BLUR, ERODE, or DILATE
	 * @param param unique for each, see above
	 * @webref image:pixels
	 * @brief Converts the image to grayscale or black and white
	 * @usage web_application
	 */
	public void filter(int kind, float param) {
		applet.filter(kind, param);
	}

	/**
	 * ( begin auto-generated from PImage_copy.xml )
	 * <p>
	 * Copies a region of pixels from one image into another. If the source and destination regions aren't the same size, it will automatically
	 * resize source pixels to fit the specified target region. No alpha information is used in the process, however if the source image has an
	 * alpha channel set, it will be copied as well. <br /><br /> As of release 0149, this function ignores <b>imageMode()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param sx X coordinate of the source's upper left corner
	 * @param sy Y coordinate of the source's upper left corner
	 * @param sw source image width
	 * @param sh source image height
	 * @param dx X coordinate of the destination's upper left corner
	 * @param dy Y coordinate of the destination's upper left corner
	 * @param dw destination image width
	 * @param dh destination image height
	 * @webref image:pixels
	 * @brief Copies the entire image
	 * @usage web_application
	 * @see processing.core.PGraphics#alpha(int)
	 * @see processing.core.PImage#blend(processing.core.PImage, int, int, int, int, int, int, int, int, int)
	 */
	public void copy(int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
		applet.copy(sx, sy, sw, sh, dx, dy, dw, dh);
	}

	/**
	 * @param src an image variable referring to the source image.
	 * @param sx
	 * @param sy
	 * @param sw
	 * @param sh
	 * @param dx
	 * @param dy
	 * @param dw
	 * @param dh
	 */
	public void copy(PImage src, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
		applet.copy(src, sx, sy, sw, sh, dx, dy, dw, dh);
	}

	public void blend(int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh, int mode) {
		applet.blend(sx, sy, sw, sh, dx, dy, dw, dh, mode);
	}

	/**
	 * ( begin auto-generated from PImage_blend.xml )
	 * <p>
	 * Blends a region of pixels into the image specified by the <b>img</b> parameter. These copies utilize full alpha channel support and a
	 * choice of the following modes to blend the colors of source pixels (A) with the ones of pixels in the destination image (B):<br /> <br />
	 * BLEND - linear interpolation of colours: C = A*factor + B<br /> <br /> ADD - additive blending with white clip: C = min(A*factor + B,
	 * 255)<br /> <br /> SUBTRACT - subtractive blending with black clip: C = max(B - A*factor, 0)<br /> <br /> DARKEST - only the darkest colour
	 * succeeds: C = min(A*factor, B)<br /> <br /> LIGHTEST - only the lightest colour succeeds: C = max(A*factor, B)<br /> <br /> DIFFERENCE -
	 * subtract colors from underlying image.<br /> <br /> EXCLUSION - similar to DIFFERENCE, but less extreme.<br /> <br /> MULTIPLY - Multiply
	 * the colors, result will always be darker.<br /> <br /> SCREEN - Opposite multiply, uses inverse values of the colors.<br /> <br /> OVERLAY
	 * - A mix of MULTIPLY and SCREEN. Multiplies dark values, and screens light values.<br /> <br /> HARD_LIGHT - SCREEN when greater than 50%
	 * gray, MULTIPLY when lower.<br /> <br /> SOFT_LIGHT - Mix of DARKEST and LIGHTEST. Works like OVERLAY, but not as harsh.<br /> <br /> DODGE
	 * - Lightens light tones and increases contrast, ignores darks. Called "Color Dodge" in Illustrator and Photoshop.<br /> <br /> BURN - Darker
	 * areas are applied, increasing contrast, ignores lights. Called "Color Burn" in Illustrator and Photoshop.<br /> <br /> All modes use the
	 * alpha information (highest byte) of source image pixels as the blending factor. If the source and destination regions are different sizes,
	 * the image will be automatically resized to match the destination size. If the <b>srcImg</b> parameter is not used, the display window is
	 * used as the source image.<br /> <br /> As of release 0149, this function ignores <b>imageMode()</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @param src  an image variable referring to the source image
	 * @param sx   X coordinate of the source's upper left corner
	 * @param sy   Y coordinate of the source's upper left corner
	 * @param sw   source image width
	 * @param sh   source image height
	 * @param dx   X coordinate of the destinations's upper left corner
	 * @param dy   Y coordinate of the destinations's upper left corner
	 * @param dw   destination image width
	 * @param dh   destination image height
	 * @param mode Either BLEND, ADD, SUBTRACT, LIGHTEST, DARKEST, DIFFERENCE, EXCLUSION, MULTIPLY, SCREEN, OVERLAY, HARD_LIGHT, SOFT_LIGHT,
	 *             DODGE, BURN
	 * @webref image:pixels
	 * @brief Copies a pixel or rectangle of pixels using different blending modes
	 * @see processing.core.PApplet#alpha(int)
	 * @see processing.core.PImage#copy(processing.core.PImage, int, int, int, int, int, int, int, int)
	 * @see processing.core.PImage#blendColor(int, int, int)
	 */
	public void blend(PImage src, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh, int mode) {
		applet.blend(src, sx, sy, sw, sh, dx, dy, dw, dh, mode);
	}


	/**
	 * ( begin auto-generated from frameRate_var.xml )
	 * <p>
	 * The system variable <b>frameRate</b> contains the approximate frame rate of the software as it executes. The initial value is 10 fps and is
	 * updated with each frame. The value is averaged (integrated) over several frames. As such, this value won't be valid until after 5-10
	 * frames.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref environment
	 * @see PApplet#frameRate(float)
	 */
	public float getFrameRate() {
		return applet.frameRate;
	}

	/**
	 * ( begin auto-generated from frameCount.xml )
	 * <p>
	 * The system variable <b>frameCount</b> contains the number of frames displayed since the program started. Inside <b>setup()</b> the value is
	 * 0 and and after the first iteration of draw it is 1, etc.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref environment
	 * @see PApplet#frameRate(float)
	 */
	public int getFrameCount() {
		return applet.frameCount;
	}

	/**
	 * ( begin auto-generated from pixels.xml )
	 * <p>
	 * Array containing the values for all the pixels in the display window. These values are of the color datatype. This array is the size of the
	 * display window. For example, if the image is 100x100 pixels, there will be 10000 values and if the window is 200x300 pixels, there will be
	 * 60000 values. The <b>index</b> value defines the position of a value within the array. For example, the statement <b>color b =
	 * pixels[230]</b> will set the variable <b>b</b> to be equal to the value at that location in the array.<br /> <br /> Before accessing this
	 * array, the data must loaded with the <b>loadPixels()</b> function. After the array data has been modified, the <b>updatePixels()</b>
	 * function must be run to update the changes. Without <b>loadPixels()</b>, running the code may (or will in future releases) result in a
	 * NullPointerException.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref image:pixels
	 * @see PApplet#loadPixels()
	 * @see PApplet#updatePixels()
	 * @see PApplet#get(int, int, int, int)
	 * @see PApplet#set(int, int, int)
	 * @see PImage
	 */
	public int[] getPixels() {
		return applet.pixels;
	}


	/**
	 * ( begin auto-generated from width.xml )
	 * <p>
	 * System variable which stores the width of the display window. This value is set by the first parameter of the <b>size()</b> function. For
	 * example, the function call <b>size(320, 240)</b> sets the <b>width</b> variable to the value 320. The value of <b>width</b> is zero until
	 * <b>size()</b> is called.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref environment
	 * @see PApplet#height
	 * @see PApplet#size(int, int)
	 */
	public int getWidth() {
		return applet.width;
	}

	/**
	 * ( begin auto-generated from height.xml )
	 * <p>
	 * System variable which stores the height of the display window. This value is set by the second parameter of the <b>size()</b> function. For
	 * example, the function call <b>size(320, 240)</b> sets the <b>height</b> variable to the value 240. The value of <b>height</b> is zero until
	 * <b>size()</b> is called.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref environment
	 * @see PApplet#width
	 * @see PApplet#size(int, int)
	 */
	public int getHeight() {
		return applet.height;
	}

	/**
	 * ( begin auto-generated from mouseX.xml )
	 * <p>
	 * The system variable <b>mouseX</b> always contains the current horizontal coordinate of the mouse.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:mouse
	 * @see PApplet#mouseY
	 * @see PApplet#pmouseX
	 * @see PApplet#pmouseY
	 * @see PApplet#mousePressed
	 * @see PApplet#mousePressed()
	 * @see PApplet#mouseReleased()
	 * @see PApplet#mouseClicked()
	 * @see PApplet#mouseMoved()
	 * @see PApplet#mouseDragged()
	 * @see PApplet#mouseButton
	 * @see PApplet#mouseWheel(processing.event.MouseEvent)
	 */
	public int getMouseX() {
		return applet.mouseX;
	}

	/**
	 * ( begin auto-generated from mouseY.xml )
	 * <p>
	 * The system variable <b>mouseY</b> always contains the current vertical coordinate of the mouse.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:mouse
	 * @see PApplet#mouseX
	 * @see PApplet#pmouseX
	 * @see PApplet#pmouseY
	 * @see PApplet#mousePressed
	 * @see PApplet#mousePressed()
	 * @see PApplet#mouseReleased()
	 * @see PApplet#mouseClicked()
	 * @see PApplet#mouseMoved()
	 * @see PApplet#mouseDragged()
	 * @see PApplet#mouseButton
	 * @see PApplet#mouseWheel(processing.event.MouseEvent)
	 */
	public int getMouseY() {
		return applet.mouseY;
	}

	/**
	 * ( begin auto-generated from pmouseX.xml )
	 * <p>
	 * The system variable <b>pmouseX</b> always contains the horizontal position of the mouse in the frame previous to the current frame.<br />
	 * <br /> You may find that <b>pmouseX</b> and <b>pmouseY</b> have different values inside <b>draw()</b> and inside events like
	 * <b>mousePressed()</b> and <b>mouseMoved()</b>. This is because they're used for different roles, so don't mix them. Inside <b>draw()</b>,
	 * <b>pmouseX</b> and <b>pmouseY</b> update only once per frame (once per trip through your <b>draw()</b>). But, inside mouse events, they
	 * update each time the event is called. If they weren't separated, then the mouse would be read only once per frame, making response choppy.
	 * If the mouse variables were always updated multiple times per frame, using <NOBR><b>line(pmouseX, pmouseY, mouseX, mouseY)</b></NOBR>
	 * inside <b>draw()</b> would have lots of gaps, because <b>pmouseX</b> may have changed several times in between the calls to <b>line()</b>.
	 * Use <b>pmouseX</b> and <b>pmouseY</b> inside <b>draw()</b> if you want values relative to the previous frame. Use <b>pmouseX</b> and
	 * <b>pmouseY</b> inside the mouse functions if you want continuous response.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:mouse
	 * @see PApplet#mouseX
	 * @see PApplet#mouseY
	 * @see PApplet#pmouseY
	 * @see PApplet#mousePressed
	 * @see PApplet#mousePressed()
	 * @see PApplet#mouseReleased()
	 * @see PApplet#mouseClicked()
	 * @see PApplet#mouseMoved()
	 * @see PApplet#mouseDragged()
	 * @see PApplet#mouseButton
	 * @see PApplet#mouseWheel(processing.event.MouseEvent)
	 */
	public int getPmouseX() {
		return applet.pmouseX;
	}

	/**
	 * ( begin auto-generated from pmouseY.xml )
	 * <p>
	 * The system variable <b>pmouseY</b> always contains the vertical position of the mouse in the frame previous to the current frame. More
	 * detailed information about how <b>pmouseY</b> is updated inside of <b>draw()</b> and mouse events is explained in the reference for
	 * <b>pmouseX</b>.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:mouse
	 * @see PApplet#mouseX
	 * @see PApplet#mouseY
	 * @see PApplet#pmouseX
	 * @see PApplet#mousePressed
	 * @see PApplet#mousePressed()
	 * @see PApplet#mouseReleased()
	 * @see PApplet#mouseClicked()
	 * @see PApplet#mouseMoved()
	 * @see PApplet#mouseDragged()
	 * @see PApplet#mouseButton
	 * @see PApplet#mouseWheel(processing.event.MouseEvent)
	 */
	public int getPmouseY() {
		return applet.pmouseY;
	}

	/**
	 * previous mouseX/Y for the draw loop, separated out because this is separate from the pmouseX/Y when inside the mouse event handlers.
	 */
	protected int getDmouseX() {
		return applet.getDmouseX();
	}

	/**
	 * previous mouseX/Y for the draw loop, separated out because this is separate from the pmouseX/Y when inside the mouse event handlers.
	 */
	protected int getDmouseY() {
		return applet.getDmouseY();
	}

	/**
	 * pmouseX/Y for the event handlers (mousePressed(), mouseDragged() etc) these are different because mouse events are queued to the end of
	 * draw, so the previous position has to be updated on each event, as opposed to the pmouseX/Y that's used inside draw, which is expected to
	 * be updated once per trip through draw().
	 */
	public int getEmouseX() {
		return applet.getEmouseX();
	}

	/**
	 * pmouseX/Y for the event handlers (mousePressed(), mouseDragged() etc) these are different because mouse events are queued to the end of
	 * draw, so the previous position has to be updated on each event, as opposed to the pmouseX/Y that's used inside draw, which is expected to
	 * be updated once per trip through draw().
	 */
	public int getEmouseY() {
		return applet.getEmouseY();
	}

	/**
	 * Used to set pmouseX/Y to mouseX/Y the first time mouseX/Y are used, otherwise pmouseX/Y are always zero, causing a nasty jump.
	 * <p>
	 * Just using (frameCount == 0) won't work since mouseXxxxx() may not be called until a couple frames into things.
	 * <p>
	 *
	 * @deprecated Please refrain from using this variable, it will be removed from future releases of Processing because it cannot be used
	 * consistently across platforms and input methods.
	 */
	@Deprecated
	public boolean getFirstMouse() {
		return applet.firstMouse;
	}

	/**
	 * ( begin auto-generated from mouseButton.xml )
	 * <p>
	 * Processing automatically tracks if the mouse button is pressed and which button is pressed. The value of the system variable
	 * <b>mouseButton</b> is either <b>LEFT</b>, <b>RIGHT</b>, or <b>CENTER</b> depending on which button is pressed.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced:</h3>
	 * <p>
	 * If running on Mac OS, a ctrl-click will be interpreted as the right-hand mouse button (unlike Java, which reports it as the left mouse).
	 *
	 * @webref input:mouse
	 * @see PApplet#mouseX
	 * @see PApplet#mouseY
	 * @see PApplet#pmouseX
	 * @see PApplet#pmouseY
	 * @see PApplet#mousePressed
	 * @see PApplet#mousePressed()
	 * @see PApplet#mouseReleased()
	 * @see PApplet#mouseClicked()
	 * @see PApplet#mouseMoved()
	 * @see PApplet#mouseDragged()
	 * @see PApplet#mouseWheel(processing.event.MouseEvent)
	 */
	public int getMouseButton() {
		return applet.mouseButton;
	}

	/**
	 * ( begin auto-generated from mousePressed_var.xml )
	 * <p>
	 * Variable storing if a mouse button is pressed. The value of the system variable <b>mousePressed</b> is true if a mouse button is pressed
	 * and false if a button is not pressed.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:mouse
	 * @see PApplet#mouseX
	 * @see PApplet#mouseY
	 * @see PApplet#pmouseX
	 * @see PApplet#pmouseY
	 * @see PApplet#mousePressed()
	 * @see PApplet#mouseReleased()
	 * @see PApplet#mouseClicked()
	 * @see PApplet#mouseMoved()
	 * @see PApplet#mouseDragged()
	 * @see PApplet#mouseButton
	 * @see PApplet#mouseWheel(processing.event.MouseEvent)
	 */
	public boolean getMousePressed() {
		return applet.mousePressed;
	}


	/**
	 * @deprecated Use a mouse event handler that passes an event instead.
	 */
	@Deprecated
	public MouseEvent getMouseEvent() {
		return applet.mouseEvent;
	}

	/**
	 * ( begin auto-generated from key.xml )
	 * <p>
	 * The system variable <b>key</b> always contains the value of the most recent key on the keyboard that was used (either pressed or released).
	 * <br/> <br/> For non-ASCII keys, use the <b>keyCode</b> variable. The keys included in the ASCII specification (BACKSPACE, TAB, ENTER,
	 * RETURN, ESC, and DELETE) do not require checking to see if they key is coded, and you should simply use the <b>key</b> variable instead of
	 * <b>keyCode</b> If you're making cross-platform projects, note that the ENTER key is commonly used on PCs and Unix and the RETURN key is
	 * used instead on Macintosh. Check for both ENTER and RETURN to make sure your program will work for all platforms.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3>
	 * <p>
	 * Last key pressed.
	 * <p>
	 * If it's a coded key, i.e. UP/DOWN/CTRL/SHIFT/ALT, this will be set to CODED (0xffff or 65535).
	 *
	 * @webref input:keyboard
	 * @see PApplet#keyCode
	 * @see PApplet#keyPressed
	 * @see PApplet#keyPressed()
	 * @see PApplet#keyReleased()
	 */
	public char getKey() {
		return applet.key;
	}

	/**
	 * ( begin auto-generated from keyCode.xml )
	 * <p>
	 * The variable <b>keyCode</b> is used to detect special keys such as the UP, DOWN, LEFT, RIGHT arrow keys and ALT, CONTROL, SHIFT. When
	 * checking for these keys, it's first necessary to check and see if the key is coded. This is done with the conditional "if (key == CODED)"
	 * as shown in the example. <br/> <br/> The keys included in the ASCII specification (BACKSPACE, TAB, ENTER, RETURN, ESC, and DELETE) do not
	 * require checking to see if they key is coded, and you should simply use the <b>key</b> variable instead of <b>keyCode</b> If you're making
	 * cross-platform projects, note that the ENTER key is commonly used on PCs and Unix and the RETURN key is used instead on Macintosh. Check
	 * for both ENTER and RETURN to make sure your program will work for all platforms. <br/> <br/> For users familiar with Java, the values for
	 * UP and DOWN are simply shorter versions of Java's KeyEvent.VK_UP and KeyEvent.VK_DOWN. Other keyCode values can be found in the Java <a
	 * href="http://download.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html">KeyEvent</a> reference.
	 * <p>
	 * ( end auto-generated )
	 * <p>
	 * <h3>Advanced</h3> When "key" is set to CODED, this will contain a Java key code.
	 * <p>
	 * For the arrow keys, keyCode will be one of UP, DOWN, LEFT and RIGHT. Also available are ALT, CONTROL and SHIFT. A full set of constants can
	 * be obtained from java.awt.event.KeyEvent, from the VK_XXXX variables.
	 *
	 * @webref input:keyboard
	 * @see PApplet#key
	 * @see PApplet#keyPressed
	 * @see PApplet#keyPressed()
	 * @see PApplet#keyReleased()
	 */
	public int getKeyCode() {
		return applet.keyCode;
	}

	/**
	 * ( begin auto-generated from keyPressed_var.xml )
	 * <p>
	 * The boolean system variable <b>keyPressed</b> is <b>true</b> if any key is pressed and <b>false</b> if no keys are pressed.
	 * <p>
	 * ( end auto-generated )
	 *
	 * @webref input:keyboard
	 * @see PApplet#key
	 * @see PApplet#keyCode
	 * @see PApplet#keyPressed()
	 * @see PApplet#keyReleased()
	 */
	public boolean getKeyPressed() {
		return applet.keyPressed;
	}
}
