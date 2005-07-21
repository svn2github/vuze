/*
 * File    : DownloadManagerStatsImpl.java
 * Created : 24-Oct-2003
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
 
package org.gudy.azureus2.core3.download.impl;

/**
 * @author parg
 */

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.disk.*;

public class 
DownloadManagerStatsImpl 
	implements DownloadManagerStats
{
	protected DownloadManagerImpl	download_manager;
	
	protected int maxUploads 			= 4;
	
		//Completed (used for auto-starting purposes)
		
	protected int completed;
	protected int downloadCompleted;
	
		// saved downloaded and uploaded
  protected long saved_data_bytes_downloaded;
  protected long saved_protocol_bytes_downloaded;
  
	protected long saved_data_bytes_uploaded;
  protected long saved_protocol_bytes_uploaded;
  
	protected long saved_discarded = 0;
	protected long saved_hashfails = 0;
	
	protected long saved_SecondsDownloading = 0;
	protected long saved_SecondsOnlySeeding = 0;
	
  protected int max_upload_rate_bps = 0;  //0 for unlimited
  protected int max_download_rate_bps = 0;  //0 for unlimited
  
  

	protected
	DownloadManagerStatsImpl(
		DownloadManagerImpl	dm )
	{
		download_manager = dm;
	}
	
	public void 
	dataBytesReceived(
		int length )
	{
		GlobalManager	gm = download_manager.getGlobalManager();
		
	   	if (length > 0 && gm != null){
	   		
	   	  gm.getStats().dataBytesReceived(length);
	   	}
	}
  
  
  public void 
  protocolBytesReceived(
    int length )
  {
    GlobalManager gm = download_manager.getGlobalManager();
    
      if (length > 0 && gm != null){
        
        gm.getStats().protocolBytesReceived(length);
      }
  }
  
  
  
  
	public void 
	discarded(int length) 
	{
		GlobalManager	gm = download_manager.getGlobalManager();
		
	   	if (length > 0 && gm != null){
	   		
	   	  gm.getStats().discarded(length);
	   	}
	}

	 public void 
	 dataBytesSent(int length) 
	 {
		GlobalManager	gm = download_manager.getGlobalManager();
		
	   	if (length > 0 && gm != null){
	   	
	   	  gm.getStats().dataBytesSent(length);
	   	}
	 }
   
   
   public void 
   protocolBytesSent(int length) 
   {
    GlobalManager gm = download_manager.getGlobalManager();
    
      if (length > 0 && gm != null){
      
        gm.getStats().protocolBytesSent(length);
      }
   }
   

	public long 
	getDataReceiveRate() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getDataReceiveRate();
	  	}
	  	
	  	return 0;
	}
  
  
  public long 
  getProtocolReceiveRate() 
  {
    PEPeerManager pm = download_manager.getPeerManager();
    
      if (pm != null){
      
        return pm.getStats().getProtocolReceiveRate();
      }
      
      return 0;
  }
  
  
  
	public long 
	getDataSendRate() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getDataSendRate();
	  	}
	  	
	  	return 0;
	}
  
  public long 
  getProtocolSendRate() 
  {
    PEPeerManager pm = download_manager.getPeerManager();
    
      if (pm != null){
      
        return pm.getStats().getProtocolSendRate();
      }
      
      return 0;
  }
  
  
	
	public long 
	getETA()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null){
	  
			return pm.getETA();
	  }
	  
	  return -1;   //return exactly -1 if ETA is unknown
	}

	public int getCompleted() {
		DiskManager	dm = download_manager.getDiskManager();
		
	  if (dm == null) {
	    int state = download_manager.getState();
	    if (state == DownloadManager.STATE_ALLOCATING ||
	        state == DownloadManager.STATE_CHECKING ||
	        state == DownloadManager.STATE_INITIALIZING)
	      return completed;
	    else
	      return downloadCompleted;
	  }
	  if (dm.getState() == DiskManager.ALLOCATING || 
	      dm.getState() == DiskManager.CHECKING || 
	      dm.getState() == DiskManager.INITIALIZING)
      return dm.getPercentDone();
	  else {
      long total = dm.getTotalLength();
      return total == 0 ? 0 : (int) ((1000 * (total - dm.getRemaining())) / total);
	  }
	}

	public void setCompleted(int _completed) {
	  completed = _completed;
	}

	public int 
	getDownloadCompleted(
		boolean bLive ) 
	{
		DiskManager	dm = download_manager.getDiskManager();
		
			// no disk manager -> not running -> use stored value
		
		if ( dm == null ){
			
		   return downloadCompleted;
		}
		
	    int state = dm.getState();

	    boolean	transient_state = 
	    		state == DiskManager.INITIALIZING ||
	            state == DiskManager.ALLOCATING   ||
	            state == DiskManager.CHECKING;
	    
	    long total = dm.getTotalLength();
	    
	    int computed_completion = (total == 0) ? 0 : (int) ((1000 * (total - dm.getRemaining())) / total);

	    	// use non-transient values to update the record of download completion
	    
	    if ( !transient_state ){
	    	
	    	downloadCompleted = computed_completion;
	    }
	    
	    if ( bLive ){
	    
	    		// return the transient completion level
	    	
	    	return computed_completion;
	    	
	    }else{
	    	
	    		// return the non-transient one
	    	
	    	return( downloadCompleted );
	    }
	}
  
  public void setDownloadCompleted(int _completed) {
    downloadCompleted = _completed;
  }


	/**
	 * @return
	 */
	public int getMaxUploads() {
	  return maxUploads;
	}

	/**
	 * @param i
	 */
	public void setMaxUploads(int i) {
	  maxUploads = i;
	}

	public String getElapsedTime() {
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null)
		return pm.getElapsedTime();
	  return ""; //$NON-NLS-1$
	}
	
	public long getTimeStarted() {
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null)
		  return pm.getTimeStarted();
		return -1;
	}

	public long getTimeStartedSeeding() {
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null)
		  return pm.getTimeStartedSeeding();
		return -1;
	}

	public long getTotalDataBytesReceived() {
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null) {
	    return saved_data_bytes_downloaded + pm.getStats().getTotalDataBytesReceived();
	  }
	  return(saved_data_bytes_downloaded);
	}	
  
  
  public long getTotalProtocolBytesReceived() {
    PEPeerManager pm = download_manager.getPeerManager();
    
    if (pm != null) {
      return saved_protocol_bytes_downloaded + pm.getStats().getTotalProtocolBytesReceived();
    }
    return(saved_protocol_bytes_downloaded);
  } 
  
  
  
	public long getTotalDataBytesSent() {
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null) {
	    return saved_data_bytes_uploaded + pm.getStats().getTotalDataBytesSent();
    }
	  return( saved_data_bytes_uploaded );
	}
  
  
  public long getTotalProtocolBytesSent() {
    PEPeerManager pm = download_manager.getPeerManager();
    if (pm != null) {
      return saved_protocol_bytes_uploaded + pm.getStats().getTotalProtocolBytesSent();
    }
    return( saved_protocol_bytes_uploaded );
  }
  
  
	
	public long getDiscarded(){
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getStats().getTotalDiscarded();
	  return( saved_discarded );
	}
  
   public void setSavedDiscarded() {
     PEPeerManager  pm = download_manager.getPeerManager();
     if (pm == null) System.out.println("setDiscarded:: PeerManager null");
     else pm.getStats().setTotalDiscarded(saved_discarded);
   }
   
   public void saveDiscarded(long discarded) {
     this.saved_discarded = discarded;
   }

	public long getHashFails(){
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getNbHashFails();
	  return( saved_hashfails );
	}
  
	public void setSavedHashFails() {
	  PEPeerManager  pm = download_manager.getPeerManager();
	  if (pm == null) System.out.println("setSavedHashFails:: PeerManager null");
	  else pm.setNbHashFails((int)saved_hashfails);
	}
  
  public void saveHashFails(long fails) {
    this.saved_hashfails = fails;
  }
  

	public long 
	getTotalAverage() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
	
		if (pm != null){
			
			return pm.getStats().getTotalAverage();
		}
		
		return( 0 );
	}
      
  
	public int 
	getShareRatio() 
	{
	  long downloaded,uploaded;
	  
	  PEPeerManager	pm = download_manager.getPeerManager();
    
	  if (pm != null ){
		  
		  downloaded = saved_data_bytes_downloaded + pm.getStats().getTotalDataBytesReceived();
	    
		  uploaded = saved_data_bytes_uploaded + pm.getStats().getTotalDataBytesSent();
	    
	  }else{
		  downloaded = saved_data_bytes_downloaded;
		  
		  uploaded = saved_data_bytes_uploaded;
	  }
        
	  downloaded -= ( getHashFails() + getDiscarded());
	  
	  if( downloaded <= 0) {
		  
	    return -1;
	  }

	  return (int) ((1000 * uploaded) / downloaded);
	}
	
	
	public void 
	setSavedDownloadedUploaded( //TODO separate into data+protocol ?
		long 	downloaded,
		long 	uploaded ) 
	{
	  saved_data_bytes_downloaded = downloaded;
	  saved_data_bytes_uploaded = uploaded;
	}
  

	protected long
	getSavedDownloaded() //TODO separate into data+protocol ?
	{
		return( saved_data_bytes_downloaded );
	}
		
	protected long
	getSavedUploaded()  //TODO separate into data+protocol ?
	{
		return( saved_data_bytes_uploaded );
	}
	
  
	public long getSecondsDownloading() {
	  long lTimeStartedDL = getTimeStarted();
	  if (lTimeStartedDL >= 0) {
  	  long lTimeEndedDL = getTimeStartedSeeding();
  	  if (lTimeEndedDL == -1) {
  	    lTimeEndedDL = SystemTime.getCurrentTime();
  	  }
  	  if (lTimeEndedDL > lTimeStartedDL) {
    	  return saved_SecondsDownloading + ((lTimeEndedDL - lTimeStartedDL) / 1000);
    	}
  	}
	  return saved_SecondsDownloading;
	}

	public long getSecondsOnlySeeding() {
	  long lTimeStarted = getTimeStartedSeeding();
	  if (lTimeStarted >= 0) {
	    return saved_SecondsOnlySeeding + 
	           ((SystemTime.getCurrentTime() - lTimeStarted) / 1000);
	  }
	  return saved_SecondsOnlySeeding;
	}
	
	public void setSecondsDownloading(long seconds) {
	  saved_SecondsDownloading = seconds;
	}

	public void setSecondsOnlySeeding(long seconds) {
	  saved_SecondsOnlySeeding = seconds;
	}

	
	public float
	getAvailability()
	{
	    PEPeerManager  pm = download_manager.getPeerManager();

	    if ( pm == null ){
	    	
	    	return( -1 );
	    }
		
	    return( pm.getMinAvailability());
	}
	 
  
	public int getUploadRateLimitBytesPerSecond() {  return max_upload_rate_bps;  }

	public void setUploadRateLimitBytesPerSecond( int max_rate_bps ) {  max_upload_rate_bps = max_rate_bps;  }
  
  
  public int getDownloadRateLimitBytesPerSecond() {  return max_download_rate_bps;  }
  
  public void setDownloadRateLimitBytesPerSecond( int max_rate_bps ) {  max_download_rate_bps = max_rate_bps;  }
    
}
