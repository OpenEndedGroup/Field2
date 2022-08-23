package fieldcef.browser

import com.google.common.collect.MapMaker
import com.jetbrains.cef.JCefAppConfig
import field.app.RunLoop
import field.utility.Log
import field.utility.SimpleCommand
import org.cef.CefApp
import org.cef.CefApp.CefAppState
import org.cef.CefClient
import org.cef.CefSettings.LogSeverity
import org.cef.browser.*
import org.cef.callback.CefDragData
import org.cef.callback.CefMediaAccessCallback
import org.cef.callback.CefQueryCallback
import org.cef.handler.*
import org.cef.network.CefRequest.TransitionType
import java.awt.Point
import java.awt.Rectangle
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.function.Consumer

/**
 * Singleton class, has all of the CEF singleton stuff hidden inside.
 */
class CefSystem protected constructor() {
    private var mainHandler: CefMessageRouterHandler
    private val cefApp: CefApp
    private val client: CefClient
    private val router: CefMessageRouter

    fun interface PaintCallback {
        fun onPaint(popup: Boolean, dirtyRects: Array<Rectangle>?, buffer: ByteBuffer?, width: Int, height: Int)
    }

    fun interface MessageCallback {
        fun message(id: Long, message: String?, answer: Consumer<String?>?)
    }

    var callbacks: MutableMap<CefBrowser, MessageCallback> = MapMaker().weakKeys().makeMap()
    var completionCallbacks: MutableMap<CefBrowser, Runnable> = MapMaker().weakKeys().makeMap()
    fun makeBrowser(
        w: Int,
        h: Int,
        callback: PaintCallback,
        message: MessageCallback,
        completionCallback: Runnable
    ): CefBrowserOsrWithHandler {
//		CefRenderer cefRenderer = new CefRenderer(false) {
//
//
//			@Override
//			public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
//				callback.onPaint(popup, dirtyRects, buffer, width, height);
//			}
//		};
//		CefRendererBrowserBuffer browser  = (CefRendererBrowserBuffer) CefBrowserFactory.create(client, null, CefRendering.BYTEBUFFER, false, null, w, h, cefRenderer);
        val r = Rectangle(0, 0, w, h)
        val browser = CefBrowserOsrWithHandler(
            client,
            null,
            CefRequestContext.getGlobalContext(),
            object : CefRenderHandlerAdapter() {
                override fun getViewRect(browser: CefBrowser): Rectangle {
//                    println(" ** getViewRect $browser ** ")
                    //				return super.getViewRect(browser);
                    return r
                }

                override fun getScreenPoint(browser: CefBrowser, viewPoint: Point): Point {
//                    println(" ** getScreenPoint $browser $viewPoint ** ")
                    return super.getScreenPoint(browser, viewPoint)
                }

                override fun getDeviceScaleFactor(browser: CefBrowser): Double {
                    println(" ** getDeviceScaleFactor ** ")
                    return 1.0
                }

                override fun onPopupShow(browser: CefBrowser, show: Boolean) {
                    println(" ** onPopupShow ** ")
                    super.onPopupShow(browser, show)
                }

                override fun onPopupSize(browser: CefBrowser, size: Rectangle) {
                    println(" ** onPopupSize ** ")
                    super.onPopupSize(browser, size)
                }

                override fun onPaint(
                    browser: CefBrowser,
                    popup: Boolean,
                    dirtyRects: Array<Rectangle>,
                    buffer: ByteBuffer,
                    width: Int,
                    height: Int
                ) {
//                    println(" ** onPaint!! ** ")
                    callback.onPaint(popup, dirtyRects, buffer, width, height)
                }

                override fun startDragging(
                    browser: CefBrowser,
                    dragData: CefDragData,
                    mask: Int,
                    x: Int,
                    y: Int
                ): Boolean {
//                    println(" ** startDragging ** ")
                    return super.startDragging(browser, dragData, mask, x, y)
                }

                override fun updateDragCursor(browser: CefBrowser, operation: Int) {
//                    println(" ** updateDragCursor ** ")
                    super.updateDragCursor(browser, operation)
                }
            })
        browser.createImmediately()
        callbacks[browser] = message
        completionCallbacks[browser] = completionCallback
        return browser
    }

    inner class GetOwnPid {
        val pid: Long
            get() = ProcessHandle.current().pid()
    }

    companion object {
        @JvmField
        var cefSystem = CefSystem()
    }

    init {
        CefApp.startup(arrayOf())
        val config = JCefAppConfig.getInstance()
        val appArgs: MutableList<String?> = ArrayList<String?>()
        appArgs.addAll(config.appArgsAsList)
        appArgs.add("--no-sandbox")
        appArgs.add("--enable-media-stream")
        appArgs.add("--use-fake-ui-for-media-stream")
        appArgs.add("--disable-background-timer-throttling")
        appArgs.add("--disable-site-isolation-trials")
        appArgs.add("--single-process")



//                appArgs.add("--multi-threaded-message-loop");
//        appArgs.add("--external-message-pump");
        println(" args $appArgs")
        val args = appArgs.toTypedArray()
        CefApp.addAppHandler(object : CefAppHandlerAdapter(args) {
            override fun stateHasChanged(state: CefAppState) {
                println(" ** state has changed $state **")
                // Shutdown the app if the native CEF part is terminated
                if (state == CefAppState.TERMINATED) System.exit(0)
            }
        })
        val settings = config.cefSettings
        settings.remote_debugging_port = 10560
        settings.log_severity = LogSeverity.LOGSEVERITY_ERROR
        settings.windowless_rendering_enabled = true
        cefApp = CefApp.getInstance(settings)
        Runtime.getRuntime().addShutdownHook(Thread {
            println(" -- sleeping for 2 seconds, then killing")
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            val pid = GetOwnPid().pid
            System.err.println(" pid is :$pid")
            try {
                SimpleCommand.go(File("."), "/bin/kill", "-9", "" + pid)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        })

        println(" -- creating router for CEFSYSTEM")
        router = CefMessageRouter.create();

//        var l = System.currentTimeMillis()
//        RunLoop.main.getLoop().attach("__cefSystemMessageloop__") { i ->
//            print(System.currentTimeMillis() - l)
////            if (System.currentTimeMillis() - l > 5000)
////                cefApp.doMessageLoopWork(0);
//            true
//        };


        client = cefApp.createClient()
        client.addMessageRouter(router)
        client.addLifeSpanHandler(object : CefLifeSpanHandler {
            override fun onBeforePopup(
                browser: CefBrowser,
                frame: CefFrame,
                target_url: String,
                target_frame_name: String
            ): Boolean {
                return false
            }

            override fun onAfterCreated(browser: CefBrowser) {
                Log.log("cef.debug") { "afterCreated $browser" }
            }

            override fun onAfterParentChanged(browser: CefBrowser) {}
            override fun doClose(browser: CefBrowser): Boolean {
                return true
            }

            override fun onBeforeClose(browser: CefBrowser) {
                Log.log("cef.debug") { "beforeClose $browser" }
            }
        })
        client.addDisplayHandler(object : CefDisplayHandler {
            override fun onAddressChange(browser: CefBrowser, frame: CefFrame, url: String) {
                Log.log("cef.debug") { "Address change :$browser -> $url" }
            }

            override fun onTitleChange(browser: CefBrowser, title: String) {
                Log.log("cef.debug") { "Title change :$browser -> $title" }
            }

            override fun onTooltip(browser: CefBrowser, text: String): Boolean {
                return false
            }

            override fun onStatusMessage(browser: CefBrowser, value: String) {
//				Log.log("cef.debug", "Status change :" + browser + " -> " + value);
            }

            override fun onConsoleMessage(
                browser: CefBrowser,
                level: LogSeverity,
                message: String,
                source: String,
                line: Int
            ): Boolean {
//				Log.log("cef.console", () -> " CONSOLE :" + browser + " " + message + " " + source + " " + line);
//                if (level.ordinal>=LogSeverity.LOGSEVERITY_ERROR.ordinal)
                println(" console message [$browser] $level $message $source $line")

                client.removeMessageRouter(router)
                client.addMessageRouter(router)
                return false
            }

            override fun onCursorChange(cefBrowser: CefBrowser, i: Int): Boolean {
                return false
            }
        })
        client.addMediaAccessHandler(object : CefMediaAccessHandler {
            override fun onRequestMediaAccessPermission(
                p0: CefBrowser?,
                p1: CefFrame?,
                p2: String?,
                p3: Int,
                p4: CefMediaAccessCallback?
            ): Boolean {
                if (p4 != null)
                    p4.Continue(15)
                return true
            }
        })
        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                browser: CefBrowser,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                println(" ** loading state change **")
                Log.log("cef.debug") { "state change :$browser -> $isLoading $canGoBack $canGoForward" }
            }

            override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: TransitionType) {
                println(" ** loading state onLoadStart **")
            }

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                println(" ** loading state onLoadEnd **")
            }

            override fun onLoadError(
                browser: CefBrowser,
                frame: CefFrame,
                errorCode: CefLoadHandler.ErrorCode,
                errorText: String,
                failedUrl: String
            ) {
                println(" ** loading state onLoadError **")
            } //			@Override
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
        })

        mainHandler = object : CefMessageRouterHandler {
            override fun onQuery(
                browser: CefBrowser,
                frame: CefFrame,
                queryId: Long,
                request: String,
                persistent: Boolean,
                callback: CefQueryCallback
            ): Boolean {
                Log.log("cef.debug") { " -- query :$request $callback $queryId" }
                System.err.println(" internal browser message $request $callback $queryId")
                val m = callbacks[browser]
                if (m != null) {
                    m.message(queryId, request, Consumer { x: String? -> callback.success(x) })
                } else {
                    Log.log("cef.error") { "No message handler" }
                    callback.failure(0, "No message handler")
                    System.out.println(" - can't find callback")
                }
                return true
            }

            override fun onQueryCanceled(browser: CefBrowser, frame: CefFrame, queryId: Long) {}
            override fun setNativeRef(identifer: String, nativeRef: Long) {}
            override fun getNativeRef(identifer: String): Long {
                return 0
            }
        }
        router.addHandler(mainHandler, true)

//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
    }
}