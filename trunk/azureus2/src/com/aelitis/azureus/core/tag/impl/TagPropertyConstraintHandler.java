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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
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
	
	private Map<Tag,TagConstraint>	constrained_tags 	= new HashMap<Tag,TagConstraint>();
	
	private Map<Tag,Map<DownloadManager,Long>>			apply_history 		= new HashMap<Tag, Map<DownloadManager,Long>>();
	
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
								apply_history.clear();
								
								apply();
							}
						});
			}
			
		}else if ( timer != null ){
			
			timer.cancel();
			
			timer = null;
			
			apply_history.clear();
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
				
		dispatcher.dispatch(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					List<TagConstraint>	cons;
					
					synchronized( constrained_tags ){
					
						cons = new ArrayList<TagConstraint>( constrained_tags.values());
					}
					
					for ( TagConstraint con: cons ){
							
						con.apply( dm );
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
					List<TagConstraint>	cons;
					
					synchronized( constrained_tags ){
					
						cons = new ArrayList<TagConstraint>( constrained_tags.values());
					}
						
						// set up initial constraint tagged state without following implications
					
					for ( TagConstraint con: cons ){
						
						con.apply( dms );
					}
						
					if ( initial_assignment ){
						
						synchronized( constrained_tags ){
						
							initial_assignment_complete = true;
						}
					
							// go over them one more time to pick up consequential constraints
						
						for ( TagConstraint con: cons ){
							
							con.apply( dms );
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
					
					List<TagConstraint>	cons;
					
					synchronized( constrained_tags ){
					
						cons = new ArrayList<TagConstraint>( constrained_tags.values());
					}
					
					for ( TagConstraint con: cons ){
						
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
		
		private ConstraintExpr	expr;
		
		private
		TagConstraint(
			Tag			_tag,
			String		_constraint )
		{
			tag			= _tag;
			constraint	= _constraint;
		
			try{
				expr = compile( constraint );
				
			}catch( Throwable e ){
				
				Debug.out( "Invalid constraint: " + constraint + " - " + Debug.getNestedExceptionMessage( e ));
			}
		}
		
		private ConstraintExpr
		compile(
			String	str )
		{
			if ( str.contains( "||" )){
				
				String[] bits = str.split( "\\|\\|" );
				
				return( new ConstraintExprOr( compile( bits )));
				
			}else if ( str.contains( "&&" )){
				
				String[] bits = str.split( "&&" );
				
				return( new ConstraintExprAnd( compile( bits )));
				
			}else if ( str.startsWith( "!" )){
				
				return( new ConstraintExprNot( compile( str.substring(1).trim())));
				
			}else{
				
				if ( str.startsWith( "hasTag(" ) || !str.endsWith( ")" )){
					
					String temp = str.substring( 7, str.length() - 1 ).trim();
					
					if ( temp.startsWith( "\"" ) && temp.endsWith( "\"" )){
						
						String tag_name = temp.substring(1, temp.length() - 1 );
						
						return( new ConstraintExprHasTag( tag_name ));
						
					}else{
						
						throw( new RuntimeException( "Expected string literal" ));
					}
				}else{
					
					throw( new RuntimeException( "Unsupported construct: " + str ));
				}
			}
		}
		
		private ConstraintExpr[]
		compile(
			String[]	bits )
		{
			ConstraintExpr[] res = new ConstraintExpr[ bits.length ];
			
			for ( int i=0; i<bits.length;i++){
				
				res[i] = compile( bits[i].trim());
			}
			
			return( res );
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

			if ( expr == null ){
				
				return;
			}
			
			Set<Taggable>	existing = tag.getTagged();
						
			if ( testConstraint( dm )){
				
				if ( !existing.contains( dm )){
					
					if( canAddTaggable( dm )){
					
						tag.addTaggable( dm );
					}
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
			if ( expr == null ){
				
				return;
			}

			Set<Taggable>	existing = tag.getTagged();
			
			for ( DownloadManager dm: dms ){
			
				if ( dm.isDestroyed() || !dm.isPersistent()){
					
					continue;
				}
				
				if ( testConstraint( dm )){
					
					if ( !existing.contains( dm )){
						
						if ( canAddTaggable( dm )){
						
							tag.addTaggable( dm );
						}
					}
				}else{
					
					if ( existing.contains( dm )){
						
						tag.removeTaggable( dm );
					}
				}
			}
		}
		
		private boolean
		canAddTaggable(
			DownloadManager		dm )
		{
			long	now = SystemTime.getMonotonousTime();
				
			Map<DownloadManager,Long> recent_dms = apply_history.get( tag );
				
			if ( recent_dms != null ){
					
				Long time = recent_dms.get( dm );
					
				if ( time != null && now - time < 1000 ){
					
					System.out.println( "Not applying constraint as too recently actioned: " + dm.getDisplayName() + "/" + tag.getTagName( true ));

					return( false );
				}
				
				if ( recent_dms == null ){
					
					recent_dms = new HashMap<DownloadManager,Long>();
					
					apply_history.put( tag, recent_dms );
				}
				
				recent_dms.put( dm, now );
			}
			
			return( true );
		}
		
		private boolean
		testConstraint(
			DownloadManager	dm )
		{
			List<Tag> dm_tags = tag_manager.getTagsForTaggable( dm );
			
			return( expr.eval( dm_tags ));
		}
	}
	
	private interface
	ConstraintExpr
	{
		public boolean
		eval(
			List<Tag>		tags );
	}
	
	private class
	ConstraintExprNot
		implements  ConstraintExpr
	{
		private	ConstraintExpr expr;
		
		private
		ConstraintExprNot(
			ConstraintExpr	e )
		{
			expr = e;
		}
		
		public boolean
		eval(
			List<Tag>		tags )
		{
			return( !expr.eval( tags ));
		}
	}
	
	private class
	ConstraintExprOr
		implements  ConstraintExpr
	{
		private ConstraintExpr[]	exprs;
		
		private
		ConstraintExprOr(
			ConstraintExpr[]	_exprs )
		{
			exprs = _exprs;
		}
		
		public boolean
		eval(
			List<Tag>		tags )
		{
			for ( ConstraintExpr expr: exprs ){
				
				if ( expr.eval( tags )){
					
					return( true );
				}
			}
			
			return( false );
		}
	}
	
	private class
	ConstraintExprAnd
		implements  ConstraintExpr
	{
		private ConstraintExpr[]	exprs;
		
		private
		ConstraintExprAnd(
			ConstraintExpr[]	_exprs )
		{
			exprs = _exprs;
		}
		
		public boolean
		eval(
			List<Tag>		tags )
		{
			for ( ConstraintExpr expr: exprs ){
				
				if ( !expr.eval( tags )){
					
					return( false );
				}
			}
			
			return( true );
		}
	}
	
	private class
	ConstraintExprHasTag
		implements  ConstraintExpr
	{
		private	String tag_name;
		
		private
		ConstraintExprHasTag(
			String n )
		{
			tag_name = n;
		}
		
		public boolean
		eval(
			List<Tag>		tags )
		{
			for ( Tag t: tags ){
				
				if ( t.getTagName( true ).equals( tag_name )){
					
					return( true );
				}
			}
			
			return( false );
		}
	}
}
