package field.utility;

import field.utility.Util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Cached<t_check, t_witness, t_value> implements Function<t_check, t_value> {

	private BiFunction<t_check, t_value, t_value> compute;
	private final Function<t_check, t_witness> witness;
	private t_witness valid;
	private t_value value;
	private boolean invalid = false;
	private String debug = null;

	public Cached(BiFunction<t_check, t_value, t_value> compute, Function<t_check,t_witness> witness)
	{
		this.witness = witness;
		this.compute = compute;
		invalid = true;
	}

	public Cached<t_check, t_witness, t_value> setCompute(BiFunction<t_check, t_value, t_value> compute) {
		this.compute = compute;
		return this;
	}

	public Cached<t_check, t_witness, t_value> invalidate()
	{
		if (debug!=null)
			System.out.println(" cache invalidated :"+debug);
		invalid = true;
		return this;
	}

	public t_value apply(t_check check)
	{
		t_witness w = witness.apply(check);

		if (invalid || !Util.safeEq(w, valid))
		{
			if (debug!=null)
			{
				System.out.println(" cache invalid "+debug+" :"+invalid+" "+w+" "+valid+" "+Util.safeEq(w, valid));
			}
			value = compute.apply(check, value);
			valid = w;
			invalid = false;
		}
		else if (debug!=null) System.out.println(" cache valid "+debug+" :"+invalid+" "+w+" "+valid+" "+Util.safeEq(w, valid));
		return value;
	}

	public Cached<t_check, t_witness, t_value> debugOn(String name)
	{
		this.debug = name;
		return this;
	}


}
