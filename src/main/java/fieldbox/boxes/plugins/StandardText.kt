package fieldbox.boxes.plugins

import field.graphics.*
import field.graphics.gdxtext.DrawBitmapFont
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.IdempotencyMap
import java.util.function.Supplier

class StandardText(val shader: Shader) {
    val triangles: BaseMesh
    val triangles_builder: MeshBuilder

    val lines: IdempotencyMap<Supplier<FLine>> = IdempotencyMap(Supplier::class.java)
    val bulkLines: IdempotencyMap<Supplier<Collection<Supplier<FLine>>>> = IdempotencyMap(Collection::class.java)

    private var font: DrawBitmapFont

    @JvmField
    var guard: Supplier<Boolean> = Supplier<Boolean> { true }

    init {
        triangles = BaseMesh.triangleList(0, 0)
        triangles_builder = MeshBuilder(triangles)

        font = DrawBitmapFont(Thread.currentThread().contextClassLoader.getResource("fonts/" + "source-sans-pro-regular-92.fnt")!!
                .file, this.triangles_builder, 0, 5000)

        triangles.attach(font.getTexture())

        with(shader) {

            attach(-3, { x ->
                if (guard.get())
                    render(x)
            })

            attach(triangles)
        }
    }

    private fun render(p: Int) {

        val q = lines
        val q2 = bulkLines

        triangles_builder.open()

        try {
            q.values
                    .stream()
                    .map({ x -> x.get() })
                    .filter({ x -> x != null })
                    .forEach { x -> dispatchText(x, triangles_builder) }
            q2.values
                    .stream()
                    .map({ x -> x.get() })
                    .filter({ x -> x != null })
                    .flatMap({ x -> x.stream() })
                    .filter({ x -> x != null })
                    .map({ x -> x.get() })
                    .filter({ x -> x != null })
                    .forEach { x -> dispatchText(x, triangles_builder) }
        } finally {
            triangles_builder.close()
        }
    }

    private fun dispatchText(x: FLine, triangles_builder: MeshBuilder) {
        if (!x.attributes.isTrue(StandardFLineDrawing.hasText, false)) return

        val fc = Vec4(x.attributes.getOr(StandardFLineDrawing.fillColor, { x.attributes.getOr(StandardFLineDrawing.color, { Vec4(0.0, 0.0, 0.0, 1.0) }) }).get())
        val op = x.attributes.getOr(StandardFLineDrawing.opacity, { 1f })
        val fop = x.attributes.getOr(StandardFLineDrawing.fillOpacity, { 1f })

        fc.w *= (op * fop).toDouble()
        x.nodes.stream()
                .filter({ node -> node.attributes.has(StandardFLineDrawing.text) })
                .forEach { node ->
                    val textToDraw = "" + node.attributes.get(StandardFLineDrawing.text)!!
                    val textScale = node.attributes.getFloat(StandardFLineDrawing.textScale, 1f) * 0.15f
                    val align = node.attributes.getFloat(StandardFLineDrawing.textAlign, 0.5f)
                    val v = font.dimensions(textToDraw, textScale)
                    triangles_builder.aux(1, fc)
                    font.draw(textToDraw, Vec3(node.to.x - align * v.x, node.to.y, node.to.z), textScale, x)
                }

    }

}