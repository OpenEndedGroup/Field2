package field.utility;

/**
 * Created by marc on 3/25/14.
 */
public class Triple<A, B, C> extends Pair<A,B> {
	final public C third;

	public Triple(A first, B second, C third) {
		super(first, second);
		this.third = third;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Triple)) return false;
		if (!super.equals(o)) return false;

		Triple triple = (Triple) o;

		if (third != null ? !third.equals(triple.third) : triple.third != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (third != null ? third.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Triple{" + first + "," + second + ","+third+"}";
	}
}
