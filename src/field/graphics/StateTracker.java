package field.graphics;

import field.utility.Log;
import field.utility.Util;
import org.lwjgl.opengl.GL20;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL11.*;

/**
 * Some OpenGL state needs to be tracked (given the slowness and deprecation of glPush/PopAttrib. Most of glPushAttrib referred to old fixed function
 * stuff that we don't use any more; but there are still a handful of things that need tracking -- viewport, current shader etc.
 */
public class StateTracker {

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
			if (value == null) return () -> {};

			if (GraphicsContext.currentGraphicsContext == null) throw new IllegalStateException(" save() only valid inside draw method ");
			T v = value;
			AtomicBoolean b = new AtomicBoolean(false);
			GraphicsContext.currentGraphicsContext.postQueue.add(() -> {
				b.set(true);
			});
			return () -> {
				if (b.get()) throw new IllegalStateException(" save() tokens are only valid for current render cycle");

				set(v);
			};
		}

		public T get()
		{
			return value;
		}


		abstract protected void apply(T value);
	}

	static LinkedHashMap<String, State> allStates = new LinkedHashMap<>();

	static public State<int[]> viewport = new State<int[]>() {
		@Override
		protected void apply(int[] value) {
			glViewport(value[0], value[1], value[2], value[3]);
		}
	};

	static public State<int[]> scissor = new State<int[]>() {
		@Override
		protected void apply(int[] value) {
			glScissor(value[0], value[1], value[2], value[3]);
			glEnable(GL_SCISSOR_TEST);
		}
	};

	static public State<Integer> shader = new State<Integer>() {
		@Override
		protected void apply(Integer value) {
			GL20.glUseProgram(value);
		}
	};


	protected StateTracker() {

	}

	public Util.ExceptionlessAutoCloasable save() {
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
		for (State s : allStates.values())
			r.add(s.save());

		return () -> {

			r.forEach(x -> {
				try {
					x.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		};
	}

}
