package field.graphics.util.onsheetui

import field.graphics.FLine
import field.graphics.FLinesAndJavaShapes
import field.graphics.StandardFLineDrawing
import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.Rect
import field.utility.Vec2
import field.utility.Vec4
import field.utility.plusAssign
import fieldbox.DefaultMenus
import fieldbox.boxes.*
import fieldbox.boxes.MarkingMenus.*
import fieldbox.boxes.plugins.FLineButton
import fieldbox.boxes.plugins.FileBrowser
import fieldbox.boxes.plugins.xw
import fieldbox.boxes.plugins.yh
import java.awt.BasicStroke
import java.util.function.Consumer
import java.util.function.Supplier

/* reusable FLineInteraction things that do things

 */
class Gizmos() {

    @JvmOverloads
    fun makeCloseBox(box: Box, name: String = "__closebox__", relative: Vec2 = Vec2(0, 0), frame: Rect = Rect(0.0, 0.0, 10.0, 10.0)) {

        val r = mutableListOf<FLine>()

        FLine().roundedRect(frame.x.toDouble(), frame.y.toDouble(), frame.w.toDouble(), frame.h.toDouble(), 7.0).apply {
            this += StandardFLineDrawing.color to Vec4(1.0, 1.0, 1.0, 0.1)
            this += StandardFLineDrawing.filled to true
            this += StandardFLineDrawing.stroked to false
            r += this

            FLineButton.attach(box, this, mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.9, 0.0, 0.25)), mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.0, 0.0, 0.25)), { up, _, _ ->
                if (up) {
                    Callbacks.transition(box, Mouse.isSelected, false, false, Callbacks.onSelect, Callbacks.onDeselect)
                    Callbacks.call(box, Callbacks.onDelete)
                    box.disconnectFromAll()
                }
            })
        }

        val inset = 6.0;

        FLine().moveTo(frame.x.toDouble() + inset, frame.y.toDouble() + inset).lineTo(frame.xw.toDouble() - inset, frame.yh.toDouble() - inset)
                .moveTo(frame.xw.toDouble() - inset, frame.y.toDouble() + inset).lineTo(frame.x.toDouble() + inset, frame.yh.toDouble() - inset).apply {
            this += StandardFLineDrawing.color to Vec4(1.0, 1.0, 1.0, 0.5)
            this += StandardFLineDrawing.thicken to BasicStroke(1f)
            r += this

            FLineButton.attach(box, this, mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.9, 0.0, 0.15)), mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.0, 0.0, 0.5)), { up, _, _ ->

            })
        }

        var id = 0

        r.map { FLineDrawing.boxOrigin(it, relative, box) }.forEach {

            println(" adding ${name} + ${id}")

            box.properties.putToMap(FLineDrawing.lines, name + (id), it)
            box.properties.putToMap(FLineInteraction.interactiveLines, name + (id), it)

            id++
        }
    }

    @JvmOverloads
    fun makeMenuBox(box: Box, name: String = "__menubox__", relative: Vec2 = Vec2(0, 0), frame: Rect = Rect(0.0, 0.0, 10.0, 10.0)) {

        val r = mutableListOf<FLine>()

        FLine().roundedRect(frame.x.toDouble(), frame.y.toDouble(), frame.w.toDouble(), frame.h.toDouble(), 7.0).apply {
            this += StandardFLineDrawing.color to Vec4(1.0, 1.0, 1.0, 0.1)
            this += StandardFLineDrawing.filled to true
            this += StandardFLineDrawing.stroked to false
            r += this

            val spec = MenuSpecification()

            var dragging: Mouse.Dragger? = null

            FLineButton.attach(box, this, mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.9, 0.0, 0.25)), mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.0, 0.0, 0.25)), { up, _, e ->
                if (dragging == null)
                    dragging = runMenu(box, Vec2(e.after.mx, e.after.my), spec)

                val terminated = e.after.buttonsDown.size == 0

                dragging!!.update(e, terminated)

                if (terminated)
                    dragging = null
            }).setUpSemantics(false)
        }

        val inset = 6.0;

        FLine().moveTo(frame.x.toDouble() + inset, frame.y.toDouble() + inset).lineTo(frame.xw.toDouble() - inset, (frame.yh.toDouble() + frame.y.toDouble()) / 2)
                .moveTo(frame.xw.toDouble() - inset, (frame.yh.toDouble() + frame.y.toDouble()) / 2).lineTo(frame.x.toDouble() + inset, frame.yh.toDouble() - inset).apply {
            this += StandardFLineDrawing.color to Vec4(1.0, 1.0, 1.0, 0.5)
            this += StandardFLineDrawing.thicken to BasicStroke(1f)
            r += this

            FLineButton.attach(box, this, mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.9, 0.0, 0.15)), mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.0, 0.0, 0.5)), { up, _, _ ->

            })
        }

        var id = 0

        r.map { FLineDrawing.boxOrigin(it, relative, box) }.forEach {

            println(" adding ${name} + ${id}")

            box.properties.putToMap(FLineDrawing.lines, name + (id), it)
            box.properties.putToMap(FLineInteraction.interactiveLines, name + (id), it)

            id++
        }
    }

    @JvmOverloads
    fun makeToggleBox(box: Box, labelOne: String? = null, labelTwo: String? = null, name: String = "__button__", relative: Vec2 = Vec2(0, 0), frame: Rect = Rect(0.0, 0.0, 10.0, 10.0), oneToTwo: Runnable, twoToOne: Runnable) {

        var state = 1
        var b: Consumer<String>? = null

        b = makeButton(box, labelOne, name, relative, frame, Runnable {
            if (state == 1) {
                state = 2
                oneToTwo.run()
                if (labelTwo != null)
                    b!!.accept(labelTwo)
            } else {
                state = 1
                twoToOne.run()
                if (labelOne != null)
                    b!!.accept(labelOne)
            }
        });
    }

    @JvmOverloads
    fun makeButton(box: Box, label: String? = null, name: String = "__button__", relative: Vec2 = Vec2(0, 0), frame: Rect = Rect(0.0, 0.0, 10.0, 10.0), go: Runnable): Consumer<String> {

        val r = mutableListOf<FLine>()

        val text = box.first<TextDrawing>(TextDrawing.textDrawing, box.both()).get()
        val fs = text.getFontSupport("source-sans-pro-regular-92.fnt")


        val inset = 0.0;

        var ret: Consumer<String> = Consumer<String> {}

        val frame2 = if (label == null) {
            frame
        } else {

            val dim = fs.font.dimensions(label, 0.12f)
            frame.w += dim.x.toFloat() + 1
            frame.h += dim.y.toFloat() + 2.5f
            val f = FLine()
            f.moveTo((frame.x + 2 + frame.w / 2).toDouble(), (frame.y + frame.h / 2 + 30 / 5.0f).toDouble())

            f.attributes.put(StandardFLineDrawing.hasText, true)
            f.attributes.put(StandardFLineDrawing.textScale, 0.12f)
            f.attributes.put<Supplier<Vec4>>(StandardFLineDrawing.color, Vec4(1.0, 1.0, 1.0, 0.5))
            var name = box.properties.getOr(Box.name) { "" }

            f.nodes[f.nodes.size - 1].attributes.put(StandardFLineDrawing.text, label)

            ret = Consumer<String> { n ->
                f.nodes[f.nodes.size - 1].attributes.put(StandardFLineDrawing.text, n)
                f.modify()
                Drawing.dirty(box)
//                Drawing.dirty(box, "__main__")
            }

            r += f
            frame
        }

        FLine().roundedRect(frame2.x.toDouble(), frame2.y.toDouble(), frame2.w.toDouble(), frame2.h.toDouble(), 7.0).apply {
            this += StandardFLineDrawing.color to Vec4(0.25, 0.25, 0.25, 0.5)
            this += StandardFLineDrawing.filled to true
            this += StandardFLineDrawing.stroked to false
            r += this
            FLineButton.attach(box, this, mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.9, 0.0, 0.1)), mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.0, 0.0, 0.25)), { up, _, e ->
                if (up) go.run()
            }).setUpSemantics(true)
        }

        var l1 = FLine().roundedRect(frame2.x.toDouble() + inset, frame2.y.toDouble() + inset, frame2.w.toDouble() - inset * 2, frame2.h.toDouble() - inset * 2, 7.0).apply {
            this += StandardFLineDrawing.color to Vec4(1.0, 1.0, 1.0, 0.1)
            this += StandardFLineDrawing.thicken to BasicStroke(1f)
            r += this

            FLineButton.attach(box, this, mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.9, 0.0, 0.5)), mapOf(StandardFLineDrawing.color.name to Vec4(1.0, 0.0, 0.0, 0.5)), { up, _, _ ->

            })
        }

        val c1 = l1.cursor().setT(FLinesAndJavaShapes.closestT(l1, Vec3(0.0, 0.0, 0.0))).position()

        val boxFrame = (box get Box.frame)!!
        val offset = boxFrame.convert(relative.x, relative.y)

        val boxFrameLine = FLine().roundedRect(boxFrame.x.toDouble() - offset.x, boxFrame.y.toDouble() - offset.y, boxFrame.w.toDouble(), boxFrame.h.toDouble(), 19.0)
        val c2 = boxFrameLine.cursor().setT(FLinesAndJavaShapes.closestT(boxFrameLine, Vec3(0.0, 0.0, 0.0))).position()
        val c3 = l1.cursor().setT(FLinesAndJavaShapes.closestT(l1, Vec3(c2.x, c2.y, 0.0))).position()


        if (c2.distance(c3)>30) {
            FLine().moveTo(c3).lineTo(c2).apply {
                this += StandardFLineDrawing.color to Vec4(1.0, 1.0, 1.0, 0.1)
                this += StandardFLineDrawing.thicken to BasicStroke(1f)
                r += this
            }

            FLine().circle(c3.x, c3.y, 2.0).circle(c2.x, c2.y, 4.0).apply {
                this += StandardFLineDrawing.fillColor to Vec4(0.55, 0.55, 0.55, 1)
                this += StandardFLineDrawing.filled to true
                this += StandardFLineDrawing.stroked to false
                r += this
            }
        }

        var id = 0

        r.map {
//            it += FLineDrawing.layer to "__main__"
            it
        }.map {
            FLineDrawing.boxOrigin(it, relative, box)
        }.forEach {
            println(" adding ${name} + ${id}")

            box.properties.putToMap(FLineDrawing.lines, name + (id), it)
            box.properties.putToMap(FLineInteraction.interactiveLines, name + (id), it)

            id++
        }

        return ret


    }

}