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
LGLogger 
{
		// log types
	
	public static final int INFORMATION 	= 0;
	public static final int RECEIVED 		= 1;
	public static final int SENT 			= 2;
	public static final int ERROR 			= 3;

		// alert types
	
	public static final int	AT_COMMENT		= 0;
	public static final int	AT_WARNING		= 1;
	public static final int	AT_ERROR		= 3;
	

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
		int color, 
		String text ) 
	{
		log(0,0,color,text );
	}	
	
	public static void 
	log(
		String text ) 
	{
		log(0,0,INFORMATION,text );
	}	
	
	public static String
	exceptionToString(
		Throwable 	e )
	{
		try{
			CharArrayWriter cw = new CharArrayWriter();
			
			PrintWriter pw = new PrintWriter( cw );
		
			e.printStackTrace( pw );
			
			pw.close();
			
			return( new String(cw.toCharArray()));
			
		}catch( Throwable f ){
			
			return( e.toString());
		}
	}
	
	public static void 
	log(
		int 		componentId, 
		int 		event, 
		String		text, 
		Throwable	e )
	{
		LGLoggerImpl.log(componentId,event,ERROR, text.concat(" ('").concat(exceptionToString(e)).concat("')") );
	}
	
	public static void 
	log(
		String		text, 
		Throwable	e )
	{
		log( 0, 0, text, e );
	}
	
	public static void 
	log(
		Throwable	e )
	{
		log( 0, 0, "", e );
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
	
	public static void
	logAlert(
		int			type,
		String		message )
	{
		LGLoggerImpl.logAlert(type,message);
	}
	
	public static void
	logAlert(
		String		message,
		Throwable	e )
	{
		LGLoggerImpl.logAlert(message,e);
	}
	
	public static void
	addAlertListener(
		LGAlertListener	l )
	{
		LGLoggerImpl.addAlertListener(l);
	}
	
	public static void
	removeAlertListener(
		LGAlertListener	l )
	{
		LGLoggerImpl.removeAlertListener(l);
	}
}
