package fieldbox.ui;

import field.graphics.GraphicsContext;
import field.graphics.Scene;
import field.utility.Dict;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.glViewport;

/**
 *
 * lwjgl for now, but how to do multiple Window_lwjgl2s!?
 * Created by marc on 3/18/14.
 */
public class Window_lwjgl2 {

	private final GraphicsContext graphicsContext;
	private Compositor compositor;


	public Window_lwjgl2(int x, int y, int w, int h)
	{
		graphicsContext = GraphicsContext.newContext();
		try{
			DisplayMode mode = new DisplayMode(w, h);
			Display.setDisplayMode(mode);
			Display.setFullscreen(false);
			Display.setTitle("Field");
			Display.setResizable(true);

			PixelFormat pf = new PixelFormat().withSamples(8).withCoverageSamples(8);
			ContextAttribs ca = new ContextAttribs(3,2).withForwardCompatible(true).withProfileCore(true);

			Display.create(pf, ca);

			Mouse.create();
			Keyboard.create();

			addMouseHandler((eve) -> {System.out.println(eve); return true; });
			addKeyboardHandler((eve) -> {System.out.println(eve); return true; });

//			compositor = new Compositor(this);

		}
		catch(Throwable t)
		{
			System.err.println(" -- trouble opening Window_lwjgl2 --");
			t.printStackTrace();
		}
	}


	// todo: optimize out this loop to wait for dirty (from repaint, or from fieldbox)
	// no need to run a game loop here
	public void loop()
	{
		while (!Display.isCloseRequested()) {
			loopOnce();
		}
	}
	public void loopOnce()
	{
		Window_lwjgl2 = this;

		if (Display.wasResized())
		{
			glViewport(0,0,Display.getWidth(), Display.getHeight());
			GraphicsContext.isResizing = true;
			System.out.println(" width is :"+Display.getWidth());
		}
		else
		{
			GraphicsContext.isResizing = false;
		}

//			System.out.println(" -- loop -- ");
		GL11.glClearColor(1, 0.25f, 0, 1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		mouse();
		keyboard();
		updateScene();

		Display.update();
		Display.sync(60);
		Window_lwjgl2 = null;
	}


	Scene mainScene = new Scene();

	static Window_lwjgl2 Window_lwjgl2 = null;
	protected void updateScene() {
		GraphicsContext.enterContext(graphicsContext);
		try{
			if (GraphicsContext.trace)
				System.out.println("scene is ...\n"+mainScene.debugPrintScene());

//			compositor.updateScene();

			mainScene.updateAll();
		}
		finally
		{
			GraphicsContext.exitContext(graphicsContext);
		}
	}

	static public int getCurrentWidth()
	{
		if (Window_lwjgl2==null) throw new IllegalArgumentException("no Window_lwjgl2 mouseState");
		return Display.getWidth();
	}

	static public int getCurrentHeight()
	{
		if (Window_lwjgl2==null) throw new IllegalArgumentException("no Window_lwjgl2 mouseState");
		return Display.getHeight();
	}

	public int getWidth()
	{
		return Display.getWidth();
	}

	public int getHeight()
	{
		return Display.getHeight();
	}

	public Scene scene()
	{
		return mainScene;
	}

	public Scene mainLayer()
	{
		return compositor.getMainLayer().getScene();
	}

	public GraphicsContext getGraphicsContext()
	{
		return graphicsContext;
	}

	public Compositor getCompositor() {
		return compositor;
	}


	static public class MouseState
	{
		public final Set<Integer> buttonsDown = new LinkedHashSet<Integer>();
		public final long time;
		public final int dx;
		public final int dy;
		public final int x;
		public final int y;
		public final int dwheel;

		public MouseState()
		{
			time = 0;
			dx = 0;
			dy = 0;
			x = 0;
			y = 0;
			dwheel = 0;
		}

		public MouseState(Set<Integer> buttonsDown, int x, int y, int dwheel, int dx, int dy, long time) {
			this.x = x;
			this.y = y;
			this.dwheel = dwheel;
			this.dx = dx;
			this.dy = dy;
			this.time = time;
			this.buttonsDown.addAll(buttonsDown);
		}

		public MouseState next(int button, int dwheel, int dx, int dy, int x, int y, boolean bs, long time)
		{
			Set<Integer> buttonsDown = new LinkedHashSet<Integer>(this.buttonsDown);
			if (bs && button!=-1)
				buttonsDown.add(button);
			else if (!bs && button!=-1)
				buttonsDown.remove(button);

			return new MouseState(buttonsDown, x, y, dwheel, dx, dy, time);
		}

		static public Set<Integer> buttonsPressed(MouseState before, MouseState after)
		{
			Set<Integer> b = new LinkedHashSet<Integer>(after.buttonsDown);
			b.removeAll(before.buttonsDown);
			return b;
		}

		static public Set<Integer> buttonsReleased(MouseState before, MouseState after)
		{
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
			int result = buttonsDown.hashCode();
			result = 31 * result + x;
			result = 31 * result + y;
			result = 31 * result + dwheel;
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


	}

	static public class KeyboardState
	{
		public final Set<Integer> keysDown = new LinkedHashSet<Integer>();
		public final Map<Integer, Character> charsDown = new LinkedHashMap<>();
		public final long time;

		public KeyboardState()
		{
			time = 0;
		}

		public KeyboardState(Set<Integer> keysDown, Map<Integer, Character> charsDown, long time) {
			this.keysDown.addAll(keysDown);
			this.charsDown.putAll(charsDown);
			this.time = time;
		}

		public KeyboardState next(int key, char character, boolean state, long time)
		{
			Set<Integer> keysDown = new LinkedHashSet<Integer>(this.keysDown);
			Map<Integer, Character> charsDown = new LinkedHashMap<Integer, Character>(this.charsDown);
			if (state)
			{
				keysDown.add(key);
				charsDown.put(key, character);
			}
			else
			{
				keysDown.remove(key);
				charsDown.remove(key);
			}

			return new KeyboardState(keysDown, charsDown, time);
		}

		static public Set<Integer> keysPressed(KeyboardState before, KeyboardState after)
		{
			Set<Integer> b = new LinkedHashSet<>(after.keysDown);
			b.removeAll(before.keysDown);
			return b;
		}

		static public Set<Integer> keysReleased(KeyboardState before, KeyboardState after)
		{
			Set<Integer> b = new LinkedHashSet<>(before.keysDown);
			b.removeAll(after.keysDown);
			return b;
		}

		static public Set<Character> charsPressed(KeyboardState before, KeyboardState after)
		{
			Set<Character> b = new LinkedHashSet<>(after.charsDown.values());
			b.removeAll(before.charsDown.values());
			return b;
		}

		static public Set<Character> charsReleased(KeyboardState before, KeyboardState after)
		{
			Set<Character> b = new LinkedHashSet<>(before.charsDown.values());
			b.removeAll(after.charsDown.values());
			return b;
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
	}

	protected KeyboardState keyboardState = new KeyboardState();
	protected MouseState mouseState = new MouseState();

	static public class Event<T>
	{
		public final T before;
		public final T after;

		public final Dict properties = new Dict();

		public Event(T before, T after)
		{
			this.before = before;
			this.after = after;
		}

		public String toString()
		{
			return "ev<"+before+"->"+after+">";
		}

		public Event<T> copy()
		{
			Event<T> e = new Event(before, after);
			e.properties.putAll(properties);
			return e;
		}
	}

	Queue<Function<Event<KeyboardState>,Boolean>> keyboardHandlers = new LinkedBlockingQueue<>();
	Queue<Function<Event<MouseState>,Boolean>> mouseHandlers = new LinkedBlockingQueue<>();


	public Window_lwjgl2 addKeyboardHandler(Function<Event<KeyboardState>,Boolean> h)
	{
		keyboardHandlers.add(h);
		return this;
	}

	public Window_lwjgl2 addMouseHandler(Function<Event<MouseState>, Boolean> h)
	{
		mouseHandlers.add(h);
		return this;
	}


	private void mouse() {
		while(Mouse.next())
		{
			int button = Mouse.getEventButton();
			int dwheel = Mouse.getEventDWheel();
			int dx = Mouse.getEventDX();
			int dy= Mouse.getEventDY();
			int x = Mouse.getEventX();
			int y= Mouse.getEventY();
			boolean bs = Mouse.getEventButtonState();
			long time= Mouse.getEventNanoseconds();

			MouseState was = mouseState;
			mouseState = mouseState.next(button,dwheel, dx, dy, x, y, bs, time);
			fireMouseTransition(was, mouseState);
		}
	}

	private void fireMouseTransition(MouseState before, MouseState after) {
		Iterator<Function<Event<MouseState>, Boolean>> i = mouseHandlers.iterator();
		Event<MouseState> event = new Event<>(before, after);
		while(i.hasNext())
			if (!i.next().apply(event))
				i.remove();
	}

	private void keyboard() {
		while(Keyboard.next())
		{
			char character = Keyboard.getEventCharacter();
			boolean state = Keyboard.getEventKeyState();
			int key = Keyboard.getEventKey();
			long time = Keyboard.getEventNanoseconds();

			KeyboardState was = keyboardState;
			keyboardState = keyboardState.next(key, character, state, time);
			fireKeyboardTransition(was, keyboardState);
		}
	}

	private void fireKeyboardTransition(KeyboardState before, KeyboardState after) {
		Iterator<Function<Event<KeyboardState>, Boolean>> i = keyboardHandlers.iterator();
		Event<KeyboardState> event = new Event<>(before, after);
		while(i.hasNext())
			if (!i.next().apply(event))
				i.remove();

	}

	static boolean debugKeyboardTransition(Event<KeyboardState> event)
	{
		Set<Character> pressed = KeyboardState.charsPressed(event.before, event.after);
		Set<Character> released = KeyboardState.charsReleased(event.before, event.after);
		Set<Integer> kpressed = KeyboardState.keysPressed(event.before, event.after);
		Set<Integer> kreleased = KeyboardState.keysReleased(event.before, event.after);

		if (pressed.size()>0) System.out.print("down<"+pressed+">");
		if (kpressed.size()>0) System.out.print("down<"+kpressed+">");
		if (released.size()>0) System.out.print("up<"+released+">");
		if (kreleased.size()>0) System.out.print("up<"+kreleased+">");
		System.out.println(" key@"+event.after.time+" -- ("+event.after.charsDown+")");
		return true;
	}

	static boolean debugMouseTransition(Event<MouseState> event)
	{
		Set<Integer> pressed = MouseState.buttonsPressed(event.before, event.after);
		Set<Integer> released = MouseState.buttonsReleased(event.before, event.after);
		if (pressed.size()>0) System.out.print("down<"+pressed+">");
		if (released.size()>0) System.out.print("up<"+released+">");
		System.out.println(" mouse@"+event.after.time+" "+event.after.x+" "+event.after.y+" "+event.after.dwheel+" -- "+event.after.buttonsDown);
		return true;
	}


}
