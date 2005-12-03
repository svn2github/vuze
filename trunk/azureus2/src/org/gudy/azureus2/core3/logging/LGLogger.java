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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.internat.*;

/** Use Logger
 * @deprecated use Logger
 * @see Logger
 */
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
  
  //categories
  public static final int CORE_SYSTEM   = 10;
  public static final int CORE_NETWORK  = 11;
  public static final int CORE_DISK     = 12;
  

  // until LGLogger is removed..
  private static boolean bUseLogger = true;

	public static void
	initialise()
	{
		if (bUseLogger) {
			System.out.println("Don't call LGLogger.initialise!");
			System.out.println(Debug.getStackTrace(false, false));
			return;
		}

		LGLoggerImpl.initialise();
	}
	
	private static LogIDs componentIDtoLogID(int componentId) {
		LogIDs logID;
		if (componentId == 1)
			logID = LogIDs.PEER;
		else if (componentId == 1)
			logID = LogIDs.TRACKER;
		else
			logID = LogIDs.CORE;
		return logID;
	}
	
	public static void 
	log(
		int componentId, 
		int event, 
		int color, 
		String text ) 
	{
		if (bUseLogger) {
			Logger.log(new LogEvent(componentIDtoLogID(componentId), color, text));
			return;
		}
			
		LGLoggerImpl.log(componentId,event,color,text );
	}	
	
	public static void 
	log(
		int category, 
		String text ) 
	{
		log(0,category,0,text );
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
		if (bUseLogger) {
			Logger.log(new LogEvent(componentIDtoLogID(componentId), text + " - "
					+ exceptionToString(e), e));
			return;
		}

		LGLoggerImpl.log(componentId,event,ERROR, text + " - " + exceptionToString(e));
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
	
	public static void
	checkRedirection()
	{
		if (bUseLogger) {
			Logger.doRedirects();
			return;
		}

		LGLoggerImpl.checkRedirection();
	}
	
	public static boolean
	isLoggingOn()
	{
		if (bUseLogger) {
			return Logger.isEnabled();
		}

		return( LGLoggerImpl.isLoggingOn());
	}
  
  
  /**
   * Is the logger component enabled.
   * @return true if logger is enabled, false if disabled
   */
  public static boolean isEnabled() {   
		if (bUseLogger) {
			return Logger.isEnabled();
		}

    return LGLoggerImpl.isEnabled();
  }
	
  
	public static void
	setListener(
		ILoggerListener	listener )
	{
		if (bUseLogger) {
			System.out.println("LGLogger.setListener() deprecated.  Use Logger.addListener");
			System.out.println(Debug.getStackTrace(false, false));
			return;
		}

		LGLoggerImpl.setListener( listener );
	}	
	
	public static void
	removeListener()
	{
		if (bUseLogger) {
			System.out.println("LGLogger.removeListener() deprecated.  Use Logger.removeListener");
			System.out.println(Debug.getStackTrace(false, false));
			return;
		}

		LGLoggerImpl.removeListener();
	}
	
	public static void
	logUnrepeatableAlert(
		int			type,
		String		message )
	{
		if (bUseLogger) {
			Logger.log(new LogAlert(LogAlert.UNREPEATABLE, type, message));
			return;
		}

		LGLoggerImpl.logAlert(type,message,false);
	}
	
	public static void
	logRepeatableAlert(
		int			type,
		String		message )
	{
		if (bUseLogger) {
			Logger.log(new LogAlert(LogAlert.REPEATABLE, type, message));
			return;
		}

		LGLoggerImpl.logAlert(type,message,true);
	}
	
	public static void
	logUnrepeatableAlertUsingResource(
		int			type,
		String		resource_key )
	{
		if (bUseLogger) {
			Logger.log(new LogAlert(LogAlert.UNREPEATABLE, type, MessageText
					.getString(resource_key)));
			return;
		}

		LGLoggerImpl.logAlert( type, MessageText.getString( resource_key ), false);
	}

	public static void
	logUnrepeatableAlertUsingResource(
		int			type,
		String		resource_key,
		String[]	params )
	{
		if (bUseLogger) {
			Logger.log(new LogAlert(LogAlert.UNREPEATABLE, type, MessageText
					.getString(resource_key, params)));
			return;
		}

		LGLoggerImpl.logAlert( type, MessageText.getString( resource_key, params ), false);
	}

	public static void
	logUnrepeatableAlert(
		String		message,
		Throwable	e )
	{
		if (bUseLogger) {
			Logger.log(new LogAlert(LogAlert.UNREPEATABLE, message, e));
			return;
		}

		LGLoggerImpl.logAlert(message,e,false);
	}
	
	public static void
	logRepeatableAlert(
		String		message,
		Throwable	e )
	{
		if (bUseLogger) {
			Logger.log(new LogAlert(LogAlert.REPEATABLE, message, e));
			return;
		}

		LGLoggerImpl.logAlert(message,e,true );
	}
	
	public static void
	addAlertListener(
		LGAlertListener	l )
	{
		if (bUseLogger) {
			System.out.println("LGLogger.addAlertListener() deprecated.  Use Logger.addListener");
			System.out.println(Debug.getStackTrace(false, false));
			return;
		}

		LGLoggerImpl.addAlertListener(l);
	}
	
	public static void
	removeAlertListener(
		LGAlertListener	l )
	{
		if (bUseLogger) {
			System.out.println("LGLogger.removeAlertListener() deprecated.  Use Logger.removeListener");
			System.out.println(Debug.getStackTrace(false, false));
			return;
		}

		LGLoggerImpl.removeAlertListener(l);
	}
}
