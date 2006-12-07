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
import org.gudy.azureus2.core3.util.FileUtil;
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
		// However, we'd like to change the config defaults for AZ3 users while
		// keeping az2 users at their present defaults.  To do this, we check
		// default save path.  If it's userPath + "data", there's a very good
		// chance the user started on az3.

		String sFirstVersion = config.getStringParameter("First Recorded Version",
				"");
		
		if (sFirstVersion == null || sFirstVersion.length() == 0) {
			if (config.isNewInstall()) {
				sFirstVersion = Constants.AZUREUS_VERSION;
			} else {
  			String userPath = SystemProperties.getUserPath();
  			File f = new File(userPath, "data");
  			String sDefSavePath = config.getStringParameter("Default save path");
  			if (sDefSavePath != null && f.equals(new File(sDefSavePath))) {
  				sFirstVersion = "3.0.0.0";
  			} else {
  				sFirstVersion = "2.5.0.0"; // guess
  			}
			}
			config.setParameter("First Recorded Version", sFirstVersion);
			config.save();
		}

		if (Constants.compareVersions(sFirstVersion, "3.0.0.0") >= 0) {
			ConfigurationDefaults defaults = ConfigurationDefaults.getInstance();

			defaults.addParameter("Auto Upload Speed Enabled", true);
			defaults.addParameter("Wizard Completed", true);
			defaults.addParameter("Use default data dir", true);
			defaults.addParameter("Add URL Silently", true);
			defaults.addParameter("add_torrents_silently", true);
			defaults.addParameter("Popup Download Finished", true);
			defaults.addParameter("Popup Download Added", true);

			defaults.addParameter("Status Area Show SR", false);
			defaults.addParameter("Status Area Show NAT", false);
			defaults.addParameter("Status Area Show IPF", false);

			defaults.addParameter("window.maximized", true);

			String userPath = SystemProperties.getUserPath();
			File f = new File(userPath, "data");
			if (FileUtil.mkdirs(f)) {
				config.setParameter("Default save path", f.getAbsolutePath());
			}
			config.save();
		}
	}
}
