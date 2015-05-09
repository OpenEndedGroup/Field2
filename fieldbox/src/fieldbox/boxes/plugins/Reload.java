package fieldbox.boxes.plugins;


import field.utility.Log;
import field.utility.Pair;
import fieldagent.Trampoline;
import fieldbox.boxes.Box;
import fielded.Commands;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Plugin that supports reloading classes on the fly by connecting to a debug port in the running VM.
 */
public class Reload extends Box {

	private List<Map.Entry<Class, Trampoline.Record>> toReload;

	ReloadTarget target = new ReloadTarget();

	public Reload(Box root) {
		Log.on(".*reload.*", Log::red);

		new Thread(() -> {
			int lastNotified = 0;
			while (true) {
				try {
					toReload = Trampoline.loadMap.entrySet().stream().filter(x -> x.getValue().modified())
						    .collect(Collectors.toList());

				} catch (ConcurrentModificationException e) {
					// doesn't matter (will happen if classes are loading while we iterate over it)
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (toReload.size()>lastNotified) {
					lastNotified = toReload.size();
					String classList = toReload.stream().map( x-> x.getKey().getName().substring(x.getKey().getName().lastIndexOf(".")+1)).collect(Collectors.joining(", "));
					if (classList.length()>100) classList = classList.substring(0, 100)+"...";
					Log.log("reload", "There are classes that can be reloaded: " + classList);
					Log.log("reload", "Use the reload command to do so");
				}
			}
		}).start();


		properties.put(Commands.commands, () -> {
			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();


			Log.log("reload", "needing reloading...", toReload);

			List<Map.Entry<Class, Trampoline.Record>> toReload_copy = toReload;

			if (toReload_copy.size() == 0) {
			} else {
				String classList = toReload_copy.stream().map( x-> x.getKey().getName().substring(x.getKey().getName().lastIndexOf(".")+1)).collect(Collectors.joining(", "));
				if (classList.length()>100) classList = classList.substring(0, 100)+"...";
				m.put(new Pair<>("Reload changed classes", "Reloads the class" + (toReload_copy
					    .size() != 1 ? "es" : "") + " that have changed on disk &mdash; <i>" + classList+"</i>"), () -> {
					Hotswapper.swapClass(toReload_copy.stream().map(x ->x .getKey()).collect(Collectors.toList()).toArray(new Class[0]));
					toReload_copy.stream().forEach( x -> Trampoline.loadMap.compute( x.getKey(), (z, r) -> r.update()));
				});
			}

			m.put(new Pair<>("do that thing", "does it work yet?"), () -> Hotswapper.doSomething());


			return m;
		});
	}


}
