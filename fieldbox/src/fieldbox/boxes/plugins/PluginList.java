package fieldbox.boxes.plugins;

import field.utility.Options;
import fieldagent.Main;
import fieldagent.Trampoline;
import fieldbox.boxes.Box;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EDN based list of plugins to load — extends classloader with a classpath, sets and extends Options (see Options.java), and instantiates and loads
 * plugins into the graph
 */
public class PluginList {

	private final Parser parser;

	public PluginList() {
		Parser.Config.Builder builder = Parsers.newParserConfigBuilder();
		parser = Parsers.newParser(builder.build());
	}

	public Map<String, List<Object>> read(String filename, boolean createOnDemand) throws IOException {

		if (!new File(filename).exists()) {
			if (createOnDemand) createBlankFile(filename);
			return Collections.emptyMap();
		}

		try (FileReader fr = new FileReader(new File(filename))) {
			Parseable parseme = Parsers.newParseable(fr);

			Map<String, List<Object>> r = new LinkedHashMap<>();

			while (true) {
				Object o = parser.nextValue(parseme);
				if (o.equals(parser.END_OF_INPUT)) break;

				massageAndCollapse(o, r);
			}
			return r;
		}
	}

	public void interpretMap(Map<String, List<Object>> o, Box root) {
		// note the order here is important: we extend the classpath, set options, extend options and then activate the plugins.
		o.entrySet().forEach(e -> {
			if (e.getKey().toLowerCase().equals("classpath")) {
				extendClassPath(e.getValue());
			} else if (e.getKey().toLowerCase().equals("plugin")) {
			} else if (e.getKey().endsWith("+")) {
			} else {
			}
		});

		o.entrySet().forEach(e -> {
			if (e.getKey().toLowerCase().equals("classpath")) {
			} else if (e.getKey().toLowerCase().equals("plugin")) {
			} else if (e.getKey().endsWith("+")) {
			} else {
				setOption(e.getKey(), e.getValue());
			}
		});
		o.entrySet().forEach(e -> {
			if (e.getKey().toLowerCase().equals("classpath")) {
			} else if (e.getKey().toLowerCase().equals("plugin")) {
			} else if (e.getKey().endsWith("+")) {
				extendOption(e.getKey().substring(0, e.getKey().length() - 1), e.getValue());
			} else {
			}
		});
		o.entrySet().forEach(e -> {
			if (e.getKey().toLowerCase().equals("classpath")) {
			} else if (e.getKey().toLowerCase().equals("plugin")) {
				activeatePlugin(e.getValue(), root);
			} else if (e.getKey().endsWith("+")) {
			} else {
			}
		});
	}

	private void activeatePlugin(List<Object> value, Box root) {

		for (Object o : value) {
			try {
				Box plugin = (Box) this.getClass().getClassLoader().loadClass(o.toString()).getConstructor(Box.class)
					    .newInstance(root);
				plugin.connect(root);

			} catch (Throwable e) {
				System.out.println(" -- problem launching plugin \"" + o + "\", will continue on regardless -- ");
				e.printStackTrace();
			}
		}
	}

	private void extendOption(String substring, List<Object> value) {

	}

	private void setOption(String key, List<Object> value) {

	}

	private void extendClassPath(List<Object> value) {

		//TODO: handle relative paths!

		for (Object o : value) {
			String m = o.toString();
			System.out.println(" extending classpath <"+m+">");
			try {
				Trampoline.addURL(new URL("file:" + m));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}


	private void createBlankFile(String filename) throws IOException {
		new File(filename).getParentFile().mkdirs();
		try (PrintWriter p = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {
			p.println("; this is a Field plugin configuration file\n" +
				    "; lines that start with ; are comments\n" +
				    "; some examples\t:-\n" +
				    "; {:something 10 :something-else \"banana\"} ; sets some Options (equivalent to command line arguments)\n" +
				    "; {:something+ 10 } ; appends an option to a list \n" +
				    "; {:classpath \"/absolute/path/to/someplace/foo.jar\"} ; adds a .jar file to the classpath\n" +
				    "; {:classpath [\"./some/relative/path\" \"./some/place/else.jar\"] } ; adds several paths to the classpath\n" +
				    "; {:sourcepath \"/absolute/path/to/somplace/foo-src.jar\"} ; adds a path to the sourcepath (used for completion and docs)\n" +
				    "; {:plugin some.lovely.plugin.MainClass} ; add a plugin (instantiate this class\tand add\tit to the box graph)\n" +
				    "; -----------------------------\n\n");
		}
	}

	private void massageAndCollapse(Object o, Map<String, List<Object>> r) {
		System.out.println(" read :" + o + " of " + (o == null ? null : o.getClass()));

		if (o instanceof Map) {
			System.out.println("        -- map " + ((Map) o).keySet() + " : " + ((Map) o).values());

			((Map<Object, Object>) o).entrySet().forEach(x -> {
				r.computeIfAbsent(asKey(x.getKey()), k -> new ArrayList<Object>()).addAll(asList(x.getValue()));
			});
		} else throw new IllegalArgumentException(" can't parse object <" + o + "> in plugins file (expected a map)");

	}

	private String trimColon(String s) {
		return (s.startsWith(":")) ? s.substring(1) : s;
	}

	private List<Object> asList(Object value) {
		if (value instanceof Collection) {
			return ((Collection<Object>) value).stream().map(this::asValue).collect(Collectors.toList());
		}

		return Collections.singletonList(asValue(value));
	}

	private String asKey(Object v) {
		return trimColon(v.toString());
	}

	private Object asValue(Object v) {
		if (v instanceof Map) {

			Map<String, Object> v2 = new HashMap<>();
			((Map<Object, Object>) v).entrySet().forEach(e -> v2.put(e.getKey().toString(), asValue(e.getValue())));
			return v2;
		}
		if (v instanceof Number) {
			return v;
		} else return v.toString();
	}
}
