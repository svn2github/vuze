/*
 * Created on 08-Dec-2004
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

import com.aelitis.azureus.core.proxy.socks.*;
import com.aelitis.azureus.core.proxy.*;

/**
 * @author parg
 *
 */

public class 
AESocksProxyImpl 
	implements AESocksProxy, AEProxyHandler
{
	protected AEProxy									proxy;
	protected AESocksProxyPlugableConnectionFactory		connection_factory;
	
	public
	AESocksProxyImpl(
		int										_port,
		long									_ct,
		long									_rt,
		AESocksProxyPlugableConnectionFactory	_connection_factory )
	
		throws AEProxyException
	{
		connection_factory	= _connection_factory;
		
		proxy = AEProxyFactory.create( _port, _ct, _rt, this );
	}
	
	public int
	getPort()
	{
		return( proxy.getPort());
	}
	
	public AESocksProxyPlugableConnection
	getDefaultPlugableConnection(
		AESocksProxyConnection		basis )
	{
		return( new AESocksProxyPlugableConnectionDefault(basis ));
	}
	
	public AEProxyState
	getInitialState(
		AEProxyConnection	connection )
	{
		return( new AESocksProxyConnectionImpl( this, connection_factory, connection ).getInitialState());
	}
}
