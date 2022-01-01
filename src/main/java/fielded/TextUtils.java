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
	 * like quote, but without the leading and trailing " characters
	 */
	static public String quoteNoOuter(String s)
	{
		String q = JSONObject.quote(s);
		return q.substring(1, q.length()-1);
	}


	/**
	 * make a string safe for inclusion in HTML
	 */
	static public String html(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}


}
