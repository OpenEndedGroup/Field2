package field.graphics.util;

import com.badlogic.jglfw.Glfw;
import field.graphics.Camera;
import field.graphics.RunLoop;
import field.graphics.Window;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static field.graphics.Window.KeyboardState;

/**
 * Created by marc on 4/14/14.
 */
public class KeyboardCamera implements Function<Window.Event<KeyboardState>, Boolean> {

	private final Camera target;

	public class Applicator {
		float amount;
		BiFunction<Camera.State, Float, Camera.State> apply;

		public Applicator(float amount, BiFunction<Camera.State, Float, Camera.State> apply) {
			this.amount = amount;
			this.apply = apply;
		}
	}

	Map<KeyBinding.KeyName, Applicator> bindings = new LinkedHashMap<>();

	float rotationAmount = 0.01f;
	float translationAmount = 0.01f;
	float decay = 0.99f;
	float onset = 0.9f;

	public void standardMap() {
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_PAGE_UP, false, false, false, false), new Applicator(0, (state, amount) -> state.lookUp(rotationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_PAGE_UP, true, false, false, false), new Applicator(0, (state, amount) -> state.orbitUp(rotationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_PAGE_DOWN, false, false, false, false), new Applicator(0, (state, amount) -> state.lookUp(-rotationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_PAGE_DOWN, true, false, false, false), new Applicator(0, (state, amount) -> state.orbitUp(-rotationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_LEFT, false, false, false, false), new Applicator(0, (state, amount) -> state.lookLeft(-rotationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_LEFT, true, false, false, false), new Applicator(0, (state, amount) -> state.orbitLeft(-rotationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_RIGHT, false, false, false, false), new Applicator(0, (state, amount) -> state.lookLeft(rotationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_RIGHT, true, false, false, false), new Applicator(0, (state, amount) -> state.orbitLeft(rotationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_UP, false, false, false, false), new Applicator(0, (state, amount) -> state.translateIn(translationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_UP, true, false, false, false), new Applicator(0, (state, amount) -> state.dollyIn(translationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_DOWN, false, false, false, false), new Applicator(0, (state, amount) -> state.translateIn(-translationAmount)));
		bindings.put(new KeyBinding.KeyName(Glfw.GLFW_KEY_DOWN, true, false, false, false), new Applicator(0, (state, amount) -> state.dollyIn(-translationAmount)));
	}

	public KeyboardCamera(Camera target) {
		this.target = target;
		RunLoop.main.mainLoop.connect(0, (i) -> update());
	}

	private void update() {
		bindings.entrySet().forEach((k) -> {
			if (currentState != null && k.getKey().matches(currentState)) {
				k.getValue().amount = k.getValue().amount * onset + (1 - onset) * 1;
			} else {
				k.getValue().amount *= decay;
			}

			if (k.getValue().amount > 1e-5)
				target.advanceState( s -> k.getValue().apply.apply(s, k.getValue().amount));
		});
	}


	KeyboardState currentState = null;

	@Override
	public Boolean apply(Window.Event<KeyboardState> keyboardStateEvent) {
		currentState = keyboardStateEvent.after;
		return true;
	}


}
