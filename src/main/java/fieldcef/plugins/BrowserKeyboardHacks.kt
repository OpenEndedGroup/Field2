package fieldcef.plugins

import field.app.RunLoop
import field.graphics.Window
import field.graphics.util.KeyEventMapping
import field.utility.Log
import fieldagent.Main
import fieldbox.boxes.Keyboard.Hold
import org.cef.browser.CefBrowser
import java.awt.Component
import java.awt.Point
import java.awt.event.KeyEvent


/**
 *
 */
class BrowserKeyboardHacks(private val target: CefBrowser) {
    var foced = 0 // tab
    var keysDown: HashSet<Int> = LinkedHashSet()
    var charsDown = HashSet<Char>()
    private val mapper = KeyEventMapping()

    // AWT's MouseEvent constructor throws an NPE unless you give it a component.
    var component: Component = object : Component() {
        override fun getLocationOnScreen(): Point {
            return Point(0, 0)
        }
    }
    var lastEventAt: Long = 0
    fun onKeyDown(e: Window.Event<Window.KeyboardState>, key: Int): Hold? {
        val c = HashSet(e.after.charsDown.values)
        c.removeAll(e.before.charsDown.values)
        val m = HashSet(e.after.keysDown)
        m.removeAll(keysDown)
        Log.log("keyboard2") { "onKeyDown new keys:$m new chars:$c" }
        if (m.size == 0 && e.after.charsDown.size == 0 && e.before.charsDown.size == 0 && RunLoop.tick - lastEventAt > 0) {
            // why send the event? its a keyboard repeat for a non-char character, that's why
            m.addAll(e.after.keysDown)
        } else {
            c.removeAll(charsDown)
            m.removeAll(keysDown)
        }
        Log.log("keyboard2") { "onKeyDown new2 keys:$m new chars:$c" }
        if (m.size == 0) {
            return null
        }
        lastEventAt = RunLoop.tick
        // now we have m new keypresses to deal with and c new characters
        var mod = if (e.after.isAltDown) KeyEvent.ALT_DOWN_MASK else 0
        mod = mod or if (e.after.isShiftDown) KeyEvent.SHIFT_DOWN_MASK else 0
        mod = mod or if (e.after.isControlDown) KeyEvent.CTRL_DOWN_MASK else 0
        mod = mod or if (e.after.isSuperDown) KeyEvent.META_DOWN_MASK else 0
        val fmod = mod
        for (mm in m) {
            if (mapper.isForcedTyped(mm) != null && mod != 0) {
                val ke =
                    KeyEvent(component, KeyEvent.KEY_TYPED, 0, mod, KeyEvent.VK_UNDEFINED, mapper.isForcedTyped(mm)!!)
                e.properties.put(Window.consumed, true)
                println(" sending key typed directly, because the key is force typed")
                target.sendKeyEvent(ke)
                keysDown.add(mm)
                break
            }
            val translated = mapper.translateCode(mm)
            keysDown.add(mm)
            if (translated != null) {
                if (Main.os == Main.OS.windows) {

                    if (e.after.windowsScancodes.size > 0) {

                    // WINDOWS ONLY
//                    KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, 0, translated, (char) translated.intValue());
                    val ke = KeyEvent(component, KeyEvent.KEY_PRESSED, 0, fmod, translated, translated.toInt().toChar())
                    try {
                        val f = ke.javaClass.getDeclaredField("scancode")
                        f.isAccessible = true
                        //                        f.setInt(ke, );
                        foced = e.after.windowsScancodes[e.after.windowsScancodes.size - 1]
                        println("\n\n\n foced $foced \n\n\n")
                        if (foced > 256) foced -= 256
                        f.setInt(ke, foced)
                    } catch (ex: NoSuchFieldException) {
                        ex.printStackTrace()
                    } catch (illegalAccessException: IllegalAccessException) {
                        illegalAccessException.printStackTrace()
                    }
                    e.properties.put(Window.consumed, true)
                    println(" >> send $ke")
                    target.sendKeyEvent(ke)
                    val ke2 = KeyEvent(component, KeyEvent.KEY_RELEASED, 0, 0, translated, translated.toInt().toChar())
                    try {
                        val f = ke.javaClass.getDeclaredField("scancode")
                        f.isAccessible = true
                        //                        f.setInt(ke, e.after.windowsScancodes.get(e.after.windowsScancodes.size() - 1));
                        if (foced > 256) foced -= 256
                        f.setInt(ke2, foced)
                    } catch (ex: NoSuchFieldException) {
                        ex.printStackTrace()
                    } catch (illegalAccessException: IllegalAccessException) {
                        illegalAccessException.printStackTrace()
                    }
                    e.properties.put(Window.consumed, true)
                    target.sendKeyEvent(ke2)
                    break
                } else {
                }
                }
                else
                {
                    var ke =
                        KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, translated, translated.toChar() )
                    e.properties.put(Window.consumed, true)
                    target.sendKeyEvent(ke)

                    ke = KeyEvent(component, KeyEvent.KEY_RELEASED, 0, fmod, translated, translated.toChar())
                    e.properties.put(Window.consumed, true)
                    target.sendKeyEvent(ke)
                }
            }
        }
        for (cc in c) {
            charsDown.add(cc)
        }
        return Hold { e2: Window.Event<Window.KeyboardState?>?, term: Boolean ->
            if (term) {
//				Log.log("keyboard2", "onKeyUp releasing keys:" + m + " chars:" + c);
                for (mm in m) {
                    keysDown.remove(mm)
                    val translated = mapper.translateCode(mm)
                    if (translated != null) {
                    }
                }
                for (cc in c) {
                    charsDown.remove(cc)
                }
            }
            !term
        }
    }

    fun onCharDown(e: Window.Event<Window.KeyboardState>, key: Char) {
        val c = HashSet(e.after.charsDown.values)
        c.removeAll(e.before.charsDown.values)
        val m = HashSet(e.after.keysDown)
        m.removeAll(keysDown)

        // now we have m new keypresses to deal with and c new characters
        var mod = if (e.after.isAltDown) KeyEvent.ALT_DOWN_MASK else 0
        mod = mod or if (e.after.isShiftDown) KeyEvent.SHIFT_DOWN_MASK else 0
        mod = mod or if (e.after.isControlDown) KeyEvent.CTRL_DOWN_MASK else 0
        mod = mod or if (e.after.isSuperDown) KeyEvent.META_DOWN_MASK else 0
        val fmod = mod


//		Log.log("keyboard2", "onCharDown char :" + c + " with mod " + fmod+"/"+key+" "+(int)key);
        if (key.code > 63000) {
//			Log.log("keyboard2", "ignoring extended keypress");
            return
        }
        for (cc in c) {
            val ke = KeyEvent(component, KeyEvent.KEY_TYPED, 0, mod, KeyEvent.VK_UNDEFINED, cc)
            e.properties.put(Window.consumed, true)
            if (e.after.isAltDown) continue
            if (e.after.isControlDown) continue
            println(" >> TYPED $cc / $mod")
            target.sendKeyEvent(ke)
        }
    }
}