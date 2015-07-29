/*
 * Created on Sep 4, 2013
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagFeatureProperties;
import com.aelitis.azureus.core.tag.TagFeatureProperties.TagProperty;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.TagTypeAdapter;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.TaggableLifecycleAdapter;
import com.aelitis.azureus.core.tag.TaggableLifecycleListener;

public class 
TagPropertyTrackerHandler 
	implements TagFeatureProperties.TagPropertyListener
{
	private AzureusCore		azureus_core;
	private TagManagerImpl	tag_manager;
	
	private Map<String,List<Tag>>	tracker_host_map = new HashMap<String,List<Tag>>();
	
	protected
	TagPropertyTrackerHandler(
		AzureusCore		_core,
		TagManagerImpl	_tm )
	{
		azureus_core	= _core;
		tag_manager		= _tm;
		
		tag_manager.addTaggableLifecycleListener(
			Taggable.TT_DOWNLOAD,
			new TaggableLifecycleAdapter()
			{
				public void
				initialised(
					List<Taggable>	current_taggables )
				{
					TagType tt = tag_manager.getTagType( TagType.TT_DOWNLOAD_MANUAL );
					
					tt.addTagTypeListener(
						new TagTypeAdapter()
						{
							public void
							tagAdded(
								Tag			tag )
							{
								TagFeatureProperties tfp = (TagFeatureProperties)tag;
								
								TagProperty[] props = tfp.getSupportedProperties();
								
								for ( TagProperty prop: props ){
									
									if ( prop.getName( false ).equals( TagFeatureProperties.PR_TRACKERS )){
										
										hookTagProperty( prop );
										
										break;
									}
								}
							}
						},
						true );
				}
				
				public void
				taggableCreated(
					Taggable		taggable )
				{
					handleDownload( (DownloadManager)taggable );
				}
			});
	}
	
	private void
	hookTagProperty(
		TagProperty		property )
	{
		property.addListener( this );
		
		handleProperty( property, true );
	}
	
	public void
	propertyChanged(
		TagProperty		property )
	{
		handleProperty( property, false );
	}
	
	public void
	propertySync(
		TagProperty		property )
	{	
	}
	
	private void
	handleProperty(
		TagProperty		property,
		boolean			start_of_day )
	{
		String[] trackers = property.getStringList();
		
		Set<String>	tag_hosts = new HashSet<String>( Arrays.asList( trackers ));
		
		Tag tag = property.getTag();
		
		synchronized( tracker_host_map ){
			
			for ( Map.Entry<String,List<Tag>> entry: tracker_host_map.entrySet()){
			
				List<Tag> tags = entry.getValue();
				
				if ( tags.contains( tag )){
					
					if ( !tag_hosts.contains( entry.getKey())){
						
						tags.remove( tag );
					}
				}
			}
			
			for ( String host: tag_hosts ){
				
				List<Tag> tags = tracker_host_map.get( host );
				
				if ( tags == null ){
					
					tags = new ArrayList<Tag>();
					
					tracker_host_map.put( host, tags );
					
				}else if ( tags.contains( tag )){
					
					continue;
				}
				
				tags.add( tag );
			}
		}
		
		if ( start_of_day ){
			
			return;
		}
		
		Set<Taggable> tag_dls = tag.getTagged();
		
		for ( Taggable tag_dl: tag_dls ){
			
			DownloadManager dm = (DownloadManager)tag_dl;
						
			Set<String> hosts = TorrentUtils.getUniqueTrackerHosts( dm.getTorrent());
				
			boolean	hit = false;
			
			for ( String host: hosts ){
				
				if ( tag_hosts.contains( host )){
					
					hit = true;
					
					break;
				}
			}
			
			if ( !hit ){
				
				tag.removeTaggable( tag_dl );
			}
		}
		
		List<DownloadManager> managers = azureus_core.getGlobalManager().getDownloadManagers();
		
		for ( DownloadManager dm: managers ){
			
			if ( !dm.isPersistent()){
				
				continue;
			}
			
			if ( tag.hasTaggable( dm )){
				
				continue;
			}
			
			Set<String> hosts = TorrentUtils.getUniqueTrackerHosts( dm.getTorrent());
			
			boolean	hit = false;
			
			for ( String host: hosts ){
				
				if ( tag_hosts.contains( host )){
					
					hit = true;
					
					break;
				}
			}
			
			if ( hit ){
				
				tag.addTaggable( dm );
			}
		}
	}
	
	protected List<Tag>
	getTagsForDownload(
		DownloadManager		dm )
	{
		List<Tag>	result = new ArrayList<Tag>();
		
		if ( dm.isPersistent()){
										
			synchronized( tracker_host_map ){
				
				if ( tracker_host_map.size() > 0 ){
					
					Set<String> hosts = TorrentUtils.getUniqueTrackerHosts( dm.getTorrent());
		
					for ( String host: hosts ){
						
						List<Tag> tags = tracker_host_map.get( host );
						
						if ( tags != null ){
							
							result.addAll( tags );
						}
					}
				}
			}
		}
		
		return( result );
	}
	
	private void
	handleDownload(
		DownloadManager		dm )
	{
		List<Tag> applicable_tags = getTagsForDownload( dm );
		
		for ( Tag tag: applicable_tags ){
			
			if ( !tag.hasTaggable( dm )){
				
				tag.addTaggable( dm );
			}
		}
	}
}
