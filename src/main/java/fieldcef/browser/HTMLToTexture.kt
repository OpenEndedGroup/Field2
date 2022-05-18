package fieldcef.browser

import field.graphics.Scene.Perform
import field.graphics.Texture
import field.utility.Ports
import fielded.webserver.NanoHTTPD
import fielded.webserver.Server
import org.cef.browser.CefBrowser
import java.awt.Rectangle
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * Created by marc on 9/20/2016.
 */
class HTMLToTexture(private val w: Int, private val h: Int) {
    private val sourceView: MutableMap<String, ByteBuffer> = LinkedHashMap()
    private val callback: MutableMap<String, Runnable> = LinkedHashMap()
    private var s: Server? = null
    private val a: Int
    private val b: Int
    private val browser: CefBrowser

    @Transient
    var callbackOnNextReload: Runnable? = null
    var textTable: MutableMap<String, String> = object : LinkedHashMap<String, String>() {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>): Boolean {
            return size > 100
        }
    }
    var pendingRequests: MutableList<Runnable> = ArrayList()

    @Transient
    var requestDone = true
    var mod = AtomicInteger(0)
    private var timeSinceLastLoad: Long = 0
    fun renderURL(url: String): Future<ByteBuffer?> {
        val f = CompletableFuture<ByteBuffer?>()
        pendingRequests.add(Runnable {
            sourceView[url] = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder())
            callback[url] = Runnable {
                f.complete(sourceView[url])
                requestDone = true
            }
            callbackOnNextReload = Runnable {}
            timeSinceLastLoad = System.currentTimeMillis()

            println(" -- html_to_texture is asking for a browser load url to $url")
            browser.loadURL(url)

        })
        checkAndRunRequests()
        return f
    }

    fun renderURL_setCallback(url: String, update: (ByteBuffer) -> Unit): Future<ByteBuffer?> {
        val f = CompletableFuture<ByteBuffer?>()
        pendingRequests.add(Runnable {
            sourceView[url] = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder())
            callback[url] = Runnable {
                f.complete(sourceView[url])
                requestDone = true
                update(sourceView[url]!!)
            }
            callbackOnNextReload = Runnable {}
            timeSinceLastLoad = System.currentTimeMillis()

            println(" -- html_to_texture is asking for a browser load url to $url")
            browser.loadURL(url)

        })
        checkAndRunRequests()
        return f
    }

    fun renderText(text: String): Future<ByteBuffer?> {
        val f = CompletableFuture<ByteBuffer?>()
        pendingRequests.add(Runnable {
            val res = UUID.randomUUID().toString()
            val url = "http://localhost:$a/$res"
            sourceView[url] = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder())
            callback[url] = Runnable {
                f.complete(sourceView[url])
                requestDone = true
            }
            callbackOnNextReload = Runnable {}
            textTable[url] = text
            browser.loadURL(url)
        })
        checkAndRunRequests()
        return f
    }

    private fun checkAndRunRequests() {
        if (pendingRequests.size > 0 && requestDone) {
            requestDone = false
            pendingRequests.removeAt(0).run()
        }
    }

    fun createTextureURL(url: String, unit: Int): Texture {
        val t = Texture(
            Texture.TextureSpecification.byte4(
                unit,
                w,
                h,
                ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder()),
                true
            )
        )
        toTextureWhenReady(renderURL(url), t)
        return t
    }


    fun createTextureURL_stream(url: String, unit: Int): Texture {
        val t = Texture(
            Texture.TextureSpecification.byte4(
                unit,
                w,
                h,
                ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder()),
                true
            )
        )

        val u = renderURL_setCallback(url) {
            println(" -- uploading texture ")
            t.upload(it, true)
        }

        return t
    }

    fun createTextureString(url: String, unit: Int): Texture {
        val t = Texture(
            Texture.TextureSpecification.byte4(
                unit,
                w,
                h,
                ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder()),
                true
            )
        )
        toTextureWhenReady(renderText(url), t)
        return t
    }

    protected fun message(id: Long, message: String, reply: Consumer<String?>?) {
        println(" message from HTMLToTexture :$message")
    }

    protected fun paint(popup: Boolean, dirty: Array<Rectangle>, buffer: ByteBuffer, w: Int, h: Int) {

        System.out.println(" -- paint for HTMLToTexture ${browser.url}")

        var sourceView = sourceView[browser.url]
        if (sourceView == null) {
            sourceView = this.sourceView[browser.url + "/"]
            if (sourceView == null) {
                sourceView = this.sourceView[browser.url.replace("http", "https") + "/"]
                if (sourceView == null) {
                    sourceView = this.sourceView[browser.url.replace("http", "https")]
                    if (sourceView == null) {
                        val v = browser.url.replace("https", "http")
                        sourceView = this.sourceView[v.substring(0, v.length - 1)]
                    }
                }
            }
        }
        if (sourceView == null) {
            System.err.println("ERROR?: no backing for :" + browser.url + " backings are available for " + this.sourceView.keys + " / " + this.sourceView)
            return
        }
        if (dirty.size == 0) return
        val cb = callback[browser.url]
        sourceView.clear()
        buffer.clear()
        var x0 = w
        var x1 = 0
        var y0 = h
        var y1 = 0
        for (r in dirty) {
            if (r.x == 0 && r.y == 0 && r.width == w && r.height == h) {
                buffer.clear()
                sourceView.clear()
                sourceView.put(buffer)
                x0 = 0
                y0 = 0
                x1 = w
                y1 = h
            } else {
                for (y in r.y until r.y + r.height) {
                    buffer.limit(r.x * 4 + y * 4 * w + r.width * 4)
                    buffer.position(r.x * 4 + y * 4 * w)
                    sourceView.limit(r.x * 4 + y * 4 * w + r.width * 4)
                    sourceView.position(r.x * 4 + y * 4 * w)
                    sourceView.put(buffer)
                }
                //                System.out.println("r= " + r);
                x0 = Math.min(x0, r.x)
                x1 = Math.max(x1, r.width + r.x)
                y0 = Math.min(y0, r.y)
                y1 = Math.max(y1, r.height + r.y)
            }
        }
        sourceView.clear()
        buffer.clear()
        mod.incrementAndGet()
        cb?.run()
    }

    protected fun run() {
        val now = System.currentTimeMillis()
        if (now - timeSinceLastLoad > 2000 && pendingRequests.size > 0) {
            requestDone = true
            checkAndRunRequests()
        }
    }

    companion object {
        fun toTextureWhenReady(b: Future<ByteBuffer?>, m: Texture) {
            m.attach(-1, Perform {
                if (b.isDone) {
                    try {
                        // the buffer is done, but perhaps nobody has painted into it yet; which is clearly not what we want done to be
                        m.upload(b.get(), false)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } catch (e: ExecutionException) {
                        e.printStackTrace()
                    }
                    false
                } else {
                    true
                }
            })
        }
    }

    init {
        a = Ports.nextAvailable(9080)
        b = Ports.nextAvailable(a + 1)
        try {
            s = Server(a, b)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        s!!.addURIHandler { uri: String, method: NanoHTTPD.Method?, headers: Map<String?, String?>?, params: Map<String?, String?>?, files: Map<String?, String?>? ->
            val m = textTable["http://localhost:$a$uri"]
            if (m != null) return@addURIHandler NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", m)
            null
        }
        browser = CefSystem.cefSystem.makeBrowser(
            w,
            h,
            { popup: Boolean, dirty: Array<Rectangle>?, buffer: ByteBuffer?, w: Int, h: Int ->
                paint(
                    popup,
                    dirty!!,
                    buffer!!,
                    w,
                    h
                )
            },
            { id: Long, message: String?, reply: Consumer<String?>? -> message(id, message!!, reply) }) {
            try {
                if (callbackOnNextReload != null) {
                    callbackOnNextReload!!.run()
                    checkAndRunRequests()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                callbackOnNextReload = null
            }
        }
        Thread {
            while (true) {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                run()
            }
        }.start()
    }
}