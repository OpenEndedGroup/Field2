package field.graphics.util;

import field.app.RunLoop;
import field.graphics.Camera;
import field.graphics.Window;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.KeyboardFocus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static field.graphics.Window.KeyboardState;
import static org.lwjgl.glfw.GLFW.*;

/**
 * A Keyboard controlled camera. Standard First-Person camera controls with the arrow keys + orbit while holding down shift. Smooths out attack and
 * release of the keys.
 */
public class KeyboardCamera implements Function<Window.Event<KeyboardState>, Boolean> {

	private final Camera target;
	private Window w;


	public class Applicator {
		float amount;
		BiFunction<Camera.State, Float, Camera.State> apply;

		public Applicator(float amount, BiFunction<Camera.State, Float, Camera.State> apply) {
			this.amount = amount;
			this.apply = apply;
		}
	}

	Map<KeyBinding.KeyName, Applicator> bindings = new LinkedHashMap<>();

	public float rotationAmount = 0.02f;
	public float translationAmount = 0.02f;

	public float decay = 0.9f;
	public float onset = 0.4f;

	public void standardMap() {
		bindings.put(new KeyBinding.KeyName("page_up"), new Applicator(0, (state, amount) -> state
			.lookUp(rotationAmount * amount)));
		bindings.put(new KeyBinding.KeyName("shift-page_up"), new Applicator(0, (state, amount) -> state
			.orbitUp(rotationAmount * amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_PAGE_DOWN, false, false, false, false), new Applicator(0, (state, amount) -> state
			.lookUp(rotationAmount * -amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_PAGE_DOWN, true, false, false, false), new Applicator(0, (state, amount) -> state
			.orbitUp(rotationAmount * -amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_LEFT, false, false, false, false), new Applicator(0, (state, amount) -> state
			.lookLeft(rotationAmount * amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_LEFT, true, false, false, false), new Applicator(0, (state, amount) -> state
			.orbitLeft(rotationAmount * amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_RIGHT, false, false, false, false), new Applicator(0, (state, amount) -> state
			.lookLeft(-rotationAmount * amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_RIGHT, true, false, false, false), new Applicator(0, (state, amount) -> state
			.orbitLeft(-rotationAmount * amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_UP, false, false, false, false), new Applicator(0, (state, amount) -> state
			.translateIn(translationAmount * amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_UP, true, false, false, false), new Applicator(0, (state, amount) -> state
			.dollyIn(translationAmount * amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_DOWN, false, false, false, false), new Applicator(0, (state, amount) -> state
			.translateIn(-translationAmount * amount)));
		bindings.put(new KeyBinding.KeyName(GLFW_KEY_DOWN, true, false, false, false), new Applicator(0, (state, amount) -> state
			.dollyIn(-translationAmount * amount)));


	}

	public KeyboardCamera(Camera target, Window w) {
		this.target = target;

		RunLoop.main.mainLoop.attach(0, (i) -> update());
		w.addKeyboardHandler(this);
	}

	public KeyboardCamera(Camera target, Box viewport) {
		this.target = target;

		viewport.properties.putToMap(Keyboard.onKeyDown, "keyboardcontrolledcamera", (k, c) -> {

			if (!viewport.properties.isTrue(Mouse.isSelected, false) || !bindings.keySet().stream().filter(x -> x.matches(k.after)).findAny().isPresent())
				return null;

			if (!getKeyboardFocus(viewport).isFocused(viewport)) return null;

			k.properties.put(Window.consumed, true);

			currentState = k.after;
			return (k2, t) -> {
				currentState = k2.after;
				return !t;
			};
		});
		viewport.properties.putToMap(Boxes.insideRunLoop, "main._applyCamera", () -> {
			if (update()) {
				Drawing.dirty(viewport);
			}
			return true;
		});

	}

	public KeyboardCamera(Camera target, Box viewport, String suffix) {
		this.target = target;

		viewport.properties.putToMap(Keyboard.onKeyDown, suffix+"keyboardcontrolledcamera", (k, c) -> {

			if (!viewport.properties.isTrue(Mouse.isSelected, false) || !bindings.keySet().stream().filter(x -> x.matches(k.after)).findAny().isPresent())
				return null;

			if (!getKeyboardFocus(viewport).isFocused(viewport)) return null;

			k.properties.put(Window.consumed, true);

			currentState = k.after;
			return (k2, t) -> {
				currentState = k2.after;
				return !t;
			};
		});
		viewport.properties.putToMap(Boxes.insideRunLoop, "main._applyCamera"+suffix, () -> {
			if (update()) {
				Drawing.dirty(viewport);
			}
			return true;
		});

	}


	KeyboardFocus cachedFocus = null;

	private KeyboardFocus getKeyboardFocus(Box viewport) {
		return cachedFocus == null ? cachedFocus = viewport.find(KeyboardFocus._keyboardFocus, viewport.both()).findFirst().get() : cachedFocus;
	}

	private boolean update() {

		if (currentState == null) return false;

		if (w != null)
			currentState = currentState.clean(w.getGLFWWindowReference());

		boolean[] changed = {false};
		bindings.entrySet().forEach((k) -> {
			if (currentState != null && k.getKey().matches(currentState)) {
				k.getValue().amount = k.getValue().amount * onset + (1 - onset) * 1;
			} else {
				k.getValue().amount *= decay;
			}

			if (k.getValue().amount > 1e-5) {
				target.advanceState(s -> k.getValue().apply.apply(s, k.getValue().amount));
				changed[0] = true;
			}
		});
		return changed[0];
	}


	KeyboardState currentState = null;

	@Override
	public Boolean apply(Window.Event<KeyboardState> keyboardStateEvent) {
		currentState = keyboardStateEvent.after;
		return true;
	}


}
