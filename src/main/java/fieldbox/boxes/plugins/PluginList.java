package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Log;
import field.utility.Options;
import fieldagent.Main;
import fieldbox.boxes.Box;
import fieldbox.boxes.Callbacks;
import us.bpsm.edn.EdnSyntaxException;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static fieldbox.io.IO.Loaded;

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

        try {
            if (!new File(filename).exists()) {
                if (createOnDemand) createBlankFile(filename);
                return Collections.emptyMap();
            }

            try (FileReader fr = new FileReader(new File(filename))) {
                Parseable parseme = Parsers.newParseable(fr);

                Map<String, List<Object>> r = new LinkedHashMap<>();

                while (true) {
                    Object o = parser.nextValue(parseme);
                    if (o.equals(Parser.END_OF_INPUT)) break;

                    massageAndCollapse(o, r);
                }
                return r;
            }
        } catch (EdnSyntaxException syntax) {
            syntax.printStackTrace();
            Log.log("startup.error", () -> "Syntax error in plugins.edn file " + syntax + " " + filename);
            return null;
        }
    }


    public void interpretClassPathAndOptions(Map<String, List<Object>> o) {
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
    }

    public void interpretPlugins(Map<String, List<Object>> o, Box root) {
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

        Map<Object, Loaded> loaded = new LinkedHashMap<>();
        for (Object o : value) {
            Log.log("startup", () -> ">>>>>>>>>>>>> activating plugin '" + o + "'");

            try {
                Box plugin = (Box) this.getClass().getClassLoader().loadClass(o.toString()).getConstructor(Box.class)
                        .newInstance(root);
                plugin.connect(root);
                if (plugin instanceof Loaded)
                    loaded.put(o, (Loaded) plugin);

            } catch (Throwable e) {
                System.err.println(" -- problem activating plugin \"" + o + "\", will continue on regardless -- ");
                e.printStackTrace();
            }
            Log.log("startup", () -> "<<<<<<<<<<<<< finished plugin '" + o + "'");
        }

        for (Map.Entry<Object, Loaded> o : loaded.entrySet()) {
            Log.log("startup", () -> ">>>>>>>>>>>>> late initialization for plugin '" + o.getValue() + "'");

            try {
                o.getValue().loaded();
                Callbacks.load((Box) o.getValue());
            } catch (Throwable e) {
                System.err.println(" -- problem activating plugin \"" + o + "\", will continue on regardless -- ");
                e.printStackTrace();
            }
            Log.log("startup", () -> "<<<<<<<<<<<<< late initialization for plugin '" + o + "' finished");
        }


    }

    private void extendOption(String key, List<Object> value) {
        for (Object v : value)
            Options.dict().putToList(new Dict.Prop<List<Object>>(key), v);
    }

    private void setOption(String key, List<Object> value) {

        if (value.size() == 1) Options.dict().put(new Dict.Prop(key), value.get(0));
        else Options.dict().put(new Dict.Prop(key), value);

    }

    private void extendClassPath(List<Object> value) {

        for (Object o : value) {
            String m = o.toString();
            if (m.startsWith("{app}"))
                m = m.replace("{app}", Main.app + "/");

            final String finalM = m;
            Log.log("startup", () -> " extending classpath <" + finalM + ">");

            if (m.endsWith("*")) {
                m = m.substring(0, m.length() - 1);
                if (!new File(m).exists()) {
                    final String finalM1 = m;
                    Log.log("startup.error", () -> " adding a path that doesn't exist to the classpath <" + finalM1 + ">, almost certainly a typo in your plugins file");
                }
                try {
                    ca.cgjennings.jvm.JarLoader.addToClassPath(new File(m));
//					Trampoline.addURL(new URL("file:" + m));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                File[] subFiles = new File(m).listFiles((d, n) -> n.endsWith(".jar"));

                if (subFiles != null) {
                    for (File f : subFiles) {
                        Log.log("startup", () -> " extending classpath <" + f.getAbsolutePath() + ">");
                        try {
                            if (f.isDirectory()) {
                                System.err.println(" -- warning, can't add directory " + f + " to classpath, added it to the launch script instead");
                            } else {
                                ca.cgjennings.jvm.JarLoader.addToClassPath(f);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    final String finalM2 = m;
                    Log.log("startup.error", () -> " added a wildcard path <" + finalM2 + "> with no .jar files in it");
                }
                m = m.substring(0, m.length() - 1);
            }

            if (!new File(m).exists()) {
                final String finalM3 = m;
                Log.log("startup.error", () -> " adding a path that doesn't exist to the classpath <" + finalM3 + ">, almost certainly a typo in your plugins file");
            }

            try {
                if (new File(m).isDirectory()) {
                    System.err.println(" -- warning, can't add directory " + m + " to classpath, added it to the launch script instead");
                } else {

                    ca.cgjennings.jvm.JarLoader.addToClassPath(new File(m));
                }
//				Trampoline.addURL(new URL("file:" + m));
            } catch (IOException e) {
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
        if (o instanceof Map) {
            ((Map<Object, Object>) o).entrySet().forEach(x -> {
                r.computeIfAbsent(asKey(x.getKey()), k -> new ArrayList<Object>()).addAll(asList(x.getValue()));
            });
        } else Log.log("startup.error", () -> " can't parse object <" + o + "> in plugins file (expected a map)");

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
