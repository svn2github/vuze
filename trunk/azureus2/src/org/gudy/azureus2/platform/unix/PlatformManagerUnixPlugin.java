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

package org.gudy.azureus2.platform.unix;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author TuxPaper
 * @created Jul 24, 2007
 *
 */
public class PlatformManagerUnixPlugin
	implements Plugin
{
	private PluginInterface plugin_interface;

	// @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	public void initialize(PluginInterface _plugin_interface)
			throws PluginException {
		plugin_interface = _plugin_interface;

		plugin_interface.getPluginProperties().setProperty("plugin.name",
				"Platform-Specific Support");

		String version = "1.0"; // default version if plugin not present

		PlatformManager platform = PlatformManagerFactory.getPlatformManager();

		if (platform.hasCapability(PlatformManagerCapabilities.GetVersion)) {

			try {
				version = platform.getVersion();

			} catch (Throwable e) {

				Debug.printStackTrace(e);
			}

		} else {

			plugin_interface.getPluginProperties().setProperty("plugin.version.info",
					"Not required for this platform");

		}

		plugin_interface.getPluginProperties().setProperty("plugin.version",
				version);
	}
}
