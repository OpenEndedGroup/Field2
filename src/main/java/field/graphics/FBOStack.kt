package field.graphics

class FBOStack(val s: FBO.FBOSpecification, val depth: Int) : BaseScene<FBOStack.State>() {

    inner class State : Modifiable() {

    }

    val stack = Array<FBO>(depth) { FBO(s) }

    val scene = Scene()

    init {
        stack.forEach {
            it.scene = scene
        }
    }

    var read = 0
    var write = depth - 1

    fun draw(): Boolean {
        val b = stack[write].draw()

        write = (write + 1) % depth
        read = (read + 1) % depth

        return b
    }

    fun getReadFBO(): FBO {
        return stack[read]
    }

    override fun getPasses(): IntArray {
        return intArrayOf(-1, 1)
    }

    override fun perform0(): Boolean {
        return stack[read].perform(-1)
    }

    override fun perform1(): Boolean {
        return stack[read].perform(1)
    }

    override fun setup(): State {

        return State()
    }

    override fun deallocate(s: State) {
    }


}