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
import org.gudy.azureus2.core3.download.DownloadManager;
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
  private File			file;
  private int			file_index;
  private CacheFile		cache_file;
  
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
  	int					_file_index,
	TOTorrentFile		_torrent_file,
	boolean				_linear_storage )
  
  	throws CacheFileManagerException
  {
    diskManager 	= _disk_manager;
    torrent_file	= _torrent_file;
  	
    file		= _file;
    file_index	= _file_index;
    
  	cache_file = CacheFileManagerFactory.getSingleton().createFile( 
  						this, _file, _linear_storage?CacheFile.CT_LINEAR:CacheFile.CT_COMPACT );
  }
  
  	public String
  	getCacheFileOwnerName()
  	{
  		return( diskManager.getDownloadManager().getInternalName());
  	}
  	
	public TOTorrentFile
	getCacheFileTorrentFile()
	{
		return( torrent_file );
	}
	
	public File 
	getCacheFileControlFile(String name) 
	{
		return( diskManager.getDownloadManager().getDownloadState().getStateFile( name ));
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
	  	// don't do anything to the actual file if this is a linked file
	  
	  if ( !isLinked()){
		  
		  cache_file.moveFile( newFile );
	  }
	  
	  file	= newFile;
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
	int	old_mode =  cache_file.getAccessMode();
	
  	cache_file.setAccessMode( mode==DiskManagerFileInfo.READ?CacheFile.CF_READ:CacheFile.CF_WRITE );
  	
  	if ( old_mode != mode ){
  		
  		diskManager.fileAccessModeChanged( this, old_mode, mode );
  	}
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
  public File 
  getFile(
	boolean	follow_link )
  	{
	  if ( follow_link ){
	  
		  File	res = getLink();
	  
		  if ( res != null ){
		
			  return( res );
		  }
	  }
	  
	  return( file );
  	}

	public boolean
	setLink(
		File	link_destination )
	{
		Debug.out( "setLink: download must be stopped" );
		
		return( false );
	}

	public File
	getLink()
	{
		return( diskManager.getDownloadManager().getDownloadState().getFileLink( getFile( false )));
	}
	
	public boolean
	setStorageType(
		int		type )
	{
		Debug.out( "setStorageType: download must be stopped" );
		
		return( false );
	}
	
	public int
	getStorageType()
	{
		String[]	types = diskManager.getStorageTypes();
		
		return( types[file_index].equals( "L")?ST_LINEAR:ST_COMPACT );

	}
	
	protected boolean
	isLinked()
	{
		return( getLink() != null );
	}
	
  /**
   * @return
   */
  public int getFirstPieceNumber() {
    return firstPieceNumber;
  }
  
  
  public int getLastPieceNumber() {
    return firstPieceNumber + nbPieces - 1;
  }

  /**
   * @return
   */
  public long getLength() {
	return length;
  }

	public int	
	getIndex()
	{
		return( file_index );
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
  public boolean isPriority() {
	return priority;
  }

  /**
   * @param b
   */
  public void setPriority(boolean b) {
	priority = b;
	diskManager.priorityChanged( this );
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
	diskManager.skippedFileSetChanged( this );
  }

  public DiskManager getDiskManager() {
    return diskManager;
  }
  
	public DownloadManager 
	getDownloadManager()
	{
		return( diskManager.getDownloadManager());
	}

}
