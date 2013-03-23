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

import java.lang.ref.WeakReference;
import java.util.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.FrequencyLimitedDispatcher;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.TaggableLifecycleHandler;
import com.aelitis.azureus.core.tag.TaggableResolver;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.util.MapUtils;

public class 
TagManagerImpl
	implements TagManager
{
	private static final String	CONFIG_FILE 				= "tag.config";

	private static TagManagerImpl	singleton = new TagManagerImpl();
	
	public static TagManagerImpl
	getSingleton()
	{
		return( singleton );
	}
	
	private CopyOnWriteList<TagType>	tag_types = new CopyOnWriteList<TagType>();
	
	private Map<Integer,TagType>	tag_type_map = new HashMap<Integer, TagType>();
	
	
	private FrequencyLimitedDispatcher dirty_dispatcher = 
		new FrequencyLimitedDispatcher(
			new AERunnable()
			{
				public void
				runSupport()
				{
					writeConfig();
				}
			},
			30*1000 );
	
	
	private Map					config;
	private WeakReference<Map>	config_ref;
	
	private boolean				config_dirty;
	
	
	
	
	private
	TagManagerImpl()
	{
		AzureusCoreFactory.getSingleton().addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				stopped(
					AzureusCore		core )
				{
					destroy();
				}
			});
	}
	
	public void
	addTagType(
		TagType		tag_type )
	{
		synchronized( tag_type_map ){
			
			if ( tag_type_map.put( tag_type.getTagType(), tag_type) != null ){
				
				Debug.out( "Duplicate tag type!" );
			}
		}
		
		tag_types.add( tag_type );
	}
	
	public TagType 
	getTagType(
		int 	tag_type) 
	{
		synchronized( tag_type_map ){

			return( tag_type_map.get( tag_type ));
		}
	}
	
	protected void
	removeTagType(
		TagType		tag_type )
	{
		tag_types.remove( tag_type );
	}
	
	public List<TagType>
	getTagTypes()
	{
		return( tag_types.getList());
	}
	
	public TaggableLifecycleHandler
	registerTaggableResolver(
		TaggableResolver	resolver )
	{
		return(
			new TaggableLifecycleHandler()
			{
				public void
				taggableCreated(
					Taggable	t )
				{	
				}
				
				public void
				taggableDestroyed(
					Taggable	t )
				{
				}
			});
	}
	
	private void
	destroy()
	{
		writeConfig();
	}
	
	private void
	setDirty()
	{
		synchronized( this ){
			
			if ( !config_dirty ){
		
				config_dirty = true;
	
				dirty_dispatcher.dispatch();
			}
		}
	}
	
	private Map
	readConfig()
	{
		Map map;
		
		if ( FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
		}else{
			
			map = new HashMap();
		}
		
		return( map );
	}
	
	private Map
	getConfig()
	{
		synchronized( this ){
			
			if ( config != null ){
				
				return( config );
			}
			
			if ( config_ref != null ){
					
				config = config_ref.get();
				
				if ( config != null ){
					
					return( config );
				}
			}
							
			config = readConfig();
			
			return( config );
		}
	}
	
	private void
	writeConfig()
	{
		synchronized( this ){
			
			if ( !config_dirty ){
				
				return;
			}
	
			config_dirty = false;
			
			if ( config != null ){
							
				FileUtil.writeResilientConfigFile( CONFIG_FILE, config );
				
				config_ref = new WeakReference<Map>( config );
				
				config = null;
			}
		}
	}
	
	protected long
	readLongAttribute(
		TagTypeBase	tag_type,
		TagBase		tag,
		String		attr,
		long		def )
	{
		try{
			synchronized( this ){
				
				Map m = getConfig();
				
				Map tt = (Map)m.get( String.valueOf( tag_type.getTagType()));
				
				if ( tt == null ){
					
					return( def );
				}
				
				Map t = (Map)tt.get( String.valueOf( tag.getTagID()));
				
				if ( t == null ){
					
					return( def );
				}
				
				Map conf = (Map)t.get( "c" );
				
				if ( conf == null ){
					
					return( def );
				}
				
				return( MapUtils.getMapLong( conf, attr, def ));
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( def );
		}
	}
	
	protected void
	writeLongAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		long			value )
	{
		try{
			synchronized( this ){
				
				Map m = getConfig();
				
				String tt_key = String.valueOf( tag_type.getTagType());
				
				Map tt = (Map)m.get( tt_key );
				
				if ( tt == null ){
					
					tt = new HashMap();
					
					m.put( tt_key, tt );
				}
				
				String t_key = String.valueOf( tag.getTagID());
				
				Map t = (Map)tt.get( t_key );
				
				if ( t == null ){
					
					t = new HashMap();
					
					tt.put( t_key, t );
				}
				
				Map conf = (Map)t.get( "c" );
				
				if ( conf == null ){
					
					conf= new HashMap();
					
					t.put( "c", conf );
				}
				
				long old = MapUtils.getMapLong( conf, attr, 0 );
				
				if ( old == value && conf.containsKey( attr )){
					
					return;
				}
				
				conf.put( attr, value );
				
				setDirty();
			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}	
}
