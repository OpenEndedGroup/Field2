@file:JvmName("Shapes")

package field.graphics.util

import field.graphics.MeshBuilder
import field.linalg.Quat
import field.linalg.Vec3
import field.utility.*
import field.utility.use
import fieldnashorn.annotations.SafeToToString
import org.lwjgl.util.par.ParShapes
import org.lwjgl.util.par.ParShapesMesh
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus
import kotlin.reflect.full.memberFunctions

/**
 * Classes that will help MeshBuilder support simple shapes
 */
class Shapes {

    @JvmField var normals = 2;
    @JvmField var textures = 3;

    @SafeToToString
    @Documentation("Set this field to rotate everything that this class appends to builders")
    @JvmField var rotate = Quat()

    @SafeToToString
    @Documentation("Set this field to translate everything that this class appends to builders")
    @JvmField var translate = Vec3()

    @Documentation("Appends a cube of size `scale` to `builder`")
    fun cube(scale: Number = 1, builder: MeshBuilder): MeshBuilder {
        return cube(Vec3(scale.toDouble(), scale.toDouble(), scale.toDouble()), builder)
    }

    @JvmOverloads
    @Documentation("Appends a cube of size `scale` to `builder`")
    fun cube(scale: Vec3 = Vec3(1.0, 1.0, 1.0), builder: MeshBuilder): MeshBuilder {
        val shape = ParShapes.par_shapes_create_cube()
        ParShapes.par_shapes_scale(shape, scale.x().toFloat(), scale.y().toFloat(), scale.z().toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @JvmOverloads
    @Documentation("Appends a sphere of radius `scale` to `builder`")
    fun sphere(scale: Vec3 = Vec3(1.0, 1.0, 1.0), slices: Int, stacks: Int, builder: MeshBuilder): MeshBuilder {
        val shape = ParShapes.par_shapes_create_parametric_sphere(slices, stacks)
        ParShapes.par_shapes_scale(shape, scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @Documentation("Appends a sphere of radius  `scale` to `builder`")
    fun sphere(scale: Number, slices: Int, stacks: Int, builder: MeshBuilder): MeshBuilder {
        return sphere(Vec3(scale.toDouble(), scale.toDouble(), scale.toDouble()), slices, stacks, builder)
    }

    @JvmOverloads
    @Documentation("Appends a torus of with radius `scale` and inner radius `scale` * `inner_radius` to `builder`")
    fun torus(scale: Vec3 = Vec3(1.0, 1.0, 1.0), slices: Int, stacks: Int, inner_radius: Number, builder: MeshBuilder): MeshBuilder {
        val shape = ParShapes.par_shapes_create_torus(slices, stacks, inner_radius.toFloat())
        ParShapes.par_shapes_scale(shape, scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @Documentation("Appends a torus of with radius `scale` and inner radius `scale` * `inner_radius` to `builder`")
    fun torus(scale: Number, slices: Int, stacks: Int, inner_radius: Number, builder: MeshBuilder): MeshBuilder {
        return torus(Vec3(scale.toDouble(), scale.toDouble(), scale.toDouble()), slices, stacks, inner_radius, builder)
    }

    @JvmOverloads
    @Documentation("Appends a tetrahedron of size `scale`")
    fun tetrahedron(scale: Vec3 = Vec3(1.0, 1.0, 1.0), builder: MeshBuilder): MeshBuilder {
        val shape = ParShapes.par_shapes_create_tetrahedron()
        ParShapes.par_shapes_scale(shape, scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @Documentation("Appends a tetrahedron of size `scale`")
    fun tetrahedron(scale: Number, builder: MeshBuilder): MeshBuilder {
        return tetrahedron(Vec3(scale.toDouble(), scale.toDouble(), scale.toDouble()), builder)
    }

    @JvmOverloads
    @Documentation("Appends an icosahedron of size `scale`")
    fun icosahedron(scale: Vec3 = Vec3(1.0, 1.0, 1.0), builder: MeshBuilder): MeshBuilder {
        val shape = ParShapes.par_shapes_create_icosahedron()
        ParShapes.par_shapes_scale(shape, scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @Documentation("Appends an icosahedron of size `scale`")
    fun icosahedron(scale: Number, builder: MeshBuilder): MeshBuilder {
        return icosahedron(Vec3(scale.toDouble(), scale.toDouble(), scale.toDouble()), builder)
    }

    @JvmOverloads
    @Documentation("Appends an dodecahedron of size `scale`")
    fun dodecahedron(scale: Vec3 = Vec3(1.0, 1.0, 1.0), builder: MeshBuilder): MeshBuilder {
        val shape = ParShapes.par_shapes_create_dodecahedron()
        ParShapes.par_shapes_scale(shape, scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @Documentation("Appends an dodecahedron of size `scale`")
    fun dodecahedron(scale: Number, builder: MeshBuilder): MeshBuilder {
        return dodecahedron(Vec3(scale.toDouble(), scale.toDouble(), scale.toDouble()), builder)
    }

    @JvmOverloads
    @Documentation("Appends an octahedron of size `scale`")
    fun octahedron(scale: Vec3 = Vec3(1.0, 1.0, 1.0), builder: MeshBuilder): MeshBuilder {
        val shape = ParShapes.par_shapes_create_octahedron()
        ParShapes.par_shapes_scale(shape, scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @Documentation("Appends an octahedron of size `scale`")
    fun octahedron(scale: Number, builder: MeshBuilder): MeshBuilder {
        return octahedron(Vec3(scale.toDouble(), scale.toDouble(), scale.toDouble()), builder)
    }


    fun parMeshToMeshBuilder(shape: ParShapesMesh, builder: MeshBuilder) {

        val p = shape.npoints()
        val t = shape.ntriangles()

        val vertex = shape.points(p * 3)
        val normal = shape.normals(p * 3)
        val texture = shape.tcoords(p * 2)
        val triangle = shape.triangles(t * 3)

        val rot = { x: Number, y: Number, z: Number -> rotate.transform(Vec3(x.toDouble(), y.toDouble(), z.toDouble())) }
        val trans = { x: Number, y: Number, z: Number -> translate + Vec3(x.toDouble(), y.toDouble(), z.toDouble()) }

        builder.use {
            for (n in 0 until p) {
                if (normals > 0)
                    it.aux(normals, rot(normal.get(), normal.get(), normal.get()))
                if (textures > 0)
                    it.aux(textures, texture.get(), texture.get())


                it.v(trans(vertex.get(), vertex.get(), vertex.get()))
            }

            when (builder.target.elementDimension) {
                3 -> for (n in 0 until t) {
                    it.e(p - 1 - triangle.get(), p - 1 - triangle.get(), p - 1 - triangle.get())
                }
                2 -> for (n in 0 until t) {
                    val a = p - 1 - triangle.get()
                    val b = p - 1 - triangle.get()
                    val c = p - 1 - triangle.get()
                    it.e(a, b)
                    it.e(b, c)
                    it.e(c, a)
                }
                1 -> {
                }
            }
        }
    }

}
