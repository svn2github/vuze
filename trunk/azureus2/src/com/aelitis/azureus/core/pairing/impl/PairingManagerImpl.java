/*
 * Created on Oct 5, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.pairing.impl;

import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.InfoParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.pairing.*;

public class 
PairingManagerImpl
	implements PairingManager
{
	private static final PairingManagerImpl	singleton = new PairingManagerImpl();
	
	public static PairingManager
	getSingleton()
	{
		return( singleton );
	}
	
	private InfoParameter		param_ac;
	private BooleanParameter 	param_e_enable;
	private StringParameter		param_ipv4;
	private StringParameter		param_ipv6;
	private StringParameter		param_host;
	
	private Map<String,PairedService>		services = new HashMap<String, PairedService>();
	
	private AESemaphore	init_sem = new AESemaphore( "PM:init" );
	
	private TimerEventPeriodic	update_event;
	
	protected
	PairingManagerImpl()
	{
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener()
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					initialise( core );
				}
			});
	}
	
	
	protected void
	initialise(
		AzureusCore		_core )
	{
		try{
			PluginInterface default_pi = PluginInitializer.getDefaultInterface();
	
			final UIManager	ui_manager = default_pi.getUIManager();
			
			BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
					ConfigSection.SECTION_CONNECTION, "Pairing");
	
			param_ac = configModel.addInfoParameter2( "pairing.accesscode", "" );
			
			final ActionParameter ap = configModel.addActionParameter2( "pairing.ac.getnew", "pairing.ac.getnew.create" );
			
			ap.addListener(
				new ParameterListener()
				{
					public void 
					parameterChanged(
						Parameter 	param ) 
					{
						try{
							ap.setEnabled( false );
							
							allocateAccessCode();
							
							SimpleTimer.addEvent(
								"PM:enabler",
								SystemTime.getOffsetTime(30*1000),
								new TimerEventPerformer()
								{
									public void 
									perform(
										TimerEvent event ) 
									{
										ap.setEnabled( true );
									}
								});
							
						}catch( Throwable e ){
							
							ap.setEnabled( true );
							
							String details = MessageText.getString(
									"pairing.alloc.fail",
									new String[]{ Debug.getNestedExceptionMessage( e )});
							
							ui_manager.showMessageBox(
									"pairing.op.fail",
									"!" + details + "!",
									UIManagerEvent.MT_OK );
						}
					}
				});
			
			LabelParameter	param_e_info = configModel.addLabelParameter2( "pairing.explicit.info" );
			
			param_e_enable = configModel.addBooleanParameter2( "pairing.explicit.enable", "pairing.explicit.enable", false );
			
			param_ipv4	= configModel.addStringParameter2( "pairing.ipv4", "pairing.ipv4", "" );
			param_ipv6	= configModel.addStringParameter2( "pairing.ipv6", "pairing.ipv6", "" );
			param_host	= configModel.addStringParameter2( "pairing.host", "pairing.host", "" );
			
			param_e_enable.addEnabledOnSelection( param_ipv4 );
			param_e_enable.addEnabledOnSelection( param_ipv6 );
			param_e_enable.addEnabledOnSelection( param_host );
			
			configModel.createGroup(
				"pairing.group.explicit",
				new Parameter[]{
					param_e_info,
					param_e_enable,
					param_ipv4,	
					param_ipv6,
					param_host,
				});
			
		}finally{
			
			init_sem.releaseForever();
		}
	}
	
	protected void
	waitForInitialisation()
	
		throws PairingException
	{
		if ( !init_sem.reserve( 30*1000 )){
		
			throw( new PairingException( "Timeout waiting for initialisation" ));
		}
	}
	
	protected void
	allocateAccessCode()
	
		throws PairingException
	{
		param_ac.setValue( "og" );
		
		//throw( new PairingException( "parp" ));
	}
	
	public String
	getAccessCode()
	
		throws PairingException
	{
		waitForInitialisation();
		
		String ac = param_ac.getValue();
		
		if ( ac == null || ac.length() == 0 ){
			
			allocateAccessCode();
		}
		
		return( param_ac.getValue());
	}
	
	public String
	getReplacementAccessCode()
	
		throws PairingException
	{
		waitForInitialisation();
		
		allocateAccessCode();
		
		return( param_ac.getValue());
	}
	
	public PairedService
	addService(
		String		sid )
	{
		synchronized( services ){
			
			if ( update_event == null ){
				
				update_event = 
					SimpleTimer.addPeriodicEvent(
					"PM:updater",
					60*1000,
					new TimerEventPerformer()
					{
						public void 
						perform(
							TimerEvent event ) 
						{
							updateGlobals();
						}
					});
				
				updateGlobals();
			}
			
			PairedService	result = services.get( sid );
			
			if ( result == null ){
				
				System.out.println( "PS: added " + sid );

				result = new PairedServiceImpl( sid );
				
				services.put( sid, result );
			}
			
			return( result );
		}
	}
	
	public PairedService
	getService(
		String		sid )
	{
		synchronized( services ){
			
			PairedService	result = services.get( sid );
			
			if ( services.size() == 0 ){
				
				if ( update_event != null ){
					
					update_event.cancel();
					
					update_event = null;
				}
			}
			return( result );
		}
	}
	
	protected void
	remove(
		PairedServiceImpl	service )
	{
		synchronized( services ){

			String sid = service.getSID();
			
			if ( services.remove( sid ) != null ){
				
				System.out.println( "PS: removed " + sid );
			}
		}
		
		updateNeeded();
	}
	
	protected void
	sync(
		PairedServiceImpl	service )
	{
		updateNeeded();
	}
	
	protected void
	updateGlobals()
	{
		
	}
	
	protected void
	updateNeeded()
	{
		System.out.println( "PS: updateNeeded" );
	}
	
	protected class
	PairedServiceImpl
		implements PairedService, PairingConnectionData
	{
		private String				sid;
		private Map<String,String>	attributes	= new HashMap<String, String>();
		
		protected
		PairedServiceImpl(
			String		_sid )
		{
			sid		= _sid;
		}
		
		public String
		getSID()
		{
			return( sid );
		}
		
		public PairingConnectionData
		getConnectionData()
		{
			return( this );
		}
		
		public void
		remove()
		{
			PairingManagerImpl.this.remove( this );
		}
		
		public void
		setAttribute(
			String		name,
			String		value )
		{
			System.out.println( "PS: " + sid + ": " + name + " -> " + value );

			attributes.put( name, value );
		}
		
		public String
		getAttribute(
			String		name )
		{
			return( attributes.get( name ));
		}
		
		public void
		sync()
		{
			PairingManagerImpl.this.sync( this );
		}
	}
}
