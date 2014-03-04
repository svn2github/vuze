/*
 * Created on Sep 4, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.tag.impl;

import java.util.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagFeatureProperties;
import com.aelitis.azureus.core.tag.TagFeatureProperties.TagProperty;
import com.aelitis.azureus.core.tag.TagFeatureProperties.TagPropertyListener;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.TagTypeListener;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.TaggableLifecycleListener;

public class 
TagPropertyConstraintHandler 
	implements TagTypeListener
{
	private final AzureusCore		azureus_core;
	private final TagManagerImpl	tag_manager;
		
	private boolean		initialised;
	private boolean 	initial_assignment_complete;
	
	private Map<Tag,TagConstraint>	constrained_tags = new HashMap<Tag,TagConstraint>();
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( "tag:constraints" );
	
	private TimerEventPeriodic		timer;
	
	protected
	TagPropertyConstraintHandler(
		AzureusCore		_core,
		TagManagerImpl	_tm )
	{
		azureus_core	= _core;
		tag_manager		= _tm;
		
		tag_manager.addTaggableLifecycleListener(
			Taggable.TT_DOWNLOAD,
			new TaggableLifecycleListener()
			{
				public void
				initialised(
					List<Taggable>	current_taggables )
				{
					try{
						TagType tt = tag_manager.getTagType( TagType.TT_DOWNLOAD_MANUAL );
						
						tt.addTagTypeListener( TagPropertyConstraintHandler.this, true );

					}finally{
						
						AzureusCoreFactory.addCoreRunningListener(
							new AzureusCoreRunningListener()
							{	
								public void 
								azureusCoreRunning(
									AzureusCore core )
								{
									synchronized( constrained_tags ){
																				
										initialised = true;

										apply( core.getGlobalManager().getDownloadManagers(), true );
									}
								}
							});
					}
				}
				
				public void
				taggableCreated(
					Taggable		taggable )
				{
					apply((DownloadManager)taggable, null, false );
				}
				
				public void
				taggableDestroyed(
					Taggable		taggable )
				{
				}
			});
	}
	
	public void
	tagTypeChanged(
		TagType		tag_type )
	{
	}
	
	public void
	tagAdded(
		Tag			tag )
	{
		TagFeatureProperties tfp = (TagFeatureProperties)tag;
		
		TagProperty prop = tfp.getProperty( TagFeatureProperties.PR_CONSTRAINT );
		
		if ( prop != null ){
			
			prop.addListener(
				new TagPropertyListener() 
				{
					public void
					propertyChanged(
						TagProperty		property )
					{		
						handleProperty( property );
					}
					
					public void
					propertySync(
						TagProperty		property )
					{	
					}
				});
			
			handleProperty( prop );
		}
		
		tag.addTagListener(
			new TagListener() 
			{	
				public void 
				taggableSync(
					Tag tag ) 
				{
				}
				
				public void 
				taggableRemoved(
					Tag 		tag, 
					Taggable 	tagged ) 
				{
					apply((DownloadManager)tagged, tag, true );
				}
				
				public void 
				taggableAdded(
					Tag 		tag,
					Taggable 	tagged ) 
				{
					apply((DownloadManager)tagged, tag, true );
				}
			}, false );
	}
	
	public void
	tagChanged(
		Tag			tag )
	{
	}
	
	private void
	checkTimer()
	{
		if ( constrained_tags.size() > 0 ){
			
			if ( timer == null ){
				
				timer = 
					SimpleTimer.addPeriodicEvent(
						"tag:constraint:timer",
						30*1000,
						new TimerEventPerformer() {
							
							public void 
							perform(
								TimerEvent event) 
							{
								apply();
							}
						});
			}
			
		}else if ( timer != null ){
			
			timer.cancel();
			
			timer = null;
		}
	}
	
	public void
	tagRemoved(
		Tag			tag )
	{
		synchronized( constrained_tags ){
			
			if ( constrained_tags.containsKey( tag )){
				
				constrained_tags.remove( tag );
				
				checkTimer();
			}
		}
	}
	
	private void
	handleProperty(
		TagProperty		property )
	{
		Tag	tag = property.getTag();
				
		synchronized( constrained_tags ){
		
			String[] temp = property.getStringList();
			
			String constraint = temp == null || temp.length < 1?"":temp[0].trim();
						
			if ( constraint.length() == 0 ){
				
				if ( constrained_tags.containsKey( tag )){
					
					constrained_tags.remove( tag );
				}
			}else{
				
				TagConstraint con = constrained_tags.get( tag );
				
				if ( con != null && con.getConstraint().equals( constraint )){
					
					return;
				}
									
				Set<Taggable> existing = tag.getTagged();
					
				for ( Taggable e: existing ){
						
					tag.removeTaggable( e );
				}
			
				con = new TagConstraint( tag, constraint );
				
				constrained_tags.put( tag, con );
								
				if ( initialised ){
				
					apply( con );
				}
			}
			
			checkTimer();
		}
	}
	
	private void
	apply(
		final DownloadManager				dm,
		Tag									related_tag,
		boolean								auto )
	{
		if ( dm.isDestroyed()){
			
			return;
		}
		
		synchronized( constrained_tags ){
			
			if ( constrained_tags.size() == 0 || !initialised ){
				
				return;
			}
			
			if ( auto && !initial_assignment_complete ){
				
				return;
			}
		}
		
		System.out.println( "apply: " + dm.getDisplayName() + "/" + auto );
		
		dispatcher.dispatch(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					synchronized( constrained_tags ){
						
						for ( TagConstraint con: constrained_tags.values()){
							
							con.apply( dm );
						}
					}
				}
			});
	}
	
	private void
	apply(
		final List<DownloadManager>		dms,
		final boolean					initial_assignment )
	{
		synchronized( constrained_tags ){
			
			if ( constrained_tags.size() == 0 || !initialised ){
				
				return;
			}
		}
		
		dispatcher.dispatch(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					synchronized( constrained_tags ){
						
						for ( TagConstraint con: constrained_tags.values()){
							
							con.apply( dms );
						}
						
						if ( initial_assignment ){
						
							initial_assignment_complete = true;
						}
					}
				}
			});
	}
	
	private void
	apply(
		final TagConstraint		constraint )
	{
		synchronized( constrained_tags ){
			
			if ( !initialised ){
				
				return;
			}
		}
		
		dispatcher.dispatch(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					List<DownloadManager> dms = azureus_core.getGlobalManager().getDownloadManagers();

					constraint.apply( dms );
				}
			});
	}
	
	private void
	apply()
	{
		synchronized( constrained_tags ){
			
			if ( constrained_tags.size() == 0 || !initialised ){
				
				return;
			}
		}
		
		dispatcher.dispatch(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					List<DownloadManager> dms = azureus_core.getGlobalManager().getDownloadManagers();
					
					for ( TagConstraint con: constrained_tags.values()){
						
						con.apply( dms );
					}
				}
			});
	}
	
	private class
	TagConstraint
	{
		private Tag		tag;
		private String	constraint;
		
		private
		TagConstraint(
			Tag			_tag,
			String		_constraint )
		{
			tag			= _tag;
			constraint	= _constraint;
		}
		
		private Tag
		getTag()
		{
			return( tag );
		}
		
		private String
		getConstraint()
		{
			return( constraint );
		}
		
		private void
		apply(
			DownloadManager			dm )
		{
			if ( dm.isDestroyed() || !dm.isPersistent()){
				
				return;
			}

			Set<Taggable>	existing = tag.getTagged();
						
			if ( testConstraint( dm )){
				
				if ( !existing.contains( dm )){
					
					tag.addTaggable( dm );
				}
			}else{
				
				if ( existing.contains( dm )){
					
					tag.removeTaggable( dm );
				}
			}
		}
		
		private void
		apply(
			List<DownloadManager>	dms )
		{
			Set<Taggable>	existing = tag.getTagged();
			
			for ( DownloadManager dm: dms ){
			
				if ( dm.isDestroyed() || !dm.isPersistent()){
					
					continue;
				}
				
				if ( testConstraint( dm )){
					
					if ( !existing.contains( dm )){
						
						tag.addTaggable( dm );
					}
				}else{
					
					if ( existing.contains( dm )){
						
						tag.removeTaggable( dm );
					}
				}
			}
		}
		
		private boolean
		testConstraint(
			DownloadManager	dm )
		{
			List<Tag> dm_tags = tag_manager.getTagsForTaggable( dm );
			
			for ( Tag t: dm_tags  ){
				
				if ( t.getTagName( true ).equals( "derp" )){
					
					return( false );
				}
			}
			
			return( true );
		}
	}
}
