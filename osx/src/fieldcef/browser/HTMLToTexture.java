package fieldcef.browser;

import field.app.RunLoop;
import field.graphics.Scene;
import field.graphics.Texture;
import field.utility.Pair;
import field.utility.Ports;
import field.utility.Rect;
import fieldbox.boxes.Drawing;
import fielded.webserver.NanoHTTPD;
import fielded.webserver.Server;
import org.cef.browser.CefRendererBrowserBuffer;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by marc on 9/20/2016.
 */
public class HTMLToTexture  {


    private final Map<String, ByteBuffer> sourceView = new LinkedHashMap<>();
    private final Map<String, Runnable> callback= new LinkedHashMap<>();
    private Server s;
    private final int a;
    private final int b;

    private CefRendererBrowserBuffer browser;
    private final int w;
    private final int h;

    transient Runnable callbackOnNextReload = null;


    Map<String, String> textTable = new LinkedHashMap<String, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 100;
        }
    };
    List<Runnable> pendingRequests = new ArrayList<>();
    transient boolean requestDone = true;


    public AtomicInteger mod = new AtomicInteger(0);
    private long timeSinceLastLoad;

    public HTMLToTexture(int w, int h) {

        this.w = w;
        this.h = h;

        a = Ports.nextAvailable(9080);
        b = Ports.nextAvailable(a + 1);
        try {
            this.s = new Server(a, b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.s.addURIHandler((uri, method, headers, params, files) -> {

            System.out.println(" handler ? "+uri+" "+textTable.keySet());

            String m = textTable.get("http://localhost:"+a+uri);
            System.out.println(" response is :"+m);
            if (m!=null)
                return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", m);

            return null;
        });

        browser = CefSystem.cefSystem.makeBrowser(w, h, this::paint, this::message, () -> {

            System.out.println(" completed :" + browser.getURL());

            try {
                if (callbackOnNextReload != null) {
                    callbackOnNextReload.run();

                    checkAndRunRequests();
                }
            } catch (Throwable t) {
                t.printStackTrace();

            } finally {
                callbackOnNextReload = null;
            }
        });

        new Thread(() -> {
            while (true) {
                System.out.println(browser.isLoading());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                run();
            }

        }).start();
    }

    public Future<ByteBuffer> renderURL(String url) {
        CompletableFuture<ByteBuffer> f = new CompletableFuture<>();

        pendingRequests.add(() -> {
            sourceView.put(url, ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder()));
            callback.put(url, () -> {
                f.complete(sourceView.get(url));
                requestDone = true;
            });

            callbackOnNextReload = () -> {
            };

            timeSinceLastLoad = System.currentTimeMillis();
            browser.loadURL(url);
        });
        checkAndRunRequests();
        return f;
    }

    public Future<ByteBuffer> renderText(String text) {

        CompletableFuture<ByteBuffer> f = new CompletableFuture<>();
        pendingRequests.add(() -> {

            String res = UUID.randomUUID().toString();
            String url = "http://localhost:" + a + "/" + res;

            sourceView.put(url, ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder()));
            callback.put(url, () -> {
                f.complete(sourceView.get(url));
                requestDone = true;
            });


            callbackOnNextReload = () -> {
//                f.complete(sourceView.get(url));
//                requestDone = true;
            };

            textTable.put(url, text);

            browser.loadURL(url);
        });
        checkAndRunRequests();

        return f;
    }

    private void checkAndRunRequests() {
        if (pendingRequests.size() > 0 && requestDone) {
            requestDone = false;
            pendingRequests.remove(0).run();
        }
    }

    static public void toTextureWhenReady(Future<ByteBuffer> b, Texture m) {
        m.attach(-1, new Scene.Perform() {

            @Override
            public boolean perform(int pass) {
                if (b.isDone()) {
                    try {
                        // the buffer is done, but perhaps nobody has painted into it yet; which is clearly not what we want done to be

                        m.upload(b.get(), false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    return false;
                } else {
                    return true;
                }
            }
        });
    }

    public Texture createTextureURL(String url, int unit) {
        Texture t = new Texture(Texture.TextureSpecification.byte4(unit, w, h, ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder()), true));
        toTextureWhenReady(renderURL(url), t);
        return t;
    }

    public Texture createTextureString(String url, int unit) {
        Texture t = new Texture(Texture.TextureSpecification.byte4(unit, w, h, ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder()), true));
        toTextureWhenReady(renderText(url), t);
        return t;
    }

    protected void message(long id, String message, Consumer<String> reply) {
        System.out.println(" message from HTMLToTexture :" + message);
    }

    protected void paint(boolean popup, Rectangle[] dirty, ByteBuffer buffer, int w, int h) {

        ByteBuffer sourceView = this.sourceView.get(this.browser.getURL());
        if (sourceView == null) {
            sourceView = this.sourceView.get(this.browser.getURL()+"/");
            if (sourceView == null) {
                sourceView = this.sourceView.get(this.browser.getURL().replace("http", "https")+"/");
                if (sourceView == null) {
                    sourceView = this.sourceView.get(this.browser.getURL().replace("http", "https"));
                    if (sourceView == null) {
                        String v = this.browser.getURL().replace("http", "https");
                        sourceView = this.sourceView.get(v.substring(0, v.length()-1));
                    }
                }
            }
        }


        if (sourceView == null) {
            System.out.println(" no backing for :" + this.browser.getURL() + " backings are available for " + this.sourceView.keySet()+" / "+this.sourceView);
            return;
        }

        if (dirty.length == 0) return;


        Runnable cb = this.callback.get(this.browser.getURL());

        sourceView.clear();
        buffer.clear();

        int x0 = w;
        int x1 = 0;
        int y0 = h;
        int y1 = 0;

        for (Rectangle r : dirty) {
            System.out.println(" rect :"+r);
            if (r.x == 0 && r.y == 0 && r.width == w && r.height == h) {
                buffer.clear();
                sourceView.clear();
                sourceView.put(buffer);
                x0 = 0;
                y0 = 0;
                x1 = w;
                y1 = h;
            } else {

                for (int y = r.y; y < r.y + r.height; y++) {
                    buffer.limit(r.x * 4 + y * 4 * w + r.width * 4);
                    buffer.position(r.x * 4 + y * 4 * w);
                    sourceView.limit(r.x * 4 + y * 4 * w + r.width * 4);
                    sourceView.position(r.x * 4 + y * 4 * w);
                    sourceView.put(buffer);
                }
//                System.out.println("r= " + r);
                x0 = Math.min(x0, r.x);
                x1 = Math.max(x1, r.width + r.x);
                y0 = Math.min(y0, r.y);
                y1 = Math.max(y1, r.height + r.y);
            }
        }


        sourceView.clear();
        buffer.clear();

        mod.incrementAndGet();

        if (cb!=null)
            cb.run();

    }

    protected void run() {

        long now = System.currentTimeMillis();
        if (now - timeSinceLastLoad > 2000 && pendingRequests.size() > 0) {
            requestDone = true;
            checkAndRunRequests();
        }

    }
}
