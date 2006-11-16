/*
 * Created on May 31, 2006 1:21:29 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.update;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.FileUtil;

/**
 * Utility functions for Updater Plugin.
 * <p>
 * Moved from UpdateUpdateChecker to reduce (cyclical) references
 */
public class UpdaterUtils
{
	protected static String PLUGIN_ID = "azupdater";

	public static boolean disableNativeCode(String version) {
		try {
			File plugin_dir = null;

			// we can't check the user-dir here due to crazy recursion problems
			// during startup (platform manager init etc)

			File shared_plugin_dir = FileUtil.getApplicationFile("plugins");

			File shared_updater_plugin = new File(shared_plugin_dir, PLUGIN_ID);

			if (shared_updater_plugin.exists()) {

				plugin_dir = shared_updater_plugin;
			}

			if (plugin_dir == null) {

				return (false);
			}

			return (new File(plugin_dir, "disnat" + version).exists());

		} catch (Throwable e) {

			e.printStackTrace();
		}

		return (false);
	}

	public static void checkPlugin() {
		try {
			// this is a bootstrap to ensure that the updater plugin exists

			File user_plugin_dir = FileUtil.getUserFile("plugins");

			File user_updater_plugin = new File(user_plugin_dir, PLUGIN_ID);

			File user_updater_props = new File(user_updater_plugin,
					"plugin.properties");

			if (user_updater_props.exists()) {

				return;
			}

			File shared_plugin_dir = FileUtil.getApplicationFile("plugins");

			File shared_updater_plugin = new File(shared_plugin_dir, PLUGIN_ID);

			FileUtil.mkdirs(shared_updater_plugin);

			File props = new File(shared_updater_plugin, "plugin.properties");

			if (props.exists()) {

				return;
			}

			PrintWriter pw = null;

			try {
				pw = new PrintWriter(new FileWriter(props));

				pw.println("plugin.class=org.gudy.azureus2.update.UpdaterUpdateChecker;org.gudy.azureus2.update.UpdaterPatcher");
				pw.println("plugin.name=Azureus Update Support;Azureus Updater Support Patcher");

			} finally {

				if (pw != null) {

					pw.close();
				}
			}

			if (!props.exists()) {

				throw (new Exception("Failed to write '" + props.toString() + "'"));
			}

		} catch (Throwable e) {

			Logger.log(new LogAlert(LogAlert.UNREPEATABLE,
					"azupdater plugin: initialisation error", e));
		}
	}

}
