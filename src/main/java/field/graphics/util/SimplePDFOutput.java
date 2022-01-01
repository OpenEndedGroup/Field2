package field.graphics.util;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.IdempotencyMap;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Very straightforward methods for writing PDF's from FLine's, dependancy free, since we're talking to less than 1% of the spec.
 */
public class SimplePDFOutput {

	static public Dict.Prop<Number> pdfWidth = new Dict.Prop<>("pdfWidth"); //...

	private CharCountingWriter w;
	int at = 0;

	public class CharCountingWriter extends PrintWriter {
		public CharCountingWriter(Writer out) {
			super(out);
		}

		@Override
		public void write(int c) {
			super.write(c);
			at++;
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			at += len;
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			at += len;
		}

		@Override
		public void println() {
			super.println();
			at += "\n".length();
		}
	}

	public SimplePDFOutput(Writer w) {
		this.w = new CharCountingWriter(w);
	}

	public SimplePDFOutput(String filename) throws IOException {
		this(new BufferedWriter(new FileWriter(new File(filename))));
	}


	public interface Transform extends Function<Vec3, Vec2> {
	}

	public Transform transform = x -> x.toVec2();

	public interface FilterLineAttributes extends Function<Dict, Dict>
	{
	}

	public IdempotencyMap<FilterLineAttributes> filters = new IdempotencyMap<>(FilterLineAttributes.class);

	public void draw(FLine f)
	{
		startLine();
		try{

			for(FLine.Node ff : f.nodes)
			{
				if (ff instanceof FLine.MoveTo)
				{
					Vec2 t = transform.apply(((FLine.MoveTo) ff).to);
					moveTo(t.x, t.y);
				}
				else if (ff instanceof FLine.LineTo)
				{
					Vec2 t = transform.apply(ff.to);
					lineTo(t.x, t.y);
				}
				else if (ff instanceof FLine.CubicTo)
				{
					Vec2 c1 = transform.apply(((FLine.CubicTo) ff).c1);
					Vec2 c2 = transform.apply(((FLine.CubicTo) ff).c2);
					Vec2 to = transform.apply(((FLine.CubicTo) ff).to);
					cubicTo(c1.x, c1.y, c2.x, c2.y, to.x, to.y);
				}
			}

			Dict d = f.attributes;
			for(FilterLineAttributes a : filters.values())
			{
				d = a.apply(f.attributes);
			}

			double w = 0.1f;
			BasicStroke t = d.get(StandardFLineDrawing.thicken);
			if (t!=null)
			{
				w = t.getLineWidth();
			}
			else
			{
				w = d.getOr(pdfWidth, () -> 0.1f).floatValue();
			}

			Vec4 c = d.getOr(StandardFLineDrawing.color, () -> new Vec4(0,0,0,1)).get();
			Supplier<Vec4> sc = d.get(StandardFLineDrawing.strokeColor);
			Supplier<Vec4> fc = d.get(StandardFLineDrawing.fillColor);

			if (sc==null) sc = c;
			if (fc==null) fc = c;

			if (sc==null) sc = new Vec4(0,0,0,1);
			if (fc==null) fc = new Vec4(0,0,0,1);

			Vec4 rsc = sc.get();
			Vec4 rfc = fc.get();


			boolean stroked = d.isTrue(StandardFLineDrawing.stroked, true);
			boolean filled = d.isTrue(StandardFLineDrawing.filled, false);

			if (stroked && filled) {
				this.width(w);
				this.strokeColor(rsc.x, rsc.y, rsc.z, rsc.w);
				this.fillColor(rfc.x, rfc.y, rfc.z, rfc.w);
				this.fillAndStroke();
			}
			else if (stroked) {
				this.width(w);
				this.strokeColor(rsc.x, rsc.y, rsc.z, rsc.w);
				this.stroke();
			}
			else if (filled) {
				this.fillColor(rfc.x, rfc.y, rfc.z, rfc.w);
				this.fill();
			}

		}finally
		{
			endLine();
		}
	}


	// direct interface below ------------------------------------------------------------------------


	Map<Integer, Integer> index = new LinkedHashMap<>();

	int startStreamAt = 0;

	public void header(int wi, int he) {
		w.println("%PDF-1.7");
		mark(1);
		w.println("1 0 obj <</Type /Catalog /Pages 2 0 R>>");
		w.println("endobj");
		mark(2);
		w.println("2 0 obj <</Type /Pages /Kids [3 0 R] /Count 1 /MediaBox [0 0 "+wi+" "+he+"]>>");
		w.println("endobj");
		mark(3);
		w.println("3 0 obj<</Type /Page /Parent 2 0 R /Contents 4 0 R /Resources 6 0 R>>");
		w.println("endobj");
		mark(4);
		w.println("4 0 obj");
		w.println("<< /Length 5 0 R >> stream");
		startStreamAt = at;
	}

	public void startLine() {
		w.println("q");
	}

	public void endLine() {
		w.println("Q");
	}

	public void moveTo(double x, double y) {
		w.print(x + " " + y + " m ");
	}

	public void lineTo(double x, double y) {
		w.print(x + " " + y + " l ");
	}

	public void cubicTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
		w.print(c1x + " " + c1y + " " + c2x + " " + c2y + " " + x + " " + y + " c ");
	}

	public void fill() {
		w.println(" f ");
	}

	public void stroke() {
		w.println(" S ");
	}

	public void fillAndStroke() {
		w.println(" b ");
	}

	public void width(double d)
	{
		w.println(d+" w 1 J");
	}

	public void strokeColor(double r, double g, double b, double a) {
		w.print(r + " " + g + " " + b + " RG ");

		// find alpha that's close enough and reuse if we can

		ExtGSState q = new ExtGSState(a, true);
		if (!states.containsKey(q)) {
			states.put(q, q);
		} else {
			q = states.get(q);
		}

		w.println("/" + q.name + " gs");
	}

	public void fillColor(double r, double g, double b, double a) {
		w.print(r + " " + g + " " + b + " rg ");

		// find alpha that's close enough and reuse if we can

		ExtGSState q = new ExtGSState(a, false);
		if (!states.containsKey(q)) {
			states.put(q, q);
		} else {
			q = states.get(q);
		}

		w.println("/" + q.name + " gs");
	}

	public void lineWidth(double d)
	{
		w.println(d+" w 1 J 1 j");
	}


	int stateCount = 0;

	public class ExtGSState {
		String name = "GS" + (stateCount++);
		double alpha;
		boolean stroke = true;

		public ExtGSState(double alpha, boolean stroke) {
			this.alpha = alpha;
			this.stroke = stroke;
		}

		public String toString() {
			return "<< /Type /ExtGState " + (stroke ? "/CA" : "/ca") + " " + alpha + " >>";
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ExtGSState that = (ExtGSState) o;

			if (Double.compare((int) (that.alpha * 100) / 100f, (int) (alpha * 100) / 100f) != 0) return false;
			return stroke == that.stroke;

		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			temp = Double.doubleToLongBits((int) (alpha * 100) / 100f);
			result = (int) (temp ^ (temp >>> 32));
			result = 31 * result + (stroke ? 1 : 0);
			return result;
		}
	}

	Map<ExtGSState, ExtGSState> states = new LinkedHashMap<ExtGSState, ExtGSState>();

	public void finish() {
		int length = at - startStreamAt;
		w.println("endstream");
		w.println("endobj");
		mark(5);
		w.println("5 0 obj ");
		w.println("" + length);
		w.println("endobj");

		// do the opacity states
		int o = 6;
		mark(o);
		w.println(o + " 0 obj");
		w.println("<< /ExtGState << ");

		ArrayList<ExtGSState> lstates = new ArrayList<>(states.keySet());

		for (int i = 0; i < lstates.size(); i++)
			w.println("/" + lstates.get(i).name + " " + (o + i + 1) + " 0 R");
		w.println(">> >>");
		o++;

		for (int i = 0; i < states.size(); i++) {
			// need one of these and then lots of the next
			mark(o);
			w.println(o + " 0 obj");
			w.println(lstates.get(i)
					 .toString());
			o++;
		}

		int xrefAt = at;
		w.println("xref");
		w.println("0 " + o);
		w.println("0000000000 65535 f");
		for (int i = 1; i < 6; i++) {
			w.println(pad(index.get(i)) + " 00000 n");
		}
		w.println("trailer << /Size " + o + " /Root 1 0 R >>");
		w.println("startxref");
		w.println(xrefAt);
		w.println("%%EOF");
		w.close();
	}

	private void mark(int i) {
		index.put(i, at);
	}

	private String pad(int i) {
		String q = "" + i;
		while (q.length() < 10) q = "0" + q;
		return q;
	}
}
