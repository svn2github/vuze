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

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.diskmanager.file.*;

public abstract class 
FMFileImpl
	implements FMFile
{	
	private static final String		READ_ACCESS_MODE	= "r";
	private static final String		WRITE_ACCESS_MODE	= "rwd";
	
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
	
	private FMFileManagerImpl	manager;
	private FMFileOwner			owner;
	private int					type				= FT_LINEAR;
	private int					access_mode			= FM_READ;
	
	private File				linked_file;
	private String				canonical_path;
	private RandomAccessFile	raf;
	
	private FMFileAccess		file_access;
	
	protected AEMonitor			this_mon	= new AEMonitor( "FMFile" );
	
	
	protected
	FMFileImpl(
		FMFileOwner			_owner,
		FMFileManagerImpl	_manager,
		File				_file,
		int					_type )
	
		throws FMFileManagerException
	{
		owner			= _owner;
		manager			= _manager;
		type			= _type;
		
		linked_file		= manager.getLinkedFile( _file );
		
		boolean	file_was_created	= false;
		
		try{
      
			try {
				canonical_path = linked_file.getCanonicalPath();
				
			}catch( IOException ioe ) {
				
				String msg = ioe.getMessage();
				
		        if( msg != null && msg.indexOf( "There are no more files" ) != -1 ) {
					
		          String abs_path = linked_file.getAbsolutePath();
				  
		          String error = "Caught 'There are no more files' exception during file.getCanonicalPath(). " +
		                         "os=[" +Constants.OSName+ "], file.getPath()=[" +linked_file.getPath()+ "], file.getAbsolutePath()=[" +abs_path+ "]. ";
				  
		          Debug.out( error, ioe );
		        }
				
		        throw ioe;
			}
			
			File	parent = linked_file.getParentFile();
			
	        if ( !parent.exists()){
	        	  
	        	if ( !parent.mkdirs()){
	        		  
	        		throw( new FMFileManagerException( "Failed to create parent directory '" + parent + "'"));	
	        	}
	        }
	        
			reserveFile();
						
		
			// linear file shouldn't exist for change to occur - it is the responsibility
			// of the caller to delete the file first and take consequent actions (in
			// particular force recheck the file to ensure that the loss in save state
			// is represented in the resume view of the world)
		
			File	control_file = getControlFile();
			
			if ( control_file == null ){
				
				Debug.out( "No control file" );
				
			}else{
			
				int	old_type = control_file.exists()?FT_COMPACT:FT_LINEAR;
					
				if ( old_type != type ){
				
					if ( linked_file.exists()){
							
						throw( new FMFileManagerException( "Can't change between linear and compact file formats as file already exists" ));
					}
			
						// get rid if any existing control file as it is redundant info
			
					if ( control_file.exists()){
						
						control_file.delete();
					}
					
						// create a new file so that the switch cycle works correctly. If we
						// don't do this then the download will fail with "missing file" exception
						// when started
					
					try{
						if ( linked_file.createNewFile()){
							
							file_was_created	= true;
						}
						
					}catch( Throwable e ){
						
						throw( new FMFileManagerException( "createNewFile fails", e ));
					}
				}
			}
					
			if ( type == FT_LINEAR ){
				
				file_access = new FMFileAccessLinear( this );
				
			}else{
				
				file_access = new FMFileAccessCompact( control_file,  new FMFileAccessLinear( this ) );
			}	
		}catch( Throwable e ){
			
			if ( file_was_created ){
				
				linked_file.delete();
			}
			
			if ( e instanceof FMFileManagerException ){
				
				throw((FMFileManagerException)e);
			}
			
			throw( new FMFileManagerException( "initialisation failed", e ));
		}
	}

	protected FMFileManagerImpl
	getManager()
	{
		return( manager );
	}
	
	public String
	getName()
	{
		return( linked_file.toString());
	}
	
	public boolean
	exists()
	{
		return( linked_file.exists());
	}
	
	public FMFileOwner
	getOwner()
	{
		return( owner );
	}
	
	protected File
	getControlFile()
	{
		TOTorrentFile	tf = owner.getTorrentFile();
		
		if ( tf == null ){

			return( null );
		}
		
		TOTorrent	torrent = tf.getTorrent();
		
		TOTorrentFile[]	files = torrent.getFiles();
		
		int	file_index = -1;
		
		for (int i=0;i<files.length;i++){
			
			if ( files[i] == tf ){
		
				file_index = i;
				
				break;
			}
		}
		
		if ( file_index == -1 ){
			
			Debug.out("File '" + canonical_path + "' not found in torrent!" );
			
			return( null );
		}else{
			
			File	control = owner.getControlFile( "fmfile" + file_index + ".dat" );
		
			return( control );
		}
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
		File		new_unlinked_file )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			String	new_canonical_path;
			File	new_linked_file	= manager.getLinkedFile( new_unlinked_file );
			
			try{
        
		        try {
					
		          new_canonical_path = new_linked_file.getCanonicalPath();
				  
	
		        }catch( IOException ioe ) {
					
		          String msg = ioe.getMessage();
				  
		          if( msg != null && msg.indexOf( "There are no more files" ) != -1 ) {
		            String abs_path = new_linked_file.getAbsolutePath();
		            String error = "Caught 'There are no more files' exception during new_file.getCanonicalPath(). " +
		                           "os=[" +Constants.OSName+ "], new_file.getPath()=[" +new_linked_file.getPath()+ "], new_file.getAbsolutePath()=[" +abs_path+ "]. ";
		                           //"new_canonical_path temporarily set to [" +abs_path+ "]";
		            Debug.out( error, ioe );
		          }
		          throw ioe;
		        }
						
			}catch( Throwable e ){
				
				throw( new FMFileManagerException( "getCanonicalPath fails", e ));
			}	
			
			if ( new_linked_file.exists()){
				
				throw( new FMFileManagerException( "moveFile fails - file '" + new_canonical_path + "' already exists"));	
			}
			
	        File	parent = new_linked_file.getParentFile();
	          
	        if ( !parent.exists()){
	        	  
	        	if ( !parent.mkdirs()){
	        		  
	        		throw( new FMFileManagerException( "moveFile fails - failed to create parent directory '" + parent + "'"));	
	        	}
	        }
	        
			boolean	was_open	= raf != null;
			
			close();
			
			if ( FileUtil.renameFile( linked_file, new_linked_file )) {
				
				linked_file		= new_linked_file;
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
					  		
			openSupport( reason );

		}finally{
			
			this_mon.exit();
		}
	}

	protected long
	getLengthSupport()
	
		throws FMFileManagerException
	{
		return( file_access.getLength( raf ));
	}
	
	protected void
	setLengthSupport(
		long		length )
	
		throws FMFileManagerException
	{
		file_access.setLength( raf, length );
	}
	
	protected void
	openSupport(
		String	reason )
	
		throws FMFileManagerException
	{
		if ( raf != null ){
			
			closeSupport(true);
		}

		reserveAccess( reason );
		
		try{		
			raf = new RandomAccessFile( linked_file, access_mode==FM_READ?READ_ACCESS_MODE:WRITE_ACCESS_MODE);
			
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
	
	public void
	flush()
	
		throws FMFileManagerException
	{
		file_access.flush();
	}
	
	public void
	delete()
	
		throws FMFileManagerException
	{
		close();
		
		if ( linked_file.exists()){
			
			if ( !linked_file.delete()){
				
				throw( new FMFileManagerException( "Failed to delete '" + linked_file + "'" ));
			}
		}
	}
	
	protected void
	readSupport(
		DirectByteBuffer	buffer,
		long				offset )
	
		throws FMFileManagerException
	{
		file_access.read( raf, buffer, offset );
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
		file_access.write( raf, buffers, position );
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
					
					Debug.out( "FMFileImpl:reserve file - entry already present" );
					
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
