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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.diskmanager.file.*;

public abstract class 
FMFileImpl
	implements FMFile
{
	//protected static final long REOPEN_EVERY_BYTES 		= 50 * 1024 * 1024;
	private static final int 	WRITE_RETRY_LIMIT		= 10;
	private static final int	WRITE_RETRY_DELAY		= 100;
	
	private static final boolean	DEBUG			= true;
	private static final boolean	DEBUG_VERBOSE	= false;
	
	private static final String		READ_ACCESS_MODE	= "r";
	private static final String		WRITE_ACCESS_MODE	= "rwd";
	
	//protected long lBytesRead = 0;
	//protected long lClosedAt = 0;

	private static Map			file_map = new HashMap();
	private static AEMonitor	file_map_mon	= new AEMonitor( "FMFile:map");
	
	static{
		AEDiagnostics.addEvidenceGenerator(
			new AEDiagnosticsEvidenceGenerator()
			{
				public void
				generate(
					IndentWriter		writer )
				{
					generateEvidence( writer );
				}
			});
	}
	
	private FMFileOwner			owner;
	private int					access_mode			= FM_READ;
	private File				file;
	private String				canonical_path;
	private RandomAccessFile	raf;
	
	protected AEMonitor				this_mon	= new AEMonitor( "FMFile" );
	
	
	protected
	FMFileImpl(
		FMFileOwner		_owner,
		File			_file )
	
		throws FMFileManagerException
	{
		owner	= _owner;
		file	= _file;
		
		try{
      
			try {
				canonical_path = file.getCanonicalPath();
				
			}catch( IOException ioe ) {
				
				String msg = ioe.getMessage();
				
		        if( msg != null && msg.indexOf( "There are no more files" ) != -1 ) {
					
		          String abs_path = file.getAbsolutePath();
				  
		          String error = "Caught 'There are no more files' exception during file.getCanonicalPath(). " +
		                         "os=[" +Constants.OSName+ "], file.getPath()=[" +file.getPath()+ "], file.getAbsolutePath()=[" +abs_path+ "]. ";
		                         //"canonical_path temporarily set to [" +abs_path+ "]";
				  
		          Debug.out( error, ioe );
		        }
				
		        throw ioe;
			}
			
			reserveFile();
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "getCanonicalPath fails", e ));
		}
	}

	public File
	getFile()
	{
		return( file );
	}
	
	public FMFileOwner
	getOwner()
	{
		return( owner );
	}
	
	public int
	getAccessMode()
	{
		return( access_mode );
	}
	
	protected void
	setAccessModeSupport(
		int		mode )
	{
		access_mode	= mode;
	}
	
	public void
	moveFile(
		File		new_file )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			String	new_canonical_path;
	
			try{
        
		        try {
					
		          new_canonical_path = new_file.getCanonicalPath();
				  
		        }catch( IOException ioe ) {
					
		          String msg = ioe.getMessage();
				  
		          if( msg != null && msg.indexOf( "There are no more files" ) != -1 ) {
		            String abs_path = new_file.getAbsolutePath();
		            String error = "Caught 'There are no more files' exception during new_file.getCanonicalPath(). " +
		                           "os=[" +Constants.OSName+ "], new_file.getPath()=[" +new_file.getPath()+ "], new_file.getAbsolutePath()=[" +abs_path+ "]. ";
		                           //"new_canonical_path temporarily set to [" +abs_path+ "]";
		            Debug.out( error, ioe );
		          }
		          throw ioe;
		        }
						
			}catch( Throwable e ){
				
				throw( new FMFileManagerException( "getCanonicalPath fails", e ));
			}	
			
			if ( new_file.exists()){
				
				throw( new FMFileManagerException( "moveFile fails - file '" + new_canonical_path + "' already exists"));	
			}
			
			boolean	was_open	= raf != null;
			
			close();
			
			if ( FileUtil.renameFile( file, new_file)) {
				
				file			= new_file;
				canonical_path	= new_canonical_path;
				
				reserveFile();
				
				openSupport( "moveFile target" );
				
			}else{
			
				try{
					reserveFile();
					
				}catch( FMFileManagerException e ){
					
					Debug.printStackTrace( e );
				}
				
				if ( was_open ){
					
					try{
						openSupport( "moveFile recovery" );
						
					}catch( FMFileManagerException e){
						
						Debug.printStackTrace( e );
					}
				}
				
				throw( new FMFileManagerException( "moveFile fails"));
			}	
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	ensureOpen(
		String	reason )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
	
			if ( raf != null ){
				
			  return;
			}
			
      /*
      long lTimeToWait = lClosedAt + 1000 - SystemTime.getCurrentTime();
			if (lTimeToWait > 0) {
	      try {
	        Thread.sleep(lTimeToWait);
	      } catch (Exception ignore) { Debug.printStackTrace( ignore ); }
	    }
      */
	
			if ( raf == null ){
	  		
				openSupport( reason );
			}
		}finally{
			
			this_mon.exit();
		}
	}

	protected long
	getLengthSupport()
	
		throws FMFileManagerException
	{
		try{
			return( raf.length());
			
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "getLength fails", e ));
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
			
			throw( new FMFileManagerException( "setLength fails", e ));
		}
	}
	
	protected long
	getSizeSupport()
	
		throws FMFileManagerException
	{
		if (raf == null){
			
			throw new FMFileManagerException( "getSize: raf is null" );
		}
	      
		try{
			FileChannel	channel = raf.getChannel();
			
			if ( channel.isOpen()){
				
				return( channel.size());
				
			}else{
				
				Debug.out("FileChannel is not open");
				
				throw( new FMFileManagerException( "getSize: channel not open"));
			}
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "getSize fails", e ));
		}
	}
	
	protected void
	openSupport(
		String	reason )
	
		throws FMFileManagerException
	{
		if (raf != null){
			
			closeSupport(true);
		}

		reserveAccess( reason );
		
		try{		
			raf = new RandomAccessFile( file, access_mode==FM_READ?READ_ACCESS_MODE:WRITE_ACCESS_MODE);
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			throw( new FMFileManagerException( "open fails", e ));
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
			
			throw( new FMFileManagerException("close fails", e ));
			
		}finally{
			
  	  //lClosedAt = SystemTime.getCurrentTime();
  	  
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
			
			throw new FMFileManagerException( "read: raf is null" );
		}
    
		FileChannel fc = raf.getChannel();
    		
		if ( !fc.isOpen()){
			
			Debug.out("FileChannel is closed: " + file.getAbsolutePath());
			
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

    // Recycle handle to clear OS cache
	  //lBytesRead += lRemainingBeforeRead - buffer.remaining(DirectByteBuffer.SS_FILE);
	  //if (lBytesRead >= REOPEN_EVERY_BYTES) {
	  //  lBytesRead = 0;
  	//  close();
  	//}
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
	
	protected boolean
	isOpen()
	{
		return( raf != null );
	}
	
		// file reservation is used to manage the possibility of multiple torrents
		// refering to the same file. Initially introduced to stop a common problem
		// whereby different torrents contain the same files (DVD rips) - without 
		// this code the torrents could interfere resulting in all sorts of problems
		// The original behavior was to completely prevent the sharing of files.
		// However, better behaviour is to allow sharing of a file as long as only
		// read access is required.
		// we store a list of owners against each canonical file with a boolean "write" marker
	
	private void
	reserveFile()
	
		throws FMFileManagerException
	{
		try{
			file_map_mon.enter();
			
			// System.out.println( "FMFile::reserveFile:" + canonical_path + "("+ owner.getName() + ")" );
			
			List	owners = (List)file_map.get(canonical_path);
			
			if ( owners == null ){
				
				owners = new ArrayList();
				
				file_map.put( canonical_path, owners );				
			}
			
			for (Iterator it=owners.iterator();it.hasNext();){
				
				Object[]	entry = (Object[])it.next();
			
				if ( owner.getName().equals(entry[0])){
				
						// already present, start off read-access
					
					entry[1] = new Boolean( false );
					
					return;	
				}
			}
			
			owners.add( new Object[]{ owner.getName(), new Boolean( false ), "<reservation>" });
			
		}finally{
			
			file_map_mon.exit();
		}
	}
	
	private void
	reserveAccess(
		String	reason )
	
		throws FMFileManagerException
	{
		try{
			file_map_mon.enter();
			
			// System.out.println( "FMFile::reserveAccess:" + canonical_path + "("+ owner.getName() + ")" + " [" + (access_mode==FM_WRITE?"write":"read") + "]" );
			
			List	owners = (List)file_map.get( canonical_path );
			
			Object[]	my_entry = null;
					
			if ( owners == null ){
				
				throw( new FMFileManagerException( "File '"+canonical_path+"' has not been reserved (no entries), '" + owner.getName()+"'"));
			}
			
			for (Iterator it=owners.iterator();it.hasNext();){
					
				Object[]	entry = (Object[])it.next();
				
				if ( owner.getName().equals(entry[0])){
					
					my_entry	= entry;
				}
			}				
			
			if ( my_entry == null ){
				
				throw( new FMFileManagerException( "File '"+canonical_path+"' has not been reserved (not found), '" + owner.getName()+"'"));
			}
		
			my_entry[1] = new Boolean( access_mode==FM_WRITE );
			my_entry[2] = reason;
			
			int	read_access 	= 0;
			int write_access	= 0;
			
			String	users = "";
				
			for (Iterator it=owners.iterator();it.hasNext();){
				
				Object[]	entry = (Object[])it.next();
								
				if (((Boolean)entry[1]).booleanValue()){
					
					write_access++;
					
					users += (users.length()==0?"":",") + entry[0] + " [write]";

				}else{
					
					read_access++;
					
					users += (users.length()==0?"":",") + entry[0] + " [read]";
				}
			}

			if ( 	write_access > 1 ||
					( write_access == 1 && read_access > 0 )){
				
				throw( new FMFileManagerException( "File '"+canonical_path+"' is in use by '" + users +"'"));
			}
			
		}finally{
			
			file_map_mon.exit();
		}
	}
	
	private void
	releaseFile()
	{
		try{
			file_map_mon.enter();
		
			// System.out.println( "FMFile::releaseFile:" + canonical_path + "("+ owner.getName() + ")" );
					
			List	owners = (List)file_map.get( canonical_path );
			
			if ( owners != null ){
				
				for (Iterator it=owners.iterator();it.hasNext();){
					
					Object[]	entry = (Object[])it.next();
					
					if ( owner.getName().equals(entry[0])){
						
						it.remove();
						
						break;
					}
				}
				
				if ( owners.size() == 0 ){
					
					file_map.remove( canonical_path );
				}
			}
		}finally{
			
			file_map_mon.exit();
		}
	}
	
	protected static void
	generateEvidence(
		IndentWriter	writer )
	{
		writer.println( "FMFile Reservations" );
		
		try{
			writer.indent();

			try{
				file_map_mon.enter();
			
				Iterator	it = file_map.keySet().iterator();
				
				while( it.hasNext()){
					
					String	key = (String)it.next();
					
					List	owners = (List)file_map.get(key);
					
					Iterator	it2 = owners.iterator();
					
					String	str = "";
						
					while( it2.hasNext()){
						
						Object[]	entry = (Object[])it2.next();

						String	owner 	= (String)entry[0];
						Boolean	write	= (Boolean)entry[1];
						String	reason	= (String)entry[2];
						
						str += (owner.length()==0?"":", ") + owner + "[" + (write.booleanValue()?"write":"read")+ "/" + reason + "]";
					}
					
					writer.println( key + " -> " + str );
				}
			}finally{
				
				file_map_mon.exit();
			}
		}finally{
			
			writer.exdent();
		}
	}
}
