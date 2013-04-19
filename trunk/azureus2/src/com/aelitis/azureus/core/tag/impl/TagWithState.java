/*
 * Created on Mar 21, 2013
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
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.util.MapUtils;

public abstract class 
TagWithState 
	extends TagBase
{
	private CopyOnWriteList<Taggable>	objects = new CopyOnWriteList<Taggable>();
	
	private boolean	removed;
	
	public
	TagWithState(
		TagTypeBase			tt,
		int					tag_id,
		String				name,
		boolean				auto_add )
	{
		super( tt, tag_id, name, auto_add );		
	}
	
	protected
	TagWithState(
		TagTypeBase			tt,
		int					tag_id,
		Map					map )
	{
		super( tt, tag_id, MapUtils.getMapString( map, "n", "" ), true );
		
		if ( map != null ){
			
			List<byte[]> list = (List<byte[]>)map.get( "o" );
			
			if ( list != null ){
				
				for ( byte[] b: list ){
					
					try{
						String id = new String( b, "UTF-8" );
						
						Taggable taggable = tt.resolveTaggable( id );
						
						if ( taggable != null ){
							
							objects.add( taggable );
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
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
			
			List<Taggable> o = objects.getList();
			
			List l = new ArrayList( o.size());
			
			for ( Taggable t: o ){
				
				try{
					l.add( t.getTaggableID().getBytes( "UTF-8" ));
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			map.put( "o", l );
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
	
	public void
	addTaggable(
		Taggable	t )
	{
		if ( removed ){
			
			Debug.out( "Tag has been removed" );
			
			return;
		}
		
		objects.add( t );
		
		super.addTaggable( t );
		
		getManager().tagContentsChanged( this );
	}
	
	public void
	removeTaggable(
		Taggable	t )
	{
		boolean removed = objects.remove( t );
		
		super.removeTaggable( t );
		
		if ( removed ){
		
			getManager().tagContentsChanged( this );
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
	
	public List<Taggable>
	getTagged()
	{
		return( objects.getList());
	}
}
