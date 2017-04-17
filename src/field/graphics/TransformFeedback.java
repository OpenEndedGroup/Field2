package field.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Created by marc on 4/5/17.
 */
public class TransformFeedback extends BaseScene<TransformFeedback.State> implements Scene.Perform{

	private final String target;
	private final int size;
	private final Primitive primitive;

	// move this elsewhere
	enum Primitive
	{
		triangles(3, GL_TRIANGLES), points(1, GL_POINTS), lines(2, GL_LINES);

		public final int numVertex;
		public final int glName;

		Primitive(int numVertex, int glName)
		{
			this.numVertex = numVertex;
			this.glName = glName;
		}
	}

	static public class State extends Modifiable
	{
		public int tbo;
		public int query;
		public FloatBuffer cpuCopy;
		public int num;
	}


	public TransformFeedback(String name, int size, Primitive primitive)
	{
		this.target = name;
		this.size = size;
		this.primitive = primitive;
	}

	// the typical case for geometry shaders
	public TransformFeedback(String name, int size)
	{
		this(name, size, Primitive.triangles);
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1, 1};
	}

	State lastState;

	public int getNumWritten()
	{
		if (lastState==null) throw new NullPointerException(" transform feedback has not run yet");
		return lastState.num;
	}

	@Override
	protected boolean perform0() {

		State s = GraphicsContext.get(this, () -> setup());

		lastState = s;
		Integer shaderName = GraphicsContext.getContext().stateTracker.shader.get();
		if (shaderName==0)
		{
			System.out.println(" no active shader associated with TransformFeedback "+target);
		}
		else {
			glTransformFeedbackVaryings(shaderName, this.target, GL_INTERLEAVED_ATTRIBS);
			glBindBufferBase(GL_TRANSFORM_FEEDBACK_BUFFER, 0, s.tbo);
			glBeginTransformFeedback(primitive.glName);
			glBeginQuery(GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN, s.query);
		}

		return true;
	}

	@Override
	protected boolean perform1() {
		glEndTransformFeedback();
		State s = GraphicsContext.get(this, () -> setup());
		glEndQuery(GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN);

		//stall, right here, will need to double buffer once we get this working
		s.cpuCopy.reset();
		glGetBufferSubData(GL_TRANSFORM_FEEDBACK_BUFFER, 0, s.cpuCopy);

		// and here
		int[] res = {0};
		glGetQueryObjectuiv(s.query, GL_QUERY_RESULT, res);

		s.num = res[0];


		return true;
	}

	@Override
	protected State setup() {
		State s= new State();

		int query = glGenQueries();

		int tbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, tbo);
		glBufferData(GL_ARRAY_BUFFER, size*primitive.numVertex, GL_STATIC_READ);
		s.tbo = tbo;

		s.cpuCopy = ByteBuffer.allocateDirect(4*size*primitive.numVertex).order(ByteOrder.nativeOrder()).asFloatBuffer();

		return s;
	}

	@Override
	protected void deallocate(TransformFeedback.State s) {

	}
}
