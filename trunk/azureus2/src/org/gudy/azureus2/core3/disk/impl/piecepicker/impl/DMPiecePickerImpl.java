/*
 * Created on 01-Aug-2004
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

package org.gudy.azureus2.core3.disk.impl.piecepicker.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.*;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.disk.impl.piecepicker.*;
import org.gudy.azureus2.core3.peer.impl.PEPieceImpl;
import org.gudy.azureus2.core3.util.SystemTime;

/**
 * @author parg
 *
 */

public class 
DMPiecePickerImpl
	implements DMPiecePicker, ParameterListener
{
	private static final long PRIORITY_COMPUTE_MIN	= 30*60*1000;
	
	private DiskManagerHelper		disk_manager;
	
	private boolean firstPiecePriority = COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
	private int		nbPieces;
	
	private boolean	has_piece_to_download;
	
	private volatile boolean	compute_priority_recalc_outstanding;
	
	private long	last_priority_computation;
	
	private BitSet[] priorityLists;
	
	private MyDiskManagerListener myDiskManListener;
	
	public
	DMPiecePickerImpl(
		DiskManagerHelper		_disk_manager )
	{
		disk_manager	= _disk_manager;
		
		nbPieces	= disk_manager.getNumberOfPieces();
		
		myDiskManListener = new MyDiskManagerListener();
	}
	
	/**
	 * An instance of this listener is registered at disk_manager.
	 * It updates the value returned by hasDownloadablePiece to reflect
	 * changes in file/piece priority values.
	 * @author Balazs Poka
	 */
	private class MyDiskManagerListener implements DiskManagerListener {

        public void stateChanged(int oldState, int newState) {
        }

        public void 
        filePriorityChanged(
        	DiskManagerFileInfo	file ) {
            
        	compute_priority_recalc_outstanding	= true;
        }


    	public void
    	pieceDoneChanged(
    		DiskManagerPiece	piece )
    	{
    		compute_priority_recalc_outstanding	= true;
    	}
    	
     	public void
    	fileAccessModeChanged(
    		DiskManagerFileInfo		file,
    		int						old_mode,
    		int						new_mode )
    	{	
    	}
	}
	
	public void
	start()
	{
		COConfigurationManager.addParameterListener("Prioritize First Piece", this);
    
		disk_manager.addListener(myDiskManListener);
		
		has_piece_to_download	= false;
        compute_priority_recalc_outstanding = true;
		
		
//		computePriorityIndicator();
	}
	
	public void
	stop()
	{
		COConfigurationManager.removeParameterListener("Prioritize First Piece", this);
		disk_manager.removeListener(myDiskManListener);
	}
	
	public void 
	parameterChanged(
		String parameterName ) 
	{
	   firstPiecePriority = COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
	}

/*
	public void 
	computePriorityIndicator() 
	{
			// this has been changed to be driven by explicit changes in file priority
			// and piece status rather than calculating every time.
			// however, it is unsynchronised so there is a very small chance that we'll
			// miss a recalc indication, hence the timer
		
		long	now = SystemTime.getCurrentTime();
		
		if ( 	compute_priority_recalc_outstanding ||
				now - last_priority_computation >= PRIORITY_COMPUTE_MIN ||
				now < last_priority_computation ){	// clock changed
						
			last_priority_computation			= now;
			compute_priority_recalc_outstanding	= false;
			
			DiskManagerPiece[]	pieces	= disk_manager.getPieces();
			
			for (int i = 0; i < pieceCompletion.length; i++) {
			  
			   //if the piece is already complete, skip computation
			   if (pieces[i].getDone()) {
			     pieceCompletion[i] = -1;
			     continue;
			   }
	      
				DMPieceList pieceList = disk_manager.getPieceList(i);
				int completion = -1;
				
				int size=pieceList.size();
				for (int k = 0; k < size; k++) {
					//get the piece and the file 
					DiskManagerFileInfoImpl fileInfo = (pieceList.get(k)).getFile();
					
					//If the file isn't skipped
					if(fileInfo.isSkipped()) {
						continue;
					}
	
					//if this is the first or last piece of the file
					if( firstPiecePriority && (i == fileInfo.getFirstPieceNumber() || i == fileInfo.getLastPieceNumber() ) ) {
					  if (fileInfo.isPriority()) completion = 99;
					  else if (completion < 97) completion = 97;
					}
	        
					//if the file is high-priority
					else if (fileInfo.isPriority()) {
					  if (completion < 98) completion = 98;
					}
					
					else {
					  if(completion < 0) completion = 0;
					}
				}
	      
				pieceCompletion[i] = completion;
			}
	
			// this clears and resizes all priorityLists to the
			// length of pieceCompletion
			for (int i = 0; i < priorityLists.length; i++) {
				BitSet list = priorityLists[i];
				if (list == null) {
					list = new BitSet(pieceCompletion.length);
				} else {
					list.clear();
				}
				priorityLists[i]=list;
			}
			
		
			has_piece_to_download	= false;
						
				// for all pieces, set the priority bits accordingly
			
			for (int i = 0; i < pieceCompletion.length; i++) {
				
				int priority = pieceCompletion[i];
				
				if ( priority >= 0 ){
					
					pieces[i].setNeeded( true );
					
					has_piece_to_download	= true;
					
					priorityLists[priority].set(i);
					
				}else{
					
					pieces[i].setNeeded( false );
				}
			}
		}
	}

	public int[] getPiecenumberToDownload(boolean[] pieceCandidates) {
		if (0 >=PriorityPiecesCnt)
		{	//cant do anything if no pieces to startup
			return null;
		}
		
		int startI;
		int direction;
		
		if (1 ==PriorityPiecesCnt)
		{
			startI =piecesBestStart;
			direction =1;
		} else
		{
			// Mix it up!
			startI =piecesBestStart +rand.nextInt(piecesBestEnd -piecesBestStart);
			direction =rand.nextBoolean() ?-1 :1;
		}
		
		//For every Priority piece
		for (i =startI; i >=piecesBestStart &&i <=piecesBestEnd ;i +=direction)
		{
			//is piece flagged and confirmed not in progress
			if (pieceCandidates[i] &&(null ==_pieces[i]))
			{
				// This should be a piece we want to start    
				PEPieceImpl piece =new PEPieceImpl(this, dm_pieces[i], peerSpeed /2, false);
				if (null !=piece)
				{
					//send the request ...
					blockNumber =piece.getBlock();
					if (0 <=blockNumber)
					{
						return new int[] {i, blockNumber};
					}
					piece =null;
				}
				// seems to be something wrong with memory; can't create a new piece
				System.runFinalization();
				//keep trying
			}
		}
		//suan le - hui ba
		System.gc();
		return null;
	}

	public boolean
	hasDownloadablePiece() 
	{
		return( has_piece_to_download );
	}  
*/

}
