/*
 * File    : GlobalStats.java
 * Created : 25 oct. 2003 16:17:15
 * By      : Olivier 
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
 
package org.gudy.core3.plugins.stats.data;

/**
 * @author Olivier
 * 
 */
public interface GlobalStats {

  /**
   * 
   * @return the current download speed in bytes per second
   */
  public int getDownloadSpeed();
  
  /**
   * 
   * @return the current upload speed in bytes per second
   */
  public int getUploadSpeed();
  
  /**
   * 
   * @return the current number of torrents in azureus
   */
  public int getNumberOfLoadedTorrents();
  
  /**
   * 
   * @return the current number of running torrents in azureus
   */
  public int getNumberOfActiveTorrents();
  
  /**
   * 
   * @return the current number of downloading torrents
   */
  public int getNumberOfDownloads();
  
  /**
   * 
   * @return the current number of seeding torrents
   */
  public int getNumberOfUploads();
  
  
}
