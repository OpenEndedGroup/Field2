package field.utility;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Command line options, statically accessible from everywhere.
 * <p>
 * I've found that command line options are one place that tends to couple pieces of otherwise unrelated code bidirectionally. Let's just accept the
 * static here and move on.
 * <p>
 * Initialize this with parseCommandLine(String[]) from main
 */
public class Options {

	static public final Options options = new Options();

	public Map<String, String> o = Collections.synchronizedMap(new LinkedHashMap<>());

	static public void parseCommandLine(String[] arg) {

		for (int i = 0; i < arg.length; i++) {
			if (arg[i].startsWith("-")) {
				String q = arg[i].substring(1);

				if (arg[i].startsWith("--")) q = arg[i].substring(2);

				if (i < arg.length - 1) {
					String v = arg[i + 1];
					options.o.put(q, v);
				}
			}
		}
	}

	static public String getString(String name, Supplier<String> def)
	{
		return options.o.computeIfAbsent(name, x->def.get());
	}


	static public String getDirectory(String name, Supplier<String> def)
	{
		String d = options.o.computeIfAbsent(name, x -> def.get());
		return d.endsWith("/") ? d : (d+"/");
	}

	static public Integer getInt(String name, Supplier<Integer> def)
	{
		Integer n = (Integer) toNumber(options.o.computeIfAbsent(name, x -> "" + def.get()));
		if (n==null)
		{
			options.o.put(name, ""+def.get());
			return def.get();
		}
		else return n;
	}


	static public Set<String> identifiers() {
		return options.o.values().stream().filter(Options::isValidIdentifier).collect(Collectors.toSet());
	}

	/**
	 * checks to see if this can be made an Integer, a Long, a Float, or a Double (in that order)
	 * <p>
	 * returns null on failure;
	 *
	 * We often want Number representations of command line things rather than string representations
	 */
	static public Number toNumber(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e2) {
				try {

					return Float.parseFloat(s);
				} catch (NumberFormatException e3) {
					try {
						return Double.parseDouble(s);
					} catch (NumberFormatException e4) {
						return null;
					}
				}
			}
		}
	}

	private static boolean isValidIdentifier(String x) {
		if (x.length() == 0) return false;
		if (!Character.isJavaIdentifierStart(x.charAt(0))) return false;
		for (int i = 1; i < x.length(); i++)
			if (!Character.isJavaIdentifierPart(x.charAt(i))) return false;
		return true;
	}


}
