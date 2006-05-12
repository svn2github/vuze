/*
 * Created on 07-May-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.pluginsimpl.local.update;

/**
 * @author parg
 *
 */

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.update.*;

import com.aelitis.azureus.core.AzureusCore;

public class 
UpdateManagerImpl
	implements UpdateManager
{
	protected static UpdateManagerImpl		singleton;
	
    private static final String pub_exp = "10001";
    private static final String modulus	= "9a68296f49bf47b2a83ae4ba3cdb5a840a2689e5b36a6f2bfc27b916fc4dc9437f9087c4f0b5ae2fc5127a901b3c048753aa63d29cd7f9da7c81d475380de68236bd919230b0074aa6f40f29a78ac4a14e84fb8946cbcb5a840d1c2f77d83c795c289e37135843b8da008e082654a83b8bd3341b9f2ff6064e20b6c7ba89a707a1f3e1d8b2e0035dae539b04e49775eba23e5cbe89e22290da6c84ec3f450d07";
    
	
	public static UpdateManager
	getSingleton(
		AzureusCore		core )
	{
		if ( singleton == null ){
			
			singleton = new UpdateManagerImpl( core );
		}
		
		return( singleton );
	}

	protected AzureusCore	azureus_core;
		
	protected List	components 				= new ArrayList();
	protected List	listeners				= new ArrayList();
	protected List	verification_listeners	= new ArrayList();
	
	protected List	installers	= new ArrayList();
	
	protected AEMonitor	this_mon 	= new AEMonitor( "UpdateManager" );

	protected
	UpdateManagerImpl(
		AzureusCore		_azureus_core )
	{
		azureus_core	= _azureus_core;
		
		UpdateInstallerImpl.checkForFailedInstalls();
		
			// cause the platform manager to register any updateable components
		
		try{
			PlatformManagerFactory.getPlatformManager();
			
		}catch( Throwable e ){
		
		}
	}
	
	public void
	registerUpdatableComponent(
		UpdatableComponent		component,
		boolean					mandatory )
	{
		try{
			this_mon.enter();
			
			components.add( new UpdatableComponentImpl( component, mandatory ));
		}finally{
			
			this_mon.exit();
		}
	}
	
	
	public UpdateCheckInstance
	createUpdateCheckInstance()
	{
		return( createUpdateCheckInstance( UpdateCheckInstance.UCI_UPDATE, "" ));
	}
	
	public UpdateCheckInstance
	createUpdateCheckInstance(
		int			type,
		String		name )
	{
		try{
			this_mon.enter();
	
			UpdatableComponentImpl[]	comps = new UpdatableComponentImpl[components.size()];
			
			components.toArray( comps );
			
			UpdateCheckInstance	res = new UpdateCheckInstanceImpl( this, type, name, comps );
			
			for (int i=0;i<listeners.size();i++){
				
				((UpdateManagerListener)listeners.get(i)).checkInstanceCreated( res );
			}
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public UpdateCheckInstance
	createEmptyUpdateCheckInstance(
		int			type,
		String		name )
	{
		try{
			this_mon.enter();
	
			UpdatableComponentImpl[]	comps = new UpdatableComponentImpl[0];
			
			UpdateCheckInstance	res = new UpdateCheckInstanceImpl( this, type, name, comps );
			
			for (int i=0;i<listeners.size();i++){
				
				((UpdateManagerListener)listeners.get(i)).checkInstanceCreated( res );
			}
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}		
	}

	public UpdateInstaller
	createInstaller()
		
		throws UpdateException
	{
		UpdateInstaller	installer = new UpdateInstallerImpl();
		
		installers.add( installer );
		
		return( installer );
	}
	
	public UpdateInstaller[]
	getInstallers()
	{
		UpdateInstaller[]	res = new UpdateInstaller[installers.size()];
		
		installers.toArray( res );
		
		return( res );
	}
	
	public void
	restart()
	
		throws UpdateException
	{
		applyUpdates( true );
	}
	
	public void
	applyUpdates(
		boolean	restart_after )
	
		throws UpdateException
	{
		try{
			if ( restart_after ){
				
				azureus_core.requestRestart();
				
			}else{
				
				azureus_core.requestStop();
			}
		}catch( Throwable e ){
			
			throw( new UpdateException( "UpdateManager:applyUpdates fails", e ));
		}
	}
	
	public InputStream
	verifyData(
		Update			update,
		InputStream		is,
		boolean			force )
	
		throws UpdateException
	{
		boolean	queried 	= false;
		boolean	ok			= false;
		Throwable	failure	= null;
		
		try{
			File	temp = AETemporaryFileHandler.createTempFile();
			
			FileUtil.copyFile( is, temp );
			
			try{
				verifyData( temp );
			
				ok	= true;
				
				return( new FileInputStream( temp ));

			}catch( UpdateException e ){
								
				if ( (!force) && e.getMessage().indexOf( "Signature missing" ) != -1 ){
					
					for (int i=0;i<verification_listeners.size();i++){
						
						try{
							queried	= true;
							
							if ( ((UpdateManagerVerificationListener)verification_listeners.get(i)).acceptUnVerifiedUpdate(
									update )){
								
								ok	= true;
								
								return( new FileInputStream( temp ));
							}
						}catch( Throwable f ){
							
							Debug.printStackTrace(f);
						}
					}
				}
				
				failure	= e;
				
				throw( e );
			}
		}catch( UpdateException e ){

			failure	= e;
			
			throw( e );
				
		}catch( Throwable e ){
			
			failure	= e;
			
			throw( new UpdateException( "Verification failed", e ));
			
		}finally{
			
			if ( !( queried || ok )){
				
				if ( failure == null ){
					
					failure = new UpdateException( "Verification failed" );
				}
				
				for (int i=0;i<verification_listeners.size();i++){
					
					try{
						((UpdateManagerVerificationListener)verification_listeners.get(i)).verificationFailed( update, failure );
	
					}catch( Throwable f ){
						
						Debug.printStackTrace(f);
					}
				}
			}
		}
	}
	
	public static void
	verifyData(
		File		file )
	
		throws UpdateException, Exception
	{
		KeyFactory key_factory = KeyFactory.getInstance("RSA");
		
		RSAPublicKeySpec 	public_key_spec = 
			new RSAPublicKeySpec( new BigInteger(modulus,16), new BigInteger(pub_exp,16));

		RSAPublicKey public_key 	= (RSAPublicKey)key_factory.generatePublic( public_key_spec );

		verifyData( file, public_key );
	}
	
	protected static void
	verifyData(
		File			file,
		RSAPublicKey	key )
	
		throws Exception
	{
		ZipInputStream	zis = null;
		
		try{
			zis = new ZipInputStream( 
					new BufferedInputStream( new FileInputStream( file ) ));
				
			byte[]		signature	= null;
			
			Signature	sig = Signature.getInstance("MD5withRSA" );

			sig.initVerify( key );
			
			while( true ){
				
				ZipEntry	entry = zis.getNextEntry();
					
				if ( entry == null ){
					
					break;
				}
			
				if ( entry.isDirectory()){
					
					continue;
				}
				
				String	name = entry.getName();
			
				ByteArrayOutputStream	output = null;
				
				if ( name.equalsIgnoreCase("azureus.sig")){
					
					output	= new ByteArrayOutputStream();
				}
												
				byte[]	buffer = new byte[65536];
				
				while( true ){
				
					int	len = zis.read( buffer );
					
					if ( len <= 0 ){
						
						break;
					}
					
					if ( output == null ){
						
						sig.update( buffer, 0, len );
						
					}else{
						
						output.write( buffer, 0, len );
					}
				}
				
				if ( output != null ){
					
					signature = output.toByteArray();
				}
			}
						
			if ( signature == null ){
				
					// don't change this text, its used above!
				
				throw( new UpdateException( "Signature missing from file" ));
			}
			
			if ( !sig.verify( signature )){
				
				throw( new UpdateException( "Signature doesn't match data" ));
			}
		}finally{
			
			if ( zis != null ){
				
				zis.close();
			}
		}
	}
	
	public static void
	verifyData(
		String			data,
		byte[]			signature )
	
		throws Exception
	{
		KeyFactory key_factory = KeyFactory.getInstance("RSA");
		
		RSAPublicKeySpec 	public_key_spec = 
			new RSAPublicKeySpec( new BigInteger(modulus,16), new BigInteger(pub_exp,16));

		RSAPublicKey public_key 	= (RSAPublicKey)key_factory.generatePublic( public_key_spec );
		
		Signature	sig = Signature.getInstance("MD5withRSA" );

		sig.initVerify( public_key );
		
		sig.update( data.getBytes( "UTF-8" ));
			
		if ( !sig.verify( signature )){
			
			throw( new UpdateException( "Data verification failed, signature doesn't match data" ));
		}
	}
	
	public void
	addVerificationListener(
		UpdateManagerVerificationListener	l )
	{
		verification_listeners.add( l );
	}
	
	public void
	removeVerificationListener(
		UpdateManagerVerificationListener	l )
	{
		verification_listeners.add( l );
	}
	
	public void
	addListener(
		UpdateManagerListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removeListener(
		UpdateManagerListener	l )
	{
		listeners.remove(l);
	}
}
