/*
 * Created on 13-Jul-2004
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

package com.aelitis.azureus.core.impl;

import java.io.UnsupportedEncodingException;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerFactory;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.*;

/**
 * @author parg
 *
 */

public class 
AzureusCoreImpl 
	extends		LocaleUtil
	implements 	AzureusCore
{
	protected static AzureusCore		singleton;
	
	public static synchronized AzureusCore
	create()
	
		throws AzureusCoreException
	{
		if ( singleton != null ){
	
			throw( new AzureusCoreException( "Azureus core already instantiated" ));
		}
		
		singleton	= new AzureusCoreImpl();
		
		return( singleton );
	}
	
	protected GlobalManager		global_manager;
	
	
	protected boolean			running;
	
	
	protected
	AzureusCoreImpl()
	{
		COConfigurationManager.setSystemProperties();
	}
	
	public synchronized void
	start()
	
		throws AzureusCoreException
	{
		if ( running ){
			
			throw( new AzureusCoreException( "core already running" ));
		}
		
		running	= true;
		
		LocaleUtil.setLocaleUtilChooser(this);
		
		global_manager = GlobalManagerFactory.create();
			
		COConfigurationManager.checkConfiguration();
		
	    PluginInitializer.getSingleton(global_manager,null).initializePlugins( PluginManager.UI_NONE );
	        
	     new AEThread("Plugin Init Complete")
	        {
	        	public void
	        	run()
	        	{
	        		PluginInitializer.initialisationComplete();
	        	}
	        }.start();    
	}
	
	public void
	stop()
	
		throws AzureusCoreException
	{
		
	}
	
	public GlobalManager
	getGlobalManager()
	
		throws AzureusCoreException
	{
		if ( !running ){
			
			throw( new AzureusCoreException( "core not running" ));
		}
		
		return( global_manager );
	}
	
	
	public LocaleUtil getProperLocaleUtil() {
		return this;
	}
	
	public String getChoosableCharsetString(byte[] array) throws UnsupportedEncodingException {
		return( new String( array ));
	}
	
}
