/*
 * Created on 22-Sep-2004
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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;

public class 
AEDiagnostics 
{
		// these can not be set true and have a usable AZ!
	
	public static final boolean	ALWAYS_PASS_HASH_CHECKS			= false;
	public static final boolean	USE_DUMMY_FILE_DATA				= false;
	public static final boolean	CHECK_DUMMY_FILE_DATA			= false;

		// these can safely be set true, things will work just slower
	
	public static final boolean	DEBUG_MONITOR_SEM_USAGE			= false;
	
	public static final boolean	TRACE_DIRECT_BYTE_BUFFERS		= false;
	public static final boolean	TRACE_DBB_POOL_USAGE			= false;
	public static final boolean	PRINT_DBB_POOL_USAGE			= false;
	
	static{
		if ( ALWAYS_PASS_HASH_CHECKS ){
			System.out.println( "**** Always passing hash checks ****" );
		}
		if ( USE_DUMMY_FILE_DATA ){
			System.out.println( "**** Using dummy file data ****" );
		}
		if ( CHECK_DUMMY_FILE_DATA ){
			System.out.println( "**** Checking dummy file data ****" );
		}
		if ( DEBUG_MONITOR_SEM_USAGE ){
			System.out.println( "**** AEMonitor/AESemaphore debug on ****" );
		}
		if ( TRACE_DIRECT_BYTE_BUFFERS ){
			System.out.println( "**** DirectByteBuffer tracing on ****" );
		}
		if ( TRACE_DBB_POOL_USAGE ){
			System.out.println( "**** DirectByteBufferPool tracing on ****" );
		}
		if ( PRINT_DBB_POOL_USAGE ){
			System.out.println( "**** DirectByteBufferPool printing on ****" );
		}
	}
	
	private static final int	MAX_FILE_SIZE	= 128*1024;	// get two of these per logger type
	
	private static final String	CONFIG_KEY	= "diagnostics.tidy_close";
	
	private static File	debug_dir;

	private static File	debug_save_dir;
	
	private static boolean	started_up;
	private static Map		loggers	= new HashMap();
	
	public static synchronized void
	startup()
	{
		if ( started_up ){
			
			return;
		}
		
		started_up	= true;
		
		try{
			debug_dir		= FileUtil.getUserFile( "logs" );
			
			debug_save_dir	= new File( debug_dir, "save" );
			
			boolean	was_tidy	= COConfigurationManager.getBooleanParameter( CONFIG_KEY );
			
			COConfigurationManager.setParameter( CONFIG_KEY, false );
			
			COConfigurationManager.save();
			
			if ( debug_dir.exists()){
				
				debug_save_dir.mkdir();
				
				File[]	files = debug_dir.listFiles();
				
				if ( files != null ){
					
					boolean	file_moved	= false;
					
					long	now = SystemTime.getCurrentTime();
					
					for (int i=0;i<files.length;i++){
						
						File	file = files[i];
						
						if ( file.isDirectory()){
							
							continue;
						}
						
						if ( !was_tidy ){
				
							file_moved	= true;
							
							FileUtil.renameFile( file, new File( debug_save_dir, now + "_" + file.getName()));
							
						}else{
							
							// leave the file there, it'll get appended to
							// file.delete();
						}
					}
					
					if ( file_moved ){
						
						LGLogger.logAlertUsingResource(
							LGLogger.AT_WARNING,
							"diagnostics.log_found",
							new String[]{ debug_save_dir.toString() } );
					}
				}
			}else{
				
				debug_dir.mkdir();
			}
		}catch( Throwable e ){
			
				// with webui we don't have the file stuff so this fails with class not found
			
			if ( !(e instanceof NoClassDefFoundError )){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public static synchronized AEDiagnosticsLogger
	getLogger(
		String		name )
	{
		AEDiagnosticsLogger	logger = (AEDiagnosticsLogger)loggers.get(name);
		
		if ( logger == null ){
			
			startup();
			
			logger	= new AEDiagnosticsLogger( name );
			
			try{
				File	f1 = getLogFile( logger );
				
				logger.setFirstFile( false );
				
				File	f2 = getLogFile( logger );
				
				logger.setFirstFile( true );
	
					// if we were writing to the second file, carry on from there
				
				if ( f1.exists() && f2.exists()){
		
					if ( f1.lastModified() < f2.lastModified()){
						
						logger.setFirstFile( false );	
					}
				}
			}catch( Throwable ignore ){
				
			}
			
			loggers.put( name, logger );
		}
		
		return( logger );
	}

	protected static synchronized void
	log(
		AEDiagnosticsLogger		logger,
		String					str )
	{
		try{
			
			File	log_file	= getLogFile( logger );
			
			if ( log_file.exists() && log_file.length() >= MAX_FILE_SIZE ){
				
				logger.setFirstFile(!logger.isFirstFile());
				
				log_file	= getLogFile( logger );
			
				if ( log_file.exists()){
					
					log_file.delete();
				}
			}
			
			Calendar now = GregorianCalendar.getInstance();
	        
			String timeStamp =
				"[" + now.get(Calendar.HOUR_OF_DAY)+ ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "] ";        

			str = timeStamp + str;
	
			PrintWriter	pw = null;
	
			try{		
						
				pw = new PrintWriter(new FileWriter( log_file, true ));
			
				pw.println( str );
		 							
			}finally{
				
				if ( pw != null ){
										
					pw.close();
				}
			}
		}catch( Throwable ignore ){
			
		}
	}
	
	private static File
	getLogFile(
		AEDiagnosticsLogger		logger )
	{
		return( new File( debug_dir, logger.getName() + "_" + (logger.isFirstFile()?"1":"2") + ".log" ));
	}
	
	private static String 
	format(
		int 	n ) 
	{
		if (n < 10){
	   	
			return( "0" + n );
	   }
		
	   return( String.valueOf(n));
	}
	
	protected static void
	log(
		AEDiagnosticsLogger		logger,
		Throwable				e )
	{
		try{
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			PrintWriter	pw = new PrintWriter( new OutputStreamWriter( baos ));
			
			e.printStackTrace( pw );
			
			pw.close();
			
			log( logger, baos.toString());
			
		}catch( Throwable ignore ){
			
		}
	}
	
	public static void
	shutdown()
	{
		try{
			COConfigurationManager.setParameter( CONFIG_KEY, true );
			
			COConfigurationManager.save();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
}
