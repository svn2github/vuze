/*
 * File    : DownloadManagerStats.java
 * Created : 22-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.download;

/**
 * @author parg
 *
 */
public interface 
DownloadManagerStats 
{
	public int
	getMaxUploads();
	
	public int
	getMaxDownloadKBSpeed();
	
  /** Find out percentage done of current state
   * Use getDownloadCompleted() if you wish to find out a torrents download completion level
   *
   * @return 0 to 1000, 0% to 100% respectively
   *         When state is Allocating, Checking, or Initializing, this
   *         returns the % done of that task
   *         Any other state MAY return getDownloadCompleted()
   */
	public int
	getCompleted();

  /** Retrieve the level of download completion.
   * 
   * To understand the bLive parameter, you must know a bit about the
   * Torrent activation process:
   * 1) Torrent goes into ST_WAITING
   * 2) Torrent moves to ST_PREPARING
   * 3) Torrent moves to ST_DOWNLOADING or ST_SEEDING
   *
   * While in ST_PREPARING, Completion Level is rebuilt (either via Fast Resume
   * or via piece checking). Quite often, the download completion level before
   * ST_PREPARING and after ST_PREPARING are identical.
   *
   * Before going into ST_PREPARING, we store the download completion level.
   * If you wish to retrieve this value instead of the live "building" one,
   * pass false for the parameter.
   *
   * @param bLive true - Always returns the known completion level of the torrent
   *               false - In the case of ST_PREPARING, return completion level 
   *                       before of the torrent ST_PREPARING started.  
   *                       Otherwise, same as true.
   * @return 0 - 1000
   */
	public int
	getDownloadCompleted(boolean bLive);
	
	public void 
	setDownloadCompleted(int completed);
				
	public long
	getDownloaded();
	
	public long
	getUploaded();
	
	public long
	getDiscarded();
  
   public void saveDiscarded(long discarded);
   
   public void setSavedDiscarded();
	
	public long
	getHashFails();
  
	public void saveHashFails(long fails);
  
	public void setSavedHashFails();
	
	public int
	getShareRatio();
	
	public long
	getDownloadAverage();
		
	public long
	getUploadAverage();

	public long
	getTotalAverage();
			
	public String
	getElapsedTime();
	
	// in ms
	public long
	getTimeStarted();

  /* -1 if not seeding */		
	public long
	getTimeStartedSeeding();

	public long
	getETA();
	
	public float
	getAvailability();
		

	public long 
	getSecondsDownloading();

	public long 
	getSecondsOnlySeeding();

		// set methods

	public void
	setSavedDownloadedUploaded(
		long	d,
		long	u );

	public void
	setMaxUploads(
		int		max );
		
	public void
	setMaxDownloadKBSpeed(
		int	max );
	
	public void
	setCompleted(
		int		c );
		
		
	public void
	received(
		int		l );
			
	public void
	discarded(
		int		l );
			
	public void
	sent(
		int		l );

	public void 
	setSecondsOnlySeeding(long seconds);
	
	public void 
	setSecondsDownloading(long seconds);
}
