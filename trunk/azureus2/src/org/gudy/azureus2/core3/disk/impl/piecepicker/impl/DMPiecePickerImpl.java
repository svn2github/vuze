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
import org.gudy.azureus2.core3.disk.impl.*;
import org.gudy.azureus2.core3.disk.impl.piecepicker.*;

/**
 * @author parg
 *
 */

public class 
DMPiecePickerImpl
	implements DMPiecePicker, ParameterListener
{
	protected DiskManagerHelper		disk_manager;
	
	protected boolean firstPiecePriority = COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
    
	protected int	nbPieces;
	protected int 	pieceCompletion[];
	
	protected BitSet[] priorityLists;
	
	//private int[][] priorityLists;

	public
	DMPiecePickerImpl(
		DiskManagerHelper		_disk_manager )
	{
		disk_manager	= _disk_manager;
		
		nbPieces	= disk_manager.getNumberOfPieces();
	}
	
	public void
	start()
	{
		COConfigurationManager.addParameterListener("Prioritize First Piece", this);
		
		pieceCompletion = new int[nbPieces];
		
		priorityLists = new BitSet[100];
		
		//    priorityLists = new int[10][nbPieces + 1];

		// the piece numbers for getPiecenumberToDownload
		//    _priorityPieces = new int[nbPieces + 1];

	}
	
	public void
	stop()
	{
		COConfigurationManager.removeParameterListener("Prioritize First Piece", this);
	}
	
	public void 
	parameterChanged(
		String parameterName ) 
	{
	   firstPiecePriority = COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
	}
	
	public void 
	computePriorityIndicator() 
	{
		boolean[]	piecesDone	= disk_manager.getPiecesDone();
		
		for (int i = 0; i < pieceCompletion.length; i++) {
		  
		   //if the piece is already complete, skip computation
		   if (piecesDone[i]) {
		     pieceCompletion[i] = -1;
		     continue;
		   }
      
			PieceList pieceList = disk_manager.getPieceList(i);
			int completion = -1;
			
			for (int k = 0; k < pieceList.size(); k++) {
				//get the piece and the file 
				DiskManagerFileInfoImpl fileInfo = (pieceList.get(k)).getFile();
				
				//If the file isn't skipped
				if(fileInfo.isSkipped()) {
					continue;
				}

				//if this is the first piece of the file
				if (firstPiecePriority && i == fileInfo.getFirstPieceNumber()) {
				  if (fileInfo.isPriority()) completion = 99;
				  else completion = 97;
				}
        
        //if the file is high-priority
				else if (fileInfo.isPriority()) {
				  completion = 98;
				}
				
				//If the file is started but not completed
				else {
				  int percent = 0;
				  if (fileInfo.getLength() != 0) {
				    percent = (int) ((fileInfo.getDownloaded() * 100) / fileInfo.getLength());
				  }
				  if (percent < 100) {
				    completion = percent;
				  }
				}
			}
      
			pieceCompletion[i] = completion;
		}

		for (int i = 0; i < priorityLists.length; i++) {
			BitSet list = priorityLists[i];
			if (list == null) {
				list = new BitSet(pieceCompletion.length);
			} else {
				list.clear();
			}
			priorityLists[i]=list;
		}
		
		int priority;
		for (int j = 0; j < pieceCompletion.length; j++) {
			priority = pieceCompletion[j];
			if (priority >= 0) {
				priorityLists[priority].set(j);
			}
		}
	}

	 /*
	// searches from 0 to searchLength-1
    public static int binarySearch(int[] a, int key, int searchLength) {
		int low = 0;
		int high = searchLength - 1;

		while (low <= high) {
			int mid = (low + high) >> 1;
			int midVal = a[mid];

			if (midVal < key)
				low = mid + 1;
			else if (midVal > key)
				high = mid - 1;
			else
				return mid; // key found
		}
		return - (low + 1); // key not found.
	}
  */

	public int getPiecenumberToDownload(boolean[] _piecesRarest) {
		//Added patch so that we try to complete most advanced files first.
		List _pieces = new ArrayList();
    
		for (int i = 99; i >= 0; i--) {

		  if (priorityLists[i].isEmpty()) {
		    //nothing is set for this priority, so skip
		    continue;
		  }
		  
		  //Switch comments to enable sequential piece picking.
		  //int k = 0;
		  //for (int j = 0; j < nbPieces && k < 50; j++) {
      
		  for (int j = 0; j < nbPieces ; j++) {
		    if (_piecesRarest[j] && priorityLists[i].get(j)) {
		      _pieces.add( FlyWeightInteger.getInteger(j) );
		      //k++;
		    }
		  }
		  
		  if (_pieces.size() != 0) {
				break;
		  }
		}

		if (_pieces.size() == 0) {
		  return -1;
		}

		return ((Integer)_pieces.get((int) (Math.random() * _pieces.size()))).intValue();
	}

	/*
	  public int getPiecenumberToDownload(boolean[] _piecesRarest) {
		int pieceNumber;
		//Added patch so that we try to complete most advanced files first.
		_priorityPieces[nbPieces] = 0;
		for (int i = priorityLists.length - 1; i >= 0; i--) {
		  for (int j = 0; j < nbPieces; j++) {
			if (_piecesRarest[j] && binarySearch(priorityLists[i], j, priorityLists[i][nbPieces]) >= 0) {
			  _priorityPieces[_priorityPieces[nbPieces]++] = j;
			}
		  }
		  if (_priorityPieces[nbPieces] != 0)
			break;
		}
      
		if (_priorityPieces[nbPieces] == 0)
		  System.out.println("Size 0");
      
		int nPiece = (int) (Math.random() * _priorityPieces[nbPieces]);
		pieceNumber = _priorityPieces[nPiece];
		return pieceNumber;
	  }
	*/
	
	private static class FlyWeightInteger {
	    private static Integer[] array = new Integer[1024];

	    final static Integer getInteger(final int value) {
	      Integer tmp = null;
	      
	      if (value >= array.length) {
	        Integer[] arrayNew = new Integer[value + 256];
	        System.arraycopy(array, 0, arrayNew, 0, array.length);
	        array = arrayNew;
	      }
	      else {
	        tmp = array[value];
	      }
	      
	      if (tmp == null) {
	        tmp = new Integer(value);
	        array[value] = tmp;
	      }
	      
	      return tmp;
	    }
		}
	  
	  
}
