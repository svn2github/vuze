/*
 * File    : FMFile.java
 * Created : 12-Feb-2004
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

package org.gudy.azureus2.core3.disk.file;

/**
 * @author parg
 *
 */

import java.io.File;
import java.nio.ByteBuffer;


public interface 
FMFile 
{
	public void
	setFile(
		File		file );
	
	public File
	getFile();
	
	public void
	setAccessMode(
		int		mode )
	
		throws FMFileManagerException;
	
	public int
	getAccessMode();
	
	public long
	getSize()
	
		throws FMFileManagerException;
	
	public long
	getLength()
		
		throws FMFileManagerException;

	public void
	setLength(
		long		length )
	
		throws FMFileManagerException;
	
	public void
	read(
		ByteBuffer	buffer,
		long		offset )
	
		throws FMFileManagerException;
	
	
	public int
	write(
		ByteBuffer	buffer,
		long		position )
	
		throws FMFileManagerException;
	
	public void
	close()
	
		throws FMFileManagerException;
}
