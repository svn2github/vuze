/*
 * Created on 05-Jul-2004
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

package org.gudy.azureus2.core3.ipfilter.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import java.net.UnknownHostException;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;

public class 
IPAddressRangeManager 
{
	protected Map		entries = new HashMap();
	
	protected boolean	rebuild_required;
	
	protected entry[]	merged_entries	= new entry[0];
	
	protected AEMonitor	this_mon	= new AEMonitor( "IPAddressRangeManager" );

	public void
	addRange(
		String	start,
		String	end,
		Object	user_data )
	{
		try{
			this_mon.enter();
			
			long	s = addressToInt( start );
			
			if ( s < 0 ){
				
				s += 0x100000000L;
			}
			long	e = addressToInt( end );
			
			if ( e < 0 ){
				
				e += 0x100000000L;
			}
			
			addRange( s, e,user_data);
							
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	addRange(
		long	start_int,
		long	end_int,
		Object	user_data )
	{
		try{
			this_mon.enter();
		
			rebuild_required	= true;
			
			entries.put( user_data, new entry( start_int, end_int, user_data ));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeRange(
		Object		user_data )
	{
		try{
			this_mon.enter();
		
			entries.remove( user_data );
		
			rebuild_required	= true;
		}finally{
			
			this_mon.exit();
		}
	}
	
	public Object
	isInRange(
		String	ip )
	{
		try{
			this_mon.enter();
			
			long i = addressToInt( ip );
			
			if ( i < 0 ){
				
				i += 0x100000000L;
			}
			
			Object res = isInRange( i );
			
			// LGLogger.log( "IPAddressRangeManager: checking '" + ip + "' against " + entries.size() + "/" + merged_entries.length + " -> " + res );
			
			return( res );
						
		}finally{
			
			this_mon.exit();
		}
	}
	
	public Object
	isInRange(
		long	ip_int )
	{
		try{
			this_mon.enter();
		
			if ( rebuild_required ){
				
				rebuild_required	= false;
				
				rebuild();
			}
					
			if ( merged_entries.length == 0 ){
				
				return( null );
			}
				
				// assisted binary chop 
			
			int	bottom 	= 0;
			int	top		= merged_entries.length-1;
			
			int	current	= -1;
			
			while( top >= 0 && bottom < merged_entries.length && bottom <= top){
				
				current = (bottom+top)/2;
				
				entry	e = merged_entries[current];
				
				long	this_start 	= e.getStart();
				long this_end	= e.getMergedEnd();
				
				if ( ip_int == this_start ){
					
					break;
					
				}else if ( ip_int > this_start ){
					
					if ( ip_int <= this_end ){
						
						break;
					}
					
						// lies to the right of this entry
					
					bottom	= current + 1;
					
				}else if ( ip_int == this_end ){
					
					break;
					
				}else{
					// < this_end
					
					if ( ip_int >= this_start ){
						
						break;
					}
					
					top = current - 1;
				}
			}
			
			if ( top >= 0 && bottom < merged_entries.length && bottom <= top ){
	
				entry	e = merged_entries[current];
			
				if ( ip_int <= e.getEnd()){
					
					return( e.getUserData());
				}
				
				List	merged = e.getMergedEntries();
				
				if ( merged == null ){
					
					Debug.out( "IPAddressRangeManager: inconsistent merged details - no entries" );
					
					return( null );
				}
				
				for (int i=0;i<merged.size();i++){
					
					entry	me = (entry)merged.get(i);
					
					if ( me.getStart() <= ip_int && me.getEnd() >= ip_int ){
						
						return( me.getUserData());
					}
				}
				
				Debug.out( "IPAddressRangeManager: inconsistent merged details - entry not found" );
			}
			
			return( null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected int
	addressToInt(
		String		address )
	{
		try{
			return( PRHelpers.addressToInt( address ));
			
		}catch( UnknownHostException e ){
			
			return( UnresolvableHostManager.getPseudoAddress( address ));
		}
	}
	
	protected void
	rebuild()
	{
		Collection col = entries.values();
		
		entry[]	ents = new entry[col.size()];
		
		col.toArray(ents);
		
		for (int i=0;i<ents.length;i++){
			
			ents[i].reset();
		}
		
			// sort based on start address
		
		Arrays.sort( 
			ents,
			new Comparator()
			{
				public int 
				compare(
					Object o1, 
					Object o2 )
				{
					entry	e1 = (entry)o1;
					entry 	e2 = (entry)o2;
					
					if ( e1.getStart() < e2.getStart()){
						return( -1 );
					}else if ( e1.getStart() > e2.getStart()){
						return( 1 );
					}else{
						long l = e2.getEnd() - e1.getEnd();
						
						if ( l < 0 ){
							
							return( -1 );
							
						}else if ( l > 0 ){
							
							return( 1 );
							
						}else{
							return( 0 );
						}
					}
				}
				
				public boolean 
				equals(Object obj)
				{
					return( false );
				}
			});
		
			// now merge overlapping ranges
		
		List me = new ArrayList( ents.length );
		
		for (int i=0;i<ents.length;i++){
			
			entry	entry = ents[i];
			
			if ( entry.getMerged()){
				
				continue;
			}
			
			me.add( entry );
			
			int	pos = i+1;
			
			while( pos < ents.length ){
				
				long	end_pos = entry.getMergedEnd();
				
				entry	e2 = ents[pos++];
				
				if (!e2.getMerged()){
					
					if ( end_pos >= e2.getStart()){
						
						e2.setMerged();
						
						if ( e2.getEnd() > end_pos ){
							
							entry.setMergedEnd( e2.getEnd());
							
							entry.addMergedEntry( e2 );
						}
					}else{
						
						break;
					}	
				}
			}
		}
		
		/*
		for (int i=0;i<ents.length;i++){
			
			entry	e = ents[i];
			
			System.out.println( Long.toHexString(e.getStart()) + " - " + Long.toHexString(e.getEnd()) + ": " + e.getMerged() + "/" + Long.toHexString(e.getMergedEnd()));
		}
		*/
		
		merged_entries = new entry[me.size()];
		
		me.toArray( merged_entries );
		
			//	System.out.println( "non_merged = " + merged_entries.length );
	}
	
	protected class
	entry
	{
		protected long		start;
		protected long		end;
		protected Object	user_data;
		
		protected boolean	merged;
		protected long		merged_end;
		
		protected List		my_merged_entries;
		
		protected
		entry(
			long		_start,
			long		_end,
			Object		_ud )
		{
			start		= _start;
			end			= _end;
			user_data	= _ud;
		}
		
		protected long
		getStart()
		{
			return( start );
		}
		
		protected long
		getEnd()
		{
			return( end );
		}
		
		protected void
		setMergedEnd(
			long		_merged_end )
		{
			merged_end	= _merged_end;
		}
		
		protected long
		getMergedEnd()
		{
			return( merged_end );
		}
		
		protected Object
		getUserData()
		{
			return( user_data );
		}
		
		protected boolean
		getMerged()
		{
			return( merged );
		}
		
		protected void
		setMerged()
		{
			merged	= true;
		}
	
		protected void
		addMergedEntry(
			entry	e2 )
		{
			if ( my_merged_entries == null ){
				
				my_merged_entries = new ArrayList(1);
			}
			
			my_merged_entries.add(e2);
		}
		
		protected List
		getMergedEntries()
		{
			return( my_merged_entries );
		}
		
		protected void
		reset()
		{
			merged		= false;
			merged_end	= end;
		}
	}
	
	
	
	public static void
	main(
		String[]	args )
	{
		IPAddressRangeManager manager = new IPAddressRangeManager();
		
		
		manager.addRange( "3.1.1.1", "3.1.1.2", "1" );
		manager.addRange( "1.1.1.1", "2.2.2.2", "2" );
		manager.addRange( "0.1.1.1", "2.2.2.2", "3" );
		manager.addRange( "1.1.1.1", "1.2.2.2", "4" );
		manager.addRange( "7.7.7.7", "7.7.8.7", "5" );
		manager.addRange( "8.8.8.8", "8.8.8.8", "6" );
		//manager.addRange( "0.0.0.0", "255.255.255.255", "7" );
		manager.addRange( "5.5.5.5", "6.6.6.9", "8" );
		manager.addRange( "6.6.6.6", "7.7.0.0", "9" );
		
		
		System.out.println( "inRange -> " + manager.isInRange( "6.6.6.8" ));
		
		/*
		for (int i=0;i<100000;i++){
			
			int	start 	= (int)(Math.random() * 0xfffff000);
			int	end		= start + (int)(Math.random()*5000);
			
			manager.addRange( start, end, new Object());
		}
		*/
		/*
		int	num 	= 0;
		int	hits	= 0;
		
		while(true){
			
			num++;
		
			if ( num % 1000 == 0 ){
				
				System.out.println( num + "/" + hits );
				
			}
			
			int	ip 	= (int)(Math.random() * 0xffffffff);

			Object	res = manager.isInRange( ip );
			
			if ( res != null ){
				
				hits++;
			}
		}
		*/
	}
}
