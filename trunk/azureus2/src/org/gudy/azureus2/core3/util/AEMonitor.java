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
	extends AEMonSem
{
	protected int			dont_wait	= 1;
	protected int			nests		= 0;
	protected Thread		owner		= null;
	
	public
	AEMonitor(
		String			_name )
	{
		super( _name, true );
	}
	
	public void
	enter()
	{
		if ( DEBUG ){
			
			debugEntry();
		}				

		Thread	current_thread = Thread.currentThread();
		
		synchronized( this ){
			
			entry_count++;
			
			if ( owner == current_thread ){
				
				nests++;
				
			}else{
				
				if ( dont_wait == 0 ){

					try{
						waiting++;

						if ( waiting > 1 ){
							
							// System.out.println( "AEMonitor: " + name + " contended" );
						}
						
						wait();

					}catch( Throwable e ){

							// we know here that someone's got a finally clause to do the
							// balanced 'exit'. hence we should make it look as if we own it...
						
						waiting--;

						owner	= current_thread;
						
						Debug.out( "**** monitor interrupted ****" );
						
						throw( new RuntimeException("AEMonitor:interrupted" ));
					}
				}else{
					
					dont_wait--;
				}
			
				owner	= current_thread;
			}
		}
	}

	public void
	exit()
	{
		try{
			synchronized( this ){

				if ( nests > 0 ){
					
					if ( DEBUG ){
						
						if ( owner != Thread.currentThread()){
						
							Debug.out( "nested exit but current thread not owner");
						}
					}
					
					nests--;
					
				}else{
					
					owner	= null;
					
					if ( waiting != 0 ){

						waiting--;

						notify();

					}else{
						
						dont_wait++;
						
						if ( dont_wait > 1 ){
							
							Debug.out( "**** AEMonitor '" + name + "': multiple exit detected" );
						}
					}
				}
			}
			
		}finally{
		
			if ( DEBUG ){
				
				debugExit();
			}
		}
	}
	
	public static Map
	getSynchronisedMap(
		Map	m )
	{
		return( Collections.synchronizedMap(m));
	}
}