/*
 * File    : DownloadScrapeResult.java
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

package org.gudy.azureus2.plugins.download;

/**
 * @author parg
 * This class represents the results of scrapes for the download
 */

public interface 
DownloadScrapeResult 
{
	public static final int	RT_SUCCESS	= 1;
	public static final int RT_ERROR	= 2;
	
	/**
	 * Gives access to the associated download
	 * @return
	 */
	
	public Download
	getDownload();
	
	/**
	 * A scrape result can denote either a successful or failed scrape.
	 * @return RT_SUCCESS or RT_ERROR
	 */
	
	public int
	getResponseType();	// either RT_SUCCESS or RT_ERROR
	
	/**
	 * Gives the number of seeds returned by the scrape
	 * @return
	 */
	
	public int
	getSeedCount();
	
	/**
	 * Gives the number of non-seeds returned by the scrape
	 * @return
	 */
	
	public int
	getNonSeedCount();
}
