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

public abstract class 
FMFileImpl
	implements FMFile
{
	protected int					access_mode;
	protected File					file;
	protected RandomAccessFile		raf;
	
	protected
	FMFileImpl(
		File		_file )
	{
		file	= _file;
	}

	public File
	getFile()
	{
		return( file );
	}
	
	public int
	getAccessMode()
	{
		return( access_mode );
	}
	
	public synchronized void
	moveFile(
		File		new_file )
	
		throws FMFileManagerException
	{
		close();
		
		if ( file.renameTo(new_file)) {
			
			file	= new_file;
      
      openSupport();
			
		}else{
			
			throw( new FMFileManagerException( "FMFile::moveFile: Fails"));
		}	
	}
	
	protected long
	getLengthSupport()
	
		throws FMFileManagerException
	{
		try{
			return( raf.length());
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "FMFile::getLength: Fails", e ));
		}
	}
	
	protected void
	setLengthSupport(
		long		length )
	
		throws FMFileManagerException
	{
		try{			
			raf.setLength( length );
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "FMFile::setLength: Fails", e ));
		}
	}
	
	protected long
	getSizeSupport()
	
		throws FMFileManagerException
	{
		try{
      if (raf == null) throw new FMFileManagerException( "FMFile::getSize: raf is null" );
      
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
	
	protected void
	openSupport()
	
		throws FMFileManagerException
	{
		try{		
			raf = new RandomAccessFile( file, access_mode==FM_READ?"r":"rw");
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new FMFileManagerException( "FMFile::Open fails", e ));
		}
	}
	
	protected void
	closeSupport()
	
		throws FMFileManagerException
	{
		if ( raf == null ){
			
			return;
		}
		
		try{			
			raf.close();
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException("FMFile::close Fails", e ));
			
		}finally{
			
			raf	= null;
		}
	}
	
	protected void
	readSupport(
		ByteBuffer		buffer,
		long			offset )
	
		throws FMFileManagerException
	{
    if (raf == null) throw new FMFileManagerException( "FMFile::read: raf is null" );
    
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
	
	protected int
	writeSupport(
		ByteBuffer		buffer,
		long			position )
	
		throws FMFileManagerException
	{
    if (raf == null) throw new FMFileManagerException( "FMFile::write: raf is null" );
    
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
