package fieldbox;

import field.utility.Log;
import field.utility.Options;
import fieldagent.Main;
import fieldbox.boxes.plugins.PluginList;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class Run {
	static public void main(String[] s) {

		LoggingDefaults.initialize();

		if (Main.os == Main.OS.mac) Toolkit.getDefaultToolkit();

		// TODO --- get from command line / previous
		Options.parseCommandLine(s);

		PluginList pluginList;
		try {
			pluginList = new PluginList();
			Map<String, List<Object>> plugins = pluginList.read(System.getProperty("user.home") + "/.field/plugins.edn", true);

			if (plugins != null) pluginList.interpretClassPathAndOptions(plugins);
		} catch (IOException e) {
			e.printStackTrace();
			pluginList = null;
		}

		String mainClass = Options.getString("main", () -> "-main NOT SPECIFIED");

		try {
			Class<?> c = Run.class.getClassLoader()
					      .loadClass(mainClass);
			Object o = c.getConstructor()
				    .newInstance();
			if (o instanceof Runnable) ((Runnable) o).run();

		} catch (ClassNotFoundException e) {
			Log.log("startup.error", ()->"couldn't find class to run <" + mainClass + ">");
			e.printStackTrace();
			System.exit(1);
		} catch (InvocationTargetException e) {
			Log.log("startup.error", ()->"couldn't construct to run <" + mainClass + ">");
			e.printStackTrace();
			System.exit(1);
		} catch (NoSuchMethodException e) {
			Log.log("startup.error", ()->"couldn't construct to run <" + mainClass + ">");
			e.printStackTrace();
			System.exit(1);
		} catch (InstantiationException e) {
			Log.log("startup.error", ()->"couldn't construct to run <" + mainClass + ">");
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalAccessException e) {
			Log.log("startup.error", ()->"couldn't construct to run <" + mainClass + ">");
			e.printStackTrace();
			System.exit(1);
		}

		System.exit(0);

	}
}
