package field.graphics;

import field.graphics.Scene.Perform;
import fieldbox.execution.InverseDebugMapping;

import java.util.function.Function;

/**
 * An ImageProcessor is a amalgam of up to three pieces: an FBO, a rectangular mesh that draws into the FBO, and a shader to draw it with. Image Processors can quickly be put on the screen, and they
 * broadcast their FBO to other ImageProcessors
 * <p>
 * If the FBO is missing then this means that we'll output to the screen.
 */
public class ImageProcessor {

	public final FBO output;
	public final Shader shader;
	public final BaseMesh mesh;

	public ImageProcessor(FBO output, Shader shader, BaseMesh mesh) {
		this.output = output;
		this.shader = shader;
		this.mesh = mesh;
		init();
	}

	public ImageProcessor(FBO.FBOSpecification spec, Shader shader, BaseMesh mesh) {
		this.output = spec == null ? null : new FBO(spec);
		this.shader = shader;
		this.mesh = mesh;
		init();
	}

	public ImageProcessor(FBO output, Shader shader) {
		this.output = output;
		this.shader = shader;
		this.mesh = defaultMesh();
		init();
	}

	public ImageProcessor(FBO.FBOSpecification spec, Shader shader) {
		this.output = spec == null ? null : new FBO(spec);
		this.shader = shader;
		this.mesh = defaultMesh();
		init();
	}

	public ImageProcessor(Shader shader) {
		this.output = null;
		this.shader = shader;
		this.mesh = defaultMesh();
		init();
	}

	protected void init() {
		if (output != null) {
			this.output.scene
				   .attach(new Guard(shader, new Function<Integer, Boolean>() {
					   @Override
					   public Boolean apply(Integer x) {
						   return !disabledPlane;
					   }

					   @Override
					   public String toString() {
						   return "if plane is not disabled, currently "+(!disabledPlane);
					   }
				   }));
		}
		shader.attach(new Guard(mesh, new Function<Integer, Boolean>() {
			@Override
			public Boolean apply(Integer x) {
				return !disabledPlane;
			}

			@Override
			public String toString() {
				return "if plane is not disabled, currently "+(!disabledPlane);
			}
		}));
	}


	public boolean disabled = false;
	public boolean disabledPlane = false;

	/**
	 * returns a Perform that will draw this when inserted into the internalScene graph (as in canvas.internalScene.updateMyProcessor = proc.getDraw(4) )
	 */
	public Perform getDraw() {
		return new Perform() {

			@Override
			public boolean perform(int pass) {
				if (disabled) return true;

				if (output != null) if (pass == 0) {
					output.draw();
				} else return shader.perform(pass);
				return true;
			}

			@Override
			public int[] getPasses() {
				if (output != null) return new int[]{0};
				else return shader.getPasses();
			}

			@Override
			public String toString() {
				if (output != null) return "IP("+ InverseDebugMapping.describe(ImageProcessor.this)+") draws " + InverseDebugMapping.describe(output);
				else return "IP("+InverseDebugMapping.describe(ImageProcessor.this)+") shades " + InverseDebugMapping.describe(shader);
			}
		};
	}

	public void disable() {
		disabled = true;
	}

	public void enable() {
		disabled = false;
	}

	public void disablePlane() {
		disabledPlane = true;
	}

	public void enablePlane() {
		disabledPlane = false;
	}


	/**
	 * a mesh that's a -1 to 1 rectangle, with texture coords 0->1 in aux '2'
	 */
	static public BaseMesh defaultMesh() {
		BaseMesh t = BaseMesh.triangleList(4, 2);
		MeshBuilder tt = new MeshBuilder(t);
		tt.open();
		tt.aux(2, 0, 0);
		tt.v(-1, -1, 0);
		tt.aux(2, 1, 0);
		tt.v(1, -1, 0);
		tt.aux(2, 1, 1);
		tt.v(1, 1, 0);
		tt.aux(2, 0, 1);
		tt.v(-1, 1, 0);
		tt.e(0, 1, 2);
		tt.e(0, 2, 3);
		tt.close();
		return t;
	}


}
