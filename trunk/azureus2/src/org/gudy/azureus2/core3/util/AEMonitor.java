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
	private static boolean	DEBUG		= true;
	
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
	
	private static	Map debug_traces	= new HashMap();
	
		
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
			
			stack.push( name );
			
			String	str = "";

			for (int i=0;i<stack.size();i++){
			
				str += (i==0?"":",") + stack.get(i);
			}
			
			synchronized( debug_traces ){
				
				if ( debug_traces.get(str) == null ){
			
					System.out.println( "AEMonitor: " + str );
					
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
		if ( owner == Thread.currentThread()){
			
			nests++;
			
		}else{
			
			if ( dont_wait == 0 ){

				try{
					waiting++;

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
}