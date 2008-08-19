/**
 * File: PluginStateImpl.java
 * Date: 19 Aug 2008
 * Author: Allan Crooks
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the COPYING file ).
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.pluginsimpl.local;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginState;

public class PluginStateImpl implements PluginState {

	private PluginInterface pi;
	boolean failed;
	public PluginStateImpl(PluginInterface pi) {
		this.pi = pi;
	}
	
	public void setLoadedAtStartup(boolean load_at_startup) {
		String param_name = "PluginInfo." + pi.getPluginID() + ".enabled";
		COConfigurationManager.setParameter(param_name, load_at_startup);
	}
	
	public boolean isLoadedAtStartup() {
		String param_name = "PluginInfo." + pi.getPluginID() + ".enabled";
		if (!COConfigurationManager.hasParameter(param_name, false)) {
			return true; // Load at startup by default.
		}
		return COConfigurationManager.getBooleanParameter(param_name);
	}
	
	public boolean hasFailed() {
		return failed;
	}

	
}
