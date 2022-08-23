package field.graphics.gltf

import de.javagl.jgltf.model.GltfConstants
import de.javagl.jgltf.viewer.GlContext
import field.graphics.GraphicsContext
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL20.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.charset.Charset
import java.util.logging.Logger


/**
 * Implementation of a [GlContext] based on LWJGL
 */
class FieldGLContext : GlContext {
    /**
     * A buffer that will be used temporarily for the values of
     * integer uniforms. This is a direct buffer that is created
     * and resized as necessary in [.putIntBuffer]
     */
    private var uniformIntBuffer: IntBuffer? = null

    /**
     * A buffer that will be used temporarily for the values of
     * float uniforms. This is a direct buffer that is created
     * and resized as necessary in [.putFloatBuffer]
     */
    private var uniformFloatBuffer: FloatBuffer? = null

    /**
     * Put the given values into a direct IntBuffer and return it.
     * The returned buffer may always be a slice of the same instance.
     * This method is supposed to be called only from the OpenGL thread.
     *
     * @param value The value
     * @return The IntBuffer
     */
    private fun putIntBuffer(value: IntArray): IntBuffer? {
        val total = value.size
        if (uniformIntBuffer == null || uniformIntBuffer!!.capacity() < total) {
            uniformIntBuffer = ByteBuffer
                .allocateDirect(total * Integer.BYTES)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
        }
        uniformIntBuffer!!.position(0)
        uniformIntBuffer!!.limit(uniformIntBuffer!!.capacity())
        uniformIntBuffer!!.put(value)
        uniformIntBuffer!!.flip()
        return uniformIntBuffer
    }

    /**
     * Put the given values into a direct IntBuffer and return it.
     * The returned buffer may always be a slice of the same instance.
     * This method is supposed to be called only from the OpenGL thread.
     *
     * @param value The value
     * @return The IntBuffer
     */
    private fun putFloatBuffer(value: FloatArray): FloatBuffer? {
        val total = value.size
        if (uniformFloatBuffer == null || uniformFloatBuffer!!.capacity() < total) {
            uniformFloatBuffer = ByteBuffer
                .allocateDirect(total * java.lang.Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        }
        uniformFloatBuffer!!.position(0)
        uniformFloatBuffer!!.limit(uniformFloatBuffer!!.capacity())
        uniformFloatBuffer!!.put(value)
        uniformFloatBuffer!!.flip()
        return uniformFloatBuffer
    }

    override fun createGlProgram(
        v: String, f: String
    ): Int? {
        GraphicsContext.checkErrorStackTrace { "check" }

        var vertexShaderSource = "#version 330 \n" + v
        var fragmentShaderSource = "#version 330 \n out vec4 __outColor;" + f

        println("vertex source :\n${vertexShaderSource}")
        println("fragment source :\n${fragmentShaderSource}")

        var n = 0
        vertexShaderSource = Regex("attribute (.*?);").replace(vertexShaderSource) {
            val e = it.groups[1]
            print(n)
            "layout (location = ${n++}) in " + e!!.value + ";"
        }

        vertexShaderSource = Regex("varying (.*?);").replace(vertexShaderSource) {
            val e = it.groups[1]
            "out " + e!!.value + ";"
        }

        fragmentShaderSource = Regex("varying (.*?);").replace(fragmentShaderSource) {
            val e = it.groups[1]
            "in " + e!!.value + ";"
        }

        fragmentShaderSource = Regex("gl_FragColor").replace(fragmentShaderSource) {
            "__outColor"
        }

        fragmentShaderSource = Regex("texture2D").replace(fragmentShaderSource) {
            "texture"
        }




        if (vertexShaderSource == null) {
            logger.warning("The vertexShaderSource is null")
            return null
        }
        if (fragmentShaderSource == null) {
            logger.warning("The fragmentShaderSource is null")
            return null
        }
        logger.fine("Creating vertex shader...")
        val glVertexShader = createGlShader(GL20.GL_VERTEX_SHADER, vertexShaderSource)
        if (glVertexShader == null) {
            logger.warning("Creating vertex shader FAILED")
            return null
        }
        logger.fine("Creating vertex shader DONE")
        logger.fine("Creating fragment shader...")
        val glFragmentShader = createGlShader(GL20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        if (glFragmentShader == null) {
            logger.warning("Creating fragment shader FAILED")
            return null
        }
        logger.fine("Creating fragment shader DONE")
        val glProgram = GL20.glCreateProgram()
        GL20.glAttachShader(glProgram, glVertexShader)
        GL20.glDeleteShader(glVertexShader)
        GL20.glAttachShader(glProgram, glFragmentShader)
        GL20.glDeleteShader(glFragmentShader)
        GL20.glLinkProgram(glProgram)

        // we can't validate this program until we have bound a VAO
        GL20.glValidateProgram(glProgram)
        val validateStatus: Int = glGetProgrami(glProgram, GL20.GL_VALIDATE_STATUS)
        if (validateStatus != GL11.GL_TRUE) {
            printProgramLogInfo(glProgram)
            return null
        }
        GraphicsContext.checkErrorStackTrace { "check" }
        return glProgram
    }

    /**
     * Creates an OpenGL shader with the given type, from the given source
     * code, and returns the GL shader object. If the shader cannot be
     * compiled, then `null` will be returned.
     *
     * @param shaderType The shader type
     * @param shaderSource The shader source code
     * @return The GL shader
     */
    private fun createGlShader(shaderType: Int, shaderSource: String): Int {
        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            val glShader = GL20.glCreateShader(shaderType)
            GL20.glShaderSource(glShader, shaderSource)
            GL20.glCompileShader(glShader)
            val compileStatus: Int = glGetShaderi(glShader, GL20.GL_COMPILE_STATUS)
            if (compileStatus != GL11.GL_TRUE) {
                printShaderLogInfo(glShader)
            }
            return glShader
        }
        finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun useGlProgram(glProgram: Int) {

        GraphicsContext.checkErrorStackTrace { "check" }
        GL20.glUseProgram(glProgram)
        GraphicsContext.checkErrorStackTrace { "check" }
    }

    override fun deleteGlProgram(glProgram: Int) {
        GraphicsContext.checkErrorStackTrace { "check" }
        GL20.glDeleteProgram(glProgram)
        GraphicsContext.checkErrorStackTrace { "check" }
    }

    override fun enable(states: Iterable<Number>) {
        GraphicsContext.checkErrorStackTrace { "check" }
        if (states != null) {
            for (state in states) {
                if (state != null) {
                    println("ENABLE $state")
                    GL11.glEnable(state.toInt())
                }
            }
        }
        GraphicsContext.checkErrorStackTrace { "check" }
    }

    override fun disable(states: Iterable<Number>) {
        GraphicsContext.checkErrorStackTrace { "check" }
        if (states != null) {
            for (state in states) {
                if (state != null) {
                    println("DISABLE $state")
                    GL11.glDisable(state.toInt())
                }
            }
        }
        GraphicsContext.checkErrorStackTrace { "check" }
    }

    override fun getUniformLocation(glProgram: Int, uniformName: String): Int {

//        return 0

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL20.glUseProgram(glProgram)
            var loc = GL20.glGetUniformLocation(glProgram, uniformName)
            return loc
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun getAttributeLocation(glProgram: Int, attributeName: String): Int {

//        return 0

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL20.glUseProgram(glProgram)
            var loc = GL20.glGetAttribLocation(glProgram, attributeName)
            return loc
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setUniformiv(type: Int, location: Int, count: Int, value: IntArray) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            if (value == null) {
                logger.warning("Invalid uniform value: $value")
                return
            }
            when (type) {
                GltfConstants.GL_INT, GltfConstants.GL_UNSIGNED_INT -> {
                    glUniform1i(location, value[0])
                }

                GltfConstants.GL_INT_VEC2 -> {
                    glUniform2i(location, value[0], value[1])
                }

                GltfConstants.GL_INT_VEC3 -> {
                    glUniform3i(location, value[0], value[1], value[2])
                }

                GltfConstants.GL_INT_VEC4 -> {
                    glUniform4i(location, value[0], value[1], value[2], value[3])
                }

                else -> logger.warning(
                    "Invalid uniform type: " +
                            GltfConstants.stringFor(type)
                )
            }
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setUniformfv(type: Int, location: Int, count: Int, value: FloatArray) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            if (value == null) {
                logger.warning("Invalid uniform value: $value")
                return
            }
            when (type) {
                GltfConstants.GL_FLOAT -> {
                    glUniform1f(location, value[0])
                }

                GltfConstants.GL_FLOAT_VEC2 -> {
                    glUniform2f(location, value[0], value[1])
                }

                GltfConstants.GL_FLOAT_VEC3 -> {
                    glUniform3f(location, value[0], value[1], value[2])
                }

                GltfConstants.GL_FLOAT_VEC4 -> {
                    glUniform4f(location, value[0], value[1], value[2], value[3])
                }

                else -> logger.warning(
                    "Invalid uniform type: " +
                            GltfConstants.stringFor(type)
                )
            }
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setUniformMatrixfv(
        type: Int, location: Int, count: Int, value: FloatArray
    ) {
//        return


        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            if (value == null) {
                logger.warning("Invalid uniform value: $value")
                return
            }
            when (type) {
                GltfConstants.GL_FLOAT_MAT2 -> {
                    val b = putFloatBuffer(value)
                    glUniformMatrix2fv(location, false, b)
                }

                GltfConstants.GL_FLOAT_MAT3 -> {
                    val b = putFloatBuffer(value)
                    glUniformMatrix3fv(location, false, b)
                }

                GltfConstants.GL_FLOAT_MAT4 -> {
                    val b = putFloatBuffer(value)
                    glUniformMatrix4fv(location, false, b)
                }

                else -> logger.warning(
                    "Invalid uniform type: " +
                            GltfConstants.stringFor(type)
                )
            }
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setUniformSampler(location: Int, textureIndex: Int, glTexture: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureIndex)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture)
            GL20.glUniform1i(location, textureIndex)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun createGlVertexArray(): Int {
//
//        return 0

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            return GL30.glGenVertexArrays()
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun deleteGlVertexArray(glVertexArray: Int) {

//        return


        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL30.glDeleteVertexArrays(glVertexArray)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun createGlBufferView(
        target: Int, byteLength: Int, bufferViewData: ByteBuffer
    ): Int {

//        return 0

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            val glBufferView = GL15.glGenBuffers()
            GL15.glBindBuffer(target, glBufferView)
            val data = bufferViewData.slice()
            data.limit(byteLength)
            GL15.glBufferData(target, wrap(bufferViewData), GL15.GL_STATIC_DRAW)
            return glBufferView
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    private fun wrap(bufferViewData: ByteBuffer): ByteBuffer {
        val o = ByteBuffer.allocateDirect(bufferViewData.limit()).order(ByteOrder.nativeOrder())
        o.put(bufferViewData)
        o.clear()
        return o
    }

    override fun createVertexAttribute(
        glVertexArray: Int,
        target: Int, glBufferView: Int, attributeLocation: Int,
        size: Int, type: Int, stride: Int, offset: Int
    ) {
//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL30.glBindVertexArray(glVertexArray)
            GL15.glBindBuffer(target, glBufferView)
            GL20.glVertexAttribPointer(
                attributeLocation, size, type, false, stride, offset.toLong()
            )
            GL20.glEnableVertexAttribArray(attributeLocation)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun updateVertexAttribute(
        glVertexArray: Int,
        target: Int, glBufferView: Int, offset: Int, size: Int, data: ByteBuffer
    ) {
//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL30.glBindVertexArray(glVertexArray)
            GL15.glBindBuffer(target, glBufferView)
            GL15.glBufferSubData(target, offset.toLong(), wrap(data))
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun deleteGlBufferView(glBufferView: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL15.glDeleteBuffers(glBufferView)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun createGlTexture(
        pixelData: ByteBuffer, internalFormat: Int,
        width: Int, height: Int, format: Int, type: Int
    ): Int {

//        return 1

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            val glTexture = GL11.glGenTextures()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture)
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D, 0, internalFormat, width, height,
                0, format, type, wrap(pixelData)
            )
            return glTexture
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setGlTextureParameters(
        glTexture: Int,
        minFilter: Int, magFilter: Int, wrapS: Int, wrapT: Int
    ) {
//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapT)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun deleteGlTexture(glTexture: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glDeleteTextures(glTexture)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun renderIndexed(
        glVertexArray: Int, mode: Int, glIndicesBuffer: Int,
        numIndices: Int, indicesType: Int, offset: Int
    ) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL30.glBindVertexArray(glVertexArray)
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glIndicesBuffer)
            GL11.glDrawElements(mode, numIndices, indicesType, offset.toLong())
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun renderNonIndexed(glVertexArray: Int, mode: Int, numVertices: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL30.glBindVertexArray(glVertexArray)
            GL11.glDrawArrays(mode, 0, numVertices)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setBlendColor(r: Float, g: Float, b: Float, a: Float) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL14.glBlendColor(r, g, b, a)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setBlendEquationSeparate(modeRgb: Int, modeAlpha: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL20.glBlendEquationSeparate(modeRgb, modeAlpha)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setBlendFuncSeparate(
        srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int
    ) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setColorMask(r: Boolean, g: Boolean, b: Boolean, a: Boolean) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glColorMask(r, g, b, a)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setCullFace(mode: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glCullFace(mode)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setDepthFunc(func: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glDepthFunc(func)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setDepthMask(mask: Boolean) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glDepthMask(mask)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setDepthRange(zNear: Float, zFar: Float) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glDepthRange(zNear.toDouble(), zFar.toDouble())
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setFrontFace(mode: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glFrontFace(mode)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setLineWidth(width: Float) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glLineWidth(width)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setPolygonOffset(factor: Float, units: Float) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glPolygonOffset(factor, units)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int) {

//        return

        GraphicsContext.checkErrorStackTrace { "check" }
        try {
            GL11.glScissor(x, y, width, height)
        } finally {
            GraphicsContext.checkErrorStackTrace { "check" }
        }
    }

    /**
     * For debugging: Print shader log info
     *
     * @param id shader ID
     */
    private fun printShaderLogInfo(id: Int) {
        val infoLogString = glGetShaderInfoLog(id)
        if (infoLogString.trim { it <= ' ' }.length > 0) {
            logger.warning("shader log:\n$infoLogString")
        }
    }

    /**
     * For debugging: Print program log info
     *
     * @param id program ID
     */
    private fun printProgramLogInfo(id: Int) {
        val infoLogString = glGetProgramInfoLog(id)
        if (infoLogString.trim { it <= ' ' }.length > 0) {
            logger.warning("program log:\n$infoLogString")
        }
    }

    companion object {
        /**
         * The logger used in this class
         */
        private val logger = Logger.getLogger(FieldGLContext::class.java.name)
    }
}
