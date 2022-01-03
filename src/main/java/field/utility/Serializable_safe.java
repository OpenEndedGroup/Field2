package field.utility;

import java.io.Serializable;

/**
 * Marks an interface as safe to serialize by IO. Needed until we wrestle out the issues with using Serializable directly in EDN (for example)
 */
public interface Serializable_safe extends Serializable {
}
