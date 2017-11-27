package field.utility;

import com.github.rjeschke.txtmark.BlockEmitter;
import com.github.rjeschke.txtmark.Configuration;
import com.github.rjeschke.txtmark.Processor;

/**
 * Created by marc on 6/2/16.
 */
public class MarkdownToHTML {

	static public String convert(String md)
	{
		return md==null ? "" : Processor.process(md);
	}

	static public String convertFully(String md, BlockEmitter code)
	{
		if (md==null) return "";

		return Processor.process(md, Configuration.builder().forceExtentedProfile().setCodeBlockEmitter(code).build());
	}

	public static String unwrapFirstParagraph(String convert) {
		return convert.replaceFirst("<p>", "").replaceFirst("</p>", "");
	}
}
