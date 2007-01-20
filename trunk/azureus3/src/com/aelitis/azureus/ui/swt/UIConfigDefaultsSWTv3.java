/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.ui.swt;

import java.io.File;

import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemProperties;

/**
 * @author TuxPaper
 * @created Nov 3, 2006
 *
 */
public class UIConfigDefaultsSWTv3
{
	public static void initialize() {
		ConfigurationManager config = ConfigurationManager.getInstance();

		if (System.getProperty("FORCE_PROGRESSIVE", "").length() > 0) { //TODO HACK FOR DEMO PURPOSES ONLY!
			config.setParameter("Prioritize First Piece", true);
			config.save();
		}

		// Up to az > 3.0.0.2, we did not store the original version the user starts
		// on.
		String sFirstVersion = config.getStringParameter("First Recorded Version");

		ConfigurationDefaults defaults = ConfigurationDefaults.getInstance();
		// Always have the wizard complete when running az3
		defaults.addParameter("Wizard Completed", true);
		
		defaults.addParameter("ui", "az3");

		if (Constants.compareVersions(sFirstVersion, "3.0.0.0") >= 0) {
			
			if (!config.isNewInstall()
					&& Constants.compareVersions(sFirstVersion, "3.0.0.4") < 0) {
				// We can guess first version based on the Default save path.
				// In 3.0.0.0 to 3.0.0.3, we set it to userPath + "data". Anything
				// else is 2.x.  We don't want to change the defaults for 2.x people
  			String userPath = SystemProperties.getUserPath();
  			File f = new File(userPath, "data");
  			String sDefSavePath = config.getStringParameter("Default save path");
  			if (sDefSavePath == null || !f.equals(new File(sDefSavePath))) {
  				sFirstVersion = "2.5.0.0"; // guess
  				config.setParameter("First Recorded Version", sFirstVersion);
  				config.save();
  				return;
  			}
			}

			defaults.addParameter("Auto Upload Speed Enabled", true);
			defaults.addParameter("Use default data dir", true);
			defaults.addParameter("Add URL Silently", true);
			defaults.addParameter("add_torrents_silently", true);
			defaults.addParameter("Popup Download Finished", true);
			defaults.addParameter("Popup Download Added", true);

			defaults.addParameter("Status Area Show SR", false);
			defaults.addParameter("Status Area Show NAT", false);
			defaults.addParameter("Status Area Show IPF", false);

			defaults.addParameter("window.maximized", true);

			config.save();
		}
	}
}
