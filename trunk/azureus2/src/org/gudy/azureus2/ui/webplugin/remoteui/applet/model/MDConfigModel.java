/*
 * File    : MDConfigModel.java
 * Created : 17-Feb-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet.model;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.*;

public class 
MDConfigModel 
{
	protected PluginInterface	pi;
	
	protected int		refresh_period;
	
	protected List		listeners = new ArrayList();
	
	public
	MDConfigModel(
		PluginInterface	_pi )
	{
		pi		= _pi;
		
		PluginConfig	plugin_config = pi.getPluginconfig();
		
		refresh_period = plugin_config.getPluginIntParameter( "MDConfigModel:refresh_period", 30 );
	}
	
	public int
	getRefreshPeriod()
	{
		return( refresh_period );
	}
	
	public void
	setRefreshPeriod(
		int	v )
	{
		refresh_period = v;
	
		PluginConfig	plugin_config = pi.getPluginconfig();
		
		plugin_config.setPluginParameter( "MDConfigModel:refresh_period", refresh_period );
		
		fireEvent( MDConfigModelPropertyChangeEvent.PT_REFRESH_PERIOD, new Integer( refresh_period ));
	}
	
	protected void
	fireEvent(
		int		type,
		Object	value )
	{
		MDConfigModelPropertyChangeEvent	ev = new MDConfigModelPropertyChangeEvent( type, value );
		
		for (int i=0;i<listeners.size();i++){
			
			((MDConfigModelListener)listeners.get(i)).propertyChanged(ev);
		}
	}
	
	public void
	addListener(
			MDConfigModelListener	l )
	{
		listeners.add( l );
	}
}
