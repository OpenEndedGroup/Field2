package fieldbox.boxes.plugins;

import com.sun.istack.internal.Nullable;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for little pieces of static helpfulness
 */
public class PluginUtils {

	/**
	 * Adds a command to a box (it will keep all previously added commands as well. If you want to finesse when this command appears use the 5 argument version of this
	 */
	static public void makeCommand(Box destination, String commandName, String documentation, Runnable doit)
	{
		makeCommand(destination,commandName, documentation, doit, null);
	}

	/**
	 * Adds a command to a box (it will keep all previously added commands as well. This command will only appear if check() returns true.
	 */
	static public void makeCommand(Box destination, String commandName, String documentation, Runnable doit, @Nullable Supplier<Boolean> check)
	{
		Supplier<Map<Pair<String, String>, Runnable>> previously = destination.properties.get(RemoteEditor.commands);
		destination.properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			if (check==null || check.get()) {
				m.put(new Pair<>(commandName, documentation), doit);
			}
			if (previously!=null)
			{
				Map<Pair<String, String>, Runnable> p = previously.get();
				if (p!=null)
				{
					m.putAll(p);
				}
			}
			return m;
		});

	}

}
