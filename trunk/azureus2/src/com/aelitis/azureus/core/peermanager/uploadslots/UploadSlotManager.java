/*
 * Created on Jul 15, 2006
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

import org.gudy.azureus2.core3.util.Debug;




/**
 * 
 */
public class UploadSlotManager {
	
	private static final int EXPIRE_NORMAL			= 1;   //1 round = 10sec upload
	private static final int EXPIRE_OPTIMISTIC	= 3;   //3 rounds = 30sec upload
	private static final int EXPIRE_SEED				= 6;   //6 rounds = 60sec upload
	
	
	private static final UploadSlotManager instance = new UploadSlotManager();
	
	public static UploadSlotManager getSingleton() {  return instance;  }
	
	
	private final UploadSessionPicker picker = new UploadSessionPicker();
	
	//init with empty slots, optimistic first in line
	private final UploadSlot[] slots = new UploadSlot[] {	new UploadSlot( UploadSlot.TYPE_OPTIMISTIC ),  //TODO dynamic # of slots
																												new UploadSlot( UploadSlot.TYPE_NORMAL ),
																												new UploadSlot( UploadSlot.TYPE_NORMAL ),
																												new UploadSlot( UploadSlot.TYPE_NORMAL ) };
	

	
	private long current_round = 0;
	
	
	
	private UploadSlotManager() {
		/*nothing*/
	}
	
	
	
	public void registerHelper( UploadHelper helper ) {
		picker.registerHelper( helper );
	}
	
	
	public void deregisterHelper( UploadHelper helper ) {
		picker.deregisterHelper( helper );
	}
	
	
	/**
	 * Notify of helper state change (i.e. priority changed)
	 * @param helper
	 */
	public void updateHelper( UploadHelper helper ) {
		picker.updateHelper( helper );
	}
	
	
	
	
	public void process() {
		current_round++;
		
		ArrayList to_stop = new ArrayList();
		ArrayList to_start = new ArrayList();
		
		//get a list of the best sessions, peers who are uploading to us in download mode
		LinkedList best_sessions = picker.pickBestDownloadSessions( slots.length );		
		
		//go through all currently expired slots and pick sessions for next round
		for( int i=0; i < slots.length; i++ ) {
			UploadSlot slot = slots[i];
			
			if( slot.getExpireRound() <= current_round ) {  //expired			
				UploadSession session = slot.getSession();
				
				to_stop.add( session );  //make sure it gets stopped
				
				if( slot.getSlotType() == UploadSlot.TYPE_OPTIMISTIC ) {					
					//pick new session for optimistic upload
					session = picker.pickNextOptimisticSession();					
					if( session == null ) {  Debug.outNoStack( "session == null" );  continue;  }
					
					if( session.getSessionType() == UploadSession.TYPE_SEED ) {  //place first seed session in a normal slot
						best_sessions.addFirst( session );  //put at front of good list to ensure it gets picked						
						//pick a new optimistic session, whatever type
						session = picker.pickNextOptimisticSession();						
						if( session == null ) {  Debug.outNoStack( "session == null" );  continue;  }
					}
					
					slot.setSession( session );  //place the new session in the slot
					slot.setExpireRound( current_round + EXPIRE_OPTIMISTIC );  //set the new expire time
				}
				else {  //normal					
					if( best_sessions == null || best_sessions.isEmpty() ) {  //no download mode peers, must be only seeding
						Debug.outNoStack( "best_sessions.isEmpty()" );						
						session = picker.pickNextOptimisticSession();
						if( session == null ) {  Debug.outNoStack( "session == null" );  continue;  }			
					}
					else {
						session = (UploadSession)best_sessions.removeFirst();
					}
					
					slot.setSession( session );  //place the session in the slot
					slot.setExpireRound( current_round + ( session.getSessionType() == UploadSession.TYPE_SEED ? EXPIRE_SEED : EXPIRE_NORMAL ) );  //set the new expire time
				}
				
				to_start.add( session );  //make sure it gets started			
				
			}
		}
				
		//start and stop sessions for the round
	
		//filter out sessions allowed to continue another round, so we don't stop-start them
		for( Iterator it1 = to_stop.iterator(); it1.hasNext(); ) {
			UploadSession stop_s = (UploadSession)it1.next();
			
			for( Iterator it2 = to_start.iterator(); it2.hasNext(); ) {
				UploadSession start_s = (UploadSession)it2.next();
				if( stop_s.isSameSession( start_s ) ) {  //need to do this because two session objects can represent the same peer
					it1.remove();
					break;
				}
			}			
		}
		
		//stop discontinued sessions
		for( Iterator it = to_stop.iterator(); it.hasNext(); ) {  
			UploadSession session = (UploadSession)it.next();
			session.stop();
		}

		//ensure sessions are started
		for( Iterator it = to_start.iterator(); it.hasNext(); ) {  
			UploadSession session = (UploadSession)it.next();
			session.start();
		}
		
		printSlotStats();		
	}
	
	
	
	private void printSlotStats() {		
		System.out.println( "UPLOAD SLOTS [" +current_round+ "x]:" );
		
		for( int i=0; i < slots.length; i++ ) {
			UploadSlot slot = slots[i];
			
			System.out.print( "[" +i+ "]: " );
			
			String slot_type = slot.getSlotType() == UploadSlot.TYPE_NORMAL ? "NORMAL" : "OPTIMISTIC";
			
			String remaining = " " + (slot.getExpireRound() - current_round) + "rem";
			
			System.out.println( slot_type + remaining+ " : " +slot.getSession().getStatsTrace() );			
		}		
	}
	
	
	
	

}
