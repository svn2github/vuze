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

import java.util.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.TrackersUtil;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagFeatureProperties;
import com.aelitis.azureus.core.tag.TagFeatureProperties.TagProperty;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.TagTypeAdapter;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.TaggableLifecycleAdapter;
import com.aelitis.azureus.core.tag.TaggableLifecycleListener;

public class 
TagPropertyTrackerTemplateHandler 
	implements TagFeatureProperties.TagPropertyListener, TagListener
{
	private TagManagerImpl	tag_manager;
		
	protected
	TagPropertyTrackerTemplateHandler(
		AzureusCore		_core,
		TagManagerImpl	_tm )
	{
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
								
								TagProperty prop = tfp.getProperty( TagFeatureProperties.PR_TRACKER_TEMPLATES );
								
								if ( prop != null ){
									
									prop.addListener( TagPropertyTrackerTemplateHandler.this );
									
									tag.addTagListener( TagPropertyTrackerTemplateHandler.this, false );
								}
							}
						},
						true );
				}
			});
	}

	private String[]
	getPropertyBits(
		TagProperty		prop )
	{
		String[] bits = prop.getStringList();
		
		if ( bits == null || bits.length == 0 ){
			
			return( null );
		}
		
		return( bits );
	}
	
	private void
	handleStuff(
		String[]			bits,
		Set<Taggable>		taggables )
	{
		Map<String,List<List<String>>> templates = TrackersUtil.getInstance().getMultiTrackers();
		 
		for ( String bit: bits ){
			 
			String[] temp = bit.split( ":" );
			 
			String t_name = temp[1];
			 
			List<List<String>> template_trackers = templates.get( t_name );
			 
			if ( template_trackers == null ){
				 
				Debug.out( "Tracker template '" + t_name + "' not found" );
				 
				continue;
			}
			 
			String type = temp[0];
			 
			for ( Taggable t: taggables ){
				 
				DownloadManager dm = (DownloadManager)t;
				 
				TOTorrent torrent = dm.getTorrent();
				
				if ( torrent != null ){
					
					List<List<String>> trackers = TorrentUtils.announceGroupsToList( torrent );
					
					if ( type.equals( "m" )){
						
						trackers = TorrentUtils.mergeAnnounceURLs( trackers, template_trackers );
						
					}else if ( type.equals( "r" )){
						
						trackers = template_trackers;
						
					}else{
						
						trackers = TorrentUtils.removeAnnounceURLs( trackers, template_trackers, true );
					}
					
					TorrentUtils.listToAnnounceGroups( trackers, torrent );
				}
			 }
		 }
	}
	
	public void
	propertyChanged(
		TagProperty		property )
	{
		String[] bits = getPropertyBits( property );
		
		if ( bits == null ){
			
			return;
		}
				
		handleStuff( bits, property.getTag().getTagged());
	}
	
	public void
	propertySync(
		TagProperty		property )
	{
		propertyChanged( property );
	}
	
	public void
	taggableAdded(
		Tag			tag,
		Taggable	tagged )
	{
		TagFeatureProperties tfp = (TagFeatureProperties)tag;
		
		TagProperty prop = tfp.getProperty( TagFeatureProperties.PR_TRACKER_TEMPLATES );

		if ( prop != null ){
			
			String[] bits = getPropertyBits( prop );
			
			if ( bits == null ){
				
				return;
			}
			
			Set<Taggable> taggables = new HashSet<Taggable>();
			
			taggables.add( tagged );
			
			handleStuff(  bits, taggables );
		}
	}
	
	public void
	taggableSync(
		Tag			tag )
	{	
	}
	
	public void
	taggableRemoved(
		Tag			tag,
		Taggable	tagged )
	{
	}
}
