/*
 * File    : Main.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.tracker.server.test;

import org.gudy.azureus2.core3.tracker.server.TRTrackerServerFactory;

public class 
Main 
{
	static void
	usage()
	{
		System.err.println( "Usage:" );
		
		System.exit(1);
	}
	
	public static void
	main(
		String[]	args )
	{
		int	test_type= 0;
		
		if ( args.length != 0 ){
			
			usage();
		}
		
		
		try{

			TRTrackerServerFactory.create( TRTrackerServerFactory.PR_UDP, 6969 );
													 
		}catch( Throwable e ){
			
			e.printStackTrace();
			
		}
	}
}
