package field.graphics

import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL15.glDeleteBuffers
import java.nio.FloatBuffer

import org.lwjgl.opengl.GL15.glGenBuffers
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * this uses a uniform buffer object to provide an array of vec4 of some kind (note that, std140 layout rules make all array elements smaller than a vec4 take up the space of a vec4). This doesn't expose the complete flexibility of a UBO, just, perhaps, the most common case
 */
class UniformVec4Object : BaseScene<UniformVec4Object.State> {


    class State : BaseScene.Modifiable() {
        internal var buffer: Int = 0
    }

    val numVec4: Int
    private val data: FloatBuffer
    val binding: Int

    constructor(binding: Int, numVec4: Int) {
        this.numVec4 = numVec4
        this.data = ByteBuffer.allocateDirect(4 * numVec4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        this.binding = binding;
    }

    constructor(binding: Int, data: FloatBuffer) {
        this.numVec4 = data.capacity() / 4
        this.data = data
        this.binding = binding;
    }

    override fun setup(): State {

        val s = State()

        s.buffer = glGenBuffers()
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, s.buffer);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, 4L * 4L * numVec4, GL15.GL_STREAM_DRAW);
        if (data != null)
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

        return s
    }

    fun set(index: Int, value: Vec4) {
        data[index * 4] = value
        mod++
    }

    fun set(index: Int, value: Vec3) {
        data[index * 4] = value
        mod++
    }

    fun set(index: Int, value: Vec2) {
        data[index * 4] = value
        mod++
    }

    fun set(index: Int, x: Number) {
        data[index * 4] = x
        mod++
    }

    fun floats(readOnly :Boolean = false) : FloatBuffer
    {
        if (!readOnly) mod++
        return data
    }

    override fun upload(s: State?): Int {
        val e1 = GL11.glGetError()
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, s!!.buffer);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        val e2 = GL11.glGetError()
        if (e1!=0 || e2!=0)
            println(" error on upload $e1 $e2")

        return mod
    }

    override fun perform0(): Boolean {
        val s = GraphicsContext.get<State>(this) { setup() }

        val e1 = GL11.glGetError()
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, binding, s.buffer)
        val e2 = GL11.glGetError()

        if (e1!=0 || e2!=0)
            println(" error on bind $e1 $e2")

        return true;
    }

    override fun getPasses(): IntArray {
        return intArrayOf(-1,1)
    }

    override fun perform1(): Boolean {

        return true;
    }

    override fun deallocate(s: State?) {
        if (s != null) {
            glDeleteBuffers(s.buffer)
            s.buffer = -1
        }
    }

}

private operator fun FloatBuffer.set(i: Int, value: Vec4) {
    this.put(i + 0, value.x.toFloat())
    this.put(i + 1, value.y.toFloat())
    this.put(i + 2, value.z.toFloat())
    this.put(i + 3, value.w.toFloat())
}

private operator fun FloatBuffer.set(i: Int, value: Vec3) {
    this.put(i + 0, value.x.toFloat())
    this.put(i + 1, value.y.toFloat())
    this.put(i + 2, value.z.toFloat())
}

private operator fun FloatBuffer.set(i: Int, value: Vec2) {
    this.put(i + 0, value.x.toFloat())
    this.put(i + 1, value.y.toFloat())
}

private operator fun FloatBuffer.set(i: Int, value: Number) {
    this.put(i + 0, value.toFloat())
}

