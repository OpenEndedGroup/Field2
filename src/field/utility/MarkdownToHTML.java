package field.utility;

import com.github.rjeschke.txtmark.Processor;

/**
 * Created by marc on 6/2/16.
 */
public class MarkdownToHTML {

	static public String convert(String md)
	{
		return md==null ? "" : Processor.process(md);
	}

	public static String unwrapFirstParagraph(String convert) {
		return convert.replaceFirst("<p>", "").replaceFirst("</p>", "");
	}
}
