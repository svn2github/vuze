/*
 * File    : TorrentDownloader2Impl.java
 * Created : 27-Feb-2004
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

package org.gudy.azureus2.core3.resourcedownloader.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.resourcedownloader.*;

public abstract class 
ResourceDownloaderBaseImpl
	implements ResourceDownloader
{
	protected List			listeners		= new ArrayList();
	
	protected boolean		result_informed;
	
	protected void
	informPercentDone(
		int	percentage )
	{
		for (int i=0;i<listeners.size();i++){
			
			((ResourceDownloaderListener)listeners.get(i)).reportPercentComplete(this,percentage);
		}
	}
	
	protected void
	informActivity(
		String	activity )
	{
		for (int i=0;i<listeners.size();i++){
			
			((ResourceDownloaderListener)listeners.get(i)).reportActivity(this,activity);
		}
	}
	protected boolean
	informComplete(
		InputStream	is )
	{
		if ( !result_informed ){
			
			for (int i=0;i<listeners.size();i++){
				
				if ( !((ResourceDownloaderListener)listeners.get(i)).completed(this,is)){
					
					return( false );
				}
			}
			
			result_informed	= true;
		}
		
		return( true );
	}
	
	protected void
	informFailed(
		ResourceDownloaderException	e )
	{
		if ( !result_informed ){
			
			result_informed	= true;
		
			for (int i=0;i<listeners.size();i++){
				
				((ResourceDownloaderListener)listeners.get(i)).failed(this,e);
			}
		}
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		informActivity( activity );
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
		informPercentDone( percentage );
	}
	
	public void
	addListener(
		ResourceDownloaderListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		ResourceDownloaderListener		l )
	{
		listeners.remove(l);
	}
}
