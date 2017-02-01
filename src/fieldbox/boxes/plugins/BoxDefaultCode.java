package fieldbox.boxes.plugins;

import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldbox.execution.Execution;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A mechanism for producing default entries in "_.code" for various plugin classes directly from the class-path
 */
public class BoxDefaultCode {

	static public List<String> extensions = new ArrayList<>(Arrays.asList("js", "html", "css", "txt"));

	static public final Dict.Prop<Boolean> _configured = new Dict.Prop<Boolean>("_configured").toCannon().set(Dict.domain, "*/attributes");

	static public void configure(Box a) {

		Dict.cannonicalProperties().filter(x -> x.getAttributes().isTrue(_configured, false)).forEach(x -> {

			String c = find(a, "." + x.getName());
			if (c != null)
				a.properties.put(Execution.code, c);
		});
	}

	static public String find(Box a, String propertyName) {
		Class c = a.getClass();
		while (c != null) {
			String code = find(a, c, propertyName);
			if (code != null) {
				return code;
			}
			c = c.getSuperclass();
		}
		return null;
	}

	private static String find(Box a, Class c, String propertyName) {
		String n = c.getName();
		n = n.replaceAll("\\.", "/");
		n = n + propertyName;

		for (String ex : extensions) {
			URL is = Thread.currentThread().getContextClassLoader().getResource(n + "." + ex);
			if (is != null) {
				try {
					return new String(Files.readAllBytes(Paths.get(is.toURI())));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}

			}
		}
		return null;

	}

}
