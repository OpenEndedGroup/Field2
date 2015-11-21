package field.graphics;

import field.utility.Util;

import java.util.*;

/**
 * GraphicsContext global cache of uniform values. The key complexity here is that OpenGL want uniforms to be set on an active shader, whereas we want uniforms to be set anywhere -- across multiple
 * shader, and before shaders become active.
 */
public class UniformCache {

	static public class Cache {
		final String name;
		final List<Object> value = new ArrayList<>();
		final List<Integer> on = new ArrayList<>();
		final List<Runnable> trampoline = new ArrayList<>();

		public Cache(String name) {
			this.name = name;
		}

		public boolean push(Object value, Runnable trampoline) {
			this.value.add(value);
			this.on.add(GraphicsContext.getContext().stateTracker.shader.get());
			this.trampoline.add(trampoline);

			return this.value.size()==1 || !Util.safeEq(this.value.get(this.value.size()-2), this.value.get(this.value.size()-1)) || !Util.safeEq(this.on.get(this.on.size()-2), this.on.get(this.on.size()-1));

		}

		public void refresh()
		{
			if (trampoline.size()==0) return;
			trampoline.get(trampoline.size()-1).run();
		}

		public Runnable pop() {
			if (value.size() == 0) {
				System.err.println(" warning: uniform cache for <" + name + "> popped without corresponding push");
				return null;
			}
			Object popped = value.remove(value.size() - 1);
			on.remove(on.size()-1);

			Runnable r = trampoline.remove(trampoline.size() - 1);

			return r;
		}
	}

	public LinkedHashMap<String, Cache> cache = new LinkedHashMap<>();

	public boolean push(String name, Object value, Runnable trampoline)
	{
		return cache.computeIfAbsent(name, (k) -> new Cache(name)).push(value, trampoline);
	}

	public Runnable pop(String name)
	{
		return cache.computeIfAbsent(name, (k) -> new Cache(name)).pop();
	}

	public void changeShader(int newName)
	{
		cache.values().forEach(x -> x.refresh());
	}

}
