/*
 * Created on 18-Sep-2004
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import java.util.*;

public class 
AEMonitor 
{
	private static boolean	DEBUG					= true;
	private static boolean	DEBUG_CHECK_DUPLICATES	= true;
	
	private static long		DEBUG_TIMER				= 30000;
	
	static{
		if ( DEBUG ){
			
			System.out.println( "**** AEMonitor debug on ****" );
		}
	}
	
	private static ThreadLocal		tls	= 
		new ThreadLocal()
		{
			public Object
			initialValue()
			{
				return( new Stack());
			}
		};
	
	private static long	monitor_id_next;
	
	private static Map 	debug_traces	= new HashMap();
	
	private static Map	debug_name_mapping		= new WeakHashMap();
	private static Map	debug_monitors			= new WeakHashMap();

	private static long		last_total_entry;
	
	protected long			monitor_id;
	protected long			entry_count;
	
	static{
		new Timer("AEMonitor").addPeriodicEvent(
				DEBUG_TIMER,
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent	event )
					{
						checkMonitors();
					}
				});
	}
	
	protected static void
	checkMonitors()
	{
		synchronized( AEMonitor.class ){

			System.out.println( "AEMonitor: id = " + monitor_id_next + ", monitors = " + debug_monitors.size() + ", names = " + debug_name_mapping.size());

		
			Iterator 	it = debug_monitors.keySet().iterator();
			
			long	total_entry	= 0;
			
			while (it.hasNext()){
				
				monitorData	data = (monitorData)it.next();
				
				AEMonitor	monitor = data.monitor;
				
				total_entry += monitor.entry_count;
			}
			
			
			System.out.println( "    total in = " + total_entry + " - " + ((total_entry - last_total_entry ) / (DEBUG_TIMER/1000)) + "/sec" );
			
			last_total_entry	= total_entry;
		}
	}
		// non-debug stuff
	
	
	protected String		name;
	
	
	protected int			waiting		= 0;
	protected int			dont_wait	= 1;
	protected int			nests		= 0;
	protected Thread		owner		= null;
	
	public
	AEMonitor(
		String			_name )
	{
		name		= _name;
		
		if ( DEBUG ){
			
			synchronized( AEMonitor.class ){
				
				monitor_id	= monitor_id_next++;
								
				StackTraceElement	elt = new Exception().getStackTrace()[1];
				
				String	class_name 	= elt.getClassName();
				int		line_number	= elt.getLineNumber(); 
			
				monitorData new_entry	= new monitorData( this, class_name, line_number);

				debug_monitors.put( new_entry, new_entry );
				
				if ( DEBUG_CHECK_DUPLICATES ){
					
					monitorData		existing_name_entry	= (monitorData)debug_name_mapping.get( name );
					
					if ( existing_name_entry == null ){
						
						debug_name_mapping.put( name, new_entry );
						
					}else{
						
						if ( 	( !existing_name_entry.owning_class.getName().equals( class_name )) ||
								existing_name_entry.line_number != line_number ){
							
							new Exception("Duplicate AEMonitor name '" + name + "'").printStackTrace();
						}
					}
				}
			}
			
		}
	}
	
	public void
	enter()
	{
		if ( DEBUG ){
			
				// bad things are:
				// A->B and somewhere else B->A
				// or
				// A(inst1) -> A(inst2)
			
			Stack	stack = (Stack)tls.get();
			
			stack.push( this );
					
			String	str = "";

			for (int i=0;i<stack.size();i++){
			
				str += (i==0?"":",") + ((AEMonitor)stack.get(i)).name;
			}
			
			synchronized( debug_traces ){
				
				if ( debug_traces.get(str) == null ){
			
					// System.out.println( "AEMonitor: " + str );
					
					debug_traces.put( str, str );
				}
			}
		}
		
		reserve();
	}
	
	public void
	exit()
	{
		try{
			release();
			
		}finally{
		
			if ( DEBUG ){
				
				// System.out.println( "release: " + name );

				((Stack)tls.get()).pop();
			}
		}
	}
	
	protected synchronized void
	reserve()
	{
		entry_count++;
		
		if ( owner == Thread.currentThread()){
			
			nests++;
			
		}else{
			
			if ( dont_wait == 0 ){

				try{
					waiting++;

					if ( waiting > 1 ){
						
						System.out.println( "AEMonitor: " + name + " contended" );
					}
					
					wait();

				}catch( Throwable e ){

						// we know here that someone's got a finally clause to do the
						// balanced 'exit'. hence we should make it look as if we own it...
					
					waiting--;

					owner	= Thread.currentThread();
					
					System.out.println( "**** monitor interrupted ****" );
					
					throw( new RuntimeException("AEMonitor:interrupted" ));
				}
			}else{
				dont_wait--;
			}
		
			owner	= Thread.currentThread();
		}
	}

	protected synchronized void
	release()
	{
		if ( nests > 0 ){
			
			nests--;
			
		}else{
			
			owner	= null;
			
			if ( waiting != 0 ){

				waiting--;

				notify();

			}else{
				
				dont_wait++;
				
				if ( dont_wait > 1 ){
					
					System.out.println( "**** AEMonitor '" + name + "': multiple exit detected" );
				}
			}
		}
	}
	
	public static Map
	getSynchronisedMap(
		Map	m )
	{
		return( Collections.synchronizedMap(m));
	}
	
	protected static class
	monitorData
	{
		protected AEMonitor		monitor;
		protected Class			owning_class;
		protected int			line_number;
		
		
		protected
		monitorData(
			AEMonitor		_monitor,
			String			_class_name,
			int				_line_number )
		{
			monitor			= _monitor;
			
			try{
				owning_class	= Class.forName( _class_name );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
			
			line_number		= _line_number;
		}
	}
}