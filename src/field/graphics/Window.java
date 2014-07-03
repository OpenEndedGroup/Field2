package field.graphics;

import com.badlogic.jglfw.Glfw;
import com.badlogic.jglfw.GlfwCallback;
import com.badlogic.jglfw.GlfwCallbackAdapter;
import field.linalg.Vec2;
import field.utility.Dict;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static com.badlogic.jglfw.Glfw.*;
import static com.badlogic.jglfw.gl.GL.glViewport;

/**
 * An Window with an associated OpenGL draw context, and a base Field graphics Scene
 */
public class Window {

	static public final Dict.Prop<Boolean> consumed = new Dict.Prop<>("consumed").type().doc("marks an event as handled elsewhere").toCannon();

	protected GraphicsContext graphicsContext;
	protected long window;
	protected int w;
	protected int h;

	public Window(int x, int y, int w, int h, String title) {
		Windows.windows.init();

		graphicsContext = GraphicsContext.newContext();

		glfwWindowHint(GLFW_DEPTH_BITS, 24);
		glfwWindowHint(GLFW_SAMPLES, 8);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
		glfwWindowHint(GLFW_SAMPLES, 8);

		glfwWindowHint(GLFW_DECORATED, title == null ? 0 : 1);

		this.w = w;
		this.h = h;

		window = glfwCreateWindow(w, h, title, 0, 0);

		Windows.windows.register(window, makeCallback());
		glfwShowWindow(window);

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);

		glfwWindowShouldClose(window);

		try {
			GLContext.useContext(this);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		GL11.glClearColor(0.25f, 0.25f, 0.25f, 1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		glfwSwapBuffers(window);


//		addMouseHandler(Window::debugMouseTransition);
//		addKeyboardHandler(Window::debugKeyboardTransition);

		RunLoop.main.getLoop().connect(0, (i) -> loop());

		ByteBuffer dest = ByteBuffer.allocateDirect(32*32*4);
		for(int i=0;i<32*32*4;i++)
			dest.put((byte)(Math.random()*255));

		dest.rewind();
//		Glfw.glfwSetCursor(window, Glfw.glfwCreateCursor(dest, 32,32,32,32));

		Glfw.glfwSetInputMode(window, Glfw.GLFW_CURSOR, Glfw.GLFW_CURSOR_NORMAL);

	}

	static Window currentWindow = null;

	public void loop() {

		if (!needsRepainting()) {
			glfwPollEvents();
			return;
		}

		currentWindow = this;
		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);

		try {
			GLContext.useContext(this);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		int w = glfwGetWindowWidth(window);
		int h = glfwGetWindowHeight(window);

		if (w != this.w || h != this.h) {
			glViewport(0, 0, w, h);
			GraphicsContext.isResizing = true;
			this.w = w;
			this.h = h;
		} else {
			GraphicsContext.isResizing = false;
		}

		GL11.glClearColor(1, 0.25f, 0, 1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		updateScene();

		glfwSwapBuffers(window);
		glfwPollEvents();
		currentWindow = null;

		ByteBuffer dest = ByteBuffer.allocateDirect(32*32*4);
		for(int i=0;i<32*32*4;i++)
			dest.put((byte)(Math.random()*255));

		dest.rewind();
//		Glfw.glfwSetCursor(window, Glfw.glfwCreateCursor(dest, 32,32,32,32));

	}

	/**
	 * By default, we are animated, and we draw every frame. Some subclasses (notibly FieldBoxWindow) will finesse this down a bit, but a standard
	 * graphics window has a traditional "draw-every-frame" draw loop
	 */
	protected boolean needsRepainting() {
		return true;
	}

	public void setTitle(String title) {
		glfwSetWindowTitle(window, title);
	}

	public void setBounds(int x, int y, int w, int h) {
		glfwSetWindowSize(window, w, h);
		glfwSetWindowPos(window, x, y);
		glfwSetWindowSize(window, w, h);
		glfwSetWindowPos(window, x, y);
	}


	protected Scene mainScene = new Scene();

	protected void updateScene() {
		GraphicsContext.enterContext(graphicsContext);
		try {
			if (GraphicsContext.trace) System.out.println("scene is ...\n" + mainScene.debugPrintScene());

			mainScene.updateAll();
		} finally {
			GraphicsContext.exitContext(graphicsContext);
		}
	}

	/**
	 * returns the Scene associated with this window.
	 *
	 * This is the principle entry point into this window
	 */
	public Scene scene() {
		return mainScene;
	}


	public GraphicsContext getGraphicsContext() {
		return graphicsContext;
	}

	/**
	 * returns the internal glfw window handle for this window. You'll only need this if you are going to do GLFW stuff to this window)
	 */
	public long getGLFWWindowReference() {
		return window;
	}

	static public interface HasPosition
	{
		public Optional<Vec2> position();
	}


	static public class MouseState implements HasPosition {
		public final Set<Integer> buttonsDown = new LinkedHashSet<Integer>();
		public final long time;
		public final double dx;
		public final double dy;
		public final double x;
		public final double y;
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

		// currently we ignore sy
		public MouseState withScroll(double sx, double sy) {
			return new MouseState(buttonsDown, x, y, (float) sx, (float)sy, dx, dy, time, mods);
		}

		public MouseState withPosition(double x, double y) {
			return new MouseState(buttonsDown, x, y, dwheel, dwheely, x - this.x, y - this.y, time, mods);
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
			return Optional.of(new Vec2(x,y));
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

		public Optional<Vec2> position()
		{
			if (mouseState==null) return Optional.empty();
			return mouseState.position();
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
			if (mouseState==null) return Optional.empty();
			return mouseState.position();
		}
	}

	static public class Event<T> {
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
	}

	protected MouseState mouseState = new MouseState();
	protected KeyboardState keyboardState = new KeyboardState();


	Queue<Function<Event<KeyboardState>, Boolean>> keyboardHandlers = new LinkedBlockingQueue<>();
	Queue<Function<Event<MouseState>, Boolean>> mouseHandlers = new LinkedBlockingQueue<>();
	Queue<Function<Event<Drop>, Boolean>> dropHandlers = new LinkedBlockingQueue<>();


	/**
	 * A keyboard handler is a Function<Event<KeyboardState>, Boolean>>, that is a function that takes a transition between two KeyboardStates and
	 * returns a boolean, whether or not it ever wants to be called again
	 */
	public Window addKeyboardHandler(Function<Event<KeyboardState>, Boolean> h) {
		keyboardHandlers.add(h);
		return this;
	}

	/**
	 * A keyboard handler is a Function<Event<MouseState>, Boolean>>, that is a function that takes a transition between two MouseStates and
	 * returns a boolean, whether or not it ever wants to be called again
	 */
	public Window addMouseHandler(Function<Event<MouseState>, Boolean> h) {
		mouseHandlers.add(h);
		return this;
	}

	/**
	 * A keyboard handler is a Function<Event<Drop>, Boolean>>, that is a function that takes a transition between null and a Drop and returns a
	 * boolean, whether or not it ever wants to be called again
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
		else after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_SHIFT, false).withKey(Glfw.GLFW_KEY_RIGHT_SHIFT, false);
		if ((after.mods & Glfw.GLFW_MOD_CONTROL) != 0) after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_CONTROL, true);
		else after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_CONTROL, false).withKey(Glfw.GLFW_KEY_RIGHT_CONTROL, false);
		if ((after.mods & Glfw.GLFW_MOD_ALT) != 0) after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_ALT, true);
		else after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_ALT, false).withKey(Glfw.GLFW_KEY_RIGHT_ALT, false);
		if ((after.mods & Glfw.GLFW_MOD_SUPER) != 0) after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_SUPER, true);
		else after.keyboardState = after.keyboardState.withKey(Glfw.GLFW_KEY_LEFT_SUPER, false).withKey(Glfw.GLFW_KEY_RIGHT_SUPER, false);

		Iterator<Function<Event<MouseState>, Boolean>> i = mouseHandlers.iterator();
		Event<MouseState> event = new Event<>(before, after);
		while (i.hasNext()) if (!i.next().apply(event)) i.remove();
	}

	private void fireKeyboardTransition(KeyboardState before, KeyboardState after) {
		after.mouseState = mouseState;
		Iterator<Function<Event<KeyboardState>, Boolean>> i = keyboardHandlers.iterator();
		Event<KeyboardState> event = new Event<>(before, after);
		while (i.hasNext()) if (!i.next().apply(event)) i.remove();
	}

	private void fireDrop(Drop drop) {
		Iterator<Function<Event<Drop>, Boolean>> i = dropHandlers.iterator();
		Event<Drop> event = new Event<>(null, drop);
		while (i.hasNext()) if (!i.next().apply(event)) i.remove();
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

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	static public int getCurrentWidth() {
		if (currentWindow == null) throw new IllegalArgumentException(" no window mouseState ");
		return currentWindow.w;
	}

	static public int getCurrentHeight() {
		if (currentWindow == null) throw new IllegalArgumentException(" no window mouseState ");
		return currentWindow.h;
	}


	protected GlfwCallback makeCallback() {
		return new GlfwCallbackAdapter() {

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
					fireMouseTransition(mouseState, next);
					next = mouseState.withScroll(0,0);
					mouseState = next;
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
				if (window == Window.this.window) {
					KeyboardState next = keyboardState.withKey(key, action != GLFW_RELEASE);
					fireKeyboardTransition(keyboardState, next);
					keyboardState = next;
				}
			}

			@Override
			public void character(long window, char character) {
				if (window == Window.this.window) {
					System.out.println(" char -- " + character);
				}
			}

			@Override
			public void drop(long window, String[] files) {
				if (window == Window.this.window) {
					fireDrop(new Drop(files, mouseState, keyboardState));
				}
			}

		};
	}


}
