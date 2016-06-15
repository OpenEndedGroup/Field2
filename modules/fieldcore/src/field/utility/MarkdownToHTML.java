package field.utility;

import com.github.rjeschke.txtmark.Processor;

/**
 * Created by marc on 6/2/16.
 */
public class MarkdownToHTML {

	static public String convert(String md)
	{
		return Processor.process(md);
	}

}
