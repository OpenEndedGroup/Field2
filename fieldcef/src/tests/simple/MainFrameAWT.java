// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.simple;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserFactory;
import org.cef.browser.CefBrowser_N;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This is a simple example application using JCEF. It displays a JFrame with a JTextField at its top and a CefBrowser in its center. The JTextField
 * is used to enter and assign an URL to the browser UI. No additional handlers or callbacks are used in this example.
 * <p>
 * The number of used JCEF classes is reduced (nearly) to its minimum and should assist you to get familiar with JCEF.
 * <p>
 * For a more feature complete example have also a look onto the example code within the package "browser.detailed".
 */
public class MainFrameAWT extends JFrame {
	private static final long serialVersionUID = -5570653778104813836L;
	private  JTextField address_;
	private final CefApp cefApp_;
	private final CefClient client_;
	private  CefBrowser browser_;
	private  Component browserUI_;

	/**
	 * To display a simple browser window, it suffices completely to create an instance of the class CefBrowser and to assign its UI component to
	 * your application (e.g. to your content pane). But to be more verbose, this CTOR keeps an instance of each object on the way to the browser
	 * UI.
	 */
	private MainFrameAWT(String startURL, boolean useOSR, boolean isTransparent) {
		// (1) The entry point to JCEF is always the class CefApp. There is only one
		//     instance per application and therefore you have to call the method
		//     "getInstance()" instead of a CTOR.
		//
		//     CefApp is responsible for the global CEF context. It loads all
		//     required native libraries, initializes CEF accordingly, starts a
		//     background task to handle CEF's message loop and takes care of
		//     shutting down CEF after disposing it.
		cefApp_ = CefApp.getInstance();

		// (2) JCEF can handle one to many browser instances simultaneous. These
		//     browser instances are logically grouped together by an instance of
		//     the class CefClient. In your application you can create one to many
		//     instances of CefClient with one to many CefBrowser instances per
		//     client. To get an instance of CefClient you have to use the method
		//     "createClient()" of your CefApp instance. Calling an CTOR of
		//     CefClient is not supported.
		//
		//     CefClient is a connector to all possible events which come from the
		//     CefBrowser instances. Those events could be simple things like the
		//     change of the browser title or more complex ones like context menuSpecs
		//     events. By assigning handlers to CefClient you can control the
		//     behavior of the browser. See browser.detailed.MainFrame for an example
		//     of how to use these handlers.
		client_ = cefApp_.createClient();

		// (3) One CefBrowser instance is responsible to control what you'll see on
		//     the UI component of the instance. It can be displayed off-screen
		//     rendered or windowed rendered. To get an instance of CefBrowser you
		//     have to call the method "createBrowser()" of your CefClient
		//     instances.
		//
		//     CefBrowser has methods like "goBack()", "goForward()", "loadURL()",
		//     and many more which are used to control the behavior of the displayed
		//     content. The UI is held within a UI-Compontent which can be accessed
		//     by calling the method "getUIComponent()" on the instance of CefBrowser.
		//     The UI component is inherited from a java.awt.Component and therefore
		//     it can be embedded into any AWT UI.

		browser_ = client_.createBrowser(startURL, isTransparent, CefBrowserFactory.RenderType.RENDER_AWT_WINDOW);

		browserUI_ = browser_.getUIComponent();

		// (4) For this minimal browser, we need only a text field to enter an URL
		//     we want to navigate to and a CefBrowser window to display the content
		//     of the URL. To respond to the input of the user, we're registering an
		//     anonymous ActionListener. This listener is performed each time the
		//     user presses the "ENTER" key within the address field.
		//     If this happens, the entered value is passed to the CefBrowser
		//     instance to be loaded as URL.
		address_ = new JTextField(startURL, 100);
		address_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browser_.loadString("<html><head></head><body>hello isabel</body></html>", "http://localhost");
//				browser_.loadURL(address_.getText());
			}
		});

		// (5) All UI components are assigned to the default content pane of this
		//     JFrame and afterwards the frame is made visible to the user.
		getContentPane().add(address_, BorderLayout.NORTH);
		if (browserUI_ != null) getContentPane().add(browserUI_, BorderLayout.CENTER);

		pack();
		setSize(800, 600);
		setVisible(true);

		// (6) To take care of shutting down CEF accordingly, it's important to call
		//     the method "dispose()" of the CefApp instance if the Java
		//     application will be closed. Otherwise you'll get asserts from CEF.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
				cefApp_.dispose();
			}
		});

		addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				System.out.println(e);

			}

			@Override
			public void mouseReleased(MouseEvent e) {
				System.out.println(e);

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				System.out.println(e);

			}

			@Override
			public void mouseExited(MouseEvent e) {
				System.out.println(e);

			}
		});

		System.out.println(" -- pausing --- ");
		try {
			Thread.sleep(5000);
		}
		catch(Throwable t)
		{
			t.printStackTrace();;
		}

		System.out.println(" -- mousing around --- ");

//		client_.addMessageRouter();

		browser_.executeJavaScript("console.log('BANANA')", "", 0);

		for(int y=50;y<500;y+=10)
		{
			System.out.println(" - click :"+y);
			((CefBrowser_N)browser_).sendMouseEvent(new MouseEvent(new Component() {
				@Override
				public String getName() {
					return super.getName();
				}

				@Override
				public Point getLocationOnScreen() {
					return new Point(0, 0);
				}
			}, MouseEvent.MOUSE_CLICKED, 0, MouseEvent.BUTTON1_MASK, 400, y, 1, false));
			try {
				Thread.sleep(500);
			}
			catch(Throwable t)
			{
				t.printStackTrace();;
			}
		}
	}

	public static void main(String[] args) {
		// The simple example application is created as anonymous class and points
		// to Google as the very first loaded page. If this example is used on
		// Linux, it's important to use OSR mode because windowed rendering is not
		// supported yet. On Macintosh and Windows windowed rendering is used as
		// default. If you want to test OSR mode on those platforms, simply replace
		// "OS.isLinux()" with "true" and recompile.
		new MainFrameAWT("http://news.ycombinator.com", true, false);
	}
}
