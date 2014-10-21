package fieldprocessing;

import field.graphics.RunLoop;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Drawing;
import field.utility.IdempotencyMap;
import fieldprocessing.Processing.KeyHandler;
import fieldprocessing.Processing.MouseHandler;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.awt.event.MouseWheelEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FieldProcessingApplet extends PApplet {

	private final int sizeX;
	private final int sizeY;
	private final List<Runnable> queue;
	private final Box box;
	private Consumer<String> error;

	protected FieldProcessingApplet(int sizeX, int sizeY, List<Runnable> queue, Box root, Consumer<String> error) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.queue = queue;
		this.box = root;
		this.error = error;
	}


	@Override
	public void setup() {
		size(sizeX, sizeY, P3D);
	}

	@Override
	public void draw() {
		try {
			if (RunLoop.lock.tryLock(1, TimeUnit.DAYS)) {
				try {
					for (Runnable r : queue) {
						try {
							r.run();
						} catch (Throwable t) {
							System.err.println(" exception thrown inside Processing runloop");
							t.printStackTrace();
							error.accept(t.getMessage());
						}
					}
					queue.clear();

					box.find(Boxes.insideRunLoop, box.both()).forEach(x -> {

						Iterator<Map.Entry<String, Supplier<Boolean>>> rn = x.entrySet().iterator();
						while (rn.hasNext()) {
							Map.Entry<String, Supplier<Boolean>> n = rn.next();
							if (n.getKey().startsWith("processing.")) {
								try {
									if (!n.getValue().get()) {
										rn.remove();
										Drawing.dirty(box);
									}
								} catch (Throwable t) {
									System.err.println(" exception thrown inside Processing runloop");
									t.printStackTrace();
									error.accept(t.getMessage());
									rn.remove();
								}
							}
						}
					});
				}
				finally
				{
					autoPopMatrix();
				}
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

	protected int popCount = 0;

	@Override
	public void pushMatrix() {
		super.pushMatrix();
		popCount++;
	}

	@Override
	public void popMatrix() {
		super.popMatrix();
		popCount--;
	}

	protected void autoPopMatrix()
	{
		while(popCount>0)
			popMatrix();
	}

	/**
	 * A Map containing handlers to be called when the mouse is clicked with (Applet, MouseEvent)
	 */
	public final Map<String, MouseHandler> onMouseClicked = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse is moved with (Applet, MouseEvent)
	 */
	public final Map<String, MouseHandler> onMouseMoved = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse is pressed with (Applet, MouseEvent)
	 */
	public final Map<String, MouseHandler> onMousePressed = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse is dragged with (Applet, MouseEvent)
	 */
	public final Map<String, MouseHandler> onMouseDragged = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse enters with (Applet, MouseEvent)
	 */
	public final Map<String, MouseHandler> onMouseEntered = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse exits with (Applet, MouseEvent)
	 */
	public final Map<String, MouseHandler> onMouseExited = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse is released with (Applet, MouseEvent)
	 */
	public final Map<String, MouseHandler> onMouseReleased = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse wheel is pressed with (Applet, MouseEvent)
	 */
	public final Map<String, MouseHandler> onMouseWheel = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse wheel is moved with (Applet, MouseWheelEvent)
	 */
	public final Map<String, MouseHandler> onMouseWheelMoved = new IdempotencyMap<>(MouseHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse wheel is moved with (Applet, MouseWheelEvent)
	 */
	public final Map<String, KeyHandler> onKeyPressed = new IdempotencyMap<>(KeyHandler.class);

	/**
	 * A Map containing handlers to be called when the mouse wheel is moved with (Applet, MouseWheelEvent)
	 */
	public final Map<String, KeyHandler> onKeyReleased = new IdempotencyMap<>(KeyHandler.class);

	@Override
	public void mouseClicked(MouseEvent event) {
		onMouseClicked.values().forEach(x -> x.handle(this, event));
	}


	@Override
	public void mouseMoved(MouseEvent event) {
		onMouseMoved.values().forEach(x -> x.handle(this, event));
	}


	@Override
	public void mousePressed(MouseEvent event) {
		onMousePressed.values().forEach(x -> x.handle(this, event));
	}


	@Override
	public void mouseDragged(MouseEvent event) {
		onMouseDragged.values().forEach(x -> x.handle(this, event));
	}


	@Override
	public void mouseEntered(MouseEvent event) {
		onMouseEntered.values().forEach(x -> x.handle(this, event));
	}


	@Override
	public void mouseExited(MouseEvent event) {
		onMouseExited.values().forEach(x -> x.handle(this, event));
	}


	@Override
	public void mouseReleased(MouseEvent event) {
		onMouseReleased.values().forEach(x -> x.handle(this, event));
	}


	@Override
	public void mouseWheel(MouseEvent event) {
		onMouseWheel.values().forEach(x -> x.handle(this, event));
	}


	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		onMouseWheelMoved.values().forEach(x -> x.handle(this, e));
	}


	@Override
	public void keyPressed(KeyEvent event) {
		onKeyPressed.values().forEach(x -> x.handle(this, event));

	}

	@Override
	public void keyReleased(KeyEvent event) {
		onKeyReleased.values().forEach(x -> x.handle(this, event));
	}

	public int getDmouseX() {
		return dmouseX;
	}

	public int getDmouseY() {
		return dmouseY;
	}

	public int getEmouseX() {
		return emouseX;
	}

	public int getEmouseY() {
		return emouseY;
	}
}
