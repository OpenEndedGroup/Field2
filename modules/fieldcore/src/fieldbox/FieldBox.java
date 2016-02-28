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

	public IO io;


	public void go() {
		RunLoop.main.enterMainLoop();
	}


	static public void main(String[] s) {

		// experimenting with moving this initialization first. Does this remove the occasional crash on startup?
		new Thread(() -> {
			System.err.println(" building the CefSystem");
			CefSystem sys = CefSystem.cefSystem;
			System.err.println(" finished building the CefSystem");
		}).start();

		if (Main.os== Main.OS.mac)
			Toolkit.getDefaultToolkit();

		// TODO --- get from command line / previous
		Options.parseCommandLine(s);


		fieldBox.io = new IO(Options.getDirectory("workspace",() -> System.getProperty("user.home")+"/Documents/FirstNewFieldWorkspace/"));
		fieldBox.io.addFilespec("code", IO.EXECUTION, IO.EXECUTION);
		LoggingDefaults.initialize();

		Open open = new Open(Options.getString("file", () -> "testIB.field2"));

		fieldBox.go();

	}



}
