package field.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * An OpenGL Texture/Buffer/Object. This is like a texture (you can randomly read from it) but like a buffer (it can be really large).
 */
public class TextureBuffer extends BaseScene<TextureBuffer.State> implements Scene.Perform, OffersUniform<Integer> {

	static public class State extends BaseScene.Modifiable {
		int textureName;
	}

	SimpleArrayBuffer buffer;
	TextureBufferSpecification specification;

	static public class TextureBufferSpecification {
		int elements;
		int dimension;
		int type;
		int unit;
		FloatBuffer customStorage;

		static public TextureBufferSpecification float1(int unit, int width) {
			TextureBufferSpecification s = new TextureBufferSpecification();
			s.elements = width;
			s.dimension = 1;
			s.type = GL30.GL_R32F;
			s.customStorage = null;
			s.unit = unit;
			return s;
		}

		static public TextureBufferSpecification float2(int unit, int width) {
			TextureBufferSpecification s = new TextureBufferSpecification();
			s.elements = width;
			s.dimension = 2;
			s.type = GL30.GL_RG32F;
			s.customStorage = null;
			s.unit = unit;
			return s;
		}

		static public TextureBufferSpecification float3(int unit, int width) {
			TextureBufferSpecification s = new TextureBufferSpecification();
			s.elements = width;
			s.dimension = 3;
			s.type = GL30.GL_RGB32F;
			s.customStorage = null;
			s.unit = unit;
			return s;
		}

		static public TextureBufferSpecification float4( int unit, int width) {
			TextureBufferSpecification s = new TextureBufferSpecification();
			s.elements = width;
			s.dimension = 4;
			s.type = GL30.GL_RGBA32F;
			s.customStorage = null;
			s.unit = unit;
			return s;
		}

		static public TextureBufferSpecification float1(int unit, FloatBuffer storage) {
			TextureBufferSpecification s = new TextureBufferSpecification();
			s.dimension = 1;
			s.elements = storage.capacity()/s.dimension;
			s.type = GL30.GL_R32F;
			s.customStorage = storage;
			return s;
		}

		static public TextureBufferSpecification float2(int unit, FloatBuffer storage) {
			TextureBufferSpecification s = new TextureBufferSpecification();
			s.dimension = 2;
			s.elements = storage.capacity()/s.dimension;
			s.type = GL30.GL_RG32F;
			s.customStorage = storage;
			s.unit = unit;
			return s;
		}

		static public TextureBufferSpecification float3(int unit, FloatBuffer storage) {
			TextureBufferSpecification s = new TextureBufferSpecification();
			s.dimension = 3;
			s.type = GL30.GL_RGB32F;
			s.elements = storage.capacity()/s.dimension;
			s.customStorage = storage;
			s.unit = unit;
			return s;
		}

		static public TextureBufferSpecification float4( int unit, FloatBuffer storage) {
			TextureBufferSpecification s = new TextureBufferSpecification();
			s.dimension = 4;
			s.type = GL30.GL_RGBA32F;
			s.elements = storage.capacity()/s.dimension;
			s.customStorage = storage;
			s.unit = unit;
			return s;
		}
	}

	public TextureBuffer(TextureBufferSpecification spec) throws NoSuchFieldException, IllegalAccessException {
		this.specification = spec;
		this.buffer = new SimpleArrayBuffer(spec.elements, GL31.GL_TEXTURE_BUFFER, -1, specification.dimension, 0);
		if (specification.customStorage != null) this.buffer.setCustomStorage(specification.customStorage);
	}

	public SimpleArrayBuffer getBuffer() {
		return buffer;
	}

	@Override
	protected boolean perform0() {

		GraphicsContext.checkError(() -> "before perform0 of TextureBuffer");
		buffer.clean(specification.elements);
		GraphicsContext.checkError(() -> "after clean");
		State s = GraphicsContext.get(this);

		glActiveTexture(GL_TEXTURE0 + specification.unit);
		GraphicsContext.checkError(() -> "after active texture");
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, s.textureName);
		GraphicsContext.checkError(() -> "after bind");

		return false;
	}

	@Override
	protected TextureBuffer.State setup() {

		State s = new State();
		GraphicsContext.checkError(() -> "before genTextures");
		s.textureName = glGenTextures();
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, s.textureName);
		GraphicsContext.checkError(() -> "before first clean");
		buffer.clean(specification.elements);
		GraphicsContext.checkError(() -> "after clean / before texBuffer");
		GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, specification.type, buffer.getOpenGLNameInCurrentContext());
		GraphicsContext.checkError(() -> "after texBuffer");

		return s;
	}

	@Override
	protected void deallocate(TextureBuffer.State s) {

	}

	@Override
	public Integer getUniform() {
		return specification.unit;
	}

	@Override
	public int[] getPasses() {
		return new int[]{-1};
	}

	public void dirty()
	{
		buffer.mod++;
	}
}
