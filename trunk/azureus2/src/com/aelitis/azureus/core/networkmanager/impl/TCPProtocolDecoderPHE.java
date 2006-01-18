/*
 * Created on 17-Jan-2006
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector.VirtualSelectorListener;

public class 
TCPProtocolDecoderPHE 
	extends TCPProtocolDecoder 
	implements VirtualSelectorListener
{
	private static final byte		SUPPORTED_PLAIN	= 0x00;
	private static final byte		SUPPORTED_XOR	= 0x01;
	private static final byte		SUPPORTED_AES	= 0x02;

	private static final int		DH_SIZE	= 512;
	private static final int		DH_SIZE_BYTES = DH_SIZE/8;
	
	private static final String 	DH_P = "92d862b3a95bff4e6cbdce3a266ff4b46e6e1ecad76c0a877d92a3dae4999e6414efde56fc14d1cca6d5408a8ef9ea248389168876b6e8f4503845dfe373549f";
	private static final String 	DH_G = "4383b53ee650fd73e41e8c9e8527997ab8cb41e1cbd73ac7685493e1e5d091e3e3789dea03ab9d5b2c368faa617bb30e427cbaeb23c268edb38eb8c747756080";
	
	private static final BigInteger	DH_P_BI = new BigInteger( DH_P, 16 );
	private static final BigInteger	DH_G_BI = new BigInteger( DH_G, 16 );
	
	private static KeyPairGenerator		dh_key_generator;
	private static long					last_dh_key_generate;
	
	private static boolean	crypto_ok;
	
	private static final String		STREAM_ALG				= "AES";
	private static final String		STREAM_CIPHER			= "AES/CFB8/NoPadding";
	private static final int		STREAM_KEY_SIZE			= 128;
	private static final int		STREAM_KEY_SIZE_BYTES	= STREAM_KEY_SIZE/8;
    private static final byte[]		STREAM_IV				= 
    	{ 	(byte)0x15, (byte)0xE0, (byte)0x6B, (byte)0x7E, (byte)0x98, (byte)0x59, (byte)0xE4, (byte)0xA7, 
    		(byte)0x34, (byte)0x66, (byte)0xAD, (byte)0x48, (byte)0x35, (byte)0xE2, (byte)0xD0, (byte)0x24 };
    	
    
    private static final int		PADDING_MAX	= 512;
    
	private static final Random	random = new SecureRandom();
	
	static{
		try{
			DHParameterSpec dh_param_spec = new DHParameterSpec( DH_P_BI, DH_G_BI );
			
			dh_key_generator = KeyPairGenerator.getInstance("DH");
	        
			dh_key_generator.initialize(dh_param_spec);
	        
			dh_key_generator.generateKeyPair();
	        
	        byte[]	test_secret = new byte[DH_SIZE_BYTES];
	        	        
	        SecretKeySpec	test_secret_key_spec = new SecretKeySpec(test_secret, 0, STREAM_KEY_SIZE_BYTES, STREAM_ALG );
	        	        
	        AlgorithmParameterSpec	spec = 	new IvParameterSpec( STREAM_IV );
	        
	        Cipher cipher = Cipher.getInstance( STREAM_CIPHER );
	        
	        cipher.init( Cipher.ENCRYPT_MODE, test_secret_key_spec, spec );
	        
	        cipher = Cipher.getInstance( STREAM_CIPHER );
	        
	        cipher.init( Cipher.DECRYPT_MODE, test_secret_key_spec, spec );
	        
	        crypto_ok	= true;
	        
	        	// TODO: logging
	        
	        System.out.println( "PHE Crypto OK" );
	        
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			crypto_ok	= false;
		}
	}
	
	public static boolean
	isCryptoOK()
	{
		return( crypto_ok );
	}
	
	private static VirtualChannelSelector	read_selector	= NetworkManager.getSingleton().getReadSelector();
	private static VirtualChannelSelector	write_selector	= NetworkManager.getSingleton().getWriteSelector();

	private SocketChannel		channel;
	private ByteBuffer			write_buffer;
	private ByteBuffer			read_buffer;
	

	private TCPProtocolDecoderAdapter	adapter;

	private KeyAgreement 	key_agreement;
	private byte[]			dh_public_key_bytes;
	
	private byte[]			secret_bytes;
	private byte[]			sha1_secret_bytes;
	
	private Cipher			write_cipher;
	private Cipher			read_cipher;
	
	private byte			my_supported_protocols;
	private byte			selected_protocol;
	
	private static final int		PS_OUTBOUND_1	= 0;
	private static final int		PS_OUTBOUND_2	= 1;
	private static final int		PS_OUTBOUND_3	= 2;
	
	private static final int		PS_INBOUND_1	= 10;
	private static final int		PS_INBOUND_2	= 11;
	private static final int		PS_INBOUND_3	= 12;
	
	private int		protocol_state;
	private int		protocol_substate;
	
	private boolean	handshake_complete;

	private int		bytes_read;
	private int		bytes_written;
	
	private TCPTransportHelperFilter		filter;
	
	private boolean processing_complete;
	
	public 
	TCPProtocolDecoderPHE(
		SocketChannel				_channel,
		ByteBuffer					_header,
		TCPProtocolDecoderAdapter	_adapter )
	
		throws IOException
	{
		super( false );
		
		if ( !isCryptoOK()){
			
			throw( new IOException( "PHE crypto broken" ));
		}
		
		channel	= _channel;
		adapter	= _adapter;
		
		my_supported_protocols = SUPPORTED_AES | SUPPORTED_XOR;
		
		initCrypto();

		read_selector.register( channel, this, null );
		write_selector.register( channel, this, null );
		
		write_selector.pauseSelects( channel );
		
		if ( _header == null ){
		
			protocol_state	= PS_OUTBOUND_1;

			read_selector.pauseSelects( channel );
			
		}else{
			
			protocol_state	= PS_INBOUND_1;

			read_buffer = ByteBuffer.allocate( dh_public_key_bytes.length );					
				
			read_buffer.put( _header );
		
			bytes_read += _header.limit();
		}
		
		process();
	}
	
	protected void
	initCrypto()
	
		throws IOException
	{
		try{
	        KeyPair key_pair = generateDHKeyPair();
	    	    
	        key_agreement = KeyAgreement.getInstance("DH");
	        
	        key_agreement.init(key_pair.getPrivate());
	       
	        DHPublicKey	dh_public_key = (DHPublicKey)key_pair.getPublic();
	        
	        BigInteger	dh_y = dh_public_key.getY();
	        
	        dh_public_key_bytes = bigIntegerToBytes( dh_y, DH_SIZE_BYTES );
	        
		}catch( Throwable e ){
			
			throw( new IOException( Debug.getNestedExceptionMessage(e)));
		}
	}
	
	protected void
	completeDH(
		byte[]	buffer )
	
		throws IOException
	{
		try{			
	        BigInteger	other_dh_y = bytesToBigInteger( buffer, 0, DH_SIZE_BYTES );
	        
	        KeyFactory dh_key_factory = KeyFactory.getInstance("DH");
	        	    
		    PublicKey other_public_key = dh_key_factory.generatePublic( new DHPublicKeySpec( other_dh_y, DH_P_BI, DH_G_BI ));
	        		
		    key_agreement.doPhase( other_public_key, true );
		    
		    secret_bytes = key_agreement.generateSecret();
	
		    sha1_secret_bytes	= new SHA1Simple().calculateHash( secret_bytes );
		    
	        SecretKeySpec	secret_key_spec = new SecretKeySpec( secret_bytes, 0, STREAM_KEY_SIZE_BYTES, STREAM_ALG );
	        		        
	        AlgorithmParameterSpec	spec = 	new IvParameterSpec( STREAM_IV );
	        
	        write_cipher = Cipher.getInstance( STREAM_CIPHER );
	        
	        write_cipher.init( Cipher.ENCRYPT_MODE, secret_key_spec, spec );
		    
	        read_cipher = Cipher.getInstance( STREAM_CIPHER );
	        
	        read_cipher.init( Cipher.DECRYPT_MODE, secret_key_spec, spec );
	        
		}catch( Throwable e ){
			
			throw( new IOException( Debug.getNestedExceptionMessage(e)));
		}
	}
	
	protected void
	handshakeComplete()
	{
		read_buffer		= null;
		write_buffer	= null;
		
		key_agreement		= null;
		dh_public_key_bytes	= null;
		secret_bytes		= null;
		sha1_secret_bytes	= null;
		
		TCPTransportHelper	helper = new TCPTransportHelper( channel );
		
		if ( selected_protocol == SUPPORTED_PLAIN ){
			
			filter = new TCPTransportHelperFilterTransparent( helper );
			
		}else if ( selected_protocol == SUPPORTED_XOR ){
		
			filter = new TCPTransportHelperFilterXOR( helper, secret_bytes );
						
		}else{
			
			filter = new TCPTransportHelperFilterCipherStream( 
							helper,
							read_cipher,
							write_cipher );

		}
		
		read_cipher		= null;
		write_cipher	= null;
		
		handshake_complete	= true;
	}
	
	
	/*
	  
	 	**** OUTBOUND_1
	 	
	A sends B Ya + Pa
	 
	 	**** INBOUND_1
	 	
	B receives Ya
	B computes Yb
	B computes S and HS
	
		**** OUTBOUND_2
		
	B sends A Yb + HS( "supported methods" + len(Pb)) + Pb
	 
	 	**** INBOUND_2
	 	
	A receives Yb
	A computes S and HS
	A receives HS( "supported methods" + len(Pb)) and decrypts using HS
	A skips len(Pb) random bytes
	
		**** OUTBOUND_3
		
	A sends SHA1(S) + HS( "selected method" + len(Pc)) + Pc + selectedCrypt( payload )
	 
	 	**** INBOUND_3
	 	
	B skips Pa bytes until receives SHA1(S)
	B decrypts "selected method" + len(Pc) and skips len(Pc) bytes to get to selectedCrypt( payload... )
	B sends A selectedCrypt( payload... )
	*/
	
	protected void
	process()
	
		throws IOException
	{
		try{
			boolean	loop = true;
		
			while( loop ){
					
				if ( protocol_state == PS_OUTBOUND_1 ){
					
					if ( write_buffer == null ){
						
							// A sends B Ya + Pa
						
						byte[]	padding = getPadding();
												
						write_buffer = ByteBuffer.allocate( dh_public_key_bytes.length + padding.length );
						
						write_buffer.put( dh_public_key_bytes );
						
						write_buffer.put( padding );
						
						write_buffer.flip();
					}
					
					write( write_buffer );
						
					if ( !write_buffer.hasRemaining()){
					
						write_buffer	= null;
					
						protocol_state	= PS_INBOUND_2;
					}
					
				}else if ( protocol_state == PS_OUTBOUND_2 ){
					
						// B sends A Yb + HS( "supported methods" + len(Pb)) + Pb
		
					if ( write_buffer == null ){
						
						byte[]	padding = getPadding();
						
						write_buffer = ByteBuffer.allocate( dh_public_key_bytes.length + 4 + 2 + padding.length );
						
						write_buffer.put( dh_public_key_bytes );
														
							// 4 bytes for my supported protocols
						
						write_buffer.put( write_cipher.update( new byte[3] ));
						
						write_buffer.put( write_cipher.update( new byte[]{ my_supported_protocols }));
						
						write_buffer.put( write_cipher.update( new byte[]{ (byte)(padding.length>>8),(byte)padding.length }));
					
						write_buffer.put( padding );
						
						write_buffer.flip();
					}
					
					write( write_buffer );
					
					if ( !write_buffer.hasRemaining()){
					
						write_buffer	= null;
					
						protocol_state	= PS_INBOUND_3;
					}
					
				}else if ( protocol_state == PS_OUTBOUND_3 ){
					
						// 	A sends SHA1(S) + HS( "selected method" + len(Pc)) + Pc + selectedCrypt( payload )
		
					if ( write_buffer == null ){
						
						byte[]	padding = getPadding();
						
						write_buffer = ByteBuffer.allocate( 20 + 1 + 2 + padding.length );
						
						write_buffer.put( sha1_secret_bytes );
														
						write_buffer.put( write_cipher.update( new byte[]{ selected_protocol }));
						
						write_buffer.put( write_cipher.update( new byte[]{ (byte)(padding.length>>8),(byte)padding.length }));
					
						write_buffer.put( padding );
						
						write_buffer.flip();
					}
					
					write( write_buffer );
					
					if ( !write_buffer.hasRemaining()){
					
						write_buffer	= null;
					
						handshakeComplete();
					}
					
				}else if ( protocol_state == PS_INBOUND_1 ){
							
						// B receives Ya
		
					read( read_buffer );
							
					if ( !read_buffer.hasRemaining()){
											
						read_buffer.flip();
						
						byte[] other_dh_public_key_bytes = new byte[read_buffer.limit()];
						
						read_buffer.get( other_dh_public_key_bytes );
			
						completeDH( other_dh_public_key_bytes );
									
				        read_buffer	= null;
				        		        
						protocol_state	= PS_OUTBOUND_2;
					}
						
				}else if ( protocol_state == PS_INBOUND_2 ){
						
					/*
					A receives Yb
					A computes S and HS
					A receives HS( "supported methods" + len(Pb)) and decrypts using HS
					A skips len(Pb) random bytes
					*/
						
					if ( read_buffer == null ){
												
						read_buffer = ByteBuffer.allocate( dh_public_key_bytes.length + 6 );
						
						protocol_substate	= 1;
					}					
						
					read( read_buffer );
							
					if ( !read_buffer.hasRemaining()){
											
						if ( protocol_substate == 1 ){
							
							read_buffer.flip();
							
							byte[] other_dh_public_key_bytes_etc = read_buffer.array();
			
							completeDH( other_dh_public_key_bytes_etc );
			
							byte[]	etc = read_cipher.update( other_dh_public_key_bytes_etc, DH_SIZE_BYTES, 6 );
							
							byte	other_supported_protocols = etc[3];
							
							int	common_protocols = my_supported_protocols & other_supported_protocols;
							
							if (( common_protocols & SUPPORTED_AES )!= 0 ){
								
								selected_protocol = SUPPORTED_AES;
								
							}else if (( common_protocols & SUPPORTED_XOR )!= 0 ){
								
								selected_protocol = SUPPORTED_XOR;
								
							}else{
								
								selected_protocol = SUPPORTED_PLAIN;
							}
								
							int	padding	= (( etc[4] & 0xff ) << 8 ) + ( etc[5] & 0xff );
							
							read_buffer = ByteBuffer.allocate( padding );
							
							protocol_substate	= 2;
							
						}else{
							
							read_buffer	= null;
				        						
							protocol_state	= PS_OUTBOUND_3;
						}
					}
				}else if ( protocol_state == PS_INBOUND_3 ){
					
					/*
						B skips Pa bytes until receives SHA1(S)
						B decrypts "selected method" + len(Pc) and skips len(Pc) bytes 
					*/
						
					if ( read_buffer == null ){
												
						read_buffer = ByteBuffer.allocate( 20 + PADDING_MAX );
						
						read_buffer.limit( 20 );
						
						protocol_substate	= 1;
					}					
					
					while( true ){
						
						read( read_buffer );
								
						if ( read_buffer.hasRemaining()){
						
							break;
						}
						
						if ( protocol_substate == 1 ){
							
							int	limit = read_buffer.limit();
							
							read_buffer.position( limit - 20 );
							
							boolean match	= true;
							
							for (int i=0;i<20;i++){
								
								if ( read_buffer.get() != sha1_secret_bytes[i] ){
									
									match	= false;
									
									break;
								}
							}
							
							if ( match ){
							
								read_buffer = ByteBuffer.allocate( 3 );
								
								protocol_substate	= 2;
								
								break;
							
							}else{
								
								if ( limit == read_buffer.capacity()){
									
									throw( new IOException( "PHE skip to SHA1 marker failed" ));
								}
								
								read_buffer.limit( limit + 1 );
								
								read_buffer.position( limit );
							}
						}else if ( protocol_substate == 2 ){
							
							read_buffer.flip();
								
							byte[]	etc = read_cipher.update( read_buffer.array());
							
							selected_protocol = etc[0];
							
							int	padding	= (( etc[1] & 0xff ) << 8 ) + ( etc[2] & 0xff );
							
							read_buffer = ByteBuffer.allocate( padding );
							
							protocol_substate	= 3;
							
							break;
							
						}else{
							
							read_buffer	= null;
				        
							handshakeComplete();
							
							break;
						}
					}
				}
		
				if ( handshake_complete ){
					
					read_selector.cancel( channel );
					
					write_selector.cancel( channel );
					
					loop	= false;
					
					complete();
					
				}else{
				
					if ( read_buffer == null ){
						
						read_selector.pauseSelects( channel );
						
					}else{
						
						read_selector.resumeSelects ( channel );
						
						loop	= false;
						
					}
					
					if ( write_buffer == null ){
						
						write_selector.pauseSelects( channel );
						
					}else{
						
						write_selector.resumeSelects ( channel );
						
						loop	= false;
					}
				}
			}
		}catch( Throwable e ){
			
			failed( e );
			
			if ( e instanceof IOException ){
				
				throw((IOException)e);
				
			}else{
				
				throw( new IOException( Debug.getNestedExceptionMessage(e)));
			}
		}
	}
	
	protected void
	read(
		ByteBuffer		buffer )
	
		throws IOException
	{
		int	len = channel.read( buffer );
	
		// System.out.println( "read:" + this + "/" + protocol_state + "/" + protocol_substate + " -> " + len +"[" + buffer +"]");
		
		if ( len < 0 ){
			
			throw( new IOException( "bytes read < 0 " ));
		}
		
		bytes_read += len;
	}
	
	protected void
	write(
		ByteBuffer		buffer )
	
		throws IOException
	{
		int	len = channel.write( buffer );
		
		// System.out.println( "write:" + this + "/" + protocol_state + "/" + protocol_substate + " -> " + len +"[" + buffer +"]");

		if ( len < 0 ){
			
			throw( new IOException( "bytes written < 0 " ));			
		}
		
		bytes_written += len;
	}
	
	public boolean 
	selectSuccess(
		VirtualChannelSelector 	selector, 
		SocketChannel 			sc, 
		Object 					attachment)
	{
		try{
			int	old_bytes_read	= bytes_read;
			
			process();
			
			if ( selector == write_selector ){
				
				return( true );
				
			}else{
				
				return( bytes_read != old_bytes_read );
			}
			
		}catch( Throwable  e ){
			
			failed( e );
			
			return( false );
		}
	}

	public void 
	selectFailure(
		VirtualChannelSelector 	selector, 
		SocketChannel 			sc, 
		Object 					attachment, 
		Throwable				msg )
	{
		failed( msg );
	}
	
	protected byte[]
	bigIntegerToBytes(
		BigInteger	bi,
		int			num_bytes )
	{
		String	str = bi.toString(16);
		
		while( str.length() < num_bytes*2 ){
			str = "0" + str;
		}
		
		return( ByteFormatter.decodeString(str));
	}
	
	protected BigInteger
	bytesToBigInteger(
		byte[]	bytes,
		int		offset,
		int		len )
	{		
		return( new BigInteger( ByteFormatter.encodeString( bytes, offset, len  ), 16 ));
	}
	
	protected static byte[]
	getPadding()
	{
		byte[]	bytes = new byte[ random.nextInt(PADDING_MAX)];
		
		random.nextBytes(bytes);
		
		return( bytes );
	}
	
	protected static KeyPair
	generateDHKeyPair()
	{
		synchronized( dh_key_generator ){
			
			long	now = SystemTime.getCurrentTime();
			
			long	since_last = now - last_dh_key_generate;
			
			long	delay = 100 - since_last;
			
				// limit key gen operations to 10 a second
			
			if ( delay > 0 && delay < 100 ){
				
				try{
					Thread.sleep( delay );
					
				}catch( Throwable e ){
				}
			}
			
			KeyPair	res = dh_key_generator.generateKeyPair();
			
			last_dh_key_generate = now;
			
			return( res );
		}
	}
	
	protected void
	complete()
	{
		processing_complete	= true;
		
		adapter.decodeComplete( this );	
	}
	
	protected void
	failed(
		Throwable 	cause )
	{
		processing_complete	= true;
		
		read_selector.cancel( channel );
		
		write_selector.cancel( channel );

		adapter.decodeFailed( this, cause );
	}
	
	public boolean
	isComplete(
		long		now )
	{
		return( processing_complete );
	}
	
	public TCPTransportHelperFilter
	getFilter()
	{
		return( filter );
	}
	
	public SocketChannel
	getChannel()
	{
		return( channel );
	}
}
