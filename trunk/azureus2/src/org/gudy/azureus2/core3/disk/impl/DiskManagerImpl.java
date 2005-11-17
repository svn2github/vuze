/*
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
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
 * 
 * Created on Oct 18, 2003
 * Created by Paul Gardner
 * Modified Apr 13, 2004 by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */

package org.gudy.azureus2.core3.disk.impl;

import com.aelitis.azureus.core.diskmanager.cache.CacheFile;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerException;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerFactory;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileOwner;
import com.aelitis.azureus.core.diskmanager.file.FMFileManagerFactory;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.access.DMAccessFactory;
import org.gudy.azureus2.core3.disk.impl.access.DMReader;
import org.gudy.azureus2.core3.disk.impl.access.DMWriterAndChecker;
import org.gudy.azureus2.core3.disk.impl.piecepicker.DMPiecePicker;
import org.gudy.azureus2.core3.disk.impl.piecepicker.DMPiecePickerFactory;
import org.gudy.azureus2.core3.disk.impl.resume.RDResumeHandler;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.LocaleUtilEncodingException;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 
 * The disk Wrapper.
 * 
 * @author Tdv_VgA
 *
 */
public class 
DiskManagerImpl
	implements DiskManagerHelper 
{  
	private boolean	used	= false;
	
	private boolean started = false;
	private AESemaphore	started_sem	= new AESemaphore( "DiskManager::started" );
	private boolean	starting;
	private boolean	stopping;
	
	
	private int state_set_via_method;
	private String errorMessage = "";

	private int pieceLength;
	private int lastPieceLength;

	private int 		nbPieces;
	private long 		totalLength;
	private int 		percentDone;
	private long 		allocated;
	private long 		remaining;

    
	private	TOTorrent		torrent;


	private DMReader				reader;
	private DMWriterAndChecker		writer_and_checker;
	
	private RDResumeHandler			resume_handler;
	private DMPiecePicker			piece_picker;
	private DiskManagerPieceMapper	piece_mapper;
	
	
	
	private DiskManagerPieceImpl[]	pieces;
	private PieceList[] 			pieceMap;

	private DiskManagerFileInfoImpl[] 	files;
	
    private DownloadManager 			download_manager;

	private boolean alreadyMoved = false;

	private boolean				skipped_file_set_changed;
	private long				skipped_file_set_size;
	private long				skipped_but_downloaded;
	
	
		// DiskManager listeners
	
	private static final int LDT_STATECHANGED			= 1;
	private static final int LDT_PRIOCHANGED			= 2;
	private static final int LDT_PIECE_DONE_CHANGED		= 3;
	private static final int LDT_ACCESS_MODE_CHANGED	= 4;
	
	private static ListenerManager	listeners_aggregator 	= ListenerManager.createAsyncManager(
			"DiskM:ListenAggregatorDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DiskManagerListener	listener = (DiskManagerListener)_listener;
					
					if (type == LDT_STATECHANGED){
						
						int params[] = (int[])value;
						
  						listener.stateChanged(params[0], params[1]);
  						
					}else if (type == LDT_PRIOCHANGED) {
						
					    listener.filePriorityChanged((DiskManagerFileInfo)value);
					    
					}else if (type == LDT_PIECE_DONE_CHANGED) {
						
					    listener.pieceDoneChanged((DiskManagerPiece)value);
					    
					}else if (type == LDT_ACCESS_MODE_CHANGED) {
						
						Object[]	o = (Object[])value;
						
					    listener.fileAccessModeChanged( 
					    	(DiskManagerFileInfo)o[0],
					    	((Integer)o[1]).intValue(),
					    	((Integer)o[2]).intValue());
					}
				}
			});		
	
	private ListenerManager	listeners 	= ListenerManager.createManager(
			"DiskM:ListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		listener,
					int			type,
					Object		value )
				{
					listeners_aggregator.dispatch( listener, type, value );
				}
			});	
	
	private AEMonitor	start_stop_mon	= new AEMonitor( "DiskManager:startStop" );
	private AEMonitor	file_piece_mon		= new AEMonitor( "DiskManager:filePiece" );
	
	public 
	DiskManagerImpl(
		TOTorrent			_torrent, 
		DownloadManager 	_dmanager) 
	{
	    torrent 	= _torrent;
	    download_manager 	= _dmanager;
	 
	    pieces		= new DiskManagerPieceImpl[0];	// in case things go wrong later
	    
	    setState( INITIALIZING );
	    
	    percentDone = 0;
	    
		if ( torrent == null ){
			
			setState( FAULTY );
			
			return;
		}
   
		LocaleUtilDecoder	locale_decoder = null;
		
		try{
			locale_decoder = LocaleUtil.getSingleton().getTorrentEncoding( torrent );
			
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace( e );
			
			this.errorMessage = TorrentUtils.exceptionToText(e) + " (Constructor)";
			
			setState( FAULTY );
			
			return;
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			this.errorMessage = e.getMessage() + " (Constructor)";
			
			setState( FAULTY );
			
			return;
		}
		
		piece_mapper	= new DiskManagerPieceMapper( this );

			//build something to hold the filenames/sizes
		
		TOTorrentFile[] torrent_files = torrent.getFiles();

		if ( torrent.isSimpleTorrent()){
			 								
			piece_mapper.buildFileLookupTables( torrent_files[0], download_manager.getAbsoluteSaveLocation().getName());

		}else{

			piece_mapper.buildFileLookupTables( torrent_files, locale_decoder );
		}
		
		if ( getState() == FAULTY){
			
			return;
		}

		totalLength	= piece_mapper.getTotalLength();
		
		remaining 	= totalLength;

		nbPieces 	= torrent.getNumberOfPieces();
		
		pieceLength		 	= (int)torrent.getPieceLength();
		
		lastPieceLength  	= piece_mapper.getLastPieceLength();
		
		pieces		= new DiskManagerPieceImpl[ nbPieces ];
		
		for (int i=0;i<pieces.length;i++){
			
			pieces[i] = new DiskManagerPieceImpl( this, i );
		}
		
		reader 				= DMAccessFactory.createReader(this);
		
		writer_and_checker 	= DMAccessFactory.createWriterAndChecker(this);
		
		resume_handler		= new RDResumeHandler( this, writer_and_checker );
	
		piece_picker		= DMPiecePickerFactory.create( this );
	}

	public void 
	start() 
	{		
		try{
			start_stop_mon.enter();
	
			if ( used ){
				
				Debug.out( "DiskManager reuse not supported!!!!" );
			}
			
			used	= true;
			
			if ( getState() == FAULTY ){
				
				Debug.out( "starting a faulty disk manager");
				
				return;
			}
			
			started 	= true;
			starting	= true;
			
		    Thread init = 
		    	new AEThread("DiskManager:start") 
				{
					public void 
					runSupport() 
					{
						try{
							startSupport();
							
						}catch( Throwable e ){
							
							errorMessage = Debug.getNestedExceptionMessage(e) + " (start)";
							
							setState( FAULTY );

						}finally{
														
							started_sem.release();
						}
						
						boolean	stop_required;
						
						try{
							start_stop_mon.enter();
						
							stop_required = DiskManagerImpl.this.getState() == DiskManager.FAULTY || stopping;
							
							starting	= false;
							
						}finally{
							
							start_stop_mon.exit();
						}
						
						if ( stop_required ){
						
							DiskManagerImpl.this.stop();
						}
					}
				};
				
			init.setPriority(Thread.MIN_PRIORITY);
			
			init.start();
			
		}finally{
			
			start_stop_mon.exit();
		}
	}

	private void 
	startSupport() 
	{		
			//if the data file is already in the completed files dir, we want to use it
		
		boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);
		
		String moveToDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
   
		if ( moveWhenDone && moveToDir.length() > 0 && download_manager.isPersistent()){
		
				//if the data file already resides in the completed files dir
					
			if ( filesExist( moveToDir )){
				
				alreadyMoved = true;
		
				download_manager.setTorrentSaveDir( moveToDir );
			}
		}

		writer_and_checker.start();
		
		reader.start();
		
			//allocate / check every file

		int newFiles = allocateFiles();
      
		if ( getState() == FAULTY ){
			
				// bail out if broken in the meantime
				// state will be "faulty" if the allocation process is interrupted by a stop
			
			return;
		}
    
        pieceMap = piece_mapper.constructPieceMap();

		constructFilesPieces();
		
		piece_picker.start();
		
		if ( getState() == FAULTY  ){
			
				// bail out if broken in the meantime

			return;
		}

		resume_handler.start();
		  
		if ( newFiles == 0 ){
			
			resume_handler.checkAllPieces(false);
			
		}else if ( newFiles != files.length ){
			
				//	if not a fresh torrent, check pieces ignoring fast resume data
			
			resume_handler.checkAllPieces(true);
		}
		
		if ( getState() == FAULTY  ){
			
			return;
		}
			// in all the above cases we want to continue to here if we have been "stopped" as
			// other components require that we end up either FAULTY or READY
		
			//3.Change State   
		
		setState( READY );
	}

	public void 
	stop() 
	{	
		try{
			start_stop_mon.enter();
		
			if ( !started ){
			
				return;
			}
		
				// we need to be careful if we're still starting up as this may be
				// a re-entrant "stop" caused by a faulty state being reported during
				// startup. Defer the actual stop until starting is complete
			
			if ( starting ){
				
				stopping	= true;

					// we can however safely stop things at this point - this is important
					// to interrupt an alloc/recheck process that might be holding up the start
					// operation
				
			   	writer_and_checker.stop();
		    					
				resume_handler.stop();
								
				return;
			}
			
			started		= false;
			
			stopping	= false;
			
		}finally{
			
			start_stop_mon.exit();
		}
		
		started_sem.reserve();
		
    	writer_and_checker.stop();
    	
		reader.stop();
		
		resume_handler.stop();
		
		piece_picker.stop();
		
		if ( files != null ){
			
			for (int i = 0; i < files.length; i++){
				
				try{
					if (files[i] != null) {
						
						files[i].getCacheFile().close();
					}
				}catch (Exception e){
					
					Debug.printStackTrace( e );
				}
			}
		
			storeFileDownloaded( download_manager, files );
		}
		
			// can't be used after a stop so we might as well clear down the listeners
		
		listeners.clear();
	}
	
	public boolean
	filesExist()
	{
		return( filesExist( download_manager.getAbsoluteSaveLocation().getParent()));
	}

	protected boolean 
	filesExist(
		String	root_dir )
	{
		if ( !torrent.isSimpleTorrent()){
			
			root_dir += File.separator + download_manager.getAbsoluteSaveLocation().getName();
		}
		
		if ( !root_dir.endsWith( File.separator )){
			
			root_dir	+= File.separator;
		}
		
		// System.out.println( "root dir = " + root_dir_file );
		
		List btFileList	= piece_mapper.getFileList();
		
		String[]	storage_types = getStorageTypes();
		
		for (int i = 0; i < btFileList.size(); i++) {
				//get the BtFile
			DiskManagerPieceMapper.fileInfo pm_info = (DiskManagerPieceMapper.fileInfo)btFileList.get(i);
			
			File	relative_file = pm_info.getDataFile();
			
			long target_length = pm_info.getLength();
			
				// use the cache file to ascertain length in case the caching/writing algorithm
				// fiddles with the real length
				// Unfortunately we may be called here BEFORE the disk manager has been 
				// started and hence BEFORE the file info has been setup...
				// Maybe one day we could allocate the file info earlier. However, if we do
				// this then we'll need to handle the "already moved" stuff too...
			
			DiskManagerFileInfoImpl	file_info = pm_info.getFileInfo();
			
			boolean	close_it	= false;
			
			try{
				if ( file_info == null ){
					
					boolean linear = storage_types[i].equals("L");
					
					file_info = new DiskManagerFileInfoImpl( 
										this, 
										new File( root_dir + relative_file.toString()), 
										i,
										pm_info.getTorrentFile(),
										linear );
	
					close_it	= true;					
				}
				
				try{
					CacheFile	cache_file	= file_info.getCacheFile();
					File		data_file	= file_info.getFile(true);

					if ( !cache_file.exists()){
						
							// look for something sensible to report
						
						  File current = data_file;
						  
						  while( !current.exists()){
							
						  	File	parent = current.getParentFile();
						  	
						  	if ( parent == null ){
						  		
						  		break;
						  		
						  	}else if ( !parent.exists()){
						  		
						  		current	= parent;
						  		
						  	}else{
						  		
						  		if ( parent.isDirectory()){
						  			
						  			errorMessage = current.toString() + " not found.";
						  			
						  		}else{
						  			
						  			errorMessage = parent.toString() + " is not a directory.";
						  		}
						  		
						  		return( false );
						  	}
						  }
						  
						  errorMessage = data_file.toString() + " not found.";
						  
						  return false;
					}
					
						// only test for too big as if incremental creation selected
						// then too small is OK
					
					long	existing_length = file_info.getCacheFile().getLength();
					
					if ( existing_length > target_length ){
						
						if ( COConfigurationManager.getBooleanParameter("File.truncate.if.too.large")){
							
							file_info.setAccessMode( DiskManagerFileInfo.WRITE );

							file_info.getCacheFile().setLength( target_length );
							
							Debug.out( "Existing data file length too large [" +existing_length+ ">" +target_length+ "]: " + data_file.getAbsolutePath() + ", truncating" );

						}else{

							errorMessage = "Existing data file length too large [" +existing_length+ ">" +target_length+ "]: " + data_file.getAbsolutePath();
					  
							return false;
						}
					}
				}finally{
					
					if ( close_it ){
						
						file_info.getCacheFile().close();
					}
				}
			}catch( Throwable e ){
			
				errorMessage = Debug.getNestedExceptionMessage(e) + " (filesExist:" + relative_file.toString() + ")";
				
				return( false );
			}
		}
		
		return true;
	}
	
	private int 
	allocateFiles() 
	{
		DiskManagerFileInfoImpl[] allocated_files = new DiskManagerFileInfoImpl[piece_mapper.getFileList().size()];
	      
		try{
			setState( ALLOCATING );
			
			allocated = 0;
			
			int numNewFiles = 0;
			
			List btFileList	= piece_mapper.getFileList();
		
			String	root_dir = download_manager.getAbsoluteSaveLocation().getParent();
			
			if ( !torrent.isSimpleTorrent()){
				
				root_dir += File.separator + download_manager.getAbsoluteSaveLocation().getName();
			}
			
			root_dir	+= File.separator;	
			
			String[]	storage_types = getStorageTypes();
	
			for ( int i=0;i<btFileList.size();i++ ){
				
				final DiskManagerPieceMapper.fileInfo pm_info = (DiskManagerPieceMapper.fileInfo)btFileList.get(i);
						
				final long target_length = pm_info.getLength();
	
				File relative_data_file = pm_info.getDataFile();
									
				DiskManagerFileInfoImpl fileInfo;
				
				try{
					boolean linear = storage_types[i].equals("L");
	
					fileInfo = new DiskManagerFileInfoImpl( 
									this, 
									new File( root_dir + relative_data_file.toString()), 
									i,
									pm_info.getTorrentFile(),
									linear );
					
					allocated_files[i] = fileInfo;
		
					pm_info.setFileInfo( fileInfo );
					
				}catch ( CacheFileManagerException e ){
					
					this.errorMessage = Debug.getNestedExceptionMessage(e) + " (allocateFiles:" + relative_data_file.toString() + ")";
					
					setState( FAULTY );
	        
					return( -1 );
				}
				
				CacheFile	cache_file 		= fileInfo.getCacheFile();
				File		data_file		= fileInfo.getFile(true);
				String		data_file_name 	= data_file.getName();
				
				int separator = data_file_name.lastIndexOf(".");
				
				if ( separator == -1 ){
					
					separator = 0;
				}
				
				fileInfo.setExtension(data_file_name.substring(separator));
				
					//Added for Feature Request
					//[ 807483 ] Prioritize .nfo files in new torrents
					//Implemented a more general way of dealing with it.
				
				String extensions = COConfigurationManager.getStringParameter("priorityExtensions","");
				
				if(!extensions.equals("")) {
					boolean bIgnoreCase = COConfigurationManager.getBooleanParameter("priorityExtensionsIgnoreCase");
					StringTokenizer st = new StringTokenizer(extensions,";");
					while(st.hasMoreTokens()) {
						String extension = st.nextToken();
						extension = extension.trim();
						if(!extension.startsWith("."))
							extension = "." + extension;
						boolean bHighPriority = (bIgnoreCase) ? 
											  fileInfo.getExtension().equalsIgnoreCase(extension) : 
											  fileInfo.getExtension().equals(extension);
						if (bHighPriority)
							fileInfo.setPriority(true);
					}
				}
				
				fileInfo.setLength(target_length);
				
				fileInfo.setDownloaded(0);
				
				if ( cache_file.exists() ){
					
					try {
	
				  		//make sure the existing file length isn't too large
				  	
						long	existing_length = fileInfo.getCacheFile().getLength();
				  	
						if(  existing_length > target_length ){
						
							if ( COConfigurationManager.getBooleanParameter("File.truncate.if.too.large")){
							
							  	fileInfo.setAccessMode( DiskManagerFileInfo.WRITE );
		
							  	cache_file.setLength( target_length );
							
								Debug.out( "Existing data file length too large [" +existing_length+ ">" +target_length+ "]: " +data_file.getAbsolutePath() + ", truncating" );
		
							}else{
							
								this.errorMessage = "Existing data file length too large [" +existing_length+ ">" +target_length+ "]: " + data_file.getAbsolutePath();
			          
								setState( FAULTY );
	            
								return( -1 );
							}
						}
				  	
						fileInfo.setAccessMode( DiskManagerFileInfo.READ );
				  	
					}catch (CacheFileManagerException e) {
				  	
						this.errorMessage = Debug.getNestedExceptionMessage(e) + 
												" (allocateFiles existing:" + data_file.getAbsolutePath() + ")";
						setState( FAULTY );
				 
						return( -1 );
					}
				  
					allocated += target_length;
	        
				}else{  //we need to allocate it
	        
						//make sure it hasn't previously been allocated
					
					if ( download_manager.isDataAlreadyAllocated() ){
	        	
						this.errorMessage = "Data file missing: " + data_file.getAbsolutePath();
	          
						setState( FAULTY );
	          
						return( -1 );
					}
	       
					try{	          	          
						fileInfo.setAccessMode( DiskManagerFileInfo.WRITE );
		          
						if( COConfigurationManager.getBooleanParameter("Enable incremental file creation") ) {
		          	
								//	do incremental stuff
		          	
							fileInfo.getCacheFile().setLength( 0 );
		            
						}else { 
							
								//fully allocate
							
							if( COConfigurationManager.getBooleanParameter("Zero New") ) {  //zero fill
								
								if ( !writer_and_checker.zeroFile( fileInfo, target_length )) {
		                
									try{
											// failed to zero it, delete it so it gets done next start
																			
										fileInfo.getCacheFile().close();
										
										fileInfo.getCacheFile().delete();
																			
									}catch( Throwable e ){
										
									}
									
									setState( FAULTY );
		                
									return( -1 );
								}
							}else{ 
								
									//reserve the full file size with the OS file system
		            	
								fileInfo.getCacheFile().setLength( target_length );
		              
								allocated += target_length;
							}
						}
					}catch ( Exception e ) {
						
						this.errorMessage = Debug.getNestedExceptionMessage(e)
									+ " (allocateFiles new:" + data_file.toString() + ")";
		          
						setState( FAULTY );
		          
						return( -1 );
					}
		        
					numNewFiles++;
				}
			}
	    
				// make sure that "files" doens't become visible to the rest of the world until all
				// entries have been populated
			
			files	= allocated_files;
			
			loadFilePriorities();
	    
			download_manager.setDataAlreadyAllocated( true );
	    
			return( numNewFiles );
			
		}finally{
			
				// if we failed to do the allocation make sure we close all the files that
				// we might have opened
			
			if ( files == null ){
				
				for (int i=0;i<allocated_files.length;i++){
					
					if ( allocated_files[i] != null ){
					
						try{
							allocated_files[i].getCacheFile().close();
							
						}catch( Throwable e ){
						}
					}
				}
			}
		}
	}	
	
  
	public void 
	enqueueReadRequest( 
		DiskManagerReadRequest request, 
		DiskManagerReadRequestListener listener ) 
	{
		reader.enqueueReadRequest( request, listener );
	}


	public int 
	getNumberOfPieces() 
	{
		return nbPieces;
	}

	public int 
	getPercentDone() 
	{
		return percentDone;
	}
	
	public void
	setPercentDone(
		int			num )
	{
		percentDone	= num;
	}
	
	public long 
	getRemaining() {
		return remaining;
	}
	
	public long 
	getRemainingExcludingDND() 
	{
		if ( skipped_file_set_changed ){
			
			DiskManagerFileInfoImpl[]	current_files = files;
			
			if ( current_files != null ){
				
				skipped_file_set_changed	= false;
				
				try{
					file_piece_mon.enter();
					
					skipped_file_set_size	= 0;
					skipped_but_downloaded	= 0;
					
					for (int i=0;i<current_files.length;i++){
						
						DiskManagerFileInfoImpl	file = current_files[i];
						
						if ( file.isSkipped()){
							
							skipped_file_set_size	+= file.getLength();
							skipped_but_downloaded	+= file.getDownloaded();
						}
					}
				}finally{
					
					file_piece_mon.exit();
				}
			}
		}
		
		long rem = ( remaining - ( skipped_file_set_size - skipped_but_downloaded ));
		
		if ( rem < 0 ){
			
			rem	= 0;
		}
		
		return( rem );
	}
	
	public long
	getAllocated()
	{
		return( allocated );
	}
	
	public void
	setAllocated(
		long		num )
	{
		allocated	= num;
	}
	
		// called when status has CHANGED and should only be called by DiskManagerPieceImpl
	
	protected void
	setPieceDone(
		DiskManagerPieceImpl	piece,
		boolean					done )
	{
		int	piece_number = piece.getPieceNumber();
		
		int	piece_length = piece.getLength();
		
		PieceList piece_list = pieceMap[piece_number];

		try{
			file_piece_mon.enter();					
			
			if ( piece.getDone() != done ){
				
				piece.setDoneSupport( done );
	
				if ( done ){
	
					remaining -= piece_length;
					
				}else{
					
					remaining += piece_length;
				}
									
				for (int i = 0; i < piece_list.size(); i++) {
								
					PieceMapEntry piece_map_entry = piece_list.get(i);
								
					DiskManagerFileInfoImpl	this_file = piece_map_entry.getFile();
						
					long file_length = this_file.getLength();
					
					long file_done = this_file.getDownloaded();
						
					long file_done_before = file_done;
					
					if ( done ){
						
						file_done += piece_map_entry.getLength();
						
					}else{
						
						file_done -= piece_map_entry.getLength();
					}
					
					if ( file_done < 0 ){
						
						Debug.out( "piece map entry length negative" );
						
						file_done = 0;
						
					}else if ( file_done > file_length ){
						
						Debug.out( "piece map entry length too large" );
						
						file_done = file_length;
					}
					
					if ( this_file.isSkipped()){
						
						skipped_but_downloaded += ( file_done - file_done_before );
					}
					
					this_file.setDownloaded( file_done );
						
						// change file modes based on whether or not the file is complete or not
					
					if (	file_done == file_length &&
							this_file.getAccessMode() == DiskManagerFileInfo.WRITE){
												
						try{
							this_file.setAccessMode( DiskManagerFileInfo.READ );
									
						}catch (Exception e) {
									
							Debug.printStackTrace( e );
						}
						
						// note - we don't set the access mode to write if incomplete as we may 
						// be rechecking a file and during this process the "file_done" amount
						// will not be file_length until the end. If the file is read-only then
						// changing to write will cause trouble!
					}
				}
			}
		}finally{
				
			file_piece_mon.exit();
		}			
		
		listeners.dispatch(LDT_PIECE_DONE_CHANGED, piece);
	}
	
	protected void
	fileAccessModeChanged(
		DiskManagerFileInfoImpl		file,
		int							old_mode,
		int							new_mode )
	{
		listeners.dispatch( 
			LDT_ACCESS_MODE_CHANGED,
			new Object[]{ file, new Integer(old_mode), new Integer(new_mode)});
	}
	
	public DiskManagerPiece[]
	getPieces()
	{
		return( pieces );
	}

	public int getPieceLength() {
		return pieceLength;
	}

	public long getTotalLength() {
		return totalLength;
	}

	public int getLastPieceLength() {
		return lastPieceLength;
	}

	public int getState() {
		return state_set_via_method;
	}

	public void
	setState(
		int		_state ) 
	{
			// we never move from a faulty state
		
		if ( state_set_via_method == FAULTY ){
			
			if ( _state != FAULTY ){
				
				Debug.out( "DiskManager: attempt to move from faulty state to " + _state );
			}
			
			return;
		}
		
		if ( state_set_via_method != _state ){
			
			int params[] = {state_set_via_method, _state};
		  
			state_set_via_method = _state;
			
			listeners.dispatch( LDT_STATECHANGED, params);
		}
	}

	
	public DiskManagerFileInfo[] 
	getFiles() 
	{
		return files;
	}


	private void 
	constructFilesPieces() 
	{
		for (int i = 0; i < pieceMap.length; i++) {
			PieceList pieceList = pieceMap[i];
			//for each piece

			for (int j = 0; j < pieceList.size(); j++) {
				//get the piece and the file 
				DiskManagerFileInfoImpl fileInfo = (pieceList.get(j)).getFile();
				if (fileInfo.getFirstPieceNumber() == -1)
					fileInfo.setFirstPieceNumber(i);
				fileInfo.setNbPieces(fileInfo.getNbPieces() + 1);
			}
		}
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void
	setFailed(
		final String		reason )
	{
			/**
			 * need to run this on a separate thread to avoid deadlock with the stopping
			 * process - setFailed tends to be called from within the read/write activities
			 * and stopping these requires this.
			 */
		
    	new AEThread("DiskManager:setFailed") 
		{
			public void 
			runSupport() 
			{
				errorMessage	= reason;
				
				LGLogger.logUnrepeatableAlert( LGLogger.AT_ERROR, errorMessage );
				

				setState( DiskManager.FAULTY );
									
				DiskManagerImpl.this.stop();
			}
		}.start();

	}

	
	public PieceList
	getPieceList(
		int		piece_number )
	{
		return( pieceMap[piece_number] );
	}
	
	public byte[]
	getPieceHash(
		int	piece_number )
	
		throws TOTorrentException
	{
		return( torrent.getPieces()[ piece_number ]);
	}
	
	public DiskManagerReadRequest
	createReadRequest(
		int pieceNumber,
		int offset,
		int length )
	{
		return( reader.createRequest( pieceNumber, offset, length ));
	}
	
  
	public void 
	enqueueCompleteRecheckRequest(
		final DiskManagerCheckRequestListener 	listener,
		final Object							user_data ) 
	{
	  	writer_and_checker.enqueueCompleteRecheckRequest( listener, user_data );
	}

	public void 
	enqueueCheckRequest(
		int 							pieceNumber,
		DiskManagerCheckRequestListener listener,
		Object							user_data ) 
	{
	  	writer_and_checker.enqueueCheckRequest( pieceNumber, listener, user_data );
	}
	  
	public boolean isChecking() 
	{
	  return ( writer_and_checker.isChecking());
	}
  
	public DirectByteBuffer 
	readBlock(
		int pieceNumber, 
		int offset, 
		int length )
	{
		return( reader.readBlock( pieceNumber, offset, length ));
	}
	
	public void 
	enqueueWriteRequest(
		int 							pieceNumber, 
		int 							offset, 
		DirectByteBuffer 				data,
		Object 							user_data,
		DiskManagerWriteRequestListener	listener )
	{
		writer_and_checker.writeBlock( pieceNumber, offset, data, user_data, listener );
	}
	
	public boolean 
	checkBlockConsistency(
		int pieceNumber, 
		int offset, 
		DirectByteBuffer data) 
	{
		return( writer_and_checker.checkBlock( pieceNumber, offset, data ));
	}
	
	public boolean 
	checkBlockConsistency(
		int pieceNumber, 
		int offset, 
		int length) 
	{
		return( writer_and_checker.checkBlock( pieceNumber, offset, length ));
	}
	
	public void 
	dumpResumeDataToDisk(
		boolean savePartialPieces, 
		boolean force_recheck )
	
		throws Exception
	{			
		resume_handler.dumpResumeDataToDisk( savePartialPieces, force_recheck );
	}
		
  /**
   * Moves files to the CompletedFiles directory.
   * Returns a string path to the new torrent file.
   */
	
  public void 
  downloadEnded() 
  {
    try{
    	start_stop_mon.enter();
    	
	    String fullPath;
	    
	    String subPath;
	    
	    String rPath = download_manager.getAbsoluteSaveLocation().getParent();
	    
	    File destDir;
	    
	    //String returnName = "";
	    
	    	// don't move non-persistent files as these aren't managed by us
	    
	    if ( !download_manager.isPersistent()){
	    	
	    	return;
	    }
	    
	    //make sure the torrent hasn't already been moved
	
	    	
	  	if ( alreadyMoved ){
	  		
	  		return;
	  	}
	    	
	  	alreadyMoved = true;	
	 
	    boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);
	    
	    if (!moveWhenDone){
	    	
	    	return;
	    }
	    
	    String moveToDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
	    
	    if (moveToDir.length() == 0){
	    	
	    	return;
	    }
	
	    try{
	
	      boolean moveOnlyInDefault = COConfigurationManager.getBooleanParameter("Move Only When In Default Save Dir");
	      
	      if (moveOnlyInDefault) {
	      	
	        String defSaveDir = COConfigurationManager.getStringParameter("Default save path");

	        if (!rPath.equals(defSaveDir)){
	        	
	          LGLogger.log(LGLogger.INFORMATION, "Not moving-on-complete since data is not within default save dir");
	          
	          return;
	        }
	      }
	      
	      	// first of all check that no destination files already exist
	      
	      File[]	new_files 	= new File[files.length];
	      File[]	old_files	= new File[files.length];
	      
	      for (int i=0; i < files.length; i++) {
	          	    	  
	          File old_file = files[i].getFile(false);
	          
	          old_files[i]	= old_file;
	          
	          	//get old file's parent path
	          
	          fullPath = old_file.getParent();
	          
	           	//compute the file's sub-path off from the default save path
	          
	          subPath = fullPath.substring(fullPath.indexOf(rPath) + rPath.length());
	    
	          	//create the destination dir
	          
	          if ( subPath.startsWith( File.separator )){
	          	
	          	subPath = subPath.substring(1);
	          }
	          
	          destDir = new File(moveToDir, subPath);
	     
	          	//create the destination file pointer
	          
	          File newFile = new File(destDir, old_file.getName());
	
	          new_files[i]	= newFile;

	    	  if ( !files[i].isLinked()){
		             
		          if ( newFile.exists()){
		          	
		            String msg = "" + old_file.getName() + " already exists in MoveTo destination dir";
		            
		            LGLogger.log(LGLogger.ERROR,msg);
		            
		            LGLogger.logUnrepeatableAlertUsingResource( 
		            		LGLogger.AT_ERROR, "DiskManager.alert.movefileexists", 
		            		new String[]{ old_file.getName() } );
		            
		            Debug.out(msg);
		            
		            return;
		            
		          }else{
		        	  
		    		  destDir.mkdirs();
  
		          }
	    	  }
	      }
	      
	      for (int i=0; i < files.length; i++){
	      		 	          
	          File new_file = new_files[i];
	          	          
	          try{
	          	
	          	files[i].moveFile( new_file );
	           	
	            files[i].setAccessMode(DiskManagerFileInfo.READ);
	            
	          }catch( CacheFileManagerException e ){
	          	
	            String msg = "Failed to move " + old_files[i].toString() + " to destination dir";
	            
	            LGLogger.log(LGLogger.ERROR,msg);
	            
	            LGLogger.logUnrepeatableAlertUsingResource( 
	            		LGLogger.AT_ERROR, "DiskManager.alert.movefilefails", 
	            		new String[]{ old_files[i].toString(),
	            		Debug.getNestedExceptionMessage(e)});
	
	            Debug.out(msg);
	            
	            	// try some recovery by moving any moved files back...
	            
	            for (int j=0;j<i;j++){
	            	
	            	try{
	            		files[j].moveFile( old_files[j]);
	
	            		files[j].setAccessMode(DiskManagerFileInfo.READ);
	         		
	            	}catch( CacheFileManagerException f ){
	              
	            		LGLogger.logUnrepeatableAlertUsingResource( 
	                    		LGLogger.AT_ERROR, "DiskManager.alert.movefilerecoveryfails", 
	                    		new String[]{ old_files[j].toString(),
	                    		Debug.getNestedExceptionMessage(f)} );
	           		
	            	}
	            }
	            
	            return;
	          }
	      }
	      
	      	//remove the old dir
	      
	      File tFile = download_manager.getAbsoluteSaveLocation();
	      
	      if (	tFile.isDirectory() && 
	      		!moveToDir.equals(rPath)){
	      	
	      		deleteDataFiles(torrent, tFile.getParent(), tFile.getName());
	      }
	        
	      download_manager.setTorrentSaveDir( moveToDir );
	      
	      	//move the torrent file as well
	      
	      boolean moveTorrent = COConfigurationManager.getBooleanParameter("Move Torrent When Done", true);
	      
	      if ( moveTorrent ){
	      	
	          String oldFullName = download_manager.getTorrentFileName();
	          
	          File oldTorrentFile = new File(oldFullName);
	          
	          String oldFileName = oldTorrentFile.getName();
	          
	          File newTorrentFile = new File(moveToDir, oldFileName);
	          
	          if (!newTorrentFile.equals(oldTorrentFile)){
	          	
	          	if ( TorrentUtils.move( oldTorrentFile, newTorrentFile )){
	            	            
	          		download_manager.setTorrentFileName(newTorrentFile.getCanonicalPath());
	          		
	          	}else{
	          
		            String msg = "Failed to move " + oldTorrentFile.toString() + " to " + newTorrentFile.toString();
		            
		            LGLogger.log(LGLogger.ERROR,msg);
		            
		            LGLogger.logUnrepeatableAlertUsingResource( 
		            		LGLogger.AT_ERROR, "DiskManager.alert.movefilefails", 
		            		new String[]{ 	oldTorrentFile.toString(),
		            						newTorrentFile.toString()});
		
		            Debug.out(msg);
	          	}
	          }
	      }
	    }catch( Exception e){
	    	
	    	Debug.printStackTrace( e ); 
	    }	    
    }finally{
    	
    	try{
            boolean resumeEnabled = COConfigurationManager.getBooleanParameter("Use Resume", true);
            
            	//update resume data
            
            if (resumeEnabled){
            	
            	try{
            		dumpResumeDataToDisk(true, false);
            		
            	}catch( Exception e ){
            		
            			// won't go wrong here due to cache write fails as these must have completed
            			// prior to the files being moved. Possible problems with torrent save but
            			// if this fails we can live with it (just means that on restart we'll do
            			// a recheck )
            		
            		Debug.out( "dumpResumeDataToDisk fails" );
            	}
            }
    	}catch( Throwable e ){
    		
    		Debug.printStackTrace(e);
    	}
    	
    	start_stop_mon.exit();
    }
  }
  
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}


	public void
	addListener(
		DiskManagerListener	l )
	{
		listeners.addListener( l );

		int params[] = {getState(), getState()};
  		
		listeners.dispatch( l, LDT_STATECHANGED, params);
	}
  
	public void
	removeListener(
		DiskManagerListener	l )
	{
		listeners.removeListener(l);
	}

		  /** Deletes all data files associated with torrent.
		   * Currently, deletes all files, then tries to delete the path recursively
		   * if the paths are empty.  An unexpected result may be that a empty
		   * directory that the user created will be removed.
		   *
		   * TODO: only remove empty directories that are created for the torrent
		   */
  
	public static void 
	deleteDataFiles(
		TOTorrent 	torrent, 
		String		torrent_save_dir,		// enclosing dir, not for deletion 
		String		torrent_save_file ) 	// file or dir for torrent
	{	
		if (torrent == null || torrent_save_file == null ){
	  
			return;
		}
	  	  
		try{
			if (torrent.isSimpleTorrent()){

				FileUtil.deleteWithRecycle(new File( torrent_save_dir, torrent_save_file ));

			}else{

                PlatformManager mgr = PlatformManagerFactory.getPlatformManager();
                if( Constants.isOSX &&
                      torrent_save_file.length() > 0 &&
                      COConfigurationManager.getBooleanParameter("Move Deleted Data To Recycle Bin" ) &&
                      mgr.hasCapability(PlatformManagerCapabilities.RecoverableFileDelete) ) {

                    try
                    {
                        mgr.performRecoverableFileDelete(torrent_save_dir + File.separatorChar + torrent_save_file + File.separatorChar);
                    }
                    catch(PlatformManagerException ex)
                    {
                        deleteDataFileContents( torrent, torrent_save_dir, torrent_save_file );
                    }
                }
                else{
                    deleteDataFileContents(torrent, torrent_save_dir, torrent_save_file);
                }

            }
		}catch( Throwable e ){
		
			Debug.printStackTrace( e );
		}
	}

    private static void deleteDataFileContents(TOTorrent torrent, String torrent_save_dir, String torrent_save_file)
            throws TOTorrentException, UnsupportedEncodingException, LocaleUtilEncodingException
    {
        LocaleUtilDecoder locale_decoder = LocaleUtil.getSingleton().getTorrentEncoding( torrent );

        TOTorrentFile[] files = torrent.getFiles();

        // delete all files, then empty directories

        for (int i=0;i<files.length;i++){

            byte[][]path_comps = files[i].getPathComponents();

            String	path_str = torrent_save_dir + File.separator + torrent_save_file + File.separator;

            for (int j=0;j<path_comps.length;j++){

                try{

                    String comp = locale_decoder.decodeString( path_comps[j] );

                    comp = FileUtil.convertOSSpecificChars( comp );

                    path_str += (j==0?"":File.separator) + comp;

                }catch( UnsupportedEncodingException e ){

                    System.out.println( "file - unsupported encoding!!!!");
                }
            }

            File file = new File(path_str);

            if (file.exists() && !file.isDirectory()){

                try{
                    FileUtil.deleteWithRecycle( file );

                }catch (Exception e){

                    Debug.out(e.toString());
                }
            }
        }

        FileUtil.recursiveEmptyDirDelete(new File( torrent_save_dir, torrent_save_file ));
    }

    protected void
    skippedFileSetChanged(
    	DiskManagerFileInfo	file )
    {
    	skipped_file_set_changed	= true;
	    listeners.dispatch(LDT_PRIOCHANGED, file);
    }

	protected void 
	priorityChanged(
		DiskManagerFileInfo	file ) 
	{
	    listeners.dispatch(LDT_PRIOCHANGED, file);
    }
  
  private void 
  loadFilePriorities() 
  {
	  loadFilePriorities( download_manager, files );
  }
  
  private static void 
  loadFilePriorities(
	DownloadManager			download_manager,
	DiskManagerFileInfo[]	files )
  {
  	//  TODO: remove this try/catch.  should only be needed for those upgrading from previous snapshot
    try {
    	if ( files == null ) return;
    	List file_priorities = (List)download_manager.getData( "file_priorities" );
    	if ( file_priorities == null ) return;
    	for (int i=0; i < files.length; i++) {
    		DiskManagerFileInfo file = files[i];
    		if (file == null) return;
    		int priority = ((Long)file_priorities.get( i )).intValue();
    		if ( priority == 0 ) file.setSkipped( true );
    		else if (priority == 1) file.setPriority( true );
    	}
    }
    catch (Throwable t) {Debug.printStackTrace( t );}
  }
  
  public void 
  storeFilePriorities() 
  {
	  storeFilePriorities( download_manager, files );
  }
  
  private static void 
  storeFilePriorities(
	DownloadManager			download_manager,
	DiskManagerFileInfo[]	files )
  {
    if ( files == null ) return;
    List file_priorities = new ArrayList();
    for (int i=0; i < files.length; i++) {
      DiskManagerFileInfo file = files[i];
      if (file == null) return;
      boolean skipped = file.isSkipped();
      boolean priority = file.isPriority();
      int value = -1;
      if ( skipped ) value = 0;
      else if ( priority ) value = 1;
      file_priorities.add( i, new Long(value));            
    }
    download_manager.setData( "file_priorities", file_priorities );
  }
  
  protected static void
  storeFileDownloaded(
	DownloadManager			download_manager,
	DiskManagerFileInfo[]	files )
  {
	  DownloadManagerState	state = download_manager.getDownloadState();
	  
	  Map	details = new HashMap();
	  
	  List	downloaded = new ArrayList();
	  
	  details.put( "downloaded", downloaded );
	  
	  for (int i=0;i<files.length;i++){
		  
		  downloaded.add( new Long( files[i].getDownloaded()));
	  }
	  
	  state.setMapAttribute( DownloadManagerState.AT_FILE_DOWNLOADED, details );
	  
	  state.save();
  }
  
  protected static void
  loadFileDownloaded(
	DownloadManager				download_manager,
	DiskManagerFileInfoHelper[]	files )
  {
	  DownloadManagerState	state = download_manager.getDownloadState();

	  Map	details = state.getMapAttribute( DownloadManagerState.AT_FILE_DOWNLOADED );
	  
	  if ( details == null ){
		  
		  return;
	  }  
		
	  List	downloaded = (List)details.get( "downloaded" );
	  
	  if ( downloaded == null ){
		  
		  return;
	  }
		
	  try{
		  for (int i=0;i<files.length;i++){
	
			  files[i].setDownloaded(((Long)downloaded.get(i)).longValue());
		  }
	 }catch( Throwable e ){
		 
		 Debug.printStackTrace(e);
	 }
	
  }
  
  public DownloadManager getDownloadManager() {
    return download_manager;
  }
    
  	public void 
	computePriorityIndicator()
  	{
  		piece_picker.computePriorityIndicator();
  	}
  	
	public int 
	getPieceNumberToDownload(
		boolean[] _piecesRarest)
	{
		return( piece_picker.getPiecenumberToDownload( _piecesRarest ));
	}
	
	public boolean hasDownloadablePiece() {
	    return piece_picker.hasDownloadablePiece();
	}
	
	protected String[]
	getStorageTypes()                 
	{
		return( getStorageTypes( download_manager ));
	}
	
	protected static String[]
	getStorageTypes(
		DownloadManager		download_manager )
	{
		DownloadManagerState	state = download_manager.getDownloadState();

		String[]	types = state.getListAttribute( DownloadManagerState.AT_FILE_STORE_TYPES );
		
		if ( types.length == 0 ){
			
			types = new String[download_manager.getTorrent().getFiles().length];
			
			for (int i=0;i<types.length;i++){
				
				types[i] = "L";
			}	
		}
		
		return( types );
	}
	
	protected static boolean
	setFileLink(
		DownloadManager			download_manager,
		DiskManagerFileInfo[]	info,
		DiskManagerFileInfo		file_info,
		File					from_link,
		File					to_link )
	{
		File	existing_link = FMFileManagerFactory.getSingleton().getFileLink( to_link );
		
		if ( existing_link != to_link ){
			
			LGLogger.logRepeatableAlert( 
					LGLogger.AT_ERROR, "Attempt to link to existing link '" + existing_link.toString() + "'" );
			
			return( false );
		}
		
		for (int i=0;i<info.length;i++){
			
			if ( to_link.equals( info[i].getFile( true ))){
				
				LGLogger.logRepeatableAlert( 
						LGLogger.AT_ERROR, "Attempt to link to existing file '" + info[i].getFile( true ) + "'" );
				
				return( false );
			}
		}

		File	existing_file = file_info.getFile( true );
		
		if ( to_link.exists()){
   		 
			if ( to_link.equals( existing_file )){
		  
				return( true );
				
			}else if ( !existing_file.exists()){

					// using a new file, make sure we recheck
				
				download_manager.recheckFile( file_info );
				
			}else{
				
				if ( FileUtil.deleteWithRecycle( existing_file )){
		    		
						// new file, recheck
					
					download_manager.recheckFile( file_info );
					
				}else{
	    	
					LGLogger.logRepeatableAlert( 
							LGLogger.AT_ERROR, "Failed to delete '" + existing_file.toString() + "'" );
					
					return( false );
				}
			}
		}else{
			
			if ( existing_file.exists()){
  			  
				if ( !FileUtil.renameFile( existing_file, to_link )){
		  			  
					LGLogger.logRepeatableAlert( 
		    			LGLogger.AT_ERROR, "Failed to rename '" + existing_file.toString() + "'" );

					return( false );
				}
			}
		}

		DownloadManagerState	state = download_manager.getDownloadState();
		
		state.setFileLink( from_link, to_link );
		
		state.save();
		
		return( true );
	}
	
	public static DiskManagerFileInfo[]
	getFileInfoSkeleton(
		final DownloadManager		download_manager )
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent == null ){
			
			return( new DiskManagerFileInfo[0]);
		}
		
		String	root_dir = download_manager.getAbsoluteSaveLocation().getParent();
		
		if ( !torrent.isSimpleTorrent()){
			
			root_dir += File.separator + download_manager.getAbsoluteSaveLocation().getName();
		}
		
		root_dir	+= File.separator;	

		try{
		    LocaleUtilDecoder locale_decoder = LocaleUtil.getSingleton().getTorrentEncoding( torrent );
			
		    TOTorrentFile[]	torrent_files = torrent.getFiles();
			
		    long	piece_size 	= torrent.getPieceLength();
		    long	size_so_far	= 0;
		    
			final DiskManagerFileInfoHelper[]	res = new DiskManagerFileInfoHelper[ torrent_files.length ];
			
			for (int i=0;i<res.length;i++){
			
				final TOTorrentFile	torrent_file	= torrent_files[i];
				
				final int file_index = i;
				
				long	file_length = torrent_file.getLength();
				
				final int	first_piece = (int)(size_so_far/piece_size);
				
				size_so_far += file_length;
				
				final int	last_piece	= (int)((size_so_far-1)/piece_size);
								
				String	path_str = root_dir + File.separator;
				
					// for a simple torrent the target file can be changed 
				
				if ( torrent.isSimpleTorrent()){
					
					path_str = path_str + download_manager.getAbsoluteSaveLocation().getName();
					
				}else{
			        byte[][]path_comps = torrent_file.getPathComponents();
		
			        for (int j=0;j<path_comps.length;j++){
		
						String comp = locale_decoder.decodeString( path_comps[j] );
		
			            comp = FileUtil.convertOSSpecificChars( comp );
		
			            path_str += (j==0?"":File.separator) + comp;
			        }
				}
				
				final File		data_file	= new File( path_str );
	
				final String	data_name 	= data_file.getName();
							
				int separator = data_name.lastIndexOf(".");
				
				if (separator == -1){
					
					separator = 0;
				}
				
				final String	data_extension	= data_name.substring(separator);
				
				DiskManagerFileInfoHelper	info = 
					new DiskManagerFileInfoHelper()
					{		
						private boolean	priority;
						private boolean	skipped;
						private long	downloaded;
						
						public void 
						setPriority(boolean b)
						{
							priority	= b;
							
							storeFilePriorities( download_manager, res );
						}
				
						public void 
						setSkipped(boolean b)
						{
							skipped	= b;
							
							storeFilePriorities( download_manager, res );
						}
				 		 	
						public int 
						getAccessMode()
						{
							return( READ );
						}
						
						public long 
						getDownloaded()
						{
							return( downloaded );
						}
						
						public void
						setDownloaded(
							long	l )
						{
							downloaded	= l;
						}
						
						public String 
						getExtension()
						{
							return( data_extension );
						}
							
						public int 
						getFirstPieceNumber()
						{
							return( first_piece );
						}
					  
						public int 
						getLastPieceNumber()
						{
							return( last_piece );
						}
						
						public long 
						getLength()
						{
							return( torrent_file.getLength());
						}
							
						public int	
						getIndex()
						{
							return( file_index );
						}
						
						public int 
						getNbPieces()
						{
							return( last_piece - first_piece + 1 );
						}
													
						public boolean 
						isPriority()
						{
							return( priority );
						}
						
						public boolean 
						isSkipped()
						{
							return( skipped );
						}
						
						public DiskManager 
						getDiskManager()
						{
							return( null );
						}
			
						public DownloadManager 
						getDownloadManager()
						{
							return( download_manager );
						}
	
						public File 
						getFile(
							boolean	follow_link )
						{
							if ( follow_link ){
								
								File link = getLink();
								
								if ( link != null ){
									
									return( link );
								}
							}
							return( data_file );
						}
						
						public boolean
						setLink(
							File	link_destination )
						{
							return( setFileLink( download_manager, res, this, data_file, link_destination ));
						}
												
						public File
						getLink()
						{
							return( download_manager.getDownloadState().getFileLink( data_file ));
						}
						
						public boolean
						setStorageType(
							int		type )
						{
							String[]	types = getStorageTypes( download_manager );

							int	old_type = types[file_index].equals( "L")?ST_LINEAR:ST_COMPACT;
							
							if ( type == old_type ){
								
								return( true );
							}
							
							int	s = download_manager.getState();
							
							if ( s != DownloadManager.STATE_STOPPED && s != DownloadManager.STATE_ERROR ){
								
								Debug.out( "setStorageType: download not stopped" );
								
								return( false );
							}
							
							File	existing_file = getFile(true);
							
							if ( existing_file.exists()){
														    		
								if ( FileUtil.deleteWithRecycle( existing_file )){
						    																
									download_manager.resetFile( this );
										
								}else{
						    					
									LGLogger.logRepeatableAlert( 
											LGLogger.AT_ERROR, "Failed to delete '" + existing_file.toString() + "'" );
									
									return( false );
								}
							}
							
								// at this point, if we could be bothered, we could migrate storage types
								// from one to another. We could also handle "allocate all space" and "zero new"
								// properly with files returning to a linear state...
							
								// establish the new file type 
							
							try{
							 	CacheFile cache_file = 
							 		CacheFileManagerFactory.getSingleton().createFile( 
					  						new CacheFileOwner()
					  						{
					  						 	public String
					  						  	getCacheFileOwnerName()
					  						  	{
					  						  		return( download_manager.getInternalName());
					  						  	}
					  						  	
					  							public TOTorrentFile
					  							getCacheFileTorrentFile()
					  							{
					  								return( torrent_file );
					  							}
					  							
					  							public File 
					  							getCacheFileControlFile(String name) 
					  							{
					  								return( download_manager.getDownloadState().getStateFile( name ));
					  							}  							
					  						}, 
					  						existing_file, 
					  						type==ST_LINEAR?CacheFile.CT_LINEAR:CacheFile.CT_COMPACT );							
					  							
					  			cache_file.close();
					  			
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
								
								return( false );
							}
				  							
							types[file_index] = type==ST_LINEAR?"L":"C";
							
							DownloadManagerState	dm_state = download_manager.getDownloadState();
							
							dm_state.setListAttribute( DownloadManagerState.AT_FILE_STORE_TYPES, types );
							
							dm_state.save();
							
							return( true );
						}
						
						public int
						getStorageType()
						{
							String[]	types = getStorageTypes( download_manager );
							
							return( types[file_index].equals( "L")?ST_LINEAR:ST_COMPACT );
						}
						
						public void
						flushCache()
						{
						}
					};
					
				res[i]	= info;
			}
			
			loadFilePriorities( download_manager, res );
			
			loadFileDownloaded( download_manager, res );
			
			return( res );

		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( new DiskManagerFileInfo[0]);
	
		}
	}
	
	public static void
	setFileLinks(
		DownloadManager		download_manager,
		Map					links )
	{
		try{
			CacheFileManagerFactory.getSingleton().setFileLinks( links );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
}