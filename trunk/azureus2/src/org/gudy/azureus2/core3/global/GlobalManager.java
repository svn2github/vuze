/*
 * File    : GlobalManager.java
 * Created : 21-Oct-2003
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

package org.gudy.azureus2.core3.global;

import java.util.List;

import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.download.*;

public interface
GlobalManager
{
	public DownloadManager
	addDownloadManager(
		String			file_name,
		String			save_path );
	
	public DownloadManager
	addDownloadManager(
		String			file_name,
		String			save_path,
		boolean			start_stopped );
		
	public DownloadManager
	addDownloadManager(
	    String 		fileName,
	    String 		savePath,
	    boolean 	startStopped,
		boolean		persistent );
  
	public void
	removeDownloadManager(
		DownloadManager	dm )
	
		throws GlobalManagerDownloadRemovalVetoException;
	
	public void
	canDownloadManagerBeRemoved(
			DownloadManager	dm )
	
		throws GlobalManagerDownloadRemovalVetoException;
	
	public List
	getDownloadManagers();
	
	public void
	stopAll();

  /**
   * Stops all downloads without removing them
   *
   * @author Rene Leonhardt
   */
  public void
  stopAllDownloads();
  
  /**
   * Starts all downloads
   */
  public void
  startAllDownloads();
    
	public TRTrackerScraper
	getTrackerScraper();
	
	public GlobalManagerStats
	getStats();

	public int
	getIndexOf(
		DownloadManager	dm );
	
	public boolean
	isMoveableDown(
		DownloadManager	dm );
	
	public boolean
	isMoveableUp(
		DownloadManager	dm );
	
	public void
	moveDown(
		DownloadManager	dm );
	
	public void
	moveUp(
		DownloadManager	dm );
		
  public void
  moveEnd(
      DownloadManager[] dm );
  
  public void
  moveTop(
      DownloadManager[] dm );
  
  public void
	addListener(
		GlobalManagerListener	l );
		
	public void
	removeListener(
		GlobalManagerListener	l );

	public void
	addDownloadWillBeRemovedListener(
		GlobalManagerDownloadWillBeRemovedListener	l );
	
	public void
	removeDownloadWillBeRemovedListener(
		GlobalManagerDownloadWillBeRemovedListener	l );
	
  /**
   * @param c the character to be found 
   * @param lastSelectedIndex the highest selection index; -1 to start from the beginning
   * @return index of next item with a name beginning with c, -1 else
   *
   * @author Rene Leonhardt
   */
  public int getNextIndexForCharacter(char c, int lastSelectedIndex);
  
  public void startChecker();
}