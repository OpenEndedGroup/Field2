package fieldbox;

import field.utility.Log;

/**
 * Created by marc on 7/5/14.
 */
public class LoggingDefaults {

	public static void initialize() {

		Log.on(".*startup.*", Log::blue);
		Log.on(".*error.*", Log::red);
		Log.disable(".*trace.*");
		Log.on("cef.*", Log::green);

	}
}
