/*
 * File    : DownloadStats.java
 * Created : 25 oct. 2003 16:23:16
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
public interface DownloadStats {

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
   * @return the current downloaded bytes
   */
  public int getDownloaded();
  
  /**
   * 
   * @return the current uploaded bytes
   */
  public int getUploaded();
  
  /**
   * 
   * @return the current share ratio 1000 means 1:1
   */
  public int getShareRatio();
  
  /**
   * 
   * @return the current number of peers
   */
  public int getNumberOfPeers();
  
  /**
   * 
   * @return the current number of seeds
   */
  public int getNumberOfSeeds();
  
  /**
   * 
   * @return the completion info 1000 means done
   */
  public int getPerThousandDone();
  
  
  /**
   * 
   * @return the current number of hash fails
   */
  public int getNumberOfHashFails();
  
  
  /**
   * 
   * @return the piece size
   */
  public int getPieceSize();
  
  /**
   * 
   * @return the estimated time of arrival in second
   */
  public int getEta();
  
  
  //TODO PLUGINS : Define other usefull methods
}
