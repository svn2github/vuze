/*
 * File    : DownloadScrapeResultImpl.java
 * Created : 13-Jan-2004
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

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;

import org.gudy.azureus2.core3.tracker.client.*;

public class 
DownloadScrapeResultImpl
	implements DownloadScrapeResult
{
	protected Download					download;
	protected TRTrackerScraperResponse	response;
	
	protected
	DownloadScrapeResultImpl(
		Download					_download,
		TRTrackerScraperResponse	_response )
	{
		download	= _download;
		response	= _response;
	}
	
	protected void
	setContent(
		TRTrackerScraperResponse	_response )
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
		if ( response != null && response.isValid()){
			
			return( RT_SUCCESS );
			
		}else{
			
			return( RT_ERROR );
		}
	}
	
	public int
	getSeedCount()
	{
		return( response==null?-1:response.getSeeds());
	}
	
	public int
	getNonSeedCount()
	{
		return( response==null?-1:response.getPeers());
	}

  public long getScrapeStartTime() {
		return( response==null?-1:response.getScrapeStartTime());
  }

  public void setNextScrapeStartTime(long nextScrapeStartTime) {
    if (response != null)
      response.setNextScrapeStartTime(nextScrapeStartTime);
  }
  
  public String
  getStatus()
  {
  	if ( response != null ){
  		return( response.getStatusString());
  	}
  	
  	return("");
  }
}
