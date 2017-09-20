package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.graphics.*;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Drawing;
import fieldbox.ui.FieldBoxWindow;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Plugin that adds the ability to easily put (jpg) images on the canvas.
 */
public class Image2 extends Box {

    static public final Dict.Prop<FunctionOfBoxValued<IdempotencyMap<TextureLoader>>> images = new Dict.Prop<>("images").toCannon();
    static public final Dict.Prop<IdempotencyMap<TextureLoader>> _currentImages = new Dict.Prop<>("_currentImages").toCannon();

    FastJPEG j = new FastJPEG();
    Map<String, PerLayer> layerLocal = new LinkedHashMap<>();


    public Image2(Box root) {
        properties.put(images, this::newImage);
        install(root);
    }

    protected Box install(Box root) {
        return install(root, "__main__");
    }

    public Box install(Box root, String layerName) {
        FieldBoxWindow window = root.first(Boxes.window)
                .orElseThrow(() -> new IllegalArgumentException(" can't draw a box hierarchy with no window to draw it in !"));
        Drawing drawing = root.first(Drawing.drawing)
                .orElseThrow(() -> new IllegalArgumentException(" can't install Image into something without drawing support"));


        PerLayer layer = layerLocal.computeIfAbsent(layerName, (k) -> new PerLayer());

        layer.mainShader = new Shader();

        layer.mainShader.addSource(Shader.Type.vertex, "#version 410\n" +
                "layout(location=0) in vec3 position;\n" +
                "layout(location=1) in vec4 color;\n" +
                "layout(location=2) in vec4 tc;\n" +
                "out vec4 vertexColor;\n" +
                "out vec4 vtc;\n" +

                "uniform vec2 translation;\n" +
                "uniform vec2 scale;\n" +
                "uniform vec2 bounds;\n" +

                "void main()\n" +
                "{\n" +
                "	vec2 at = (scale.xy*position.xy+translation.xy)/bounds.xy;\n" +
//			    "	vec2 at = (position.xy);\n" +
                "   gl_Position =  vec4(-1+at.x*2, 1-at.y*2, 0.5, 1.0);\n" +
                "   vertexColor = color;\n" +
                "   vtc =tc;\n" +
                "   vtc.z =tc.z;\n" +
                "}");


        layer.mainShader.addSource(Shader.Type.fragment, "#version 410\n" +
                "layout(location=0) out vec4 _output;\n" +
                "in vec4 vertexColor;\n" +
                "in vec4 vtc;\n" +
                "uniform sampler2D te;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tvec4 current = texture(te, vtc.xy/textureSize(te,0),0);\n" +
                "\t_output  = current*vertexColor;\n" +
//			    "\t_output  = vec4(1,0,0,1);\n" +
                "\n" +
                "}");

        layer.mainShader.attach(new Uniform<Vec2>("translation", () -> drawing.getTranslationRounded()));
        layer.mainShader.attach(new Uniform<Vec2>("scale", () -> drawing.getScale()));
        layer.mainShader.attach(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));

        window.getCompositor()
                .getLayer(layerName)
                .getScene()
                .attach(layer.mainShader);

        return this;
    }

    public IdempotencyMap<TextureLoader> newImage(Box v)
    {
        return v.properties.computeIfAbsent(_currentImages, k -> new TextureLoader_IdempotencyMap());
//        return null;
    }

    public class TextureLoader {
        public String filename;
        public ByteBuffer data;
        public volatile boolean loaded = false;
        public Texture target;
        public BaseMesh mesh;
        public int w;
        public int h;

        public Consumer<TextureLoader> updator = null;

        public TextureLoader(String filename) {
            this.filename = filename;
            int[] d = j.dimensions(filename);
            w = d[0];
            h = d[1];
            data = ByteBuffer.allocateDirect(w * h * 3);
            target = new Texture(Texture.TextureSpecification.byte3(0, w, h, data, true));
            target.setIsDoubleBuffered(false);
            RunLoop.workerPool.submit(() -> {
                j.decompress(filename, data, w, h);
                target.upload(data, false);
                loaded = true;
                Drawing.dirty(Image2.this);
                return null;
            });
            mesh = BaseMesh.triangleList(4, 2);
            mesh.attach(target);

            geometry(new Rect(0, 0, 10 * w / (float) h, 10));
            color(new Vec4(1, 1, 1, 0.5f));

            PerLayer pl = layerLocal.computeIfAbsent("__main__", (k) -> new PerLayer());
            pl.mainShader.attach(mesh);

            mesh.attach(-100, (x) -> {
                if (updator!=null) updator.accept(this);
            });
        }

        public void makeLive(Consumer<TextureLoader> updator)
        {
            this.updator = updator;
        }

        public void pinTo(Box source, Vec2 centerOffset, Vec2 scale, Rect crop)
        {
            makeLive( x -> {

                Rect f = source.properties.get(Box.frame);


                double aw = w*scale.x;
                double ah = h*scale.y;

                double ax = aw/2+f.x+f.w/2+centerOffset.x;
                double ay = ah/2+f.y+f.h/2+centerOffset.y;

                geometry(new Rect(ax-aw/2, ay-ah/2, aw, ah), crop);
            });
        }

        public void reload(String s)
        {
            if (filename!=null && filename.equals(s)) return; //todo check timestamp
            this.filename = s;
            if (!new File(filename).exists())
            {
                RunLoop.workerPool.submit(() -> {
                    for(int i=0;i<data.capacity();i++)
                        data.put(i, (byte)0);
                    target.upload(data, false);
                    loaded = true;
                    Drawing.dirty(Image2.this);
                    return null;
                });
                return;
            }

            int[] d = j.dimensions(filename);
            if (d[0]!=w || d[1]!=h) throw new IllegalArgumentException(" dimensions mismatch :"+d[0]+"!="+w+" || "+d[1]+"!="+h);
            RunLoop.workerPool.submit(() -> {
                j.decompress(filename, data, w, h);
                target.upload(data, false);
                loaded = true;
                Drawing.dirty(Image2.this);
                return null;
            });
        }

        public void delete()
        {
            PerLayer pl = layerLocal.computeIfAbsent("__main__", (k) -> new PerLayer());
            pl.mainShader.detach(mesh);
        }

        public TextureLoader geometry(Rect r) {
            return geometry(r, new Rect(0, 0, w, h));
        }

        public TextureLoader geometry(Rect r, Rect crop) {
            mesh.vertex()
                    .put(r.x)
                    .put(r.y)
                    .put(0)
                    .put(r.x + r.w)
                    .put(r.y)
                    .put(0)
                    .put(r.x + r.w)
                    .put(r.y + r.h)
                    .put(0)
                    .put(r.x)
                    .put(r.y + r.h)
                    .put(0);
            mesh.aux(2, 2)
                    .put(crop.x)
                    .put(crop.y)
                    .put(crop.x + crop.w)
                    .put(crop.y)
                    .put(crop.x + crop.w)
                    .put(crop.y + crop.h)
                    .put(crop.x)
                    .put(crop.y + crop.h);
            mesh.elements().put(0).put(1).put(2).put(0).put(2).put(3);
            return this;
        }

        public TextureLoader color(Vec4 c) {
            mesh.aux(1, 4)
                    .put((float) c.x)
                    .put((float) c.y)
                    .put((float) c.z)
                    .put((float) c.w)
                    .put((float) c.x)
                    .put((float) c.y)
                    .put((float) c.z)
                    .put((float) c.w)
                    .put((float) c.x)
                    .put((float) c.y)
                    .put((float) c.z)
                    .put((float) c.w)
                    .put((float) c.x)
                    .put((float) c.y)
                    .put((float) c.z)
                    .put((float) c.w);
            return this;
        }

    }

    public class PerLayer {
        protected Shader mainShader;
    }


    // can't be inline due to javac bug !?
    private class TextureLoader_IdempotencyMap extends IdempotencyMap<TextureLoader> {
        public TextureLoader_IdempotencyMap() {
            super(TextureLoader.class);
        }

        @Override
        protected TextureLoader massage(Object value) {
            String f = ""+value;
            return super.massage(new TextureLoader(f));
        }

        @Override
        protected TextureLoader massage(Object vraw, Object previously) {
            if (previously == null)
                return massage(vraw);
            else {
                try {
                    ((TextureLoader) previously).reload("" + vraw);
                    return ((TextureLoader) previously);
                }
                catch(IllegalArgumentException e)
                {
                    ((TextureLoader) previously).delete();
                    return massage(vraw);
                }
            }
        }

        @Override
        protected void _removed(Object v) {
            ((TextureLoader)v).delete();
        }
    }
}
