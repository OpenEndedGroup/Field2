package fieldbox.ui;

import field.graphics.GlfwCallback;

/**
 * Created by marc on 4/14/14.
 */
public class GlfwCallbackDelegate extends GlfwCallback {
	private final GlfwCallback delegate;

	public GlfwCallbackDelegate(GlfwCallback glfwCallback) {
		delegate = glfwCallback;
	}

	@Override
	public void error(int i, String s) {
		delegate.error(i, s);
	}

	@Override
	public void monitor(long l, boolean b) {
		delegate.monitor(l, b);
	}

	@Override
	public void windowPos(long l, int i, int i2) {
		delegate.windowPos(l, i, i2);
	}

	@Override
	public void windowSize(long l, int i, int i2) {
		delegate.windowSize(l, i, i2);
	}

	@Override
	public boolean windowClose(long l) {
		return delegate.windowClose(l);
	}

	@Override
	public void windowRefresh(long l) {
		delegate.windowRefresh(l);
	}

	@Override
	public void windowFocus(long l, boolean b) {
		delegate.windowFocus(l, b);
	}

	@Override
	public void windowIconify(long l, boolean b) {
		delegate.windowIconify(l, b);
	}

	@Override
	public void key(long l, int i, int i2, int i3, int i4) {
		delegate.key(l, i, i2, i3, i4);
	}

	@Override
	public void character(long l, int c) {
		delegate.character(l, c);
	}

	@Override
	public void mouseButton(long l, int i, int b, int i2) {
		delegate.mouseButton(l, i, b, i2);
	}

	@Override
	public void cursorPos(long l, double v, double v2) {
		delegate.cursorPos(l, v, v2);
	}

	@Override
	public void cursorEnter(long l, boolean b) {
		delegate.cursorEnter(l, b);
	}

	@Override
	public void scroll(long l, double v, double v2) {
		delegate.scroll(l, v, v2);
	}

	@Override
	public void drop(long l, String[] strings)
	{
		delegate.drop(l, strings);
	}

	@Override
	public void framebufferSize(long window, int width, int height) {
		delegate.framebufferSize(window, width, height);
	}
}
