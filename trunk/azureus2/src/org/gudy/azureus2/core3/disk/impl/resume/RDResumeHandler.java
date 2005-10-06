/*
 * Created on 31-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.disk.impl.resume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.File;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.disk.impl.*;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.disk.*;

import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerException;

/**
 * @author parg
 *
 */
public class 
RDResumeHandler
	implements ParameterListener
{
	private static final byte		PIECE_NOT_DONE			= 0;
	private static final byte		PIECE_DONE				= 1;
	private static final byte		PIECE_RECHECK_REQUIRED	= 2;

	
	protected DiskManagerHelper		disk_manager;
	protected DMWriterAndChecker	writer_and_checker;
	protected DownloadManagerState	download_manager_state;
	
	protected TOTorrent				torrent;
		
	protected int					nbPieces;
	protected int					pieceLength;
	protected int					lastPieceLength;
	
	protected boolean				bOverallContinue;
	
	protected boolean useFastResume = COConfigurationManager.getBooleanParameter("Use Resume", true);

	public 
	RDResumeHandler(
		DiskManagerHelper	_disk_manager,
		DMWriterAndChecker	_writer_and_checker )
	{
		disk_manager		= _disk_manager;
		writer_and_checker	= _writer_and_checker;
		
		download_manager_state	= disk_manager.getDownloadManager().getDownloadState();
		
		torrent			= disk_manager.getTorrent();
		nbPieces		= disk_manager.getNumberOfPieces();
		pieceLength		= disk_manager.getPieceLength();
		lastPieceLength	= disk_manager.getLastPieceLength();

	}
	
	public void
	start()
	{
		COConfigurationManager.addParameterListener("Use Resume", this);
		
		bOverallContinue	= true;
	}
	
	public void
	stop()
	{
		bOverallContinue	= false;
		
		COConfigurationManager.removeParameterListener("Use Resume", this);
	}
	
	public void 
	parameterChanged(
		String parameterName )
	{
	    useFastResume = COConfigurationManager.getBooleanParameter("Use Resume", true);
	}
	
	public void 
	checkAllPieces(
		boolean newfiles ) 
	{
		//long	start = System.currentTimeMillis();
		
		try{
			disk_manager.setState( DiskManager.CHECKING );
						
			boolean resumeEnabled = useFastResume;
			
				//disable fast resume if a new file was created
			
			if (newfiles){
				
				resumeEnabled = false;
			}
			
			boolean	resume_data_complete = false;
			
			
			final AESemaphore	pending_checks_sem 	= new AESemaphore( "RD:PendingChecks" );
			int					pending_check_num	= 0;

			DiskManagerPiece[]	pieces	= disk_manager.getPieces();


			if ( resumeEnabled ){
				
				boolean resumeValid = false;
				
				byte[] resume_pieces = null;
				
				Map partialPieces = null;
				
				Map	resume_data = getResumeData();							
				
				if ( resume_data != null ){
					
					try {
						
						resume_pieces = (byte[])resume_data.get("resume data");
						
						if ( resume_pieces != null ){
							
							if ( resume_pieces.length != pieces.length ){
							
								Debug.out( "Resume data array length mismatch: " + resume_pieces.length + "/" + pieces.length );
								
								resume_pieces	= null;
							}
						}
						
						partialPieces = (Map)resume_data.get("blocks");
						
						resumeValid = ((Long)resume_data.get("valid")).intValue() == 1;
						
							// if the torrent download is complete we don't need to invalidate the
							// resume data
						
						if ( isTorrentResumeDataComplete( disk_manager.getDownloadManager())){
							
							resume_data_complete	= true;
									
						}else{
							
								// set it so that if we crash the NOT_DONE pieces will be
								// rechecked
							
							resume_data.put("valid", new Long(0));
							
							saveResumeData( resume_data );
						}
						
					}catch(Exception ignore){
						
						// ignore.printStackTrace();
					}
					
				}else{
					
					// System.out.println( "resume dir not found");
				}
								
				if ( resume_pieces == null ){
					
					resumeValid	= false;
					
					resume_pieces	= new byte[pieces.length];
				}
				
					// calculate the current file sizes up front for performance reasons
				
				DiskManagerFileInfo[]	files = disk_manager.getFiles();
				
				Map	file_sizes = new HashMap();
				
				for (int i=0;i<files.length;i++){
					
					try{
						Long	len = new Long(((DiskManagerFileInfoImpl)files[i]).getCacheFile().getLength());
					
						file_sizes.put( files[i], len );
						
					}catch( CacheFileManagerException e ){
						
						Debug.printStackTrace(e);
					}
				}
				
				for (int i = 0; i < pieces.length && bOverallContinue; i++){ 
					
					DiskManagerPiece	dm_piece	= pieces[i];
					
					disk_manager.setPercentDone(((i + 1) * 1000) / nbPieces );
					
					byte	piece_state = resume_pieces[i];
					
						// valid resume data means that the resume array correctly represents
						// the state of pieces on disk, be they done or not
					
					if ( piece_state == PIECE_DONE ){
					
							// at least check that file sizes are OK for this piece to be valid
						
						PieceList list = disk_manager.getPieceList(i);
						
						for (int j=0;j<list.size();j++){
							
							PieceMapEntry	entry = list.get(j);
							
							Long	file_size 		= (Long)file_sizes.get(entry.getFile());
							
							if ( file_size == null ){
								
								piece_state	= PIECE_NOT_DONE;
								
								LGLogger.log(0, 0, LGLogger.INFORMATION, "Piece #" + i + ": file is missing, fails re-check." );

								break;
							}
							
							long	expected_size 	= entry.getOffset() + entry.getLength();
							
							if ( file_size.longValue() < expected_size ){
								
								piece_state	= PIECE_NOT_DONE;
								
								LGLogger.log(0, 0, LGLogger.INFORMATION, "Piece #" + i + ": file is too small, fails re-check. File size = " + file_size + ", piece needs " + expected_size );

								break;
							}
						}
					}
					
					if ( piece_state == PIECE_DONE ){
						
						dm_piece.setDone( true );
						
					}else{								
						
							// We only need to recheck pieces that are marked as not-ok
							// if the resume data is invalid or explicit recheck needed
						
						if ( piece_state == PIECE_RECHECK_REQUIRED || !resumeValid ){
						
							try{								
								writer_and_checker.checkPiece(
									i,
									new CheckPieceResultHandler()
									{
										public void
										processResult(
											int		piece_number,
											int		result,
											Object	user_data )
										{
											LGLogger.log(0, 0, LGLogger.INFORMATION, "Piece #" + piece_number + (result==CheckPieceResultHandler.OP_SUCCESS?" passed":" failed") + " re-check.");
		
											pending_checks_sem.release();
										}
									},
									null, 
									true );
								
								pending_check_num++;
								
							}catch( Throwable e ){
							
								Debug.printStackTrace(e);
							}
						}
					}
				}
					
				if ( partialPieces != null && resumeValid ){
														
					Iterator iter = partialPieces.entrySet().iterator();
					
					while (iter.hasNext()) {
						
						Map.Entry key = (Map.Entry)iter.next();
						
						int pieceNumber = Integer.parseInt((String)key.getKey());
													
						List blocks = (List)partialPieces.get(key.getKey());
						
						Iterator iterBlock = blocks.iterator();
						
						while (iterBlock.hasNext()) {
							
							pieces[pieceNumber].setWritten(((Long)iterBlock.next()).intValue());
						}
					}
				}
			}else{
				
					// resume not enabled, recheck everything
				
				for (int i = 0; i < pieces.length && bOverallContinue; i++){ 
										
					disk_manager.setPercentDone(((i + 1) * 1000) / nbPieces );						
						
					try{
						writer_and_checker.checkPiece(
							i,
							new CheckPieceResultHandler()
							{
								public void
								processResult(
									int		piece_number,
									int		result,
									Object	user_data )
								{
									LGLogger.log(0, 0, LGLogger.INFORMATION, "Piece #" + piece_number + (result==CheckPieceResultHandler.OP_SUCCESS?" passed":" failed") + " re-check.");
	
									pending_checks_sem.release();
								}
							},
							null,
							true );
						
						pending_check_num++;
						
					}catch( Throwable e ){
					
						Debug.printStackTrace(e);
					}
				}								
			}
						
			while( pending_check_num > 0 ){
				
				pending_checks_sem.reserve();
				
				pending_check_num--;
			}
			
				//dump the newly built resume data to the disk/torrent
			
			if ( bOverallContinue && !resume_data_complete ){
				
				try{
					dumpResumeDataToDisk(false, false);
					
				}catch( Exception e ){
					
					Debug.out( "Failed to dump initial resume data to disk" );
					
					Debug.printStackTrace( e );
				}
			}
		}catch( Throwable e ){
			
				// if something went wrong then log and continue. 
			
			Debug.printStackTrace(e);
			
		}finally{
			// System.out.println( "Check of '" + disk_manager.getDownloadManager().getDisplayName() + "' completed in " + (System.currentTimeMillis() - start));
		}
	}
	
	public void 
	dumpResumeDataToDisk(
		boolean savePartialPieces, 
		boolean force_recheck )
	
		throws Exception
	{
    
			// if file caching is enabled then this is an important time to ensure that the cache is
			// flushed as we are going to record details about the accuracy of written data.
			// First build the resume map from the data (as updates can still be goin on)
			// Then, flush the cache. This means that on a successful flush the built resume
			// data matches at least the valid state of the data
			// Then update the torrent
		
		DiskManagerFileInfo[]	files = disk_manager.getFiles();
		
		if ( !useFastResume ){
			
				// flush cache even if resume disable
			
			for (int i=0;i<files.length;i++){
				
				files[i].flushCache();
			}
			
			return;
		}

		boolean	was_complete = isTorrentResumeDataComplete( disk_manager.getDownloadManager());
		
		DiskManagerPiece[] pieces	= disk_manager.getPieces();

			//build the piece byte[]
		
		byte[] resume_pieces = new byte[pieces.length];
		
		if ( !force_recheck ){
				
			for (int i = 0; i < resume_pieces.length; i++) {
		  	
			  	if ( pieces[i].getDone()){
			  		
					resume_pieces[i] = PIECE_DONE;
			  		
			  	}else{
			  	
					resume_pieces[i] = PIECE_NOT_DONE;
			  	}
			}
		}
		
		Map	resume_data = new HashMap();
	  	  
		resume_data.put( "resume data", resume_pieces );
		
		Map partialPieces = new HashMap();
	
		if ( savePartialPieces && !force_recheck ){
	  		  		      
			for (int i = 0; i < pieces.length; i++) {
				
				DiskManagerPiece piece = pieces[i];
				
					// save the partial pieces for any pieces that have not yet been completed
					// and are in-progress (i.e. have at least one block downloaded)
				
				boolean[] downloaded = piece.getWritten();

				if (( !piece.getDone()) && piece.getCompleteCount() > 0 && downloaded != null ){
					
					
					List blocks = new ArrayList();
					
					for (int j = 0; j < downloaded.length; j++) {
						
						if (downloaded[j]){
							
							blocks.add(new Long(j));
						}
					}
          
					partialPieces.put("" + i, blocks);
				}
			}
			
			resume_data.put("blocks", partialPieces);
		}
		
			// savePartialPieces has overloaded meanings. It also implies that the download
			// is stopping, as opposed to this being an interim resume data save, and therefore
			// that the resume data should be set as "valid". Being valid has the meaning that
			// blocks marked as not-done will *not* be checked when the torrent is restarted
			// to see if they are actually complete.
			// TODO: fix this up!!!!
		
		resume_data.put("valid", new Long( force_recheck?0:(savePartialPieces?1:0)));
		
		for (int i=0;i<files.length;i++){
			
			files[i].flushCache();
		}
		
	  		// OK, we've got valid resume data and flushed the cache
	  
		boolean	is_complete = isTorrentResumeDataComplete( disk_manager.getDownloadManager());
	
		if ( was_complete && is_complete ){
	 
	  		// no change, no point in writing
	  		  	
		}else{
	  	
			saveResumeData( resume_data );
		}
	}
	
	protected Map
	getResumeData()
	{
		return( getResumeData( disk_manager.getDownloadManager()));
	}
	
	protected static Map
	getResumeData(
		DownloadManager		download_manager)
	{
		DownloadManagerState download_manager_state = download_manager.getDownloadState();
		
		Map resumeMap = download_manager_state.getResumeData();
		
		if ( resumeMap != null ){
			
				// time to remove this directory based madness - just use a "data" key
			
			Map	resume_data = (Map)resumeMap.get( "data" );
			
			if ( resume_data != null ){
				
				return( resume_data );
			}
			
				// backward compatability here over path management changes :(
			
			String	resume_key = 
				download_manager.getTorrent().isSimpleTorrent()?
						download_manager.getAbsoluteSaveLocation().getParent():
						download_manager.getAbsoluteSaveLocation().toString();
	
			String[]	resume_keys = new String[4];
	
				// see bug 869749 for explanation of this mangling
				
				// unfortunately, if the torrent hasn't been saved and restored then the
				// mangling with not yet have taken place. So we have to also try the 
				// original key (see 878015)

				// also I've introduced canonicalisation into the resume key (2.1.0.5), so until any migration
				// issues have been resolved we need to support both original + non-canonicalised forms
		
			resume_keys[0]	= resume_key;
			
			try{
				resume_keys[1]= new String( resume_key.getBytes(Constants.DEFAULT_ENCODING),Constants.BYTE_ENCODING);
				
				// System.out.println( "resume: path = " + ByteFormatter.nicePrint(path )+ ", mangled_path = " + ByteFormatter.nicePrint(mangled_path));
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
			
			String	canonical_resume_key = resume_key;
			
			try{
				canonical_resume_key	= new File( resume_key).getCanonicalFile().toString();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
			
			resume_keys[2]	= canonical_resume_key;
			
			try{
				resume_keys[3]= new String( resume_keys[2].getBytes(Constants.DEFAULT_ENCODING),Constants.BYTE_ENCODING);
				
				// System.out.println( "resume: path = " + ByteFormatter.nicePrint(path )+ ", mangled_path = " + ByteFormatter.nicePrint(mangled_path));
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		
			Map resumeDirectory = null;
			
			for (int i=0;i<resume_keys.length;i++){
				
				String	rk = resume_keys[i];
				
				if ( rk != null ){
						
					resumeDirectory	= (Map)resumeMap.get(rk);
					
					if ( resumeDirectory != null ){
						
						break;
					}
				}
			}
			
				// if we've migrated, move it into the right place
			
			if ( resumeDirectory != null ){
				
				saveResumeData( download_manager_state, resumeDirectory );
			}
			
			return( resumeDirectory );
			
		}else{
			
			return( null );
		}
	}

	protected void
	saveResumeData(
		Map		resume_data )
	{
		saveResumeData( disk_manager.getDownloadManager().getDownloadState(), resume_data );
	}
	
	protected static void
	saveResumeData(
		DownloadManagerState		download_manager_state,
		Map							resume_data )
	{		
		Map	resume_map = new HashMap();
		
		resume_map.put( "data", resume_data );
		
			// for a short while (2305 B33 current) we'll save the resume data in any existing locations as well so that
			// people can regress AZ versions after updating and their resume data will still work.... 
		
		Map	old_resume_data = download_manager_state.getResumeData();
		
		if ( old_resume_data != null ){
			
			Iterator	it = old_resume_data.keySet().iterator();
			
			while( it.hasNext()){
				
				Object	key = it.next();
							
				resume_map.put( key, resume_data );
			}
		}
		
		download_manager_state.setResumeData( resume_map );
		
		download_manager_state.save();
	}
	
	
	public static void
	setTorrentResumeDataComplete(
		DownloadManagerState	download_manager_state )
	{
		TOTorrent	torrent = download_manager_state.getTorrent();
		
		int	piece_count = torrent.getNumberOfPieces();
		
		byte[] resume_pieces = new byte[piece_count];
		
		for (int i = 0; i < resume_pieces.length; i++) {
			
			resume_pieces[i] = PIECE_DONE;
		}

		Map resume_data = new HashMap();
			
		resume_data.put( "resume data", resume_pieces );
		
		Map partialPieces = new HashMap();
		
		resume_data.put("blocks", partialPieces );
		
		resume_data.put("valid", new Long(1));	
	
		saveResumeData( download_manager_state, resume_data );
	}
	
	protected static void
	clearResumeDataSupport(
		DownloadManager			download_manager,
		DiskManagerFileInfo		file,
		boolean					recheck )
	{
		DownloadManagerState	download_manager_state = download_manager.getDownloadState();
		
		Map resume_data = getResumeData( download_manager );

		if ( resume_data == null ){
			
			return;
		}
			
			// TODO: we could be a bit smarter with the first and last pieces regarding
			// partial blocks where the piece spans the file bounaries.
		
			// clear any affected pieces
		
		byte[]	resume_pieces = (byte[])resume_data.get("resume data");
		
		int	first_piece = file.getFirstPieceNumber();
		int last_piece	= file.getLastPieceNumber();
		
		if ( resume_pieces != null ){
			
			for (int i=first_piece;i<=last_piece;i++){
				
				if ( i >= resume_pieces.length ){
					
					break;
				}
								
				resume_pieces[i] = recheck?PIECE_RECHECK_REQUIRED:PIECE_NOT_DONE;
			}
		}
			// clear any affected partial pieces
		
		Map	partial_pieces = (Map)resume_data.get("blocks");
		
		if ( partial_pieces != null ){
			
			Iterator iter = partial_pieces.keySet().iterator();
		
			while (iter.hasNext()) {
			
				int piece_number = Integer.parseInt((String)iter.next());
	
				if ( piece_number >= first_piece && piece_number <= last_piece ){
					
					iter.remove();
				}
			}
		}
					
			// either way we're valid as 
			//    1) clear -> pieces are set as not done
			//	  2) recheck -> pieces are set as "recheck" and will be checked on restart
		
		resume_data.put( "valid", new Long(1));	
		
		saveResumeData( download_manager_state, resume_data );
	}
	

	
	public static void
	clearResumeData(
		DownloadManager			download_manager,
		DiskManagerFileInfo		file )
	{
		clearResumeDataSupport( download_manager, file, false );
	}
	
	public static void
	recheckFile(
		DownloadManager			download_manager,
		DiskManagerFileInfo		file )
	{
		clearResumeDataSupport( download_manager, file, true );
	}
	
	public static void
	setTorrentResumeDataNearlyComplete(
		DownloadManagerState	download_manager_state )
	{
			// backwards compatability, resume data key is the dir
		
		TOTorrent	torrent = download_manager_state.getTorrent();
		
		long	piece_count = torrent.getNumberOfPieces();
		
		byte[] resume_pieces = new byte[(int)piece_count];
		
		for (int i = 0; i < resume_pieces.length; i++) {
			
			resume_pieces[i] = PIECE_DONE;
		}

			// randomly clear some pieces
		
		for (int i=0;i<3;i++){
			
			int	piece_num = (int)(Math.random()*piece_count);
						
			resume_pieces[piece_num]= PIECE_NOT_DONE;
		}
		
		Map resumeMap = new HashMap();
								
		resumeMap.put( "resume data", resume_pieces);
		
		Map partialPieces = new HashMap();
		
		resumeMap.put("blocks", partialPieces);
		
		resumeMap.put("valid", new Long(0));	// recheck the not-done pieces
	
		saveResumeData(download_manager_state,resumeMap);
	}
	
	public static boolean
	isTorrentResumeDataComplete(
		DownloadManager			download_manager )
	{
		DownloadManagerState	download_manager_state = download_manager.getDownloadState();
		
		TOTorrent	torrent = download_manager_state.getTorrent();
		
			// backwards compatability, resume data key is the dir
		
		Map	resume_data = getResumeData( download_manager );
					
		try{
			int	piece_count = torrent.getNumberOfPieces();
							
			if ( resume_data != null ){
				
				byte[] 	pieces 	= (byte[])resume_data.get("resume data");
				Map		blocks	= (Map)resume_data.get("blocks");
				boolean	valid	= ((Long)resume_data.get("valid")).intValue() == 1;
				
					// any partial pieced -> not complete
				
				if ( blocks == null || blocks.size() > 0 ){
					
					return( false );
				}
				
				if ( valid && pieces != null && pieces.length == piece_count ){
					
					for (int i=0;i<pieces.length;i++){

						if ( pieces[i] != PIECE_DONE ){
							
								// missing piece or recheck outstanding
							
							return( false );
						}
					}
					
					return( true );
				}
			}
		}catch( Throwable e ){
		
			Debug.printStackTrace( e );
		}	
		
		return( false );
	}
}
