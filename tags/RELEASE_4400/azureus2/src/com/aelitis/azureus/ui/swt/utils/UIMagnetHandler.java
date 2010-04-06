/**
 * Created on May 27, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.utils;

import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.UISwitcherUtil;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.plugins.magnet.MagnetPlugin;
import com.aelitis.azureus.plugins.magnet.MagnetPluginListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.net.magneturi.MagnetURIHandler;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author TuxPaper
 * @created May 27, 2008
 *
 */
public class UIMagnetHandler
{

	/**
	 * @param azureus_core
	 */
	public UIMagnetHandler(AzureusCore core) {
		int val = Integer.parseInt(Constants.getBaseVersion().replaceAll("\\.", ""));

		String ui = COConfigurationManager.getStringParameter("ui");
		if (!"az2".equals(ui)) {
			val += 10000;
		}

		MagnetURIHandler magnetURIHandler = MagnetURIHandler.getSingleton();
		magnetURIHandler.addInfo("get-version-info", val);

		core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			public void componentCreated(final AzureusCore core,
					AzureusCoreComponent component) {
				if (component instanceof PluginInterface) {
					PluginInterface pi = (PluginInterface) component;
					if (pi.getPlugin() instanceof MagnetPlugin) {

						MagnetPlugin magnetPlugin = (MagnetPlugin) pi.getPlugin();
						magnetPlugin.addListener(new MagnetPluginListener() {
							public boolean set(String name, Map values) {
								if (name.equals("AZMSG") && values != null) {
									String val = (String) values.get("value");
									if (val.indexOf(";switch-ui;") > 0) {

										if (COConfigurationManager.getStringParameter("ui", "az3").equals(
												"az3")) {
											return false;
										}

										UIFunctions uif = UIFunctionsManager.getUIFunctions();
										if (uif == null) {
											core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
												public void componentCreated(AzureusCore core,
														AzureusCoreComponent component) {
													if (component instanceof UIFunctions) {
														uiswitch(core, (UIFunctions) component);
													}
												}
											});
										} else {
											uiswitch(core, uif);
										}

										return true;
									}
								}
								return false;
							}

							public int get(String name, Map values) {
								return Integer.MIN_VALUE;
							}
						});
					}
				}
			}
		});
	}

	private static void uiswitch(final AzureusCore core, final UIFunctions uif) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				uif.bringToFront();
				uif.promptUser(MessageText.getString("dialog.uiswitch.title"),
						MessageText.getString("dialog.uiswitch.text"), new String[] {
							MessageText.getString("dialog.uiswitch.button"),
						}, 0, null, null, false, 0, new UserPrompterResultListener() {
							public void prompterClosed(int returnVal) {
								if (returnVal != 0) {
									return;
								}
								COConfigurationManager.setParameter("ui", "az3");
								COConfigurationManager.save();
								core.requestRestart();
							}
						});
			}
		});
	}
}
