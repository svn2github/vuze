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
import org.gudy.azureus2.core3.disk.file.*;

/**
 * @author Olivier
 * 
 */
public class 
DiskManagerFileInfoImpl
	implements DiskManagerFileInfo
{
  private FMFile		fm_file = FMFileManagerFactory.getSingleton().createFile();
  
  private String 		path;
  private String 		name;
  private String 		extension;
  private long 			length;
  private long 			downloaded;
  private int 			firstPieceNumber = -1;
  private int 			nbPieces = 0;
  
  private boolean priority = false;  
  private boolean skipped = false;
  
  public FMFile
  getFMFile()
  {
  	return( fm_file );
  }
  
  public void
  setAccessMode(
  	int		mode )
  
  	throws FMFileManagerException
  {
  	fm_file.setAccessMode( mode );
  }
  
  public int 
  getAccessMode(){
	return( fm_file.getAccessMode());
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
	return( fm_file.getFile());
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
   * @param file
   */
  public void setFile(File file) {
	fm_file.setFile( file );
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
   * @param string
   */
  public void setName(String string) {
	name = string;
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
   * @param string
   */
  public void setPath(String string) {
	path = string;
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
  public void setSkipped(boolean skipped) {
	this.skipped = skipped;
  }

}
