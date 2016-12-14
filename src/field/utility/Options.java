package field.utility;

import field.app.RunLoop;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

	public Map<Dict.Prop, Object> allAccessed = new LinkedHashMap<>();

	public Dict o = new Dict() {
		@Override
		public <T> T get(Prop<T> key) {
			T v = super.get(key);
			allAccessed.put(key, v);
			return v;
		}
	};

	protected Options() {
		RunLoop.main.onExit(() -> {

			try (PrintWriter b = new PrintWriter(new FileWriter(System.getProperty("user.home") + "/.field/options_accessed.edn"))) {
				allAccessed.entrySet().forEach(e -> {
					// TODO: make this actually edn
					b.println("{:" + e.getKey() + " " + e.getValue() + "} ; " + o.dictionary.get(e.getKey()));
				});
			} catch (IOException e) {
				System.err.println(" error saving out accessed options");
			}
		});
	}

	static public String[] remainingArgs;

	static public void parseCommandLine(String[] arg) {

		int lastArg = 0;

		for (int i = 0; i < arg.length; i++) {
			if (arg[i].startsWith("-")) {
				String q = arg[i].substring(1);

				if (arg[i].startsWith("--")) q = arg[i].substring(2);

				if (i < arg.length - 1) {
					String v = arg[i + 1];

					Number n = toNumber(v);
					if (n!=null)
						options.o.put(new Dict.Prop<Number>(q), n);
					else
						options.o.put(new Dict.Prop<String>(q), v);
					i+=1;
				}
				lastArg = i+1;
			}
		}

		if (lastArg<arg.length) {
			remainingArgs = new String[arg.length - lastArg];
			System.arraycopy(arg, lastArg, remainingArgs, 0, remainingArgs.length);
		}
		else
		{
			remainingArgs = new String[0];
		}

	}

	static public Dict dict() {
		return options.o;
	}

	static public Set<String> identifiers() {
		return options.o.getMap().keySet().stream().map(x -> x.getName()).filter(Options::isValidIdentifier).collect(Collectors.toSet());
	}

	/**
	 * checks to see if this can be made an Integer, a Long, a Float, or a Double (in that order)
	 * <p>
	 * returns null on failure;
	 * <p>
	 * We often want Number representations of command line things rather than string representations
	 */
	static public Number toNumber(Object s) {
		if (s instanceof Number) return ((Number) s);
		if (s instanceof Boolean) return ((Boolean) s).booleanValue() ? 1 : 0;
		try {
			return Integer.parseInt("" + s);
		} catch (NumberFormatException e) {
			try {
				return Long.parseLong("" + s);
			} catch (NumberFormatException e2) {
				try {

					return Float.parseFloat("" + s);
				} catch (NumberFormatException e3) {
					try {
						return Double.parseDouble("" + s);
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


	public static String getString(String key, Supplier<String> def) {
		return ""+dict().computeIfAbsent(new Dict.Prop(key), (k) -> def.get());
	}

	public static String getDirectory(String key, Supplier<String> def) {
		String v = getString(key, def);
		return v.endsWith("/") ? v : (v+"/");
	}
}
