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
	
	private static Map 	debug_traces		= new HashMap();
	private static List	debug_recursions	= new ArrayList();
	private static List	debug_reciprocals	= new ArrayList();
	
	
	
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

			System.out.println( "AEMonitor: id = " + monitor_id_next + ", monitors = " + debug_monitors.size() + ", names = " + debug_name_mapping.size() + ", traces = " + debug_traces.size());

		
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
						
			if ( !stack.isEmpty()){
				
				StringBuffer	sb = new StringBuffer();
	
				boolean	check_recursion = !debug_recursions.contains( name );
				
				String	prev_name	= null;
				
				for (int i=0;i<stack.size();i++){
				
					AEMonitor	mon = (AEMonitor)stack.get(i);
					
					if ( check_recursion ){
						if ( 	mon.name.equals( name ) &&
								mon != this ){
							
							Debug.out( "AEMonitor: recursive locks on different monitor instances: " + name );
							
							debug_recursions.add( name );
						}
					}
	
						// remove consecutive duplicates
					
					if ( prev_name == null || !mon.name.equals( prev_name )){
						
						sb.append("$");
						sb.append(mon.name);
					}
					
					prev_name	= mon.name;
				}
				
				sb.append( "$" );
				sb.append( name );
				sb.append( "$" );
				
				String trace_key = sb.toString();
				
				stack.push( this );

				synchronized( debug_traces ){
								
					if ( debug_traces.get(trace_key) == null ){
					
						String	thread_name	= Thread.currentThread().getName();
						String	stack_trace	= Debug.getCompressedStackTrace(2);
						
						Iterator	it = debug_traces.keySet().iterator();
					
						while( it.hasNext()){
							
							String	key = (String)it.next();
							
							String[]	data = (String[])debug_traces.get(key);
							
							String	old_thread_name	= data[0];
							String	old_trace		= data[1];
							
							for (int i=0;i<stack.size();i++){
								
								for (int j=i+1;j<stack.size();j++){
									
									String	n1 = ((AEMonitor)stack.get(i)).name;
									String	n2 = ((AEMonitor)stack.get(j)).name;
									
										// same object recursion already tested above
									
									if ( !n1.equals( n2 )){
									
										int	p1 = key.indexOf( "$" + n1 + "$");
										int p2 = key.indexOf( "$" + n2 + "$");
										
										if ( p1 != -1 && p2 != -1 && p1 > p2 ){
											
											String	reciprocal_log = trace_key + " / " + key;
											
											if ( !debug_reciprocals.contains( reciprocal_log )){
												
												debug_reciprocals.add( reciprocal_log );
												
												System.out.println(
														"AEMonitor: Reciprocal monitor usage:\n" +
														"    " + trace_key + "\n" + 
														"        [" + thread_name + "] " + stack_trace + "\n" +
														"    " + key + "\n" +
														"        [" + old_thread_name + "] " + old_trace );
											}
										}
									}
								}
							}
						}
						
						debug_traces.put( trace_key, new String[]{ thread_name, stack_trace });
				
							// look through all the traces for an A->B and B->A
					}
				}
				
			}else{
				
				stack.push( this );
				
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