package field.utility;

/**
 */
public interface Mutable<T extends Mutable> {

	public T duplicate();
}
