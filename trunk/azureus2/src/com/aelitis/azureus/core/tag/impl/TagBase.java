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

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.Taggable;

public abstract class 
TagBase
	implements Tag
{
	protected static final String	AT_RATELIMIT_UP		= "rl.up";
	protected static final String	AT_RATELIMIT_DOWN	= "rl.down";
	protected static final String	AT_VISIBLE			= "vis";
	  
	private TagTypeBase	tag_type;
	
	private int			tag_id;
	private String		tag_name;
	
	private static final int TL_ADD 	= 1;
	private static final int TL_REMOVE 	= 2;
	
	private ListenerManager<TagListener>	t_listeners 	= 
		ListenerManager.createManager(
			"TagListeners",
			new ListenerManagerDispatcher<TagListener>()
			{
				public void
				dispatch(
					TagListener			listener,
					int					type,
					Object				value )
				{					
					if ( type == TL_ADD ){
						
						listener.tagabbleAdded(TagBase.this,(Taggable)value);
						
					}else if ( type == TL_REMOVE ){
						
						listener.tagabbleRemoved(TagBase.this,(Taggable)value);
					}
				}
			});	
		
	private boolean	is_visible;
	
	protected
	TagBase(
		TagTypeBase			_tag_type,
		int					_tag_id,
		String				_tag_name )
	{
		tag_type		= _tag_type;
		tag_id			= _tag_id;
		tag_name		= _tag_name;
		
		is_visible = readBooleanAttribute( AT_VISIBLE, getVisibleDefault());
		
		tag_type.addTag( this );
	}
	
	protected TagManagerImpl
	getManager()
	{
		return( tag_type.getManager());
	}
	
	public TagTypeBase
	getTagType()
	{
		return( tag_type );
	}
	
	public int
	getTagID()
	{
		return( tag_id );
	}
	
	protected String
	getTagNameRaw()
	{
		return( tag_name );
	}
	
	public String
	getTagName(
		boolean		localize )
	{
		if ( localize ){
			
			if ( tag_name.startsWith( "tag." )){
			
				return( MessageText.getString( tag_name ));
				
			}else{
				
				return( tag_name );
			}
		}else{
		
			if ( tag_name.startsWith( "tag." )){
			
				return( tag_name );
				
			}else{
				
				return( "!" + tag_name + "!" );
			}
		}
	}
	
	public void 
	setTagName(
		String name )
	
		throws TagException 
	{
		if ( getTagType().isTagTypeAuto()){
			
			throw( new TagException( "Not supported" ));
		}
		
		tag_name = name;
				
		tag_type.fireChanged( this );
	}
	
	protected boolean
	getVisibleDefault()
	{
		return( true );
	}
	
	public boolean
	isVisible()
	{
		return( is_visible );
	}
	
	public void
	setVisible(
		boolean	v )
	{
		if ( v != is_visible ){
			
			is_visible	= v;
			
			writeBooleanAttribute( AT_VISIBLE, v );
			
			tag_type.fireChanged( this );
		}
	}
	
	public void
	addTaggable(
		Taggable	t )
	{
		t_listeners.dispatch( TL_ADD, t );
		
		tag_type.fireChanged( this );
	}
	
	public void
	removeTaggable(
		Taggable	t )
	{
		t_listeners.dispatch( TL_REMOVE, t );
		
		tag_type.fireChanged( this );
	}
	
	public int 
	getTaggedCount() 
	{
		return( getTagged().size());
	}
		
	public boolean 
	hasTaggable(
		Taggable	t )
	{
		return( getTagged().contains( t ));
	}
	
	public void
	removeTag()
	{
		tag_type.removeTag( this );
	}
	
	public void
	addTagListener(
		TagListener	listener,
		boolean		fire_for_existing )
	{
		t_listeners.addListener( listener );
		
		if ( fire_for_existing ){
			
			for ( Taggable t: getTagged()){
				
				listener.tagabbleAdded( this, t );
			}
		}
	}
	
	public void
	removeTagListener(
		TagListener	listener )
	{
		t_listeners.removeListener( listener );
	}
	
	protected boolean
	readBooleanAttribute(
		String	attr,
		boolean	def )
	{
		return( tag_type.readBooleanAttribute( this, attr, def ));
	}
	
	protected void
	writeBooleanAttribute(
		String	attr,
		boolean	value )
	{
		tag_type.writeBooleanAttribute( this, attr, value );
	}
	
	protected long
	readLongAttribute(
		String	attr,
		long	def )
	{
		return( tag_type.readLongAttribute( this, attr, def ));
	}
	
	protected void
	writeLongAttribute(
		String	attr,
		long	value )
	{
		tag_type.writeLongAttribute( this, attr, value );
	}
}
