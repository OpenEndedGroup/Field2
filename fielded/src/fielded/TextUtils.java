package fielded;

import org.json.JSONObject;

/**
 * Utilities for quoting and unquoting text to and from html
 */
public class TextUtils {

	/**
	 * make a string safe for inclusion in JSON
	 */
	static public String quote(String s)
	{
		return JSONObject.quote(s);
	}


	/**
	 * make a string safe for inclusion in HTML
	 */
	static public String html(String s)
	{
		return s.replace("<", "&lt;").replace(">", "&rt;").replace("&", "&amp;");
	}


}
