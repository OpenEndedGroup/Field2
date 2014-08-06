package fieldcef.tests;

import field.utility.Log;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandler;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Singleton class, has all of the CEF singleton stuff hidden inside.
 */
public class CefSystem {

	static public CefSystem cefSystem = new CefSystem();
	private final CefApp cefApp;
	private final CefClient client;
	private final CefMessageRouter router;

	protected CefSystem()
	{
		cefApp = CefApp.getInstance(new String[]{"--off-screen-rendering-mode-enabled"});

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.err.println(" disposing...");
			cefApp.dispose();
			System.err.println(" disposed ");
		}));

		client = cefApp.createClient();
		client.addDisplayHandler(new CefDisplayHandler() {
			@Override
			public void onAddressChange(CefBrowser browser, String url) {
				Log.log("cef.debug", "Address change :" + browser + " -> " + url);
			}

			@Override
			public void onTitleChange(CefBrowser browser, String title) {
				Log.log("cef.debug", "Title change :" + browser + " -> " + title);
			}

			@Override
			public boolean onTooltip(CefBrowser browser, String text) {
				return false;
			}

			@Override
			public void onStatusMessage(CefBrowser browser, String value) {
				Log.log("cef.debug", "Status change :" + browser + " -> " + value);

			}

			@Override
			public boolean onConsoleMessage(CefBrowser browser, String message, String source, int line) {
				Log.log("cef.console", " CONSOLE :" + browser + " " + message + " " + source + " " + line);
				return false;
			}
		});

		client.addLoadHandler(new CefLoadHandlerAdapter() {
			@Override
			public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
				Log.log("cef.debug", "state change :" + browser + " -> " + isLoading+" "+canGoBack+" "+canGoForward);
			}

			@Override
			public void onLoadStart(CefBrowser browser, int frameIdentifer) {
				Log.log("cef.debug", "load start:" + browser + " -> " + frameIdentifer);
			}

			@Override
			public void onLoadEnd(CefBrowser browser, int frameIdentifier, int httpStatusCode) {
				Log.log("cef.debug", "load end:" + browser + " -> " + frameIdentifier);
			}

			@Override
			public void onLoadError(CefBrowser browser, int frameIdentifer, ErrorCode errorCode, String errorText, String failedUrl) {
				Log.log("cef.error", "load error:" + browser + " -> " + frameIdentifer + " " + errorCode + " " + errorText + " " + failedUrl);
			}
		});

		router = CefMessageRouter.create();
		client.addMessageRouter(router);

		router.addHandler(new CefMessageRouterHandler() {
			@Override
			public boolean onQuery(CefBrowser browser, long query_id, String request, boolean persistent, CefQueryCallback callback) {
				Log.log("cef.debug", " -- query :" + request + " " + callback + " " + query_id);
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
	}

	public interface PaintCallback
	{
		public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height);

	}

	public CefRendererBrowserBuffer makeBrowser(int w, int h, PaintCallback callback)
	{
		CefRenderer cefRenderer = new CefRenderer() {
			@Override
			public void render() {
				System.out.println("render?");
			}

			@Override
			public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
				Log.log("cef.debug", "Painting rectangles " + dirtyRects.length + " " + Arrays.asList(dirtyRects) + " -> " + buffer);
				callback.onPaint(popup, dirtyRects, buffer, width, height);
			}
		};
//		CefRendererBrowserBuffer browser = (CefRendererBrowserBuffer) client
//			    .createBrowser("http://www.w3.org/2002/09/tests/keys.html", true, CefBrowserFactory.RenderType.RENDER_BYTE_BUFFER, null, w, h, cefRenderer);
		CefRendererBrowserBuffer browser = (CefRendererBrowserBuffer) client
			    .createBrowser("about:blank", true, CefBrowserFactory.RenderType.RENDER_BYTE_BUFFER, null, w, h, cefRenderer);

		return browser;
	}



}
