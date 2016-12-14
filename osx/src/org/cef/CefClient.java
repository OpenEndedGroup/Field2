// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef;

import org.cef.browser.*;
import org.cef.callback.*;
import org.cef.handler.*;
import org.cef.misc.BoolRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefWebPluginInfo;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

/**
 * Client that owns a browser and renderer.
 */
public class CefClient extends CefClientHandler implements CefContextMenuHandler, CefDialogHandler, CefDisplayHandler, CefDownloadHandler, CefDragHandler, CefFocusHandler, CefGeolocationHandler, CefJSDialogHandler, CefKeyboardHandler, CefLifeSpanHandler, CefLoadHandler, CefRenderHandler, CefRequestHandler {
	private HashMap<Integer, CefBrowser> browser_ = new HashMap<Integer, CefBrowser>();
	private CefContextMenuHandler contextMenuHandler_ = null;
	private CefDialogHandler dialogHandler_ = null;
	private CefDisplayHandler displayHandler_ = null;
	private CefDownloadHandler downloadHandler_ = null;
	private CefDragHandler dragHandler_ = null;
	private CefFocusHandler focusHandler_ = null;
	private CefGeolocationHandler geolocationHandler_ = null;
	private CefJSDialogHandler jsDialogHandler_ = null;
	private CefKeyboardHandler keyboardHandler_ = null;
	private CefLifeSpanHandler lifeSpanHandler_ = null;
	private CefLoadHandler loadHandler_ = null;
	private CefRequestHandler requestHandler_ = null;

	/**
	 * The CTOR is only accessible within this package. Use CefApp.createClient() to create an instance of this class.
	 *
	 * @see org.cef.CefApp.createClient()
	 */
	CefClient() throws UnsatisfiedLinkError {
		super();
	}

	@Override
	public void dispose() {
		destroyAllBrowser();
		removeContextMenuHandler(this);
		removeDialogHandler(this);
		removeDisplayHandler(this);
		removeDownloadHandler(this);
		removeDragHandler(this);
		removeFocusHandler(this);
		removeGeolocationHandler(this);
		removeJSDialogHandler(this);
		removeKeyboardHandler(this);
		removeLifeSpanHandler(this);
		removeLoadHandler(this);
		removeRenderHandler(this);
		removeRequestHandler(this);
		super.dispose();
	}


	// CefClientHandler

	public CefBrowser createBrowser(String url, boolean isTransparent, CefBrowserFactory.RenderType renderType) {
		return createBrowser(url, isTransparent, renderType, null, 0, 0, null);
	}

	public CefBrowser createBrowser(String url, boolean isTransparent, CefBrowserFactory.RenderType renderType, CefRequestContext context) {
		return createBrowser(url, isTransparent, renderType, context, 0, 0, null);
	}

	public CefBrowser createBrowser(String url, boolean isTransparent, CefBrowserFactory.RenderType renderType, CefRequestContext context, int browserWidth, int browserHeight, CefRenderer cefRenderer) {
		return CefBrowserFactory.create(this, url, isTransparent, context, renderType, browserWidth, browserHeight, cefRenderer);
	}

	public void destroyBrowser(CefBrowser browser) {
		Integer browserId = new Integer(browser.getIdentifier());
		if (browser_.remove(browserId) != null) {
			browser.close();
		}
	}

	public void destroyAllBrowser() {
		Collection<CefBrowser> browserList = browser_.values();
		for (CefBrowser browser : browserList) {
			browser.close();
		}
		browser_.clear();
	}

	@Override
	protected CefBrowser getBrowser(int identifier) {
		return browser_.get(new Integer(identifier));
	}

	@Override
	protected Object[] getAllBrowser() {
		return browser_.values().toArray();
	}

	@Override
	protected CefContextMenuHandler getContextMenuHandler() {
		return this;
	}

	@Override
	protected CefDialogHandler getDialogHandler() {
		return this;
	}

	@Override
	protected CefDisplayHandler getDisplayHandler() {
		return this;
	}

	@Override
	protected CefDownloadHandler getDownloadHandler() {
		return this;
	}

	@Override
	protected CefDragHandler getDragHandler() {
		return this;
	}

	@Override
	protected CefFocusHandler getFocusHandler() {
		return this;
	}

	@Override
	protected CefGeolocationHandler getGeolocationHandler() {
		return this;
	}

	@Override
	protected CefJSDialogHandler getJSDialogHandler() {
		return this;
	}

	@Override
	protected CefKeyboardHandler getKeyboardHandler() {
		return this;
	}

	@Override
	protected CefLifeSpanHandler getLifeSpanHandler() {
		return this;
	}

	@Override
	protected CefLoadHandler getLoadHandler() {
		return this;
	}

	@Override
	protected CefRenderHandler getRenderHandler() {
		return this;
	}

	@Override
	protected CefRequestHandler getRequestHandler() {
		return this;
	}


	// CefContextMenuHandler

	public CefClient addContextMenuHandler(CefContextMenuHandler handler) {
		if (contextMenuHandler_ == null) contextMenuHandler_ = handler;
		return this;
	}

	public void removeContextMenuHandler() {
		contextMenuHandler_ = null;
	}

	@Override
	public void onBeforeContextMenu(CefBrowser browser, CefContextMenuParams params, CefMenuModel model) {
		if (contextMenuHandler_ != null && browser != null) contextMenuHandler_.onBeforeContextMenu(browser, params, model);
	}

	@Override
	public boolean onContextMenuCommand(CefBrowser browser, CefContextMenuParams params, int commandId, int eventFlags) {
		if (contextMenuHandler_ != null && browser != null)
			return contextMenuHandler_.onContextMenuCommand(browser, params, commandId, eventFlags);
		return false;
	}

	@Override
	public void onContextMenuDismissed(CefBrowser browser) {
		if (contextMenuHandler_ != null && browser != null) contextMenuHandler_.onContextMenuDismissed(browser);
	}


	// CefDialogHandler

	public CefClient addDialogHandler(CefDialogHandler handler) {
		if (dialogHandler_ == null) dialogHandler_ = handler;
		return this;
	}

	public void removeDialogHandler() {
		dialogHandler_ = null;
	}

	@Override
	public boolean onFileDialog(CefBrowser browser, FileDialogMode mode, String title, String defaultFileName, Vector<String> acceptTypes, CefFileDialogCallback callback) {
		if (dialogHandler_ != null && browser != null)
			return dialogHandler_.onFileDialog(browser, mode, title, defaultFileName, acceptTypes, callback);
		return false;
	}


	// CefDisplayHandler

	public CefClient addDisplayHandler(CefDisplayHandler handler) {
		if (displayHandler_ == null) displayHandler_ = handler;
		return this;
	}

	public void removeDisplayHandler() {
		displayHandler_ = null;
	}

	@Override
	public void onAddressChange(CefBrowser browser, String url) {
		if (displayHandler_ != null && browser != null) displayHandler_.onAddressChange(browser, url);
	}

	@Override
	public void onTitleChange(CefBrowser browser, String title) {
		if (displayHandler_ != null && browser != null) displayHandler_.onTitleChange(browser, title);
	}

	@Override
	public boolean onTooltip(CefBrowser browser, String text) {
		if (displayHandler_ != null && browser != null) {
			return displayHandler_.onTooltip(browser, text);
		}
		return false;
	}

	@Override
	public void onStatusMessage(CefBrowser browser, String value) {
		if (displayHandler_ != null && browser != null) {
			displayHandler_.onStatusMessage(browser, value);
		}
	}

	@Override
	public boolean onConsoleMessage(CefBrowser browser, String message, String source, int line) {
		if (displayHandler_ != null && browser != null) {
			return displayHandler_.onConsoleMessage(browser, message, source, line);
		}
		return false;
	}


	// CefDownloadHandler

	public CefClient addDownloadHandler(CefDownloadHandler handler) {
		if (downloadHandler_ == null) downloadHandler_ = handler;
		return this;
	}

	public void removeDownloadHandler() {
		downloadHandler_ = null;
	}

	@Override
	public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName, CefBeforeDownloadCallback callback) {
		if (downloadHandler_ != null && browser != null) downloadHandler_.onBeforeDownload(browser, downloadItem, suggestedName, callback);
	}

	@Override
	public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
		if (downloadHandler_ != null && browser != null) downloadHandler_.onDownloadUpdated(browser, downloadItem, callback);
	}


	// CefDragHandler

	public CefClient addDragHandler(CefDragHandler handler) {
		if (dragHandler_ == null) dragHandler_ = handler;
		return this;
	}

	public void removeDragHandler() {
		dragHandler_ = null;
	}

	@Override
	public boolean onDragEnter(CefBrowser browser, CefDragData dragData, int mask) {
		if (dragHandler_ != null && browser != null) return dragHandler_.onDragEnter(browser, dragData, mask);
		return false;
	}


	// CefFocusHandler

	public CefClient addFocusHandler(CefFocusHandler handler) {
		if (focusHandler_ == null) focusHandler_ = handler;
		return this;
	}

	public void removeFocusHandler() {
		focusHandler_ = null;
	}

	@Override
	public void onTakeFocus(CefBrowser browser, boolean next) {
		if (browser == null) return;

		browser.setFocus(false);

		if (browser.getUIComponent() != null) {
			Container parent = browser.getUIComponent().getParent();
			if (parent != null) {
				FocusTraversalPolicy policy = null;
				while (parent != null) {
					policy = parent.getFocusTraversalPolicy();
					if (policy != null) break;
					parent = parent.getParent();
				}
				if (policy != null) {
					Component nextComp = next ? policy.getComponentAfter(parent, browser.getUIComponent()) : policy
						    .getComponentBefore(parent, browser.getUIComponent());
					if (nextComp == null) {
						policy.getDefaultComponent(parent).requestFocus();
					} else {
						nextComp.requestFocus();
					}
				}
			}
		}

		if (focusHandler_ != null) focusHandler_.onTakeFocus(browser, next);
	}

	@Override
	public boolean onSetFocus(CefBrowser browser, FocusSource source) {
		if (browser == null) return false;

		boolean alreadyHandled = false;
		if (focusHandler_ != null) alreadyHandled = focusHandler_.onSetFocus(browser, source);
		if ((!alreadyHandled) && (browser.getUIComponent() != null)) browser.getUIComponent().requestFocus();
		return alreadyHandled;
	}

	@Override
	public void onGotFocus(CefBrowser browser) {
		if (browser == null) return;

		browser.setFocus(true);
		if (focusHandler_ != null) focusHandler_.onGotFocus(browser);
	}


	// CefGeolocationHandler

	public CefClient addGeolocationHandler(CefGeolocationHandler handler) {
		if (geolocationHandler_ == null) geolocationHandler_ = handler;
		return this;
	}

	public void removeGeolocationHandler() {
		geolocationHandler_ = null;
	}

	@Override
	public void onRequestGeolocationPermission(CefBrowser browser, String requesting_url, int request_id, CefGeolocationCallback callback) {
		if (geolocationHandler_ != null && browser != null)
			geolocationHandler_.onRequestGeolocationPermission(browser, requesting_url, request_id, callback);
	}

	@Override
	public void onCancelGeolocationPermission(CefBrowser browser, String requesting_url, int request_id) {
		if (geolocationHandler_ != null && browser != null)
			geolocationHandler_.onCancelGeolocationPermission(browser, requesting_url, request_id);
	}


	// CefJSDialogHandler

	public CefClient addJSDialogHandler(CefJSDialogHandler handler) {
		if (jsDialogHandler_ == null) jsDialogHandler_ = handler;
		return this;
	}

	public void removeJSDialogHandler() {
		jsDialogHandler_ = null;
	}

	@Override
	public boolean onJSDialog(CefBrowser browser, String origin_url, String accept_lang, JSDialogType dialog_type, String message_text, String default_prompt_text, CefJSDialogCallback callback, BoolRef suppress_message) {
		if (jsDialogHandler_ != null && browser != null) return jsDialogHandler_
			    .onJSDialog(browser, origin_url, accept_lang, dialog_type, message_text, default_prompt_text, callback, suppress_message);
		return false;
	}

	@Override
	public boolean onBeforeUnloadDialog(CefBrowser browser, String message_text, boolean is_reload, CefJSDialogCallback callback) {
		if (jsDialogHandler_ != null && browser != null)
			return jsDialogHandler_.onBeforeUnloadDialog(browser, message_text, is_reload, callback);
		return false;
	}

	@Override
	public void onResetDialogState(CefBrowser browser) {
		if (jsDialogHandler_ != null && browser != null) jsDialogHandler_.onResetDialogState(browser);
	}

	@Override
	public void onDialogClosed(CefBrowser browser) {
		if (jsDialogHandler_ != null && browser != null) jsDialogHandler_.onDialogClosed(browser);
	}


	// CefKeyboardHandler

	public CefClient addKeyboardHandler(CefKeyboardHandler handler) {
		if (keyboardHandler_ == null) keyboardHandler_ = handler;
		return this;
	}

	public void removeKeyboardHandler() {
		keyboardHandler_ = null;
	}

	@Override
	public boolean onPreKeyEvent(CefBrowser browser, CefKeyEvent event, BoolRef is_keyboard_shortcut) {
		if (keyboardHandler_ != null && browser != null) return keyboardHandler_.onPreKeyEvent(browser, event, is_keyboard_shortcut);
		return false;
	}

	@Override
	public boolean onKeyEvent(CefBrowser browser, CefKeyEvent event) {
		if (keyboardHandler_ != null && browser != null) return keyboardHandler_.onKeyEvent(browser, event);
		return false;
	}


	// CefLifeSpanHandler

	public CefClient addLifeSpanHandler(CefLifeSpanHandler handler) {
		if (lifeSpanHandler_ == null) lifeSpanHandler_ = handler;
		return this;
	}

	public void removeLifeSpanHandler() {
		lifeSpanHandler_ = null;
	}

	@Override
	public boolean onBeforePopup(CefBrowser browser, String target_url, String target_frame_name) {
		if (lifeSpanHandler_ != null && browser != null) return lifeSpanHandler_.onBeforePopup(browser, target_url, target_frame_name);
		return false;
	}

	@Override
	public void onAfterCreated(CefBrowser browser) {
		if (browser == null) return;

		// keep browser reference
		Integer identifier = browser.getIdentifier();
		browser_.put(identifier, browser);
		if (lifeSpanHandler_ != null) lifeSpanHandler_.onAfterCreated(browser);
	}

	@Override
	public boolean runModal(CefBrowser browser) {
		if (lifeSpanHandler_ != null && browser != null) return lifeSpanHandler_.runModal(browser);
		return false;
	}

	@Override
	public boolean doClose(CefBrowser browser) {
		if (lifeSpanHandler_ != null && browser != null) return lifeSpanHandler_.doClose(browser);
		return false;
	}

	@Override
	public void onBeforeClose(CefBrowser browser) {
		if (lifeSpanHandler_ != null && browser != null) lifeSpanHandler_.onBeforeClose(browser);
	}


	// CefLoadHandler

	public CefClient addLoadHandler(CefLoadHandler handler) {
		if (loadHandler_ == null) loadHandler_ = handler;
		return this;
	}

	public void removeLoadHandler() {
		loadHandler_ = null;
	}

	@Override
	public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
		if (loadHandler_ != null && browser != null) loadHandler_.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
	}

	@Override
	public void onLoadStart(CefBrowser browser, int frameIdentifer) {
		if (loadHandler_ != null && browser != null) loadHandler_.onLoadStart(browser, frameIdentifer);
	}

	@Override
	public void onLoadEnd(CefBrowser browser, int frameIdentifier, int httpStatusCode) {
		if (loadHandler_ != null && browser != null) loadHandler_.onLoadEnd(browser, frameIdentifier, httpStatusCode);
	}

	@Override
	public void onLoadError(CefBrowser browser, int frameIdentifer, ErrorCode errorCode, String errorText, String failedUrl) {
		if (loadHandler_ != null && browser != null) loadHandler_.onLoadError(browser, frameIdentifer, errorCode, errorText, failedUrl);
	}


	// CefMessageRouter

	@Override
	public synchronized void addMessageRouter(CefMessageRouter messageRouter) {
		super.addMessageRouter(messageRouter);
	}

	@Override
	public synchronized void removeMessageRouter(CefMessageRouter messageRouter) {
		super.removeMessageRouter(messageRouter);
	}


	// CefRenderHandler

	@Override
	public Rectangle getViewRect(CefBrowser browser) {
		if (browser == null) return new Rectangle(0, 0, 0, 0);

		CefRenderHandler realHandler = browser.getRenderHandler();
		if (realHandler != null) return realHandler.getViewRect(browser);

		if (browser.getUIComponent() == null) return new Rectangle(0, 0, 0, 0);

		Rectangle tmp = browser.getUIComponent().getBounds();
		if (OS.isMacintosh()) {
			Container parent = browser.getUIComponent().getParent();
			while (parent != null) {
				Container next = parent.getParent();
				if (next != null && next instanceof Window) break;

				Rectangle parentRect = parent.getBounds();
				tmp.x += parentRect.x;
				tmp.y += parentRect.y;
				parent = next;
			}
		}
		return tmp;
	}

	@Override
	public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
		if ((browser == null) || (browser.getUIComponent() == null)) return new Point(0, 0);

		CefRenderHandler realHandler = browser.getRenderHandler();
		if (realHandler != null) return realHandler.getScreenPoint(browser, viewPoint);
		Point screenPoint = browser.getUIComponent().getLocationOnScreen();
		screenPoint.translate(viewPoint.x, viewPoint.y);
		return screenPoint;
	}

	@Override
	public void onPopupShow(CefBrowser browser, boolean show) {
		if (browser == null) return;

		CefRenderHandler realHandler = browser.getRenderHandler();
		if (realHandler != null) realHandler.onPopupShow(browser, show);
	}

	@Override
	public void onPopupSize(CefBrowser browser, Rectangle size) {
		if (browser == null) return;

		CefRenderHandler realHandler = browser.getRenderHandler();
		if (realHandler != null) realHandler.onPopupSize(browser, size);
	}

	@Override
	public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
		if (browser == null) return;

		CefRenderHandler realHandler = browser.getRenderHandler();
		if (realHandler != null) realHandler.onPaint(browser, popup, dirtyRects, buffer, width, height);
	}

	@Override
	public void onCursorChange(CefBrowser browser, int cursorType) {
		if (browser == null) return;

		CefRenderHandler realHandler = browser.getRenderHandler();
		if (realHandler != null) realHandler.onCursorChange(browser, cursorType);
	}


	// CefRequestHandler

	public CefClient addRequestHandler(CefRequestHandler handler) {
		if (requestHandler_ == null) requestHandler_ = handler;
		return this;
	}

	public void removeRequestHandler() {
		requestHandler_ = null;
	}

	@Override
	public boolean onBeforeBrowse(CefBrowser browser, CefRequest request, boolean is_redirect) {
		if (requestHandler_ != null && browser != null) return requestHandler_.onBeforeBrowse(browser, request, is_redirect);
		return false;
	}

	@Override
	public boolean onBeforeResourceLoad(CefBrowser browser, CefRequest request) {
		if (requestHandler_ != null && browser != null) return requestHandler_.onBeforeResourceLoad(browser, request);
		return false;
	}

	@Override
	public CefResourceHandler getResourceHandler(CefBrowser browser, CefRequest request) {
		if (requestHandler_ != null && browser != null) return requestHandler_.getResourceHandler(browser, request);
		return null;
	}

	@Override
	public void onResourceRedirect(CefBrowser browser, String old_url, StringRef new_url) {
		if (requestHandler_ != null && browser != null) requestHandler_.onResourceRedirect(browser, old_url, new_url);
	}

	@Override
	public boolean getAuthCredentials(CefBrowser browser, boolean isProxy, String host, int port, String realm, String scheme, CefAuthCallback callback) {
		if (requestHandler_ != null && browser != null)
			return requestHandler_.getAuthCredentials(browser, isProxy, host, port, realm, scheme, callback);
		return false;
	}

	@Override
	public boolean onQuotaRequest(CefBrowser browser, String origin_url, long new_size, CefQuotaCallback callback) {
		if (requestHandler_ != null && browser != null) return requestHandler_.onQuotaRequest(browser, origin_url, new_size, callback);
		return false;
	}

	@Override
	public void onProtocolExecution(CefBrowser browser, String url, BoolRef allow_os_execution) {
		if (requestHandler_ != null && browser != null) requestHandler_.onProtocolExecution(browser, url, allow_os_execution);
	}

	@Override
	public boolean onCertificateError(ErrorCode cert_error, String request_url, CefAllowCertificateErrorCallback callback) {
		if (requestHandler_ != null) return requestHandler_.onCertificateError(cert_error, request_url, callback);
		return false;
	}

	@Override
	public boolean onBeforePluginLoad(CefBrowser browser, String url, String policyUrl, CefWebPluginInfo info) {
		if (requestHandler_ != null) return requestHandler_.onBeforePluginLoad(browser, url, policyUrl, info);
		return false;
	}

	@Override
	public void onPluginCrashed(CefBrowser browser, String pluginPath) {
		if (requestHandler_ != null) requestHandler_.onPluginCrashed(browser, pluginPath);
	}

	@Override
	public void onRenderProcessTerminated(CefBrowser browser, TerminationStatus status) {
		if (requestHandler_ != null) requestHandler_.onRenderProcessTerminated(browser, status);
	}
}
