/*
 * File    : PluginManager.java
 * Created : 14-Dec-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.plugins;

import java.util.Properties;

import org.gudy.azureus2.pluginsimpl.PluginManagerImpl;

/**
 * @author parg
 * This class allows Azureus to be started as an embedded component and also allows plugins to
 * be dynamically registered
 */


public class 
PluginManager
{
	public static final int	UI_NONE		= 0;	// No user interface
	public static final int	UI_SWT		= 1;	// SWT user interface
	
		// property keys
	
		// normally Azureus will only permit a single instance to run per machine
	
	public static final String	PR_MULTI_INSTANCE	= "MULTI_INSTANCE";	// values "true" or "false"
	
	public static void
	startAzureus(
		int			ui_type,
		Properties	properties )
	{
		PluginManagerImpl.startAzureus( ui_type, properties );
	}
	
	public static void
	stopAzureus()
	
		throws PluginException
	{
		PluginManagerImpl.stopAzureus();
	}
	
	/**
	 * Programatic plugin registration interface
	 * @param plugin_class	this must implement Plugin
	 */
	
	public static void
	registerPlugin(
		Class		plugin_class )
	{
		PluginManagerImpl.registerPlugin( plugin_class );
	}
}
