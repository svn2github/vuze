/*
 * Created on Jul 17, 2006
 * Created by Alon Rohter
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.peermanager.uploadslots;

import java.util.*;

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.unchoker.*;



/**
 * 
 */
public class UploadSessionPicker {

	private final LinkedList next_optimistics = new LinkedList();
	private final AEMonitor next_optimistics_mon = new AEMonitor( "UploadSessionPicker" );
	
	private final LinkedList helpers = new LinkedList();
	
	
	private final DownloadingUnchoker down_unchoker = new DownloadingUnchoker();  //TODO extract only the code actually used
	

	protected UploadSessionPicker() {
		/*nothing*/
	}
	
	
	
	protected void registerHelper( UploadHelper helper ) {
		try {  next_optimistics_mon.enter();
			helpers.add( helper );
		
			int priority = helper.getPriority();
		
			//the higher the priority, the more optimistic unchoke chances they get
			for( int i=0; i < priority; i++ ) {
				insertHelper( helper );
			}
		}
		finally {  next_optimistics_mon.exit();  }
	}
	
	
	
	protected void deregisterHelper( UploadHelper helper ) {
		try {  next_optimistics_mon.enter();
			helpers.remove( helper );
		
			boolean rem = next_optimistics.removeAll( Collections.singleton( helper ) );		
			if( !rem ) Debug.out( "!rem" );  //TODO
		}
		finally {  next_optimistics_mon.exit();  }
	}
	
	
	
	protected void updateHelper( UploadHelper helper ) {
		try {  next_optimistics_mon.enter();
			int priority = helper.getPriority();  //new priority
		
			int count = 0;
		
			for( Iterator it = next_optimistics.iterator(); it.hasNext(); ) {
				UploadHelper h = (UploadHelper)it.next();
				if( h == helper ) {
					count++;
				
					if( count > priority ) {  //new priority is lower
						it.remove();  //trim
					}
				}
			}
		
			if( count < priority ) {  //new priority is higher
				for( int i=count; i < priority; i++ ) {
					insertHelper( helper );  //add
				}
			}
		}
		finally {  next_optimistics_mon.exit();  }
	}
	
	
	
	private void insertHelper( UploadHelper helper ) {
		int pos = RandomUtils.nextInt( next_optimistics.size() + 1 );  //pick a random location
		next_optimistics.add( pos, helper );  //store into location
	}
	
	
	
	private UploadHelper getNextOptimisticHelper() {
		try {  next_optimistics_mon.enter();
			UploadHelper helper = (UploadHelper)next_optimistics.removeFirst();
			next_optimistics.addLast( helper );
			return helper;
		}
		finally {  next_optimistics_mon.exit();  }
	}
	
	
	
	
	//this picks both downloading and seeding sessions
	protected UploadSession pickNextOptimisticSession() {
		UploadHelper helper = getNextOptimisticHelper();		
		
		PEPeerTransport peer;
		
		if( helper.isSeeding() ) {
			peer = UnchokerUtil.getNextOptimisticPeer( helper.getAllPeers(), false, false );  //TODO use simple FIFO RR for seeding picks
		}
		else {
			peer = UnchokerUtil.getNextOptimisticPeer( helper.getAllPeers(), true, true );
		}
		
		if( peer == null ) {
			Debug.outNoStack( "peer == null" );
			return null;
		}
		
		UploadSession session = new UploadSession( peer, helper.isSeeding() ? UploadSession.TYPE_SEED : UploadSession.TYPE_DOWNLOAD );
		
		return session;
	}
	
	
	

	private ArrayList globalGetAllDownloadPeers() {
		try {  next_optimistics_mon.enter();
			ArrayList all = new ArrayList();
			
			for( Iterator it = helpers.iterator(); it.hasNext(); ) {
				UploadHelper helper = (UploadHelper)it.next();
				
				if( !helper.isSeeding() )  {  //filter out seeding
					all.addAll( helper.getAllPeers() );
				}
			}
			
			return all;
		}
		finally {  next_optimistics_mon.exit();  }
	}
	
	
	
	//this picks downloading sessions only
	protected LinkedList pickBestDownloadSessions( int max_sessions ) {
		//TODO use download priority in best calculation?
		
		ArrayList all_peers = globalGetAllDownloadPeers();
		
		if( all_peers.isEmpty() ) {
			Debug.outNoStack( "all_peers.isEmpty()" );
			return null;
		}
		
		down_unchoker.calculateUnchokes( max_sessions, all_peers, false );  //TODO extract only the code actually used
		
		ArrayList best = down_unchoker.getUnchokes();
		
		if( best.size() != max_sessions ) {
			Debug.outNoStack( "best.size()[" +best.size()+ "] != max_sessions[" +max_sessions+ "]" );
		}
		
		if( best.isEmpty() ) {
			Debug.outNoStack( "best.isEmpty()" );
			return null;
		}
		
		
		LinkedList best_sessions = new LinkedList();
		
		for( Iterator it = best.iterator(); it.hasNext(); ) {
			PEPeerTransport peer = (PEPeerTransport)it.next();
			UploadSession session = new UploadSession( peer, UploadSession.TYPE_DOWNLOAD );
			best_sessions.add( session );
		}
		
		return best_sessions;
	}
	
	
	
	
}
