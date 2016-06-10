/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt;

import java.io.File;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;

/**
 * @author TuxPaper
 * @created Nov 3, 2006
 *
 */
public class UIConfigDefaultsSWTv3
{
	public static void initialize(AzureusCore core) {
		ConfigurationManager config = ConfigurationManager.getInstance();
		
		if ("az2".equalsIgnoreCase(config.getStringParameter("ui", "az3"))) {
			return;
		}

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		boolean startAdvanced = userMode > 1;

		boolean configNeedsSave = false;

		if (System.getProperty("FORCE_PROGRESSIVE", "").length() > 0) { //TODO HACK FOR DEMO PURPOSES ONLY!
			config.setParameter("Prioritize First Piece", true);
			configNeedsSave = true;
		}

		// Up to az > 3.0.0.2, we did not store the original version the user starts
		// on.
		String sFirstVersion = config.getStringParameter("First Recorded Version");

		final ConfigurationDefaults defaults = ConfigurationDefaults.getInstance();

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
					configNeedsSave = true;
				}
			}
		}

		//boolean virginSwitch = config.getBooleanParameter("az3.virgin.switch", false);
		boolean immediateSwitch = config.getBooleanParameter(
				"az3.switch.immediate", false);
		if (Constants.compareVersions(sFirstVersion, "3.0.0.0") >= 0
				|| immediateSwitch) {

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
					configNeedsSave = true;
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
			defaults.addParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS,
					startAdvanced
							? ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_ALWAYS
							: ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_MANY);
			
			// defaults.addParameter("Add URL Silently", true);			not used 11/30/2015 - see "Activate Window On External Download"
			// defaults.addParameter("add_torrents_silently", true);	not used 11/30/2015
			
			defaults.addParameter("Popup Download Finished", false);
			defaults.addParameter("Popup Download Added", false);

			defaults.addParameter("Status Area Show SR", false);
			defaults.addParameter("Status Area Show NAT", false);
			defaults.addParameter("Status Area Show IPF", false);
			defaults.addParameter("Status Area Show RIP", true);
			
			defaults.addParameter("Message Popup Autoclose in Seconds", 10 );

			defaults.addParameter("window.maximized", true);

			defaults.addParameter("update.autodownload", true);
			
			//defaults.addParameter("suppress_file_download_dialog", true);
			
			defaults.addParameter("auto_remove_inactive_items", false);
			
			defaults.addParameter("show_torrents_menu", false);
		}


		defaults.addParameter("v3.topbar.show.frog", false);
		config.removeParameter("v3.home-tab.starttab");
		defaults.addParameter("MyTorrentsView.table.style", 0);
		defaults.addParameter("v3.Show Welcome", true);
		
		defaults.addParameter("Library.viewmode", startAdvanced ? 1 : 0);
		defaults.addParameter("LibraryDL.viewmode", startAdvanced ? 1 : 0);
		defaults.addParameter("LibraryUnopened.viewmode", startAdvanced ? 1 : 0);
		defaults.addParameter("LibraryCD.viewmode", startAdvanced ? 1 : 0);
		defaults.addParameter("Library.EnableSimpleView", 1 );
		defaults.addParameter("Library.CatInSideBar", startAdvanced ? 1 : 0);
		defaults.addParameter("Library.TagInSideBar", 1 );
		defaults.addParameter("Library.ShowTabsInTorrentView", 1 );
		defaults.addParameter("list.dm.dblclick", "0");

		//=== defaults used by MainWindow
		defaults.addParameter("vista.adminquit", false);
		defaults.addParameter("Start Minimized", false);
		defaults.addParameter("Password enabled", false);
		defaults.addParameter("ToolBar.showText", true);
		defaults.addParameter("burninfo.shownonce", false);
		
		defaults.addParameter("Table.extendedErase", !Constants.isWindowsXP);
		defaults.addParameter("Table.useTree", true);
		
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
		
		// "v3.StartTab" didn't exist before 4209_B49 and is written at startup.
		// Use it as indicator to reset columns so beta users get correct columns
		// ("Big View" only).  As a backup (in addition to), reset on first 4210
		// run
		if (!COConfigurationManager.hasParameter("v3.StartTab", true)
				|| (ConfigurationChecker.isNewVersion() && Constants.compareVersions(
						Constants.getBaseVersion(), "4.2.1.0") == 0)) {
			// Reset 'big' columns, remove some tables that no longer exist
			Map<?, ?> map = FileUtil.readResilientConfigFile("tables.config");
			if (map != null && map.size() > 0) {
  			Object[] keys = map.keySet().toArray();
  			boolean removedSome = false;
  			for (int i = 0; i < keys.length; i++) {
  				if (keys[i] instanceof String) {
  					String sKey = (String) keys[i];
  					if (sKey.endsWith(".big") || sKey.startsWith("Table.library-")
  							|| sKey.startsWith("Table.Media")
  							|| sKey.startsWith("Table.activity.table")
  							|| sKey.equals("Table.Activity.big")
  							|| sKey.equals("Table.Activity_SB")) {
  						map.remove(sKey);
  						removedSome = true;
  					}
  				}
  			}
  			if (removedSome) {
  				FileUtil.writeResilientConfigFile("tables.config", map);
  			}
			}
		}
		
		// AZ3 doesn't have a View->Toolbar, so force enable
		if (!config.getBooleanParameter("IconBar.enabled")) {
			config.setParameter("IconBar.enabled", true);
		}

		
		if (configNeedsSave) {
			config.save();
		}
	}
}
