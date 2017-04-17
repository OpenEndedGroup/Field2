package field.graphics;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import field.app.RunLoop;
import field.utility.Log;
import org.lwjgl.glfw.*;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.lwjgl.glfw.GLFW.*;

/**
 * All Window instances must be registered with this singleton Windows.
 * <p>
 * GLFW multiplexes all of it's events through a singlecallback, this class demultiplexes them and inserts the events into our main thread.
 */
public class Windows {

	static public final Windows windows = new Windows();
	private final ClassLoader mainClassLoader;
	private final GlfwCallback c;

	private Windows() {


		RunLoop.main.getLoop()
			.attach(-2, this::events);

		glfwInit();

		c = makeCallback();
//		glfwSetCallback(c);


		mainClassLoader = Thread.currentThread()
			.getContextClassLoader();


		Log.log("startup.debug", () -> "main thread is :" + Thread.currentThread());
	}


	Deque<Runnable> events = new ConcurrentLinkedDeque<>();

	protected boolean events(int p) {
		RunLoop.main.shouldSleep.remove(Windows.this);

		if (events.size() > 0) Log.log("event.debug", () -> "events :" + events.size());
		while (!events.isEmpty()) {
			try {
				events.removeFirst()
					.run();
			} catch (Throwable t) {
				Log.log("events.error", () -> "Exception thrown while handling event" + t);
			}
		}
		return true;
	}

	public void init() {
		// this is already done once in the singleton constructor
	}


	Map<Long, GlfwCallback> adaptors = new LinkedHashMap<>();
	Multimap<Long, Object> callbacksForWindows = ArrayListMultimap.create();


	public void register(long window, GlfwCallback adaptor) {
		adaptors.put(window, adaptor);

		glfwSetCharCallback(window, keep(window, GLFWCharCallback.create(c::character)));
		glfwSetKeyCallback(window, keep(window, GLFWKeyCallback.create(c::key)));
		glfwSetCharModsCallback(window, keep(window, GLFWCharModsCallback.create((w, codepoint, mods) -> {
//			System.out.println(" char mod called ? " + w + " " + codepoint + " " + mods);
		})));
		glfwSetWindowSizeCallback(window, keep(window, GLFWWindowSizeCallback.create(c::windowSize)));
		glfwSetWindowPosCallback(window, keep(window, GLFWWindowPosCallback.create(c::windowPos)));
		glfwSetCursorPosCallback(window, keep(window, GLFWCursorPosCallback.create(c::cursorPos)));
		glfwSetMouseButtonCallback(window, keep(window, GLFWMouseButtonCallback.create(c::mouseButton)));
		glfwSetWindowCloseCallback(window, keep(window, GLFWWindowCloseCallback.create(c::windowClose)));
		glfwSetErrorCallback(keep(0, GLFWErrorCallback.create((a, b) -> c.error(a, "" + b))));
		glfwSetCursorEnterCallback(window, keep(window, GLFWCursorEnterCallback.create((w, en) -> c.cursorEnter(w, en == true))));
		glfwSetWindowRefreshCallback(window, keep(window, GLFWWindowRefreshCallback.create(c::windowRefresh)));
		glfwSetWindowFocusCallback(window, keep(window, GLFWWindowFocusCallback.create((w, en) -> c.windowFocus(w, en == true))));
		glfwSetScrollCallback(window, keep(window, GLFWScrollCallback.create(c::scroll)));
		glfwSetDropCallback(window, keep(window, GLFWDropCallback.create((a, b, d) -> c.drop(a, null))));
	}

	private <T> T keep(long window, T t) {
		callbacksForWindows.put(window, t);
		return t;
	}

	protected GlfwCallback makeCallback() {
		return new GlfwCallback() {

			@Override
			public void error(int error, String description) {
				System.err.println(" ERROR in GLFW windowing system :" + error + " / " + description);
				Runnable r = () -> {
					checkClassLoader();
					System.err.println(" ERROR in GLFW windowing system :" + error + " / " + description);
					new Exception().printStackTrace();
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}

			@Override
			public void windowFocus(long window, boolean focused) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null)
						a.windowFocus(window, focused);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}

			@Override
			public void windowRefresh(long window) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null) a.windowRefresh(window);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}


			boolean fakeButton1 = false;

			@Override
			public void mouseButton(long window, int button, int pressed, int mods) {
				if (button == 0 && mods == 2 && pressed != 0) {
					button = 1;
					mods = 0;
					fakeButton1 = true;
				} else if (button == 0 && pressed == 0 && fakeButton1) {
					button = 1;
					mods = 0;
					fakeButton1 = false;
				}

				final int fbutton = button;
				final int fmods = mods;

				Runnable r = () -> {
					checkClassLoader();


					GlfwCallback a = adaptors.get(window);

//					// added, because mouse down doesn't report the mouse location correctly on first click in os X
					// removed, because El Capitan reports getCursorPos in screen coords
//					if (Main.os == Main.OS.mac) {
//						if (a != null)
//							a.cursorPos(window, Glfw.glfwGetCursorPosX(window), Glfw.glfwGetCursorPosY(window));
//					}


					if (a != null)
						a.mouseButton(window, fbutton, pressed, fmods);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}


			@Override
			public void scroll(long window, double scrollX, double scrollY) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null)
						a.scroll(window, scrollX, scrollY);
				};

				if (RunLoop.main.isMainThread()) {
					r.run();
				} else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}


			@Override
			public void cursorPos(long window, double x, double y) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null)
						a.cursorPos(window, x, y);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}

			@Override
			public void key(long window, int key, int scancode, int action, int mods) {

				// we occasionally get a spurious 'a' (scancode 0) on command-tabbing to our application. If it's just a plain 'a' let's assume that the character callback will handle it
				if (scancode == 0 && mods == 0) return;

				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null)
						a.key(window, key, scancode, action, mods);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}

			@Override
			public void character(long window, int character) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null)
						a.character(window, character);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}

			@Override
			public void drop(long window, String[] files) {
				Runnable r = () -> {
					checkClassLoader();
					// cursorPos is given just before drop to tell us where the drop has occured.
					GlfwCallback a = adaptors.get(window);
					if (a != null) a.drop(window, files);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}


			@Override
			public boolean windowClose(long window) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null) a.windowClose(window);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
				return true;
			}

			@Override
			public void windowPos(long window, int x, int y) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null)
						a.windowPos(window, x, y);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}

			@Override
			public void windowSize(long window, int w, int h) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null)
						a.windowSize(window, w, h);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}
			}

			@Override
			public void framebufferSize(long window, int w, int h) {
				Runnable r = () -> {
					checkClassLoader();
					GlfwCallback a = adaptors.get(window);
					if (a != null)
						a.framebufferSize(window, w, h);
				};

				if (RunLoop.main.isMainThread()) r.run();
				else {
					events.addLast(r);
					RunLoop.main.shouldSleep.add(Windows.this);
				}

			}
		};


	}

	private void checkClassLoader() {
		ClassLoader c = Thread.currentThread()
			.getContextClassLoader();
		if (c != mainClassLoader) {
			Log.log("startup.debug", () -> "had to change classloader from <" + c + "- >" + mainClassLoader);
			Thread.currentThread()
				.setContextClassLoader(mainClassLoader);
		}


	}

}
