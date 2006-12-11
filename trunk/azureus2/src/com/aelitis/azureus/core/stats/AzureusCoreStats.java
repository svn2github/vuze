/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.stats;

import java.util.*;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

public class 
AzureusCoreStats 
{
	public static final String ST_ALL							= ".*";
	
		// DISK
	
	public static final String ST_DISK							= "disk.*";
	public static final String ST_DISK_READ_QUEUE_LENGTH		= "disk.read.queue.length";		// Long
	public static final String ST_DISK_READ_QUEUE_BYTES			= "disk.read.queue.bytes";		// Long
	public static final String ST_DISK_READ_REQUEST_COUNT		= "disk.read.request.count";	// Long
	public static final String ST_DISK_READ_REQUEST_SINGLE		= "disk.read.request.single";	// Long
	public static final String ST_DISK_READ_REQUEST_MULTIPLE	= "disk.read.request.multiple";	// Long
	public static final String ST_DISK_READ_REQUEST_BLOCKS		= "disk.read.request.blocks";	// Long
	public static final String ST_DISK_READ_BYTES_TOTAL			= "disk.read.bytes.total";		// Long
	public static final String ST_DISK_READ_BYTES_SINGLE		= "disk.read.bytes.single";		// Long
	public static final String ST_DISK_READ_BYTES_MULTIPLE		= "disk.read.bytes.multiple";	// Long
	
	public static final String ST_DISK_WRITE_QUEUE_LENGTH		= "disk.write.queue.length";	// Long
	public static final String ST_DISK_WRITE_QUEUE_BYTES		= "disk.write.queue.bytes";		// Long
	public static final String ST_DISK_WRITE_REQUEST_COUNT		= "disk.write.request.count";	// Long
	public static final String ST_DISK_WRITE_REQUEST_BLOCKS		= "disk.write.request.blocks";	// Long
	public static final String ST_DISK_WRITE_BYTES_TOTAL		= "disk.write.bytes.total";		// Long
	public static final String ST_DISK_WRITE_BYTES_SINGLE		= "disk.write.bytes.single";	// Long
	public static final String ST_DISK_WRITE_BYTES_MULTIPLE		= "disk.write.bytes.multiple";	// Long

		// NETWORK
	
	public static final String ST_NET_WRITE_CONTROL_WAIT_COUNT			= "net.write.control.wait.count";		// Long
	public static final String ST_NET_WRITE_CONTROL_ENTITY_COUNT		= "net.write.control.entity.count";	
	public static final String ST_NET_WRITE_CONTROL_CON_COUNT			= "net.write.control.con.count";			// Long
	public static final String ST_NET_WRITE_CONTROL_READY_CON_COUNT		= "net.write.control.ready.con.count";	// Long
	public static final String ST_NET_WRITE_CONTROL_READY_BYTE_COUNT	= "net.write.control.ready.byte.count";	// Long

	public static final String ST_NET_READ_CONTROL_WAIT_COUNT			= "net.read.control.wait.count";			// Long
	public static final String ST_NET_READ_CONTROL_ENTITY_COUNT			= "net.read.control.entity.count";		// Long
	public static final String ST_NET_READ_CONTROL_CON_COUNT			= "net.read.control.con.count";			// Long
	public static final String ST_NET_READ_CONTROL_READY_CON_COUNT		= "net.read.control.ready.con.count";	// Long
	

	public static final String ST_NET_TCP_OUT_CONNECT_QUEUE_LENGTH		= "net.tcp.outbound.connect.queue.length";	// Long
	public static final String ST_NET_TCP_OUT_PENDING_QUEUE_LENGTH		= "net.tcp.outbound.pending.queue.length";	// Long
	public static final String ST_NET_TCP_OUT_CANCEL_QUEUE_LENGTH		= "net.tcp.outbound.cancel.queue.length";	// Long
	public static final String ST_NET_TCP_OUT_CLOSE_QUEUE_LENGTH		= "net.tcp.outbound.close.queue.length";	// Long

	private static final Integer	POINT 		= new Integer(1);
	private static final Integer	CUMULATIVE 	= new Integer(1);
	
	private static final Map		stats_types	= new HashMap();
	
	private static final Object[][] _ST_ALL = {
		
		{ ST_DISK_READ_QUEUE_LENGTH,				POINT },
		{ ST_DISK_READ_QUEUE_BYTES,					POINT },
		{ ST_DISK_READ_REQUEST_COUNT,				CUMULATIVE },
		{ ST_DISK_READ_REQUEST_SINGLE,				CUMULATIVE },
		{ ST_DISK_READ_REQUEST_MULTIPLE,			CUMULATIVE },
		{ ST_DISK_READ_REQUEST_BLOCKS,				CUMULATIVE },
		{ ST_DISK_READ_BYTES_TOTAL,					CUMULATIVE },
		{ ST_DISK_READ_BYTES_SINGLE,				CUMULATIVE },
		{ ST_DISK_READ_BYTES_MULTIPLE,				CUMULATIVE },
		
		{ ST_DISK_WRITE_QUEUE_LENGTH,				POINT },
		{ ST_DISK_WRITE_QUEUE_BYTES,				POINT },
		{ ST_DISK_WRITE_REQUEST_COUNT,				CUMULATIVE },
		{ ST_DISK_WRITE_REQUEST_BLOCKS,				CUMULATIVE },
		{ ST_DISK_WRITE_BYTES_TOTAL,				CUMULATIVE },
		{ ST_DISK_WRITE_BYTES_SINGLE,				CUMULATIVE },
		{ ST_DISK_WRITE_BYTES_MULTIPLE,				CUMULATIVE },
		
		{ ST_NET_WRITE_CONTROL_WAIT_COUNT,			CUMULATIVE },
		{ ST_NET_WRITE_CONTROL_ENTITY_COUNT,		POINT },
		{ ST_NET_WRITE_CONTROL_CON_COUNT,			POINT },
		{ ST_NET_WRITE_CONTROL_READY_CON_COUNT,		POINT },
		{ ST_NET_WRITE_CONTROL_READY_BYTE_COUNT,	POINT },
		{ ST_NET_READ_CONTROL_WAIT_COUNT,			CUMULATIVE },
		{ ST_NET_READ_CONTROL_ENTITY_COUNT,			POINT },
		{ ST_NET_READ_CONTROL_CON_COUNT,			POINT },
		{ ST_NET_READ_CONTROL_READY_CON_COUNT,		POINT },
		
		{ ST_NET_TCP_OUT_CONNECT_QUEUE_LENGTH,		POINT },
		{ ST_NET_TCP_OUT_PENDING_QUEUE_LENGTH,		POINT },
		{ ST_NET_TCP_OUT_CANCEL_QUEUE_LENGTH,		POINT },
		{ ST_NET_TCP_OUT_CLOSE_QUEUE_LENGTH,		POINT },
	};
	
	static{
		
		for (int i=0;i<_ST_ALL.length;i++){
			
			stats_types.put( _ST_ALL[i][0], _ST_ALL[i][1] );
		}
	}
	
	private static final List	providers 	= new ArrayList();
	
	private static  Map	averages	= new HashMap();
	
	private static boolean 	enable_averages;
	private static Timer	average_timer;
	
	public static Map
	getStats(
		Set		types )
	{
		Set	expanded = new HashSet();
		
		Iterator	it = types.iterator();
		
		while( it.hasNext()){
			
			String	type = (String)it.next();
			
			if ( !type.endsWith("*")){
				
				type = type + ".*";
			}
			
			Pattern pattern = Pattern.compile( type );
						
			for (int i=0;i<_ST_ALL.length;i++){
				
				String	s = (String)_ST_ALL[i][0];
				
				if ( pattern.matcher( s ).matches()){
					
					expanded.add( s );
				}
			}
		}
		
		Map	result = getStatsSupport( expanded );
		
		Map	ave = averages;
		
		if ( ave != null ){
			
			it = result.keySet().iterator();
			
			Map	ave_results = new HashMap();
			
			while( it.hasNext()){
								
				String	key = (String)it.next();
				
				Object[]	a_entry = (Object[])ave.get( key );
				
				if ( a_entry != null ){
					
					Average	average = (Average)a_entry[0];
					
					ave_results.put( key + ".average", new Long( average.getAverage()));
				}
			}
			
			result.putAll( ave_results );
		}
		
		return( result );
	}
	
	protected static Map
	getStatsSupport(
		Set		types )
	{
		Map	result = new HashMap();
		
		for (int i=0;i<providers.size();i++){
			
			Object[]	provider_entry = (Object[])providers.get(i);
			
			Map	provider_result = new HashMap();

			Set	target_types;
			
			if ( types == null ){
				
				target_types = (Set)provider_entry[0];
			}else{
			
				target_types = types;
			}
			
			try{
				((AzureusCoreStatsProvider)provider_entry[1]).updateStats( target_types, provider_result );
				
				Iterator pit = provider_result.entrySet().iterator();
				
				while( pit.hasNext()){
					
					Map.Entry	pe = (Map.Entry)pit.next();
					
					String	key = (String)pe.getKey();
					Object	obj	= pe.getValue();
					
					if ( obj instanceof Long ){
						
						Long	old = (Long)result.get(key);
						
						if ( old == null ){
							
							result.put( key, obj );
							
						}else{
							
							long	v = ((Long)obj).longValue();
							
							result.put( key, new Long( v + old.longValue()));
						}
					}else{
						
						result.put( key, obj );
					}
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( result );
	}
	
	public static void
	registerProvider(
		Set							types,
		AzureusCoreStatsProvider	provider )
	{
		synchronized( providers ){
			
			providers.add( new Object[]{ types, provider });
		}
	}
	
	public static synchronized void
	setEnableAverages(
		boolean		enabled )
	{
		if ( enabled == enable_averages ){
			
			return;
		}
		
		enable_averages = enabled;
		
		if ( enabled ){
			
			if ( average_timer == null ){
				
				average_timer = new Timer( "AzureusCoreStats:average" );
				
				averages = new HashMap();
				
				average_timer.addPeriodicEvent(
					1000,
					new TimerEventPerformer()
					{
						private Map	ave = averages;

						public void
						perform(
							TimerEvent	event )
						{
							Map	stats = getStatsSupport( null );
																
							Iterator	it = stats.entrySet().iterator();
							
							while( it.hasNext()){
								
								Map.Entry	entry = (Map.Entry)it.next();
								
								String	key 	= (String)entry.getKey();
								Object	value 	= entry.getValue();
								
								if ( value instanceof Long ){
									
									long	last_value;
									Average	a;
									
									Object[] a_entry = (Object[])ave.get( key );
									
									if ( a_entry == null ){
	
										a 			= Average.getInstance( 1000, 10 );
										last_value	= 0;
										
										a_entry = new Object[]{ a, value };
										
										ave.put( key, a_entry );
										
									}else{
										a			= (Average)a_entry[0];
										last_value	= ((Long)a_entry[1]).longValue();
									}
									
									if ( stats_types.get( key ) == CUMULATIVE ){
									
										a.addValue(((Long)value).longValue() - last_value);
										
									}else{
										
										a.addValue(((Long)value).longValue());

									}
									
									a_entry[1] = value;
								}
							}
						}
					});
			}
		}else{
			
			if ( average_timer != null ){
				
				average_timer.destroy();
				
				average_timer = null;
				
				averages	= null;
			}
		}
	}
	
	public static boolean
	getEnableAverages()
	{
		return( enable_averages );
	}
}
