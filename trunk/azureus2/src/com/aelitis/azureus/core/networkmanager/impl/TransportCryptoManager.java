/*
 * Created on Jan 18, 2006
 * Created by Alon Rohter
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

import java.nio.ByteBuffer;


/**
 * 
 */
public class TransportCryptoManager {
		
	private static final TransportCryptoManager instance = new TransportCryptoManager();
	
	
	public static TransportCryptoManager getSingleton() {  return instance;  }


	
	public void 
	manageCrypto( 
		TransportHelper				transport,
		byte[]						shared_secret,
		boolean 					is_incoming, 
		final HandshakeListener 	listener ) 
	{						
			try{
				new ProtocolDecoderInitial( 
						transport, 
						shared_secret,
						!is_incoming,
						new ProtocolDecoderAdapter()
						{
							public int
							getMaximumPlainHeaderLength()
							{
								return( listener.getMaximumPlainHeaderLength());
							}
							
							public int
							matchPlainHeader(
								ByteBuffer			buffer )
							{
								return( listener.matchPlainHeader( buffer ));
							}

							public void
							decodeComplete(
								ProtocolDecoder	decoder )
							{
								listener.handshakeSuccess( decoder.getFilter());
							}
							
							public void
							decodeFailed(
								ProtocolDecoder	decoder,
								Throwable			cause )
							{
								listener.handshakeFailure( cause );
							}
						});
			}catch( Throwable e ){
				
				listener.handshakeFailure( e );
			}
		
	}
	
	
	
	
	public interface HandshakeListener {
		public static final int MATCH_NONE						= ProtocolDecoderAdapter.MATCH_NONE;
		public static final int MATCH_CRYPTO_NO_AUTO_FALLBACK	= ProtocolDecoderAdapter.MATCH_CRYPTO_NO_AUTO_FALLBACK;
		public static final int MATCH_CRYPTO_AUTO_FALLBACK		= ProtocolDecoderAdapter.MATCH_CRYPTO_AUTO_FALLBACK;
		
		public void handshakeSuccess( TransportHelperFilter filter );

		public void handshakeFailure( Throwable failure_msg );
		
		public int getMaximumPlainHeaderLength();
		
		public int matchPlainHeader( ByteBuffer buffer );
  }
}
