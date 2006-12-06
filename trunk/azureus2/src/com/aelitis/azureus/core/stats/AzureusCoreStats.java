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

import org.gudy.azureus2.core3.util.Debug;

public class 
AzureusCoreStats 
{
	public static final String ST_ALL							= ".*";
	
		// DISK
	
	public static final String ST_DISK							= "disk\\.*";
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
	
	public static final String ST_NET_TCP_OUT_CONNECT_QUEUE_LENGTH		= "net.tcp.outbound.connect.queue.length";	// Long
	public static final String ST_NET_TCP_OUT_PENDING_QUEUE_LENGTH		= "net.tcp.outbound.pending.queue.length";	// Long
	public static final String ST_NET_TCP_OUT_CANCEL_QUEUE_LENGTH		= "net.tcp.outbound.cancel.queue.length";	// Long
	public static final String ST_NET_TCP_OUT_CLOSE_QUEUE_LENGTH		= "net.tcp.outbound.close.queue.length";	// Long

	private static final String[] _ST_ALL = {
		
		ST_DISK_READ_QUEUE_LENGTH,
		ST_DISK_READ_QUEUE_BYTES,
		ST_DISK_READ_REQUEST_COUNT,
		ST_DISK_READ_REQUEST_SINGLE,
		ST_DISK_READ_REQUEST_MULTIPLE,
		ST_DISK_READ_REQUEST_BLOCKS,
		ST_DISK_READ_BYTES_TOTAL,
		ST_DISK_READ_BYTES_SINGLE,
		ST_DISK_READ_BYTES_MULTIPLE,
		
		ST_DISK_WRITE_QUEUE_LENGTH,
		ST_DISK_WRITE_QUEUE_BYTES,
		ST_DISK_WRITE_REQUEST_COUNT,
		ST_DISK_WRITE_REQUEST_BLOCKS,
		ST_DISK_WRITE_BYTES_TOTAL,
		ST_DISK_WRITE_BYTES_SINGLE,
		ST_DISK_WRITE_BYTES_MULTIPLE,
		
		ST_NET_TCP_OUT_CONNECT_QUEUE_LENGTH,
		ST_NET_TCP_OUT_PENDING_QUEUE_LENGTH,
		ST_NET_TCP_OUT_CANCEL_QUEUE_LENGTH,
		ST_NET_TCP_OUT_CLOSE_QUEUE_LENGTH,
	};
	
	private static List	providers = new ArrayList();
	
	public static Map
	getStats(
		Set		types )
	{
		Set	expanded = new HashSet();
		
		Iterator	it = types.iterator();
		
		while( it.hasNext()){
			
			String	type = (String)it.next();
			
			Pattern pattern = Pattern.compile( type );
						
			for (int i=0;i<_ST_ALL.length;i++){
				
				String	s = _ST_ALL[i];
				
				if ( pattern.matcher( s ).matches()){
					
					expanded.add( s );
				}
			}
		}
		
		Map	result = new HashMap();
		
		for (int i=0;i<providers.size();i++){
			
			Object[]	entry = (Object[])providers.get(i);
			
			try{
				((AzureusCoreStatsProvider)entry[1]).updateStats( expanded, result );
				
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
}
