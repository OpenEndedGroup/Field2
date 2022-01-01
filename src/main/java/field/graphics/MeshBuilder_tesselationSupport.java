package field.graphics;

import field.linalg.Vec3;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;
import org.lwjgl.util.glu.tessellation.GLUtessellatorImpl;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Package class that talks to the OpenGL tesslator
 */
class MeshBuilder_tesselationSupport implements MeshAcceptor{

	protected final MeshBuilder target;
	protected final GLUtessellatorImpl tess = new GLUtessellatorImpl();

	class VInfo {
		MeshBuilder.Bookmark vertex = null;

		Vec3 position = new Vec3();

		Map<Integer, float[]> properties;

		public VInfo(MeshBuilder.Bookmark vertex, Vec3 position, Map<Integer, float[]> properties) {
			super();
			this.vertex = vertex;
			this.position = position;
			this.properties = new HashMap<>(properties);
		}

		@Override
		public String toString() {
			return vertex + "@ " + position;
		}
	}

	public MeshBuilder_tesselationSupport(MeshBuilder target) {

		this.target = target;

		tess.gluTessCallback(GLU.GLU_TESS_VERTEX_DATA, new GLUtessellatorCallbackAdapter() {
			@Override
			public void vertexData(Object arg0, Object arg1) {
				VInfo i = (VInfo) arg0;

				nextTriangle(i);
			}
		});

		tess.gluTessCallback(GLU.GLU_TESS_COMBINE_DATA, new GLUtessellatorCallbackAdapter() {

			@Override
			public void combineData(double[] coords, Object[] data, float[] weight, Object[] outdata, Object arg4) {
				outdata[0] = new VInfo(null, new Vec3((float)coords[0], (float)coords[1], (float)coords[2]), interpolateProperites(data, weight));
			}
		});
		tess.gluTessCallback(GLU.GLU_EDGE_FLAG, new GLUtessellatorCallbackAdapter() {
		});

		tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE,GLU.GLU_TESS_WINDING_NONZERO);
//		tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);

	}

	public void begin() {
		auxValues.clear();
		prevAuxValues.clear();
		tess.gluTessBeginPolygon(null);
	}

	boolean contourFirst = false;

	public void beginContour() {
		tess.gluTessBeginContour();
		contourFirst = true;
		first = null;
	}

	public void end() {
		try {
			tess.gluTessEndPolygon();
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			e.printStackTrace();
		}
	}

	public void endContour() {
		tess.gluTessEndContour();
	}

	Map<Integer, float[]> auxValues = new HashMap<>();
	Map<Integer, float[]> prevAuxValues = new HashMap<>();
	public MeshAcceptor aux(int channel, float[] value)
	{
		auxValues.put(channel, value);
		return this;
	}

	Vec3 first = null;
	@Override
	public MeshAcceptor v(float x, float y, float z) {
		if (first==null)
		{
			first = new Vec3(x,y,z);
			prevAuxValues = new HashMap<>(auxValues);
		}
		else
		{
			Vec3 next = new Vec3(x,y,z);
			line(first, next, prevAuxValues, auxValues);
			first = next;
			prevAuxValues = new HashMap<>(auxValues);
		}
		return this;
	}


	public void line(Vec3 a, Vec3 b, Map<Integer, float[]> pa, Map<Integer, float[]> pb) {

		if (contourFirst) {
			VInfo info = new VInfo(null, a, pa);
			v(info);
			tess.gluTessVertex(new double[] { a.x, a.y, a.z }, 0, info);
		}

		VInfo info = new VInfo(null, b, pb);
		v(info);
		tess.gluTessVertex(new double[] { b.x, b.y, b.z }, 0, info);

		contourFirst = false;
	}

	public void line(int a, int b, Map<Integer, float[]> pa, Map<Integer, float[]> pb) {

		if (contourFirst) {
			VInfo info = new VInfo(null, retrieveVertex(a), pa);
			v(info, a);
			tess.gluTessVertex(new double[] { info.position.x, info.position.y, info.position.z }, 0, info);
		}

		VInfo info = new VInfo(null, retrieveVertex(b), pb);
		v(info, b);
		tess.gluTessVertex(new double[] { info.position.x, info.position.y, info.position.z }, 0, info);

		contourFirst = false;
	}

	private Vec3 retrieveVertex(int abs) {
		FloatBuffer t = target.getTarget()
				      .vertex(true);

		return new Vec3(3*(abs-1), t);
	}

	protected Map<Integer, float[]> interpolateProperites(Object/*VInfo*/[] data, float[] weight) {
		Map<Integer, float[]> res= new HashMap<Integer, float[]>();

		for(int i=0;i<data.length;i++)
		{
			if (data[i]==null) continue;

			Map<Integer, float[]> m = ((VInfo)data[i]).properties;
			for (Map.Entry<Integer, float[]> entry : m.entrySet()) {
				float[] o = res.computeIfAbsent(entry.getKey(), (k) -> new float[entry.getValue().length]);
				for(int j=0;j<o.length;j++)
					o[j] += entry.getValue()[j]*weight[i];
			}
		}

		return res;
	}

	List<VInfo> ongoingTriangle = new ArrayList<VInfo>();

	protected void nextTriangle(VInfo i) {

		if (i.vertex == null) {
			v(i);
		}

		if (ongoingTriangle.size() == 2) {
			nextTriangle(ongoingTriangle.get(0), ongoingTriangle.get(1), i);
			ongoingTriangle.clear();
		} else
			ongoingTriangle.add(i);
	}

	protected void nextTriangle(VInfo a, VInfo b, VInfo c) {
		target.e(a.vertex.at(), b.vertex.at(), c.vertex.at());
	}

	protected void v(VInfo info) {
		if (info.vertex == null) {
			decorateVertex(info.vertex, info.properties);
			info.vertex = v(info.position);
		}
	}

	protected void v(VInfo info, int absolute) {
		if (info.vertex == null) {
			info.vertex = target.bookmark(absolute);
			decorateVertex(info.vertex, info.properties);
		}
	}

	protected void decorateVertex(MeshBuilder.Bookmark vertex, Map<Integer, float[]> properties)
	{
		for (Map.Entry<Integer, float[]> e : properties.entrySet()) {
			target.aux(e.getKey(), e.getValue());
		}
	}

	protected MeshBuilder.Bookmark v(Vec3 position)
	{
		target.v(position.x, position.y, position.z);
		return target.bookmark();
	}

}
