// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefDialogHandler.FileDialogMode;
import org.cef.handler.CefRenderHandler;
import org.cef.network.CefRequest;

import java.awt.*;
import java.util.Vector;

/**
 * Interface representing a browser.
 */
public interface CefBrowser {

	/**
	 * Get the underlying UI component (e.g. java.awt.Canvas).
	 *
	 * @return The underlying UI component or null.
	 */
	Component getUIComponent();

	/**
	 * Get an implementation of CefRenderHandler if any.
	 *
	 * @return An instance of CefRenderHandler or null.
	 */
	CefRenderHandler getRenderHandler();

	//
	// The following methods are forwarded to CefBrowser.
	//

	/**
	 * Tests if the browser can navigate backwards.
	 *
	 * @return true if the browser can navigate backwards.
	 */
	boolean canGoBack();

	/**
	 * Go back.
	 */
	void goBack();

	/**
	 * Tests if the browser can navigate forwards.
	 *
	 * @return true if the browser can navigate forwards.
	 */
	boolean canGoForward();

	/**
	 * Go forward.
	 */
	void goForward();

	/**
	 * Tests if the browser is currently loading.
	 *
	 * @return true if the browser is currently loading.
	 */
	boolean isLoading();

	/**
	 * Reload the current page.
	 */
	void reload();

	/**
	 * Reload the current page ignoring any cached data.
	 */
	void reloadIgnoreCache();

	/**
	 * Stop loading the page.
	 */
	void stopLoad();

	/**
	 * Returns the unique browser identifier.
	 *
	 * @return The browser identifier
	 */
	int getIdentifier();

	/**
	 * Tests if the window is a popup window.
	 *
	 * @return true if the window is a popup window.
	 */
	boolean isPopup();

	/**
	 * Tests if a document has been loaded in the browser.
	 *
	 * @return true if a document has been loaded in the browser.
	 */
	boolean hasDocument();

	//
	// The following methods are forwarded to the mainFrame.
	//

	/**
	 * Save this frame's HTML source to a temporary file and open it in the default text viewing application. This method can only be called from
	 * the browser process.
	 */
	void viewSource();

	/**
	 * Retrieve this frame's HTML source as a string sent to the specified visitor.
	 *cefsdfdsfsdfsdf
	 * @param visitor
	 */
	void getSource(CefStringVisitor visitor);

	/**
	 * Retrieve this frame's display text as a string sent to the specified visitor.
	 *
	 * @param visitor
	 */
	void getText(CefStringVisitor visitor);

	/**
	 * Load the request represented by the request object.
	 *
	 * @param request The request object.
	 */
	void loadRequest(CefRequest request);

	/**
	 * Load the specified URL in the main frame.
	 *
	 * @param url The URL to load.
	 */
	void loadURL(String url);

	/**
	 * Load the contents of val with the specified dummy url. url should have a standard scheme (for example, http scheme) or behaviors like link
	 * clicks and web security restrictions may not behave as expected.
	 *
	 * @param val Content to be displayed.
	 * @param url dummy url to be used for.
	 */
	void loadString(String val, String url);

	/**
	 * Execute a string of JavaScript code in this frame. The url parameter is the URL where the script in question can be found, if any. The
	 * renderer may request this URL to show the developer the source of the error. The line parameter is the base line number to use for error
	 * reporting.
	 *
	 * @param code The code to be executed.
	 * @param url  The URL where the script in question can be found.
	 * @param line The base line number to use for error reporting.
	 */
	void executeJavaScript(String code, String url, int line);

	/**
	 * Emits the URL currently loaded in this frame.
	 *
	 * @return the URL currently loaded in this frame.
	 */
	String getURL();

	// The following methods are forwarded to CefBrowserHost.

	/**
	 * Close the browser.
	 */
	void close();

	/**
	 * Set or remove keyboard focus to/from the browser window.
	 *
	 * @param enable set to true to give the focus to the browser
	 */
	void setFocus(boolean enable);

	/**
	 * Get the current zoom level. The default zoom level is 0.0.
	 *
	 * @return The current zoom level.
	 */
	double getZoomLevel();

	/**
	 * Change the zoom level to the specified value. Specify 0.0 to reset the zoom level.
	 *
	 * @param zoomLevel The zoom level to be set.
	 */
	void setZoomLevel(double zoomLevel);

	/**
	 * Call to run a file chooser dialog. Only a single file chooser dialog may be pending at any given time.The dialog will be initiated
	 * asynchronously on the UI thread.
	 *
	 * @param mode            represents the type of dialog to display.
	 * @param title           to be used for the dialog and may be empty to show the default title ("Open" or "Save" depending on the mode).
	 * @param defaultFileName the default file name to select in the dialog.
	 * @param acceptTypes     is a list of valid lower-cased MIME types or file extensions specified in an input element and is used to restrict
	 *                        selectable files to such types.
	 * @param callback        will be executed after the dialog is dismissed or immediately if another dialog is already pending.
	 */
	void runFileDialog(FileDialogMode mode, String title, String defaultFileName, Vector<String> acceptTypes, CefRunFileDialogCallback callback);

	/**
	 * Download the file at url using CefDownloadHandler.
	 *
	 * @param url URL to download that file.
	 */
	void startDownload(String url);

	/**
	 * Print the current browser contents.
	 */
	void print();

	/**
	 * Search for some kind of text on the page.
	 *
	 * @param identifier can be used to have multiple searches running simultaniously.
	 * @param searchText to be searched for.
	 * @param forward    indicates whether to search forward or backward within the page.
	 * @param matchCase  indicates whether the search should be case-sensitive.
	 * @param findNext   indicates whether this is the first request or a follow-up.
	 */
	void find(int identifier, String searchText, boolean forward, boolean matchCase, boolean findNext);

	/**
	 * Cancel all searches that are currently going on.
	 *
	 * @param clearSelection Set to true to reset selection.
	 */
	void stopFinding(boolean clearSelection);

	/**
	 * Get an instance of the dev tools to be displayed in its own window or to be embedded within your UI. Only one instance per browser is
	 * available.
	 */
	CefBrowser getDevTools();
}
