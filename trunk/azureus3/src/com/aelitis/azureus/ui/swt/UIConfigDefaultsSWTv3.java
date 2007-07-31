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
import org.gudy.azureus2.core3.config.impl.ConfigurationParameterNotFoundException;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.core.*;

/**
 * @author TuxPaper
 * @created Nov 3, 2006
 *
 */
public class UIConfigDefaultsSWTv3
{
	public static void initialize(AzureusCore core) {
		ConfigurationManager config = ConfigurationManager.getInstance();

		if (System.getProperty("FORCE_PROGRESSIVE", "").length() > 0) { //TODO HACK FOR DEMO PURPOSES ONLY!
			config.setParameter("Prioritize First Piece", true);
			config.save();
		}

		// Up to az > 3.0.0.2, we did not store the original version the user starts
		// on.
		String sFirstVersion = config.getStringParameter("First Recorded Version");

		final ConfigurationDefaults defaults = ConfigurationDefaults.getInstance();
		// Always have the wizard complete when running az3
		defaults.addParameter("Wizard Completed", true);

		defaults.addParameter("ui", "az3");

		// Another hack to fix up some 3.x versions thinking their first version
		// was 2.5.0.0..
		if (Constants.compareVersions(sFirstVersion, "2.5.0.0") == 0) {
			String sDefSavePath = config.getStringParameter("Default save path");

			System.out.println(sDefSavePath);
			String sDefPath = null;
			try {
				sDefPath = defaults.getStringParameter("Default save path");
			} catch (ConfigurationParameterNotFoundException e) {
				e.printStackTrace();
			}
			if (sDefPath != null) {
				File fNewPath = new File(sDefPath);

				if (sDefSavePath != null && fNewPath.equals(new File(sDefSavePath))) {
					sFirstVersion = "3.0.0.5";
					config.setParameter("First Recorded Version", sFirstVersion);
					config.save();
				}
			}
		}

		if (Constants.compareVersions(sFirstVersion, "3.0.0.0") >= 0) {

			if (!config.isNewInstall()
					&& Constants.compareVersions(sFirstVersion, "3.0.0.4") < 0) {
				// We can guess first version based on the Default save path.
				// In 3.0.0.0 to 3.0.0.3, we set it to userPath + "data". Anything
				// else is 2.x.  We don't want to change the defaults for 2.x people
				String userPath = SystemProperties.getUserPath();
				File fOldPath = new File(userPath, "data");
				String sDefSavePath = config.getStringParameter("Default save path");

				String sDefPath = "";
				try {
					sDefPath = defaults.getStringParameter("Default save path");
				} catch (ConfigurationParameterNotFoundException e) {
				}
				File fNewPath = new File(sDefPath);

				if (sDefSavePath != null && fNewPath.equals(new File(sDefSavePath))) {
					sFirstVersion = "3.0.0.5";
					config.setParameter("First Recorded Version", sFirstVersion);
					config.save();
				} else if (sDefSavePath == null
						|| !fOldPath.equals(new File(sDefSavePath))) {
					sFirstVersion = "2.5.0.0"; // guess
					config.setParameter("First Recorded Version", sFirstVersion);
					config.save();
					return;
				} else {
					// first version was 3.0.0.0 - 3.0.0.3, which used userPath + "data"
					// remove save path, which will default it to Azureus' Doc dir
					config.removeParameter("Default save path");
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

			defaults.addParameter("update.autodownload", true);
			
			defaults.addParameter("bFirstPriority_ignore0Peer", true);

			defaults.addParameter("v3.topbar.show.frog", false);
			
			config.save();
		}

		defaults.addParameter("v3.topbar.show.plugin", false);
		defaults.addParameter("ui.toolbar.uiswitcher", true);

		// by default, turn off some slidey warning
		// Since they are plugin configs, we need to set the default after the 
		// plugin sets the default
		core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			public void started(AzureusCore core) {
				defaults.addParameter("Plugin.DHT.dht.warn.user", false);
				defaults.addParameter("Plugin.UPnP.upnp.alertothermappings", false);
				defaults.addParameter("Plugin.UPnP.upnp.alertdeviceproblems", false);
			}
		});
	}
}
