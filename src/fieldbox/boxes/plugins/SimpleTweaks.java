package fieldbox.boxes.plugins;

import field.utility.Dict;
import fieldbox.boxes.Box;

public class SimpleTweaks extends Box {

	static public final Dict.Prop<FunctionOfBox<Boolean>> applyTweaks = new Dict.Prop<>("applyTweaks").type().toCanon().doc("call `_.applyTweaks()` to make `FLine`s in this box that have been " +
		"marked as `line.tweaky = true` hand editable, and to apply their edits");
	static public final Dict.Prop<FunctionOfBox<Boolean>> clearTweaks = new Dict.Prop<>("clearTweaks").type().toCanon().doc("call `_.clearTweaks()` to clear any tweaks applied to lines in this" +
		" box");

	static public final Dict.Prop<String> _simpleTweaks = new Dict.Prop<>("_simpleTweaks").type().toCanon().autoConstructs(() -> "");
	static public final Dict.Prop<Boolean> tweaky = new Dict.Prop<>("tweaky").type().toCanon();

	String tweakNow = BoxDefaultCode.findSource(this.getClass(), "tweakNow");

	public SimpleTweaks(Box root) {

		this.properties.put(applyTweaks, (box) -> {
			box.find(Exec.exec, box.upwards()).findFirst().ifPresent(x -> x.apply(box, tweakNow));
			return true;
		});
		this.properties.put(clearTweaks, (box) -> {
			box.properties.put(_simpleTweaks, "");
			return true;
		});
	}
}
