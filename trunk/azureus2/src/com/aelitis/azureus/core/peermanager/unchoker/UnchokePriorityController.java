/*
 * Created on Mar 20, 2006
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

import org.gudy.azureus2.core3.util.*;

/**
 * 
 */
public class UnchokePriorityController {
	
	private final LinkedList next_optimistics = new LinkedList();
	private final AEMonitor next_optimistics_mon = new AEMonitor( "UnchokePriorityController" );
	
	
	protected UnchokePriorityController() {
		
	}
	
	
	protected void registerHelper( UnchokeHelper helper ) {
		try {  next_optimistics_mon.enter();
			int priority = helper.getPriority();
		
			//the higher the priority, the more optimistic unchoke chances they get
			for( int i=0; i < priority; i++ ) {
				insertHelper( helper );
			}
		}
		finally {  next_optimistics_mon.exit();  }
	}
	
	
	protected void deregisterHelper( UnchokeHelper helper ) {
		try {  next_optimistics_mon.enter();
			boolean rem = next_optimistics.removeAll( Collections.singleton( helper ) );		
			if( !rem ) Debug.out( "!rem" );  //TODO
		}
		finally {  next_optimistics_mon.exit();  }
	}
	
	
	protected void updateHelper( UnchokeHelper helper ) {
		try {  next_optimistics_mon.enter();
			int priority = helper.getPriority();  //new priority
		
			int count = 0;
		
			for( Iterator it = next_optimistics.iterator(); it.hasNext(); ) {
				UnchokeHelper h = (UnchokeHelper)it.next();
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
	
	
	
	private void insertHelper( UnchokeHelper helper ) {
		int pos = RandomUtils.nextInt( next_optimistics.size() + 1 );  //pick a random location
		next_optimistics.add( pos, helper );  //store into location
	}
	
	
	protected UnchokeHelper getNextOptimisticHelper() {
		try {  next_optimistics_mon.enter();
			UnchokeHelper helper = (UnchokeHelper)next_optimistics.removeFirst();
			next_optimistics.addLast( helper );
			return helper;
		}
		finally {  next_optimistics_mon.exit();  }
	}
	
}
