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
	getCompleted();
				
	public long
	getDownloaded();
	
	public long
	getUploaded();
	
	public long
	getDiscarded();
	
	public long
	getHashFails();
	
	public int
	getShareRatio();
	
	public int
	getDownloadAverage();
		
	public int
	getUploadAverage();

	public int
	getTotalAverage();
			
	public String
	getElapsed();
	
	public String
	getETA();
		
		// set methods

	public void
	setDownloadedUploaded(
		long	d,
		long	u );

	public void
	setMaxUploads(
		int		max );
		
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

}
