/*
 * Created on 13-Dec-2004
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

package com.aelitis.azureus.core.proxy.socks.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.logging.LGLogger;

import com.aelitis.azureus.core.proxy.*;
import com.aelitis.azureus.core.proxy.socks.*;

/**
 * @author parg
 *
 */

public class
AESocksProxyState
	implements AEProxyState
{
	private AESocksProxyConnection	socks_connection;
	
	protected ByteBuffer					buffer;

	protected
	AESocksProxyState(
		AESocksProxyConnection	_socks_connection )
	{
		socks_connection	= _socks_connection;
		
		if ( AESocksProxyConnectionImpl.TRACE ){
			
			LGLogger.log( socks_connection.getConnection().getName() + ":" + getStateName());
		}
	}
	
	public String
	getStateName()
	{
		String	state = this.getClass().getName();
		
		int	pos = state.indexOf( "$");
		
		state = state.substring(pos+1);
		
		return( state );
	}
	
	public final void
	read(
		SocketChannel 		sc )
	
		throws IOException
	{
		try{
			readSupport(sc);
			
		}finally{
			
			trace();
		}
	}
	
	protected void
	readSupport(
		SocketChannel 		sc )
	
		throws IOException
	{
		throw( new IOException( "Read not supported: " + sc ));
	}
	
	public final void
	write(
		SocketChannel 		sc )
	
		throws IOException
	{
		try{
			writeSupport(sc);
			
		}finally{
			
			trace();
		}		
	}	
	
	protected void
	writeSupport(
		SocketChannel 		sc )
	
		throws IOException
	{
		throw( new IOException( "Write not supported: " + sc ));
	}	
	
	public final void
	connect(
		SocketChannel 		sc )
	
		throws IOException
	{
		try{
			connectSupport(sc);
			
		}finally{
			
			trace();
		}
	}	
	
	protected void
	connectSupport(
		SocketChannel 		sc )
	
		throws IOException
	{
		throw( new IOException( "Connect not supported: " + sc ));
	}	
	
	protected void
	trace()
	{
		if ( AESocksProxyConnectionImpl.TRACE ){
			
			LGLogger.log( socks_connection.getConnection().getName() + ":" + getStateName() + ", " + buffer );
		}
	}
}

