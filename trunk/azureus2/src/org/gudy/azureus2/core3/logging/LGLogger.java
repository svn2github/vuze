/*
 * Created on 04-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.logging;

/**
 * @author stuff
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import org.gudy.azureus2.core.Logger;	// TODO: sort this

public class 
LGLogger 
{
	public static final int INFORMATION 	= 0;
	public static final int RECEIVED 		= 1;
	public static final int SENT 			= 2;
	public static final int ERROR 			= 3;

	public static void 
	log(
		int componentId, 
		int event, 
		int color, 
		String text ) 
	{
		Logger.getLogger().log(componentId,event,color,text );
	}	
	
	public static void 
	log(
		int 		componentId, 
		int 		event, 
		String		text, 
		Throwable	e )
	{
		Logger.getLogger().log(componentId,event,ERROR,text + " ('" + e.toString() + "')" );
	}
}
