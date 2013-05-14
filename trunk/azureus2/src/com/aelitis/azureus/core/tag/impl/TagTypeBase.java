/*
 * Created on Mar 20, 2013
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

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;

import com.aelitis.azureus.core.tag.*;

public abstract class 
TagTypeBase
	implements TagType
{	
	protected static final String	AT_COLOR_ID			= "col.rgb";

	private int		tag_type;
	private int		tag_type_features;
	private String	tag_type_name;
		
	private static final int TTL_ADD 			= 1;
	private static final int TTL_CHANGE 		= 2;
	private static final int TTL_REMOVE 		= 3;
	private static final int TTL_TYPE_CHANGE 	= 4;
	
	private static TagManagerImpl manager = TagManagerImpl.getSingleton();
	
	private ListenerManager<TagTypeListener>	tt_listeners 	= 
		ListenerManager.createManager(
			"TagTypeListeners",
			new ListenerManagerDispatcher<TagTypeListener>()
			{
				public void
				dispatch(
					TagTypeListener		listener,
					int					type,
					Object				value )
				{					
					if ( type == TTL_ADD ){
						
						listener.tagAdded((Tag)value);
						
					}else if ( type == TTL_CHANGE ){
						
						listener.tagChanged((Tag)value);
						
					}else if ( type == TTL_REMOVE ){
						
						listener.tagRemoved((Tag)value);
						
					}else if ( type == TTL_TYPE_CHANGE ){
						
						listener.tagTypeChanged( TagTypeBase.this );
					}
				}
			});	
			
	protected
	TagTypeBase(
		int			_tag_type,
		int			_tag_features,
		String		_tag_name )
	{
		tag_type			= _tag_type;
		tag_type_features	= _tag_features;
		tag_type_name		= _tag_name;
	}
	
	protected void
	addTagType()
	{
		manager.addTagType( this );
	}
	
	public TagManagerImpl
	getTagManager()
	{
		return( manager );
	}
	
	protected Taggable
	resolveTaggable(
		String		id )
	{
		return( null );
	}
	
	protected void
	removeTaggable(
		TaggableResolver	resolver,
		Taggable			taggable )
	{	
	}
	
	public int
	getTagType()
	{
		return( tag_type );
	}
	
	public String
	getTagTypeName(
		boolean	localize )
	{
		if ( localize ){
			
			if ( tag_type_name.startsWith( "tag." )){
			
				return( MessageText.getString( tag_type_name ));
				
			}else{
				
				return( tag_type_name );
			}
		}else{
		
			if ( tag_type_name.startsWith( "tag." )){
			
				return( tag_type_name );
				
			}else{
				
				return( "!" + tag_type_name + "!" );
			}
		}
	}
	
	public boolean 
	isTagTypeAuto() 
	{
		return( true );
	}
	
	public boolean
	isTagTypePersistent()
	{
		return( false );
	}
	
	public long
	getTagTypeFeatures()
	{
		return( tag_type_features );
	}
	
	public boolean 
	hasTagTypeFeature(
		long feature ) 
	{
		return((tag_type_features&feature) != 0 );
	}
	
	protected void
	fireChanged()
	{
		tt_listeners.dispatch( TTL_TYPE_CHANGE, null );
	}
	
	public Tag 
	createTag(
		String 	name,
		boolean	auto_add )
	
		throws TagException 
	{
		throw( new TagException( "Not supported" ));
	}
	
	public void
	addTag(
		Tag	t )
	{
		tt_listeners.dispatch( TTL_ADD, t );
	}
	
	public void
	removeTag(
		Tag	t )
	{
		tt_listeners.dispatch( TTL_REMOVE, t );
		
		manager.removeConfig( t );
	}
	
	protected int[]
	getColorDefault()
	{
		return( null );
	}
	
	protected void
	sync()
	{
		List<Tag>	tags = getTags();
		
		for ( Tag t: tags ){
			
			((TagBase)t).sync();
		}
	}
	
	public Tag
	getTag(
		int	tag_id )
	{
		for ( Tag t: getTags()){
			
			if ( t.getTagID() == tag_id ){
				
				return( t );
			}
		}
		
		return( null );
	}
	
	public Tag
	getTag(
		String	tag_name,
		boolean	is_localized )
	{
		for ( Tag t: getTags()){
			
			if ( t.getTagName( is_localized ).equals( tag_name )){
				
				return( t );
			}
		}
		
		return( null );
	}
	
	public List<Tag>
	getTagsForTaggable(
		Taggable	taggable )
	{
		List<Tag>	result = new ArrayList<Tag>();
		
		for ( Tag t: getTags()){
			
			if ( t.hasTaggable( taggable )){
				
				result.add( t );
			}
		}
		
		return( result );
	}
	
	protected void
	fireChanged(
		Tag	t )
	{
		tt_listeners.dispatch( TTL_CHANGE, t );
	}
	
	public void
	removeTagType()
	{
		manager.removeTagType( this );
	}
	
	public void
	addTagTypeListener(
		TagTypeListener	listener,
		boolean			fire_for_existing )
	{
		tt_listeners.addListener( listener );
		
		if ( fire_for_existing ){
			
			for ( Tag t: getTags()){
				
				listener.tagAdded( t );
			}
		}
	}
	
	public void
	removeTagTypeListener(
		TagTypeListener	listener )
	{
		tt_listeners.removeListener( listener );
	}
	
	protected Boolean
	readBooleanAttribute(
		TagBase		tag,
		String		attr,
		Boolean		def )
	{
		return( manager.readBooleanAttribute( this, tag, attr, def ));
	}
	
	protected void
	writeBooleanAttribute(
		TagBase	tag,
		String	attr,
		boolean	value )
	{
		manager.writeBooleanAttribute( this, tag, attr, value );
	}
	
	protected long
	readLongAttribute(
		TagBase	tag,
		String	attr,
		long	def )
	{
		return( manager.readLongAttribute( this, tag, attr, def ));
	}
	
	protected void
	writeLongAttribute(
		TagBase	tag,
		String	attr,
		long	value )
	{
		manager.writeLongAttribute( this, tag, attr, value );
	}
	
	protected String
	readStringAttribute(
		TagBase	tag,
		String	attr,
		String	def )
	{
		return( manager.readStringAttribute( this, tag, attr, def ));
	}
	
	protected void
	writeStringAttribute(
		TagBase	tag,
		String	attr,
		String	value )
	{
		manager.writeStringAttribute( this, tag, attr, value );
	}
}
