package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.IdempotencyMap;
import fieldbox.boxes.Box;
import fielded.Commands;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by marc on 9/8/15.
 */
public class PresentationMode extends Box {

	static public Dict.Prop<IdempotencyMap<Runnable>> onEnterPresentationMode = new Dict.Prop<>("onEnterPresentationMode").toCannon()
															      .type()
															      .autoConstructs(() -> new IdempotencyMap<Runnable>(Runnable.class));
	static public Dict.Prop<IdempotencyMap<Runnable>> onExitPresentationMode = new Dict.Prop<>("onExitPresentationMode").toCannon()
															    .type()
															    .autoConstructs(() -> new IdempotencyMap<Runnable>(Runnable.class));

	static public Dict.Prop<Runnable> enterPresentationMode = new Dict.Prop<>("enterPresentationMode").toCannon()
													  .type();
	static public Dict.Prop<Runnable> exitPresentationMode = new Dict.Prop<>("exitPresentationMode").toCannon()
		.type();

	static public Dict.Prop<PresentationMode> _presentationMode = new Dict.Prop<>("_presentationMode").toCannon()
		.type();

	boolean present = false;

	public PresentationMode(Box root_ignored) {
		this.properties.put(enterPresentationMode, this::enterPresentationMode);
		this.properties.put(exitPresentationMode, this::exitPresentationMode);

		Commands.exportAsCommand(this, this::enterPresentationMode, (x) -> !present, "Enter Presentation Mode", "");
		Commands.exportAsCommand(this, this::exitPresentationMode, (x) -> present, "Exit Presentation Mode", "");

		this.properties.put(_presentationMode, this);
	}

	private void enterPresentationMode() {
		if (present) return;
		List<Runnable> l = this.breadthFirst(both())
				       .filter(x -> x.properties.has(onEnterPresentationMode))
				       .flatMap(x -> x.properties.get(onEnterPresentationMode)
								 .values()
								 .stream())
				       .collect(Collectors.toList());
		for (Runnable rr : l) {
			try {
				rr.run();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		present = true;
	}

	private void exitPresentationMode() {
		if (!present) return;

		List<Runnable> l = this.breadthFirst(both())
				       .filter(x -> x.properties.has(onExitPresentationMode))
				       .flatMap(x -> x.properties.get(onExitPresentationMode)
								 .values()
								 .stream())
				       .collect(Collectors.toList());
		for (Runnable rr : l) {
			try {
				rr.run();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		present = false;
	}

	public boolean isPresent() {
		return present;
	}
}
