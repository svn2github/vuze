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

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;

public class 
IPAddressRangeManager 
{
	protected Map		entries = new HashMap();
	
	protected long		total_span;
	
	protected boolean	rebuild_required;
	protected long		last_rebuild_time;
	
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
			
			int	s = addressToInt( start );
			
			int	e = addressToInt( end );
			
			addRange( s, e,user_data);
							
		}finally{
			
			this_mon.exit();
		}
	}
	
	private void
	addRange(
		int		start_int,
		int		end_int,
		Object	user_data )
	{
		try{
			this_mon.enter();
		
				// see if entry is already there
			
			entry	old_entry = (entry)entries.get( user_data );

			if( old_entry != null ){
				
				if ( old_entry.getStartInt() == start_int && old_entry.getEndInt() == end_int ){
				
						// no change, bail out
					
					return;
				}
				
				old_entry.setStartInt( start_int );
				
				old_entry.setEndInt( end_int );
				
			}else{
				
				entries.put( user_data, new entry( start_int, end_int, user_data ));
			}	
			
			rebuild_required	= true;
			
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
			
			long address_long = addressToInt( ip );
			
			if ( address_long < 0 ){
				
				address_long += 0x100000000L;
			}
			
			Object res = isInRange( address_long );
			
			// LGLogger.log( "IPAddressRangeManager: checking '" + ip + "' against " + entries.size() + "/" + merged_entries.length + " -> " + res );
			
			return( res );
						
		}finally{
			
			this_mon.exit();
		}
	}
	
	public boolean
	isInRange(
		Object		user_data,
		String		address )
	{
		try{
			this_mon.enter();
			
			long	address_long	= PRHelpers.addressToInt( address );
				
	    	if ( address_long < 0 ){
	     		
	    		address_long += 0x100000000L;
	     	}
	
			entry	e = (entry)entries.get( user_data );
			
			if ( e == null ){
				
				return( false );
			}
			
			return( address_long >= e.getStartLong() && address_long <= e.getEndLong());

		}catch( UnknownHostException e ){
			
			return( false );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected Object
	isInRange(
		long	address_long )
	{
		try{
			this_mon.enter();
			
			checkRebuild();
			
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
				
				long	this_start 	= e.getStartLong();
				long 	this_end	= e.getMergedEndLong();
				
				if ( address_long == this_start ){
					
					break;
					
				}else if ( address_long > this_start ){
					
					if ( address_long <= this_end ){
						
						break;
					}
					
						// lies to the right of this entry
					
					bottom	= current + 1;
					
				}else if ( address_long == this_end ){
					
					break;
					
				}else{
					// < this_end
					
					if ( address_long >= this_start ){
						
						break;
					}
					
					top = current - 1;
				}
			}
			
			if ( top >= 0 && bottom < merged_entries.length && bottom <= top ){
	
				entry	e = merged_entries[current];
			
				if ( address_long <= e.getEndLong()){
					
					return( e.getUserData());
				}
				
				entry[]	merged = e.getMergedEntries();
				
				if ( merged == null ){
					
					Debug.out( "IPAddressRangeManager: inconsistent merged details - no entries" );
					
					return( null );
				}
				
				for (int i=0;i<merged.length;i++){
					
					entry	me = (entry)merged[i];
					
					if ( me.getStartLong() <= address_long && me.getEndLong() >= address_long ){
						
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
	checkRebuild()
	{
		try{
			this_mon.enter();
		
			if ( rebuild_required ){
				
					// with substantial numbers of filters (e.g. 80,000) rebuilding
					// is a slow process. Therefore prevent frequent rebuilds at the 
					// cost of delaying the effect of the change 
				
				long	now = SystemTime.getCurrentTime();
				
				long	secs_since_last_build = (now - last_rebuild_time)/1000;
				
					// allow one second per 2000 entries
				
				if ( secs_since_last_build > entries.size()/2000 ){
					
					last_rebuild_time	= now;
					
					rebuild_required	= false;
				
					rebuild();
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	rebuild()
	{
		LGLogger.log( "IPAddressRangeManager: rebuilding " + entries.size() + " entries starts" );

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
					
					if ( e1.getStartLong() < e2.getStartLong()){
						return( -1 );
					}else if ( e1.getStartLong() > e2.getStartLong()){
						return( 1 );
					}else{
						long l = e2.getEndLong() - e1.getEndLong();
						
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
				
				long	end_pos = entry.getMergedEndLong();
				
				entry	e2 = ents[pos++];
				
				if (!e2.getMerged()){
					
					if ( end_pos >= e2.getStartLong()){
						
						e2.setMerged();
						
						if ( e2.getEndLong() > end_pos ){
							
							entry.setMergedEndInt( e2.getEndInt());
							
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
		
		total_span	= 0;
		
		for (int i=0;i<merged_entries.length;i++){
			
			entry	e = (entry)merged_entries[i];
			
				// span is inclusive
			
			long	span = ( e.getMergedEndLong() - e.getStartLong()) + 1;
			
			total_span	+= span;
		}
			//	System.out.println( "non_merged = " + merged_entries.length );
		
		LGLogger.log( "IPAddressRangeManager: rebuilding " + entries.size() + " entries ends" );

	}
	
	protected long
	getTotalSpan()
	{
		checkRebuild();
		
		return( total_span );
	}
	
	protected class
	entry
	{
		private int			start;
		private int			end;
		private Object		user_data;
		
		private boolean		merged;
		private int			merged_end;
		
		private entry[]		my_merged_entries;
		
		protected
		entry(
			int			_start,
			int			_end,
			Object		_ud )
		{
			start		= _start;
			end			= _end;
			user_data	= _ud;
		}
		
		
		protected int
		getStartInt()
		{
			return( start );
		}
		
		protected long
		getStartLong()
		{
			return( start<0?(start+0x100000000L):start );
		}
		
		protected void
		setStartInt(
			int		_start )
		{
			start	= _start;
		}
		
		protected int
		getEndInt()
		{
			return( end );
		}
		
		protected long
		getEndLong()
		{
			return( end<0?(end+0x100000000L):end );
		}
		
		protected void
		setEndInt(
			int		_end )
		{
			end	= _end;
		}
		
		protected void
		setMergedEndInt(
			int		_merged_end )
		{
			merged_end	= _merged_end;
		}
		
		protected long
		getMergedEndInt()
		{
			return( merged_end );
		}
		
		protected long
		getMergedEndLong()
		{
			return( merged_end<0?(merged_end+0x100000000L):merged_end );
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
				
				my_merged_entries = new entry[]{ e2 };
				
			}else{
				
				entry[]	x = new entry[my_merged_entries.length+1];
				
				System.arraycopy( my_merged_entries, 0, x, 0, my_merged_entries.length );
				
				x[x.length-1] = e2;
				
				my_merged_entries	= x;
			}
		}
		
		protected entry[]
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
		manager.addRange( "3.1.1.1", "3.1.1.3", "1" );
		manager.addRange( "1.1.1.1", "2.2.2.2", "2" );
		manager.addRange( "0.1.1.1", "2.2.2.2", "3" );
		manager.addRange( "1.1.1.1", "1.2.2.2", "4" );
		manager.addRange( "7.7.7.7", "7.7.8.7", "5" );
		manager.addRange( "8.8.8.8", "8.8.8.8", "6" );
		//manager.addRange( "0.0.0.0", "255.255.255.255", "7" );
		manager.addRange( "5.5.5.5", "6.6.6.9", "8" );
		manager.addRange( "6.6.6.6", "7.7.0.0", "9" );
		manager.addRange( "254.6.6.6", "254.7.0.0", "10" );
		
		
		System.out.println( "inRange -> " + manager.isInRange( "254.6.6.8" ));
		
		System.out.println( "Total span = " + manager.getTotalSpan());
		
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
