/*
 * File    : LGLoggerImpl.java
 * Created : 16-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.logging.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;

public class 
LGLoggerImpl
{
	public static final String		LOG_FILE_NAME	= "az.log";
	public static final String		BAK_FILE_NAME	= "az.log.bak";
	
	public static final String		NL = "\r\n";
	// when adding a new component, don't forget to add it to ConfigurationDefaults
	public static final int[] components = { 0, 1, 2, 4 };
	
	private static boolean			initialised = false;
	private static List				log_history	= new ArrayList();	// reported to at most one listener and then only pre-init crud
	
	private static ILoggerListener listener;

	private static List				alert_listeners	= new ArrayList();
	private static List				alert_history	= new ArrayList();
	
	
	private static boolean			log_to_file		= false;
  
    private static boolean      log_to_stdout = System.getProperty("azureus.log.stdout") != null;
    private static PrintStream  old_system_out = null;
    private static PrintStream  old_system_err = null;
    private static PrintStream	out_redirector;
    private static PrintStream	err_redirector;
    
	private static String			log_dir			= "";
	private static int				log_file_max	= 1;		// MB
	private static int        		log_types[] = new int[components.length];
	
	public static synchronized void
	initialise()
	{
		if ( !initialised ){
			
			initialised	= true;
			
	  	  	boolean overrideLog = System.getProperty("azureus.overridelog") != null;
	  	  	
	  	  	if (!overrideLog) {
	  			COConfigurationManager.addListener(
	  				new COConfigurationListener()
	  				{
	  					public void
	  					configurationSaved()
	  					{
	  						checkLoggingConfig();
	  					}
	  				});
			}
	  	  	
			checkLoggingConfig();
			
 			doRedirects();
			
			LGLogger.log( "**** Logging starts: " + Constants.AZUREUS_VERSION + " ****" );
			
			LGLogger.log( "java.home=" + System.getProperty("java.home"));
			
			LGLogger.log( "java.version=" + System.getProperty("java.version"));
			
			LGLogger.log( "os=" + 	System.getProperty("os.arch") + "/" + 
									System.getProperty("os.name") + "/" + 
									System.getProperty("os.version" ));
			
			LGLogger.log( "user.dir=" + System.getProperty("user.dir"));
			
			LGLogger.log( "user.home=" + System.getProperty("user.home"));
			
			if ( log_to_file ){
				
				for (int i=0;i<log_history.size();i++){
					
					Object[]	entry = (Object[])log_history.get(i);
					
					log(	((Integer)entry[0]).intValue(),
							((Integer)entry[1]).intValue(),
							((Integer)entry[2]).intValue(),
							(String)entry[3] );
				}
			}
			
			log_history.clear();
	
		}
	}
	
	public static void
	checkRedirection()
	{
	 	  boolean overrideLog = System.getProperty("azureus.overridelog") != null;
	 	  
	  	  if (!overrideLog) {
	 		
	  	  	doRedirects();
	  	  }
	}
	
	protected static void
	doRedirects()
	{
		try{
		
			if ( System.out != out_redirector ){
			
				if ( old_system_out == null ){
					
					old_system_out = System.out;
				}
	      
				out_redirector = new PrintStream(new redirectorOutputStream( old_system_out ));
				
				System.setOut( out_redirector );
			}
			
			if ( System.err != err_redirector ){
				
				if ( old_system_err == null ){
					
					old_system_err = System.err;
				}
	      
				err_redirector = new PrintStream(new redirectorOutputStream( old_system_err ));
				
				System.setErr( err_redirector );
			}
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected static void
	checkLoggingConfig()
	{
	  boolean overrideLog = System.getProperty("azureus.overridelog") != null;
	  if (overrideLog) {
	    log_to_file = true;
	    log_dir = ".";
	    log_file_max = 2;
  		for (int i = 0; i < log_types.length; i++) {
    		log_types[i] = 0;
        for (int j = 0; j <= 3; j++)
      		log_types[i] |= true ? (1 << j) : 0;
    	}
	  } else {
  		log_to_file 	= COConfigurationManager.getBooleanParameter("Logging Enable", false );
  		
  		log_dir			= COConfigurationManager.getStringParameter("Logging Dir", "" );
  		
  		log_file_max	= COConfigurationManager.getIntParameter("Logging Max Size" );
  		for (int i = 0; i < log_types.length; i++) {
    		log_types[i] = 0;
        for (int j = 0; j <= 3; j++)
      		log_types[i] |= COConfigurationManager.getBooleanParameter("bLog" + components[i] + "-" + j) ? (1 << j) : 0;
    	}
		}
		
	}

	public static synchronized void 
	log(
		int componentId, 
		int event, 
		int color, 
		String text) 
	{
		if ( initialised ){
			if ( log_to_file ){
			  int logTypeIndex = 0;
	  		for (int i = 0; i < components.length; i++) {
	  		  if (components[i] == componentId) {
	  		    logTypeIndex = i;
	  		    break;
	  		  }
	  		}
	  		if ((log_types[logTypeIndex] & (1 << color)) != 0)
	  			logToFile("{" + componentId + ":" + event + ":" + color + "}  " + text + NL);
			}
			
			if( listener !=  null ){
			
				listener.log(componentId,event,color,text);
			} 
		}else{
			
			log_history.add( new Object[]{	new Integer(componentId),
											new Integer(event),
											new Integer( color ),
											text  });
									
		}
	}
  
	public static boolean
	isLoggingOn()
	{
		return( listener != null || log_to_file );
	}
	
	public static synchronized void 
	setListener(ILoggerListener _listener) 
	{
		listener = _listener;
	}
  
	public static synchronized void 
	removeListener() 
	{		
		listener = null;
	}
	
	public static void
	logAlert(
		int			type,
		String		message,
		boolean		repeatable )
	{
		LGLogger.log( "Alert:" + type + ":" + message );
		
		alert_history.add( new Object[]{ new Integer(type), message, new Boolean(repeatable)});
		
		if ( alert_history.size() > 256 ){
			
			alert_history.remove(0);
		}
		
		for (int i=0;i<alert_listeners.size();i++){
			
			try{
				
				((LGAlertListener)alert_listeners.get(i)).alertRaised( type, message, repeatable  );
				
			}catch( Throwable f ){
				
				f.printStackTrace();
			}
		}
	}
	
	public static void
	logAlert(
		String		message,
		Throwable	e,
		boolean		repeatable )
	{
		LGLogger.log( "Alert:" + message, e );
	
		alert_history.add( new Object[]{ message, e, new Boolean( repeatable ) });
		
		if ( alert_history.size() > 256 ){
			
			alert_history.remove(0);
		}
	
		for (int i=0;i<alert_listeners.size();i++){
			
			try{
				((LGAlertListener)alert_listeners.get(i)).alertRaised( message, e, repeatable );
				
			}catch( Throwable f ){
				
				f.printStackTrace();
			}
		}
	}
	
	public static void
	addAlertListener(
		LGAlertListener	l )
	{
		alert_listeners.add(l);
		
		for (int i=0;i<alert_history.size();i++){
			
			Object[]	entry = (Object[])alert_history.get(i);
			
			boolean repeatable = ((Boolean)entry[2]).booleanValue();
			
			if ( entry[0] instanceof Integer ){
				
				l.alertRaised(((Integer)entry[0]).intValue(),(String)entry[1], repeatable);
				
			}else{
				
				l.alertRaised((String)entry[0],(Throwable)entry[1], repeatable);
			}
		}
	}
	
	public static void
	removeAlertListener(
		LGAlertListener	l )
	{
		alert_listeners.remove(l);
	}
	
	protected static void
	logToFile(
		String	str )
	{
		if ( log_to_file ){

			synchronized( LGLogger.class ){
			
				Calendar now = GregorianCalendar.getInstance();
				        
				 String timeStamp =
				   "[".concat(String.valueOf(now.get(Calendar.HOUR_OF_DAY))).concat(":").concat(format(now.get(Calendar.MINUTE))).concat(":").concat(format(now.get(Calendar.SECOND))).concat("]  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$        
		
				str = timeStamp.concat(str);
				
				PrintWriter	pw = null;
				
				File	file_name = new File( log_dir.concat(File.separator).concat(LOG_FILE_NAME) );
				
				try{		
							
					pw = new PrintWriter(new FileWriter( file_name, true ));
				
					pw.print( str );
	         
	                if( log_to_stdout ) old_system_out.println( str );
					
				}catch( Throwable e ){
					
					// can't log this as go recursive!!!!
					
				}finally{
					
					if ( pw != null ){
						
						try{
						
							pw.close();
							
						}catch( Throwable e ){
							
							// can't log as go recursive!!!!
						}
						
						long	max_bytes = (log_file_max*1024*1024)/2;	// two files so half
						
						if ( file_name.length() > max_bytes ){
							
							File	back_name = new File( log_dir.concat(File.separator).concat(BAK_FILE_NAME) );
							
							if ( (!back_name.exists()) || back_name.delete()){
							
								if ( !file_name.renameTo( back_name )){
									
									file_name.delete();
								}
								
							}else{
								
								file_name.delete();
							}
						}
					}
				}
			}
		}
	}
	
	private static String format(int n) {
	   if(n < 10) return "0".concat(String.valueOf(n)); //$NON-NLS-1$
	   return String.valueOf(n); //$NON-NLS-1$
	 }  
	 
	protected static class
	redirectorOutputStream
		extends OutputStream
	{
		protected PrintStream		ps;
		
		protected StringBuffer	buffer = new StringBuffer(1024);
		
		protected
		redirectorOutputStream(
			PrintStream		_ps )
		{
			ps	= _ps;
		}
			
		public void
		write(
			int		data )
		{
			char	c = (char)data;
			
			buffer.append(c);
			
			if ( c == '\n'){
			
				ps.print(buffer);
			
				logToFile(buffer.toString());
				
				buffer.setLength(0);
			}
		}
		
  	    public void 
		write(
			byte 	b[],
			int 	off, 
			int 	len )
  	    {
  	    	for (int i=off;i<off+len;i++){
  	    		int	d = b[i];
  	    		if ( d < 0 )d+=256;
  	    		write(d);
  	    	}
  	    }
	}
}
