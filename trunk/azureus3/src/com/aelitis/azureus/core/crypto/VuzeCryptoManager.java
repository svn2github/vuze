/*
 * Created on Apr 18, 2008
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


package com.aelitis.azureus.core.crypto;

import java.util.Iterator;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.security.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;

public class 
VuzeCryptoManager 
{
	private static VuzeCryptoManager	singleton;
	
	public static synchronized VuzeCryptoManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new VuzeCryptoManager();
		}
		
		return( singleton );
	}
	
	private boolean			init_tried;
	
	private CryptoManager	crypt_man;
	private CopyOnWriteList	listeners = new CopyOnWriteList();
	
	private volatile CryptoManagerPasswordHandler.passwordDetails	session_pw;
	
	protected
	VuzeCryptoManager()
	{
		crypt_man = CryptoManagerFactory.getSingleton();
		
		crypt_man.addPasswordHandler(
			new CryptoManagerPasswordHandler()
			{
				public int
				getHandlerType()
				{
					return( HANDLER_TYPE_SYSTEM );
				}
				
				public passwordDetails
				getPassword(
					int			handler_type,
					int			action_type,
					boolean		last_pw_incorrect,
					String		reason )
				{
					if ( last_pw_incorrect ){
						
						Iterator it = listeners.iterator();
						
						while( it.hasNext()){

							((VuzeCryptoListener)it.next()).sessionPasswordIncorrect();
						}
						
						return( null );
					}
					
					if ( session_pw != null ){
						
						return( session_pw );
					}
					
					Iterator it = listeners.iterator();
					
					while( it.hasNext()){
						
						try{
						
							final char[] pw = ((VuzeCryptoListener)it.next()).getSessionPassword( reason );
							
							session_pw =
								new passwordDetails()
								{
									public char[] 
									getPassword() 
									{
										return( pw );
									}
									
									public int 
									getPersistForSeconds()
									{
										return( -1 );	// session 
									}
								};
							
								return( session_pw );
								
						}catch( Throwable e ){
							
							Debug.out( "Listener failed", e );
						}
					}
					
					Debug.out( "VuzeCryptoManager: no listeners returned session key" );
					
					return( null );
				}
			});
		
			// auto enable buddy plugin and system handler 
		
		boolean	init_done = COConfigurationManager.getBooleanParameter( "vuze.crypto.manager.initial.login.done", false );
		
		if ( !init_done ){
		
			AzureusCore core = AzureusCoreFactory.getSingleton();
			
				// back this off until core started otherwise plugin not loaded...
			
			AzureusCoreFactory.getSingleton().addLifecycleListener(
				new AzureusCoreLifecycleAdapter()
				{
					public void
					started(
						AzureusCore		core )
					{			
						initialise( core );
					}
				});
			
				// if already started do now
			
			if ( core.isStarted()){
				
				initialise( core );
			}
		}
	}
	
	protected void
	initialise(
		AzureusCore		core )
	{
		synchronized( this ){
			
			if ( init_tried ){
				
				return;
			}
			
			init_tried = true;
		}
		
		PluginInterface pi =  core.getPluginManager().getPluginInterfaceByID( "azbuddy" );
		
		if ( pi != null ){
			
			Plugin plugin = pi.getPlugin();
			
			if ( plugin instanceof BuddyPlugin ){
				
				BuddyPlugin buddy_plugin = (BuddyPlugin)plugin;
				
				if ( !buddy_plugin.isEnabled()){
					
					CryptoHandler handler = crypt_man.getECCHandler();
					
						// try and switch password handler if no keys yet defined
					
					if ( handler.peekPublicKey() == null ){
						
						try{
							handler.setDefaultPasswordHandlerType(
								CryptoManagerPasswordHandler.HANDLER_TYPE_SYSTEM );
						
						}catch( Throwable e ){
							
							VuzeBuddyManager.log( "CRYPTO: Failed to set default password handler type: " + Debug.getNestedExceptionMessage( e ));
						}
					}
					
					buddy_plugin.setEnabled( true );
					
					COConfigurationManager.setParameter( "vuze.crypto.manager.initial.login.done", true );
					
					COConfigurationManager.save();
					
					VuzeBuddyManager.log( "CRYPTO: initialised buddy plugin and default handler type" );
				}
			}
		}
	}
	
	public byte[]
	getPlatformAZID()
	{
		return( crypt_man.getSecureID());
	}

	public String
	getPublicKey(
		String		reason )
	
		throws VuzeCryptoException
	{
		try{
			return( Base32.encode(crypt_man.getECCHandler().getPublicKey(reason)));
			
		}catch( Throwable e ){
			
			throw( new VuzeCryptoException( "Failed to access public key", e ));
		}
	}
	
	public boolean
	hasPublicKey()
	{
		return crypt_man.getECCHandler().peekPublicKey() != null;
	}
	
	public void
	createNewKeys()
	
		throws VuzeCryptoException
	{
		try{
			crypt_man.getECCHandler().resetKeys( "Creating new keys" );
			
		}catch( Throwable e ){
			
			throw( new VuzeCryptoException( "Failed to access public key", e ));
		}
	}
	
		/**
		 * Remove cached password
		 */
	
	public void
	clearPassword()
	{
		session_pw	= null;
	
		crypt_man.clearPasswords();
	}
	
		/**
		 * Explicitly set password instead of waiting for listener trigger
		 * @param pw
		 */
		 
	public void
	setPassword(
		String		pw )
	{
		final char[]	pw_chars = pw.toCharArray();
		
		session_pw =
			new CryptoManagerPasswordHandler.passwordDetails()
			{
				public char[] 
				getPassword() 
				{
					return( pw_chars );
				}
				
				public int 
				getPersistForSeconds()
				{
					return( -1 );	// session 
				}
			};
	}
	
	public void
	addListener(
		VuzeCryptoListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		VuzeCryptoListener		listener )
	{
		listeners.remove( listener );
	}
}
