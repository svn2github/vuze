/*
 * Created on 22-Sep-2004
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

/**
 * @author parg
 *
 */

public abstract class 
AEMonSem 
{
	protected static boolean	DEBUG					= true;
	protected static boolean	DEBUG_CHECK_DUPLICATES	= true;
	
	protected static long		DEBUG_TIMER				= 30000;
	
	static{
		if ( DEBUG ){
			
			System.out.println( "**** AEMonitor/AESemaphore debug on ****" );
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
	private static long	semaphore_id_next;
	
	private static Map 	debug_traces		= new HashMap();
	private static List	debug_recursions	= new ArrayList();
	private static List	debug_reciprocals	= new ArrayList();
	private static List	debug_sem_in_mon	= new ArrayList();
	
	
	
	private static Map	debug_name_mapping		= new WeakHashMap();
	private static Map	debug_monitors			= new WeakHashMap();
	private static Map	debug_semaphores		= new WeakHashMap();
	
	static{
		new Timer("AEMonSem").addPeriodicEvent(
				DEBUG_TIMER,
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent	event )
					{
						check();
					}
				});
	}
	
	protected static void
	check()
	{
		List	active				= new ArrayList();
		List	waiting_monitors	= new ArrayList();
		List	waiting_semaphores	= new ArrayList();
		
		synchronized( AEMonSem.class ){

			System.out.println( 
					"AEMonSem: mid = " + monitor_id_next +
					", sid = " + semaphore_id_next +
					", monitors = " + debug_monitors.size() + 
					", semaphores = " + debug_semaphores.size() + 
					", names = " + debug_name_mapping.size() + 
					", traces = " + debug_traces.size());

		
			Iterator 	it = debug_monitors.keySet().iterator();
			
			long	new_mon_entries	= 0;
			
			while (it.hasNext()){
				
				AEMonSem	monitor = (AEMonSem)it.next();
							
				long	diff = monitor.entry_count - monitor.last_entry_count;
				
				if (  diff != 0 ){
					
					active.add( monitor );
					
					new_mon_entries += diff;
				}
				
				if (monitor.waiting > 0 ){
					
					waiting_monitors.add( monitor );
				}
			}
			
			it = debug_semaphores.keySet().iterator();
			
			long	new_sem_entries	= 0;
			
			while (it.hasNext()){
				
				AEMonSem	semaphore = (AEMonSem)it.next();
							
				long	diff = semaphore.entry_count - semaphore.last_entry_count;
				
				if (  diff != 0 ){
					
					active.add( semaphore );

					new_sem_entries += diff;
				}
				
				if (semaphore.waiting > 0 ){
					
					waiting_semaphores.add( semaphore );
				}
			}		
			
			System.out.println( 
					"    activity: monitors = " + new_mon_entries + " - " + (new_mon_entries / (DEBUG_TIMER/1000)) + 
					"/sec, semaphores = " + new_sem_entries + " - " +
					(new_sem_entries / (DEBUG_TIMER/1000)) + "/sec ");
		}
		
		AEMonSem[]	x = new AEMonSem[active.size()];
		
		active.toArray(x);
		
		Arrays.sort(
			x,
			new Comparator()
			{
				public int
				compare(
					Object	o1,
					Object	o2 )
				{
					AEMonSem	a1 = (AEMonSem)o1;
					AEMonSem	a2 = (AEMonSem)o2;
					
					return((int)((a2.entry_count - a2.last_entry_count ) - (a1.entry_count - a1.last_entry_count )));
				}
				
			});
		
		System.out.print("    top activity:" );
		
		for (int i=0;i<Math.min(10,x.length);i++){
			
			System.out.print( (i==0?"":", ") + x[i].name + " = " + (x[i].entry_count - x[i].last_entry_count ));
		}
		
		System.out.println();
	
		if ( waiting_monitors.size() > 0 ){
			
			System.out.println( "    waiting monitors" );
			
			for (int i=0;i<waiting_monitors.size();i++){
				
				AEMonSem	ms = (AEMonSem)waiting_monitors.get(i);
				
				System.out.println( "        " + ms.name + " - " + ms.last_trace_key );
			}
		}
		
		if ( waiting_semaphores.size() > 0 ){
			
			System.out.println( "    waiting semaphores" );
			
			for (int i=0;i<waiting_semaphores.size();i++){
				
				AEMonSem	ms = (AEMonSem)waiting_semaphores.get(i);
				
				System.out.println( "        " + ms.name + " - " + ms.last_trace_key );
			}
		}
		for (int i=0;i<x.length;i++){
			
			AEMonSem	ms = x[i];
			
			ms.last_entry_count = ms.entry_count;
		}
	}
	
	
	protected long			entry_count;
	protected long			last_entry_count;
	protected String		last_trace_key;
	
	
	protected String		name;
	protected boolean		is_monitor;
	protected int			waiting		= 0;

	
	protected
	AEMonSem(
		String	_name,
		boolean	_monitor )
	{
		is_monitor		= _monitor;
		
		if ( is_monitor) {
			
			name		= _name;
		}else{
			
			name		= "(S)" + _name;
		}
		
		if ( DEBUG ){
			
			synchronized( AEMonSem.class ){
				
				if ( is_monitor ){
					monitor_id_next++;
				}else{
					semaphore_id_next++;
				}
								
				StackTraceElement	elt = new Exception().getStackTrace()[2];
				
				String	class_name 	= elt.getClassName();
				int		line_number	= elt.getLineNumber(); 
			
				monSemData new_entry	= new monSemData( class_name, line_number);

				if ( is_monitor ){
					
					debug_monitors.put( this, new_entry );
					
				}else{
					
					debug_semaphores.put( this, new_entry );
				}
				
				if ( DEBUG_CHECK_DUPLICATES ){
					
					monSemData		existing_name_entry	= (monSemData)debug_name_mapping.get( name );
					
					if ( existing_name_entry == null ){
						
						debug_name_mapping.put( name, new_entry );
						
					}else{
						
						if ( 	( !existing_name_entry.owning_class.getName().equals( class_name )) ||
								existing_name_entry.line_number != line_number ){
							
							new Exception("Duplicate AEMonSem name '" + name + "'").printStackTrace();
						}
					}
				}
			}	
		}
	}
	
	protected void
	debugEntry()
	{
			// bad things are:
			// A->B and somewhere else B->A
			// or
			// A(inst1) -> A(inst2)
		
		Stack	stack = (Stack)tls.get();
			
		if ( stack.size() > 64 ){
			
			Debug.out( "**** Whoaaaaaa, AEMonSem debug stack is getting too large!!!! ****" );
		}
		
		if ( !stack.isEmpty()){
			
			if (	(!is_monitor) &&
					((AEMonSem)stack.peek()).is_monitor ){
				
				if ( !debug_sem_in_mon.contains( name )){
					
					Debug.out( "Semaphore reservation while holding a monitor: sem = " + name+ ", mon = " + ((AEMonSem)stack.peek()).name );
					
					debug_sem_in_mon.add( name );
				}
			}
					
			StringBuffer	sb = new StringBuffer();
	
			boolean	check_recursion = !debug_recursions.contains( name );
			
			String	prev_name	= null;
			
			for (int i=0;i<stack.size();i++){
			
				AEMonSem	mon = (AEMonSem)stack.get(i);
				
				if ( check_recursion ){
					if ( 	mon.name.equals( name ) &&
							mon != this ){
						
						Debug.out( "AEMonSem: recursive locks on different instances: " + name );
						
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
			
			last_trace_key	= trace_key;
			
			if ( !is_monitor ){
				
					// only add semaphores to the stack if they aren't already present.
					// This is because we can reserve a semaphore on one thread and
					// release it on another. This will grow the stack indefinitely
				
				boolean	match 	= false;
				
				for (int i=0;i<stack.size();i++){
					
					if ( stack.get(i) == this ){
						
						match	= true;
						
						break;
					}
				}
				
				if ( !match ){
					
					stack.push( this );
				}
			}else{
				
				stack.push( this );
			}
	
			synchronized( debug_traces ){
							
				if ( debug_traces.get(trace_key) == null ){
				
					String	thread_name	= Thread.currentThread().getName();
					String	stack_trace	= Debug.getCompressedStackTrace(3);
					
					Iterator	it = debug_traces.keySet().iterator();
				
					while( it.hasNext()){
						
						String	old_key = (String)it.next();
						
						String[]	data = (String[])debug_traces.get(old_key);
						
						String	old_thread_name	= data[0];
						String	old_trace		= data[1];
						
							// find the earliest occurrence of a common monitor - no point in searching
							// beyond it
							//    e.g.  a -> b -> c -> g
						    //          x -> y -> b -> z
							// stop at b because beyond this things are "protected"
						
						
						int	earliest_common = stack.size();
						
						for (int i=0;i<stack.size();i++){
				
							String	n1 = ((AEMonSem)stack.get(i)).name;
						
							int	p1 = old_key.indexOf( "$" + n1 + "$");
	
							if ( p1 != -1 ){
																
								earliest_common = Math.min( earliest_common, i+1 );
							}
						}
						
						for (int i=0;i<earliest_common;i++){
							
							for (int j=i+1;j<stack.size();j++){
								
								String	n1 = ((AEMonSem)stack.get(i)).name;
								String	n2 = ((AEMonSem)stack.get(j)).name;
								
									// same object recursion already tested above
								
								if ( !n1.equals( n2 )){
								
									int	p1 = old_key.indexOf( "$" + n1 + "$");
									int p2 = old_key.indexOf( "$" + n2 + "$");
									
									if ( p1 != -1 && p2 != -1 && p1 > p2 ){
										
										String	reciprocal_log = trace_key + " / " + old_key;
										
										if ( !debug_reciprocals.contains( reciprocal_log )){
											
											debug_reciprocals.add( reciprocal_log );
											
											System.out.println(
													"AEMonSem: Reciprocal usage:\n" +
													"    " + trace_key + "\n" + 
													"        [" + thread_name + "] " + stack_trace + "\n" +
													"    " + old_key + "\n" +
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
			
			last_trace_key	= "$" + name + "$";
			
			stack.push( this );
			
		}
	}
	
	protected void
	debugExit()
	{
		Stack	stack = (Stack)tls.get();
		
		if ( is_monitor ){
	
				// skip over any sem reserves within a sync block
			
			while( stack.peek() != this ){
				
				stack.pop();
			}
			
			stack.pop();
			
		}else{
			
				// for semaphores we can release stuff without a matching reserve if
				// the semaphore has an initial value or if we have one thread releasing
				// a semaphore and another reserving it
					
			if ( !stack.isEmpty()){
			
				if ( stack.peek() == this ){
									
					stack.pop();
				}
			}
		}
	}
	
	protected static class
	monSemData
	{
		protected Class			owning_class;
		protected int			line_number;
		
		
		protected
		monSemData(
			String			_class_name,
			int				_line_number )
		{			
			try{
				owning_class	= Class.forName( _class_name );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
			
			line_number		= _line_number;
		}
	}
}
