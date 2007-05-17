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

package org.gudy.azureus2.ui.swt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

/**
 * @author TuxPaper
 * @created Mar 21, 2007
 *
 */
public class UISwitcherUtil
{
	public static String openSwitcherWindow(boolean bForceAsk) {
		String forceUI = System.getProperty("force.ui");
		if (forceUI != null) {
			return forceUI;
		}

		// This is temporary until we have the UI Switcher in place
		//
		// ui.temp system property is set in one of the two main (startup) classes
		// Anyone running using "org.gudy.." get it set to "az2", and anyone running
		// "com.aelitis.." get it set to "az3".  On first run with this code,
		// we set the "ui.temp" azureus config parameter to this value.  Every
		// run after that, we use the config parameter, guaranteeing that the
		// they always get the same UI as the first time.
		String tempForceUI = COConfigurationManager.getStringParameter("ui.temp",
				null);
		if (tempForceUI != null) {
			return tempForceUI;
		}

		tempForceUI = System.getProperty("ui.temp");
		if (tempForceUI != null) {
			COConfigurationManager.setParameter("ui.temp", tempForceUI);
			return tempForceUI;
		}

		String sFirstVersion = COConfigurationManager.getStringParameter("First Recorded Version");
		if (!bForceAsk && Constants.compareVersions(sFirstVersion, "3.0.0.0") >= 0) {
			return "az3";
		} else if (bForceAsk || !COConfigurationManager.hasParameter("ui", true)) {
			try {

				final int[] result = {
					-1
				};

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						try {
							final Class uiswClass = Class.forName("com.aelitis.azureus.ui.swt.shells.uiswitcher.UISwitcherWindow");

							final Constructor constructor = uiswClass.getConstructor(new Class[] {});

							Object object = constructor.newInstance(new Object[] {});

							Method method = uiswClass.getMethod("open", new Class[] {});

							Object resultObj = method.invoke(object, new Object[] {});

							if (resultObj instanceof Number) {
								result[0] = ((Number) resultObj).intValue();
							}
						} catch (Exception e) {
							Debug.printStackTrace(e);
						}
					}
				}, false);

				if (result[0] == 0) {
					// Full AZ3UI
					COConfigurationManager.setParameter("ui", "az3");
					COConfigurationManager.setParameter("v3.Start Advanced", false);
				} else if (result[0] == 1) {
					// AZ3UI w/Advanced view default
					COConfigurationManager.setParameter("ui", "az3");
					COConfigurationManager.setParameter("v3.Start Advanced", true);
				} else if (result[0] == 2) {
					COConfigurationManager.setParameter("ui", "az2");
				}
			} catch (Exception e) {
				Debug.printStackTrace(e);
			}
		}

		return COConfigurationManager.getStringParameter("ui");
	}
}
