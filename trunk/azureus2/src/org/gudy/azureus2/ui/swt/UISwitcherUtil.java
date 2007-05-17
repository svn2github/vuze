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
	private static boolean NOT_GOOD_ENOUGH_FOR_AZ2_USERS_YET = true;

	public static String openSwitcherWindow(boolean bForceAsk) {
		if (!bForceAsk) {
			String forceUI = System.getProperty("force.ui");
			if (forceUI != null) {
				return forceUI;
			}

			forceUI = System.getProperty("ui.temp");
			if (forceUI != null) {
				COConfigurationManager.setParameter("ui", forceUI);
				return forceUI;
			}

			boolean asked = COConfigurationManager.getBooleanParameter("ui.asked",
					false);

			// NOT_GOOD_ENOUGH_FOR_AZ2_USERS_YET Notes:
			//
			// AZ2 users will always get az2 because they startup with "org.gudy.."
			// and that sets "ui.temp" to az2.  Likewise, people starting with 
			// "org.gudy.azureus2.ui.swt.mainwindow.Initializer" will have "ui.temp"
			// set to "az3".
			// The third, "com.aelitis.azureus.ui.Main" starting point does not
			// set "ui.temp", so for now it will default to "az3" if azureus was never
			// run before with the other 2 starting points.
			if (NOT_GOOD_ENOUGH_FOR_AZ2_USERS_YET || asked) {
				return COConfigurationManager.getStringParameter("ui", "az3");
			}

			// Never auto-ask people who never have had 2.x, because they'd be scared
			// and cry at the advanced coolness of the az2 ui
			String sFirstVersion = COConfigurationManager.getStringParameter("First Recorded Version");
			if (Constants.compareVersions(sFirstVersion, "3.0.0.0") >= 0) {
				return "az3";
			}
		}

		// either !asked or forceAsked at this point

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
			
			COConfigurationManager.setParameter("ui.asked", true);
		} catch (Exception e) {
			Debug.printStackTrace(e);
		}

		return COConfigurationManager.getStringParameter("ui");
	}
}
