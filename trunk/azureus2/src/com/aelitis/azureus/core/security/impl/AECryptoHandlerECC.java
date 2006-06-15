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

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.security.AECryptoHandler;
import com.aelitis.azureus.core.security.AESecurityManagerException;

public class 
AECryptoHandlerECC
	implements AECryptoHandler
{
	public static final ECNamedCurveParameterSpec ECCparam = ECNamedCurveTable.getParameterSpec("prime192v2");

	private AESecurityManagerImpl		manager;
	
	private PrivateKey			use_method_private_key;
	private PublicKey			use_method_public_key;
	
	protected
	AECryptoHandlerECC(
		AESecurityManagerImpl		_manager )
	{
		manager	= _manager;
	}
	
	public byte[]
	sign(
		byte[]		data )
	
		throws AESecurityManagerException
	{
		PrivateKey	priv = getPrivateKey();
		
		Signature sig = getSignature( priv );
		
		try{
			sig.update( data );
			
			return( sig.sign());
			
		}catch( Throwable e ){
			
			throw( new AESecurityManagerException( "Signature failed", e ));
		}
	}
	   
	protected PrivateKey
	getPrivateKey()
	
		throws AESecurityManagerException
	{
		if ( use_method_private_key == null ){
			
			byte[]	encoded = COConfigurationManager.getByteParameter( "core.crypto.ecc.privatekey", null );
			
			if ( encoded == null ){
				
				createAndStoreKeys();
				
			}else{
				
				use_method_private_key = rawdataToPrivkey( manager.decryptWithPBE( encoded ));
			}
		}
		
		if ( use_method_private_key == null ){
			
			throw( new AESecurityManagerException( "Failed to get private key" ));
		}
		
		return( use_method_private_key );
	}
	
	protected void
	createAndStoreKeys()
	
		throws AESecurityManagerException
	{
		KeyPair	keys = createKeys();
		
		use_method_public_key	= keys.getPublic();
		
		use_method_private_key	= keys.getPrivate();
		
		COConfigurationManager.setParameter( "core.crypto.ecc.publickey", keyToRawdata( use_method_public_key ));
		
		byte[]	priv_raw = keyToRawdata( use_method_private_key );
		
		byte[]	priv_enc = manager.encryptWithPBE( priv_raw );
		
		COConfigurationManager.setParameter( "core.crypto.ecc.privatekey", priv_enc );

	}
	
	protected KeyPair 
	createKeys()
	
		throws AESecurityManagerException
	{
		try
		{
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
			
			keyGen.initialize(ECCparam);

			return keyGen.genKeyPair();
			
		}catch(Throwable e){
			
			throw( new AESecurityManagerException( "Failed to create keys", e ));
		}
	}

	public Signature 
	getSignature(
		Key key )
	
		throws AESecurityManagerException
	{
		try
		{
			Signature ECCsig = Signature.getInstance("SHA1withECDSA", "BC");
			
			if( key instanceof ECPrivateKey ){
				
				ECCsig.initSign((ECPrivateKey)key);
				
			}else if( key instanceof ECPublicKey ){
				
				ECCsig.initVerify((ECPublicKey)key);

			}else{
				
				throw new AESecurityManagerException("Invalid Key Type, ECC keys required");
			}
			
			return ECCsig;
			
		}catch( AESecurityManagerException e ){
		
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new AESecurityManagerException( "Failed to create Signature", e ));
		}
	}

	protected byte[] 
	keyToRawdata( 
		PrivateKey privkey )
	
		throws AESecurityManagerException
	{
		if(!(privkey instanceof ECPrivateKey)){
			
			throw( new AESecurityManagerException( "Invalid private key" ));
		}
		
		return ((ECPrivateKey)privkey).getD().toByteArray();
	}

	protected PrivateKey 
	rawdataToPrivkey(
		byte[] input )
	
		throws AESecurityManagerException
	{
		BigInteger D = new BigInteger(input);
		
		KeySpec keyspec = new ECPrivateKeySpec(D,(ECParameterSpec)ECCparam);
		
		PrivateKey privkey = null;
		
		try{
			privkey = KeyFactory.getInstance("ECDSA","BC").generatePrivate(keyspec);
			
			return privkey;
			
		}catch( Throwable e ){
	
			throw( new AESecurityManagerException( "Failed to decode private key" ));
		}
	}
	
	protected byte[] 
	keyToRawdata(
		PublicKey pubkey )
	
		throws AESecurityManagerException
	{
		if(!(pubkey instanceof ECPublicKey)){
			
			throw( new AESecurityManagerException( "Invalid public key" ));
		}
		
		return ((ECPublicKey)pubkey).getQ().getEncoded();
	}
	
	
	protected PublicKey 
	rawdataToPubkey(
		byte[] input )
	
		throws AESecurityManagerException
	{
		ECPoint W = ECCparam.getCurve().decodePoint(input);
		
		KeySpec keyspec = new ECPublicKeySpec(W,(ECParameterSpec)ECCparam);

		try{
			
			return KeyFactory.getInstance("ECDSA", "BC").generatePublic(keyspec);
			
		}catch (Throwable e){
		
			throw( new AESecurityManagerException( "Failed to decode private key" ));
		}
	}	
}
