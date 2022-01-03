package fieldbox.boxes.plugins

import field.graphics.*
import field.utility.IdempotencyMap
import fieldbox.boxes.TextDrawing
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors

class StandardLines(val shader: Shader) {
    val triangles: BaseMesh
    val triangles_builder: MeshBuilder
    val line: BaseMesh
    val line_builder: MeshBuilder

    val lines: IdempotencyMap<Supplier<FLine>> = IdempotencyMap(Supplier::class.java)
    val bulkLines: IdempotencyMap<Supplier<Collection<Supplier<FLine>>>> = IdempotencyMap(Collection::class.java)

    @JvmField
    var guard : Supplier<Boolean> = Supplier<Boolean> { true }

    init {
        triangles = BaseMesh.triangleList(0, 0)

        triangles_builder = MeshBuilder(triangles)
        line = BaseMesh.lineList(0, 0)

        line_builder = MeshBuilder(line)

        with(shader) {

            attach(-3, { x ->
                if (guard.get())
                    render(x)
            })

            attach(triangles)
            attach(line)
        }
    }

    private fun render(p: Int) {

        val q = lines
        val q2 = bulkLines

        line_builder.open()
        triangles_builder.open()

        try {
            q.values
                    .stream()
                    .map({ x -> x.get() })
                    .filter({ x -> x != null })
                    .forEach { x -> StandardFLineDrawing.dispatchLine(x, triangles_builder, line_builder, null, Optional.empty(), "") }
            q2.values
                    .stream()
                    .map({ x -> x.get() })
                    .filter({ x -> x != null })
                    .flatMap({ x -> x.stream() })
                    .filter({ x -> x != null })
                    .map({ x -> x.get() })
                    .filter({ x -> x != null })
                    .forEach { x -> StandardFLineDrawing.dispatchLine(x, triangles_builder, line_builder, null, Optional.empty(), "") }
        } finally {
            triangles_builder.close()
            line_builder.close()
        }
    }


}