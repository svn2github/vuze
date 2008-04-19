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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.JCEIESCipher;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.jce.spec.IEKeySpec;
import org.bouncycastle.jce.spec.IESParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;
import com.aelitis.azureus.core.security.CryptoSTSEngine;

public class 
CryptoHandlerECC
	implements CryptoHandler
{
	private static final ECNamedCurveParameterSpec ECCparam = ECNamedCurveTable.getParameterSpec("prime192v2");
	
	private static final byte[]  ECIES_D = new byte[] {(byte)0x6d, (byte)0xc1, (byte)0x62, (byte)0x32, (byte)0x15, (byte)0x4d, (byte)0x0f, (byte)0x7b }; 
	private static final byte[]  ECIES_E = new byte[] {(byte)0x6a, (byte)0x64, (byte)0x98, (byte)0xde, (byte)0x1a, (byte)0xa4, (byte)0x98, (byte)0xcc }; 

	private static final int	TIMEOUT_DEFAULT_SECS		= 60*60;

	
	private CryptoManagerImpl		manager;
	
	private String				CONFIG_PREFIX = CryptoManager.CRYPTO_CONFIG_PREFIX + "ecc.";

	private PrivateKey			use_method_private_key;
	private PublicKey			use_method_public_key;
	
	private long	last_unlock_time;
	
	protected
	CryptoHandlerECC(
		CryptoManagerImpl		_manager,
		int						_instance_id )
	{
		manager	= _manager;
		
		CONFIG_PREFIX += _instance_id + ".";
	}
	
	public int
	getType()
	{
		return( CryptoManager.HANDLER_ECC );
	}
	
	public void
	unlock()
	
		throws CryptoManagerException
	{
		getMyPrivateKey( "unlock" );
	}
	
	public synchronized void
	lock()
	{
		use_method_private_key	= null;
	}
	
	public int
	getUnlockTimeoutSeconds()
	{
		return( COConfigurationManager.getIntParameter( CONFIG_PREFIX + "timeout", TIMEOUT_DEFAULT_SECS ));
	}
	
	public void
	setUnlockTimeoutSeconds(
		int		secs )
	{
		COConfigurationManager.setParameter( CONFIG_PREFIX + "timeout", secs );
	}
		
	public byte[]
	sign(
		byte[]		data,
		String		reason )
	
		throws CryptoManagerException
	{
		PrivateKey	priv = getMyPrivateKey( reason );
		
		Signature sig = getSignature( priv );
		
		try{
			sig.update( data );
			
			return( sig.sign());
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Signature failed", e ));
		}
	}
	 
	public boolean
	verify(
		byte[]		public_key,
		byte[]		data,
		byte[]		signature )
	
		throws CryptoManagerException
	{
		PublicKey	pub = rawdataToPubkey( public_key );
		
		Signature sig = getSignature( pub );
		
		try{
			sig.update( data );
			
			return( sig.verify( signature ));
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Signature failed", e ));
		}
	}
	
	public byte[]
	encrypt(
		byte[]		other_public_key,
		byte[]		data,
		String		reason )
		
		throws CryptoManagerException
	{	        
		try{
			IEKeySpec   key_spec = new IEKeySpec( getMyPrivateKey( reason ), rawdataToPubkey( other_public_key ));
	 
			IESParameterSpec param = new IESParameterSpec(ECIES_D, ECIES_E, 128);
		
			InternalECIES	cipher = new InternalECIES();
	
			cipher.internalEngineInit( Cipher.ENCRYPT_MODE, key_spec, param, null ); 
		
			return( cipher.internalEngineDoFinal(data, 0, data.length ));
			
		}catch( CryptoManagerException e ){
			
			throw( e );
			
		}catch( Throwable e){
			
			throw( new CryptoManagerException( "Encrypt failed", e ));
		}
	}
	
	public byte[]
	decrypt(
		byte[]		other_public_key,
		byte[]		data,
		String		reason )
		
		throws CryptoManagerException
	{	        
		try{
			IEKeySpec   key_spec = new IEKeySpec( getMyPrivateKey(  reason ), rawdataToPubkey( other_public_key ));
	 	
			IESParameterSpec param = new IESParameterSpec(ECIES_D, ECIES_E, 128);
		
			InternalECIES	cipher = new InternalECIES();
	
			cipher.internalEngineInit( Cipher.DECRYPT_MODE, key_spec, param, null ); 
		
			return( cipher.internalEngineDoFinal(data, 0, data.length ));
			
		}catch( CryptoManagerException e ){
			
			throw( e );
			
		}catch( Throwable e){
			
			throw( new CryptoManagerException( "Decrypt failed", e ));
		}
	}
		
	public CryptoSTSEngine
	getSTSEngine(
		String		reason )
	
		throws CryptoManagerException
	{
		return( new CryptoSTSEngineImpl( this, getMyPublicKey(  reason, true ), getMyPrivateKey( reason )));
	}
	
	public byte[]
	peekPublicKey()
	{
		try{
		
			return( keyToRawdata( getMyPublicKey( "peek", false )));
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	public byte[]
	getPublicKey(
		String		reason )
	
		throws CryptoManagerException
	{
		return( keyToRawdata( getMyPublicKey( reason, true )));
	}
  
	public byte[]
	getEncryptedPrivateKey(
		String		reason )
	
		throws CryptoManagerException
	{
		getMyPrivateKey( reason );
		
		byte[]	pk = COConfigurationManager.getByteParameter( CONFIG_PREFIX + "privatekey", null );

		if ( pk == null ){
			
			throw( new CryptoManagerException( "Private key unavailable" ));
		}
		
		int	pw_type = getCurrentPasswordType();
		
		byte[] res = new byte[pk.length+1];
		
		res[0] = (byte)pw_type;
		
		System.arraycopy( pk, 0, res, 1, pk.length );
		
		return( res );
	}
	
	public synchronized void
	recoverKeys(
		byte[]		public_key,
		byte[]		encrypted_private_key_and_type )
	
		throws CryptoManagerException
	{
		use_method_private_key	= null;
		use_method_public_key	= null;
		
		COConfigurationManager.setParameter( CONFIG_PREFIX + "publickey", public_key );
			
		int	type = (int)encrypted_private_key_and_type[0]&0xff;
		
		COConfigurationManager.setParameter( CONFIG_PREFIX + "pwtype", type );

		byte[] encrypted_private_key = new byte[encrypted_private_key_and_type.length-1];
		
		System.arraycopy( encrypted_private_key_and_type, 1, encrypted_private_key, 0, encrypted_private_key.length );
		
		COConfigurationManager.setParameter( CONFIG_PREFIX + "privatekey", encrypted_private_key );
		
		COConfigurationManager.save();
	}
	
	public synchronized void
	resetKeys(
		String		reason )
	
		throws CryptoManagerException
	{
		use_method_private_key	= null;
		use_method_public_key	= null;
		
		COConfigurationManager.removeParameter( CONFIG_PREFIX + "publickey" );
			
		COConfigurationManager.removeParameter( CONFIG_PREFIX + "privatekey" );
		
		COConfigurationManager.save();
		
		createAndStoreKeys( "resetting keys" );
	}
	
	protected synchronized PrivateKey
	getMyPrivateKey(
		String		reason )
	
		throws CryptoManagerException
	{
		if ( use_method_private_key != null ){
			
			int	timeout_secs = getUnlockTimeoutSeconds();
			
			if ( timeout_secs > 0 ){
				
				if ( SystemTime.getCurrentTime() - last_unlock_time >= timeout_secs * 1000 ){
					
					use_method_private_key = null;
				}
			}
		}
		
		if ( use_method_private_key == null ){
			
			final byte[]	encoded = COConfigurationManager.getByteParameter( CONFIG_PREFIX + "privatekey", null );
			
			if ( encoded == null ){
				
				createAndStoreKeys( reason );
				
			}else{
				
				CryptoManagerImpl.passwordDetails password_details = 
					manager.getPassword( 
							CryptoManager.HANDLER_ECC, 
							CryptoManagerPasswordHandler.ACTION_DECRYPT, 
							reason,
							new CryptoManagerImpl.passwordTester()
							{
								public boolean 
								testPassword(
									char[] password )
								{
									try{
										manager.decryptWithPBE( encoded, password );
										
										return( true );
										
									}catch( Throwable e ){
										
										return( false );
									}
								}
							},
							getCurrentPasswordType());
	
				boolean		ok = false;
				
				try{
					use_method_private_key = rawdataToPrivkey( manager.decryptWithPBE( encoded, password_details.getPassword()));
				
					last_unlock_time = SystemTime.getCurrentTime();
				
					byte[]	test_data = "test".getBytes();
					
					ok = verify( keyToRawdata( getMyPublicKey( reason, true )), test_data,  sign( test_data, reason ));
					
					if ( !ok ){
											
						throw( new CryptoManagerPasswordException( "Password incorrect" ));
					}
					
				}catch( CryptoManagerException e ){
					
					throw( e );
					
				}catch( Throwable e ){
					
					throw( new CryptoManagerException( "Password incorrect", e ));
					
				}finally{
					
					if ( !ok ){
													
						manager.clearPassword( CryptoManager.HANDLER_ECC );
						
						use_method_private_key	= null;
					}
				}
			}
		}
		
		if ( use_method_private_key == null ){
			
			throw( new CryptoManagerException( "Failed to get private key" ));
		}
		
		return( use_method_private_key );
	}
	
	protected synchronized PublicKey
	getMyPublicKey(
		String		reason,
		boolean		create_if_needed )
	
		throws CryptoManagerException
	{
		if ( use_method_public_key == null ){
			
			byte[]	key_bytes = COConfigurationManager.getByteParameter( CONFIG_PREFIX + "publickey", null );
			
			if ( key_bytes == null ){
				
				if ( create_if_needed ){
					
					createAndStoreKeys( reason );
					
				}else{
					
					return( null );
				}
			}else{
				
				use_method_public_key = rawdataToPubkey( key_bytes );
			}
		}
		
		if ( use_method_public_key == null ){
			
			throw( new CryptoManagerException( "Failed to get public key" ));
		}
		
		return( use_method_public_key );
	}
	
	protected void
	createAndStoreKeys(
		String		reason )
	
		throws CryptoManagerException
	{
			// when storing keys we allow the registered handlers decide which password type handler
			// is wearing the trousers
		
		CryptoManagerImpl.passwordDetails password_details = 
			manager.getPassword( 
							CryptoManager.HANDLER_ECC,
							CryptoManagerPasswordHandler.ACTION_ENCRYPT,
							reason,
							null,
							CryptoManagerPasswordHandler.HANDLER_TYPE_UNKNOWN );
		
		KeyPair	keys = createKeys();
		
		use_method_public_key	= keys.getPublic();
		
		use_method_private_key	= keys.getPrivate();
		
		last_unlock_time = SystemTime.getCurrentTime();
		
		storeKeys( password_details );
	}
	
	protected void
	storeKeys(
		CryptoManagerImpl.passwordDetails	password )
	
		throws CryptoManagerException
	{
		COConfigurationManager.setParameter( CONFIG_PREFIX + "publickey", keyToRawdata( use_method_public_key ));
		
		byte[]	priv_raw = keyToRawdata( use_method_private_key );
		
		byte[]	priv_enc = manager.encryptWithPBE( priv_raw, password.getPassword());
		
		COConfigurationManager.setParameter( CONFIG_PREFIX + "privatekey", priv_enc );

		COConfigurationManager.setParameter( CONFIG_PREFIX + "pwtype", password.getHandlerType());

		COConfigurationManager.save();
		
		manager.keyChanged( this );
	}
	
	protected KeyPair 
	createKeys()
	
		throws CryptoManagerException
	{
		try
		{
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
			
			keyGen.initialize(ECCparam);

			return keyGen.genKeyPair();
			
		}catch(Throwable e){
			
			throw( new CryptoManagerException( "Failed to create keys", e ));
		}
	}

	public Signature 
	getSignature(
		Key key )
	
		throws CryptoManagerException
	{
		try
		{
			Signature ECCsig = Signature.getInstance("SHA1withECDSA", "BC");
			
			if( key instanceof ECPrivateKey ){
				
				ECCsig.initSign((ECPrivateKey)key);
				
			}else if( key instanceof ECPublicKey ){
				
				ECCsig.initVerify((ECPublicKey)key);

			}else{
				
				throw new CryptoManagerException("Invalid Key Type, ECC keys required");
			}
			
			return ECCsig;
			
		}catch( CryptoManagerException e ){
		
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Failed to create Signature", e ));
		}
	}

	protected byte[] 
	keyToRawdata( 
		PrivateKey privkey )
	
		throws CryptoManagerException
	{
		if(!(privkey instanceof ECPrivateKey)){
			
			throw( new CryptoManagerException( "Invalid private key" ));
		}
		
		return ((ECPrivateKey)privkey).getD().toByteArray();
	}

	protected PrivateKey 
	rawdataToPrivkey(
		byte[] input )
	
		throws CryptoManagerException
	{
		BigInteger D = new BigInteger(input);
		
		KeySpec keyspec = new ECPrivateKeySpec(D,(ECParameterSpec)ECCparam);
		
		PrivateKey privkey = null;
		
		try{
			privkey = KeyFactory.getInstance("ECDSA","BC").generatePrivate(keyspec);
			
			return privkey;
			
		}catch( Throwable e ){
	
			throw( new CryptoManagerException( "Failed to decode private key" ));
		}
	}
	
	protected byte[] 
	keyToRawdata(
		PublicKey pubkey )
	
		throws CryptoManagerException
	{
		if(!(pubkey instanceof ECPublicKey)){
			
			throw( new CryptoManagerException( "Invalid public key" ));
		}
		
		return ((ECPublicKey)pubkey).getQ().getEncoded();
	}
	
	
	protected PublicKey 
	rawdataToPubkey(
		byte[] input )
	
		throws CryptoManagerException
	{
		ECPoint W = ECCparam.getCurve().decodePoint(input);
		
		KeySpec keyspec = new ECPublicKeySpec(W,(ECParameterSpec)ECCparam);

		try{
			
			return KeyFactory.getInstance("ECDSA", "BC").generatePublic(keyspec);
			
		}catch (Throwable e){
		
			throw( new CryptoManagerException( "Failed to decode public key" ));
		}
	}	
	
	public boolean
	verifyPublicKey(
		byte[]	encoded )
	{
		try{
			ECPublicKey pk = (ECPublicKey)rawdataToPubkey( encoded );
			
				// we can't actually verify the key size as although it should be 192 bits
				// it can be less due to leading bits being 0
			
			return( true );
			
		}catch( Throwable e ){
			
			return( false );
		}
	}
	
	public String
	exportKeys()
	
		throws CryptoManagerException
	{
		return( "salt:    " + Base32.encode(manager.getPasswordSalt()) + "\r\n" + 
				"public:  " + Base32.encode(getPublicKey( "Key export" )) + "\r\n" +
				"private: " + Base32.encode(getEncryptedPrivateKey( "Key export" )));
	}
	
	public void
	importKeys(
		String	str )
	
		throws CryptoManagerException
	{
		byte[]	existing_salt 			= manager.getPasswordSalt();
		byte[]	existing_public_key		= peekPublicKey();
		byte[]	existing_private_key	= existing_public_key==null?null:getEncryptedPrivateKey( "Key import" );
		
		byte[]		recovered_salt 			= null;
		byte[]		recovered_public_key 	= null;
		byte[]		recovered_private_key 	= null;
				
		String[]	bits = str.split( "\n" );
		
		for (int i=0;i<bits.length;i++){
			
			String	bit = bits[i].trim();
			
			if ( bit.length() == 0 ){
				
				continue;
			}
			
			String[] x = bit.split(":");
			
			if ( x.length != 2 ){
				
				continue;
			}
			
			String	lhs = x[0].trim();
			String	rhs = x[1].trim();
			
			byte[]	rhs_val = Base32.decode( rhs );
			
			if ( lhs.equals( "salt" )){
				
				recovered_salt = rhs_val;
				
			}else if ( lhs.equals( "public" )){
				
				recovered_public_key = rhs_val;
				
			}else if ( lhs.equals( "private" )){
				
				recovered_private_key = rhs_val;
			}
		}
		
		if ( recovered_salt == null || recovered_public_key == null || recovered_private_key == null ){
			
			throw( new CryptoManagerException( "Invalid input file" ));
		}
		
		boolean	ok = false;
		
		try{
			manager.setPasswordSalt( recovered_salt );
			
			recoverKeys( recovered_public_key, recovered_private_key );
		
			ok = true;
						
		}finally{
			
			if ( !ok ){
								
				manager.setPasswordSalt( existing_salt );
				
				if ( existing_public_key != null ){
					
					recoverKeys( existing_public_key, existing_private_key );
				}
			}
		}
	}
	
	protected int
	getCurrentPasswordType()
	{
		return((int)COConfigurationManager.getIntParameter( CONFIG_PREFIX + "pwtype", CryptoManagerPasswordHandler.HANDLER_TYPE_USER ));
	}
	
	class InternalECIES 
		extends JCEIESCipher.ECIES
	{
			// we use this class to obtain compatability with BC

		public void 
		internalEngineInit(
			int                     opmode,
			Key                     key,
			AlgorithmParameterSpec  params,
			SecureRandom            random ) 
		
			throws InvalidKeyException, InvalidAlgorithmParameterException
		{
			engineInit(opmode, key, params, random);
		}

		protected byte[] 
		internalEngineDoFinal(
			byte[]  input,
			int     inputOffset,
			int     inputLen ) 
		
			throws IllegalBlockSizeException, BadPaddingException
		{
			return engineDoFinal(input, inputOffset, inputLen);
		}
	}
}
