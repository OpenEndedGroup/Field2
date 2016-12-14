// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefGeolocationCallback;

/**
 * Implement this interface to handle events related to geolocation permission requests. The methods of this class will be called on the browser
 * process IO thread.
 */
public interface CefGeolocationHandler {
	/**
	 * Called when a page requests permission to access geolocation information.
	 *
	 * @param browser        The corresponding browser.
	 * @param requesting_url is the URL requesting permission.
	 * @param request_id     is the unique ID for the permission request.
	 * @param callback       Call CefGeolocationCallback.Continue to allow or deny the permission request.
	 */
	void onRequestGeolocationPermission(CefBrowser browser, String requesting_url, int request_id, CefGeolocationCallback callback);

	/**
	 * Called when a geolocation access request is canceled.
	 *
	 * @param browser        The corresponding browser.
	 * @param requesting_url is the URL that originally requested permission.
	 * @param request_id     is the unique ID for the permission request.
	 */
	void onCancelGeolocationPermission(CefBrowser browser, String requesting_url, int request_id);
}
