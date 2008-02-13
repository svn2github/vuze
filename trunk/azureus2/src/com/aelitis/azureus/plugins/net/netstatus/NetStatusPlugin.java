/*
 * Created on Jan 30, 2008
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


package com.aelitis.azureus.plugins.net.netstatus;

import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.plugins.net.netstatus.swt.NetStatusPluginView;

public class 
NetStatusPlugin
	implements Plugin, DistributedDatabaseTransferHandler
{
	public static final String VIEW_ID = "aznetstatus";
	
	private LoggerChannel	logger;
	
	private StringParameter ping_target;
	private StringParameter test_address;
	
	private DistributedDatabase	ddb;
	
	private testXferType		transfer_type;
	
	
	public void
	initialize(
		final PluginInterface		plugin_interface )
	{
		String name_res = "Views.plugins." + VIEW_ID + ".title";
		
		String name = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( name_res );
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		name );

		logger = plugin_interface.getLogger().getChannel( "NetStatus" );
		
		logger.setDiagnostic();
		
		transfer_type = new testXferType();
		
		BasicPluginConfigModel config = plugin_interface.getUIManager().createBasicPluginConfigModel( name_res );
		
		ping_target = config.addStringParameter2( "plugin.aznetstatus.pingtarget", "plugin.aznetstatus.pingtarget", "www.google.com" );
		
		test_address = config.addStringParameter2( "plugin.aznetstatus.test_address", "plugin.aznetstatus.test_address", "" );

		ActionParameter test = config.addActionParameter2( "test", "test " );
		
		test.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					runTest();
				}
			});
		
		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						UISWTInstance swt_ui = (UISWTInstance)instance;
						
						NetStatusPluginView view = new NetStatusPluginView( NetStatusPlugin.this );

						swt_ui.addView(	UISWTInstance.VIEW_MAIN, VIEW_ID, view );
						
						//swt_ui.openMainView( VIEW_ID, view, null );
					}
				}

				public void
				UIDetached(
					UIInstance		instance )
				{
				}
			});
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					new AEThread2( "NetstatusPlugin:init", true )
					{
						public void
						run()
						{
							try{
								ddb = plugin_interface.getDistributedDatabase();
								
								ddb.addTransferHandler(
									transfer_type,
									NetStatusPlugin.this );
								
							}catch( Throwable e ){
								
								log( "DDB transfer type registration failed", e );
							}
						}
					}.start();
				}
				
				public void
				closedownInitiated()
				{				
				}
				
				public void
				closedownComplete()
				{				
				}
			});
	}
	
	protected void
	runTest()
	{
		try{
			String str = test_address.getValue();
	
			String[]	bits = str.split( ":" );
			
			if ( bits.length != 2 ){
				
				log( "Invalid address - use <host>:<port> " );
				
				return;
			}
			
			InetSocketAddress address = new InetSocketAddress( bits[0].trim(), Integer.parseInt( bits[1].trim()));
			 
			DistributedDatabaseContact contact = ddb.importContact( address );
			
			Map	request = new HashMap();
			
			request.put( "v", new Long(1));
			
			Map	reply = sendRequest( contact, request );
			
			log( "Reply: " + reply );
			
		}catch( Throwable e ){
			
			log( "Test failed", e );
		}
	}
	
	protected Map
	sendRequest(
		DistributedDatabaseContact	contact,
		Map							request )
	{
		try{
			DistributedDatabaseKey key = ddb.createKey( BEncoder.encode( request ));
			
			DistributedDatabaseValue value = 
				contact.read( 
					new DistributedDatabaseProgressListener()
					{
						public void
						reportSize(
							long	size )
						{	
						}
						
						public void
						reportActivity(
							String	str )
						{	
						}
						
						public void
						reportCompleteness(
							int		percent )
						{
						}
					},
					transfer_type,
					key,
					20000 );
			
			if ( value == null ){
				
				return( null );
			}
			
			return( BDecoder.decode((byte[])value.getValue( byte[].class )));
			
		}catch( Throwable e ){
			
			log( "sendRequest failed", e );
			
			return( null );
		}
	}
	
	protected Map
	receiveRequest(
		Map		request )
	{
		Map	reply = new HashMap();
		
		reply.put( "v", new Long(1));
		
		return( reply );
	}
	
	public DistributedDatabaseValue
	read(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key )
	
		throws DistributedDatabaseException
	{
		Object	o_key = key.getKey();
		
		log( "Received ddb request from " + contact.getName() + ": " + o_key );

		try{
			Map	request = BDecoder.decode( (byte[])o_key);
			
			Map	result = receiveRequest( request );
			
			return( ddb.createValue( BEncoder.encode( result )));
			
		}catch( Throwable e ){
			
			log( "ddb read failed", e );
			
			return( null );
		}
	}
	
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
	
	public String
	getPingTarget()
	{
		return( ping_target.getValue());
	}
	
	public void
	log(
		String		str )
	{
		logger.log( str );
	}
	
	public void
	log(
		String		str,
		Throwable	e )
	{
		logger.log( str );
		logger.log( e );
	}
	
	protected class
	testXferType
		implements DistributedDatabaseTransferType
	{	
	}
}
