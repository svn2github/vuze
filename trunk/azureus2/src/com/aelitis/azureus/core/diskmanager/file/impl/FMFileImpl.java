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

package com.aelitis.azureus.core.diskmanager.file.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.diskmanager.file.*;

public abstract class 
FMFileImpl
	implements FMFile
{
	protected static final long REOPEN_EVERY_BYTES 		= 50 * 1024 * 1024;
	protected static final int 	WRITE_RETRY_LIMIT		= 10;
	protected static final int	WRITE_RETRY_DELAY		= 100;
	
	protected static final boolean	DEBUG	= true;
	
	protected long lBytesRead = 0;
	protected long lClosedAt = 0;

	protected static Map			file_map = new HashMap();
	
	protected FMFileOwner			owner;
	protected int					access_mode			= FM_READ;
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
		
		if ( FileUtil.renameFile( file, new_file)) {
			
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
	
	public synchronized void
	ensureOpen()
	
		throws FMFileManagerException
	{
		if ( raf != null )
		  return;
		long lTimeToWait = lClosedAt + 1000 - System.currentTimeMillis();
		if (lTimeToWait > 0) {
      try {
        Thread.sleep(lTimeToWait);
      } catch (Exception ignore) { ignore.printStackTrace(); }
    }

		if (raf == null)
  		openSupport();
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
	  if (raf != null) {
	    closeSupport(true);
	  }

		try{		
			raf = new RandomAccessFile( file, access_mode==FM_READ?"r":"rwd");
			
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
			
  	  lClosedAt = System.currentTimeMillis();
  	  
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

		long lRemainingBeforeRead = buffer.remaining();

		try{
			fc.position(offset);
			
			while (fc.position() < fc.size() && buffer.hasRemaining()){
				
				buffer.read(fc);
			}
			
		}catch ( Exception e ){
			
			e.printStackTrace();
			
			throw( new FMFileManagerException( "FMFile::read: " + e.getMessage() + " (readFileInfoIntoBuffer)", e ));
		}

    // Recycle handle to clear OS cache
	  lBytesRead += lRemainingBeforeRead - buffer.remaining();
	  if (lBytesRead >= REOPEN_EVERY_BYTES) {
	    lBytesRead = 0;
  	  close();
  	}
	}
	
	protected void
	writeSupport(
		DirectByteBuffer		buffer,
		long					position )
	
		throws FMFileManagerException
	{
		writeSupport(new DirectByteBuffer[]{buffer}, position );
	}
	
	protected void
	writeSupport(
		DirectByteBuffer[]		buffers,
		long					position )
	
		throws FMFileManagerException
	{
		if (raf == null){
			
			throw( new FMFileManagerException( "FMFile::write: raf is null" ));
		}
    
		FileChannel fc = raf.getChannel();
    
		try{
			
			if (fc.isOpen()){
				
				long	expected_write 	= 0;
				long	actual_write	= 0;
				
				if ( DEBUG ){
				
					for (int i=0;i<buffers.length;i++){
						
						expected_write += buffers[i].limit() - buffers[i].position();
					}
				}
				
				fc.position( position );
				
				if ( buffers.length == 1 ){
					
					DirectByteBuffer	bb = buffers[0];
					
					int	loop	= 0;
					
					while( bb.position() != bb.limit()){
																		
						int	written = bb.write(fc);
						
						actual_write	+= written;
						
						if ( written > 0 ){
							
							loop	= 0;
							
							if ( DEBUG ){
								
								if (  bb.position() != bb.limit()){
								
									System.out.println( "FMFile::write: **** partial write ****");
								}
							}
						}else{
						
							loop++;
							
							System.out.println( "FMFile::write: zero length write - retrying" );
							
							if ( loop == WRITE_RETRY_LIMIT ){
								
								throw( new FMFileManagerException( "FMFile::write: retry limit exceeded"));
							}else{
								
								try{
									Thread.sleep( WRITE_RETRY_DELAY*loop );
									
								}catch( InterruptedException e ){
									
									throw( new FMFileManagerException( "FMFile::write: interrupted" ));
								}
							}
						}
					}
					
				}else{
					
					ByteBuffer[]	bbs = new ByteBuffer[buffers.length];
					
					ByteBuffer	last_bb	= null;
					
					for (int i=0;i<bbs.length;i++){
						
						bbs[i] = buffers[i].getBuffer();
						
						if ( bbs[i].position() != bbs[i].limit()){
							
							last_bb	= bbs[i];
						}
					}
					
					if ( last_bb != null ){
											  
						int	loop	= 0;
						
						while( last_bb.position() != last_bb.limit()){
							
							long	written = fc.write( bbs );
							
							actual_write	+= written;
							
							if ( written > 0 ){
								
								loop	= 0;
								
								if ( DEBUG ){
									
									if ( last_bb.position() != last_bb.limit()){
									
										System.out.println( "FMFile::write: **** partial write ****");
									}
								}
								
							}else{
							
								loop++;
								
								System.out.println( "FMFile::write: zero length write - retrying" );
								
								if ( loop == WRITE_RETRY_LIMIT ){
									
									throw( new FMFileManagerException( "FMFile::write: retry limit exceeded"));
								}else{
									
									try{
										Thread.sleep( WRITE_RETRY_DELAY*loop );
										
									}catch( InterruptedException e ){
										
										throw( new FMFileManagerException( "FMFile::write: interrupted" ));
									}
								}
							}						
						}
					}
				}
				
				if ( DEBUG ){

					if ( expected_write != actual_write ){
						
						throw( new FMFileManagerException( "FMFile::write: expected write/actual write mismatch" ));
					
					}
				}
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
