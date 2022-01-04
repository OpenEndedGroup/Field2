package field.graphics;

import field.app.RunLoop;
import field.utility.Dict;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fielded.boxbrowser.BoxBrowser;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.lwjgl.opengl.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.*;


/**
 * An OpenGL texture.
 * <p>
 * Handles async double-buffered PBO texture upload by default.
 * <p>
 * Follows the same pattern as FBO --- create a texture by picking one of the growing number of static helpers in TextureSpecification that mask the complexity of OpenGL enums
 */
public class Texture extends BaseScene<Texture.State> implements Scene.Perform, OffersUniform<Integer>, BoxBrowser.HasMarkdownInformation {

    // global statistics on how much we're sending to OpennGL
    static public int bytesUploaded = 0;
    static FileAlterationMonitor ws = null;
    public final TextureSpecification specification;
    protected boolean bindless;
    boolean isDoubleBuffered = true;
    AtomicInteger pendingUploads = new AtomicInteger(0);
    int boundCount = 0;
    int uploadCount = 0;
    AtomicReference<Runnable> postDrawQueue = new AtomicReference<>();

    public Texture(TextureSpecification specification) {
        this.specification = specification;
        if (this.specification.forceSingleBuffered) setIsDoubleBuffered(false);

        if (specification.source != null) {
            installReloadWatch(specification.source, this);
        }
    }

    private void installReloadWatch(String sourceFilename, Texture texture) {

        if (ws == null) {
            ws = new FileAlterationMonitor(2000);
            try {
                ws.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        synchronized (ws) {

            String name = new File(sourceFilename).getName();

            FileAlterationObserver o = new FileAlterationObserver(new File(sourceFilename).getParentFile(),
                                                                  new NameFileFilter(name));
            try {
                o.initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            o.addListener(new FileAlterationListenerAdaptor() {
                @Override
                public void onFileChange(File file) {
                    if (file.getName().equals(name)) {
                        System.out.println(" automatic reload for texture :" + file);
                        texture.reloadFrom(file.getAbsolutePath());
                    }
                }
            });
            ws.addObserver(o);
        }
    }

    private void reloadFrom(String filename) {
        int[] wh = FastJPEG.j.dimensions(filename);
        if (wh[0] == specification.width && wh[1] == specification.height)
            FastJPEG.j.decompress(filename, specification.pixels, wh[0], wh[1]);
        upload(specification.pixels, false);
    }

    @Override
    public String generateMarkdown(Box inside, Dict.Prop property) {
        Future<ByteBuffer> m = asyncDebugDownloadRGB8(null);

        // repaint main window, just in case we're there
        RunLoop.main.once(() -> {
            Drawing.dirty(inside);
        });

        String pre
                = "Framebuffer object has dimensions <b>" + specification.width + "</b>x<b>" + specification.height + "</b> and is bound to texture unit <b>" + specification.unit +
                "</b><br>";
        if (specification.forceSingleBuffered) pre += "Single-buffered updates are on; ";
        if (specification.compressed) pre += "Target is compressed; ";
        if (specification.highQuality)
            pre += "Mip-maps are automatically regenerated on update; ";
        if (specification.type == GL_FLOAT)
            pre += "This is a floating-point resolution texture; ";
        pre = p(pre);
        pre
                += p("This has been bound <b>" + warnIfZero(
                boundCount) + "</b> time" + (boundCount == 1 ? "" : "s") + " and modified <b>" + uploadCount + "</b> time" + (uploadCount == 1 ?
                "" : "s") + "<br>");

        int tries = 0;
        while (!m.isDone()) {
            try {
                Thread.sleep(100 * tries);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (++tries > 5)
                return pre + p("<b>Image data for FBO is not available (FBO must be actively repainting)<b>");
        }

        try {

            File tmp = File.createTempFile("field", ".jpg");
            new FastJPEG().compress(tmp.getAbsolutePath(), m.get(), specification.width, specification.height);
            byte[] bytes = Files.readAllBytes(tmp.toPath());
            java.util.Base64.Encoder e = java.util.Base64.getEncoder();
            String v = e.encodeToString(bytes);

            String uri = "data:image/jpg;base64," + v;
            String d = "<p><b>Snapshot of contents &mdash;<br></p><img style='max-width:300px' src='" + uri + "'/></b>";

            return pre + p(d);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return p("<b>Image data for FBO is not available (there was an error fetching it)<b>");

    }

    private String p(String p) {
        return "<p>" + p + "</p>";
    }

    private String warnIfZero(int x) {
        if (x == 0) return "<span class='warning'>" + x + "</span>";
        return "" + x;
    }

    /**
     * must be called before first render
     */
    public Texture makeBindless() {
        bindless = true;
        return this;
    }

    /**
     * schedules an upload from this bytebuffer to this texture during the next drawn. Set stream to true to hint to OpenGL that you mean to keep on doing this.
     */
    public void upload(ByteBuffer upload, boolean stream) {
        if (pendingUploads.get()>10) {
            System.out.println(" too many pending uploads ("+pendingUploads.get()+"), skipping ");
//            return;
//            pendingUploads.set(0);
        }
        pendingUploads.incrementAndGet();

        attach(new Transient(() -> {
            pendingUploads.decrementAndGet();

//            System.out.println(" uploading ??");

            State s = GraphicsContext.get(this, null);

            Log.log("graphics.trace", () -> "state for texture in upload is " + s);
            Log.log("texture.trace", () -> "upload, part 1, for texture " + this + " " + s + " " + upload.capacity());

            if (s == null) return;

            s.x0 = 0;
            s.x1 = specification.width;
            s.y0 = 0;
            s.y1 = specification.height;

            Log.log("graphics.trace", () -> "uploading ");
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, isDoubleBuffered ? (stream ? s.pboA : s.pboB) : s.pbo);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
//            glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);
            s.old = GL15.glMapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, GL15.GL_WRITE_ONLY, s.old);
            s.old.rewind();
            upload.rewind();
            upload.limit(s.old.limit());
            s.old.put(upload);
            upload.clear();
            upload.rewind();
            s.old.rewind();

            bytesUploaded += s.old.limit();
            GL15.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            Log.log("graphics.trace", () -> "uploaded part 1");
            s.mod++;
        }, -200)/*.setOnceOnly()*/.setAllContextsFor(this));

    }

    /**
     * schedules an upload from the buffer that was originally used to create this texture to opengl
     */
    public void upload() {
        pendingUploads.incrementAndGet();
        attach(new Transient(() -> {
            GraphicsContext.checkError(() -> "entering texture upload " + specification);
            pendingUploads.decrementAndGet();
            State s = GraphicsContext.get(this, null);


            if (s == null) {
                System.out.println(" -- no context for this texture upload");
                return;
            }

            glBindTexture(specification.target, s.name);
            GraphicsContext.checkError(() -> "bound texture" + specification);
            if (specification.target == GL_TEXTURE_1D)
                glTexSubImage1D(specification.target, 0, 0, specification.width, specification.format,
                                specification.type, specification.pixels);
            else
                glTexSubImage2D(specification.target, 0, 0, 0, specification.width, specification.height,
                                specification.format, specification.type, specification.pixels);

            if (specification.highQuality) {
                glGenerateMipmap(specification.target);
            }
            GraphicsContext.checkError(() -> "exiting texture upload " + specification);
        }, -200)/*.setOnceOnly()*/.setAllContextsFor(this));
    }

    /**
     * schedules an upload from this bytebuffer to this texture during the next drawn. Set stream to true to hint to OpenGL that you mean to keep on doing this.
     */
    public void upload(ByteBuffer upload, boolean stream, int x0, int y0, int x1, int y1) {
        pendingUploads.incrementAndGet();
        String uid = UUID.randomUUID().toString();
        attach(new Transient(() -> {
            pendingUploads.decrementAndGet();
            State s = GraphicsContext.get(this, null);
            Log.log("graphics.trace", () -> "state for texture in upload is " + s);
            Log.log("texture.trace",
                    () -> "upload, part 1, for texture " + this + " " + s + " " + upload.capacity() + " " + x0 + " " + y0 + " " + x1 + " " + y1);

            if (s == null) return;
            s.x0 = x0;
            s.x1 = x1;
            s.y0 = y0;
            s.y1 = y1;
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, isDoubleBuffered ? (stream ? s.pboA : s.pboB) : s.pbo);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);

            int start = specification.elementSize * (specification.width * y0 + x0);
            int end = Math.min(specification.elementSize * specification.width * specification.height,
                    specification.elementSize * (specification.width * (y1) + x1));
            s.old = glMapBufferRange(GL21.GL_PIXEL_UNPACK_BUFFER, start, end - start,
                    GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_INVALIDATE_RANGE_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT,
                    s.old);

            s.old.position(0);
            upload.position(start);
            upload.limit(end);
            s.old.limit(end - start);

            s.old.put(upload);

            upload.clear();
            upload.rewind();
            s.old.clear();
            s.old.rewind();

            bytesUploaded += s.old.limit();

            GL15.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            Log.log("graphics.trace", () -> "uploaded part 1");
            s.mod++;
        }, -2)/*.setOnceOnly()*/.setAllContextsFor(this));
    }

    public void uploadLayer(ByteBuffer upload, boolean stream, int z) {
        pendingUploads.incrementAndGet();
        String uid = UUID.randomUUID().toString();
        attach(new Transient(() -> {
            pendingUploads.decrementAndGet();
            State s = GraphicsContext.get(this, null);
            Log.log("graphics.trace", () -> "state for texture in upload is " + s);
            Log.log("texture.trace",
                    () -> "upload, part 1, for texture " + this + " " + s + " " + upload.capacity() + " ");

            if (s == null) return;
            s.x0 = 0;
            s.x1 = specification.width;
            s.y0 = 0;
            s.y1 = specification.height;
            s.z = z;
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, isDoubleBuffered ? (stream ? s.pboA : s.pboB) : s.pbo);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);

            int start = 0;
            int end = specification.elementSize * specification.width*specification.height;
            s.old = glMapBufferRange(GL21.GL_PIXEL_UNPACK_BUFFER, start, end - start,
                    GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_INVALIDATE_RANGE_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT,
                    s.old);

            s.old.position(0);
            upload.clear();
            s.old.limit(end - start);
            upload.limit(end - start);

            s.old.put(upload);

            upload.clear();
            upload.rewind();
            s.old.clear();
            s.old.rewind();

            bytesUploaded += s.old.limit();

            GL15.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            Log.log("graphics.trace", () -> "uploaded part 1");
            s.mod++;
        }, -2)/*.setOnceOnly()*/.setAllContextsFor(this));
    }

    public int getPendingUploads() {
        return pendingUploads.get();
    }

    protected boolean perform0() {

        State s = GraphicsContext.get(this);

        Log.log("graphics.trace", () -> "activating texture :" + specification.unit + " = " + s.name);

        glActiveTexture(GL_TEXTURE0 + specification.unit);
        glBindTexture(specification.target, s.name);

        Runnable m = postDrawQueue.getAndSet(null);
        if (m != null) m.run();

        boundCount++;

        return true;
    }

    public int getOpenGLNameInCurrentContext() {
        State s = GraphicsContext.get(this);
        if (s == null) throw new IllegalArgumentException("No state in this context");

        return s.name;
    }


    public int getOpenGLNameInContext(GraphicsContext context) {
        State s = context.lookup(this);
        if (s == null) throw new IllegalArgumentException("No state in this context");

        return s.name;
    }

    protected State setup() {

        GraphicsContext.checkError(() -> "setting up texture, entry " + specification);

        State s = new State();
        s.name = glGenTextures();
        s.pboA = glGenBuffers();
        if (isDoubleBuffered) s.pboB = glGenBuffers();
        s.pbo = s.pboA;


        glActiveTexture(GL_TEXTURE0 + specification.unit);
        glBindTexture(specification.target, s.name);
        GraphicsContext.checkError(() -> "setting up texture, bound " + specification);


        if (!specification.highQuality) {
            glTexParameteri(specification.target, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(specification.target, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(specification.target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(specification.target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        } else {
            glTexParameteri(specification.target, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(specification.target, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(specification.target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(specification.target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        }

        GraphicsContext.checkError(() -> "setting up texture, params " + specification);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);

        GraphicsContext.checkError(() -> "setting up texture, pixel store " + specification);

        if (specification.target == GL_TEXTURE_1D) {
            ARBTextureStorage.glTexStorage1D(specification.target,
                                             specification.highQuality ? (int) (Math.floor(Math.log(
                                                     Math.max(specification.width, specification.height)) / Math.log(
                                                     2)) + 1) : 1,
                                             specification.internalFormat, specification.width);
        } else if (specification.target == GL_TEXTURE_3D) {
            ARBTextureStorage.glTexStorage3D(specification.target,
                    specification.highQuality ? (int) (Math.floor(Math.log(
                            Math.max(specification.width, specification.height)) / Math.log(
                            2)) + 1) : 1,
                    specification.internalFormat, specification.width, specification.height,
                    specification.depth);
        }else if (specification.target == GL_TEXTURE_2D_ARRAY) {
            ARBTextureStorage.glTexStorage3D(specification.target,
                    specification.highQuality ? (int) (Math.floor(Math.log(
                            Math.max(specification.width, specification.height)) / Math.log(
                            2)) + 1) : 1,
                    specification.internalFormat, specification.width, specification.height,
                    specification.depth);
        } else {
            ARBTextureStorage.glTexStorage2D(specification.target,
                                             specification.highQuality ? (int) (Math.floor(Math.log(
                                                     Math.max(specification.width, specification.height)) / Math.log(
                                                     2)) + 1) : 1,
                                             specification.internalFormat, specification.width, specification.height);
        }
        GraphicsContext.checkError(() -> "setting up texture, storage " + specification);


        if (specification.compressed) {
//			glCompressedTexSubImage2D(specification.target, 0, 0, 0, specification.width, specification.height, specification.internalFormat, specification.pixels.capacity(),
// specification.pixels);
            throw new IllegalArgumentException(" not implemented ");
        } else {
            if (specification.pixels != null) {

                if (specification.target == GL_TEXTURE_1D) {
                    glTexSubImage1D(specification.target, 0, 0, specification.width, specification.format,
                                    specification.type, specification.pixels);
                } else if (specification.target == GL_TEXTURE_3D) {
                    glTexSubImage3D(specification.target, 0, 0, 0, 0, specification.width, specification.height,
                            specification.depth, specification.format, specification.type,
                            specification.pixels);
                } else if (specification.target == GL_TEXTURE_2D_ARRAY) {
                    glTexSubImage3D(specification.target, 0, 0, 0, 0, specification.width, specification.height,
                            specification.depth, specification.format, specification.type,
                            specification.pixels);
                } else {
                    glTexSubImage2D(specification.target, 0, 0, 0, specification.width, specification.height,
                                    specification.format, specification.type, specification.pixels);
                }
                if (specification.highQuality) {
                    glGenerateMipmap(specification.target);
                }
            }
        }
        GraphicsContext.checkError(() -> "setting up texture, initial upload " + specification);


        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, s.pbo);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);

        glBufferData(GL_PIXEL_UNPACK_BUFFER, specification.elementSize * specification.width * specification.height,
                     GL15.GL_STREAM_DRAW);

        GraphicsContext.checkError(() -> "setting up texture, pbo " + specification);

        if (isDoubleBuffered) {
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, s.pboB);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);
            glBufferData(GL_PIXEL_UNPACK_BUFFER, specification.elementSize * specification.width * specification.height,
                         GL15.GL_STREAM_DRAW);

        }
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        GraphicsContext.checkError(() -> "setting up texture,finished " + specification);

        if (bindless) {
            s.textureHandle = NVBindlessTexture.glGetTextureHandleNV(s.name);
            NVBindlessTexture.glMakeTextureHandleResidentNV(s.textureHandle);
        }
        glBindTexture(specification.target, 0);

        return s;
    }

    protected int upload(State s) {

        GraphicsContext.checkError(() -> " upload part 2 entry");

        glActiveTexture(GL_TEXTURE0 + specification.unit);
        glBindTexture(specification.target, s.name);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, isDoubleBuffered ? s.pboB : s.pbo);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, specification.width);

        GraphicsContext.checkError(() -> " after some setup for upload ");

        int top = specification.elementSize * s.y0 * specification.width;
        if (specification.target == GL_TEXTURE_3D) {
            glTexSubImage3D(
                    GL_TEXTURE_3D,
                    0,
                    0,
                    0,
                    s.z,
                    specification.width,
                    specification.height,
                    1,
                    specification.format,
                    specification.type,
                    0);
        }
        else
            glTexSubImage2D(specification.target, 0, 0, s.y0, specification.width, s.y1 - s.y0 - 1, specification.format,
                            specification.type, top);

        GraphicsContext.checkError(() -> " after that glTexSubImage2D");

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        GraphicsContext.checkError(() -> " and we're done ");

        if (specification.highQuality) {
            glGenerateMipmap(specification.target);
        }
        long b = System.currentTimeMillis();

        if (isDoubleBuffered) {
            int q = s.pboA;
            s.pboA = s.pboB;
            s.pboB = q;
        }
        uploadCount++;
        glBindTexture(specification.target, 0);

        return mod;
    }

    @Override
    public String toString() {
        return super.toString() + System.identityHashCode(this);
    }

    public int forceUploadNow(ByteBuffer from) {

        State s = GraphicsContext.get(this, () -> setup());

//		glFinish();

//        int was =GL11.glGetInteger(GL_TEXTURE_BINDING_2D);

        glBindTexture(specification.target, s.name);
        glTexSubImage2D(specification.target, 0, 0, 0, specification.width, specification.height, specification.format,
                        specification.type, from);
        if (specification.highQuality) {
            glGenerateMipmap(specification.target);
        }
        glBindTexture(specification.target, 0);

//		glFinish();

        uploadCount++;
        return mod;
    }


	public Texture setIsDoubleBuffered(boolean isDoubleBuffered) {
		this.isDoubleBuffered = isDoubleBuffered;
		return this;
	}

    public int getPBOSource(GraphicsContext context) {
        State s = GraphicsContext.get(this);
        if (isDoubleBuffered)
            throw new IllegalArgumentException("can't reliably get the pbo source of a double buffered texture");
        //mod++;

        s.mod++;

        s.x0 = 0;
        s.x1 = specification.width;
        s.y0 = 0;
        s.y1 = specification.height;

        return s.pboA;
    }

    @Override
    public int[] getPasses() {
        return new int[]{-1};
    }

    protected void deallocate(State s) {
        glDeleteTextures(s.name);
        glDeleteBuffers(s.pboA);
        if (isDoubleBuffered) glDeleteBuffers(s.pboB);
    }

    @Override
    public Integer getUniform() {
//		State s = GraphicsContext.get(this);
//		if (s == null) return null;
//		return s.name;

        return specification.unit;
    }

    /**
     * this operates asynchronously if we are not currently inside the draw loop (that is, if there is no valid context for our thread). only one debug download can be pending at a time. You can
     * pass in null to this routine and you'll get a correctly sized ByteByffer back that you can reuse for subsequent calls. Regardless of the original format of the FBO this always returns
     * something that's RGBA / byte,.
     */
    public Future<ByteBuffer> asyncDebugDownloadRGBA8(ByteBuffer to) {
        ByteBuffer ato;

        int sz = specification.width * specification.height * 4;
        if (to == null || !to.isDirect() || to.limit() < sz) {
            ato = ByteBuffer.allocateDirect(sz);
        } else ato = to;

        CompletableFuture<ByteBuffer> c = new CompletableFuture<>() {
            @Override
            public ByteBuffer get() throws InterruptedException, ExecutionException {
                if (!isDone()) return ato;
                return super.get();
            }
        };
        Runnable r = () -> {
            int[] a = {0};
            a[0] = glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(specification.target, getOpenGLNameInCurrentContext());
            glGetTexImage(specification.target, 0, GL11.GL_RGBA, GL_UNSIGNED_BYTE, ato);
            glBindTexture(specification.target, a[0]);
            c.complete(ato);
        };

        if (GraphicsContext.getContext() == null) {
            postDrawQueue.set(r);
        } else {
            r.run();
        }

        return c;
    }

    /**
     * this operates asynchronously if we are not currently inside the draw loop (that is, if there is no valid context for our thread). only one debug download can be pending at a time. You can
     * pass in null to this routine and you'll get a correctly sized ByteBuffer back that you can reuse for subsequent calls. Regardless of the original format of the FBO this always returns
     * something that's RGBA / byte,.
     */
    public Future<ByteBuffer> asyncDebugDownloadRGB8(ByteBuffer to) {
        ByteBuffer ato;

        int sz = specification.width * specification.height * 3;
        if (to == null || !to.isDirect() || to.limit() < sz) {
            ato = ByteBuffer.allocateDirect(sz);
        } else ato = to;

        CompletableFuture<ByteBuffer> c = new CompletableFuture<ByteBuffer>() {
            @Override
            public ByteBuffer get() throws InterruptedException, ExecutionException {
                if (!isDone()) return ato;
                return super.get();
            }
        };
        Runnable r = () -> {
            int[] a = {0};
            a[0] = glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(specification.target, getOpenGLNameInCurrentContext());
            glGetTexImage(specification.target, 0, GL11.GL_RGB, GL_UNSIGNED_BYTE, ato);
            glBindTexture(specification.target, a[0]);
            c.complete(ato);
        };

        if (GraphicsContext.getContext() == null) {
            postDrawQueue.set(r);
        } else {
            r.run();
        }

        return c;
    }

    public Future<FloatBuffer> asyncDebugDownloadRGBF32(FloatBuffer to) {
        CompletableFuture f = new CompletableFuture<>();
        this.attach(new Scene.Transient(() -> {
            glFinish();
            int a = glGetInteger(GL_TEXTURE_BINDING_2D);
            GraphicsContext.checkError(() -> "about to bind texture");
            glBindTexture(this.specification.target, this.getOpenGLNameInCurrentContext());
            GraphicsContext.checkError(() -> "bound texture");
            glGetTexImage(this.specification.target, 0, GL_RGBA, GL_FLOAT, to);
            GraphicsContext.checkError(() -> "get texture");
            glBindTexture(this.specification.target, a);
            GraphicsContext.checkError(() -> "unbound texture");
            f.complete(to);
        }, -10).setOnceOnly());
        return f;
    }

    public int forceGenerateMipmapNow() {
        if (specification.highQuality) {
            State s = GraphicsContext.getContext().lookup(this);
            glBindTexture(specification.target, s.name);
            glGenerateMipmap(specification.target);
            glBindTexture(specification.target, 0);
            uploadCount++;
        }
        return mod;
    }

    static public class TextureSpecification {
        public final int unit;
        public final int target;
        public final int internalFormat;
        public final int width;
        public final int height;
        public final int format;
        public final int type;
        public final int elementSize;
        public final boolean highQuality;
        public final ByteBuffer pixels;
        public final boolean forceSingleBuffered;
        public final boolean compressed;
        public int depth;
        public String source; // optional

        public TextureSpecification(int unit, int target, int internalFormat, int width, int height, int format, int type, int elementSize, ByteBuffer pixels, boolean highQuality) {
            this.unit = unit;
            this.target = target;
            this.internalFormat = internalFormat;
            this.width = width;
            this.height = height;
            this.format = format;
            this.type = type;
            this.elementSize = elementSize;
            this.pixels = pixels;
            this.highQuality = highQuality;
            forceSingleBuffered = false;
            compressed = false;
        }

        public TextureSpecification(int unit, int target, int internalFormat, int width, int height, int format, int type, int elementSize, ByteBuffer pixels, boolean highQuality, boolean
                forceNotStreaming, boolean compressed) {
            this.unit = unit;
            this.target = target;
            this.internalFormat = internalFormat;
            this.width = width;
            this.height = height;
            this.format = format;
            this.type = type;
            this.elementSize = elementSize;
            this.pixels = pixels;
            this.highQuality = highQuality;
            this.forceSingleBuffered = forceNotStreaming;
            this.compressed = compressed;
        }

        static public TextureSpecification byte3(int unit, int width, int height, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB8, width, height, GL_RGB, GL_UNSIGNED_BYTE, 3,
                    source, mips);
        }

        static public TextureSpecification byte3_rev(int unit, int width, int height, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB8, width, height, GL_BGR, GL_UNSIGNED_BYTE, 3,
                    source, mips);
        }

        static public TextureSpecification byte3(int unit, int width, int height, ByteBuffer source, boolean mips, boolean forceSingleBuffered) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB8, width, height, GL_RGB, GL_UNSIGNED_BYTE, 3,
                                            source, mips, forceSingleBuffered, false);
        }

        static public TextureSpecification byte1(int unit, int width, int height, ByteBuffer source, boolean mips) {
		return new TextureSpecification(unit, GL_TEXTURE_2D, GL_R8, width, height, GL_RED, GL_UNSIGNED_BYTE, 1,
			source, mips);
        }

        static public TextureSpecification uint4_1d(int unit, int width, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_1D, GL_RGBA32UI, width, 1, GL_RGBA_INTEGER,
                                            GL_UNSIGNED_INT, 16, source, mips);
        }

        static public TextureSpecification uint1_1d(int unit, int width, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_1D, GL_R32UI, width, 1, GL_RED_INTEGER, GL_UNSIGNED_INT, 4,
                                            source, mips);
        }

        static public TextureSpecification fromJpeg(int unit, String filename, boolean mips) {

            // help with windows path problems
            filename = new File(filename).getAbsolutePath();

            int[] wh = FastJPEG.j.dimensions(filename);
            ByteBuffer data = ByteBuffer.allocateDirect(3 * wh[0] * wh[1]);
            FastJPEG.j.decompress(filename, data, wh[0], wh[1]);

            TextureSpecification ts = new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB8, wh[0], wh[1], GL_RGB,
                                                               GL_UNSIGNED_BYTE, 3, data, mips);
            ts.source = filename;
            return ts;
        }


        static public TextureSpecification fromJpegGS(int unit, String filename, boolean mips) {

            // help with windows path problems
            filename = new File(filename).getAbsolutePath();

            int[] wh = FastJPEG.j.dimensions(filename);
            ByteBuffer data = ByteBuffer.allocateDirect(wh[0] * wh[1]);
            FastJPEG.j.decompress(filename, data, wh[0], wh[1]);

            TextureSpecification ts = new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGBA8, wh[0], wh[1], GL_LUMINANCE,
                                                               GL_UNSIGNED_BYTE, 1, data, mips);
            ts.source = filename;
            return ts;
        }

        static public TextureSpecification from1DRGBAFloatBuffer(int unit, int length, FloatBuffer source) {
            ByteBuffer data = ByteBuffer.allocateDirect(4 * 4 * length)
                    .order(ByteOrder.nativeOrder());
            data.asFloatBuffer()
                    .put(source);
            data.rewind();
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RGBA32F, length, 1, GL_RGBA, GL_FLOAT, 16,
                                            data, false);
        }

        static public TextureSpecification from1DFloatBuffer(int unit, int length, FloatBuffer source) {
            ByteBuffer data = ByteBuffer.allocateDirect(4 * length);
            data.asFloatBuffer()
                    .put(source);
            data.rewind();
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_R32F, length, 1, GL_RED, GL_FLOAT, 4, data,
                                            false);
        }

        static public TextureSpecification from1DRFloatBuffer(int unit, int length, ByteBuffer data) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_R32F, length, 1, GL_RED, GL_FLOAT, 4, data,
                                            false, true, false);
        }

        static public TextureSpecification from1DRGFloatBuffer(int unit, int length, ByteBuffer data) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RG32F, length, 1, GL_RG, GL_FLOAT, 8, data,
                                            false, true, false);
        }

        static public TextureSpecification from1DRGBFloatBuffer(int unit, int length, ByteBuffer data) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RGB32F, length, 1, GL_RGB, GL_FLOAT, 12, data,
                                            false, true, false);
        }

        static public TextureSpecification from1DRGBAFloatBuffer(int unit, int length, ByteBuffer data) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RGBA32F, length, 1, GL_RGBA, GL_FLOAT, 16,
                                            data, false, true, false);
        }

        static public TextureSpecification byte4(int unit, int width, int height, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGBA8, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 4,
                                            source, mips);
        }

        static public TextureSpecification float4(int unit, int width, int height, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL30.GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT, 16,
                                            source, mips);
        }


        static public TextureSpecification float4_16(int unit, int width, int height, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGBA16F, width, height, GL_RGBA, GL_HALF_FLOAT, 8,
                                            source, mips);
        }

        static public TextureSpecification bptc(int unit, int width, int height, ByteBuffer source) {
            return new TextureSpecification(unit, GL_TEXTURE_2D,
                                            ARBTextureCompressionBPTC.GL_COMPRESSED_RGBA_BPTC_UNORM_ARB, width, height,
                                            GL_NONE, GL_NONE, 0, source, false, false,
                                            true);
        }

        static public TextureSpecification float1(int unit, int width, int height, ByteBuffer source) {
            return new TextureSpecification(unit, GL_TEXTURE_2D, GL_R32F, width, height, GL_RED, GL_FLOAT, 4,
                                            source, false);
        }


        static public TextureSpecification float3(int unit, int width, int height, ByteBuffer source) {
                return new TextureSpecification(unit, GL_TEXTURE_2D, GL_RGB32F, width, height, GL_RGB, GL_FLOAT, 3,
                                            source, false);
        }

        static public TextureSpecification float4_1d(int unit, int width, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_1D, GL30.GL_RGBA32F, width, 1, GL_RGBA, GL_FLOAT, 16,
                                            source, mips);
        }

        static public TextureSpecification float4_3d(int unit, int width, int height, int depth, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_3D, GL30.GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT, 16,
                    source, mips).depth(depth);
        }

        static public TextureSpecification float1_3d(int unit, int width, int height, int depth, ByteBuffer source, boolean mips) {
            return new TextureSpecification(unit, GL_TEXTURE_3D, GL30.GL_R32F, width, height, GL_RED, GL_FLOAT, 4,
                    source, mips).depth(depth);
        }

        public TextureSpecification depth(int depth) {
            this.depth = depth;
            return this;
        }

        @Override
        public String toString() {
            return "TextureSpecification{" +
                    "unit=" + unit +
                    ", target=" + target +
                    ", internalFormat=" + internalFormat +
                    ", width=" + width +
                    ", height=" + height +
                    ", format=" + format +
                    ", type=" + type +
                    ", elementSize=" + elementSize +
                    ", highQuality=" + highQuality +
                    ", pixels=" + pixels +
                    ", depth=" + depth +
                    '}';
        }
    }

    public class State extends BaseScene.Modifiable {
        public long textureHandle;
        protected int name = -1;
        protected int pboA;
        protected int pboB;
        protected int pbo;
        protected ByteBuffer old;
        int x0, x1, y0, y1, z;
    }

    public TextureUnitReplacement viewWithDifferentUnit(int u)
    {
        return new TextureUnitReplacement(u, this);
    }


}
