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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.util.CopyOnWriteSet;
import com.aelitis.azureus.util.MapUtils;

public abstract class 
TagWithState 
	extends TagBase
{
	private CopyOnWriteSet<Taggable>	objects = new CopyOnWriteSet<Taggable>();
	
	private boolean	removed;
	
	public
	TagWithState(
		TagTypeBase			tt,
		int					tag_id,
		String				name )
	{
		super( tt, tag_id, name );		
	}
	
	protected
	TagWithState(
		TagTypeBase			tt,
		int					tag_id,
		Map					map )
	{
		super( tt, tag_id, MapUtils.getMapString( map, "n", "" ));
				
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
			
			Iterator<Taggable> it = objects.iterator();
			
			List<byte[]> l = new ArrayList<byte[]>( objects.size());
			
			while( it.hasNext()){
				
				try{
					l.add( it.next().getTaggableID().getBytes( "UTF-8" ));
					
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
	
	public Set<Taggable>
	getTagged()
	{			
		return(objects.getSet());
	}
}
