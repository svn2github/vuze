/*
 * Created on 08-Aug-2004
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

package org.gudy.azureus2.core3.disk.impl.access.impl;

import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.disk.DiskManagerFactory;

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.disk.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.*;

/**
 * @author parg
 *
 */

public class 
Test 
{
	protected
	Test()
	{
		try{
			File	file = File.createTempFile("AZU", null );

			file.deleteOnExit();
			
			RandomAccessFile raf = new RandomAccessFile( file, "rwd" );
			
			raf.setLength( 10*1024*1024 );

			raf.close();
			
			TOTorrent	torrent = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( file, new URL("http://dummy/" ));
			
			DiskManager dm = DiskManagerFactory.create( torrent, null );
			
			dm.start();
			
		}catch( Throwable e ){
			
			
		}
	}
	public static void
	main(
		String[]	args )
	{
		new Test();
	}

	/*
	protected class
	DiskManagerDummy
		implements DiskManagerHelper
	{
		public void
		start()
		{
		}
		
		public boolean
		filesExist()
		{
			
		}

		public void 
		setPeerManager(
			PEPeerManager manager )
		{
			
		}


		public void 
		writeBlock(
			int 		pieceNumber, 
			int 		offset, 
			DirectByteBuffer 	data,
			PEPeer sender)
		{
			
		}


		public boolean 
		checkBlock(
			int 		pieceNumber, 
			int 		offset, 
			DirectByteBuffer 	data )
		{
			
		}


		public boolean 
		checkBlock(
			int pieceNumber, 
			int offset, 
			int length )
		{
			
		}

			
		public DiskManagerRequest
		createRequest(
			int pieceNumber,
			int offset,
			int length );
		

		public void enqueueReadRequest( DiskManagerRequest request, ReadRequestListener listener );

		public void
		stopIt();

		public void
	    dumpResumeDataToDisk(
	    	boolean savePartialPieces, 
			boolean invalidate )
			
			throws Exception;

		public void
		computePriorityIndicator();
		
		public PEPiece[] 
		getRecoveredPieces();

		public void
		aSyncCheckPiece(
			int	pieceNumber );
	  
		public int 
		getPieceNumberToDownload(
			boolean[] 	_piecesRarest );
		
		public boolean[] 
		getPiecesDone();

		public int 
		getNumberOfPieces();

		public DiskManagerFileInfo[]
		getFiles();

		public int
		getState();
		
		public long
		getTotalLength();
		
		public int
		getPieceLength();
		
		public int 
		getLastPieceLength();

		public long
		getRemaining();
		
		public int
		getPercentDone();
		
		public String
		getErrorMessage();
	  
		public String
		moveCompletedFiles();

		public boolean isChecking();
	  
	   
		public void
		addListener(
			DiskManagerListener	l );
		
		public void
		removeListener(
			DiskManagerListener	l );
	  
	  
	  public void storeFilePriorities();
	  
	  public DownloadManager getDownloadManager();
	  
	  public PEPeerManager getPeerManager();
		
		
		public PieceList
		getPieceList(
			int	piece_number )
		{
			return(null);
		}
		
		public byte[]
		getPieceHash(
			int	piece_number )
		{
			return(null);
		}
		
		public void
		setState(
			int	state )
		{
		}
		
		public void
		setErrorMessage(
			String	str )
		{
		}
		
		public long
		getAllocated()
		{
			return(0);
			
		}
		
		public void
		setAllocated(
			long		num )
		{
		}

		
		public void
		setRemaining(
			long		num )
		{
		}

		
		public void
		setPercentDone(
			int			num )
		{
		}

		
		public void 
		computeFilesDone(
			int pieceNumber )
		{
		}

		
		public TOTorrent
		getTorrent()
		{
			return(null);
		}

	}
*/
}
