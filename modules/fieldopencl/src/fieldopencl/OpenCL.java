package fieldopencl;

import com.nativelibs4java.opencl.*;
import field.graphics.ProvidesGraphicsContext;
import field.graphics.Shader;
import field.graphics.SimpleArrayBuffer;
import field.graphics.Texture;
import field.utility.Dict;
import field.utility.Pair;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.RemoteEditor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.BiFunction;



/**
 * scene element for initializing an OpenCL context
 */
public class OpenCL extends Box implements field.graphics.Scene.Perform {

	static public final Dict.Prop<List<Kernel>> kernels = new Dict.Prop<>("kernels").doc(" An OpenCL kernel derived from this box").type().toCannon();
	static public final Dict.Prop<String> openCL = new Dict.Prop<>("openCL").doc("OpenCL source code for a program");
	static public final Dict.Prop<BiFunctionOfBoxAnd<String, Kernel>> newOpenCLKernel = new Dict.Prop<>("newOpenCLKernel").doc("`_.newOpenCLKernel('k')` creates an OpenCL kernel from this program with entry-point 'k'").type().toCannon();

	static public final Dict.Prop<BiFunctionOfBoxAnd<Integer, Buffer>> newBufferIn = new Dict.Prop<>("newBufferIn").doc("`_.newBufferIn(20)` creates a new OpenCL input float array of length 20 ").type().toCannon();
	static public final Dict.Prop<BiFunctionOfBoxAnd<Integer, Buffer>> newBufferOut = new Dict.Prop<>("newBufferOut").doc("`_.newBufferOut(20)` creates a new OpenCL output float array of length 20 ").type().toCannon();

	static public final Dict.Prop<BiFunction<SimpleArrayBuffer, ProvidesGraphicsContext, VertexBuffer>> newGLBufferIn = new Dict.Prop<>("newGLBufferIn").doc("`_.newGLBufferIn(20)` creates a new OpenCL input float array of length 20 ").type().toCannon();
	static public final Dict.Prop<BiFunction<SimpleArrayBuffer, ProvidesGraphicsContext, VertexBuffer>> newGLBufferOut = new Dict.Prop<>("newGLBufferOut").doc("`_.newGLBufferOut(20)` creates a new OpenCL output float array of length 20 ").type().toCannon();

	static public final Dict.Prop<BiFunction<Texture, ProvidesGraphicsContext, TextureBuffer>> newGLTextureOut = new Dict.Prop<>("newGLTextureOut").doc("`_.newBufferOut(texture)` creates a new OpenCL output to a texture").type().toCannon();


	public class Kernel {

		CLKernel kernel;
		CLProgram program;
		String entryPoint;

		public void run1d(int global, Object... args) {
			int i = 0;

			List<VertexBuffer> acquired = new ArrayList<>();
			List<TextureBuffer> acquiredt = new ArrayList<>();

			try {
				for (Object o : args) {
					System.out.println(" setting arg :" + i + " to be " + o);
					if (o instanceof Buffer)
						kernel.setObjectArg(i++, ((Buffer) o).buffer);
					else if (o instanceof VertexBuffer) {
						kernel.setObjectArg(i++, ((VertexBuffer) o).bufferFromGLBuffer);
						(((VertexBuffer) o).bufferFromGLBuffer).acquireGLObject(queue);
						acquired.add((VertexBuffer) o);
					} else if (o instanceof TextureBuffer) {
						kernel.setObjectArg(i++, ((TextureBuffer) o).bufferFromGLBuffer);
						(((TextureBuffer) o).bufferFromGLBuffer).acquireGLObject(queue);
						acquiredt.add((TextureBuffer) o);
					} else
						kernel.setObjectArg(i++, o);
				}

				CLEvent clEvent = kernel.enqueueNDRange(queue, new int[]{global});

			} finally {
				for (VertexBuffer b : acquired) {
					b.bufferFromGLBuffer.releaseGLObject(queue);
				}
				for (TextureBuffer b : acquiredt) {
					b.bufferFromGLBuffer.releaseGLObject(queue);
				}
			}
		}
	}


	public class VertexBuffer {
		private final boolean input;
		private final boolean output;
		public final CLBuffer<Byte> bufferFromGLBuffer;

		public VertexBuffer(boolean input, boolean output, int name) {
			this.input = input;
			this.output = output;

			CLMem.Usage kind = CLMem.Usage.Input;

			if (input && output) {
				kind = CLMem.Usage.InputOutput;
			} else if (input) {
				kind = CLMem.Usage.Input;
			} else if (output) {
				kind = CLMem.Usage.Output;
			}
			bufferFromGLBuffer = context.createBufferFromGLBuffer(kind, name);
		}

		public VertexBuffer(boolean input, boolean output, SimpleArrayBuffer buffer, ProvidesGraphicsContext c) {
			this.input = input;
			this.output = output;

			CLMem.Usage kind = CLMem.Usage.Input;

			if (input && output) {
				kind = CLMem.Usage.InputOutput;
			} else if (input) {
				kind = CLMem.Usage.Input;
			} else if (output) {
				kind = CLMem.Usage.Output;
			}
			bufferFromGLBuffer = context.createBufferFromGLBuffer(kind, buffer.getOpenGLNameInContext(c.getGraphicsContext()));
		}
	}

	public class TextureBuffer {
		private final boolean input;
		private final boolean output;
		private final CLImage2D bufferFromGLBuffer;


		public TextureBuffer(boolean input, boolean output, Texture buffer, ProvidesGraphicsContext c) {
			this.input = input;
			this.output = output;

			CLMem.Usage kind = CLMem.Usage.Input;

			if (input && output) {
				kind = CLMem.Usage.InputOutput;
			} else if (input) {
				kind = CLMem.Usage.Input;
			} else if (output) {
				kind = CLMem.Usage.Output;
			}
			CLContext.GLTextureTarget target =
				    buffer.specification.target == 34037L ? CLContext.GLTextureTarget.Rectangle : CLContext.GLTextureTarget.Texture2D;
			bufferFromGLBuffer = context.createImage2DFromGLTexture2D(kind, target, buffer.getOpenGLNameInContext(c.getGraphicsContext()), 0);
		}
	}

	public class Buffer {

		private final boolean input;
		private final boolean output;
		private final int len;
		private final CLBuffer<Float> buffer;
		private final ByteBuffer bdata;

		public FloatBuffer floats;

		public Buffer(boolean input, boolean output, int len) {
			this.input = input;
			this.output = output;
			this.len = len;

			CLMem.Usage kind = CLMem.Usage.Input;

			if (input && output) {
				kind = CLMem.Usage.InputOutput;
			} else if (input) {
				kind = CLMem.Usage.Input;
			} else if (output) {
				kind = CLMem.Usage.Output;
			}

			bdata = ByteBuffer.allocateDirect(len * 4).order(byteOrder);
			floats = bdata.asFloatBuffer();

			buffer = context.createFloatBuffer(kind, floats, false);
		}

		public void upload(boolean blocking) {
			if (!input) throw new IllegalArgumentException("Buffer is not an input buffer, so you can't upload it");
			buffer.writeBytes(queue, 0, bdata.capacity(), bdata, blocking);
		}

		public void download(boolean blocking) {
			if (!input) throw new IllegalArgumentException("Buffer is not an input buffer, so you can't download it");
			buffer.read(queue, 0, bdata.capacity() / 4, floats, blocking);
		}
	}

	static {
		FieldBox.fieldBox.io.addFilespec("kernel", ".cl", "c");
		IO.persist(openCL);
	}

	boolean first = true;

	private CLContext context;
	private CLQueue queue;
	private ByteOrder byteOrder;

	public OpenCL() {
		properties.put(Commands.commands, () -> {
			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			RemoteEditor ed = this.find(RemoteEditor.editor, both()).findFirst().get();

			Box box = ed.getCurrentlyEditing();
			Dict.Prop<String> cep = ed.getCurrentlyEditingProperty();

			if (box == null) return m;

			List<Kernel> s = box.properties.get(kernels);

			if (!cep.equals(openCL) && (box.first(openCL, box.upwards()).isPresent() || s != null))
				m.put(new Pair<>("Edit <i>OpenCL</i>", "Switch to editing OpenCL kernel"), () -> {
					ed.setCurrentlyEditingProperty(openCL);
				});

			if (s != null) m.put(new Pair<>("Reload kernel", "Recreates all " + s.size() + " kernels" + (s
				    .size() == 1 ? "" : "s") + " associated with this box via _.newOpenCLKernel()"), () -> {
				reload(box, s);
			});


			return m;
		});


		properties.put(newOpenCLKernel, this::newOpenCLKernelFromBox);
		properties.put(newBufferIn, this::newBufferIn);
		properties.put(newBufferOut, this::newBufferOut);
		properties.put(newGLBufferIn, this::newGLBufferIn);
		properties.put(newGLBufferOut, this::newGLBufferOut);
		properties.put(newGLTextureOut, this::newGLTextureOut);
	}

	public Buffer newBufferIn(Box box, int le) {
		Buffer f = new Buffer(true, false, le);
		return f;
	}

	public Buffer newBufferOut(Box box, int le) {
		Buffer f = new Buffer(false, true, le);
		return f;
	}

	public VertexBuffer newGLBufferIn(SimpleArrayBuffer b, ProvidesGraphicsContext c) {
		VertexBuffer f = new VertexBuffer(true, false, b, c);
		return f;
	}

	public VertexBuffer newGLBufferOut(SimpleArrayBuffer b, ProvidesGraphicsContext c) {
		VertexBuffer f = new VertexBuffer(false, true, b, c);
		return f;
	}
	public TextureBuffer newGLTextureOut(Texture b, ProvidesGraphicsContext c) {
		TextureBuffer f = new TextureBuffer(false, true, b, c);
		return f;
	}



	private void reload(Box box, List<Kernel> s) {
		String text = box.properties.get(openCL);

		try {
			for (Kernel kk : s) {

				CLProgram program = context.createProgram(text);

				CLKernel k = program.createKernel(kk.entryPoint);
				kk.program = program;
				kk.kernel = k;
			}
		} catch (Throwable t) {
			System.err.println(" where do errors in shaders go?");
			t.printStackTrace();
		}
	}

	private Kernel newOpenCLKernelFromBox(Box box, String entrypoint) {

		Kernel l = new Kernel();

		String s = "__kernel void " + entrypoint + "(__global const float* a, __global const float* b, __global float* out, int n) \n" +
			    "{\n" +
			    "    int i = get_global_id(0);\n" +
			    "    if (i >= n)\n" +
			    "        return;\n" +
			    "\n" +
			    "    out[i] = cos(a[i]) + sin(b[i]);\n" +
			    "}";
		if (box.properties.get(openCL) == null) {
			box.properties.put(openCL, s);
		} else {
			s = box.properties.get(openCL);
		}

		CLProgram program = context.createProgram(s);

		CLKernel k = program.createKernel(entrypoint);

		l.kernel = k;
		l.program = program;
		l.entryPoint = entrypoint;

		box.properties.putToList(kernels, l);

		return l;
	}

	@Override
	public boolean perform(int i) {

		if (i == -100 && first == true) {

			init();
			first = false;
		}

		return true;
	}

	private void init() {
		CLDevice theDevice = null;
		CLPlatform thePlatform = null;
		// try to list all platform and devices
		for (CLPlatform platform : JavaCL.listPlatforms()) {
			System.out.println("-- platform: " + platform.getName());
			for (CLDevice device : platform.listAllDevices(true)) {
				System.out.println("-- device: " + device.getName().trim());
				if (device.getName().toLowerCase().contains("geforce")) {
					theDevice = device;
					thePlatform = platform;
					System.out.println(" found nvidia ");
				}
			}
		}


		context = theDevice == null ? JavaCL.createContextFromCurrentGL() : thePlatform.createGLCompatibleContext(theDevice);

		System.out.println(" created context is :" + context + " " + context.getPlatform() + " " + Arrays.asList(context.getDevices()));
		queue = context.createDefaultQueue();
		byteOrder = context.getByteOrder();

	}

	@Override
	public int[] getPasses() {
		return new int[]{-100};
	}

	private Shader.iErrorHandler errorHandler(Box b, Kernel shader) {
		return new Shader.iErrorHandler() {
			@Override
			public void beginError() {

			}

			@Override
			public void errorOnLine(int line, String error) {

				System.out.println(" HAS ERROR :" + b.first(RemoteEditor.outputErrorFactory).isPresent());

				b.first(RemoteEditor.outputErrorFactory)
				 .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is))).apply(b).accept(new Pair<>(line, "Error on " + shader + " reload: " + error));
			}

			@Override
			public void endError() {

			}

			@Override
			public void noError() {
				b.first(RemoteEditor.outputFactory)
				 .orElse((x) -> (is -> System.out.println("message (without remote editor attached) :" + is))).apply(b).accept(shader + " reloaded correctly");
			}
		};
	}
}
