package fieldbox;

import field.graphics.*;
import field.utility.Options;
import fieldbox.io.IO;

/**
 * The main entry-point for Field2.
 */
public class FieldBox {

	static public final FieldBox fieldBox = new FieldBox();

	public final IO io = new IO("/home/marc/Documents/FirstNewFieldWorkspace/");

	{
		io.addFilespec("code", io.EXECUTION, io.EXECUTION);


	}

	public void go() {
		RunLoop.main.enterMainLoop();
	}


	static public void main(String[] s) {

		Options.parseCommandLine(s);

		Open open = new Open("testFile.field2");

		fieldBox.go();
	}



}
