package fieldbox;

import field.app.RunLoop;
import field.utility.Options;
import fieldagent.Main;
import fieldbox.io.IO;
import fieldcef.browser.CefSystem;

import java.awt.*;

/**
 * The main entry-point for Field2.
 */
public class FieldBox {

	static public final FieldBox fieldBox = new FieldBox();
	static public String[] args;

	public IO io;

	public void go() {
		RunLoop.main.enterMainLoop();
	}


	static public void main(String[] s) {
		args = s;

		System.err.println(" lauching toolkit");
		if (Main.os == Main.OS.mac)
			Toolkit.getDefaultToolkit();

		// experimenting with moving this initialization first. Seems to remove the occasional crash on startup?
		System.err.println(" building the CefSystem");
		CefSystem sys = CefSystem.cefSystem;
		System.err.println(" finished building the CefSystem");

		// TODO --- get from command line / previous
		Options.parseCommandLine(s);

		LoggingDefaults.initialize();

		fieldBox.io = new IO(Options.getDirectory("workspace", () -> System.getProperty("user.home") + "/Documents/FirstNewFieldWorkspace/"));
		fieldBox.io.addFilespec("code", IO.EXECUTION, IO.EXECUTION);

		new Open(Options.getString("file", () -> "testIB.field2"));

		SplashScreen splash = SplashScreen.getSplashScreen();
		if (splash!=null)
			splash.close();

		fieldBox.go();
	}


}
