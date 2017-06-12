package field.graphics.vr

import field.graphics.Scene

class OpenVRDrawTarget {

    internal var warmUp = 0

    fun init(w: Scene) {
        w.attach("__initopenvr__", { pass ->

            if (warmUp++ < 5) return@w.attach true
        } as Scene.Perform)
    }
}
