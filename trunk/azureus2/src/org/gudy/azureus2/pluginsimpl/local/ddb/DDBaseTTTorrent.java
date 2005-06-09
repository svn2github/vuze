/*
 * Created on 03-Mar-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.ddb;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.plugins.dht.DHTPluginProgressListener;

/**
 * @author parg
 *
 */

public class 
DDBaseTTTorrent
	implements DistributedDatabaseTransferType, DistributedDatabaseTransferHandler
{
	public static final boolean	TRACE			= false;
	
	public static final byte	CRYPTO_VERSION	= 1;
	
	static{
		if ( TRACE ){
			System.out.println( "**** Torrent xfer tracing on ****" );
		}
	}
	private AzureusCore		azureus_core;
	private DDBaseImpl		ddb;
		
	private TorrentAttribute	ta_sha1;
	
	private boolean				crypto_tested;
	private boolean				crypto_available;
	
	protected
	DDBaseTTTorrent(
		AzureusCore		_azureus_core,
		DDBaseImpl		_ddb )
	{
		azureus_core		= _azureus_core;
		ddb					= _ddb;
	}
	
		// server side read
	
	public DistributedDatabaseValue
	read(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key )
	
		throws DistributedDatabaseException
	{
			// from protocol version 8 we use sha1(hash) as the key for torrent downloads
			// and encrypt the torrent content using the hash as the basis for a key. This
			// prevents someone without the hash from downloading the torrent

		int	protocol_version = ((DDBaseContactImpl)contact).getContact().getProtocolVersion();
		
		try{
			byte[]	search_key = ((DDBaseKeyImpl)key).getBytes();
			
			Download 	download = null;
				
			PluginInterface pi = azureus_core.getPluginManager().getDefaultPluginInterface();
			
			boolean	encrypt	= false;
			
			if ( protocol_version >= 8 ){	// DHTTransportUDP.PROTOCOL_VERSION_ENCRYPT_TT
				
				String	search_sha1 = pi.getUtilities().getFormatters().encodeBytesToString( search_key );
				
				if ( ta_sha1 == null ){
					
					ta_sha1 = pi.getTorrentManager().getPluginAttribute( "DDBaseTTTorrent::sha1");
				}
					
					// gotta look for the sha1(hash)
				
				Download[]	downloads = pi.getDownloadManager().getDownloads();
				
				for (int i=0;i<downloads.length;i++){
					
					Download	dl = downloads[i];
					
					if ( dl.getTorrent() == null ){
						
						continue;
					}
					
					String	sha1 = dl.getAttribute( ta_sha1 );
					
					if ( sha1 == null ){
						
						sha1 = pi.getUtilities().getFormatters().encodeBytesToString( 
									new SHA1Simple().calculateHash( dl.getTorrent().getHash()));
						
						dl.setAttribute( ta_sha1, sha1 );
					}
					
					if ( sha1.equals( search_sha1 )){
						
						download	= dl;
						
						encrypt	= true;
						
						break;
					}
				}
				
					// there's a bug whereby 2.3.0.2 + below clients, given a contact indirectly at, say, version 8
					// will send a request claiming to be version 8, whereas it really is version 7
					// to continue to work correctly with these we fall back to 
					
				if ( download == null ){
					
					download = pi.getShortCuts().getDownload( search_key );
					
					if (TRACE ){
						
						System.out.println( "TorrentXfer: received lookup via hash, fallback to V7 -> " + download );
					}
				}else{
					
					if ( TRACE ){
						
						System.out.println( "TorrentXfer: received lookup via sha1(hash) -> " + download );
					}
				}
				
			}else{
				
				download = pi.getShortCuts().getDownload( search_key );
				
				if ( TRACE ){
					
					System.out.println( "TorrentXfer: received lookup via hash -> " + download );
				}
			}
			
			if ( download == null ){
				
				String msg = "TorrentDownload: " + (encrypt?"secure":"insecure") + " request for '" + pi.getUtilities().getFormatters().encodeBytesToString( search_key ) + "' not found";
				
				if ( TRACE ){
					
					System.out.println( msg );
				}
				
				ddb.log( msg );
				
					// torrent not found - probably been removed whilst info still published in DHT
				
				return( null );
				
			}
			
			Torrent	torrent = download.getTorrent();
			
			String	msg = "TorrentDownload: " + (encrypt?"secure":"insecure") + " request for '" + download.getName() + "' OK";		

			if ( TRACE ){
				
				System.out.println( msg );
			}
			
			ddb.log( msg );
			
			torrent = torrent.removeAdditionalProperties();
			
				// when clients get a torrent from the DHT they take on
				// responsibility for tracking it too
			
			torrent.setDecentralisedBackupRequested( true );
			
			byte[] data = torrent.writeToBEncodedData();
			
			if ( encrypt ){
				
				data = encrypt( torrent.getHash(), data );
				
				if ( data == null ){
					
					return( null );
				}
			}
			
			return( ddb.createValue( data ));
			
		}catch( DownloadException e ){
			
				// torrent not found in shortcut stuff
			
			return( null );
			
		}catch( Throwable e ){
			
			throw( new DistributedDatabaseException("Torrent write fails", e ));
		}
	}
	
		// server side write
	
	public void
	write(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key,
		DistributedDatabaseValue			value )
	
		throws DistributedDatabaseException
	{
		throw( new DistributedDatabaseException( "not supported" ));
	}
	
		// client side read
	
	protected DistributedDatabaseValue
	read(
		DDBaseContactImpl							contact,
		final DistributedDatabaseProgressListener	listener,
		DistributedDatabaseTransferType				type,
		DistributedDatabaseKey						key,
		long										timeout )
	
		throws DistributedDatabaseException
	{
			// see comment above
		
		int	protocol_version = contact.getContact().getProtocolVersion();

		byte[]	torrent_hash	= ((DDBaseKeyImpl)key).getBytes();
		
		byte[]	lookup_key;
		
		if ( protocol_version >= 8 ){	// DHTTransportUDP.PROTOCOL_VERSION_ENCRYPT_TT
			
			if ( TRACE ){
				System.out.println( "TorrentXfer: sending via sha1(hash)" );
			}

			lookup_key	= new SHA1Simple().calculateHash( torrent_hash );
			
		}else{
			
			if (TRACE ){
				System.out.println( "TorrentXfer: sending via hash" );
			}

			lookup_key	= torrent_hash;
		}
		
		byte[]	data = ddb.getDHT().read( 
							new DHTPluginProgressListener()
							{
								public void
								reportSize(
									long	size )
								{
									listener.reportSize( size );
								}
								
								public void
								reportActivity(
									String	str )
								{
									listener.reportActivity( str );
								}
								
								public void
								reportCompleteness(
									int		percent )
								{
									listener.reportCompleteness( percent );
								}
							},
							contact.getContact(),
							DDBaseHelpers.getKey(type.getClass()).getHash(),
							lookup_key,
							timeout );
							
		if ( data == null ){
			
			return( null );
		}
		
		if ( protocol_version >= 8 ){	// DHTTransportUDP.PROTOCOL_VERSION_ENCRYPT_TT

			data = decrypt( torrent_hash, data );
			
			if ( data == null ){
				
				return( null );
			}
		}
		
		return( new DDBaseValueImpl( contact, data, SystemTime.getCurrentTime()));
	}
	
	protected byte[]
   	encrypt(
   		byte[]		hash,
   		byte[]		data )
   	{
		if ( !testCrypto()){
			
			return( null );
		}
		
   		byte[]	enc = doCrypt( Cipher.ENCRYPT_MODE, hash, data, 0 );
		
		if ( enc == null ){
			
			if ( TRACE ){
				
				System.out.println( "TorrentXfer: encryption failed, using plain" );
			}
			
			byte[]	res = new byte[data.length+2];
			
			res[0] = CRYPTO_VERSION;
			res[1] = 0;	// not encrypted

			System.arraycopy( data, 0, res, 2, data.length );
			
			return( res );
			
		}else{
		
			if ( TRACE ){
				
				System.out.println( "TorrentXfer: encryption ok" );
			}

			byte[]	res = new byte[enc.length+2];
			
			res[0] = CRYPTO_VERSION;
			res[1] = 1;	// encrypted

			System.arraycopy( enc, 0, res, 2, enc.length );

			return( res );
		}
   	}
	
	protected byte[]
  	decrypt(
  		byte[]		hash,
  		byte[]		data )
  	{
		if ( !testCrypto()){
			
			return( null );
		}
		
		if ( data[0] != CRYPTO_VERSION ){
			
			Debug.out( "Invalid crypto version received" );
			
			return( data );
		}
		
		if ( data[1] == 0 ){
			
				// encryption failed, in plain
			
			if ( TRACE ){
				System.out.println( "TorrentXfer: encryption failed, retrieving plain" );
			}

			byte[]	res = new byte[data.length-2];
			
			System.arraycopy( data, 2, res, 0, res.length );
			
			return( res );
			
		}else{
  		
			if ( TRACE ){
				System.out.println( "TorrentXfer: encryption ok, decrypting" );
			}

			byte[]	res =  doCrypt( Cipher.DECRYPT_MODE, hash, data, 2 );
			
			return( res );
		}
  	}
	
	protected byte[]
	doCrypt(
		int			mode,
		byte[]		hash,
		byte[]		data,
		int			data_offset )
	{
		try{
			byte[]	key_data = new byte[24];
			
				// hash is 20 bytes so we've got 4 zeros at the end. tough
			
			System.arraycopy( hash, 0, key_data, 0, hash.length );
			
			SecretKey tdes_key = new SecretKeySpec( key_data, "DESede" );

			Cipher cipher = Cipher.getInstance("DESede");  // Triple-DES encryption

			cipher.init(mode, tdes_key );
						
			return( cipher.doFinal(data, data_offset, data.length - data_offset ));
						
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( null );
		}
	}
	
	protected boolean
	testCrypto()
	{
		if ( !crypto_tested ){
			
			crypto_tested	= true;
		
			try{
				Cipher.getInstance("DESede");  // Triple-DES encryption
	
				crypto_available	= true;
				
			}catch( Throwable e ){
				
				LGLogger.logUnrepeatableAlert( 
						"Unable to initialise cryptographic framework for magnet-based torrent downloads, please re-install Java",
						e);
			}
		}
		
		return( crypto_available );
	}
}
