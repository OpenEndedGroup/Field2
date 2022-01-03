package field.utility;

/**
 * Created by marc on 3/25/14.
 */
public class Quin<A, B, C, D, E> extends Quad<A, B, C, D> {
	final public E fifth;

	public Quin(A first, B second, C third, D fourth, E fifth) {
		super(first, second, third, fourth);
		this.fifth = fifth;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Quin)) return false;
		if (!super.equals(o)) return false;

		Quin quin = (Quin) o;

		return !(fifth != null ? !fifth.equals(quin.fifth) : quin.fifth != null);

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (fifth != null ? fifth.hashCode() : 0);
		return result;
	}

	public String toString() {
		return String.format("Quin{%s,%s,%s,%s,%s}", this.first, this.second, this.third, this.fourth, this.fifth);
	}

	public Quin<A, B, C, D, E> duplicate() {
		return new Quin(this.first instanceof Mutable ? ((Mutable) this.first).duplicate() : this.first,
						this.second instanceof Mutable ? ((Mutable) this.second).duplicate() : this.second,
						this.third instanceof Mutable ? ((Mutable) this.third).duplicate() : this.third,
						this.fourth instanceof Mutable ? ((Mutable) this.fourth).duplicate() : this.fourth,
						this.fifth instanceof Mutable ? ((Mutable) this.fifth).duplicate() : this.fifth);
	}
}
