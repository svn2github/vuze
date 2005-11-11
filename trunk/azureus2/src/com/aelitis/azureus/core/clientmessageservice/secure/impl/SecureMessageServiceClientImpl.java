/*
 * Created on 03-Nov-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.clientmessageservice.secure.impl;

import java.security.interfaces.RSAPublicKey;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;

import com.aelitis.azureus.core.clientmessageservice.ClientMessageService;
import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClient;
import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClientAdapter;
import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClientListener;
import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClientMessage;

public class 
SecureMessageServiceClientImpl
	implements SecureMessageServiceClient
{
		// these also occur in the server
	
	public static final int STATUS_OK					= 0;
	public static final int STATUS_LOGON_FAIL			= 1;
	public static final int STATUS_INVALID_SEQUENCE		= 2;	
	public static final int STATUS_FAILED				= 3;
	
	public static final String	SERVICE_NAME	= "SecureMsgServ";
	
	private static final long	RETRY_TIME	= 5*60*1000;

	private String									host;
	private int										port;
	private RSAPublicKey							public_key;
	private SecureMessageServiceClientAdapter		adapter;
	
	private AEMonitor			message_mon;
	private AESemaphore			message_sem;
		
	private List				messages 	= new ArrayList();
	private List				listeners	= new ArrayList();
	
	public
	SecureMessageServiceClientImpl(
		String								_host,
		int									_port,
		RSAPublicKey						_key,
		SecureMessageServiceClientAdapter	_adapter )	
	{
		host		= _host;
		port		= _port;
		public_key	= _key;
		adapter		= _adapter;
				
		message_mon	= new AEMonitor( "SecureService:messages" );
		
		message_sem = new AESemaphore( "SecureService:messages" );
		
		new AEThread( "SecureService::messageSender", true )
		{
			public void
			runSupport()
			{
				while( true ){
					
					message_sem.reserve( RETRY_TIME );
					
					try{
						sendMessages();
						
					}catch( Throwable e ){
						
						adapter.log( "Message Processing failed", e);
					}
				}
			}
		}.start();
	}
	
	protected void
	sendMessages()
	{
		List	outstanding_messages;
		
		try{
			message_mon.enter();

			outstanding_messages	= new ArrayList( messages );
			
		}finally{
			
			message_mon.exit();
		}
		
		if ( outstanding_messages.size() == 0 ){
			
			return;
		}
		
		List	sent_messages	= new ArrayList();
		
		boolean	failed = false;
		
		try{
			Iterator	it = outstanding_messages.iterator();
			
			while( it.hasNext() && !failed ){
				
				SecureMessageServiceClientMessageImpl	message = (SecureMessageServiceClientMessageImpl)it.next();
					
				boolean	retry 			= true;
				int		retry_count		= 0;
				
				while( retry && !failed ){
					
					retry	= false;
					
					ClientMessageService	message_service = null;

					try{
						Map	content	= new HashMap();				
			
						long	sequence = adapter.getMessageSequence();
						
						content.put( "user", 		adapter.getUser());
						content.put( "password", 	adapter.getPassword());
						content.put( "seq", 		new Long( sequence ));
						content.put( "request", 	message.getRequest());
						
						System.out.println( "--> " + content );
						
						message_service = SecureMessageServiceClientHelper.getServerService( host, port, SERVICE_NAME, public_key );					

						message_service.sendMessage( content );
						
						Map	reply = message_service.receiveMessage();
						
						System.out.println( "<-- " + reply );
	
						long	status = ((Long)reply.get( "status" )).longValue();
						
						if ( status == STATUS_OK ){
		
							adapter.log( "Message successfully sent: " + message.getRequest());
							
							message.setReply( (Map)reply.get( "reply" ));
							
							adapter.setMessageSequence( sequence + 1 );
							
							adapter.serverOK();
	
							for (Iterator l_it=listeners.iterator();l_it.hasNext();){
								
								try{
									((SecureMessageServiceClientListener)l_it.next()).complete( message );
									
								}catch( Throwable e ){
									
									e.printStackTrace();
								}
							}
							
							sent_messages.add( message );
							
						}else if ( status == STATUS_LOGON_FAIL ){
							
							adapter.serverOK();
							
							adapter.authenticationFailed();
							
							failed	= true;
						
						}else if ( status == STATUS_INVALID_SEQUENCE ){
							
							if ( retry_count == 1 ){
																
								adapter.serverFailed( new Exception( "Sequence resynchronisation failed" ));
								
								failed = true;
								
							}else{
							
								retry_count++;
								
								retry	= true;
								
								long	expected_sequence = ((Long)reply.get( "seq" )).longValue();
								
								adapter.log( "Sequence resynchronise: local = " + sequence + ", remote = " + expected_sequence );
								
								adapter.setMessageSequence( expected_sequence );
							}

						}else if ( status == STATUS_FAILED ){

							adapter.serverFailed( new Exception( new String( (byte[])reply.get( "error" ))));
							
							failed = true;
						}	
						
					}catch( Throwable e ){
					
						adapter.serverFailed( e );
						
						failed	= true;
						
					}finally{
							
						if ( message_service != null ){
							
							message_service.close();
						}
					}
				}
			}
		}catch( Throwable e ){
			
			adapter.serverFailed( e );
			
		}finally{
			
			try{
				message_mon.enter();
					
				messages.removeAll( sent_messages );
				
			}finally{
				
				message_mon.exit();
			}
		}
	}
	
	public SecureMessageServiceClientMessage
	sendMessage(
		Map			request,
		Object		data )
	{
		try{
			message_mon.enter();
			
			SecureMessageServiceClientMessage	res =  new SecureMessageServiceClientMessageImpl( this, request, data );
			
			messages.add( res );
			
			message_sem.release();
			
			return( res );
			
		}finally{
			
			message_mon.exit();
		}
	}
	
	protected void
	cancel(
		SecureMessageServiceClientMessage	message )
	{
		boolean	inform	= false;
		
		try{
			message_mon.enter();
			
			if ( messages.remove( message )){
				
				inform	= true;
			}
		}finally{
			
			message_mon.exit();
		}
		
		if ( inform ){
			
			for (Iterator it=listeners.iterator();it.hasNext();){
				
				try{
					((SecureMessageServiceClientListener)it.next()).cancelled( message );
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}
	}
	
	public SecureMessageServiceClientMessage[]
	getMessages()
	{
		try{
			message_mon.enter();
			
			return((SecureMessageServiceClientMessage[])messages.toArray( new SecureMessageServiceClientMessage[ messages.size()]));
			
		}finally{
			
			message_mon.exit();
		}	
	}
	
	public void
	addListener(
		SecureMessageServiceClientListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		SecureMessageServiceClientListener	l )
	{
		listeners.remove( l );
	}
}
