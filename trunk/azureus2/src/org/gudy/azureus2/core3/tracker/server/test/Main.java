/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.tracker.server.test;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import org.gudy.azureus2.core3.tracker.server.*;

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

			TRTrackerServerFactory.create( 6969, 30 );
													 
		}catch( Throwable e ){
			
			e.printStackTrace();
			
		}
	}
}
