// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.simple;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandler;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Trying to get this to be headless but currently it requires that a JFrame is created. Maybe the C++ native code requires the Window.
 */
public class MainFrameNearlyHeadless /*extends JFrame*/ {
	private static final long serialVersionUID = -5570653778104413836L;

	private final CefApp cefApp_;
	private final CefClient client_;
	private final CefRendererBrowserBuffer browser_;
	private final Component browserUI_;

	private MainFrameNearlyHeadless(String startURL, boolean isTransparent) {
		Toolkit.getDefaultToolkit();


		cefApp_ = CefApp.getInstance();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.err.println(" disposing...");
			cefApp_.dispose();
			System.err.println(" disposed ");
		}));

		client_ = cefApp_.createClient();
		Toolkit.getDefaultToolkit();
		CefRenderer cefRenderer = new CefRenderer() {
			@Override
			public void render() {
				System.out.println("render");
			}

			@Override
			public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
				System.out.println("Painting rectangles " + dirtyRects.length+" "+ Arrays.asList(dirtyRects)+" -> "+buffer);
				int q = 0;
				for(int i=0;i<buffer.capacity();i++)
				{
					int z = buffer.get(i) & 0xff;
					if (z!=0)
						System.out.println(z);
				}
			}
		};
		browser_ = (CefRendererBrowserBuffer) client_
			    .createBrowser(startURL, isTransparent, CefBrowserFactory.RenderType.RENDER_BYTE_BUFFER, null, 800, 600, cefRenderer);
		browserUI_ = browser_.getUIComponent();
		client_.addDisplayHandler(new CefDisplayHandler() {
			@Override
			public void onAddressChange(CefBrowser browser, String url) {

			}

			@Override
			public void onTitleChange(CefBrowser browser, String title) {

			}

			@Override
			public boolean onTooltip(CefBrowser browser, String text) {
				return false;
			}

			@Override
			public void onStatusMessage(CefBrowser browser, String value) {

			}

			@Override
			public boolean onConsoleMessage(CefBrowser browser, String message, String source, int line) {
				System.out.println(" CONSOLE :" + browser + " " + message + " " + source + " " + line);
				return false;
			}
		});

		System.out.println(" -- pausing --- ");

		CefMessageRouter router = CefMessageRouter.create();
		client_.addMessageRouter(router);

		router.addHandler(new CefMessageRouterHandler() {
			@Override
			public boolean onQuery(CefBrowser browser, long query_id, String request, boolean persistent, CefQueryCallback callback) {
				System.out.println(" -- query :"+request+" "+callback+" "+query_id);
				callback.success("PEACH");
				return true;
			}

			@Override
			public void onQueryCanceled(CefBrowser browser, long query_id) {

			}

			@Override
			public void setNativeRef(String identifer, long nativeRef) {

			}

			@Override
			public long getNativeRef(String identifer) {
				return 0;
			}
		}, true);

		try {
			Thread.sleep(1000);
		}
		catch(Throwable t)
		{
			t.printStackTrace();;
		}

		browser_.executeJavaScript("var request_id = window.cefQuery({\n" +
					"    request: 'my_request',\n" +
					"    persistent: false,\n" +
					"    onSuccess: function(response) {console.log('success'+response);},\n" +
					"    onFailure: function(error_code, error_message) {console.log('fail', error_code, error_message);}\n" +
					"});", "", 0);

		client_.addLoadHandler(new CefLoadHandlerAdapter() {
			@Override
			public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
				System.out.println(" OLSC " + browser + " " + isLoading + " " + canGoBack + " " + canGoForward);
			}

			@Override
			public void onLoadStart(CefBrowser browser, int frameIdentifer) {
				System.out.println(" OLS " + browser + " " + frameIdentifer);
			}

			@Override
			public void onLoadEnd(CefBrowser browser, int frameIdentifier, int httpStatusCode) {
				System.out.println(" OLE " + browser + " " + frameIdentifier + " " + httpStatusCode);
				browser_.executeJavaScript("console.log('BANANA')", "", 0);
			}

			@Override
			public void onLoadError(CefBrowser browser, int frameIdentifer, ErrorCode errorCode, String errorText, String failedUrl) {
				System.out.println(" OLEr " + browser + " " + frameIdentifer + " " + errorCode + " " + errorText + " " + failedUrl);
			}
		});


		if (true)return;

		System.out.println(" -- pausing --- ");
		try {
			Thread.sleep(5000);
		}
		catch(Throwable t)
		{
			t.printStackTrace();;
		}




		System.out.println(" -- mousing around --- ");

		for(int y=50;y<500;y+=10)
		{
			System.out.println(" - click :"+y);
			browser_.sendMouseEvent(new MouseEvent(new Component() {
				@Override
				public String getName() {
					return super.getName();
				}

				@Override
				public Point getLocationOnScreen() {
					return new Point(0, 0);
				}
			}, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1_MASK, 400, y, 1, false));
			browser_.sendMouseEvent(new MouseEvent(new Component() {
				@Override
				public String getName() {
					return super.getName();
				}

				@Override
				public Point getLocationOnScreen() {
					return new Point(0, 0);
				}
			}, MouseEvent.MOUSE_RELEASED, 0, MouseEvent.BUTTON1_MASK, 400, y, 1, false));
			try {
				Thread.sleep(500);
			}
			catch(Throwable t)
			{
				t.printStackTrace();;
			}
		}

	}

	public static void main(String[] args) {
		new MainFrameNearlyHeadless("http://www.tictocfamily.com/latest/320-tictoc-appointed-to-develop-a-new-site-for-maven-capital-partners", true);
	}
}
