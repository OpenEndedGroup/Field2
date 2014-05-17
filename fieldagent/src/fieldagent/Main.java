package fieldagent;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;


public class Main {

	public static final String app = System.getProperty("appDir")+"/";

//	static Set<String> whitelist_prefix = new LinkedHashSet<>(Arrays.asList("field/"));
//	static Set<String> blacklist_prefix = new LinkedHashSet<>(Arrays.asList("--nothing--"));


	public static void premain(String agentArgs, Instrumentation inst) {
		Transform transform = new Transform();

		if (false) inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {

			if (className.contains("$$Lambda")) return classfileBuffer;

			return classfileBuffer;
/*
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
			*/
		});


	}
}