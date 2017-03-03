/*
 * Created on Mar 21, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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
 */


package com.aelitis.azureus.core.tag.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.TagFeature;
import com.aelitis.azureus.core.tag.TagFeatureNotifications;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.TaggableResolver;
import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.core.util.CopyOnWriteSet;
import com.aelitis.azureus.util.MapUtils;

public abstract class 
TagWithState 
	extends TagBase
{
	private static final String TP_KEY = "TagWithState:tp_key";

	private final CopyOnWriteSet<Taggable>	objects = new CopyOnWriteSet<Taggable>( true );

	private final String	TP_KEY_TAG_ADDED_TIME;
	
	private TagFeatureNotifications	tag_notifications;

	private boolean	removed;
		
	public
	TagWithState(
		TagTypeBase			tt,
		int					tag_id,
		String				name )
	{
		super( tt, tag_id, name );		
		
		TP_KEY_TAG_ADDED_TIME = "ta:" + getTagUID();
		
		if ( tt.hasTagTypeFeature( TagFeature.TF_NOTIFICATIONS )){
			
			tag_notifications = (TagFeatureNotifications)this;
		}
	}
	
	protected
	TagWithState(
		TagTypeBase			tt,
		int					tag_id,
		Map					map )
	{
		super( tt, tag_id, MapUtils.getMapString( map, "n", "" ));
		
		TP_KEY_TAG_ADDED_TIME = "ta:" + getTagUID();

		if ( tt.hasTagTypeFeature( TagFeature.TF_NOTIFICATIONS )){
			
			tag_notifications = (TagFeatureNotifications)this;
		}
		
		if ( map != null ){
			
			List<byte[]> 	list 	= (List<byte[]>)map.get( "o" );
			List<Map> 		props 	= (List<Map>)map.get( "p" );
			
			if ( list != null ){
				
				int	pos = 0;
				
				for ( byte[] b: list ){
					
					try{
						String id = new String( b, "UTF-8" );
						
						Taggable taggable = tt.resolveTaggable( id );
						
						if ( taggable != null ){
														
							if ( props != null ){
								
								Long time_added = (Long)props.get(pos).get("a");
								
								if ( time_added != null ){
									
									synchronized( TP_KEY ){
										
										Map all_props = (Map)taggable.getTaggableTransientProperty( TP_KEY );
										
										if ( all_props == null ){
											
											all_props = new HashMap();
										}
										
										all_props.put( TP_KEY_TAG_ADDED_TIME, time_added );
										
										taggable.setTaggableTransientProperty( TP_KEY, all_props );
									}
								}
							}
							
							objects.add( taggable );
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
					
					pos++;
				}
			}
		}
	}
	
	protected void
	exportDetails(
		Map			map,
		boolean		do_contents )
	{
		MapUtils.setMapString( map, "n", getTagNameRaw());
		
		if ( do_contents ){
			
			Iterator<Taggable> it = objects.iterator();
			
			List<byte[]> 	l = new ArrayList<byte[]>( objects.size());
			List<Map> 		p = new ArrayList<Map>( objects.size());
			
			while( it.hasNext()){
				
				try{
					Taggable taggable = it.next();
					
					String id = taggable.getTaggableID();
					
					if ( id != null ){
					
						l.add( id.getBytes( "UTF-8" ));
						
						Map all_props = (Map)taggable.getTaggableTransientProperty( TP_KEY );
						
						Map props = new HashMap();
						
						if ( all_props != null ){
							
							Long time_added = (Long)all_props.get( TP_KEY_TAG_ADDED_TIME );
							
							if ( time_added != null ){
								
								props.put( "a", time_added );
							}
						}
						
						p.add( props );
						
					}else{
						
						// Get this when the taggable is a download that has lost its torrent
						// Debug.out( "No taggable ID for " + taggable );
					}
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			map.put( "o", l );
			map.put( "p", p );
		}
	}
	
	public void 
	setTagName(
		String name )
	
		throws TagException 
	{
		super.setTagName( name );
		
		getManager().tagChanged( this );
	}
	
	@Override
	public long 
	getTaggableAddedTime(
		Taggable taggble ) 
	{
		Map all_props = (Map)taggble.getTaggableTransientProperty( TP_KEY );
		
		if ( all_props != null ){
			
			Long added_time = (Long)all_props.get( TP_KEY_TAG_ADDED_TIME );
			
			if ( added_time != null ){
				
				return( added_time*1000 );
			}
		}
		
		return( -1 );
	}
	
	public void
	addTaggable(
		Taggable	t )
	{
		if ( removed ){
			
			Debug.out( "Tag has been removed" );
			
			return;
		}
		
		boolean added = objects.add( t );
		
		if ( added ){
			
			if ( getTagType().isTagTypePersistent()){
				
					// do this before calling super.addTaggable so that the addition time is
					// available to any actions that result from it
				
				synchronized( TP_KEY ){
					
					Map all_props = (Map)t.getTaggableTransientProperty( TP_KEY );
					
					if ( all_props == null ){
						
						all_props = new HashMap();
					}
					
					all_props.put( TP_KEY_TAG_ADDED_TIME, SystemTime.getCurrentTime()/1000);
				
					t.setTaggableTransientProperty( TP_KEY, all_props );
				}
			}
		}
		
		super.addTaggable( t );

		if ( added ){
				
			getManager().tagContentsChanged( this );
			
			if ( tag_notifications != null ){
				
				checkNotifications( t, true );
			}
		}
	}
	
	public void
	removeTaggable(
		Taggable	t )
	{
		boolean removed = objects.remove( t );
		
		super.removeTaggable( t );
		
		if ( removed ){
		
			getManager().tagContentsChanged( this );
			
			if ( tag_notifications != null ){
				
				checkNotifications( t, false );
			}
		}
	}
	
	protected void
	checkNotifications(
		Taggable		taggable,
		boolean			is_add )
	{
		int flags = getPostingNotifications();
		
		if ( flags != 0 ){
			
			boolean	add = ( flags & TagFeatureNotifications.NOTIFY_ON_ADD ) != 0;
			boolean	rem = ( flags & TagFeatureNotifications.NOTIFY_ON_REMOVE ) != 0;
			
			if ( add == is_add || rem == !is_add ){
				
				AZ3Functions.provider provider = AZ3Functions.getProvider();
				
				if ( provider != null ){
					
					String name;
					
					TaggableResolver resolver = taggable.getTaggableResolver();
					
					if ( resolver != null ){
						
						name = resolver.getDisplayName( taggable );
						
					}else{
						
						name = taggable.toString();
					}
					
					name = MessageText.getString(
								is_add?"tag.notification.added":"tag.notification.removed",
								new String[]{
									name,
									getTagName( true ),
								});
					
					Map<String,String>	cb_data = new HashMap<String, String>();
					
					cb_data.put( "allowReAdd", "true" );
					cb_data.put( "taguid", String.valueOf( getTagUID() ));
					cb_data.put( "id", String.valueOf( taggable.getTaggableID()));
					
					String icon_id = "image.sidebar.tag-green";
					
					int[] color = getColor();
					
					if ( color != null && color.length == 3 ){
						
						long rgb = (color[0]<<16)|(color[1]<<8)|color[2];
						
						String hex = Long.toHexString( rgb );
						
						while( hex.length() < 6 ){
							
							hex = "0"+ hex;
						}
						
						icon_id += "#" + hex;
					}
					
					provider.addLocalActivity(
						getTagUID() + ":" + taggable.getTaggableID() + ":" + is_add,
						icon_id,
						name,
						new String[]{ MessageText.getString( "label.view" )},
						ActivityCallback.class,
						cb_data );
				}
			}
		}
	}
	
	public static class
	ActivityCallback
		implements AZ3Functions.provider.LocalActivityCallback
	{
		public void 
		actionSelected(
			String action, Map<String, String> data) 
		{
			String	taguid 	= data.get( "taguid" );
			
			final String	id 		= data.get( "id" );
			
			if ( taguid != null && id != null ){
				
				try{
					Tag tag = TagManagerFactory.getTagManager().lookupTagByUID( Long.parseLong( taguid ));
					
					if ( tag != null ){
						
						TagType tt = tag.getTagType();
						
						if ( tt instanceof TagTypeWithState ){
							
							final TaggableResolver resolver = ((TagTypeWithState)tt).getResolver();
							
							if ( resolver != null ){
								
								if ( !tag.isVisible()){
									
									tag.setVisible( true );
								}
								
								tag.requestAttention();
								
									// things are somewhat async - too much effort to try and add some callback
									// structure to ensure tag is vsible before showing download...
								
								SimpleTimer.addEvent(
									"async",
									SystemTime.getOffsetTime( 500 ),
									new TimerEventPerformer() {
										
										@Override
										public void perform(TimerEvent event) {
											resolver.requestAttention( id );
										}
									});
							
							}
						}
					}
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
	}
	
	@Override
	public void 
	removeTag() 
	{	
		super.removeTag();
		
		removed = true;
	}
	
	protected boolean
	isRemoved()
	{
		return( removed );
	}
	
	public int 
	getTaggedCount() 
	{
		return( objects.size());
	}
	
	public boolean 
	hasTaggable(
		Taggable	t )
	{
		return( objects.contains( t ));
	}
	
	public Set<Taggable>
	getTagged()
	{			
		return(objects.getSet());
	}
}
