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
public class 
AEMonitor 
{
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
		reserve();
				
	}
	
	public void
	exit()
	{
		
		release();
	}
	
	protected synchronized void
	reserve()
	{
		synchronized(this){

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