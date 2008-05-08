/*
 * Created on May 6, 2008
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


package com.aelitis.azureus.core.metasearch.impl;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearch;
import com.aelitis.azureus.core.metasearch.MetaSearchManager;
import com.aelitis.azureus.util.Constants;

public class 
MetaSearchManagerImpl
	implements MetaSearchManager
{
	private static final String	LOGGER_NAME = "MetaSearch";
	
	private static final int REFRESH_MILLIS = 23*60*60*1000;
	
	private static final MetaSearchManager	singleton = new MetaSearchManagerImpl();
	
	public static MetaSearchManager
	getSingleton()
	{
		return( singleton );
	}
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( 10000 );
	
	protected
	MetaSearchManagerImpl()
	{
		SimpleTimer.addPeriodicEvent(
			"MetaSearchRefresh",
			REFRESH_MILLIS,
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent 	event ) 
				{
					refresh();
				}
			});
		
		refresh();
	}
	
	protected void
	refresh()
	{
		dispatcher.dispatch(
			new AERunnable()
			{
				public void 
				runSupport() 
				{
					if ( dispatcher.getQueueSize() == 0 ){
						
						refreshSupport();
					}
				}
			});
	}
	
	protected void
	refreshSupport()
	{
		log( "Refreshing engines" );
		
			// featured templates are always shown - can't be deselected
			// popular ones are selected if in 'auto' mode - can't be deselected
			// manually selected ones are, well, manually selected
		
		List		selected_ids = new ArrayList();
		
		boolean		auto_mode = isAutoMode();
		
		Engine[]	engines = getMetaSearch().getEngines();

		try{
			PlatformMetaSearchMessenger.listFeaturedTemplates();
			
			if ( auto_mode ){
				
				PlatformMetaSearchMessenger.listPopularTemplates();
				
			}else{
		
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					if ( engine.isSelected()){
					
						long	id = engine.getId();
					}
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public MetaSearch 
	getMetaSearch() 
	{
		return( MetaSearchImpl.getSingleton());
	}
	
	public boolean
	isAutoMode()
	{
		return( COConfigurationManager.getBooleanParameter( "metasearch.auto.mode", true ));
	}
	
	public void
	setAutoMode(
		boolean		auto )
	{
		if ( auto != isAutoMode()){
		
			COConfigurationManager.setParameter( "metasearch.auto.mode", auto );
			
			refresh();
		}
	}
	
	public void 
	listPopularTemplates() 
	{
		try{
			PlatformMetaSearchMessenger.listPopularTemplates();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public static void 
	log(
		String 		s,
		Throwable 	e )
	{
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger( LOGGER_NAME );
		
		diag_logger.log( s );
		diag_logger.log( e );
		
		if ( Constants.DIAG_TO_STDOUT ){
			
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + s + ": " + Debug.getNestedExceptionMessage(e));
		}	
	}
	
	public static void 
	log(
		String 	s )
	{
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger( LOGGER_NAME );
		
		diag_logger.log( s );
		
		if ( Constants.DIAG_TO_STDOUT ){
			
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + s);
		}
	}
	

}
