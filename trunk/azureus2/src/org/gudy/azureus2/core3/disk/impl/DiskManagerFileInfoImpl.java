/*
 * File    : DiskManagerFileInfoImpl.java
 * Created : 18-Oct-2003
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

package org.gudy.azureus2.core3.disk.impl;
/*
 * Created on 3 juil. 2003
 *
 */
import java.io.File;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;

import com.aelitis.azureus.core.diskmanager.cache.*;

/**
 * @author Olivier
 * 
 */
public class 
DiskManagerFileInfoImpl
	implements DiskManagerFileInfo, CacheFileOwner
{
  private CacheFile		cache_file;
  
  private String 		path;
  private String 		name;
  private String 		extension;
  private long 			length;
  private long 			downloaded;
  private int 			firstPieceNumber = -1;
  private int 			nbPieces = 0;
  
  private DiskManagerImpl 	diskManager;
  private TOTorrentFile		torrent_file;
  
  private boolean priority = false;  
  private boolean skipped = false;
  
  protected
  DiskManagerFileInfoImpl(
  	DiskManagerImpl		_disk_manager,
  	File				_file,
	TOTorrentFile		_torrent_file )
  
  	throws CacheFileManagerException
  {
    diskManager 	= _disk_manager;
    torrent_file	= _torrent_file;
    
  	try{
      path	= _file.getParentFile().getCanonicalPath() + System.getProperty("file.separator");
      name	= _file.getName();
    }
    catch (Exception e) {
      Debug.out("Unable to resolve canonical path for " + _file.getName());
      Debug.printStackTrace( e );
    }
  	
  	cache_file = CacheFileManagerFactory.getSingleton().createFile( this, _file );
  }
  
  public String
  getCacheFileOwnerName()
  {
  	return( diskManager.getName());
  }
	public TOTorrentFile
	getCacheFileTorrentFile()
	{
		return( torrent_file );
	}
	
  public void
  flushCache()
	
	throws	Exception
  {
  	cache_file.flushCache();
  }
  
  protected void
  moveFile(
  	File	newFile )
  
  	throws CacheFileManagerException
  {
  	cache_file.moveFile( newFile );
  	
  	try {
      path = newFile.getParentFile().getCanonicalPath() + System.getProperty("file.separator");
    }
    catch (Exception e) { Debug.out("Unable to resolve canonical path for " + newFile.getName()); }
  }
  
  public CacheFile
  getCacheFile()
  {
  	return( cache_file );
  }
  
  public void
  setAccessMode(
  	int		mode )
  
  	throws CacheFileManagerException
  {
  	cache_file.setAccessMode( mode==DiskManagerFileInfo.READ?CacheFile.CF_READ:CacheFile.CF_WRITE );
  }
  
  public int 
  getAccessMode()
  {
  	int	mode = cache_file.getAccessMode();
  	
	return( mode == CacheFile.CF_READ?DiskManagerFileInfo.READ:DiskManagerFileInfo.WRITE);
  }

  /**
   * @return
   */
  public long getDownloaded() {
	return downloaded;
  }

  /**
   * @return
   */
  public String getExtension() {
	return extension;
  }

  /**
   * @return
   */
  public File getFile() {
	return( cache_file.getFile());
  }

  /**
   * @return
   */
  public int getFirstPieceNumber() {
	return firstPieceNumber;
  }

  /**
   * @return
   */
  public long getLength() {
	return length;
  }

  /**
   * @return
   */
  public String getName() {
	return name;
  }

  /**
   * @return
   */
  public int getNbPieces() {
	return nbPieces;
  }


  /**
   * @param l
   */
  public void setDownloaded(long l) {
	downloaded = l;
  }

  /**
   * @param string
   */
  public void setExtension(String string) {
	extension = string;
  }


  /**
   * @param i
   */
  public void setFirstPieceNumber(int i) {
	firstPieceNumber = i;
  }

  /**
   * @param l
   */
  public void setLength(long l) {
	length = l >= 0L ? l : 0L;
  }


  /**
   * @param i
   */
  public void setNbPieces(int i) {
	nbPieces = i;
  }

  /**
   * @return
   */
  public String getPath() {
	return path;
  }


  /**
   * @return
   */
  public boolean isPriority() {
	return priority;
  }

  /**
   * @param b
   */
  public void setPriority(boolean b) {
	priority = b;
  }

  /**
   * @return
   */
  public boolean isSkipped() {
	return skipped;
  }

  /**
   * @param skipped
   */
  public void setSkipped(boolean _skipped) {
	skipped = _skipped;
  }

  public DiskManager getDiskManager() {
    return diskManager;
  }
}
