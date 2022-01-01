package field.graphics;

import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.glGetNamedBufferSubData;

/**
 * Created by marc on 4/5/17.
 */
public class TransformFeedback extends BaseScene<TransformFeedback.State> implements Scene.Perform {

	final String[] target;
	private final int size;
	private final Primitive primitive;

	// move this elsewhere
	public enum Primitive {
		triangles(3, GL_TRIANGLES), points(1, GL_POINTS), lines(2, GL_LINES);

		public final int numVertex;
		public final int glName;

		Primitive(int numVertex, int glName) {
			this.numVertex = numVertex;
			this.glName = glName;
		}
	}

	static public class State extends Modifiable {
		public int tbo;
		public int query;
		public FloatBuffer cpuCopy;
		public int num;
		boolean relinkRequired = true;
	}


	public TransformFeedback(String[] name, int size, Primitive primitive) {
		this.target = name;
		this.size = size;
		this.primitive = primitive;
	}

	// the typical case for geometry shaders
	public TransformFeedback(String name, int size) {
		this(new String[]{name}, size, Primitive.triangles);
	}

	// the typical case for geometry shaders
	public TransformFeedback(String[] name, int size) {
		this(name, size, Primitive.triangles);
	}

	public Primitive getPrimitive() {
		return primitive;
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1, 1};
	}

	State lastState;

	public int getNumWritten() {
		if (lastState == null) throw new NullPointerException(" transform feedback has not run yet");
		return lastState.num;
	}

	public FloatBuffer getResult() {
		if (lastState == null) throw new NullPointerException(" transform feedback has not run yet");
		return lastState.cpuCopy;
	}

	boolean didNotApply = false;

	@Override
	protected boolean perform0() {

		GraphicsContext.checkError(() -> "entering perform0 of transform feedback");
		State s = GraphicsContext.get(this, () -> setup());
		GraphicsContext.checkError(() -> "A");

		lastState = s;
		Integer shaderName = GraphicsContext.getContext().stateTracker.shader.get();
		if (shaderName == 0) {
			System.out.println(" no active shader associated with TransformFeedback " + target);
			didNotApply = true;
		} else {
			GraphicsContext.checkError(() -> "B");

			glBindBufferBase(GL_TRANSFORM_FEEDBACK_BUFFER, 0, s.tbo);
			GraphicsContext.checkError(() -> "D");
			glBeginTransformFeedback(primitive.glName);
			GraphicsContext.checkError(() -> "E");
			glBeginQuery(GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN, s.query);
			GraphicsContext.checkError(() -> "F");
			didNotApply = false;
		}


		GraphicsContext.checkError(() -> "exiting perform0 of transform feedback");
		return true;
	}

	@Override
	protected boolean perform1() {

		State s = GraphicsContext.get(this, () -> setup());
		if (didNotApply)
		{
			s.num = -1;
			return true;
		}

		glFinish();

		GraphicsContext.checkError(() -> "entering perform1 of transform feedback");
		glEndTransformFeedback();
		GraphicsContext.checkError(() -> "eA");
		glEndQuery(GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN);
		GraphicsContext.checkError(() -> "eB");

		//stall, right here, will need to double buffer once we get this working
		s.cpuCopy.rewind();

		int size = GL15.glGetBufferParameteri(GL_TRANSFORM_FEEDBACK_BUFFER, GL_BUFFER_SIZE);

		glGetBufferSubData(GL_TRANSFORM_FEEDBACK_BUFFER, 0, s.cpuCopy);
		GraphicsContext.checkError(() -> "eC");

		// and here
		int[] res = {0};
		GraphicsContext.checkError(() -> "eD");
		res[0] = glGetQueryObjecti(s.query, GL_QUERY_RESULT);

		s.num = res[0];


		GraphicsContext.checkError(() -> "exiting perform1 of transform feedback");
		return true;
	}

	@Override
	protected State setup() {
		GraphicsContext.checkError(() -> "entering setup of transform feedback");
		State s = new State();

		int query = glGenQueries();

		int tbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, tbo);
		glBufferData(GL_ARRAY_BUFFER, 4*size * primitive.numVertex, GL_STATIC_READ);
		s.tbo = tbo;
		s.query = query;
		s.cpuCopy = ByteBuffer.allocateDirect(4 * size * primitive.numVertex).order(ByteOrder.nativeOrder()).asFloatBuffer();

		GraphicsContext.checkError(() -> "exiting setup of transform feedback");
		return s;
	}

	@Override
	protected void deallocate(TransformFeedback.State s) {

	}
}
