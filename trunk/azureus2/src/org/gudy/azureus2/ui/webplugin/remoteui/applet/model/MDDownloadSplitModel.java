/*
 * File    : MDDownloadSplitModel.java
 * Created : 01-Apr-2004
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

import org.gudy.azureus2.plugins.download.*;

public class 
MDDownloadSplitModel 
{
	protected MDDownloadFullModel		full_model;
	protected MDDownloadFilterModel		downloading_model;
	protected MDDownloadFilterModel		seeding_model;
	
	public
	MDDownloadSplitModel(
		DownloadManager		_download_manager )
	{
		full_model	= new  MDDownloadFullModel( _download_manager );
		
		downloading_model = 
			new MDDownloadFilterModel(
					full_model,
					new MDDownloadFilter()
					{
						public boolean
						isSelected(
							Download		download )
						{
							return( download.getStats().getDownloadCompleted(false) != 1000 );
						}
					});
		
		seeding_model = 
			new MDDownloadFilterModel(
					full_model,
					new MDDownloadFilter()
					{
						public boolean
						isSelected(
							Download		download )
						{
							return( download.getStats().getDownloadCompleted(false) == 1000 );
						}
					});
	}
	
	public MDDownloadModel
	getFullModel()
	{
		return( full_model );
	}
	
	public MDDownloadModel
	getDownloadingModel()
	{
		return( downloading_model );
	}
	
	public MDDownloadModel
	getSeedingModel()
	{
		return( seeding_model );
	}
}
