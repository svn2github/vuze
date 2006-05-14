/*
 * Created on Mar 15, 2006
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
package com.aelitis.azureus.core.peermanager.unchoker;

import java.util.*;

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;


/**
 * 
 */
public class UnchokeManager {
	
	private static final int MIN_UNCHOKE_SLOTS = 4;  //3 normal + 1 optimistic
	private static final int OPTIMISTIC_SLOT_RATIO = 4;
	
	private static final int ROUNDS_PER_OPTIMISTIC_REFRESH = 3;  //assumes 10sec round, 30sec optimistic
	
	
	
	private static final UnchokeManager instance = new UnchokeManager();
	
	
	private final UnchokePriorityController priority_controller = new UnchokePriorityController();
	
	
	

	
	private final ArrayList unchoke_slots = new ArrayList();
	private int round_couunt = 0;
	
	
	private UnchokeManager() {
				
		resizeSlots();  //init slots
	}
	
	
	public static UnchokeManager getSingleton() {  return instance;  }
	
	
	
	public void registerForUnchokes( UnchokeHelper helper ) {
		priority_controller.registerHelper( helper );		
		
		
	}
	
	
  public void deregisterForUnchokes( UnchokeHelper helper ) {
		priority_controller.deregisterHelper( helper );
  	
	}
	
	
	
	
	
	protected void process() {
		round_couunt++;
		
		boolean optimistic_round = false;
		
		if( round_couunt == ROUNDS_PER_OPTIMISTIC_REFRESH ) {
			optimistic_round = true;
			round_couunt = 0;  //reset
		}
		
		if( resizeSlots() ) {	//resize number of unchoke slots
			fillSlots();  //attach unchoke peers
		}
	}
	
	
	
	
	private void fillSlots() {		
		//go thru slots, looking for empty ones to attach opt peers to
		for( int i=0; i < unchoke_slots.size(); i++ ) {			
			UnchokeSlot slot = (UnchokeSlot)unchoke_slots.get( i );
			
			if( !slot.isAttached() ) {  //is empty
				UnchokeHelper helper = priority_controller.getNextOptimisticHelper();  //attach next optimistic peer from global pool
				
				PEPeerTransport peer = UnchokerUtil.getNextOptimisticPeer( helper.getAllPeers(), !helper.isSeeding(), !helper.isSeeding() );
				//TODO send unchoke???
				slot.attachPeer( peer );
			}
		}		
	}
	
	
	
	
	private boolean resizeSlots() {
		//TODO calculate optimal number of slots		
		int slots_needed = MIN_UNCHOKE_SLOTS;
		
		int slots_diff = slots_needed - unchoke_slots.size();
		
		if( slots_diff > 0 ) {  //add more slots
			for( int i=0; i < slots_diff; i++ ) {				
				boolean opt = unchoke_slots.size() % OPTIMISTIC_SLOT_RATIO == 0;				
				unchoke_slots.add( new UnchokeSlot( opt ? UnchokeSlot.UNCHOKE_TYPE_OPTIMISTIC : UnchokeSlot.UNCHOKE_TYPE_NORMAL ) );				
			}
			
			return true;  //change im number of slots
		}
		
		if( slots_diff < 0 ) {   //remove excess slots
			for( int i=slots_diff; i < 0; i++ ) {
				UnchokeSlot slot = (UnchokeSlot)unchoke_slots.remove( unchoke_slots.size() - 1 );
				slot.detachPeer();
				
				//TODO send choke?
			}

			return true;  //change im number of slots
		}
		
		return false;  //no change in number of slots
	}
	

}
