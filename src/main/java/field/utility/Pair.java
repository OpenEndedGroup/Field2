package field.utility;

/**
 * Created by marc on 3/10/14.
 */
public class Pair<A, B> implements Mutable<Pair<A,B>> {

	final public A first;
	final public B second;

	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public String toString() {
		return "Pair{" + first + "," + second + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Pair)) return false;

		Pair pair = (Pair) o;

		if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
		return !(second != null ? !second.equals(pair.second) : pair.second != null);

	}

	@Override
	public int hashCode() {
		int result = first != null ? first.hashCode() : 0;
		result = 31 * result + (second != null ? second.hashCode() : 0);
		return result;
	}

	@Override
	public Pair<A, B> duplicate() {
		return new Pair<>(first instanceof Mutable ? (A) ((Mutable)first).duplicate() : first, second instanceof Mutable ? (B) ((Mutable)second).duplicate() : second);
	}
}
