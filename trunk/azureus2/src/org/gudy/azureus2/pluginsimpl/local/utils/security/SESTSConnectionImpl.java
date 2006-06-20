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

import java.nio.ByteBuffer;
import java.util.*;


import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SEPublicKeyLocator;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

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
	
	private boolean		sent_keys;
	private boolean		sent_auth;
	
	private PooledByteBuffer	pending_message;

	private AESemaphore	crypto_complete	= new AESemaphore( "SESTSConnection:send" );
	
	
	private volatile boolean	failed;
	
	
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
	
	protected void
	setFailed()
	{
		failed	= true;
		
		crypto_complete.releaseForever();
	}
	
	public void
	receive(
		PooledByteBuffer			message )
	
		throws MessageException
	{
		try{
			boolean	forward	= false;
			
			ByteBuffer	out_buffer = null;

			synchronized( this ){
		
				if ( crypto_complete.isReleasedForever()){
					
					forward	= true;
					
				}else{
						// basic sts flow:
						//   a -> puba -> b
						//   a <- pubb <- b
						//   a -> auta -> b
						//	 a <- autb <- b
						//   a -> data -> b
					
						// optimised
					
						//  a -> puba 		 -> b
						//  a <- pubb + auta <- b
						//  a -> autb + data -> b
					
						// therefore can be one or two messages in the payload
						// 	  1 crypto
						//    2 crypto (pub + auth)
						//	  crypto + data
					
						// initial a ->puba -> is done on first data send so data is ready for phase 3
					
					ByteBuffer	in_buffer = ByteBuffer.wrap( message.toByteArray());
					
					message.returnToPool();
						
						// piggyback pub key send
					
					if ( !sent_keys ){
						
							// we've received 
							//		a -> puba -> b
							// reply with
							//		a <- puba + auta <- b
						
						out_buffer = ByteBuffer.allocate( 64*1024 );

							// write our keys
						
						sts_engine.getKeys( out_buffer );
					
						sent_keys	= true;
						
							// read their keys
						
						sts_engine.putKeys( in_buffer );
					
							// write our auth
						
						sts_engine.getAuth( out_buffer );
						
						sent_auth 	= true;
						
					}else if ( !sent_auth ){
						
						out_buffer = ByteBuffer.allocate( 64*1024 );

						// we've received 
						//		a <- puba + auta <- b
						// reply with
						//		a -> autb + data -> b

							// read their keys
						
						sts_engine.putKeys( in_buffer );

							// write our auth
						
						sts_engine.getAuth( out_buffer );

						sent_auth = true;
					
							// read their auth
						
						sts_engine.putAuth( in_buffer );

							// check we wanna talk to this person
						
						byte[]	rem_key = sts_engine.getRemotePublicKey();
						
						if ( !key_locator.accept( new SEPublicKeyImpl( my_public_key.getType(), rem_key ))){
							
							throw( new MessageException( "remote public key not accepted" ));
						}
											
						ByteBuffer	pending_bb = pending_message.toByteBuffer();
						
						if ( out_buffer.remaining() >= pending_bb.remaining()){
							
							out_buffer.put( pending_bb );
							
								// don't deallocate the pending message, the original caller does this
							
							pending_message	= null;
						}
						
						crypto_complete.releaseForever();
						
					}else{
							// we've received
							//		a -> autb + data -> b
						
							// read their auth
						
						sts_engine.putAuth( in_buffer );

							// check we wanna talk to this person
						
						byte[]	rem_key = sts_engine.getRemotePublicKey();
						
						if ( !key_locator.accept( new SEPublicKeyImpl( my_public_key.getType(), rem_key ))){
							
							throw( new MessageException( "remote public key not accepted" ));
						}
						
						crypto_complete.releaseForever();
						
							// pick up any remaining data for delivery
						
						if ( in_buffer.hasRemaining()){
							
							message = new PooledByteBufferImpl( new DirectByteBuffer( in_buffer.slice()));
							
							forward	= true;
						}
					}
				}
			}
				
			if ( out_buffer != null ){
				
				out_buffer.flip();
				
				connection.send( new PooledByteBufferImpl( new DirectByteBuffer( out_buffer )));
			}
			
			if ( forward ){
				
				receiveContent( message );
			}
		}catch( Throwable e ){
			
			reportFailed( e );
			
			if ( e instanceof MessageException ){
				
				throw((MessageException)e);
				
			}else{
				
				throw( new MessageException( "Receive failed", e ));
			}
		}
	}
	
	public void
	send(
		PooledByteBuffer			message )
	
		throws MessageException
	{
		if ( failed ){
			
			throw( new MessageException( "Connection failed" ));
		}
		
		try{
			PooledByteBuffer	crypto_message	= null;
						
			synchronized( this ){
								
				if ( !sent_keys ){
					
					sent_keys	= true;
					
					ByteBuffer	buffer = ByteBuffer.allocate( 32*1024 );
							
					sts_engine.getKeys( buffer );
							
					buffer.flip();
							
					crypto_message = new PooledByteBufferImpl( new DirectByteBuffer(buffer ));
							
					pending_message = message;
				}
			}
			
			if ( crypto_message != null ){
				
				connection.send( crypto_message );
			}
			
			crypto_complete.reserve();
			
				// crypto message != null -> message has been marked as pending for send during crypto
			
			if ( crypto_message == null ){
				
				sendContent( message );
				
			}else{
				
					// if the pending message couldn't be piggy backed it'll still be allocated
				
				boolean	send_it = false;
				
				synchronized( this ){

					if ( pending_message != null ){
						
						pending_message	= null;
						
						send_it	= true;
					}
				}
				
				if ( send_it ){
					
					sendContent( message );
				}
			}
		}catch( Throwable e ){
			
			setFailed();
			
			if ( e instanceof MessageException ){
				
				throw((MessageException)e);
				
			}else{
				
				throw( new MessageException( "Send failed", e ));
			}
		}
	}
	
	protected void
	sendContent(
		PooledByteBuffer			message )
	
		throws MessageException
	{
		connection.send( message );
	}
	
	protected void
	receiveContent(
		PooledByteBuffer			message )
	
		throws MessageException
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).receive( this, message );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}	
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
				((GenericMessageConnectionListener)listeners.get(i)).connected( this );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	reportFailed(
		Throwable	error )
	{
		setFailed();
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).failed( this, error );
				
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
