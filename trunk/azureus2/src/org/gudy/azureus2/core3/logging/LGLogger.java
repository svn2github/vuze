/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core3.logging;


/**
 * @author Olivier
 * 
 */

import java.io.*;

import org.gudy.azureus2.core3.logging.impl.*;

public class 
LGLogger {
	public static final int INFORMATION 	= 0;
	public static final int RECEIVED 		= 1;
	public static final int SENT 			= 2;
	public static final int ERROR 			= 3;

	public static void
	initialise()
	{
		LGLoggerImpl.initialise();
	}
	
	public static void 
	log(
		int componentId, 
		int event, 
		int color, 
		String text ) 
	{
		LGLoggerImpl.log(componentId,event,color,text );
	}	
	
	public static void 
	log(
		int 		componentId, 
		int 		event, 
		String		text, 
		Throwable	e )
	{
		String	ex_details = "";
		
		try{
			CharArrayWriter cw = new CharArrayWriter();
			
			PrintWriter pw = new PrintWriter( cw );
		
			e.printStackTrace( pw );
			
			pw.close();
			
			ex_details = new String(cw.toCharArray());
		}catch( Throwable f ){
		}
		
		LGLoggerImpl.log(componentId,event,ERROR,text + " ('" + (ex_details==null?e.toString():ex_details) + "')" );
	}
	
	public static boolean
	isLoggingOn()
	{
		return( LGLoggerImpl.isLoggingOn());
	}
	
	public static void
	setListener(
		ILoggerListener	listener )
	{
		LGLoggerImpl.setListener( listener );
	}	
	
	public static void
	removeListener()
	{
		LGLoggerImpl.removeListener();
	}
}
