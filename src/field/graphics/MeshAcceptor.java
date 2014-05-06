package field.graphics;

/**
 * Created by marc on 3/19/14.
 */
public interface MeshAcceptor {
	public MeshAcceptor nextVertex(float x, float y, float z);
	public MeshAcceptor aux(int channel, float[] value);
}
