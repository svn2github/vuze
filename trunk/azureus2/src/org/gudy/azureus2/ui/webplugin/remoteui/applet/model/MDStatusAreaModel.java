/*
 * File    : MDStatusAreaModel.java
 * Created : 14-Mar-2004
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

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.ipfilter.*;
import org.gudy.azureus2.pluginsremote.*;

public class 
MDStatusAreaModel 
{
	protected PluginInterface		pi;
	protected MDDownloadModel		dl_model;
	protected IPFilter				ip_filter;
	
	public
	MDStatusAreaModel(
		PluginInterface		_pi,
		MDDownloadModel		_dl_model )
	{
		pi			= _pi;
		dl_model	= _dl_model;
		
		ip_filter	= pi.getIPFilter();
	}
	
	public void
	refresh()
	{
		if ( ip_filter instanceof RPObject ){
			
			((RPObject)ip_filter)._refresh();
		}
	}
	
	public long
	getUploadSpeed()
	{
		long	res = 0;
		
		Download[]	downloads = dl_model.getDownloads();
		
		for (int i=0;i<downloads.length;i++){
			
			Download	dl = downloads[i];
			
			res += dl.getStats().getUploadAverage();
		}
		
		return( res );
	}
	
	public long
	getDownloadSpeed()
	{
		long	res = 0;
		
		Download[]	downloads = dl_model.getDownloads();
		
		for (int i=0;i<downloads.length;i++){
			
			Download	dl = downloads[i];
			
			res += dl.getStats().getDownloadAverage();
		}
		
		return( res );		
	}
	
	public long
	getIPFilterUpdateTime()
	{
		return( ip_filter.getLastUpdateTime());
	}	
	
	public int
	getIPFilterNumberOfRanges()
	{
		return( ip_filter.getNumberOfRanges());
	}	
	
	public int
	getIPFilterNumberOfBlockedIPs()
	{
		return( ip_filter.getNumberOfBlockedIPs());
	}
	
	public String
	getAzureusVersion()
	{
		return( pi.getAzureusName() + " " + pi.getAzureusVersion());
	}
}
