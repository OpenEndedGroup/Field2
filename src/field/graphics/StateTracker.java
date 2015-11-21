package field.graphics;

import field.utility.Log;
import field.utility.Util;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

/**
 * Some OpenGL state needs to be tracked (given the slowness and deprecation of glPush/PopAttrib. Most of glPushAttrib referred to old fixed function stuff that we don't use any more; but there are
 * still a handful of things that need tracking -- viewport, current shader etc.
 */
public class StateTracker {

	public final State<int[]> viewport = new State<int[]>() {
		@Override
		protected void apply(int[] value) {
			Log.log("graphics.trace", () -> "setting viewport to " + value[0] + " " + value[1] + " " + value[2] + " " + value[3]);
			glViewport(value[0], value[1], value[2], value[3]);
		}
	};
	public final State<int[]> scissor = new State<int[]>() {
		@Override
		protected void apply(int[] value) {
			Log.log("graphics.trace", () -> "setting scissor to " + value[0] + " " + value[1] + " " + value[2] + " " + value[3]);
			glScissor(value[0], value[1], value[2], value[3]);
			glEnable(GL_SCISSOR_TEST);
		}
	};
	public final State<Integer> shader = new State<Integer>() {
		@Override
		protected void apply(Integer value) {
			Log.log("graphics.trace", () -> "setting program to " + value);
			GL20.glUseProgram(value);
		}
	};
	public final State<Integer> fbo = new State<Integer>() {
		@Override
		protected void apply(Integer value) {
			Log.log("graphics.trace", () -> "setting framebuffer to " + value + " <- " + fbo.value + " " + GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER));
			glBindFramebuffer(GL_FRAMEBUFFER, value == null ? 0 : value);
			Log.log("graphics.trace", () -> "set framebuffer to " + value + " <- " + fbo.value + " " + GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER));
		}
	};
	public final State<int[]> blendState = new State<int[]>() {
		@Override
		protected void apply(int[] value) {
			glBlendFunc(value[0], value[1]);
		}
	};

	LinkedHashMap<String, State> allStates = new LinkedHashMap<>();

	protected StateTracker() {

	}

	public Util.ExceptionlessAutoCloasable save() {

		try {
			if (GraphicsContext.currentGraphicsContext == null) throw new IllegalStateException(" save() only valid inside draw method ");
			for (Field f : this.getClass()
					   .getDeclaredFields()) {
				if (f.getType()
				     .isAssignableFrom(State.class)) {
					try {
						State s = (State) f.get(this);
						allStates.put(f.getName(), s);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			List<AutoCloseable> r = new ArrayList<>();
			for (State s : allStates.values()) {

				r.add(s.save());
				GraphicsContext.checkError(() -> "" + s);
			}

			return () -> {

				r.forEach(x -> {
					try {
						x.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			};
		} finally {
//				GraphicsContext.checkError();
		}
	}

	/**
	 * dumps the current state to a string
	 *
	 * @return
	 */
	public String dumpOutput() {
		String s = "";
		for (Map.Entry<String, State> e : allStates.entrySet()) {
			s += e.getKey() + " = " + e.getValue().value + "\n";
		}
		return s;
	}

	static public abstract class State<T> {

		private T value;

		public T set(T m) {
			T was = value;
			value = m;
			apply(value);
			return was;
		}

		/**
		 * returns a Runnable that can be used to restore this value to this point. Note that this is only valid for the current draw method
		 */
		public Util.ExceptionlessAutoCloasable save() {
			if (value == null) return () -> {
			};

			if (GraphicsContext.currentGraphicsContext.get() == null) throw new IllegalStateException(" save() only valid inside draw method ");
			T v = value;
			AtomicBoolean b = new AtomicBoolean(false);
			GraphicsContext.currentGraphicsContext.get().postQueue.add(() -> {
				b.set(true);
			});
			return () -> {
				if (b.get()) throw new IllegalStateException(" save() tokens are only valid for current render cycle");

				set(v);
			};
		}

		public T get() {
			return value;
		}


		abstract protected void apply(T value);
	}

}
