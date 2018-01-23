package field.graphics

import field.linalg.Vec2
import field.linalg.Vec4
import field.utility.Dict
import field.utility.times
import java.util.function.Supplier

class FastThicken {
    companion object {
        @JvmField
        val t: Dict.Prop<Number> = Dict.Prop<Any>("t").type<Number>()
                .toCanon<Number>()
                .doc<Number>("the default thickness at this node").set(Dict.domain, "fline fnode")

        @JvmField
        val tL: Dict.Prop<Number> = Dict.Prop<Any>("tL").type<Number>()
                .toCanon<Number>()
                .doc<Number>("'left' thickness at this node").set(Dict.domain, "fline fnode")

        @JvmField
        val tR: Dict.Prop<Number> = Dict.Prop<Any>("tR").type<Number>()
                .toCanon<Number>()
                .doc<Number>("'right' thickness at this node").set(Dict.domain, "fline fnode")

    }

    fun apply(f: FLine, d : Double): FLine {

        val segments = FLinesAndJavaShapes().segment(f)
        if (segments.size == 0) return f;
        if (segments.size == 1) return apply_segment(segments[0],d);

        val r = FLine()
        segments.map { apply_segment(it,d ) }.forEach { r.append(it) }

        return r;
    }

    private fun apply_segment(f: FLine, m : Double =1.0): FLine {

        val ret = FLine();
        val c = f.cursor();

        val dT = f.attributes.getFloat(t, 1f);
        val dTL = f.attributes.getFloat(tL, dT);
        val dTR = f.attributes.getFloat(tR, dT);

        var first : Vec2? =null;

        for (i in 0 until f.nodes.size) {
            c.setT(i.toDouble());
            val THICK = f.nodes[i].attributes.getFloat(tL, f.nodes[i].attributes.getFloat(t, dTL))*m;

            val n2 = c.normal2()

            val n = n2.normalize() * THICK;

            if (n.isNaN()) n.set(0.0,0.0);

            if (i == 0)
                ret.moveTo(f.nodes[0].to.x + n.x, f.nodes[0].to.y + n.y)
            else if (f.nodes[i] is FLine.LineTo)
                ret.lineTo(f.nodes[i].to.x + n.x, f.nodes[i].to.y + n.y)
            else if (f.nodes[i] is FLine.CubicTo) {
                val n0 = c.setT(i.toDouble() - 0.66).normal2().normalize() * THICK;
                val n1 = c.setT(i.toDouble() - 0.33).normal2().normalize() * THICK;
                if (n0.isNaN()) n0.set(0.0,0.0);
                if (n1.isNaN()) n1.set(0.0,0.0);

                ret.cubicTo((f.nodes[i] as FLine.CubicTo).c1.x + n0.x, (f.nodes[i] as FLine.CubicTo).c1.y + n0.y,
                        (f.nodes[i] as FLine.CubicTo).c2.x + n1.x, (f.nodes[i] as FLine.CubicTo).c2.y + n1.y,
                        (f.nodes[i] as FLine.CubicTo).to.x + n.x, (f.nodes[i] as FLine.CubicTo).to.y + n.y)
            } else throw IllegalArgumentException();

            if (first==null)
                first = Vec2(f.nodes[0].to.x + n.x, f.nodes[0].to.y + n.y)

        }

        var last : Vec2? =null;

        for (i in f.nodes.size-1 downTo 1) {
            c.setT(i.toDouble()-1);
            val THICK = -f.nodes[i-1].attributes.getFloat(tR, f.nodes[i-1].attributes.getFloat(t, dTR))*m;

            val n2 = c.normal2()

            val n = n2.normalize() * THICK;

            if (n.isNaN()) n.set(0.0,0.0);

            if (i==f.nodes.size-1) {
                val THICK0 = -f.nodes[i].attributes.getFloat(tR, f.nodes[i].attributes.getFloat(t, dTR))*m
                val n0 = c.setT(i.toDouble()).normal2().normalize() * THICK0;
                if (n0.isNaN()) n0.set(0.0,0.0);
                ret.lineTo(f.nodes[i].to.x + n0.x, f.nodes[i].to.y + n0.y) // connect
            }

            if (f.nodes[i] is FLine.LineTo)
                ret.lineTo(f.nodes[i-1].to.x + n.x, f.nodes[i-1].to.y + n.y)
            else if (f.nodes[i] is FLine.CubicTo) {

                val n0 = c.setT(i.toDouble() - 0.33).normal2().normalize() * THICK;
                val n1 = c.setT(i.toDouble() - 0.66).normal2().normalize() * THICK;
                if (n0.isNaN()) n0.set(0.0,0.0);
                if (n1.isNaN()) n1.set(0.0,0.0);

                ret.cubicTo((f.nodes[i] as FLine.CubicTo).c2.x + n0.x, (f.nodes[i] as FLine.CubicTo).c2.y + n0.y,
                        (f.nodes[i] as FLine.CubicTo).c1.x + n1.x, (f.nodes[i] as FLine.CubicTo).c1.y + n1.y,
                        (f.nodes[i-1]).to.x + n.x, (f.nodes[i-1]).to.y + n.y)
            } else throw IllegalArgumentException();

            last = Vec2(f.nodes[i-1].to.x + n.x, f.nodes[i-1].to.y + n.y)
        }

        if (first!=null && last!=null)
        {
            ret.lineTo(first);
        }

        return ret
    }

}