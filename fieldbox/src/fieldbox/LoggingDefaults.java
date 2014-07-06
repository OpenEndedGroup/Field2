package fieldbox;

import field.utility.Log;

/**
 * Created by marc on 7/5/14.
 */
public class LoggingDefaults {

	public static void initialize() {

		Log.on(".*startup.*", Log::blue);
		Log.disable(".*trace.*");

	}
}
