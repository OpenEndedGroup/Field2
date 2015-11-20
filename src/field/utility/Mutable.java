package field.utility;

public interface Mutable<T extends Mutable> {

	T duplicate();
}
