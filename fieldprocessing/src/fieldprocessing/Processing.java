package fieldprocessing;

import field.graphics.FLine;
import field.graphics.RunLoop;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.boxes.Box;
import fielded.Execution;
import fielded.RemoteEditor;
import org.omg.CORBA.TIMEOUT;
import processing.core.PApplet;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Processing Plugin. Refer to Processing.applet to get at the applet.
 *
 * E.g. var P = Java.type('fieldprocessing.Processing').applet
 *
 * This adds a command "Bridge box to Processing". Run that to move this box (and any children) into the Processing draw cycle. Then you can write things like:
 *
 * P.background(0) // sets background to black
 *
 */
public class Processing extends Box {

	private final ProcessingExecution processingExecution;
	public static PApplet applet;

	// synchronized via Runloop.lock
	public List<Runnable> queue = new ArrayList<>();

	protected JFrame frame;

	public Processing(Box root) {

		Log.log("startup.processing", " processing plugin is starting up ");

		int sizeX = AutoPersist.persist("processing_sizeX", () -> 400, x -> Math.min(2560, Math.max(100, x)), (x) -> frame.getSize().width);
		int sizeY = AutoPersist.persist("processing_sizeY", () -> 400, x -> Math.min(2560, Math.max(100, x)), (x) -> frame.getSize().height);

		frame = new JFrame("Field/Processing");
		applet = new PApplet() {
			@Override
			public void setup() {
				size(sizeX, sizeY);
			}

			@Override
			public void draw() {
				try {
					if (RunLoop.lock.tryLock(1, TimeUnit.DAYS)) {
						for (Runnable r : queue) {
							try {
								r.run();
							} catch (Throwable t) {
								System.err.println(" exception thrown inside Processing runloop");
								t.printStackTrace();
							}
						}
						queue.clear();

						find(Boxes.insideRunLoop, both()).forEach(x -> {

							Iterator<Map.Entry<String, Supplier<Boolean>>> rn = x.entrySet().iterator();
							while (rn.hasNext()) {
								Map.Entry<String, Supplier<Boolean>> n = rn.next();
								if (n.getKey().startsWith("processing.")) {
									try {
										if (!n.getValue().get()) {
											rn.remove();
											Drawing.dirty(Processing.this);
										}
									} catch (Throwable t) {
										t.printStackTrace();
									}
								}
							}
						});

					} else {
						System.out.println(" didn't acquire lock ?");
					}
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					RunLoop.lock.unlock();
				}

			}
		};
		applet.init();
		applet.loop();
		frame.add(applet, BorderLayout.CENTER);
		frame.setSize(sizeX, sizeY);
		frame.setVisible(true);
		frame.validate();

		Execution delegate = root.find(Execution.execution, root.both()).findFirst()
			    .orElseThrow(() -> new IllegalArgumentException(" can't instantiate Processing execution - no default execution found"));
		processingExecution = new ProcessingExecution(delegate, queue);

		root.connect(processingExecution);

		properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			List<Box> selected = selection().collect(Collectors.toList());
			if (selected.size() == 1) {
				if (selected.get(0).properties.isTrue(ProcessingExecution.bridgedToProcessing, false)) {
					m.put(new Pair<>("Remove bridge to Processing", "No longer will this box execute inside the Processing draw method"), () -> {
						disconnectFromProcessing(selected.get(0));
					});
				} else {
					m.put(new Pair<>("Bridge to Processing", "This box execute will inside the Processing draw method"), () -> {
						connectToProcessing(selected.get(0));
					});
				}
			}
			return m;
		});


		Log.log("startup.processing"," searching for boxes that need processing support ");

		// we delay this for one update cycle to make sure that everybody has loaded everything that they are going to load
		RunLoop.main.once(() -> {
			root.breadthFirst(both()).forEach(box -> {
				if (box.properties.isTrue(ProcessingExecution.bridgedToProcessing, false)) {
					connectToProcessing(box);
				}
			});
		});

		Log.log("startup.processing", " processing plugin has finished starting up ");


	}

	protected void connectToProcessing(Box box) {
		processingExecution.connect(box);
		box.properties.put(ProcessingExecution.bridgedToProcessing, true);

		box.properties.putToMap(FLineDrawing.frameDrawing, "_processingBadge_", new Cached<Box, Object, FLine>((b, was) -> {

			Rect rect = box.properties.get(Box.frame);
			if (rect == null) return null;

			FLine f = new FLine();
			f.attributes.put(FLineDrawing.hasText, true);
			f.attributes.put(FLineDrawing.fillColor, new Vec4(0,0,0.25f,0.5f));
			f.moveTo(rect.x + rect.w - 7, rect.y + rect.h - 5);
			f.nodes.get(f.nodes.size() - 1).attributes.put(FLineDrawing.text, "P");

			return f;

		}, (b) -> new Pair(b.properties.get(ProcessingExecution.bridgedToProcessing), b.properties.get(Box.frame))));
		Drawing.dirty(box);

	}

	protected void disconnectFromProcessing(Box box) {
		processingExecution.disconnect(box);
		box.properties.remove(ProcessingExecution.bridgedToProcessing);
		box.properties.removeFromMap(FLineDrawing.frameDrawing, "_processingBadge_");
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}
