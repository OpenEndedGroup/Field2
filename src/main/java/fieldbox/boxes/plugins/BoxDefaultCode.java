package fieldbox.boxes.plugins;

import field.utility.Dict;
import fieldagent.Main;
import fieldbox.boxes.Box;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A mechanism for producing default entries in "_.code" for various plugin classes directly from the class-path
 */
public class BoxDefaultCode {

    static public final Dict.Prop<Boolean> _configured = new Dict.Prop<Boolean>("_configured").toCanon().set(Dict.domain, "*/attributes");
    static public List<String> extensions = new ArrayList<>(Arrays.asList("py", "js", "html", "css", "txt", "md", "", "glslf", "glslv", "glslg", "kt_", "ktt"));

    static public void configure(Box a) {

        Dict.canonicalProperties()
                .filter(x -> x.getAttributes()
                        .isTrue(_configured, false)).collect(Collectors.toList()) // needed to avoid CME
                .forEach(x -> {
                    String c = find(a, x.getName());
                    if (c != null)
                        a.properties.put(x, c);
                });
    }

    static public String find(Box a, String propertyName) {
        Class c = a.getClass();
        while (c != null) {
            String code = findSource(c, propertyName);
            if (code != null) {
                return code;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    static public String find(Class<? extends Box> a, String propertyName) {
        Class c = a;
        while (c != null) {
            String code = findSource(c, propertyName);
            if (code != null) {
                return code;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    static HashSet<String> failed = new HashSet<>();

    public static String findSource(Class c, String propertyName) {
        String n = c.getName();
        n = n.replaceAll("\\.", "/");
        n = n + "." + propertyName;

        if (failed.contains(n)) return null;
        for (String ex : extensions) {
            URL is = Thread.currentThread().getContextClassLoader().getResource(n + (ex.length() > 0 ? ("." + ex) : ""));
            if (is != null) {
                try {

                    System.out.println(" loading from '" + is + "'");

                    String nn = is.toString();
                    if (Main.os == Main.OS.windows) {
                        nn = nn.replace("file:/", "");
                        nn = nn.replace("file:", "");
                    } else {
                        nn = nn.replace("file:", "");
                    }

                    System.out.println(" >> " + nn);

                    return new String(Files.readAllBytes(Paths.get(nn)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
//				catch (URISyntaxException e) {
//					e.printStackTrace();
//				}

            }
        }
//        System.out.println(" -- failed to find source for "+c+" / "+propertyName);
        failed.add(n);
        return null;

    }

    public static Path findSourceLocation(Class c, String propertyName) {
        String n = c.getName();
        n = n.replaceAll("\\.", "/");
        n = n + "." + propertyName;

        for (String ex : extensions) {
            URL is = Thread.currentThread().getContextClassLoader().getResource(n + (ex.length() > 0 ? ("." + ex) : ""));
            if (is != null) {


                System.out.println(" loading from '" + is + "'");

                String nn = is.toString();
                if (Main.os == Main.OS.windows) {
                    nn = nn.replace("file:/", "");
                    nn = nn.replace("file:", "");
                } else {
                    nn = nn.replace("file:", "");
                }

                System.out.println(" >> " + nn);

                return Paths.get(nn);

//				catch (URISyntaxException e) {
//					e.printStackTrace();
//				}

            }
        }
        return null;

    }


}
