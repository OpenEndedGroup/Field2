package field.graphics;

/**
 * A minimal intersectional interface of MeshBuilder, FLine and MeshBuilder_tesselationSupport
 */
public interface MeshAcceptor {
	public MeshAcceptor nextVertex(float x, float y, float z);
	public MeshAcceptor aux(int channel, float[] value);
}
