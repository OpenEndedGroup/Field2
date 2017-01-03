// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.OS;
import org.cef.handler.CefClientHandler;
import org.cef.handler.CefRenderHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * This class represents a windowed rendered browser. The visibility of this class is "package". To create a new CefBrowser instance, please use
 * CefBrowserFactory.
 */
class CefRendererBrowserAWT extends CefBrowser_N {
	private Canvas canvas_ = null;
	private long window_handle_ = 0;
	private CefClientHandler clientHandler_;
	private String url_;
	private CefRequestContext context_;
	private CefRendererBrowserAWT parent_ = null;
	private CefRendererBrowserAWT devTools_ = null;

	CefRendererBrowserAWT(CefClientHandler clientHandler, String url, Boolean isTransparent, CefRequestContext context) {
		this(clientHandler, url, context, null);
	}

	@SuppressWarnings("serial")
	private CefRendererBrowserAWT(CefClientHandler clientHandler, String url, CefRequestContext context, CefRendererBrowserAWT parent) {
		super();
		clientHandler_ = clientHandler;
		url_ = url;
		context_ = context;
		parent_ = parent;

		// Disabling lightweight of popup menuSpecs is required because
		// otherwise it will be displayed behind the content of canvas_
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

		canvas_ = new Canvas() {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				super.setBounds(x, y, width, height);
				wasResized(width, height);
			}

			@Override
			public void setBounds(Rectangle r) {
				setBounds(r.x, r.y, r.width, r.height);
			}

			@Override
			public void setSize(int width, int height) {
				super.setSize(width, height);
				wasResized(width, height);
			}

			@Override
			public void setSize(Dimension d) {
				setSize(d.width, d.height);
			}

			@Override
			public void paint(Graphics g) {
				if (getNativeRef("CefBrowser") == 0) {
					if (parent_ != null) {
						createDevTools(parent_, clientHandler_, getWindowHandle(), false, canvas_);
					} else {
						createBrowser(clientHandler_, getWindowHandle(), url_, false, canvas_, context_);
					}
				}
			}

			@Override
			public void removeNotify() {
				parentWindowWillClose();
				super.removeNotify();
			}
		};
		canvas_.setFocusable(true);
		canvas_.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				setFocus(false);
			}

			@Override
			public void focusGained(FocusEvent e) {
				setFocus(true);
			}
		});
	}

	@Override
	public Component getUIComponent() {
		return canvas_;
	}

	@Override
	public CefRenderHandler getRenderHandler() {
		return null;
	}

	@Override
	public synchronized void close() {
		if (context_ != null) context_.dispose();
		if (parent_ != null) {
			parent_.closeDevTools();
			parent_.devTools_ = null;
			parent_ = null;
		}
		super.close();
	}

	@Override
	public synchronized CefBrowser getDevTools() {
		if (devTools_ == null) devTools_ = new CefRendererBrowserAWT(clientHandler_, url_, context_, this);
		return devTools_;
	}

	private long getWindowHandle() {
		if (window_handle_ == 0 && OS.isMacintosh()) {
			try {
				Class<?> cls = Class.forName("org.cef.browser.mac.CefBrowserWindowMac");
				CefBrowserWindow browserWindow = (CefBrowserWindow) cls.newInstance();
				if (browserWindow != null) {
					window_handle_ = browserWindow.getWindowHandleOfCanvas(canvas_);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return window_handle_;
	}
}
