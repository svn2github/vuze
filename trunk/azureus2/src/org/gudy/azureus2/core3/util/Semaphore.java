/*
 * File    : Semaphore.java
 * Created : 03-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

public class
Semaphore
{
	int		waiting		= 0;
	int		dont_wait	= 0;

	int		total_reserve	= 0;
	int		total_release	= 0;

	boolean	released_forever	= false;

	public
	Semaphore()
	{
	}

	public
	Semaphore(
		int		count )
	{
		dont_wait	= count;

		total_release	= count;
	}

	public void
	reserve()
	{
		reserve(0);
	}
	
	public boolean
	reserve(
		long	millis )
	{
		synchronized(this){

			if ( released_forever ){

				return(true);
			}

			if ( dont_wait == 0 ){

				try{
					waiting++;

					if ( millis == 0 ){
						
						wait();
						
					}else{
						
						wait(millis);
					}
					
					if ( total_reserve == total_release ){
							
						return( false );
					}
						
					total_reserve++;

					return( true );

				}catch( Throwable e ){

					waiting--;

					System.err.println( "**** semaphore operation interrupted ****" );

					throw( new RuntimeException("Semaphore: operation interrupted" ));
				}
			}else{
				dont_wait--;

				total_reserve++;
				
				return( true );
			}
		}
	}

	public void
	release()
	{
		synchronized(this){

			total_release++;

			if ( waiting != 0 ){

				waiting--;

				notify();

			}else{
				dont_wait++;
			}
		}
	}

	public void
	releaseAllWaiters()
	{
		int	x	= waiting;

		for ( int i=0;i<x;i++ ){

			release();
		}
	}

	public void
	releaseForever()
	{
		synchronized(this){

			releaseAllWaiters();

			released_forever	= true;
		}
	}
	
	public int
	getValue()
	{
		return( waiting - dont_wait );
	}
}