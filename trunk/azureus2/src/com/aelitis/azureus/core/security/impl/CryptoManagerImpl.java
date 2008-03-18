/*
 * Created on 15 Jun 2006
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

package com.aelitis.azureus.core.security.impl;

import java.util.*;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler.passwordDetails;

public class 
CryptoManagerImpl 
	implements CryptoManager
{
	private static final int 	PBE_ITERATIONS	= 100;
	private static final String	PBE_ALG			= "PBEWithMD5AndDES";
	
	private static CryptoManagerImpl		singleton;
	
	
	public static synchronized CryptoManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new CryptoManagerImpl();
		}
		
		return( singleton );
	}
	
	private byte[]				secure_id;
	private CryptoHandler		ecc_handler;
	private List				listeners	= Collections.synchronizedList( new ArrayList());
	
	protected
	CryptoManagerImpl()
	{
		SESecurityManager.initialise();
		
		ecc_handler = new CryptoHandlerECC( this, 1 );
	}
	
	public byte[]
	getSecureID()
	{
		String key = CryptoManager.CRYPTO_CONFIG_PREFIX + "id";
		
		if ( secure_id == null ){
			
			secure_id = COConfigurationManager.getByteParameter( key, null );
		}
		
		if ( secure_id == null ){
			
			secure_id = new byte[20];
		
			new SecureRandom().nextBytes( secure_id );
			
			COConfigurationManager.setParameter( key, secure_id );
			
			COConfigurationManager.save();
		}
		
		return( secure_id );
	}
	
	public CryptoHandler
	getECCHandler()
	{
		return( ecc_handler );
	}
	
	protected byte[]
	encryptWithPBE(
		byte[]		data,
		char[]		password )
	
		throws CryptoManagerException
	{
		try{
			byte[]	salt = new byte[8];
			
			new SecureRandom().nextBytes( salt );
			
			PBEKeySpec keySpec = new PBEKeySpec(password);
		
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( PBE_ALG );
		
			SecretKey key = keyFactory.generateSecret(keySpec);
		
			PBEParameterSpec paramSpec = new PBEParameterSpec( salt, PBE_ITERATIONS );
		
			Cipher cipher = Cipher.getInstance( PBE_ALG );
			
			cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
	
			byte[]	enc = cipher.doFinal( data );
			
			byte[]	res = new byte[salt.length + enc.length];
			
			System.arraycopy( salt, 0, res, 0, salt.length );
			
			System.arraycopy( enc, 0, res, salt.length, enc.length );
			
			return( res );
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "PBE encryption failed", e ));
		}
	}
	
	protected byte[]
   	decryptWithPBE(
   		byte[]		data,
   		char[]		password )
	
		throws CryptoManagerException
   	{
		boolean fail_is_pw_error = false;
		
		try{
			byte[]	salt = new byte[8];
			
			System.arraycopy( data, 0, salt, 0, 8 );
			
			PBEKeySpec keySpec = new PBEKeySpec(password);
	
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( PBE_ALG );
	
			SecretKey key = keyFactory.generateSecret(keySpec);
	
			PBEParameterSpec paramSpec = new PBEParameterSpec(salt, PBE_ITERATIONS);
	
			Cipher cipher = Cipher.getInstance( PBE_ALG );
			
			cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
	
			fail_is_pw_error = true;
			
			return( cipher.doFinal( data, 8, data.length-8 ));
			
		}catch( Throwable e ){
			
			if ( fail_is_pw_error ){
				
				throw( new CryptoManagerPasswordException( e ));
				
			}else{
				throw( new CryptoManagerException( "PBE decryption failed", e ));
			}
		}
   	}
	
	protected char[]
	getPassword(
		int		handler,
		int		action,
		String	reason )
	
		throws CryptoManagerException
	{
		String persist_timeout_key 	= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_timeout";
		String persist_pw_key 		= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_value";

		long	current_timeout = COConfigurationManager.getLongParameter( persist_timeout_key, 0 );
		
		if ( current_timeout > SystemTime.getCurrentTime()){
			
			String	current_pw = COConfigurationManager.getStringParameter( persist_pw_key, "" );
			
			if ( current_pw.length() > 0 ){
				
				return( current_pw.toCharArray());
			}
		}
		
		System.out.println( "getPassword:" + handler + "/" + action + "/" + reason );

		if ( listeners.size() == 0 ){
			
			throw( new CryptoManagerException( "No password handlers registered" ));
		}
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				CryptoManagerPasswordHandler.passwordDetails details = ((CryptoManagerPasswordHandler)listeners.get(i)).getPassword( handler, action, reason );
				
				if ( details != null ){
					
					char[]	pw_chars = details.getPassword();
					
						// transform password so we can persist if needed 
					
					byte[]	salt		= getPasswordSalt();
					byte[]	pw_bytes	= new String( pw_chars ).getBytes( "UTF8" );
					
					SHA1 sha1 = new SHA1();
					
					sha1.update( ByteBuffer.wrap( salt ));
					sha1.update( ByteBuffer.wrap( pw_bytes ));
					
					String	encoded_pw = ByteFormatter.encodeString( sha1.digest());
					
					int	persist_secs = details.getPersistForSeconds();
					
					long	timeout;
					
					if ( persist_secs == 0 ){
						
						timeout	= 0;
						
					}else if ( persist_secs == Integer.MAX_VALUE ){
						
						timeout = Long.MAX_VALUE;
						
					}else{
						
						timeout = SystemTime.getCurrentTime() + persist_secs * 1000;
					}
					
					COConfigurationManager.setParameter( persist_timeout_key, timeout );

					if ( timeout > 0 ){
						
						COConfigurationManager.setParameter( persist_pw_key, encoded_pw );
						
					}else{
						
						COConfigurationManager.removeParameter( persist_pw_key );
					}
					
					return( encoded_pw.toCharArray());
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		throw( new CryptoManagerException( "No password handlers returned a password" ));
	}
	
	protected byte[]
	getPasswordSalt()
	{
		return( getSecureID());
	}
	
	public void
	addPasswordHandler(
		CryptoManagerPasswordHandler		handler )
	{
		listeners.add( handler );
	}
	
	public void
	removePasswordHandler(
		CryptoManagerPasswordHandler		handler )
	{
		listeners.remove( handler );
	}
	
	public static void
	main(
		String[]	args )
	{
		try{

			String	stuff = "12345";
			
			CryptoManagerImpl man = (CryptoManagerImpl)getSingleton();
			
			man.addPasswordHandler(
				new CryptoManagerPasswordHandler()
				{
					public passwordDetails 
					getPassword(
							int handler_type, 
							int action_type, 
							String reason )
					{
						return(
								new passwordDetails()
								{
									public char[]
									getPassword()
									{
										return( "trout".toCharArray());
									}
									
									public int 
									getPersistForSeconds() 
									{
										return( 10 );
									}
								});					
						}
				});
			
			CryptoHandler	handler1 = man.getECCHandler();
			
			CryptoHandler	handler2 = new CryptoHandlerECC( man, 2 );
			

			// handler1.resetKeys( null );
			// handler2.resetKeys( null );
			
			byte[]	sig = handler1.sign( stuff.getBytes(), "h1: sign" );
			
			System.out.println( handler1.verify( handler1.getPublicKey(  "h1: Test verify" ), stuff.getBytes(), sig ));
			
			handler1.lock();
			
			byte[]	enc = handler1.encrypt( handler2.getPublicKey( "h2: getPublic" ), stuff.getBytes(), "h1: encrypt" );
			
			System.out.println( "pk1 = " + ByteFormatter.encodeString( handler1.getPublicKey("h1: getPublic")));
			System.out.println( "pk2 = " + ByteFormatter.encodeString( handler2.getPublicKey("h2: getPublic")));
			
			System.out.println( "dec: " + new String( handler2.decrypt(handler1.getPublicKey( "h1: getPublic" ), enc, "h2: decrypt" )));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
