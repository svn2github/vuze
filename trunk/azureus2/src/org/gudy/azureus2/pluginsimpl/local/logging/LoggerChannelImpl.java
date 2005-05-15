/*
 * File    : LoggerChannelImpl.java
 * Created : 28-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.local.logging;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.Debug;

public class 
LoggerChannelImpl 
	implements LoggerChannel
{
	private Logger		logger;
	private String		name;
	private boolean		timestamp;
	private boolean		no_output;
	private List		listeners = new ArrayList();
	
	protected
	LoggerChannelImpl(
		Logger		_logger,
		String		_name,
		boolean		_timestamp,
		boolean		_no_output )
	{
		logger		= _logger;
		name		= _name;
		timestamp	= _timestamp;
		no_output	= _no_output;
	}
		
	public Logger
	getLogger()
	{
		return( logger );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public boolean
	isEnabled()
	{
		return( LGLogger.isEnabled());
	}
	
	public void
	log(
		int		log_type,
		String	data )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((LoggerChannelListener)listeners.get(i)).messageLogged( log_type, addTimeStamp( data ));
	
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		if ( LGLogger.isEnabled() && !no_output ){
			
			data = "[".concat(name).concat("] ").concat(data);
			
			if ( log_type == LT_INFORMATION ){
				
				LGLogger.log( LGLogger.INFORMATION, data );
				
			}else if ( log_type == LT_WARNING ){
					
				LGLogger.log( LGLogger.RECEIVED, data );	// !!!!
	
			}else if ( log_type == LT_ERROR ){
					
				LGLogger.log( LGLogger.ERROR, data );
			}
		}
	}
	
	public void
	log(
		String	data )
	{
		log( LT_INFORMATION, data );
	}
	
	public void
	log(
		Throwable 	error )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((LoggerChannelListener)listeners.get(i)).messageLogged( "", error );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		if ( !no_output ){
			
			LGLogger.log("[".concat(name).concat("]"), error);
		}
	}
	
	public void
	log(
		String		str,
		Throwable 	error )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((LoggerChannelListener)listeners.get(i)).messageLogged( addTimeStamp( str ), error );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		if ( !no_output ){
			
			LGLogger.log("[".concat(name).concat("] ").concat(str), error);
		}
	}
	
	protected void
	logAlert(
		int			alert_type,
		String		message,
		boolean		repeatable )
	{
			// output as log message to any listeners
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((LoggerChannelListener)listeners.get(i)).messageLogged( alert_type, addTimeStamp( message ) );
	
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}	
		
		if ( !no_output ){
			
			int	at;
			
			switch( alert_type ){
				
				case LoggerChannel.LT_INFORMATION:
				{
					at	= LGLogger.AT_COMMENT;
					
					break;
				}	
				case LoggerChannel.LT_WARNING:
				{
					at	= LGLogger.AT_WARNING;
					
					break;
				}	
				default:
				{
					at	= LGLogger.AT_ERROR;
					
					break;
				}	
			}
			
			if ( repeatable ){
				
				LGLogger.logRepeatableAlert( at, message );
			}else{
				
				LGLogger.logUnrepeatableAlert( at, message );
			}
		}
	}
	
	public void
	logAlert(
		int			alert_type,
		String		message )
	{
		logAlert( alert_type, message, false );
	}
	
	public void
	logAlertRepeatable(
		int			alert_type,
		String		message )
	{
		logAlert( alert_type, message, true );
	}
	
	public void
	logAlert(
		String		message,
		Throwable 	e )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((LoggerChannelListener)listeners.get(i)).messageLogged( addTimeStamp( message ), e );
	
			}catch( Throwable f ){
				
				Debug.printStackTrace( f );
			}
		}	

		if ( !no_output ){
			
			LGLogger.logUnrepeatableAlert( message, e  );
		}
	}	
	
	public void
	logAlertRepeatable(
		String		message,
		Throwable 	e )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((LoggerChannelListener)listeners.get(i)).messageLogged( addTimeStamp( message ), e );
	
			}catch( Throwable f ){
				
				Debug.printStackTrace( f );
			}
		}	
		
		if ( !no_output ){
			
			LGLogger.logRepeatableAlert( message, e );
		}
	}
	
	public void
	addListener(
		LoggerChannelListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		LoggerChannelListener	l )
	{
		listeners.remove(l);
	}
	
	protected String
	addTimeStamp(
		String	data )
	{
		if ( timestamp  ){
			
			return( getTimeStamp() + data );
			
		}else{
			
			return( data );
		}
	}
	
	protected String
	getTimeStamp()
	{
		Calendar now = GregorianCalendar.getInstance();
    
		String timeStamp =
			"[" + now.get(Calendar.HOUR_OF_DAY)+ ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "] ";        

		return( timeStamp );
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
}
