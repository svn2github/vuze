/*
 * Created on 15 Jun 2006
 * Created by Aaron Grunthal and Paul Gardner
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

package com.aelitis.azureus.core.security.impl;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;

import org.bouncycastle.jce.provider.JCEECDHKeyAgreement;

import com.aelitis.azureus.core.security.CryptoManagerException;
import com.aelitis.azureus.core.security.CryptoSTSEngine;



/**
 * STS authentication protocol using a symmetric 4 message ECDH/ECDSA handshake  
 */
final class 
CryptoSTSEngineImpl 
	implements CryptoSTSEngine
{
	public static final int	VERSION	= 1;

	public static final int AUTH_FAILED = 0;

	public static final int SEND_PUBKEY = 1;
	public static final int WAIT_OTHER_PUBKEY = SEND_PUBKEY;

	public static final int SEND_AUTH = 2;
	public static final int WAIT_OTHER_AUTH = SEND_AUTH;

	public static final int AUTHED = 3;

	private CryptoHandlerECC	handler;

	private KeyPair 	ephemeralKeyPair;
	
	private PublicKey	myPublicKey;
	private PrivateKey	myPrivateKey;
	private PublicKey 	remotePubKey;
	private byte[] 		sharedSecret;
	private int 		state;
	
	private JCEECDHKeyAgreement ecDH;
	
	/**
	 * 
	 * @param myIdent keypair representing our current identity
	 */
	
	CryptoSTSEngineImpl(
		CryptoHandlerECC	_handler,
		PublicKey			_myPub,
		PrivateKey			_myPriv )
		
		throws CryptoManagerException
	{
		handler			= _handler;
		myPublicKey		= _myPub;
		myPrivateKey	= _myPriv;
		
		state = SEND_PUBKEY;
		
		ephemeralKeyPair = handler.createKeys();
		
		try{
			ecDH = new JCEECDHKeyAgreement.DH();
			
			//ecDH = KeyAgreement.getInstance("ECDH", "BC");
			
			ecDH.init(ephemeralKeyPair.getPrivate());
			
		}catch (Exception e){
			
			throw new CryptoManagerException("Couldn't initialize crypto handshake", e);
		}
	}

	public void 
	putMessage(
		ByteBuffer		message )
	
		throws CryptoManagerException
	{
		try{
			int	version = getInt( message, 255 );
			
			if ( version != VERSION ){
				
				throw( new CryptoManagerException( "invalid version" ));
			}
			
			int	message_state = getInt( message, 255 );
			
			if( message_state != state){
				
				throw( new CryptoManagerException( "Unexpected state: current = " + state + ", received = " + message_state ));
			}
				
			if ( state == WAIT_OTHER_PUBKEY ){
			
				final byte[] rawRemoteIdentPubkey = getBytes( message, 65535 );
				
				final byte[] rawRemoteEphemeralPubkey = getBytes( message, 65535 );
	
				final byte[] remoteSig = getBytes( message, 65535 );
				
				final byte[] pad = getBytes( message, 65535 );
				
				remotePubKey = handler.rawdataToPubkey(rawRemoteIdentPubkey);
				
				Signature check = handler.getSignature(remotePubKey);
	
				check.update(rawRemoteIdentPubkey);
				
				check.update(rawRemoteEphemeralPubkey);
				
				if ( check.verify(remoteSig)){
					
					state = SEND_AUTH;
					
					ecDH.doPhase(handler.rawdataToPubkey(rawRemoteEphemeralPubkey), true);
					
					sharedSecret = ecDH.generateSecret();
					
				}else{
												
					throw( new CryptoManagerException( "Signature check failed" ));
				}
				
			}else if( state == WAIT_OTHER_AUTH ){
				
				final byte[] IV = getBytes( message, 65535 );
				
				final byte[] remoteSig = getBytes( message, 65535);
							
				Signature check = handler.getSignature( remotePubKey );
				
				check.update(IV);
					
				check.update(sharedSecret);
					
				if ( check.verify(remoteSig)){
						
					state = AUTHED;
						
				}else{
					
					throw( new CryptoManagerException( "Signature check failed" ));
						
				}
			}
		}catch( CryptoManagerException	e ){
			
			state	= AUTH_FAILED;
			
			throw( e );
			
		}catch( Throwable e ){
			
			state	= AUTH_FAILED;
			
			throw( new CryptoManagerException( "Failed to generate message" ));
		}
	}
	
	public void 
	getMessage(
		ByteBuffer	buffer )
	
		throws CryptoManagerException
	{
		try{
			putInt( buffer, VERSION, 255 );
						
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

			Signature sig = handler.getSignature(myPrivateKey);
			
			if ( state == SEND_PUBKEY ){
				
				final byte[] rawIdentPubkey = handler.keyToRawdata(myPublicKey);
				
				final byte[] rawEphemeralPubkey = handler.keyToRawdata(ephemeralKeyPair.getPublic());
				
				sig.update(rawIdentPubkey);
					
				sig.update(rawEphemeralPubkey);
					
				final byte[] rawSign = sig.sign();
				
				final byte[] pad = new byte[random.nextInt(32)];
				
				random.nextBytes(pad);

				putInt( buffer, state, 255 );
				
				putBytes( buffer, rawIdentPubkey, 65535 );
				
				putBytes( buffer, rawEphemeralPubkey, 65535 );
				
				putBytes( buffer, rawSign, 65535 );
				
				putBytes( buffer, pad, 65535 );
	
			}else if( state == SEND_AUTH ){
								
				final byte[] IV = new byte[20 + random.nextInt(32)];
				
				random.nextBytes(IV);

				sig.update(IV);
				
				sig.update(sharedSecret);
				
				final byte[] rawSig = sig.sign();

				putInt( buffer, state, 255 );

				putBytes( buffer, IV, 65535 );

				putBytes( buffer, rawSig, 65535 );

			}else{
			
				throw( new CryptoManagerException( "Invalid state" ));
			}
			
		}catch( CryptoManagerException	e ){
			
			state	= AUTH_FAILED;
			
			throw( e );
			
		}catch( Throwable e ){
			
			state	= AUTH_FAILED;
			
			throw( new CryptoManagerException( "Failed to generate message" ));
		}
	}
	
	public byte[] 
	getSharedSecret()
	{
		return sharedSecret;
	}
	
	public PublicKey 
	getRemotePubkey()
	{
		return remotePubKey;
	}
	
	int getState()
	{
		return state;
	}
	
	protected int
	getInt(
		ByteBuffer	buffer,
		int			max_size )
	
		throws CryptoManagerException
	{
		try{
			if ( max_size < 256 ){
				
				return( buffer.get() & 0xff);
				
			}else if ( max_size < 65536 ){
				
				return( buffer.getShort() & 0xffff);
				
			}else{
				
				return( buffer.getInt());
			}
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Failed to get int", e ));
		}
	}
	
	protected byte[]
	getBytes(
		ByteBuffer	buffer,
		int			max_size )
	
		throws CryptoManagerException
	{
		int	len = getInt( buffer, max_size );
		
		try{
			byte[]	res = new byte[len];
			
			buffer.get( res );
			
			return( res );
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Failed to get byte[]", e ));
		}
	}
	
	protected void
	putInt(
		ByteBuffer	buffer,
		int			value,
		int			max_size )
	
		throws CryptoManagerException
	{
		try{
			if ( max_size < 256 ){
				
				buffer.put((byte)value);
				
			}else if ( max_size < 65536 ){
				
				buffer.putShort((short)value );
				
			}else{
				
				buffer.putInt( value );
			}
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Failed to put int", e ));
		}
	}
	
	protected void
	putBytes(
		ByteBuffer	buffer,
		byte[]		value,
		int			max_size )
	
		throws CryptoManagerException
	{
		putInt( buffer, value.length, max_size );
		
		try{
			buffer.put( value );
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Failed to put byte[]", e ));
		}
	}
}
