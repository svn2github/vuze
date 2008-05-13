/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.extlistener;

import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.browser.listener.ConfigListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.shells.main.MainWindow;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTab;
import com.aelitis.azureus.ui.swt.skin.SWTSkinTabSet;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.util.ExternalStimulusHandler;
import com.aelitis.azureus.util.ExternalStimulusListener;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Feb 7, 2008
 *
 */
public class StimulusRPC
{
	/**
	 * Hooks some listeners
	 * @param mainWindow
	 */
	public static void hookListeners(final AzureusCore core,
			final MainWindow mainWindow) {
		/*
		 * This code block was moved here from being in-line in MainWindow
		 */
		ExternalStimulusHandler.addListener(new ExternalStimulusListener() {
			public boolean receive(String name, Map values) {
				try {
					if (values == null) {
						return false;
					}

					if (!name.equals("AZMSG")) {
						return false;
					}

					Object valueObj = values.get("value");
					if (!(valueObj instanceof String)) {
						return false;
					}

					String value = (String) valueObj;

					ClientMessageContext context = PlatformMessenger.getClientMessageContext();
					if (context == null) {
						return false;
					}
					BrowserMessage browserMsg = new BrowserMessage(value);
					context.debug("Received External message: " + browserMsg);
					String opId = browserMsg.getOperationId();
					String lId = browserMsg.getListenerId();
					if (opId.equals(DisplayListener.OP_OPEN_URL)) {
						Map decodedMap = browserMsg.getDecodedMap();
						String url = MapUtils.getMapString(decodedMap, "url", null);
						if (decodedMap.containsKey("target")
								&& !PlatformConfigMessenger.isURLBlocked(url)) {

							// implicit bring to front
							final UIFunctions functions = UIFunctionsManager.getUIFunctions();
							if (functions != null) {
								functions.bringToFront();
							}

							// this is actually sync.. so we could add a completion listener
							// and return the boolean result if we wanted/needed
							context.getMessageDispatcher().dispatch(browserMsg);
							context.getMessageDispatcher().resetSequence();
							return true;
						}
						context.debug("no target or open url");

					} else if (opId.equals(TorrentListener.OP_LOAD_TORRENT)) {
						Map decodedMap = browserMsg.getDecodedMap();
						if (decodedMap.containsKey("b64")) {
							String b64 = MapUtils.getMapString(decodedMap, "b64", null);
							return TorrentListener.loadTorrentByB64(core, b64);
						} else if (decodedMap.containsKey("url")) {
							String url = MapUtils.getMapString(decodedMap, "url", null);
							boolean playNow = MapUtils.getMapBoolean(decodedMap, "play-now",
									false);
							boolean playPrepare = MapUtils.getMapBoolean(decodedMap,
									"play-prepare", false);
							boolean bringToFront = MapUtils.getMapBoolean(decodedMap,
									"bring-to-front", true);

							TorrentUIUtilsV3.loadTorrent(core, url, MapUtils.getMapString(
									decodedMap, "referer", null), playNow, playPrepare,
									bringToFront);

							return true;

						} else {
							return false;
						}
					} else if (opId.equals("is-ready")) {
						// The platform needs to know when it can call open-url, and it
						// determines this by the is-ready function
						return mainWindow.isReady();
					} else if (opId.equals("is-version-ge")) {
						Map decodedMap = browserMsg.getDecodedMap();
						if (decodedMap.containsKey("version")) {
							String id = MapUtils.getMapString(decodedMap, "id", "client");
							String version = MapUtils.getMapString(decodedMap, "version", "");
							if (id.equals("client")) {
								return org.gudy.azureus2.core3.util.Constants.isCVSVersion()
										|| org.gudy.azureus2.core3.util.Constants.compareVersions(
												org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION,
												version) >= 0;
							}
							return false;

						}
					} else if (opId.equals("is-active-tab")) {
						Map decodedMap = browserMsg.getDecodedMap();
						if (decodedMap.containsKey("tab")) {
							String tabID = MapUtils.getMapString(decodedMap, "tab", "");
							if (tabID.length() > 0) {
								SWTSkinTabSet tabSet = mainWindow.getSkin().getTabSet(
										SkinConstants.TABSET_MAIN);
								if (tabSet != null) {
									SWTSkinObjectTab activeTab = tabSet.getActiveTab();
									if (activeTab != null) {
										return activeTab.getViewID().equals("tab-" + tabID);
									}
								}
							}
						}
					} else if (ConfigListener.DEFAULT_LISTENER_ID.equals(lId)) {
						if (ConfigListener.OP_NEW_INSTALL.equals(opId)) {
							return COConfigurationManager.isNewInstall();
						} else if (ConfigListener.OP_CHECK_FOR_UPDATES.equals(opId)) {
							ConfigListener.checkForUpdates();
						}
					}else{
						
						if ( System.getProperty( "browser.route.all.external.stimuli.for.testing", "false" ).equalsIgnoreCase( "true" )){
							
							context.getMessageDispatcher().dispatch(browserMsg);
						}else{
							
							System.err.println( "Unhandled external stimulus: " + browserMsg );
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}
				return false;
			}

			public int query(String name, Map values) {
				return (Integer.MIN_VALUE );
			}
		});
	}
}
