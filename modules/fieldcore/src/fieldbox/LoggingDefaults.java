package fieldbox;

import field.utility.Log;

/**
 * Created by marc on 7/5/14.
 */
public class LoggingDefaults {

	public static void initialize() {

		Log.on(".*.autopersist", Log::blue);
//		Log.on("auto", Log::green);
//		Log.on("io.general", Log::green);
//		Log.on("completion.debug", Log::green);
		Log.on("watching.*", Log::green);
//		Log.on(".*selection.*", Log::green);
//		Log.on(".*serial.*", Log::green);
//		Log.on("nashorn.general", Log::green);
//		Log.on("INSERT", Log::green);
//		Log.on("tap.*", Log::green);
		Log.on("completion.debug", Log::green);
		Log.on(".*error", Log::red);
//		Log.on("remote.cookie", Log::green);
//		Log.on("server", Log::green);
//		Log.on("python.debug", Log::green);
//		Log.on("calllogic", Log::green);
//		Log.on("tap.*", Log::green);
//		Log.on(".*startup.*", Log::blue);
//		Log.on(".*error.*", Log::red);
//		Log.on("cef.debug*", Log::red);
////		Log.on(".*trace.*", Log::blue);
//		Log.off(".*trace.*");
//		Log.on("graphics.trace", Log::green);
//		Log.on("texture.trace", Log::green);
//		Log.on("texture.trace2", Log::green);
//		Log.disable("keyboard2");
//		Log.disable("keyboard");
//		Log.disable("event.debug");
//		Log.on(".*keyboard.*", Log::green);
//		Log.disable(".*cache.*");
//		Log.on(".*keyboard.*", Log::green);
		Log.on(".*jar.indexer.*", Log::green);
//		Log.on("finalkey", Log::green);
		Log.fallthrough((a,b) -> {});
	}
}
