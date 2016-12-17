package fielded.boxbrowser;

import field.utility.IdempotencyMap;
import fieldcef.browser.Browser;
import org.json.JSONObject;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Short-lived registerable commands that can be executed straightforwardly by links embedded in the text editor
 */
public class TransientCommands {

	static public final TransientCommands transientCommands = new TransientCommands();

	IdempotencyMap<Browser.Handler> h = new IdempotencyMap<Browser.Handler>(Browser.Handler.class);

	public boolean handle(String address, JSONObject payload, Consumer<String> reply) {
		if (h.containsKey(address)) {
			Browser.Handler m = h.get(address);
			if (m != null) {
				m.handle(address, payload, reply);
				return true;
			}
		}
		return false;
	}

	public String register(Browser.Handler h) {
		String uid = UUID.randomUUID().toString().replaceAll("-", "");
		this.h.put(uid, h);
		return uid;
	}

	public String refForCommand(String body, Runnable r) {

		String u = register((a, p, rr) -> {
			r.run();
		});
		return "<a href='#' onclick=\"_field.send('" + u + "', {})\">" + body + "</a>";
	}

}
