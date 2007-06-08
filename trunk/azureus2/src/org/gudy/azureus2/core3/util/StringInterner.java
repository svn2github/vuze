/*
 * Created on Jun 8, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.core3.util;

import java.lang.ref.*;
import java.util.*;


public class 
StringInterner 
{
	private static final int MAX_MAP_SIZE		= 1000;
	
	private static final int TICK_PERIOD			= 60*1000;
	private static final int TOTAL_CLEAN_TIME		= 15*60*1000;
	private static final int TOTAL_CLEAN_TICKS		= TOTAL_CLEAN_TIME/TICK_PERIOD;
	
	private static Map map = new WeakHashMap( MAX_MAP_SIZE );
	
	// private final static ReferenceQueue queue = new ReferenceQueue();

	static{
		SimpleTimer.addPeriodicEvent(
			"StringInterner:gc",
			TICK_PERIOD,
			new TimerEventPerformer()
			{
				private int	tick_count;
				
				public void 
				perform(
					TimerEvent	 event )
				{
					tick_count++;
					
					synchronized( StringInterner.class ){
									
							// one off start of day clear-down to get rid off init vars
						
						tidy( tick_count == TOTAL_CLEAN_TICKS );
					}
				}
			});
	}
	
	public static String
	intern(
		String		str )
	{
		synchronized( StringInterner.class ){
			
			entryDetails entry;
			
			/*
			while( ( entry = (entryDetails)queue.poll() ) != null ){
			}
			*/
						
			if ( map.size() > MAX_MAP_SIZE ){
				
				tidy( false );
			}
			
			entry = (entryDetails)map.get( str );
			
			if ( entry != null ){
				
				String	s = (String)entry.get();
				
				if ( s != null ){
					
					if ( entry.hit_count < Short.MAX_VALUE ){
					
						entry.hit_count++;
					}
										
					return( s );
				}
			}
			
			map.put( str, new entryDetails( str ));
						
			return( str );
		}
	}
	
	private static void
	tidy(
		boolean	clear )
	{
		if ( !clear ){
			
			Iterator	it = map.values().iterator();
			
			while( it.hasNext()){
				
				entryDetails entry = (entryDetails)it.next();
		
					// random guess: size of an entry is 
					// Object: 8
					// Reference: 4*8
					// entryDetails: 4
					// Map.Entry: 2*8 + 4
					// = say 90 bytes (testing shows 90 :))
				
					// a String is 24 bytes + chars
				
				final int overhead	= 90;
				final int str_size	= 24 + entry.size;
				
				if ( entry.hit_count * str_size < overhead ){
					
					it.remove();
				}
			}
			
			if ( map.size() > MAX_MAP_SIZE / 2 ){
			
				// didn't compact enough, dump the whole thing and start again!
			
				clear = true;
			}
		}
		
		if ( clear ){
			
			map = new WeakHashMap( MAX_MAP_SIZE );
		}
		
		/*
		System.out.println( "trimmed down to " + map.size());
		
		List l = new ArrayList(map.values());
	
		Collections.sort(
				l,
				new Comparator()
				{
					public int 
					compare(
						Object o1, 
						Object o2 ) 
					{
						entryDetails	e1 = (entryDetails)o1;
						entryDetails	e2 = (entryDetails)o2;
						
						return( e2.hit_count - e1.hit_count );
					}
				});
		
		for (int i=0;i<Math.min( 32, l.size());i++){
			
			entryDetails	e = (entryDetails)l.get(i);
			
			System.out.println( e.get() + " -> " + e.hit_count );
		}
		*/
	}
	
	private static class
	entryDetails
		extends WeakReference
	{
		private short		hit_count;
		private short		size;

		protected
		entryDetails(
			String		key )
		{
			// super( key, queue );
			super( key );
			
			int	len = key.length();
			
			if ( len <= Short.MAX_VALUE ){
				
				size = (short)len;
				
			}else{
				
				size = Short.MAX_VALUE;
			}
		}
	}
	
	/*
	public static void
	main(
		String[]	args )
	{
		WeakHashMap map = new WeakHashMap();
		
		String[] strings = new String[1000];
		
		System.gc();
		
		long used1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		int	entries;
		
		for (int i=0;i<4000;i++){
			
			Object	key 	= new Integer(i);
			//Object	value 	= new Integer(i);
			
			map.put( key, new entryDetails( key ));
		}
		
		entries = map.size();
		
		for (int i=0;i<strings.length;i++){
			
			strings[i] = new String("");
		}
		entries = strings.length;
		
		long used2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		long diff = used2 - used1;
		
		System.out.println( "entries=" + entries + ", diff=" + diff + " -> " + (diff/entries));
	}
	*/
}
