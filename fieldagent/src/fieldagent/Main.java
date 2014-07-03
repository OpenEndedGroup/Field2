package fieldagent;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;


public class Main {

	public enum OS
	{
		linux, mac
	}

	public static final OS os = System.getProperty("os.name").contains("Mac") ? OS.mac : OS.linux;

	public static final String app = System.getProperty("appDir")+"/";

//	static Set<String> whitelist_prefix = new LinkedHashSet<>(Arrays.asList("field/"));
//	static Set<String> blacklist_prefix = new LinkedHashSet<>(Arrays.asList("--nothing--"));


	public static void premain(String agentArgs, Instrumentation inst) {
		//jdk1.8_20's instrumentation support is busted with respect to lambdas. Fortunately, now we have our own classloader we don't care.

		/*Transform transform = new Transform();

		if (false) inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {

			if (className.contains("$$Lambda")) return classfileBuffer;

			boolean found = false;
			for (String w : whitelist_prefix)
				if (className.startsWith(w)) {
					found = true;
					break;
				}

			if (found) for (String w : blacklist_prefix)
				if (className.startsWith(w)) {
					found = false;
					break;
				}

			if (found) {
				return transform.transform(className, classfileBuffer);
			} else return classfileBuffer;
		});*/
	}
}