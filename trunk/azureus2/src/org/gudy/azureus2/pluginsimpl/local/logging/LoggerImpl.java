/*
 * File    : LoggerImpl.java
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

import org.gudy.azureus2.core3.logging.LGAlertListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.*;

public class 
LoggerImpl
	implements Logger, LGAlertListener
{
	private PluginInterface	pi;
	
	private List		channels 		= new ArrayList();
	private List		alert_listeners = new ArrayList();
	
	public
	LoggerImpl(
		PluginInterface	_pi )
	{
		pi	= _pi;
		
		LGLogger.addAlertListener( this );
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( pi );
	}
	
	public LoggerChannel
	getChannel(
		String		name )
	{
		LoggerChannel	channel = new LoggerChannelImpl( this, name, false, false );
		
		channels.add( channel );
		
		return( channel );
	}
	
	public LoggerChannel
	getTimeStampedChannel(
		String		name )
	{
		LoggerChannel	channel = new LoggerChannelImpl( this, name, true, false );
		
		channels.add( channel );
		
		return( channel );
	}
	
	public LoggerChannel
	getNullChannel(
		String		name )
	{
		LoggerChannel	channel = new LoggerChannelImpl( this, name, true, true );
		
		channels.add( channel );
		
		return( channel );
	}
	
	public LoggerChannel[]
	getChannels()
	{
		LoggerChannel[]	res = new LoggerChannel[channels.size()];
		
		channels.toArray( res );
		
		return( res );
	}
	
	public void
	alertRaised(
		int		_type,
		String	message,
		boolean	repeatable )
	{
		int	type;
		
		if ( _type == LGLogger.AT_COMMENT ){
			type = LoggerChannel.LT_INFORMATION;
		}else if ( _type == LGLogger.AT_WARNING ){
			type = LoggerChannel.LT_WARNING;
		}else{
			type = LoggerChannel.LT_ERROR;
		}
		
		for (int i=0;i<alert_listeners.size();i++){
			
			try{
				((LoggerAlertListener)alert_listeners.get(i)).alertLogged( type, message, repeatable );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}	}
	
	public void
	alertRaised(
		String		message,
		Throwable	exception,
		boolean		repeatable )
	{
		for (int i=0;i<alert_listeners.size();i++){
			
			try{
				((LoggerAlertListener)alert_listeners.get(i)).alertLogged( message, exception, repeatable );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addAlertListener(
		LoggerAlertListener		listener )
	{
		alert_listeners.add( listener );
	}
	
	public void
	removeAlertListener(
		LoggerAlertListener		listener )
	{
		alert_listeners.remove( listener );
	}
}
