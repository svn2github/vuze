/*
 * File    : PRUDPPacketHandlerRequest.java
 * Created : 20-Jan-2004
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

package org.gudy.azureus2.core3.tracker.protocol.udp.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.protocol.udp.*;

public class 
PRUDPPacketHandlerRequest 
{
	protected Semaphore		sem = new Semaphore();
	
	protected PRUDPPacketHandlerException	exception;
	protected PRUDPPacket					reply;
	
	protected long							create_time;
	
	protected
	PRUDPPacketHandlerRequest()
	{
		create_time	= SystemTime.getCurrentTime();
	}
	
	protected long
	getCreateTime()
	{
		return( create_time );
	}
	
	protected void
	setReply(
		PRUDPPacket		packet )
	{
		reply	= packet;
		
		sem.release();
	}
	
	protected void
	setException(
		PRUDPPacketHandlerException	e )
	{
		exception	= e;
		
		sem.release();
	}
	
	protected PRUDPPacket
	getReply()
	
		throws PRUDPPacketHandlerException
	{
		sem.reserve();
		
		if ( exception != null ){
			
			throw( exception );
		}
			
		return( reply );
	}
}
