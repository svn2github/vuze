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
import org.gudy.azureus2.core3.disk.*;

public class 
DownloadManagerStatsImpl 
	implements DownloadManagerStats
{
	protected DownloadManagerImpl	download_manager;
	
	protected int maxUploads = 4;
	
		//Completed (used for auto-starting purposes)
		
	protected int completed;
	protected int downloadCompleted;
	
		// saved downloaded and uploaded
		
	protected long saved_downloaded;
	protected long saved_uploaded;
  
	protected long saved_discarded = 0;
	protected long saved_hashfails = 0;
	
	protected long saved_SecondsDownloading = 0;
	protected long saved_SecondsOnlySeeding = 0;
	
  

	protected
	DownloadManagerStatsImpl(
		DownloadManagerImpl	dm )
	{
		download_manager = dm;
	}
	
	public void 
	received(
		int length )
	{
		GlobalManager	gm = download_manager.getGlobalManager();
		
	   	if (length > 0 && gm != null){
	   		
			gm.getStats().received(length);
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
	 sent(int length) 
	 {
		GlobalManager	gm = download_manager.getGlobalManager();
		
	   	if (length > 0 && gm != null){
	   	
			gm.getStats().sent(length);
	   	}
	 }

	public long 
	getDownloadAverage() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getDownloadAverage();
	  	}
	  	
	  	return 0;
	}
  
	public long 
	getUploadAverage() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getUploadAverage();
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

	public void setCompleted(int completed) {
	  this.completed = completed;
	}

	public int getDownloadCompleted(boolean bLive) {
	  if (!bLive)
	    return downloadCompleted;

    DiskManager	dm = download_manager.getDiskManager();
    if (dm == null)
      return downloadCompleted;

    long total = dm.getTotalLength();
    int newValue = (total == 0) ? 0 : (int) ((1000 * (total - dm.getRemaining())) / total);
    int state = dm.getState();
    if (state != DiskManager.INITIALIZING &&
        state != DiskManager.ALLOCATING   &&
        state != DiskManager.CHECKING)
      downloadCompleted = newValue;
        
    return newValue;
  }
  
  public void setDownloadCompleted(int completed) {
    downloadCompleted = completed;
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

	public long getDownloaded() {
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null) {
		PEPeerManagerStats ps = pm.getStats();
		return saved_downloaded + ps.getTotalReceived();
	  }
	  return(saved_downloaded);
	}

	public long getUploaded() {
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return saved_uploaded + pm.getStats().getTotalSent();
	  return( saved_uploaded );
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
  

	public long getTotalAverage() {
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getStats().getTotalAverage();
	  return( 0 );
	}
      
  
	public int getShareRatio() {
	  long downloaded,uploaded;
	  PEPeerManager	pm = download_manager.getPeerManager();
	  if(pm != null) {
		downloaded = saved_downloaded + pm.getStats().getTotalReceived();
		uploaded = saved_uploaded + pm.getStats().getTotalSent();
	  } else {
		downloaded = this.saved_downloaded;
		uploaded = this.saved_uploaded;
	  }
        
	  if(downloaded == 0) {
		return -1;
	  }
	  else {
		return (int) ((1000 * uploaded) / downloaded);
	  }
	}
	
	
	public void 
	setSavedDownloadedUploaded(
		long 	downloaded,
		long 	uploaded ) 
	{
	  saved_downloaded = downloaded;
	  saved_uploaded = uploaded;
	}
  

	protected long
	getSavedDownloaded()
	{
		return( saved_downloaded );
	}
		
	protected long
	getSavedUploaded()
	{
		return( saved_uploaded );
	}
	
	public long getSecondsDownloading() {
	  long lTimeStartedDL = getTimeStarted();
	  if (lTimeStartedDL >= 0) {
  	  long lTimeEndedDL = getTimeStartedSeeding();
  	  if (lTimeEndedDL == -1) {
  	    lTimeEndedDL = System.currentTimeMillis() / 1000;
  	  }
  	  if (lTimeEndedDL > lTimeStartedDL) {
    	  return saved_SecondsDownloading + (lTimeEndedDL - lTimeStartedDL);
    	}
  	}
	  return saved_SecondsDownloading;
	}

	public long getSecondsOnlySeeding() {
	  long lTimeStarted = getTimeStartedSeeding();
	  if (lTimeStarted >= 0) {
	    return saved_SecondsOnlySeeding + 
	           ((System.currentTimeMillis() - lTimeStarted) / 1000);
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
}
