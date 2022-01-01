package field.graphics;

/**
 * A minimal intersectional interface of MeshBuilder, FLine and MeshBuilder_tesselationSupport
 */
public interface MeshAcceptor {
	MeshAcceptor v(float x, float y, float z);

	default MeshAcceptor v(double x, double y, double z)
	{
		return v((float)x, (float)y, (float)z);
	}

	MeshAcceptor aux(int channel, float[] value);
}
