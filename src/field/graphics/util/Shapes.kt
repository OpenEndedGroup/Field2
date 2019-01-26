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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*

/**
 * Classes that will help MeshBuilder support simple shapes
 */
class Shapes {

    @JvmField var normals = 4;
    @JvmField var textures = 3;
    @JvmField var colors = 1;

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
        val shape = ParShapes.par_shapes_create_cube()!!
        ParShapes.par_shapes_scale(shape, scale.x().toFloat(), scale.y().toFloat(), scale.z().toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @JvmOverloads
    @Documentation("Appends a sphere of radius `scale` to `builder`")
    fun sphere(scale: Vec3 = Vec3(1.0, 1.0, 1.0), slices: Int, stacks: Int, builder: MeshBuilder): MeshBuilder {
        val shape = ParShapes.par_shapes_create_parametric_sphere(slices, stacks)!!
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
        val shape = ParShapes.par_shapes_create_torus(slices, stacks, inner_radius.toFloat())!!
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
        val shape = ParShapes.par_shapes_create_tetrahedron()!!
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
        val shape = ParShapes.par_shapes_create_icosahedron()!!
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
        val shape = ParShapes.par_shapes_create_dodecahedron()!!
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
        val shape = ParShapes.par_shapes_create_octahedron()!!
        ParShapes.par_shapes_scale(shape, scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
        parMeshToMeshBuilder(shape, builder)
        return builder
    }

    @Documentation("Appends an octahedron of size `scale`")
    fun octahedron(scale: Number, builder: MeshBuilder): MeshBuilder {
        return octahedron(Vec3(scale.toDouble(), scale.toDouble(), scale.toDouble()), builder)
    }


    class Mesh(val v: FloatBuffer, val e: IntBuffer, val n: FloatBuffer?, val c: FloatBuffer?, val name: String)

    fun loadMeshes(filename: String): MutableList<Mesh> {
        val scene = aiImportFile(filename, aiProcess_JoinIdenticalVertices or aiProcess_Triangulate)!!;

        val out = mutableListOf<Mesh>()

        val meshes = scene.mMeshes()!!
        for (n in 0 until scene.mNumMeshes()) {
            val aimesh = AIMesh.create(meshes[n])


            val vb = aimesh.mVertices()
            val v = bufferToFloatBuffer(vb)

            // no positions, no mesh
            if (v == null) continue

            val fe = aimesh.mFaces()
            val e = bufferToIntBuffer(fe, aimesh.mNumFaces())

            // no elements, no mesh
            if (e == null) continue

            // optionally, normals
            val vn = aimesh.mNormals()
            val normal = bufferToFloatBuffer(vn)

            // optionally color 0
            val vc = aimesh.mColors(0);
            val c = bufferToFloatBuffer(vc);

            out += Mesh(v, e, normal, c, aimesh.mName().dataString())
        }

        return out
    }

    fun appendToMeshBuilder(mesh: Mesh, builder: MeshBuilder) {
        if (builder.target.elementDimension != 3) throw IllegalArgumentException("can't write to a meshbuilder with element dimension " + builder.target.elementDimension)
        mesh.v.rewind()
        mesh.n?.rewind()
        mesh.c?.rewind()
        mesh.e.rewind()

        val rot = { x: Number, y: Number, z: Number -> rotate.transform(Vec3(x.toDouble(), y.toDouble(), z.toDouble())) }
        val trans = { x : Vec3 -> translate + x }


        builder.use {
            val V = mesh.v.limit() / 3

            for (n in 0 until V) {
                if (mesh.n != null && normals>0)
                    builder.aux(normals, rot(mesh.n.get(), mesh.n.get(), mesh.n.get()))
                if (mesh.c != null && colors>0)
                    builder.aux(colors, mesh.c.get(), mesh.c.get(), mesh.c.get(), mesh.c.get())

                builder.v(trans(rot(mesh.v.get(), mesh.v.get(), mesh.v.get())))
            }

            when (builder.target.elementDimension) {
                3 -> for (n in 0 until mesh.e.limit()/3) {
                    it.e(V - 1 - mesh.e.get(), V - 1 - mesh.e.get(), V - 1 - mesh.e.get())
                }
                2 -> for (n in 0 until mesh.e.limit()/3) {
                    val a = V - 1 - mesh.e.get()
                    val b = V - 1 - mesh.e.get()
                    val c = V - 1 - mesh.e.get()
                    it.e(a, b)
                    it.e(b, c)
                    it.e(c, a)
                }
                1 -> {
                }
            }

        }
    }


    private fun bufferToIntBuffer(buffer: AIFace.Buffer?, r: Int): IntBuffer? {
        if (buffer == null) return null;

        // 3-faces only

        val v = ByteBuffer.allocateDirect(4 * 3 * r).order(ByteOrder.nativeOrder()).asIntBuffer()
        for (n2 in 0..r - 1) {
            val v3 = buffer[n2]
            val i = v3.mIndices()

            if (v3.mNumIndices() != 3)
                throw IllegalArgumentException(" can't handle face of " + v3.mNumIndices())

            v.put(i[0])
            v.put(i[1])
            v.put(i[2])
        }
        return v
    }

    private fun bufferToFloatBuffer(buffer: AIVector3D.Buffer?): FloatBuffer? {
        if (buffer == null) return null;
        val r = buffer.remaining()

        System.out.println(" buffer has $r remaining ")
        val v = ByteBuffer.allocateDirect(4 * 3 * r).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (n2 in 0 until r) {
            val v3 = buffer[n2]
            System.out.println(" at $v3 = $v ($n2)");

            v.put(v3.x())
            v.put(v3.y())
            v.put(v3.z())
        }
        return v
    }

    private fun bufferToFloatBuffer(buffer: AIVector2D.Buffer?): FloatBuffer? {
        if (buffer == null) return null;
        val r = buffer.remaining()

        val v = ByteBuffer.allocateDirect(4 * 2 * r).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (n2 in 0 until r) {
            val v3 = buffer[n2]
            v.put(v3.x())
            v.put(v3.y())
        }
        return v
    }

    private fun bufferToFloatBuffer(buffer: AIColor4D.Buffer?): FloatBuffer? {
        if (buffer == null) return null;
        val r = buffer.remaining()

        val v = ByteBuffer.allocateDirect(4 * 4 * r).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (n2 in 0 until r) {
            val v3 = buffer[n2]
            v.put(v3.r())
            v.put(v3.g())
            v.put(v3.b())
            v.put(v3.a())
        }
        return v
    }


    fun parMeshToMeshBuilder(shape: ParShapesMesh, builder: MeshBuilder) {

        val p = shape.npoints()
        val t = shape.ntriangles()

        val vertex = shape.points(p * 3)
        val normal = shape.normals(p * 3)
        val texture = shape.tcoords(p * 2)
        val triangle = shape.triangles(t * 3)

        val rot = { x: Number, y: Number, z: Number -> rotate.transform(Vec3(x.toDouble(), y.toDouble(), z.toDouble())) }
        val trans = { x: Vec3-> translate + x }

        builder.use {
            for (n in 0 until p) {
                if (normals > 0 && normal!=null)
                    it.aux(normals, rot(normal.get(), normal.get(), normal.get()))
                if (textures > 0 && texture!=null)
                    it.aux(textures, texture.get(), texture.get())


                it.v(trans(rot(vertex.get(), vertex.get(), vertex.get())))
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
