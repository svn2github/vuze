/*
 * File    : PRUDPPacketReceiverFactoryImpl.java
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

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.net.udp.PRUDPPacketHandler;
import com.aelitis.net.udp.PRUDPRequestHandler;

public class 
PRUDPPacketHandlerFactoryImpl 
{
	protected static 			Map	receiver_map = new HashMap();
	protected static AEMonitor	class_mon	= new AEMonitor( "PRUDPPHF" );


	public static PRUDPPacketHandler
	getHandler(
		int						port,
		PRUDPRequestHandler		request_handler)
	{
		try{
			class_mon.enter();
		
			PRUDPPacketHandlerImpl	receiver = (PRUDPPacketHandlerImpl)receiver_map.get(new Integer(port));
			
			if ( receiver == null ){
				
				receiver = new PRUDPPacketHandlerImpl( port );
				
				receiver_map.put( new Integer(port), receiver );
			}
			
			receiver.setRequestHandler( request_handler );
			
			return( receiver );
			
		}finally{
			
			class_mon.exit();
		}
	}		
}
