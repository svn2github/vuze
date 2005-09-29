/*
 * Created on 28-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.diskmanager.file.impl;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.file.FMFileManagerException;

public class 
FMFileAccessLinear
	implements FMFileAccess
{
	private static final int 	WRITE_RETRY_LIMIT		= 10;
	private static final int	WRITE_RETRY_DELAY		= 100;
	
	private static final boolean	DEBUG			= true;
	private static final boolean	DEBUG_VERBOSE	= false;

	private FMFileImpl		owner;
	
	protected
	FMFileAccessLinear(
		FMFileImpl		_owner )
	{
		owner	= _owner;
	}
	
	public long
	getLength(
		RandomAccessFile		raf )
	
		throws FMFileManagerException
	{
		try{
			return( raf.length());
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "getLength fails", e ));
		}
	}
	
	public void
	setLength(
		RandomAccessFile		raf,
		long					length )
	
		throws FMFileManagerException
	{
		try{			
			raf.setLength( length );
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "setLength fails", e ));
		}
	}
	
	public void
	read(
		RandomAccessFile	raf,
		DirectByteBuffer	buffer,
		long				offset )
	
		throws FMFileManagerException
	{
		if (raf == null){
			
			throw new FMFileManagerException( "read: raf is null" );
		}
    
		FileChannel fc = raf.getChannel();
    		
		if ( !fc.isOpen()){
			
			Debug.out("FileChannel is closed: " + owner.getName());
			
			throw( new FMFileManagerException( "read - file is closed"));
		}

		long lRemainingBeforeRead = buffer.remaining(DirectByteBuffer.SS_FILE);

		try{
			fc.position(offset);
			
			while (fc.position() < fc.size() && buffer.hasRemaining(DirectByteBuffer.SS_FILE)){
				
				buffer.read(DirectByteBuffer.SS_FILE,fc);
			}
			
		}catch ( Exception e ){
			
			Debug.printStackTrace( e );
			
			throw( new FMFileManagerException( "read fails", e ));
		}
	}
	
	public void
	write(
		RandomAccessFile		raf,
		DirectByteBuffer[]		buffers,
		long					position )
	
		throws FMFileManagerException
	{
		if (raf == null){
			
			throw( new FMFileManagerException( "write fails: raf is null" ));
		}
    
		FileChannel fc = raf.getChannel();
    
		try{
			
			if (fc.isOpen()){
				
				long	expected_write 	= 0;
				long	actual_write	= 0;
				boolean	partial_write	= false;
				
				if ( DEBUG ){
				
					for (int i=0;i<buffers.length;i++){
						
						expected_write += buffers[i].limit(DirectByteBuffer.SS_FILE) - buffers[i].position(DirectByteBuffer.SS_FILE);
					}
				}
				
				fc.position( position );
									
				ByteBuffer[]	bbs = new ByteBuffer[buffers.length];
				
				ByteBuffer	last_bb	= null;
				
				for (int i=0;i<bbs.length;i++){
					
					bbs[i] = buffers[i].getBuffer(DirectByteBuffer.SS_FILE);
					
					if ( bbs[i].position() != bbs[i].limit()){
						
						last_bb	= bbs[i];
					}
				}
				
				if ( last_bb != null ){
										  
					int		loop			= 0;
					
					while( last_bb.position() != last_bb.limit()){
						
						long	written = fc.write( bbs );
						
						actual_write	+= written;
						
						if ( written > 0 ){
							
							loop	= 0;
							
							if ( DEBUG ){
								
								if ( last_bb.position() != last_bb.limit()){
								
									partial_write	= true;
									
									if ( DEBUG_VERBOSE ){
										
										Debug.out( "FMFile::write: **** partial write **** this = " + written + ", total = " + actual_write + ", target = " + expected_write );
									}
								}
							}
							
						}else{
						
							loop++;
							
							if ( loop == WRITE_RETRY_LIMIT ){
								
								Debug.out( "FMFile::write: zero length write - abandoning" );
							
								throw( new FMFileManagerException( "write fails: retry limit exceeded"));
								
							}else{
								
								if ( DEBUG_VERBOSE ){
									
									Debug.out( "FMFile::write: zero length write - retrying" );
								}
								
								try{
									Thread.sleep( WRITE_RETRY_DELAY*loop );
									
								}catch( InterruptedException e ){
									
									throw( new FMFileManagerException( "write fails: interrupted" ));
								}
							}
						}						
					}
				}
				
				if ( DEBUG ){

					if ( expected_write != actual_write ){
						
						Debug.out( "FMFile::write: **** partial write **** failed: expected = " + expected_write + ", actual = " + actual_write );

						throw( new FMFileManagerException( "write fails: expected write/actual write mismatch" ));
					
					}else{
						
						if ( partial_write && DEBUG_VERBOSE ){
							
							Debug.out( "FMFile::write: **** partial write **** completed ok" );
						}
					}
				}
			}else{
				
				Debug.out("file channel is not open !");
				
				throw( new FMFileManagerException( "write fails " ));
			}
			
		}catch (Exception e ){
			
			Debug.printStackTrace( e );
			
			throw( new FMFileManagerException( "write fails", e ));
		}		
	}
	
	public void
	flush()
	
		throws FMFileManagerException
	{
		// no state to flush
	}
}
