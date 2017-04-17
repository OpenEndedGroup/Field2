package field.utility;

/**
 * Created by marc on 3/10/14.
 */
public class UnorderedPair<A> implements Mutable<UnorderedPair<A>> {

	final public A first;
	final public A second;

	public UnorderedPair(A first, A second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public String toString() {
		return "UnorderedPair{" + first + "," + second + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof UnorderedPair)) return false;

		UnorderedPair<?> that = (UnorderedPair<?>) o;

		if (compare(first, that.first) && compare(second, that.second)) return true;
		if (compare(second, that.first) && compare(first, that.second)) return true;
		return false;
	}

	private boolean compare(Object a, Object b) {
		if (a == null) return b != null;
		if (b == null) return false;
		return a.equals(b);
	}

	@Override
	public int hashCode() {
		int result = first != null ? first.hashCode() : 0;
		result += (second != null ? second.hashCode() : 0);
		return result;
	}

	@Override
	public UnorderedPair<A> duplicate() {
		return new UnorderedPair<>(first instanceof Mutable ? (A) ((Mutable) first).duplicate() : first, second instanceof Mutable ? (A) ((Mutable) second).duplicate() : second);
	}
}
