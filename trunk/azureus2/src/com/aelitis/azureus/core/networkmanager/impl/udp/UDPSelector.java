/*
 * Created on 23 Jun 2006
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.udp;

import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;

public class 
UDPSelector 
{
	private List		ready_set	= new LinkedList();
	private AESemaphore	ready_sem	= new AESemaphore( "UDPSelector" );
	
	protected
	UDPSelector(
		final UDPConnectionManager		manager )
	{
		new AEThread( "UDPSelector", true )
		{
			public void
			runSupport()
			{
				while( true ){
					
					manager.poll();
					
					if ( ready_sem.reserve(25)){
						
						Object[]	entry;
						
						synchronized( ready_set ){
							
							entry = (Object[])ready_set.remove(0);
						}
						
					
						TransportHelper	transport 	= (TransportHelper)entry[0];
						
						TransportHelper.selectListener	listener = (TransportHelper.selectListener)entry[1];
						
						if ( listener == null ){
							
							Debug.out( "Null listener" );
							
						}else{
							
							Object	attachment = entry[2];
							
							try{
								if ( entry.length == 3 ){
									
									listener.selectSuccess( transport, attachment );
									
								}else{
									
									listener.selectFailure( transport, attachment, (Throwable)entry[3] );
									
								}
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
						}
					}
				}
			}
		}.start();
	}
	
	protected void
	ready(
		TransportHelper						transport,
		TransportHelper.selectListener		listener,
		Object								attachment )
	{
		boolean	removed = false;
		
		synchronized( ready_set ){

			Iterator	it = ready_set.iterator();
			
			while( it.hasNext()){
			
				Object[]	entry = (Object[])it.next();
				
				if ( entry[1] == listener ){
					
					it.remove();
					
					removed	= true;
					
					break;
				}
			}
			
			ready_set.add( new Object[]{ transport, listener, attachment });
		}
		
		if ( !removed ){
			
			ready_sem.release();
		}
	}
	
	protected void
	ready(
		TransportHelper						transport,
		TransportHelper.selectListener		listener,
		Object								attachment,
		Throwable							error )
	{
		boolean	removed = false;
		
		synchronized( ready_set ){

			Iterator	it = ready_set.iterator();
			
			while( it.hasNext()){
			
				Object[]	entry = (Object[])it.next();
				
				if ( entry[1] == listener ){
					
					it.remove();
					
					removed	= true;
					
					break;
				}
			}
			
			ready_set.add( new Object[]{ transport, listener, attachment, error });
		}
		
		if ( !removed ){
			
			ready_sem.release();
		}
	}
}
