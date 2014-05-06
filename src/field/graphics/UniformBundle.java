package field.graphics;

import field.utility.Dict;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by marc on 3/25/14.
 */
public class UniformBundle implements Scene.Perform {

	Map<Dict.Prop<?>, Uniform> uniforms = new HashMap<>();

	@Override
	public boolean perform(int pass) {
		uniforms.entrySet().forEach(e -> e.getValue().perform(pass));
		return true;
	}

	public <T> Uniform<T> get(Dict.Prop<T> d) {
		return uniforms.get(d);
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
		return new int[]{-1};
	}


	static public UniformBundle setUniform(Scene at) {
		return (UniformBundle) at.scene.values().stream().flatMap(x -> x.stream()).filter(x -> x instanceof UniformBundle).findFirst().orElseGet(() -> {
			UniformBundle ub = new UniformBundle();
			at.connect(ub);
			return ub;
		});
	}

}
