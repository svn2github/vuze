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
	public boolean
	addDownloadManager(
		String			file_name,
		String			save_path );
	
	public void
	removeDownloadManager(
		DownloadManager	dm );
		
	public List
	getDownloadManagers();
	
	public void
	stopAll();
	
	public TRTrackerScraper
	getTrackerScraper();
	
	public void
	sent(
		int		bytes );
		
	public void
	received(
		int		bytes );
		
	public void
	discarded(
		int		bytes );
		
	public String 
	getDownloadSpeed();

	public String 
	getUploadSpeed();

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
	addListener(
		GlobalManagerListener	l );
		
	public void
	removeListener(
		GlobalManagerListener	l );
}