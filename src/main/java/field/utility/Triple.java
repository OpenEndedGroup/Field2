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

		return !(third != null ? !third.equals(triple.third) : triple.third != null);

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

	@Override
	public Triple<A, B, C> duplicate() {
		return new Triple<>(first instanceof Mutable ? (A) ((Mutable)first).duplicate() : first, second instanceof Mutable ? (B) ((Mutable)second).duplicate() : second,  third instanceof Mutable ? (C) ((Mutable)third).duplicate() : third);
	}
}
