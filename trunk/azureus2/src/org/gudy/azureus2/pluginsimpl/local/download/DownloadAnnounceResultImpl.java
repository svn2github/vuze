/*
 * File    : DownloadAnnounceResultImpl.java
 * Created : 12-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.local.download;

/**
 * @author parg
 *
 */

import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.tracker.client.*;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResultPeer;

public class 
DownloadAnnounceResultImpl 
	implements DownloadAnnounceResult
{
	protected DownloadImpl			download;
	protected TRTrackerAnnouncerResponse		response;

	protected
	DownloadAnnounceResultImpl(
		DownloadImpl		_download,
		TRTrackerAnnouncerResponse	_response )
	{
		download	= _download;
		response	= _response;
	}
	
	protected void
	setContent(
		TRTrackerAnnouncerResponse	_response )
	{
		response = _response;
	}
	
	public Download
	getDownload()
	{
		return( download );
	}
	
	public int
	getResponseType()
	{
		if ( response == null ){
			
			return( RT_ERROR );
		}
		
		int status = response.getStatus();
		
		if ( status == TRTrackerAnnouncerResponse.ST_ONLINE ){
			
			return( RT_SUCCESS );
		}else{
			
			return( RT_ERROR );
		}	
	}
		
	public int
	getReportedPeerCount()
	{
		return( response==null?0:response.getPeers().length );
	}
	
	public int
	getSeedCount()
	{
		PEPeerManager	pm = download.getDownload().getPeerManager();
				
		if ( pm != null ){
			
			return( pm.getNbSeeds());
		}
		
		return( 0 );
	}
	
	public int
	getNonSeedCount()
	{
		PEPeerManager	pm = download.getDownload().getPeerManager();
		
		if ( pm != null ){
			
			return( pm.getNbPeers());
		}
	
		return( 0 );
	}
		
	public String
	getError()
	{
		return( response==null?"No Response":response.getFailureReason());
	}
	
	public URL
	getURL()
	{
		return( response==null?null:response.getURL());
	}
	
	public DownloadAnnounceResultPeer[]
	getPeers()
	{
		if ( response == null ){
			
			return( new DownloadAnnounceResultPeer[0]);
		}
		
		return( response.getPeers());
	}
	
	public long
	getTimeToWait()
	{
		if ( response == null ){
			
			return( -1 );
		}
		
		return( response.getTimeToWait());
	}
	
	public Map
	getExtensions()
	{
		if ( response == null ){
			
			return( null );
		}
		
		return( response.getExtensions());
	}
}
