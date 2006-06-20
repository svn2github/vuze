/*
 * Created on 20 Jun 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.utils.security;

import java.util.*;


import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SEPublicKeyLocator;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.security.CryptoSTSEngine;

public class 
SESTSConnectionImpl
	implements GenericMessageConnection
{
	private GenericMessageConnection	connection;
	private SEPublicKey					my_public_key;
	private SEPublicKeyLocator			key_locator;
	
	private CryptoSTSEngine	sts_engine;
	
	private List	listeners = new ArrayList();
	
	protected
	SESTSConnectionImpl(
		AzureusCore					_core,
		GenericMessageConnection	_connection,
		SEPublicKey					_my_public_key,
		SEPublicKeyLocator			_key_locator,
		String						_reason )
	
		throws Exception
	{
		connection		= _connection;
		my_public_key	= _my_public_key;
		key_locator		= _key_locator;
		
		sts_engine	= _core.getCryptoManager().getECCHandler().getSTSEngine( _reason );
		
		connection.addListener(
			new GenericMessageConnectionListener()
			{
				public void
				connected(
					GenericMessageConnection	connection )
				{
					reportConnected();
				}
				
				public void
				receive(
					GenericMessageConnection	connection,
					PooledByteBuffer			message )
				
					throws MessageException
				{
					SESTSConnectionImpl.this.receive( message );
				}
				
				public void
				failed(
					GenericMessageConnection	connection,
					Throwable 					error )
				
					throws MessageException
				{
					reportFailed( error );
				}
			});
	}
	
	public GenericMessageEndpoint
	getEndpoint()
	{
		return( connection.getEndpoint());
	}
	
	public void
	connect()
	
		throws MessageException
	{
		connection.connect();
	}
	
	public void
	receive(
		PooledByteBuffer			message )
	
		throws MessageException
	{
		
	}
	
	public void
	send(
		PooledByteBuffer			message )
	
		throws MessageException
	{
		
	}
	
	public void
	close()
	
		throws MessageException
	{
		connection.close();
	}
	
	protected void
	reportConnected()
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).connected( connection );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	reportFailed(
		Throwable	error )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).failed( connection, error );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	public void
	addListener(
		GenericMessageConnectionListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		GenericMessageConnectionListener		listener )
	{
		listeners.remove( listener );
	}
}
