// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefWebPluginInfoVisitor;
import org.cef.callback.CefWebPluginUnstableCallback;

/**
 * Class used to manage web plugins.
 */
public abstract class CefWebPluginManager {
    // This CTOR can't be called directly. Call method getGlobalManager() instead.
    CefWebPluginManager() {}

    /**
     * Returns the global plugin manager.
     */
    public static final CefWebPluginManager getGlobalManager() {
        return CefWebPluginManager_N.getInstance();
    }

    /**
     * Visit web plugin information. Can be called on any thread in the browser process.
     *
     * @param visitor Called with plugin information when available.
     */
    public abstract void visitPlugins(CefWebPluginInfoVisitor visitor);

    /**
     * Cause the plugin list to refresh the next time it is accessed regardless of whether it has
     * already been loaded. Can be called on any thread in the browser process.
     */
    public abstract void refreshPlugins();

    /**
     * Unregister an internal plugin. This may be undone the next time refreshPlugins() is called.
     * Can be called on any thread in the browser process.
     *
     * @param path Plugin file path (DLL/bundle/library).
     */
    public abstract void unregisterInternalPlugin(String path);

    /**
     * Register a plugin crash. Can be called on any thread in the browser process but will be
     * executed on the IO thread.
     *
     * @param path Plugin file path (DLL/bundle/library).
     */
    public abstract void registerPluginCrash(String path);

    /**
     * Query if a plugin is unstable. Can be called on any thread in the browser process.
     *
     * @param path Plugin file path (DLL/bundle/library).
     * @param callback Called when plugin information is available.
     */
    public abstract void isWebPluginUnstable(String path, CefWebPluginUnstableCallback callback);
}
