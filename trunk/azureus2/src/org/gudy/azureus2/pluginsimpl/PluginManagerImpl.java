/*
 * File    : PluginManagerImpl.java
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

package org.gudy.azureus2.pluginsimpl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.ui.swt.MainWindow;

public class 
PluginManagerImpl 
{
	public static boolean	running		= false;
	public static int		ui_type		= PluginManager.UI_NONE;
	
	public static synchronized void
	startAzureus(
		int			_ui_type,
		Properties	properties )
	{
		if ( running ){
			
			throw( new RuntimeException( "Azureus is already running"));
		}
		
		running	= true;
		ui_type	= _ui_type;
		
		if ( ui_type == PluginManager.UI_NONE ){
		
			org.gudy.azureus2.ui.common.Main.main( new String[]{"--ui=console"});
			
		}else if ( ui_type == PluginManager.UI_SWT ){
				
			if ( properties != null ){
				
				String	mi = (String)properties.get( PluginManager.PR_MULTI_INSTANCE );
				
				if ( mi != null && mi.equalsIgnoreCase("true")){
					
					System.setProperty( org.gudy.azureus2.ui.swt.Main.PR_MULTI_INSTANCE, "true" );
				}
			}
			
			org.gudy.azureus2.ui.swt.Main.main(new String[0]);
		}
	}
	
	public static synchronized void
	stopAzureus()
	
		throws PluginException
	{
		if ( !running ){
			
			throw( new RuntimeException( "Azureus is not running"));
		}
				
		if ( ui_type == PluginManager.UI_NONE ){
			
			org.gudy.azureus2.ui.common.Main.shutdown();
			
		}else if ( ui_type == PluginManager.UI_SWT ){
			
			if ( !MainWindow.getWindow().dispose()){
				
				throw( new PluginException( "PluginManager: Azureus close action failed"));
			}
		}
		
		running	= false;
	}
	
	public static void
	registerPlugin(
		Class		plugin_class )
	{
		PluginInitializer.queueRegistration( plugin_class );
	}
}
