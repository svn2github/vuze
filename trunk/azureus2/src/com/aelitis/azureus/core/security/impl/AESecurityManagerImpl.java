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

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.security.SESecurityManager;

import com.aelitis.azureus.core.security.AECryptoHandler;
import com.aelitis.azureus.core.security.AESecurityManager;
import com.aelitis.azureus.core.security.AESecurityManagerException;
import com.aelitis.azureus.core.security.AESecurityManagerPasswordHandler;

public class 
AESecurityManagerImpl 
	implements AESecurityManager
{
	private static final int 	PBE_ITERATIONS	= 100;
	private static final String	PBE_ALG			= "PBEWithMD5AndDES";
	
	private static AESecurityManagerImpl		singleton;
	
	public static synchronized AESecurityManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new AESecurityManagerImpl();
		}
		
		return( singleton );
	}
	
	private byte[]				secure_id;
	private AECryptoHandler		ecc_handler;
	private List				listeners	= Collections.synchronizedList( new ArrayList());
	
	protected
	AESecurityManagerImpl()
	{
		SESecurityManager.initialise();
	}
	
	public byte[]
	getSecureID()
	{
		if ( secure_id == null ){
			
			secure_id = COConfigurationManager.getByteParameter( "core.crypto.id", null );
		}
		
		if ( secure_id == null ){
			
			secure_id = new byte[20];
		
			new SecureRandom().nextBytes( secure_id );
			
			COConfigurationManager.setParameter( "core.crypto.id", secure_id );
			
			COConfigurationManager.save();
		}
		
		return( secure_id );
	}
	
	public synchronized AECryptoHandler
	getECCHandler()
	{
		if ( ecc_handler == null ){
			
			ecc_handler = new AECryptoHandlerECC( this );
		}
		
		return( ecc_handler );
	}
	
	protected byte[]
	encryptWithPBE(
		byte[]		data )
	
		throws AESecurityManagerException
	{
		char[]	password = getPassword();
		
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
			
			throw( new AESecurityManagerException( "PBE encryption failed", e ));
		}
	}
	
	protected byte[]
   	decryptWithPBE(
   		byte[]		data )
	
		throws AESecurityManagerException
   	{
		char[]	password = getPassword();
		
		try{
			byte[]	salt = new byte[8];
			
			System.arraycopy( data, 0, salt, 0, 8 );
			
			PBEKeySpec keySpec = new PBEKeySpec(password);
	
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( PBE_ALG );
	
			SecretKey key = keyFactory.generateSecret(keySpec);
	
			PBEParameterSpec paramSpec = new PBEParameterSpec(salt, PBE_ITERATIONS);
	
			Cipher cipher = Cipher.getInstance( PBE_ALG );
			
			cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
	
			return( cipher.doFinal( data, 8, data.length-8 ));
			
		}catch( Throwable e ){
			
			throw( new AESecurityManagerException( "PBE encryption failed", e ));
		}
   	}
	
	protected char[]
	getPassword()
	{
		return( "arse".toCharArray());
	}
	
	public void
	addPasswordHandler(
		AESecurityManagerPasswordHandler		handler )
	{
		listeners.add( handler );
	}
	
	public void
	removePasswordHandler(
		AESecurityManagerPasswordHandler		handler )
	{
		listeners.remove( handler );
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			String	stuff = "12345";
			
			AESecurityManagerImpl man = (AESecurityManagerImpl)getSingleton();
			
			byte[]	enc = man.encryptWithPBE( stuff.getBytes());
			
			System.out.println( new String( man.decryptWithPBE( enc )));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
