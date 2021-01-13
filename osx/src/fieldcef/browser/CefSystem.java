package fieldcef.browser;

import com.google.common.collect.MapMaker;
import com.jetbrains.cef.JCefAppConfig;
import field.utility.Log;
import field.utility.SimpleCommand;
import fieldagent.Trampoline;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.SystemBootstrap;
import org.cef.browser.*;
import org.cef.callback.CefDragData;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.*;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static org.cef.CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;

/**
 * Singleton class, has all of the CEF singleton stuff hidden inside.
 */
public class CefSystem {

	static public CefSystem cefSystem = new CefSystem();
	private final CefApp cefApp;
	private final CefClient client;
	private final CefMessageRouter router;

	protected CefSystem() {
		CefApp.startup(new String[]{});
		JCefAppConfig config = JCefAppConfig.getInstance();
		List<String> appArgs = new ArrayList();
		appArgs.addAll(config.getAppArgsAsList());
		String[] args = appArgs.toArray(new String[0]);


		CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
			@Override
			public void stateHasChanged(org.cef.CefApp.CefAppState state) {
				System.out.println(" ** state has changed "+state+" **");
				// Shutdown the app if the native CEF part is terminated
				if (state == CefApp.CefAppState.TERMINATED) System.exit(0);
			}
		});

		CefSettings settings = config.getCefSettings();
		settings.log_severity = LOGSEVERITY_VERBOSE;
		settings.windowless_rendering_enabled = true;
		cefApp = CefApp.getInstance(settings);


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
			public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
				return false;
			}

			@Override
			public void onAfterCreated(CefBrowser browser) {
				Log.log("cef.debug", () -> "afterCreated " + browser);
			}

			@Override
			public void onAfterParentChanged(CefBrowser browser) {

			}

			@Override
			public boolean doClose(CefBrowser browser) {
				return true;
			}

			@Override
			public void onBeforeClose(CefBrowser browser) {
				Log.log("cef.debug", () -> "beforeClose " + browser);
			}
		});

		client.addDisplayHandler(new CefDisplayHandler() {

			@Override
			public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
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
			public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
//				Log.log("cef.console", () -> " CONSOLE :" + browser + " " + message + " " + source + " " + line);
				System.out.println(" console message ["+browser+"] "+level+" "+message+" "+source+" "+line);
				return false;
			}

		});

		client.addLoadHandler(new CefLoadHandlerAdapter() {
			@Override
			public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
				Log.log("cef.debug", () -> "state change :" + browser + " -> " + isLoading + " " + canGoBack + " " + canGoForward);
			}

//			@Override
//			public void onLoadStart(CefBrowser browser, int frameIdentifer) {
//				Log.log("cef.debug", () -> "load start:" + browser + " -> " + frameIdentifer);
//			}


//			@Override
//			public void onLoadEnd(CefBrowser browser, int frameIdentifier, int httpStatusCode) {
//				Log.log("cef.debug", () -> "load end:" + browser + " -> " + frameIdentifier);
//				Runnable r = completionCallbacks.get(browser);
//				if (r != null)
//					r.run();
//			}

//			@Override
//			public void onLoadError(CefBrowser browser, int frameIdentifer, ErrorCode errorCode, String errorText, String failedUrl) {
//				Log.log("cef.error", () -> "load error:" + browser + " -> " + frameIdentifer + " " + errorCode + " " + errorText + " " + failedUrl);
//			}
		});

		router = CefMessageRouter.create();
		client.addMessageRouter(router);

		router.addHandler(new CefMessageRouterHandler() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
				Log.log("cef.debug", () -> " -- query :" + request + " " + callback + " " + queryId);
				MessageCallback m = callbacks.get(browser);
				if (m != null) {
					m.message(queryId, request, x -> callback.success(x));
				} else {
					Log.log("cef.error", () -> "No message handler");
					callback.failure(0, "No message handler");
				}
				return true;			}

			@Override
			public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {

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

	public CefBrowserOsrWithHandler makeBrowser(int w, int h, PaintCallback callback, MessageCallback message, Runnable completionCallback) {
//		CefRenderer cefRenderer = new CefRenderer(false) {
//
//
//			@Override
//			public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
//				callback.onPaint(popup, dirtyRects, buffer, width, height);
//			}
//		};
//		CefRendererBrowserBuffer browser  = (CefRendererBrowserBuffer) CefBrowserFactory.create(client, null, CefRendering.BYTEBUFFER, false, null, w, h, cefRenderer);

		Rectangle r = new Rectangle(0,0, w, h);
		CefBrowserOsrWithHandler browser =  new CefBrowserOsrWithHandler(client, null, null, new CefRenderHandlerAdapter() {
			@Override
			public void onCursorChange(CefBrowser browser, int cursorIdentifer) {
				System.out.println(" ** oncursorchange "+browser+" "+cursorIdentifer+" ** ");
				super.onCursorChange(browser, cursorIdentifer);
			}

			@Override
			public Rectangle getViewRect(CefBrowser browser) {
				System.out.println(" ** getViewRect "+browser+" ** ");
//				return super.getViewRect(browser);
				return r;
			}

			@Override
			public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
				System.out.println(" ** getScreenPoint "+browser+" "+viewPoint+" ** ");
				return super.getScreenPoint(browser, viewPoint);
			}

			@Override
			public double getDeviceScaleFactor(CefBrowser browser) {
				System.out.println(" ** getDeviceScaleFactor ** ");
				return 1.0;
			}

			@Override
			public void onPopupShow(CefBrowser browser, boolean show) {
				System.out.println(" ** onPopupShow ** ");
				super.onPopupShow(browser, show);
			}

			@Override
			public void onPopupSize(CefBrowser browser, Rectangle size) {
				System.out.println(" ** onPopupSize ** ");
				super.onPopupSize(browser, size);
			}

			@Override
			public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
				System.out.println(" ** onPaint!! ** ");
				callback.onPaint(popup, dirtyRects, buffer, width, height);
			}

			@Override
			public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
				System.out.println(" ** startDragging ** ");
				return super.startDragging(browser, dragData, mask, x, y);
			}

			@Override
			public void updateDragCursor(CefBrowser browser, int operation) {
				System.out.println(" ** updateDragCursor ** ");
				super.updateDragCursor(browser, operation);
			}
		});


		callbacks.put(browser, message);
		completionCallbacks.put(browser, completionCallback);


		return browser;
	}

	public class GetOwnPid {

		public long getPid() {
			return ProcessHandle.current().pid();
		}

	}


}
