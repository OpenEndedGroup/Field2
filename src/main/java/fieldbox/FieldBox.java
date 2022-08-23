package fieldbox;

import field.app.RunLoop;
import field.graphics.Windows;
import field.utility.Dict;
import field.utility.Options;
import field.utility.Releases;
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
	public static String workspace;

	public IO io;

	public void go() {
		RunLoop.main.enterMainLoop();
	}


	static public void main(String[] s) {
//		SwingUtilities.invokeLater(() -> {
		args = s;
		Options.parseCommandLine(s);

		if (s.length>0 && s[s.length-1].equals("upgrade"))
		{
			System.out.println("   -- upgrade --");
			Releases.Companion.upgrade();
			RunLoop.main.enterMainLoop();
		}


		// needs to be initialized after 'args'
		workspace = Options.getDirectory("workspace", () -> System.getProperty("user.home") + "/Documents/FieldWorkspace/");


		System.err.println(" lauching toolkit");
		if (Main.os == Main.OS.mac) {
			System.setProperty("java.awt.headless", "true");
			Toolkit.getDefaultToolkit();

			// this possibly works on windows?

//			Desktop.getDesktop().setOpenFileHandler(new OpenFilesHandler() {
//				@Override
//				public void openFiles(OpenFilesEvent e) {
//					System.out.println("\n\n!! open files !! \n\n" + e);
//				}
//			});
		}

		LoggingDefaults.initialize();

//			try {
//				Thread.sleep(20000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			System.err.println(" finished hanging out");

//		glfwInit();


		new Thread() {
			@Override
			public void run() {
				CefSystem hello = CefSystem.cefSystem;
				System.out.println("final cef system is called "+hello);
			}
		}.start();

		Windows.windows.init();

		fieldBox.io = new IO(workspace);
		fieldBox.io.addFilespec("code", IO.EXECUTION, IO.EXECUTION);

		new Open(Options.getString("file", () -> "testIB.field2"));

//		SplashScreen splash = SplashScreen.getSplashScreen();
//		if (splash != null)
//			splash.close();

		fieldBox.go();
//		});
	}


}
