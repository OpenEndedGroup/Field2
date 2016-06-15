package fieldbox.boxes.plugins;

import fieldbox.boxes.Box;
import fieldbox.execution.Execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A mechanism for producing default entries in "_.code" for various plugin classes directly from the class-path
 */
public class BoxDefaultCode {

	static public void configure(Box a) {
		String c = find(a, ".code.js");
		if (c != null)
			a.properties.put(Execution.code, c);
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
		n = n+propertyName;
		URL is =
			Thread.currentThread().getContextClassLoader().getResource(n);
		if (is != null) {
			try {
				return new String(Files.readAllBytes(Paths.get(is.toURI())));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

		}
		return null;

	}

}
