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

package com.aelitis.net.udp.impl;

/**
 * @author parg
 *
 */

import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.net.udp.PRUDPPacket;
import com.aelitis.net.udp.PRUDPPacketHandlerException;
import com.aelitis.net.udp.PRUDPPacketReceiver;

public class 
PRUDPPacketHandlerRequest 
{
	protected AESemaphore		sem = new AESemaphore("PRUDPPacketHandlerRequest");
	
	protected PRUDPPacketReceiver			receiver;
	
	protected PRUDPPacketHandlerException	exception;
	protected PRUDPPacket					reply;
	
	protected long							create_time;
	
	protected
	PRUDPPacketHandlerRequest(
		PRUDPPacketReceiver	_receiver )
	{
		receiver	= _receiver;
		
		create_time	= SystemTime.getCurrentTime();
	}
	
	protected long
	getCreateTime()
	{
		return( create_time );
	}
	
	protected void
	setReply(
		PRUDPPacket			packet,
		InetSocketAddress	originator )
	{
		reply	= packet;
		
		sem.release();
		
		if ( receiver != null ){
			
			receiver.packetReceived( packet, originator );
		}
	}
	
	protected void
	setException(
		PRUDPPacketHandlerException	e )
	{
		exception	= e;
		
		sem.release();
		
		if ( receiver != null ){
			
			receiver.error( e );
		}
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
