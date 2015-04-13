package field.utility;

/**
 * Created by marc on 3/25/14.
 */
public class Quad<A, B, C, D> extends Triple<A, B, C> {
	final public D fourth;

	public Quad(A first, B second, C third, D fourth) {
		super(first, second, third);
		this.fourth = fourth;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Quad)) return false;
		if (!super.equals(o)) return false;

		Quad quad = (Quad) o;

		if (fourth != null ? !fourth.equals(quad.fourth) : quad.fourth != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (fourth != null ? fourth.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "Quad{" + this.first + "," + this.second + "," + this.third + ","+this.fourth+"}";
	}

	public Quad<A, B, C, D> duplicate() {
		return new Quad(this.first instanceof Mutable ? ((Mutable) this.first).duplicate() : this.first, this.second instanceof Mutable ? ((Mutable) this.second).duplicate() : this.second,
				this.third instanceof Mutable ? ((Mutable) this.third).duplicate() : this.third, this.fourth instanceof Mutable ? ((Mutable) this.fourth).duplicate() : this.fourth);
	}
}
