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

import java.util.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.file.*;

public abstract class 
FMFileImpl
	implements FMFile
{
	protected static Map			file_map = new HashMap();
	
	protected FMFileOwner			owner;
	protected int					access_mode;
	protected File					file;
	protected String				canonical_path;
	protected RandomAccessFile		raf;
	
	protected
	FMFileImpl(
		FMFileOwner		_owner,
		File			_file )
	
		throws FMFileManagerException
	{
		owner	= _owner;
		file	= _file;
		
		try{
			canonical_path = file.getCanonicalPath();
			
			reserveFile();
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "FMFile::getCanonicalPath: Fails", e ));
		}
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
		String	new_canonical_path;

		try{
			new_canonical_path = new_file.getCanonicalPath();
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "FMFile::getCanonicalPath: Fails", e ));
		}	
		
		if ( new_file.exists()){
			
			throw( new FMFileManagerException( "FMFile::moveFile: Fails - file '" + new_canonical_path + "' already exists"));	
		}
		
		boolean	was_open	= raf != null;
		
		close();
		
		if ( file.renameTo(new_file)) {
			
			file			= new_file;
			canonical_path	= new_canonical_path;
			
			reserveFile();
			
			openSupport();
			
		}else{
		
			try{
				reserveFile();
				
			}catch( FMFileManagerException e ){
				
				e.printStackTrace();
			}
			
			if ( was_open ){
				
				try{
					openSupport();
					
				}catch( FMFileManagerException e){
					
					e.printStackTrace();
				}
			}
			
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
		if (raf == null){
			
			throw new FMFileManagerException( "FMFile::getSize: raf is null" );
		}
	      
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
	closeSupport(
		boolean		explicit )
	
		throws FMFileManagerException
	{
		if ( raf == null ){
			
				// may have previously been implicitly closed, tidy up if required
			
			if ( explicit ){
				
				releaseFile();
			}
			
			return;
		}
		
		try{			
			raf.close();
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException("FMFile::close Fails", e ));
			
		}finally{
			
			raf	= null;
			
			if ( explicit ){
				
				releaseFile();
			}
		}
	}
	
	protected void
	readSupport(
		DirectByteBuffer		buffer,
		long			offset )
	
		throws FMFileManagerException
	{
		if (raf == null){
			
			throw new FMFileManagerException( "FMFile::read: raf is null" );
		}
    
		FileChannel fc = raf.getChannel();
    		
		if ( !fc.isOpen()){
			
			Debug.out("FileChannel is closed: " + file.getAbsolutePath());
			
			throw( new FMFileManagerException( "FMFile::read - file is closed"));
		}

		try{
			fc.position(offset);
			
			while (fc.position() < (fc.size() - 1) && buffer.hasRemaining()){
				
				buffer.read(fc);
			}
			
		}catch ( Exception e ){
			
			e.printStackTrace();
			
			throw( new FMFileManagerException( "FMFile::read: " + e.getMessage() + " (readFileInfoIntoBuffer)", e ));
		}
	}
	
	protected int
	writeSupport(
		DirectByteBuffer		buffer,
		long			position )
	
		throws FMFileManagerException
	{
		if (raf == null){
			
			throw new FMFileManagerException( "FMFile::write: raf is null" );
		}
    
		FileChannel fc = raf.getChannel();
    
		try{
			
			if (fc.isOpen()){
				
				fc.position( position );
				
				return( buffer.write(fc));
				
			}else{
				
				Debug.out("file channel is not open !");
				
				throw( new FMFileManagerException( "FMFile::write: Fails " ));
			}
			
		}catch (Exception e ){
			
			e.printStackTrace();
			
			throw( new FMFileManagerException( "FMFile::write: Fails", e ));
		}		
	}
	
	protected void
	reserveFile()
	
		throws FMFileManagerException
	{
		synchronized( file_map ){
			
			// System.out.println( "FMFile::reserveFile:" + canonical_path + "("+ owner.getName() + ")" );
			
			FMFileOwner	existing_owner = (FMFileOwner)file_map.get(canonical_path);
			
			if ( existing_owner == null ){
				
				file_map.put( canonical_path, owner );
				
			}else if ( !existing_owner.getName().equals( owner.getName())){
				
				throw( new FMFileManagerException( "File '"+canonical_path+"' is in use by '" + existing_owner.getName()+"'"));
			}
		}
	}
	
	protected void
	releaseFile()
	{
		synchronized( file_map ){
			
			// System.out.println( "FMFile::releaseFile:" + canonical_path );
					
			file_map.remove( canonical_path );
		}
	}
}
