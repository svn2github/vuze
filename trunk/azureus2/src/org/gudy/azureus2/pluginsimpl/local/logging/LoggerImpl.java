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

import org.gudy.azureus2.plugins.logging.*;

public class 
LoggerImpl
	implements Logger
{
	protected List		channels = new ArrayList();
	
	public
	LoggerImpl()
	{
	}
	
	public LoggerChannel
	getChannel(
		String		name )
	{
		LoggerChannel	channel = new LoggerChannelImpl( name );
		
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
}
