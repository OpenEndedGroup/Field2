package fieldcef.browser;

import com.google.common.collect.MapMaker;
import field.utility.Log;
import field.utility.SimpleCommand;
import fieldagent.Trampoline;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefLifeSpanHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandler;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Singleton class, has all of the CEF singleton stuff hidden inside.
 */
public class CefSystem {

	static public CefSystem cefSystem = new CefSystem();
	private final CefApp cefApp;
	private final CefClient client;
	private final CefMessageRouter router;

	protected CefSystem() {
		cefApp = CefApp.getInstance(new String[]{"--overlay-scrollbars", "--off-screen-rendering-mode-enabled", "--enable-experimental-web-platform-features"});


		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println(" -- sleeping for 2 seconds, then killing");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long pid = new GetOwnPid().getPid();
			System.err.println(" pid is :" + pid);
			try {
				SimpleCommand.go(new File("."), "/bin/kill", "-9", "" + pid);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}));

		client = cefApp.createClient();
		client.addLifeSpanHandler(new CefLifeSpanHandler() {
			@Override
			public boolean onBeforePopup(CefBrowser browser, String target_url, String target_frame_name) {
				return false;
			}

			@Override
			public void onAfterCreated(CefBrowser browser) {
				Log.log("cef.debug", () -> "afterCreated " + browser);
			}

			@Override
			public boolean runModal(CefBrowser browser) {
				return true;
			}

			@Override
			public boolean doClose(CefBrowser browser) {
				System.out.println("CEF : doclose");
				return true;
			}

			@Override
			public void onBeforeClose(CefBrowser browser) {
				System.out.println("CEF : beforedoclose");
				Log.log("cef.debug", () -> "beforeClose " + browser);
			}
		});

		client.addDisplayHandler(new CefDisplayHandler() {
			@Override
			public void onAddressChange(CefBrowser browser, String url) {
				Log.log("cef.debug", () -> "Address change :" + browser + " -> " + url);
			}

			@Override
			public void onTitleChange(CefBrowser browser, String title) {
				Log.log("cef.debug", () -> "Title change :" + browser + " -> " + title);
			}

			@Override
			public boolean onTooltip(CefBrowser browser, String text) {
				return false;
			}

			@Override
			public void onStatusMessage(CefBrowser browser, String value) {
//				Log.log("cef.debug", "Status change :" + browser + " -> " + value);

			}

			@Override
			public boolean onConsoleMessage(CefBrowser browser, String message, String source, int line) {
//				Log.log("cef.console", " CONSOLE :" + browser + " " + message + " " + source + " " + line);
				return false;
			}
		});

		client.addLoadHandler(new CefLoadHandlerAdapter() {
			@Override
			public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
				Log.log("cef.debug", () -> "state change :" + browser + " -> " + isLoading + " " + canGoBack + " " + canGoForward);
			}

			@Override
			public void onLoadStart(CefBrowser browser, int frameIdentifer) {
				Log.log("cef.debug", () -> "load start:" + browser + " -> " + frameIdentifer);
			}


			@Override
			public void onLoadEnd(CefBrowser browser, int frameIdentifier, int httpStatusCode) {
				Log.log("cef.debug", () -> "load end:" + browser + " -> " + frameIdentifier);
				Runnable r = completionCallbacks.get(browser);
				if (r != null)
					r.run();
			}

			@Override
			public void onLoadError(CefBrowser browser, int frameIdentifer, ErrorCode errorCode, String errorText, String failedUrl) {
				Log.log("cef.error", () -> "load error:" + browser + " -> " + frameIdentifer + " " + errorCode + " " + errorText + " " + failedUrl);
			}
		});

		router = CefMessageRouter.create();
		client.addMessageRouter(router);

		router.addHandler(new CefMessageRouterHandler() {
			@Override
			public boolean onQuery(CefBrowser browser, long query_id, String request, boolean persistent, CefQueryCallback callback) {
				Log.log("cef.debug", () -> " -- query :" + request + " " + callback + " " + query_id);
				MessageCallback m = callbacks.get(browser);
				if (m != null) {
					m.message(query_id, request, x -> callback.success(x));
				} else {
					Log.log("cef.error", () -> "No message handler");
					callback.failure(0, "No message handler");
				}
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

//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	public interface PaintCallback {
		void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height);

	}

	public interface MessageCallback {
		void message(long id, String message, Consumer<String> answer);
	}

	Map<CefBrowser, MessageCallback> callbacks = new MapMaker().weakKeys().makeMap();
	Map<CefBrowser, Runnable> completionCallbacks = new MapMaker().weakKeys().makeMap();

	public CefRendererBrowserBuffer makeBrowser(int w, int h, PaintCallback callback, MessageCallback message, Runnable completionCallback) {
		CefRenderer cefRenderer = new CefRenderer() {
			@Override
			public void render() {

			}

			@Override
			public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
				callback.onPaint(popup, dirtyRects, buffer, width, height);
			}
		};
		CefRendererBrowserBuffer browser = (CefRendererBrowserBuffer) client
			.createBrowser(null, true, CefBrowserFactory.RenderType.RENDER_BYTE_BUFFER, null, w, h, cefRenderer);

		callbacks.put(browser, message);
		completionCallbacks.put(browser, completionCallback);


		return browser;
	}

	public class GetOwnPid {

		public long getPid() {
			return ProcessHandle.current().getPid();
		}

	}


}
