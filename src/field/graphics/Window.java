package field.graphics;

import com.badlogic.jglfw.Glfw;
import com.badlogic.jglfw.GlfwCallback;
import com.badlogic.jglfw.GlfwCallbackAdapter;
import com.badlogic.jglfw.gl.GL;
import field.CannonicalModifierKeys;
import field.app.RunLoop;
import field.graphics.util.KeyEventMapping;
import field.linalg.Vec2;
import field.utility.*;
import fieldbox.ui.FieldBoxWindow;
import fieldlinker.Linker;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static com.badlogic.jglfw.Glfw.*;

/**
 * An Window with an associated OpenGL draw context, and a base Field graphics Scene
 */
public class Window implements ProvidesGraphicsContext {

	static public final Dict.Prop<Boolean> consumed = new Dict.Prop<>("consumed").type()
										     .doc("marks an event as handled elsewhere")
										     .toCannon();
	static Window currentWindow = null;
	private final int retinaScaleFactor;
	/**
	 * the Scene associated with this window.
	 * <p>
	 * This is the principle entry point into this window
	 */

	public final Scene scene = new Scene();
	private final long windowOpenedAt;
	private final CannonicalModifierKeys modifiers;
	private final GLContext glcontext;
	protected GraphicsContext graphicsContext;
	protected long window;

	protected int w,x;
	protected int h,y;

	protected MouseState mouseState = new MouseState();
	protected KeyboardState keyboardState = new KeyboardState();
	int tick = 0;
	Queue<Function<Event<KeyboardState>, Boolean>> keyboardHandlers = new LinkedBlockingQueue<>();
	Queue<Function<Event<MouseState>, Boolean>> mouseHandlers = new LinkedBlockingQueue<>();
	Queue<Function<Event<Drop>, Boolean>> dropHandlers = new LinkedBlockingQueue<>();
	private Rect currentBounds;

	static public boolean doubleBuffered = Options.dict().isTrue(new Dict.Prop("doubleBuffered"), false);

	public Window(int x, int y, int w, int h, String title) {
		this(x,y, w,h,title, true);
	}

	public Window(int x, int y, int w, int h, String title, boolean permitRetina) {
		Windows.windows.init();

		currentBounds = new Rect(x, y, w, h);

		graphicsContext = GraphicsContext.newContext();

		glfwWindowHint(GLFW_DEPTH_BITS, 24);
		glfwWindowHint(GLFW_SAMPLES, 8);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
//		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
		glfwWindowHint(GLFW_DOUBLEBUFFER, doubleBuffered ? 1 : 0);

		glfwWindowHint(GLFW_DECORATED, title == null ? 0 : 1);

		this.w = w;
		this.h = h;
		this.x = x;
		this.y = y;

		window = glfwCreateWindow(w, h, title, 0, 0);
		Windows.windows.register(window, makeCallback());

		glfwSetWindowPos(window, x, y);
		glfwShowWindow(window);

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);

		glfwWindowShouldClose(window);

		glcontext = GLContext.createFromCurrent();

		GL11.glClearColor(0.25f, 0.25f, 0.25f, 1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		glfwSwapBuffers(window);

		RunLoop.main.getLoop()
			    .attach(0, (i) -> loop());


		Glfw.glfwSetInputMode(window, Glfw.GLFW_STICKY_MOUSE_BUTTONS, GL.GL_TRUE);

		retinaScaleFactor = permitRetina ? (int) (Options.dict().getFloat(new Dict.Prop<Number>("retina"), 0f)+1) : 1;

		windowOpenedAt = RunLoop.tick;


		modifiers = new CannonicalModifierKeys(window);

		new Thread()
		{
			long lastAt = System.currentTimeMillis();
			long lastWas = 0;

			public void run() {

				while(true) {
					if (System.currentTimeMillis() - lastAt > 5000) {
						double f = 1000*(frame - lastWas) / (float) (System.currentTimeMillis() - lastAt);
						if (lastWas>0 && (frame!=lastWas)) System.err.println(" frame rate is :" + f + " for " + Window.this);
						lastWas = frame;
						lastAt = System.currentTimeMillis();
					}

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {


					}
				}
			}
		}.start();

	}

	static boolean debugKeyboardTransition(Event<KeyboardState> event) {
		Set<Character> pressed = KeyboardState.charsPressed(event.before, event.after);
		Set<Character> released = KeyboardState.charsReleased(event.before, event.after);
		Set<Integer> kpressed = KeyboardState.keysPressed(event.before, event.after);
		Set<Integer> kreleased = KeyboardState.keysReleased(event.before, event.after);

		if (pressed.size() > 0) System.out.print("down<" + pressed + ">");
		if (kpressed.size() > 0) System.out.print("down<" + kpressed + ">");
		if (released.size() > 0) System.out.print("up<" + released + ">");
		if (kreleased.size() > 0) System.out.print("up<" + kreleased + ">");
		if (event.after.keysDown.size() > 0) System.out.println(" now<" + event.after.keysDown + ">");
		return true;
	}

	static boolean debugMouseTransition(Event<MouseState> event) {
		Set<Integer> pressed = MouseState.buttonsPressed(event.before, event.after);
		Set<Integer> released = MouseState.buttonsReleased(event.before, event.after);
		if (pressed.size() > 0) System.out.print("down<" + pressed + ">");
		if (released.size() > 0) System.out.print("up<" + released + ">");
		return true;
	}

	static public int getCurrentWidth() {
		if (currentWindow == null) throw new IllegalArgumentException(" no window mouseState ");
		return currentWindow.w;
	}

	static public int getCurrentHeight() {


		if (currentWindow == null) throw new IllegalArgumentException(" no window mouseState ");
		return currentWindow.h;
	}

	public void loop() {
		if (frame==10)
			glfwSetWindowPos(window, x, y);

		if (!needsRepainting()) {
			glfwPollEvents();
			return;
		}

		needsRepainting = false;

		currentWindow = this;
		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);

		// makes linux all go to hell
//		glcontext.makeCurrent(0);

		GraphicsContext.checkError(() -> "initially");

		int w = glfwGetWindowWidth(window);
		int h = glfwGetWindowHeight(window);


		GraphicsContext.stateTracker.viewport.set(new int[]{0, 0, w * getRetinaScaleFactor(), h * getRetinaScaleFactor()});
		GraphicsContext.stateTracker.scissor.set(new int[]{0, 0, w * getRetinaScaleFactor(), h * getRetinaScaleFactor()});
		GraphicsContext.stateTracker.fbo.set(0);
		GraphicsContext.stateTracker.shader.set(0);
		GraphicsContext.stateTracker.blendState.set(new int[]{GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA});


		//System.out.println("!! width :"+w+" "+h+" "+getRetinaScaleFactor());

		if (w != this.w || h != this.h) {
			GraphicsContext.isResizing = true;
			this.w = w;
			this.h = h;
		} else {
			GraphicsContext.isResizing = false;
		}


		updateScene();

		frame++;
		if (!dontSwap)
		glfwSwapBuffers(window);

		glfwPollEvents();
		currentWindow = null;

	}

	long frame;
	public boolean dontSwap = false;

	protected boolean disabled = false;
	protected boolean lazyRepainting = false;
	protected boolean needsRepainting = false;

	/**
	 * stops this canvas from repainting, under any conditions
	 */
	public void disable()
	{
		disabled = true;
	}

	/**
	 * allows this canvas to repaint
	 */
	public void enable()
	{
		disabled = false;
	}

	/**
	 * sets whether this canvas updates and repaints constantly (every animation cycle) or only once when repaint has been called
	 */
	public void setLazyRepainting(boolean b)
	{
		lazyRepainting = b;
	}

	/**
	 * requests that this canvas repaints itself this animation cycle. Note, that you should almost certainly setLazyRepainting(true) before you call this, otherwise the canvas will update every cycle whether you call this or not.
	 */
	public void requestRepaint()
	{
		needsRepainting = true;
	}

	/**
	 * By default, we are animated, and we draw every frame. Some subclasses (notably FieldBoxWindow) will finesse this down a bit, but a standard graphics window has a traditional
	 * "draw-every-frame" draw loop
	 */
	protected boolean needsRepainting() {
		return !disabled && (!lazyRepainting || needsRepainting);
	}

	public void setTitle(String title) {
		glfwSetWindowTitle(window, title);
	}

	public void setBounds(int x, int y, int w, int h) {
		glfwSetWindowSize(window, w, h);
		glfwSetWindowPos(window, x, y);
		glfwSetWindowSize(window, w, h);
		glfwSetWindowPos(window, x, y);
		currentBounds = new Rect(x, y, w, h);
	}


	public Rect getBounds() {

		currentBounds.x = glfwGetWindowX(window);
		currentBounds.y = glfwGetWindowY(window);
		return currentBounds;
	}

	public Rect getFramebufferBounds() {
		return new Rect(currentBounds.x, currentBounds.y, getFrameBufferWidth(), getFrameBufferHeight());
	}

	protected void updateScene() {
		GraphicsContext.enterContext(graphicsContext);
		try {
			Log.log("graphics.trace", () -> "scene is ...\n" + scene.debugPrintScene());

			scene.updateAll();
		} finally {
			GraphicsContext.exitContext(graphicsContext);
		}
	}

	public GraphicsContext getGraphicsContext() {
		return graphicsContext;
	}

	/**
	 * gets the current clipboard (as a string);
	 */
	public String getCurrentClipboard() {
		return glfwGetClipboardString(window);
	}

	/**
	 * sets the current clipboard (as a string);
	 */
	public void setCurrentClipboard(String s) {
		glfwSetClipboardString(window, s);
	}

	/**
	 * returns the internal glfw window handle for this window. You'll only need this if you are going to do GLFW stuff to this window)
	 */
	public long getGLFWWindowReference() {
		return window;
	}

	/**
	 * returns the last seen mouse state
	 */
	public MouseState getCurrentMouseState() {
		return mouseState;
	}

	/**
	 * A keyboard handler is a Function<Event<KeyboardState>, Boolean>>, that is a function that takes a transition between two KeyboardStates and returns a boolean, whether or not it ever wants
	 * to be called again
	 */
	public Window addKeyboardHandler(Function<Event<KeyboardState>, Boolean> h) {
		keyboardHandlers.add(h);
		return this;
	}

	/**
	 * A keyboard handler is a Function<Event<MouseState>, Boolean>>, that is a function that takes a transition between two MouseStates and returns a boolean, whether or not it ever wants to be
	 * called again
	 */
	public Window addMouseHandler(Function<Event<MouseState>, Boolean> h) {
		mouseHandlers.add(h);
		return this;
	}

	/**
	 * A keyboard handler is a Function<Event<Drop>, Boolean>>, that is a function that takes a transition between null and a Drop and returns a boolean, whether or not it ever wants to be called
	 * again
	 * <p>
	 * (we expect that as GLFW's notion of drop handling gets richer we'll have a use for Event.before)
	 */
	public Window addDropHandler(Function<Event<Drop>, Boolean> h) {
		dropHandlers.add(h);
		return this;
	}

	private void fireMouseTransition(MouseState before, MouseState after) {
		after.keyboardState = keyboardState;

		if ((after.mods & Glfw.GLFW_MOD_SHIFT) != 0) after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_SHIFT, true);
		else after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_SHIFT, false)
							      .withKey(Glfw.GLFW_KEY_RIGHT_SHIFT, false);
		if ((after.mods & Glfw.GLFW_MOD_CONTROL) != 0) after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_CONTROL, true);
		else after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_CONTROL, false)
							      .withKey(Glfw.GLFW_KEY_RIGHT_CONTROL, false);
		if ((after.mods & Glfw.GLFW_MOD_ALT) != 0) after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_ALT, true);
		else after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_ALT, false)
							      .withKey(Glfw.GLFW_KEY_RIGHT_ALT, false);
		if ((after.mods & Glfw.GLFW_MOD_SUPER) != 0) after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_SUPER, true);
		else after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_SUPER, false)
							      .withKey(Glfw.GLFW_KEY_RIGHT_SUPER, false);

		Iterator<Function<Event<MouseState>, Boolean>> i = mouseHandlers.iterator();
		Event<MouseState> event = new Event<>(before, after);
		while (i.hasNext()) if (!i.next()
					  .apply(event)) i.remove();
	}

	private void fireMouseTransitionNoMods(MouseState before, MouseState after) {
		after.keyboardState = keyboardState;

		Iterator<Function<Event<MouseState>, Boolean>> i = mouseHandlers.iterator();
		Event<MouseState> event = new Event<>(before, after);
		while (i.hasNext()) if (!i.next()
					  .apply(event)) i.remove();
	}

	private void fireKeyboardTransition(KeyboardState before, KeyboardState after) {
		after.mouseState = mouseState;
		Iterator<Function<Event<KeyboardState>, Boolean>> i = keyboardHandlers.iterator();
		Event<KeyboardState> event = new Event<>(before, after);
		while (i.hasNext()) if (!i.next()
					  .apply(event)) i.remove();
	}

	private void fireDrop(Drop drop) {
		Iterator<Function<Event<Drop>, Boolean>> i = dropHandlers.iterator();
		Event<Drop> event = new Event<>(null, drop);
		while (i.hasNext()) if (!i.next()
					  .apply(event)) i.remove();
	}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	public int getFrameBufferWidth() {
		return glfwGetFramebufferWidth(window);
		//return w * retinaScaleFactor;
	}

	public int getFrameBufferHeight() {
		return glfwGetFramebufferHeight(window);
		//return h * retinaScaleFactor;
	}

	protected GlfwCallback makeCallback() {
		return new GlfwCallbackAdapter() {

			int event = 0;

			@Override
			public void error(int error, String description) {
				System.err.println(" ERROR in GLFW windowing system :" + error + " / " + description);
			}

			@Override
			public void windowRefresh(long window) {
			}

			@Override
			public void mouseButton(long window, int button, boolean pressed, int mods) {
				if (window == Window.this.window) {
					MouseState next = mouseState.withButton(button, pressed, mods);
					fireMouseTransition(mouseState, next);
					mouseState = next;
				}
			}

			@Override
			public void windowFocus(long window, boolean focused) {
				keyboardState.keysDown.clear();
			}

			@Override
			public void scroll(long window, double scrollX, double scrollY) {
				if (window == Window.this.window) {
					MouseState next = mouseState.withScroll(scrollX, scrollY);
					next.keyboardState = keyboardState;
					fireMouseTransitionNoMods(mouseState, next);
					next = mouseState.withScroll(0, 0);
					mouseState = next;
					next.keyboardState = keyboardState;
				}
			}

			@Override
			public void cursorPos(long window, double x, double y) {
				if (window == Window.this.window) {
					MouseState next = mouseState.withPosition(x, y);
					fireMouseTransition(mouseState, next);
					mouseState = next;
				}
			}

			@Override
			public void key(long window, int key, int scancode, int action, int mods) {
				if (window == Window.this.window && RunLoop.tick > windowOpenedAt + 10) { // we ignore keyboard events from the first couple of updates; they can refer to key downs that we'll never recieve up fors


					KeyboardState next = keyboardState.withKey(key, action != GLFW_RELEASE);

					modifiers.event(key, scancode, action, mods, next.keysDown);


					next = modifiers.cleanModifiers(next);
					next = next.clean(window);

					fireKeyboardTransition(keyboardState, next);
					keyboardState = next;
				}
			}

			@Override
			public void character(long window, char character) {
				if (window == Window.this.window) {
					KeyboardState next = keyboardState.withChar(character, true);

					boolean shift = (Glfw.glfwGetKey(window, Glfw.GLFW_KEY_LEFT_SHIFT)) || (Glfw.glfwGetKey(window, Glfw.GLFW_KEY_RIGHT_SHIFT));
					boolean alt = (Glfw.glfwGetKey(window, Glfw.GLFW_KEY_LEFT_ALT)) || (Glfw.glfwGetKey(window, Glfw.GLFW_KEY_RIGHT_ALT));
					boolean meta = (Glfw.glfwGetKey(window, Glfw.GLFW_KEY_LEFT_SUPER)) || (Glfw.glfwGetKey(window, Glfw.GLFW_KEY_RIGHT_SUPER));
					boolean ctrl = (Glfw.glfwGetKey(window, Glfw.GLFW_KEY_LEFT_CONTROL)) || (Glfw.glfwGetKey(window, Glfw.GLFW_KEY_RIGHT_CONTROL));

					next = modifiers.cleanModifiers(next);
					next = next.clean(window);

					fireKeyboardTransition(keyboardState, next);
					keyboardState = next;
					next = keyboardState.withChar(character, false);

					next = modifiers.cleanModifiers(next);
					next = next.clean(window);
					fireKeyboardTransition(keyboardState, next);
					keyboardState = next;
				}
			}

			@Override
			public void drop(long window, String[] files) {
				if (window == Window.this.window) {
					fireDrop(new Drop(files, mouseState, keyboardState));
				}
			}

			@Override
			public void windowPos(long window, int x, int y) {
				if (window == Window.this.window) {
					currentBounds.x = x;
					currentBounds.y = y;
				}
			}

			@Override
			public void windowSize(long window, int width, int height) {
				if (window == Window.this.window) {
					currentBounds.w = width;
					currentBounds.h = height;
				}
			}

			@Override
			public void framebufferSize(long window, int width, int height) {

			}
		};
	}

	public int getRetinaScaleFactor() {

//		return glfwGetFramebufferWidth(window)/glfwGetWindowWidth(window);
		return 1;
//		return retinaScaleFactor;
	}

	static public interface HasPosition {
		public Optional<Vec2> position();
	}

	static public class MouseState implements HasPosition {
		public final Set<Integer> buttonsDown = new LinkedHashSet<Integer>();
		public final long time;

		public final double dx;
		public final double dy;
		public final double x;
		public final double y;


		public double mx; // in drawing space
		public double my;
		public double mdx;
		public double mdy;


		public final float dwheel;
		public final float dwheely;
		public final int mods;

		// not final (but still immutable), not part of the transition framework, just along to reduce static access to Window
		public KeyboardState keyboardState;

		public MouseState() {
			time = 0;
			dx = 0;
			dy = 0;
			x = 0;
			y = 0;
			dwheel = 0;
			dwheely = 0;
			mods = 0;
		}

		public MouseState(Set<Integer> buttonsDown, double x, double y, float dwheel, float dwheely, double dx, double dy, long time, int mods) {
			this.x = x;
			this.y = y;
			this.dwheel = dwheel;
			this.dwheely = dwheely;
			this.dx = dx;
			this.dy = dy;
			this.time = time;
			this.buttonsDown.addAll(buttonsDown);
			this.mods = mods;
		}

		static public Set<Integer> buttonsPressed(MouseState before, MouseState after) {
			Set<Integer> b = new LinkedHashSet<Integer>(after.buttonsDown);
			b.removeAll(before.buttonsDown);
			return b;
		}

		static public Set<Integer> buttonsReleased(MouseState before, MouseState after) {
			Set<Integer> b = new LinkedHashSet<Integer>(before.buttonsDown);
			b.removeAll(after.buttonsDown);
			return b;
		}

		public MouseState next(int button, float dwheel, int dx, int dy, double x, double y, boolean bs, long time, int mods) {
			Set<Integer> buttonsDown = new LinkedHashSet<Integer>(this.buttonsDown);
			if (bs && button != -1) buttonsDown.add(button);
			else if (!bs && button != -1) buttonsDown.remove(button);

			return new MouseState(buttonsDown, x, y, dwheel, dwheely, dx, dy, time, mods);
		}

		public MouseState withButton(int button, boolean bs, int mods) {
			Set<Integer> buttonsDown = new LinkedHashSet<Integer>(this.buttonsDown);
			if (bs && button != -1) buttonsDown.add(button);
			else if (!bs && button != -1) buttonsDown.remove(button);
			MouseState m = new MouseState(buttonsDown, x, y, dwheel, dwheely, 0, 0, time, mods);
			return m;
		}

		public MouseState withMods(int mods)
		{
			return new MouseState(buttonsDown, x, y, dwheel, dwheely, x-this.x, y-this.y, time, mods);
		}

		// currently we ignore sy
		public MouseState withScroll(double sx, double sy) {
			return new MouseState(buttonsDown, x, y, (float) sx, (float) sy, dx, dy, time, mods);
		}

		public MouseState withPosition(double x, double y) {
			return new MouseState(buttonsDown, x, y, dwheel, dwheely, x - this.x, y - this.y, time, mods);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof MouseState)) return false;

			MouseState that = (MouseState) o;

			if (dwheel != that.dwheel) return false;
			if (x != that.x) return false;
			if (y != that.y) return false;
			if (!buttonsDown.equals(that.buttonsDown)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			temp = Double.doubleToLongBits(x);
			result = (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(y);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			result = 31 * result + (dwheel != +0.0f ? Float.floatToIntBits(dwheel) : 0);
			return result;
		}

		@Override
		public String toString() {
			return "MouseState{" +
				    "buttonsDown=" + buttonsDown +
				    ", x=" + x +
				    ", y=" + y +
				    ", dwheel=" + dwheel +
				    ", time=" + time +
				    '}';
		}

		@Override
		public Optional<Vec2> position() {
			return Optional.of(new Vec2(x, y));
		}
	}

	static public class KeyboardState implements HasPosition {
		public final Set<Integer> keysDown = new LinkedHashSet<>();
		public final Map<Integer, Character> charsDown = new LinkedHashMap<>();
		public final long time;

		// not final (but still immutable), not part of the transition framework, just along to reduce static access to Window
		public MouseState mouseState;

		public KeyboardState() {
			time = 0;
		}

		public KeyboardState(Set<Integer> keysDown, Map<Integer, Character> charsDown, long time) {
			this.keysDown.addAll(keysDown);
			this.charsDown.putAll(charsDown);
			this.time = time;
		}

		static public Set<Integer> keysPressed(KeyboardState before, KeyboardState after) {
			Set<Integer> b = new LinkedHashSet<>(after.keysDown);
			b.removeAll(before.keysDown);
			return b;
		}

		static public Set<Integer> keysReleased(KeyboardState before, KeyboardState after) {
			Set<Integer> b = new LinkedHashSet<>(before.keysDown);
			b.removeAll(after.keysDown);
			return b;
		}

		static public Set<Character> charsPressed(KeyboardState before, KeyboardState after) {
			Set<Character> b = new LinkedHashSet<>(after.charsDown.values());
			b.removeAll(before.charsDown.values());
			return b;
		}

		static public Set<Character> charsReleased(KeyboardState before, KeyboardState after) {
			Set<Character> b = new LinkedHashSet<>(before.charsDown.values());
			b.removeAll(after.charsDown.values());
			return b;
		}

		public KeyboardState next(int key, char character, boolean state, long time) {
			Set<Integer> keysDown = new LinkedHashSet<>(this.keysDown);
			Map<Integer, Character> charsDown = new LinkedHashMap<>(this.charsDown);
			if (state) {
				keysDown.add(key);
				charsDown.put(key, character);
			} else {
				keysDown.remove(key);
				charsDown.remove(key);
			}

			return new KeyboardState(keysDown, charsDown, time);
		}

		public KeyboardState withKey(int key, boolean down) {
			Set<Integer> keysDown = new LinkedHashSet<>(this.keysDown);
			Map<Integer, Character> charsDown = new LinkedHashMap<>(this.charsDown);
			if (down) {
				keysDown.add(key);
			} else {
				keysDown.remove(key);
			}

			return new KeyboardState(keysDown, charsDown, time);
		}

		public KeyboardState withChar(char c, boolean down) {
			Set<Integer> keysDown = new LinkedHashSet<>(this.keysDown);
			Map<Integer, Character> charsDown = new LinkedHashMap<>(this.charsDown);
			if (down) {
				charsDown.put((int) c, c);
			} else {
				charsDown.remove((int) c);
			}

			return new KeyboardState(keysDown, charsDown, time);
		}

		public boolean isShiftDown() {
			return keysDown.contains(Glfw.GLFW_KEY_LEFT_SHIFT) || keysDown.contains(Glfw.GLFW_KEY_RIGHT_SHIFT);
		}

		public boolean isAltDown() {
			return keysDown.contains(Glfw.GLFW_KEY_LEFT_ALT) || keysDown.contains(Glfw.GLFW_KEY_RIGHT_ALT);
		}

		public boolean isSuperDown() {
			return keysDown.contains(Glfw.GLFW_KEY_LEFT_SUPER) || keysDown.contains(Glfw.GLFW_KEY_RIGHT_SUPER);
		}

		public boolean isControlDown() {
			return keysDown.contains(Glfw.GLFW_KEY_LEFT_CONTROL) || keysDown.contains(Glfw.GLFW_KEY_RIGHT_CONTROL);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof KeyboardState)) return false;

			KeyboardState that = (KeyboardState) o;

			if (!charsDown.equals(that.charsDown)) return false;
			if (!keysDown.equals(that.keysDown)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = keysDown.hashCode();
			result = 31 * result + charsDown.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return "KeyboardState{" +
				    "keysDown=" + keysDown +
				    ", charsDown=" + charsDown +
				    ", time=" + time +
				    '}';
		}

		public Optional<Vec2> position() {
			if (mouseState == null) return Optional.empty();
			return mouseState.position();
		}


		public KeyboardState clean(long window) {
			Set<Integer> k = new LinkedHashSet<>(keysDown);
			Iterator<Integer> ii = k.iterator();
			while (ii.hasNext()) {
				Integer m = ii.next();

				if (m == Glfw.GLFW_KEY_LEFT_SHIFT || m == Glfw.GLFW_KEY_RIGHT_SHIFT || m == Glfw.GLFW_KEY_LEFT_ALT || m == Glfw.GLFW_KEY_RIGHT_ALT || m == Glfw.GLFW_KEY_LEFT_SUPER || m == Glfw.GLFW_KEY_RIGHT_SUPER || m == Glfw.GLFW_KEY_LEFT_CONTROL || m == Glfw.GLFW_KEY_RIGHT_CONTROL)
					continue;

				boolean notReally = glfwGetKey(window, m);
				if (!notReally) {
					Log.log("keyboard.debug", ()->"Got an imposter :" + m + " " + KeyEventMapping.lookup(m));
					ii.remove();
					charsDown.remove(m);
				} else {
				}
			}

			return new KeyboardState(k, charsDown, time);
		}
	}

	static public class Drop implements HasPosition {
		public final String[] files;
		public final MouseState mouseState;
		public final KeyboardState keyboardState;

		public Drop(String[] files, MouseState mouseState, KeyboardState keyboardState) {
			this.files = files;
			this.mouseState = mouseState;
			this.keyboardState = keyboardState;
		}

		@Override
		public Optional<Vec2> position() {
			if (mouseState == null) return Optional.empty();
			return mouseState.position();
		}
	}

	static public class Event<T> extends AsMapDelegator {
		public final T before;
		public final T after;

		public final Dict properties = new Dict();

		public Event(T before, T after) {
			this.before = before;
			this.after = after;
		}

		public String toString() {
			return "ev<" + before + "->" + after + ">";
		}

		public Event<T> copy() {
			Event<T> e = new Event(before, after);
			e.properties.putAll(properties);
			return e;
		}

		@Override
		protected Linker.AsMap delegateTo() {
			return properties;
		}
	}

}
