/*
 * File    : FMFileImpl.java
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

package org.gudy.azureus2.core3.disk.file.impl;

/**
 * @author parg
 *
 */

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.file.*;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;

public class 
FMFileImpl
	implements FMFile
{
	protected int					access_mode;
	protected File					file;
	protected RandomAccessFile		raf;
	
	
	public void
	setFile(
		File		_file )
	{
		file		= _file;
	}
	
	public File
	getFile()
	{
		return( file );
	}
	
	public synchronized void
	setAccessMode(
		int		mode )
	
		throws FMFileManagerException
	{
		if( mode == access_mode && raf != null ){
			
			return;
		}
	
		access_mode		= mode;
		
		if ( raf != null ){
		
			try{
					
				raf.close();
				
			}catch( Throwable e ){
			
				e.printStackTrace();
				
				throw( new FMFileManagerException( "FMFile::Close Fails", e ));
			}
		}
		
		try{
			
			raf = new RandomAccessFile( file, access_mode==DiskManagerFileInfo.READ?"r":"rwd");
						
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new FMFileManagerException( "FMFile::Open fails", e ));
		}
	}
	
	public int
	getAccessMode()
	{
		return( access_mode );
	}
	
	public synchronized void
	close()
	
		throws FMFileManagerException
	{
		try{
			if ( raf != null ){
				
				raf.close();
			
				raf	= null;
			}
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException("Close fails", e ));
		}
	}
	
	public long
	getSize()
	
		throws FMFileManagerException
	{
		try{
			FileChannel	channel = raf.getChannel();
			
			if ( channel.isOpen()){
				
				return( channel.size());
				
			}else{
				
				Debug.out("FileChannel is not open");
				
				throw( new FMFileManagerException( "FMFile::getSize: channel not open"));
			}
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "FMFile::getSize: Fails", e ));
		}
	}
	
	public long
	getLength()
	
		throws FMFileManagerException
	{
		try{
			return( raf.length());
				
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "FMFile::getLength: Fails", e ));
		}
	}
	
	public void
	setLength(
		long		length )
	
		throws FMFileManagerException
	{
		try{			
			raf.setLength( length );
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "FMFile::setLength: Fails", e ));
		}
	}
	public synchronized void
	read(
		ByteBuffer		buffer,
		long			offset )
	
		throws FMFileManagerException
	{
		FileChannel fc = raf.getChannel();
	
		if ( !fc.isOpen()){
				
			Debug.out("FileChannel is closed: " + file.getAbsolutePath());
				
			throw( new FMFileManagerException( "FMFile::read - file is closed"));
		}

		try{
			fc.position(offset);
				
			while (fc.position() < (fc.size() - 1) && buffer.hasRemaining()){
					
				fc.read(buffer);
			}
								
		}catch ( Exception e ){
				
			e.printStackTrace();
			
			throw( new FMFileManagerException( "FMFile::read: " + e.getMessage() + " (readFileInfoIntoBuffer)", e ));
		}
	}
	
	public synchronized int
	write(
		ByteBuffer		buffer,
		long			position )
	
		throws FMFileManagerException
	{
		FileChannel fc = raf.getChannel();
		
		try{
			
			if (fc.isOpen()){
			
				fc.position( position );
	
				return( fc.write(buffer));
				
			}else{
	
				Debug.out("file channel is not open !");
			
				throw( new FMFileManagerException( "FMFile::write: Fails " ));
			}
				
		}catch (Exception e ){
	
			e.printStackTrace();
			
			throw( new FMFileManagerException( "FMFile::write: Fails", e ));
		}		
	}
}
