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
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.impl.DiskManagerHelper;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.disk.*;

/**
 * @author parg
 *
 */
public class 
RDResumeHandler
	implements ParameterListener
{
	protected DiskManagerHelper		disk_manager;
	protected DMWriterAndChecker	writer_and_checker;
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
			
			if (newfiles) resumeEnabled = false;
			
			boolean	resume_data_complete = false;
			
			
			final AESemaphore	pending_checks_sem 	= new AESemaphore( "RD:PendingChecks" );
			int					pending_check_num	= 0;

			DiskManagerPiece[]	pieces	= disk_manager.getPieces();


			if ( resumeEnabled ){
				
				boolean resumeValid = false;
				
				byte[] resumeArray = null;
				
				Map partialPieces = null;
				
				Map resumeMap = torrent.getAdditionalMapProperty("resume");
				
				if (resumeMap != null) {
					
						// backward compatability here over path management changes :(
					
					String resume_key = 
						torrent.isSimpleTorrent()?
							disk_manager.getDownloadManager().getTorrentSaveDir():
							disk_manager.getDownloadManager().getTorrentSaveDirAndFile();
					
				
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
					
					resume_keys[2]	= getCanonicalResumeKey( resume_key );
					
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
									
					if ( resumeDirectory != null ){
						
						try {
							
							resumeArray = (byte[])resumeDirectory.get("resume data");
							
							if ( resumeArray != null ){
								
								if ( resumeArray.length != pieces.length ){
								
									Debug.out( "Resume data array length mismatch: " + resumeArray.length + "/" + pieces.length );
									
									resumeArray	= null;
								}
							}
							
							partialPieces = (Map)resumeDirectory.get("blocks");
							
							resumeValid = ((Long)resumeDirectory.get("valid")).intValue() == 1;
							
								// if the torrent download is complete we don't need to invalidate the
								// resume data
							
							if ( isTorrentResumeDataComplete( 
									torrent, 
									disk_manager.getDownloadManager().getTorrentSaveDir(),
									disk_manager.getDownloadManager().getTorrentSaveFile())){
								
								resume_data_complete	= true;
										
							}else{
								
								resumeDirectory.put("valid", new Long(0));
								
								saveTorrent();
							}
							
						}catch(Exception ignore){
							
							// ignore.printStackTrace();
						}
						
					}else{
						
						// System.out.println( "resume dir not found");
					}
				}
								
				if ( resumeArray == null ){
					
					resumeValid	= false;
				}
				
				for (int i = 0; i < pieces.length && bOverallContinue; i++){ 
					
					DiskManagerPiece	dm_piece	= pieces[i];
					
					disk_manager.setPercentDone(((i + 1) * 1000) / nbPieces );
					
					if ( resumeValid ){
						
						dm_piece.setDone( resumeArray[i] == 1 );
						
					}else{								
						
						pending_check_num++;
						
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
							null );
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
						
					pending_check_num++;
					
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
						null );
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
				}
			}
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

		boolean	was_complete = isTorrentResumeDataComplete( 
									torrent, 
									disk_manager.getDownloadManager().getTorrentSaveDir(), 
									disk_manager.getDownloadManager().getTorrentSaveFile());
		
		DiskManagerPiece[] pieces	= disk_manager.getPieces();

			//build the piece byte[]
		
		byte[] resumeData = new byte[pieces.length];
		
				
		for (int i = 0; i < resumeData.length; i++) {
	  	
		  	if ( pieces[i].getDone()){
		  		
				resumeData[i] = (byte)1;
		  		
		  	}else{
		  	
				resumeData[i] = (byte)0;
		  	}
		}
		
		Map resumeMap = new HashMap();
	  
		Map resumeDirectory = new HashMap();
	  
	  		// We *really* shouldn't be using a localised string as a Map key (see bug 869749)
	  		// currently fixed by mangling such that decode works
	  
		// System.out.println( "writing resume data: key = " + ByteFormatter.nicePrint(path));
	  
		String resume_key = 
			torrent.isSimpleTorrent()?
				disk_manager.getDownloadManager().getTorrentSaveDir():
				disk_manager.getDownloadManager().getTorrentSaveDirAndFile();
		
		resume_key	= getCanonicalResumeKey( resume_key );

		resumeMap.put(resume_key, resumeDirectory);
	  
		resumeDirectory.put("resume data", resumeData);
	  
		Map partialPieces = new HashMap();
	
		if ( savePartialPieces  ){
	  		  		      
			for (int i = 0; i < pieces.length; i++) {
				
				DiskManagerPiece piece = pieces[i];
				
				if (piece != null && piece.getCompleteCount() > 0){
					
					boolean[] downloaded = piece.getWritten();
					
					List blocks = new ArrayList();
					
					for (int j = 0; j < downloaded.length; j++) {
						
						if (downloaded[j]){
							
							blocks.add(new Long(j));
						}
					}
          
					partialPieces.put("" + i, blocks);
				}
			}
			
			resumeDirectory.put("blocks", partialPieces);
		}
		
		resumeDirectory.put("valid", new Long( force_recheck?0:1));
		
		for (int i=0;i<files.length;i++){
			
			files[i].flushCache();
		}
		
	  		// OK, we've got valid resume data and flushed the cache
	  
		torrent.setAdditionalMapProperty("resume", resumeMap);

		boolean	is_complete = 
			isTorrentResumeDataComplete( 
	  			torrent, 
				disk_manager.getDownloadManager().getTorrentSaveDir(),
				disk_manager.getDownloadManager().getTorrentSaveFile());

		if ( was_complete && is_complete ){
	 
	  		// no change, no point in writing
	  		  	
		}else{
	  	
			saveTorrent();
		}
	}

		/**
		 * data_dir must be the parent folder for a simple torrent or the *actual* folder for non-simple
		 * @param torrent
		 * @param data_dir
		 */
	
	public static void
	setTorrentResumeDataComplete(
		TOTorrent	torrent,
		String		resume_key  )
	{
		resume_key	= getCanonicalResumeKey( resume_key );

		int	piece_count = torrent.getPieces().length;
		
		byte[] resumeData = new byte[piece_count];
		
		for (int i = 0; i < resumeData.length; i++) {
			
			resumeData[i] = (byte)1;
		}

		Map resumeMap = new HashMap();
		
		torrent.setAdditionalMapProperty("resume", resumeMap);

		Map resumeDirectory = new HashMap();
		
		// We *really* shouldn't be using a localised string as a Map key (see bug 869749)
		// currently fixed by mangling such that decode works
		
		resumeMap.put(resume_key, resumeDirectory);
		
		resumeDirectory.put("resume data", resumeData);
		
		Map partialPieces = new HashMap();
		
		resumeDirectory.put("blocks", partialPieces);
		
		resumeDirectory.put("valid", new Long(1));	
	}
	
	public static void
	setTorrentResumeDataNearlyComplete(
		TOTorrent	torrent,
		String		torrent_save_dir,
		String		torrent_save_file )
	{
			// backwards compatability, resume data key is the dir
		
		String	resume_key = torrent.isSimpleTorrent()?
								torrent_save_dir:
								(torrent_save_dir + File.separator + torrent_save_file );
		
		resume_key	= getCanonicalResumeKey( resume_key );
		
		int	piece_count = torrent.getPieces().length;
		
		byte[] resumeData = new byte[piece_count];
		
		for (int i = 0; i < resumeData.length; i++) {
			
			resumeData[i] = (byte)1;
		}

			// randomly clear some pieces
		
		for (int i=0;i<3;i++){
			
			int	piece_num = (int)(Math.random()*piece_count);
						
			resumeData[piece_num]= 0;
		}
		
		Map resumeMap = new HashMap();
		
		torrent.setAdditionalMapProperty("resume", resumeMap);

		Map resumeDirectory = new HashMap();
		
		// We *really* shouldn't be using a localised string as a Map key (see bug 869749)
		// currently fixed by mangling such that decode works
		
		resumeMap.put(resume_key, resumeDirectory);
		
		resumeDirectory.put("resume data", resumeData);
		
		Map partialPieces = new HashMap();
		
		resumeDirectory.put("blocks", partialPieces);
		
		resumeDirectory.put("valid", new Long(0));	
	}
	
	public static boolean
	isTorrentResumeDataComplete(
		TOTorrent	torrent,
		String		torrent_save_dir,
		String		torrent_save_file )
	{
			// backwards compatability, resume data key is the dir
		
		String	resume_key = torrent.isSimpleTorrent()?
								torrent_save_dir:
								(torrent_save_dir + File.separator + torrent_save_file );
		
		// System.out.println( "resume key = " + resume_key );
	
		resume_key	= getCanonicalResumeKey( resume_key );
		
		try{
			int	piece_count = torrent.getPieces().length;
		
			Map resumeMap = torrent.getAdditionalMapProperty("resume");
		
			if (resumeMap != null) {
			
					// see bug 869749 for explanation of this mangling
				
				String mangled_path;
				
				try{
					mangled_path = new String(resume_key.getBytes(Constants.DEFAULT_ENCODING),Constants.BYTE_ENCODING);
									
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
					
					mangled_path = resume_key;
				}
				
				Map resumeDirectory = (Map)resumeMap.get(mangled_path);
				
				if ( resumeDirectory == null ){
					
					// unfortunately, if the torrent hasn't been saved and restored then the
					// mangling with not yet have taken place. So we have to also try the 
					// original key (see 878015)
					
					resumeDirectory = (Map)resumeMap.get(resume_key);
				}
				
				if (resumeDirectory != null) {
										
					byte[] 	resume_data =  (byte[])resumeDirectory.get("resume data");
					Map		blocks		= (Map)resumeDirectory.get("blocks");
					boolean	valid		= ((Long)resumeDirectory.get("valid")).intValue() == 1;
					
						// any partial pieced -> not complete
					if ( blocks == null || blocks.size() > 0 ){
						
						return( false );
					}
					
					if ( valid && resume_data.length == piece_count ){
						
						for (int i=0;i<resume_data.length;i++){
	
							if ( resume_data[i] == 0 ){
								
									// missing piece
								
								return( false );
							}
						}
						
						return( true );
					}
				}
			}
		}catch( Throwable e ){
		
			Debug.printStackTrace( e );
		}	
		
		return( false );
	}
	
	private void 
	saveTorrent() 
	{
		try{
			TorrentUtils.writeToFile( torrent );
						
		} catch (TOTorrentException e) {
			
			Debug.printStackTrace( e );
		}
	}

	protected static String
	getCanonicalResumeKey(
		String		resume_key )
	{
		try{
			resume_key	= new File( resume_key).getCanonicalFile().toString();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		return( resume_key );
	}
}
