package fieldbox;

import field.graphics.*;
import fieldbox.io.IO;

import java.awt.*;

/**
 * Created by marc on 3/21/14.
 */
public class FieldBox {

	static public final FieldBox fieldBox = new FieldBox();


	// TODO --- there needs to be mechanism to set this from someplace other than my home directory
	public final IO io = new IO("/Users/marc/Documents/FirstNewFieldWorkspace/");

	{
		io.addFilespec("code", io.EXECUTION, io.EXECUTION);

		io.addFilespec("fragment", ".glslf", "glsl");
		io.addFilespec("vertex", ".glslf", "glsl");
		io.addFilespec("geometry", ".glslg", "glsl");
	}

	public void go() {
		RunLoop.main.enterMainLoop();
	}


	static public void main(String[] s) {

		Toolkit.getDefaultToolkit();

		// TODO --- get from command line / previous
		Open open = new Open("testFile.field2");

		// TODO --- save automatically on exit
		fieldBox.go();
	}



}
