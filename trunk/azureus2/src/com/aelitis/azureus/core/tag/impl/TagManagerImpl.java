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
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagManagerListener;
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
	
		// order is important as 'increases' in effects (see applyConfigUpdates)
	
	private static final int CU_TAG_CREATE		= 1;
	private static final int CU_TAG_CHANGE		= 2;
	private static final int CU_TAG_CONTENTS	= 3;
	private static final int CU_TAG_REMOVE		= 4;
	
	private static TagManagerImpl	singleton;
	
	public static synchronized TagManagerImpl
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new TagManagerImpl();
			
			singleton.init();
		}
		
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
					try{
							// just in case there's a bunch of changes coming in together
						
						Thread.sleep( 1000 );
						
					}catch( Throwable e ){
						
					}
					
					writeConfig();
				}
			},
			30*1000 );
	
	
	private Map					config;
	private WeakReference<Map>	config_ref;
	
	private boolean				config_dirty;
	
	private List<Object[]>		config_change_queue = new ArrayList<Object[]>();
	
	
	private CopyOnWriteList<TagManagerListener>		listeners = new CopyOnWriteList<TagManagerListener>();
	
	
	
	private
	TagManagerImpl()
	{
	}
	
	private void
	init()
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
	
	private void
	resolverInitialized(
		TaggableResolver		resolver )
	{
		TagTypeDownloadManual ttdm = new TagTypeDownloadManual( resolver );
		
		synchronized( this ){
			
			Map config = getConfig();
			
			Map<String,Object> tt = (Map<String,Object>)config.get( String.valueOf( ttdm.getTagType()));
			
			if ( tt != null ){
				
				for ( Map.Entry<String,Object> entry: tt.entrySet()){
					
					String key = entry.getKey();
					
					try{
						if ( Character.isDigit( key.charAt(0))){
						
							int	tag_id 	= Integer.parseInt( key );
							Map m		= (Map)entry.getValue();
							
							ttdm.createTag( tag_id, m );
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
	}
	
	private void
	removeTaggable(
		TaggableResolver	resolver,
		Taggable			taggable )
	{
		for ( TagType	tt: tag_types ){
			
			TagTypeBase	ttb = (TagTypeBase)tt;
			
			ttb.removeTaggable( resolver, taggable );
		}
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
		
		removeConfig( tag_type );
	}
	
	public List<TagType>
	getTagTypes()
	{
		return( tag_types.getList());
	}
	
	public TaggableLifecycleHandler
	registerTaggableResolver(
		final TaggableResolver	resolver )
	{
		return(
			new TaggableLifecycleHandler()
			{
				public void 
				initialized() 
				{				
					resolverInitialized( resolver );
				}
				
				public void
				taggableCreated(
					Taggable	t )
				{	
					// could do some initial tag allocations here
				}
				
				public void
				taggableDestroyed(
					Taggable	t )
				{
					removeTaggable( resolver, t );
				}
			});
	}
	
	public void
	addTagManagerListener(
		TagManagerListener		listener,
		boolean					fire_for_existing )
	{
		listeners.add( listener );
		
		if ( fire_for_existing ){
						
			for (TagType tt: tag_types ){
				
				listener.tagTypeAdded( this, tt );
			}
		}
	}
	
	public void
	removeTagManagerListener(
		TagManagerListener		listener )
	{
		listeners.remove( listener );
	}
	
	protected void
	tagCreated(
		TagWithState	tag )
	{
		addConfigUpdate( CU_TAG_CREATE, tag );
	}
	
	protected void
	tagChanged(
		TagWithState	tag )
	{
		addConfigUpdate( CU_TAG_CHANGE, tag );

	}
	
	protected void
	tagRemoved(
		TagWithState	tag )
	{
		addConfigUpdate( CU_TAG_REMOVE, tag );
	}
	
	protected void
	tagContentsChanged(
		TagWithState	tag )
	{
		addConfigUpdate( CU_TAG_CONTENTS, tag );
	}
	
	private void
	addConfigUpdate(
		int				type,
		TagWithState	tag )
	{
		if ( !tag.getTagType().isTagTypePersistent()){
			
			return;
		}
		
		if ( tag.isRemoved() && type != CU_TAG_REMOVE ){
			
			return;
		}
		
		synchronized( this ){
			
			config_change_queue.add( new Object[]{ type, tag });
		}
		    
		setDirty();
	}
	
	private void
	applyConfigUpdates(
		Map			config )
	{
		Map<TagWithState,Integer>	updates = new HashMap<TagWithState, Integer>();
		
		for ( Object[] update: config_change_queue ){
			
			int				type	= (Integer)update[0];
			TagWithState	tag 	= (TagWithState)update[1];
			
			if ( tag.isRemoved()){
				
				type = CU_TAG_REMOVE;
			}
			
			Integer existing = updates.get( tag );
			
			if ( existing == null ){
				
				updates.put( tag, type );
				
			}else{
				
				if ( existing == CU_TAG_REMOVE ){
					
				}else if ( type > existing ){
					
					updates.put( tag, type );			
				}
			}
		}
		
		for ( Map.Entry<TagWithState,Integer> entry: updates.entrySet()){
			
			TagWithState 	tag = entry.getKey();
			int				type	= entry.getValue();
			
			TagType	tag_type = tag.getTagType();
			
			String tt_key = String.valueOf( tag_type.getTagType());
			
			Map tt = (Map)config.get( tt_key );
			
			if ( tt == null ){
				
				if ( type == CU_TAG_REMOVE ){
					
					continue;
				}
				
				tt = new HashMap();
					
				config.put( tt_key, tt );
			}
			
			String t_key = String.valueOf( tag.getTagID());
			
			if ( type == CU_TAG_REMOVE ){
				
				tt.remove( t_key );
				
				continue;
			}
			
			Map t = (Map)tt.get( t_key );
			
			if ( t == null ){
				
				t = new HashMap();
				
				tt.put( t_key, t );
			}
			
			tag.exportDetails( t, type == CU_TAG_CONTENTS );
		}

		config_change_queue.clear();
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
			
			if ( config_change_queue.size() > 0 ){
			
				applyConfigUpdates( getConfig());
			}
			
			if ( config != null ){
							
				FileUtil.writeResilientConfigFile( CONFIG_FILE, config );
				
				config_ref = new WeakReference<Map>( config );
				
				config = null;
			}
		}
	}
	
	protected boolean
	readBooleanAttribute(
		TagTypeBase	tag_type,
		TagBase		tag,
		String		attr,
		boolean		def )
	{
		return( readLongAttribute(tag_type, tag, attr, def?1:0 ) == 1 );
	}
	
	protected void
	writeBooleanAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		boolean			value )
	{
		writeLongAttribute( tag_type, tag, attr, value?1:0 );
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
	
	protected void
	removeConfig(
		TagType	tag_type )
	{
		synchronized( this ){
			
			Map m = getConfig();
			
			String tt_key = String.valueOf( tag_type.getTagType());
			
			Map tt = (Map)m.remove( tt_key );

			if ( tt != null ){
				
				setDirty();
			}
		}
	}
	
	protected void
	removeConfig(
		Tag	tag )
	{
		TagType	tag_type = tag.getTagType();
		
		synchronized( this ){
			
			Map m = getConfig();
			
			String tt_key = String.valueOf( tag_type.getTagType());
			
			Map tt = (Map)m.get( tt_key );

			if ( tt == null ){
				
				return;
			}
			
			String t_key = String.valueOf( tag.getTagID());
			
			Map t = (Map)tt.remove( t_key );
			
			if ( t != null ){
				
				setDirty();
			}
		}
	}
}
