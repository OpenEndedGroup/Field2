package field.graphics.util.onsheetui

import field.graphics.FLine
import field.graphics.StandardFLineDrawing.*
import field.linalg.Vec2
import field.linalg.Vec4
import field.utility.Cached
import field.utility.Rect
import field.utility.plusAssign
import field.utility.xw
import fieldbox.boxes.Box
import fieldbox.boxes.FLineDrawing.frameDrawing
import java.util.function.BiFunction
import java.util.function.Function

class SimpleConnection {
    fun middleXYToMiddleY(name: String, a: Box, b: Box, col: Vec4) {

        val f = Cached<Box, Any, FLine>(BiFunction<Box, FLine, FLine> { _: Box?, _: FLine? ->

            val middlea = (a get Box.frame)!!.center

            val rectb = (b get Box.frame)!!

            val q = Math.max(rectb.x.toDouble(), Math.min(rectb.xw.toDouble(), middlea.x));

            val f = FLine().moveTo(middlea).lineTo(q, (rectb.y + rectb.h / 2).toDouble());

            f += pointed to true
            f += color to col
            f += pointSize to 5.0

            f
        }, Function<Box, Any> { _: Box -> field.utility.Pair((a get Box.frame) , (b get Box.frame)) })

        a.properties.putToMap(frameDrawing, name, f);
    }

    fun leftXToMiddleY(name: String, a: Box, b: Box, col: Vec4) {

        val f = Cached<Box, Any, FLine>(BiFunction<Box, FLine, FLine> { _: Box?, _: FLine? ->

            val middlea = Vec2( (a get Box.frame)!!.x.toDouble(), (a get Box.frame)!!.center.y)

            val rectb = (b get Box.frame)!!

            val q = Math.max(rectb.x.toDouble(), Math.min(rectb.xw.toDouble(), middlea.x));

            val f = FLine().moveTo(middlea).lineTo(q, (rectb.y + rectb.h / 2).toDouble());

            f += pointed to true
            f += color to col
            f += pointSize to 5.0

            f
        }, Function<Box, Any> { _: Box -> field.utility.Pair((a get Box.frame) , (b get Box.frame)) })

        a.properties.putToMap(frameDrawing, name, f);
    }
}

private val Rect.center: Vec2
    get() {
        return Vec2((this.x + this.w / 2).toDouble(), (this.y + this.h / 2).toDouble())
    }

