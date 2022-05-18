package field.graphics.util

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.lwjgl.glfw.GLFW
import java.awt.event.KeyEvent

//import org.cef.browser.CefBrowser_N;
class KeyEventMapping {
    var keyCodes: BiMap<Int, Int> = HashBiMap.create()
    var glfwModifiers: MutableSet<Int> = LinkedHashSet()
    var forceTyped: Map<Int, Char> = LinkedHashMap()
    fun translateCode(glfwcode: Int): Int? {
        return keyCodes.inverse()[glfwcode]
    }

    fun isModifier(glfwcode: Int): Boolean {
        return glfwModifiers.contains(glfwcode)
    }

    fun isForcedTyped(glfwcode: Int): Char? {
        return forceTyped[glfwcode]
    }

    companion object {
        var lookupMap: HashMap<Int, String>? = null
        @JvmStatic
		fun lookup(num: Int): String? {
            if (lookupMap == null) {
                lookupMap = HashMap()
                val ff = GLFW::class.java.fields
                for (fff in ff) {
                    try {
                        if (fff.name
                                .startsWith("GLFW_KEY")
                        ) lookupMap!![(fff[null] as Number).toInt()] = fff.name
                    } catch (e: IllegalAccessException) {
                    } catch (e: ClassCastException) {
                    }
                }
            }
            return lookupMap!![num]
        }
    }

    init {
        keyCodes[KeyEvent.VK_ENTER] = GLFW.GLFW_KEY_ENTER
        keyCodes[KeyEvent.VK_UP] = GLFW.GLFW_KEY_UP
        keyCodes[KeyEvent.VK_DOWN] = GLFW.GLFW_KEY_DOWN
        keyCodes[KeyEvent.VK_LEFT] = GLFW.GLFW_KEY_LEFT
        keyCodes[KeyEvent.VK_RIGHT] = GLFW.GLFW_KEY_RIGHT
        keyCodes[KeyEvent.VK_PAGE_UP] = GLFW.GLFW_KEY_PAGE_UP
        keyCodes[KeyEvent.VK_PAGE_DOWN] = GLFW.GLFW_KEY_PAGE_DOWN
        keyCodes[KeyEvent.VK_ESCAPE] = GLFW.GLFW_KEY_ESCAPE
        keyCodes[KeyEvent.VK_END] = GLFW.GLFW_KEY_END
        keyCodes[KeyEvent.VK_HOME] = GLFW.GLFW_KEY_HOME
        keyCodes[KeyEvent.VK_BACK_SPACE] = GLFW.GLFW_KEY_BACKSPACE
        keyCodes[KeyEvent.VK_DELETE] = GLFW.GLFW_KEY_DELETE
        keyCodes[KeyEvent.VK_TAB] = GLFW.GLFW_KEY_TAB
        keyCodes[KeyEvent.VK_CONTROL] = GLFW.GLFW_KEY_LEFT_CONTROL
        //		keyCodes.put(KeyEvent.VK_CONTROL, GLFW_KEY_RIGHT_CONTROL);
        keyCodes[KeyEvent.VK_SHIFT] = GLFW.GLFW_KEY_LEFT_SHIFT
        keyCodes[KeyEvent.VK_ALT] = GLFW.GLFW_KEY_LEFT_ALT
        keyCodes[KeyEvent.VK_META] = GLFW.GLFW_KEY_LEFT_SUPER
        //		keyCodes.put(KeyEvent.VK_QUOTE, GLFW_KEY_APOSTROPHE);
//		keyCodes.put(KeyEvent.VK_QUOTEDBL, GLFW_KEY_APOSTROPHE);
        keyCodes[KeyEvent.VK_0] = GLFW.GLFW_KEY_0
        keyCodes[KeyEvent.VK_Z] = GLFW.GLFW_KEY_Z
        keyCodes[KeyEvent.VK_C] = GLFW.GLFW_KEY_C
        keyCodes[KeyEvent.VK_G] = GLFW.GLFW_KEY_G
        keyCodes[KeyEvent.VK_A] = GLFW.GLFW_KEY_A
        keyCodes[KeyEvent.VK_SLASH] = GLFW.GLFW_KEY_SLASH
        keyCodes[KeyEvent.VK_BACK_SLASH] = GLFW.GLFW_KEY_BACKSLASH
        keyCodes[KeyEvent.VK_V] = GLFW.GLFW_KEY_V
        keyCodes[KeyEvent.VK_X] = GLFW.GLFW_KEY_X
        keyCodes[KeyEvent.VK_I] = GLFW.GLFW_KEY_I
        keyCodes[KeyEvent.VK_1] = GLFW.GLFW_KEY_1
        keyCodes[KeyEvent.VK_2] = GLFW.GLFW_KEY_2
        keyCodes[KeyEvent.VK_3] = GLFW.GLFW_KEY_3
        keyCodes[KeyEvent.VK_4] = GLFW.GLFW_KEY_4
        keyCodes[KeyEvent.VK_5] = GLFW.GLFW_KEY_5
        keyCodes[KeyEvent.VK_6] = GLFW.GLFW_KEY_6
        keyCodes[KeyEvent.VK_7] = GLFW.GLFW_KEY_7
        keyCodes[KeyEvent.VK_8] = GLFW.GLFW_KEY_8
        keyCodes[KeyEvent.VK_9] = GLFW.GLFW_KEY_9
        keyCodes[KeyEvent.VK_SPACE] = GLFW.GLFW_KEY_SPACE
        keyCodes[KeyEvent.VK_PERIOD] = GLFW.GLFW_KEY_PERIOD
        glfwModifiers.add(GLFW.GLFW_KEY_LEFT_CONTROL)
        glfwModifiers.add(GLFW.GLFW_KEY_RIGHT_CONTROL)
        glfwModifiers.add(GLFW.GLFW_KEY_LEFT_SHIFT)
        glfwModifiers.add(GLFW.GLFW_KEY_RIGHT_SHIFT)
        glfwModifiers.add(GLFW.GLFW_KEY_LEFT_ALT)
        glfwModifiers.add(GLFW.GLFW_KEY_RIGHT_ALT)
        glfwModifiers.add(GLFW.GLFW_KEY_LEFT_SUPER)
        glfwModifiers.add(GLFW.GLFW_KEY_RIGHT_SUPER)

//		forceTyped.put(GLFW_KEY_DELETE, (char)0x7f);
    }
}