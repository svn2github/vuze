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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerException;
import com.aelitis.azureus.core.security.CryptoManagerKeyChangeListener;
import com.aelitis.azureus.core.security.CryptoManagerPasswordException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;
import com.aelitis.azureus.core.util.CopyOnWriteList;

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
	private CopyOnWriteList		password_handlers	= new CopyOnWriteList();
	private CopyOnWriteList		keychange_listeners	= new CopyOnWriteList();
	
	private Map	session_passwords =	Collections.synchronizedMap( new HashMap());
	
	protected
	CryptoManagerImpl()
	{
		SESecurityManager.initialise();
				
		long	now = SystemTime.getCurrentTime();
		
		for (int i=0;i<CryptoManager.HANDLERS.length;i++){
			
			int	handler = CryptoManager.HANDLERS[i];
			
			String persist_timeout_key 	= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_timeout";
			String persist_pw_key 		= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_value";

			long	timeout = COConfigurationManager.getLongParameter( persist_timeout_key, 0 );
							
			if ( now > timeout ){
			
				COConfigurationManager.setParameter( persist_timeout_key, 0 );
				COConfigurationManager.setParameter( persist_pw_key, "" );
				
			}else{
				
				addPasswordTimer( persist_timeout_key, persist_pw_key, timeout );
			}
		}
		
		ecc_handler = new CryptoHandlerECC( this, 1 );
	}
	
	protected void
	addPasswordTimer(
		final String		timeout_key,
		final String		pw_key,
		final long			timeout )
	{
		SimpleTimer.addEvent(
			"CryptoManager:pw_timeout",
			timeout,
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent event) 
				{
					synchronized( CryptoManagerImpl.this ){
						
						if ( COConfigurationManager.getLongParameter( timeout_key, 0 ) == timeout ){
							
							COConfigurationManager.removeParameter( timeout_key );
							COConfigurationManager.removeParameter( pw_key );
						}
					}
				}
			});
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
				
				throw( new CryptoManagerPasswordException( "Password incorrect", e ));
				
			}else{
				throw( new CryptoManagerException( "PBE decryption failed", e ));
			}
		}
   	}
	
	public void
	clearPasswords()
	{
		ecc_handler.lock();
		
		session_passwords.clear();
		
		for (int i=0;i<CryptoManager.HANDLERS.length;i++){
			
			clearPassword( CryptoManager.HANDLERS[i] );
		}
	}
	
	protected void
   	clearPassword(
   		int		handler )
   	{
   		final String persist_timeout_key 	= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_timeout";
   		final String persist_pw_key 		= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_value";
   		
		COConfigurationManager.removeParameter( persist_timeout_key );
		COConfigurationManager.removeParameter( persist_pw_key );
   	}
	
	protected passwordDetails
	getPassword(
		int				handler,
		int				action,
		String			reason,
		passwordTester	tester,
		int				pw_type )
	
		throws CryptoManagerException
	{
		final String persist_timeout_key 	= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_timeout";
		final String persist_pw_key 		= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_value";
		final String persist_pw_key_type	= CryptoManager.CRYPTO_CONFIG_PREFIX + "pw." + handler + ".persist_type";

		long	current_timeout = COConfigurationManager.getLongParameter( persist_timeout_key, 0 );
		
			// session timeout 
		
		if ( current_timeout < 0 ){
			
			passwordDetails	pw = (passwordDetails)session_passwords.get( persist_pw_key );
			
			if ( pw != null ){
				
				return( pw );
			}
		}
			
			// absolute timeout
		
		if ( current_timeout > SystemTime.getCurrentTime()){
			
			String	current_pw = COConfigurationManager.getStringParameter( persist_pw_key, "" );
			
			if ( current_pw.length() > 0 ){
				
				int	type = (int)COConfigurationManager.getLongParameter( persist_pw_key_type, CryptoManagerPasswordHandler.HANDLER_TYPE_USER );
				
				return( new passwordDetails( current_pw.toCharArray(), type ));
			}
		}
				
		Iterator	it = password_handlers.iterator();
		
		while( it.hasNext()){
			
			int	retry_count	= 0;
			
			char[]	last_pw_chars = null;
			
			CryptoManagerPasswordHandler provider = (CryptoManagerPasswordHandler)it.next();
			
			if ( 	pw_type != CryptoManagerPasswordHandler.HANDLER_TYPE_UNKNOWN &&
					pw_type != provider.getHandlerType()){
				
				continue;
			}
			
			while( retry_count < 64 ){
				
				try{
					CryptoManagerPasswordHandler.passwordDetails details = provider.getPassword( handler, action, retry_count > 0, reason );
					
					if ( details == null ){
						
							// try next password provider
						
						break;
					}
					
					char[]	pw_chars = details.getPassword();
					
					if ( last_pw_chars != null && Arrays.equals( last_pw_chars, pw_chars )){
						
							// no point in going through verification if same as last
						
						retry_count++;
						
						continue;
					}
					
					last_pw_chars = pw_chars;
					
						// transform password so we can persist if needed 
					
					byte[]	salt		= getPasswordSalt();
					byte[]	pw_bytes	= new String( pw_chars ).getBytes( "UTF8" );
					
					SHA1 sha1 = new SHA1();
					
					sha1.update( ByteBuffer.wrap( salt ));
					sha1.update( ByteBuffer.wrap( pw_bytes ));
					
					String	encoded_pw = ByteFormatter.encodeString( sha1.digest());
					
					if ( tester != null && !tester.testPassword( encoded_pw.toCharArray())){
					
							// retry
						
						retry_count++;
						
						continue;
					}
					
					int	persist_secs = details.getPersistForSeconds();
					
					long	timeout;
					
					if ( persist_secs == 0 ){
						
						timeout	= 0;
						
					}else if ( persist_secs == Integer.MAX_VALUE ){
						
						timeout = Long.MAX_VALUE;
						
					}else if ( persist_secs < 0 ){
						
							// session only
						
						timeout = -1;
						
					}else{
						
						timeout = SystemTime.getCurrentTime() + persist_secs * 1000L;
					}
					
					passwordDetails	result = new passwordDetails( encoded_pw.toCharArray(), provider.getHandlerType());
					
					synchronized( this ){
						
						COConfigurationManager.setParameter( persist_timeout_key, timeout );
						COConfigurationManager.setParameter( persist_pw_key_type, provider.getHandlerType());
						
						session_passwords.remove( persist_pw_key );
						
						COConfigurationManager.removeParameter( persist_pw_key );
													
						if ( timeout < 0 ){
								
							session_passwords.put( persist_pw_key, result );
								
						}else if ( timeout > 0 ){
							
							COConfigurationManager.setParameter( persist_pw_key, encoded_pw );
	
							addPasswordTimer( persist_timeout_key, persist_pw_key, timeout );
						}
					}
					
					return( result );

				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
					
						// next provider
					
					break;
				}
			}
		}
		
		throw( new CryptoManagerPasswordException( "No password handlers returned a password" ));
	}
	
	protected byte[]
	getPasswordSalt()
	{
		String key = CryptoManager.CRYPTO_CONFIG_PREFIX + "salt";
			
		byte[] salt = COConfigurationManager.getByteParameter( key, null );
		
		if ( salt == null ){
			
			salt = getSecureID();
			
			COConfigurationManager.setParameter( key, salt );
		}
		
		return( salt );
	}

	protected void
	setPasswordSalt(
		byte[]	salt )
	{
		String key = CryptoManager.CRYPTO_CONFIG_PREFIX + "salt";

		COConfigurationManager.setParameter( key, salt );
	}
	
	protected void
	keyChanged(
		CryptoHandler	handler )
	{
		Iterator it = keychange_listeners.iterator();
		
		while( it.hasNext()){
			
			try{		
				((CryptoManagerKeyChangeListener)it.next()).keyChanged( handler );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	addPasswordHandler(
		CryptoManagerPasswordHandler		handler )
	{
		password_handlers.add( handler );
	}
	
	public void
	removePasswordHandler(
		CryptoManagerPasswordHandler		handler )
	{
		password_handlers.remove( handler );
	}
	
	public void
	addKeyChangeListener(
		CryptoManagerKeyChangeListener		listener )
	{
		keychange_listeners.add( listener );
	}
	
	public void
	removeKeyChangeListener(
		CryptoManagerKeyChangeListener		listener )
	{
		keychange_listeners.remove( listener );
	}
	
	public interface
	passwordTester
	{
		public boolean
		testPassword(
			char[]		pw );
	}
	
	public class
	passwordDetails
	{
		private char[]		password;
		private int			type;
		
		protected 
		passwordDetails(
			char[]		_password,
			int			_type )
		{
			password	= _password;
			type		= _type;
		}
		
		public char[]
		getPassword()
		{
			return( password );
		}
		
		public int
		getHandlerType()
		{
			return( type );
		}
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
					public int
					getHandlerType()
					{
						return( HANDLER_TYPE_USER );
					}
					
					public passwordDetails 
					getPassword(
							int 		handler_type, 
							int 		action_type, 
							boolean		last_pw_incorrect,
							String 		reason )
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
