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

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.impl.DiskManagerHelper;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.peer.*;

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
	
	protected PEPiece[] 			recovered_pieces;
	
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
	 
	public PEPiece[] 
	getRecoveredPieces()
	{
		return( recovered_pieces );
	}
	
	public void 
	checkAllPieces(
		boolean newfiles ) 
	{
		disk_manager.setState( DiskManager.CHECKING );
		
		int startPos = 0;
		
		boolean resumeEnabled = useFastResume;
		//disable fast resume if a new file was created
		if (newfiles) resumeEnabled = false;
		
		boolean	resume_data_complete = false;
		
		if (resumeEnabled){
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
				
			
				// see bug 869749 for explanation of this mangling
				
				/*
				System.out.println( "Resume map");
				
				Iterator it = resumeMap.keySet().iterator();
				
				while( it.hasNext()){
					
					System.out.println( "\tmap:" + ByteFormatter.nicePrint((String)it.next()));
				}
				*/
				
				String mangled_path;
				
				try{
					mangled_path = new String( resume_key.getBytes(Constants.DEFAULT_ENCODING),Constants.BYTE_ENCODING);
					
					// System.out.println( "resume: path = " + ByteFormatter.nicePrint(path )+ ", mangled_path = " + ByteFormatter.nicePrint(mangled_path));
					
				}catch( Throwable e ){
					
					e.printStackTrace();
					
					mangled_path = resume_key;
				}
				
				Map resumeDirectory = (Map)resumeMap.get(mangled_path);
				
				if ( resumeDirectory == null ){
					
						// unfortunately, if the torrent hasn't been saved and restored then the
						// mangling with not yet have taken place. So we have to also try the 
						// original key (see 878015)
					
					resumeDirectory = (Map)resumeMap.get(resume_key);
				}
				
				if ( resumeDirectory != null ){
					
					try {
						
						resumeArray = (byte[])resumeDirectory.get("resume data");
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
						/* ignore */ 
					}
					
				}else{
					
					// System.out.println( "resume dir not found");
				}
			}
			
			boolean[]	pieceDone	= disk_manager.getPiecesDone();
			
			if (resumeEnabled && (resumeArray != null) && (resumeArray.length <= pieceDone.length)) {
				startPos = resumeArray.length;
				for (int i = 0; i < resumeArray.length && bOverallContinue; i++) { //parse the array
					disk_manager.setPercentDone(((i + 1) * 1000) / nbPieces );
					//mark the pieces
					if (resumeArray[i] == 0) {
						if (!resumeValid) pieceDone[i] = writer_and_checker.checkPiece(i);
					}
					else {
						disk_manager.computeFilesDone(i);
						
						pieceDone[i] = true;
						
						if (i < nbPieces - 1) {
							
							disk_manager.setRemaining( disk_manager.getRemaining() - pieceLength );
						}
						if (i == nbPieces - 1) {
							
							disk_manager.setRemaining( disk_manager.getRemaining() - lastPieceLength );
						}
					}
				}
				
				if (partialPieces != null && resumeValid) {
					
					recovered_pieces = new PEPiece[nbPieces];
									
					Iterator iter = partialPieces.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry key = (Map.Entry)iter.next();
						int pieceNumber = Integer.parseInt((String)key.getKey());
						PEPiece piece;
						if (pieceNumber < nbPieces - 1)
							piece = PEPieceFactory.create(disk_manager.getPeerManager(), disk_manager.getPieceLength(), pieceNumber);
						else
							piece = PEPieceFactory.create(disk_manager.getPeerManager(), disk_manager.getLastPieceLength(), pieceNumber);
						List blocks = (List)partialPieces.get(key.getKey());
						Iterator iterBlock = blocks.iterator();
						while (iterBlock.hasNext()) {
							piece.setWritten(null,((Long)iterBlock.next()).intValue());
						}
						recovered_pieces[pieceNumber] = piece;
					}
				}
			}
		}
		
		for (int i = startPos; i < nbPieces && bOverallContinue; i++) {
			
			disk_manager.setPercentDone(((i + 1) * 1000) / nbPieces);
			
			writer_and_checker.checkPiece(i);
		}
		
			//dump the newly built resume data to the disk/torrent
		
		if (bOverallContinue && resumeEnabled && !resume_data_complete){
			
			try{
				dumpResumeDataToDisk(false, false);
				
			}catch( Exception e ){
				
				Debug.out( "Failed to dump initial resume data to disk" );
			}
		}
	}
	
	public void 
	dumpResumeDataToDisk(
		boolean savePartialPieces, 
		boolean invalidate )
	
		throws Exception
	{
		if(!useFastResume)
		  return;
    
			// if file caching is enabled then this is an important time to ensure that the cache is
			// flushed as we are going to record details about the accuracy of written data
		
		DiskManagerFileInfo[]	files = disk_manager.getFiles();
		
		for (int i=0;i<files.length;i++){
			
			files[i].flushCache();
		}
		
		boolean	was_complete = isTorrentResumeDataComplete( 
									torrent, 
									disk_manager.getDownloadManager().getTorrentSaveDir(), 
									disk_manager.getDownloadManager().getTorrentSaveFile());
		
		boolean[]	pieceDone	= disk_manager.getPiecesDone();

		//build the piece byte[] 
		byte[] resumeData = new byte[pieceDone.length];
		for (int i = 0; i < resumeData.length; i++) {
		  if (invalidate) resumeData[i] = (byte)0;
		  else resumeData[i] = pieceDone[i] ? (byte)1 : (byte)0;
		}

		//Attach the resume data
		Map resumeMap = new HashMap();
		torrent.setAdditionalMapProperty("resume", resumeMap);

	  Map resumeDirectory = new HashMap();
	  
	  	// We *really* shouldn't be using a localised string as a Map key (see bug 869749)
	  	// currently fixed by mangling such that decode works
	  
	  // System.out.println( "writing resume data: key = " + ByteFormatter.nicePrint(path));
	  
	  String resume_key = 
			torrent.isSimpleTorrent()?
				disk_manager.getDownloadManager().getTorrentSaveDir():
				disk_manager.getDownloadManager().getTorrentSaveDirAndFile();
		

	  resumeMap.put(resume_key, resumeDirectory);
	  
	  resumeDirectory.put("resume data", resumeData);
	  
	  Map partialPieces = new HashMap();
	
	  if (savePartialPieces  && !invalidate) {
	  	
	  		// get the pieces to save - peer manager's are most recent if they exist
	  	
	  	PEPeerManager	peer_manager = disk_manager.getPeerManager();
	  	
	  	if ( peer_manager != null ){
	  		
	  		PEPiece[]	pm_pieces = peer_manager.getPieces();
	  		
	  		if ( pm_pieces != null ){
	  			
	  			recovered_pieces	= pm_pieces;
	  		}
	  	}
	  	
	    if( recovered_pieces != null) {
	      
	      for (int i = 0; i < recovered_pieces.length; i++) {
	        PEPiece piece = recovered_pieces[i];
	        if (piece != null && piece.getCompleted() > 0) {
	          boolean[] downloaded = piece.getWritten();
	          List blocks = new ArrayList();
	          for (int j = 0; j < downloaded.length; j++) {
	            if (downloaded[j])
	              blocks.add(new Long(j));
	          }
	          partialPieces.put("" + i, blocks);
	        }
	      }
	      resumeDirectory.put("blocks", partialPieces);
	    }
	    resumeDirectory.put("valid", new Long(1));
	  } else {
	    resumeDirectory.put("valid", new Long(0));
	  }
		
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

	public static void
	setTorrentResumeDataComplete(
		TOTorrent	torrent,
		String		data_dir )
	{
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
		
		resumeMap.put(data_dir, resumeDirectory);
		
		resumeDirectory.put("resume data", resumeData);
		
		Map partialPieces = new HashMap();
		
		resumeDirectory.put("blocks", partialPieces);
		
		resumeDirectory.put("valid", new Long(1));	
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
		
		try{
			int	piece_count = torrent.getPieces().length;
		
			Map resumeMap = torrent.getAdditionalMapProperty("resume");
		
			if (resumeMap != null) {
			
					// see bug 869749 for explanation of this mangling
				
				String mangled_path;
				
				try{
					mangled_path = new String(resume_key.getBytes(Constants.DEFAULT_ENCODING),Constants.BYTE_ENCODING);
									
				}catch( Throwable e ){
					
					e.printStackTrace();
					
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
		
			e.printStackTrace();
		}	
		
		return( false );
	}
	
	private void 
	saveTorrent() 
	{
		try{
			TorrentUtils.writeToFile( torrent );
						
		} catch (TOTorrentException e) {
			
			e.printStackTrace();
		}
	}


}
