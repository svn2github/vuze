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

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearch;
import com.aelitis.azureus.core.metasearch.MetaSearchManager;

public class 
MetaSearchManagerImpl
	implements MetaSearchManager
{
	private static final int REFRESH_MILLIS = 23*60*60*1000;
	
	private static final MetaSearchManager	singleton = new MetaSearchManagerImpl();
	
	public static MetaSearchManager
	getSingleton()
	{
		return( singleton );
	}
	
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
		new AEThread2( "MetaSearchRefresh", true ){
			public void
			run()
			{
				refreshSupport();
			}
		}.start();
	}
	
	protected void
	refreshSupport()
	{
		Engine[]	engines = getMetaSearch().getEngines();
		
		for (int i=0;i<engines.length;i++){
			
			Engine	engine = engines[i];
			
			long	id = engine.getId();
			
		}
	}
	
	public MetaSearch 
	getMetaSearch() 
	{
		return( MetaSearchImpl.getSingleton());
	}
	
	public void 
	listPopularTemplates() 
	{
		PlatformMetaSearchMessenger.listPopularTemplates();
	}
}
