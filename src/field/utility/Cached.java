package field.utility;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Handy Utility class for guarding computation of function with a check-function
 * <p>
 * This is a Function<t_check, t_value> and you can give a function 'witness' that's <t_check, t_witness>. If the witness function returns something
 * that's different from the last time it returned (or this cache has been marked as invalid manually) then the original function is executed and it's
 * return value stored and return. Otherwise, you get the same result as before.
 */
public class Cached<t_check, t_witness, t_value> implements Function<t_check, t_value> {

	private BiFunction<t_check, t_value, t_value> compute;
	private final Function<t_check, t_witness> witness;
	private t_witness valid;
	private t_value value;
	private boolean invalid = false;
	private String debug = null;

	public Cached(BiFunction<t_check, t_value, t_value> compute, Function<t_check, t_witness> witness) {
		this.witness = witness;
		this.compute = compute;
		invalid = true;
	}

	public Cached<t_check, t_witness, t_value> setCompute(BiFunction<t_check, t_value, t_value> compute) {
		this.compute = compute;
		return this;
	}

	public Cached<t_check, t_witness, t_value> invalidate() {
		if (debug != null) Log.log("cached." + debug, " cache invalidated :");
		invalid = true;
		return this;
	}

	public t_value apply(t_check check) {
		t_witness w = witness.apply(check);

		if (invalid || !Util.safeEq(w, valid)) {
			if (debug != null) {
				Log.log("cached." + debug, " cache invalid :" + invalid + " " + w + " " + valid + " " + Util.safeEq(w, valid));
			}
			value = compute.apply(check, value);
			valid = w;

			if (valid instanceof Mutable)
				valid = (t_witness) ((Mutable)valid).duplicate();

			invalid = false;
		} else if (debug != null)
			Log.log("cached." + debug, " cache valid :" + invalid + " " + w + " " + valid + " " + Util.safeEq(w, valid));
		return value;
	}

	public Cached<t_check, t_witness, t_value> debugOn(String name) {
		this.debug = name;
		return this;
	}


	public Supplier<t_value> toSupplier(Supplier<t_check> q)
	{
		return () -> apply(q.get());
	}

}
