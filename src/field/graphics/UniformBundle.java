package field.graphics;

import field.utility.Dict;
import fieldbox.execution.Completion;
import fieldbox.execution.Errors;

import java.util.*;
import java.util.function.Supplier;

/**
 * Maintains a group of Uniforms as a Map<Dict.Prop<T>, Uniform<T>>, without the overhead of maintaining them as separate Scene.Perform objects.
 *
 * The star method here is the static UniformBundle setUniform(Scene at) which will lazily initialize a UniformBundle child of any scene ready to accept uniforms.
 */
public class UniformBundle implements Scene.Perform {

	Map<Dict.Prop<?>, Uniform> uniforms = new HashMap<>();

	/** Uniform Bundles pass all errors onto the Uniforms themselves */
	@Override
	public Errors.ErrorConsumer getErrorConsumer() {
		return null;
	}

	@Override
	public void setErrorConsumer(Errors.ErrorConsumer c) {
	}

	@Override
	public boolean perform(int pass) {
		uniforms.entrySet().forEach(e -> e.getValue().perform(pass));
		return true;
	}

	public <T> Uniform<T> get(Dict.Prop<T> d) {
		return uniforms.get(d);
	}

	public <T> Uniform<T> get(String d) {
		return uniforms.get(new Dict.Prop<T>(d));
	}

	public <T> Uniform<T> set(Dict.Prop<T> d, Supplier<T> s) {
		return uniforms.computeIfAbsent(d, (k) -> new Uniform<T>(d.getName(), s)).setValue(s);
	}


	public <T> Uniform<T> set(String d, Supplier s) {
		return uniforms.computeIfAbsent(new Dict.Prop(d), (k) -> new Uniform<T>(d, s)).setValue(s);
	}


	public <T> void remove(Dict.Prop<T> d) {
		uniforms.remove(d);
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1, 1};
	}


	static public UniformBundle setUniform(Scene at) {
		return (UniformBundle) at.scene.values().stream().flatMap(x -> x.stream()).filter(x -> x instanceof UniformBundle).findFirst().orElseGet(() -> {
			UniformBundle ub = new UniformBundle();
			at.attach(ub);
			return ub;
		});
	}

	static public Object getUniformHere(Scene at, String name) {
		return ((UniformBundle) at.scene.values().stream().flatMap(x -> x.stream()).filter(x -> x instanceof UniformBundle).findFirst().orElseGet(() -> {
			UniformBundle ub = new UniformBundle();
			at.attach(ub);
			return ub;
		})).get(new Dict.Prop(name));
	}

	@Override
	public String toString() {

		String q = uniforms.keySet()
						  .stream()
						  .map(x -> x.getName())
						  .reduce((x, y) -> x + ", " + y).orElseGet(()->"");
		return "uniforms{"+q+"}";
	}

	public Collection<? extends Completion> getCompletionsFor(String prefix) {
		List<Completion> cc = new ArrayList<>();
		for (Map.Entry<Dict.Prop<?>, Uniform> entry : uniforms.entrySet()) {
			if (prefix==null || prefix.equals("") || entry.getKey().getName().startsWith(prefix))
			{
				Completion c = new Completion(-1,-1, entry.getKey().getName(),"="+entry.getValue().get());
				cc.add(c);
			}
		}

		return cc;
	}
}
