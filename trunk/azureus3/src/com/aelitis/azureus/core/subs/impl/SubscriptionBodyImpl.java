/*
 * Created on Jul 15, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.subs.impl;

import java.io.File;
import java.io.IOException;
import java.security.Signature;
import java.util.*;

import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.security.CryptoECCUtils;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;

public class 
SubscriptionBodyImpl 
{
	private SubscriptionManagerImpl		manager;
	
	private String	name;
	private byte[]	public_key;
	private int		version;
	
	private byte[]	hash;
	private byte[]	sig;
	private int		sig_data_size;
	
	private Map		map;

		// import constructor
	
	protected 
	SubscriptionBodyImpl(
		SubscriptionManagerImpl	_manager,
		Map						_map )
	
		throws IOException
	{
		manager	= _manager;
		map		= _map;
		
		hash 	= (byte[])map.get( "hash" );
		sig	 	= (byte[])map.get( "sig" );
		Long	l_size	= (Long)map.get( "size" );
		
		Map	details = (Map)map.get( "details" );
		
		if ( details == null || hash == null || sig == null || l_size == null ){
			
			throw( new IOException( "Invalid subscription - details missing" ));
		}
		
		sig_data_size	= l_size.intValue();
		
		name		= new String((byte[])details.get( "name" ), "UTF-8" );
		public_key	= (byte[])details.get( "public_key" );
		version		= ((Long)details.get( "version" )).intValue();
		
			// verify
		
		byte[] contents = BEncoder.encode( details );
		
		byte[] actual_hash = new SHA1Simple().calculateHash( contents );

		if ( !Arrays.equals( actual_hash, hash )){
			
			throw( new IOException( "Hash mismatch" ));
		}
		
		if ( sig_data_size != contents.length ){
			
			throw( new IOException( "Signature data length mismatch" ));
		}
		
		try{
			Signature signature = CryptoECCUtils.getSignature( CryptoECCUtils.rawdataToPubkey( public_key ));

			signature.update( hash );
			signature.update( SubscriptionImpl.intToBytes(version));
			signature.update( SubscriptionImpl.intToBytes(sig_data_size));

			if ( !signature.verify( sig )){
				
				throw( new IOException( "Signature verification failed" ));
			}
			
			
		}catch( Throwable e ){
			
			if ( e instanceof IOException ){
				
				throw((IOException)e);
				
			}else{
				
				throw( new IOException( "Crypto failed: " + Debug.getNestedExceptionMessage(e)));

			}
		}
	}

		// create constructor
	
	protected
	SubscriptionBodyImpl(
		SubscriptionManagerImpl	_manager,
		String					_name,
		byte[]					_public_key,
		int						_version )
	
		throws IOException
	{
		manager		= _manager;
		
		name		= _name;
		public_key	= _public_key;
		version		= _version;
		
		map			= new HashMap();
		
		Map details = new HashMap();
			
		map.put( "details", details );
		
		details.put( "name", name.getBytes( "UTF-8" ));
		details.put( "public_key", public_key );
		details.put( "version", new Long( version ));
	}
	
	protected String
	getName()
	{
		return( name );
	}
	
	protected byte[]
	getPublicKey()
	{
		return( public_key );
	}
	
	protected int
	getVersion()
	{
		return( version );
	}
	
		// derived data
	
	protected byte[]
	getHash()
	{
		return( hash );
	}

	protected byte[]
	getSig()
	{
		return( sig );
	}
	
	protected int
	getSigDataSize()
	{
		return( sig_data_size );
	}
	
	/*
	 * 			TOTorrentCreator creator = 
				TOTorrentFactory.createFromFileOrDirWithFixedPieceLength( 
					file, 
					TorrentUtils.getDecentralisedEmptyURL(),
					256*1024 );
	 */
	protected void
	writeVuzeFile(
		SubscriptionImpl		subs )
	
		throws IOException
	{
		File file = manager.getVuzeFile( subs );

		if ( map == null ){
	
			readMap( file );
		}
						
		byte[] old_hash	= (byte[])map.get( "hash" );
				
		Map	details = (Map)map.get( "details" );
		
		byte[] contents = BEncoder.encode( details );
				
		byte[] new_hash = new SHA1Simple().calculateHash( contents );
		
		if ( old_hash == null || !Arrays.equals( old_hash, new_hash )){
			
			byte[]	private_key = subs.getPrivateKey();
			
			if ( private_key == null ){
				
				throw( new IOException( "Only the originator of a subscription can modify it" ));
			}
						
			map.put( "size", new Long( contents.length ));
			
			try{
				Signature signature = CryptoECCUtils.getSignature( CryptoECCUtils.rawdataToPrivkey( private_key ));
				
					// key for signature is hash + version + size so we have some
					// control over auto-update process and prevent people from injecting
					// potentially huge bogus updates
				
				signature.update( new_hash );
				signature.update( SubscriptionImpl.intToBytes(version));
				signature.update( SubscriptionImpl.intToBytes(contents.length));
				
				map.put( "hash", new_hash );
				
				map.put( "sig", signature.sign());
				
			}catch( Throwable e ){
				
				throw( new IOException( "Crypto failed: " + Debug.getNestedExceptionMessage(e)));
			}
		}
		
		File	backup_file	= null;
		
		if ( file.exists()){
			
			backup_file = new File( file.getParent(), file.getName() + ".bak" );
			
			backup_file.delete();
			
			if ( !file.renameTo( backup_file )){
				
				throw( new IOException( "Backup failed" ));
			}
		}
		
		try{
			VuzeFile	vf = VuzeFileHandler.getSingleton().create();
			
			vf.addComponent( VuzeFileComponent.COMP_TYPE_SUBSCRIPTION, map );
			
			vf.write( file );
		
			hash			= new_hash;
			sig				= (byte[])map.get( "sig" );
			sig_data_size	= contents.length;
			
		}catch( Throwable e ){
			
			if ( backup_file != null ){
				
				backup_file.renameTo( file );
			}
			
			if ( e instanceof IOException ){
				
				throw((IOException)e);
			}
			
			throw( new IOException( "File write failed: " + Debug.getNestedExceptionMessage(e)));
		}
	}
	
	protected Map
	readMap(
		File	vuze_file )
	
		throws IOException
	{
		VuzeFile	vf = VuzeFileHandler.getSingleton().loadVuzeFile( vuze_file.getAbsolutePath());

		if ( vf == null ){
			
			throw( new IOException( "Failed to load vuze file '" + vuze_file + "'" ));
		}
		
		return( vf.getComponents()[0].getContent());
	}
}
