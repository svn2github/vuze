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












import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.tag.TagFeatureProperties.*;


public class 
TagPropertyUntaggedHandler 
	implements TagTypeListener
{
	private AzureusCore		azureus_core;
	private TagManagerImpl	tag_manager;
	
	private boolean	is_initialised;
	private boolean	is_enabled;
	
	private Set<Tag>	untagged_tags = new HashSet<Tag>();
	
	private Map<Taggable,int[]>		taggable_counts = new IdentityHashMap<Taggable, int[]>();
	
	
	protected
	TagPropertyUntaggedHandler(
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
					try{
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
										
										if ( prop.getName( false ).equals( TagFeatureProperties.PR_UNTAGGED )){
											
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
									}
								}	
							},
							true );
						
					}finally{
						
						AzureusCoreFactory.addCoreRunningListener(
							new AzureusCoreRunningListener()
							{	
								public void 
								azureusCoreRunning(
									AzureusCore core )
								{
									synchronized( taggable_counts ){
										
										is_initialised = true;
										
										if ( is_enabled ){
											
											enable();
										}
									}
								}
							});
					}
				}
				
				public void
				taggableCreated(
					Taggable		taggable )
				{
					addDownloads( Arrays.asList( new DownloadManager[]{ (DownloadManager)taggable }));
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
		tag.addTagListener(
			new TagListener()
			{
				public void
				taggableAdded(
					Tag			tag,
					Taggable	tagged )
				{
					synchronized( taggable_counts ){
					
						if ( untagged_tags.contains( tag )){
							
							return;
						}
						
						int[] num = taggable_counts.get( tagged );
						
						if ( num == null ){
							
							num = new int[1];
							
							taggable_counts.put( tagged, num );
						}
						
						if ( num[0]++ == 0 ){
							
							//System.out.println( "tagged: " + tagged.getTaggableID());
							
							for ( Tag t: untagged_tags ){
								
								t.removeTaggable( tagged );
							}
						}					
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
					synchronized( taggable_counts ){
						
						if ( untagged_tags.contains( tag )){
							
							return;
						}
						
						int[] num = taggable_counts.get( tagged );
						
						if ( num != null ){			
						
							if ( num[0]-- == 1 ){
								
								//System.out.println( "untagged: " + tagged.getTaggableID());
								
								taggable_counts.remove( tagged );
								
								DownloadManager dm = (DownloadManager)tagged;
								
								if ( !dm.isDestroyed()){
									
									for ( Tag t: untagged_tags ){
										
										t.addTaggable( tagged );
									}
								}
							}
						}
					}
				}
			},
			false );
		
		synchronized( taggable_counts ){
			
			if ( untagged_tags.contains( tag )){
				
				return;
			}
		}
		
		Set<Taggable>	existing = tag.getTagged();
		
		synchronized( taggable_counts ){
			
			for ( Taggable tagged: existing ){
				
				int[] num = taggable_counts.get( tagged );
				
				if ( num == null ){
					
					num = new int[1];
					
					taggable_counts.put( tagged, num );
				}
				
				if ( num[0]++ == 0 ){
					
					//System.out.println( "tagged: " + tagged.getTaggableID());
					
					for ( Tag t: untagged_tags ){
						
						t.removeTaggable( tagged );
					}
				}	
			}
		}
	}
	
	public void
	tagChanged(
		Tag			tag )
	{
	}
	
	public void
	tagRemoved(
		Tag			tag )
	{
		synchronized( taggable_counts ){
				
			boolean was_untagged = untagged_tags.remove( tag );
			
			if ( was_untagged ){
				
				if ( untagged_tags.size() == 0 ){
					
					setEnabled( tag, false );
				}
			}
		}
	}
	
	private void
	enable()
	{
		TagType tt = tag_manager.getTagType( TagType.TT_DOWNLOAD_MANUAL );
		
		tt.addTagTypeListener( this, false );
		
		for ( Tag tag: tt.getTags()){
			
			tagAdded( tag );
		}

		List<DownloadManager> existing = azureus_core.getGlobalManager().getDownloadManagers();
		
		addDownloads( existing );
	}
	
	private void
	disable()
	{
		TagType tt = tag_manager.getTagType( TagType.TT_DOWNLOAD_MANUAL );

		tt.removeTagTypeListener( this );
		
		taggable_counts.clear();
	}
	
	private void
	setEnabled(
		Tag			current_tag,
		boolean		enabled )
	{
		if ( enabled == is_enabled ){
			
			if ( is_enabled ){
				
				if ( untagged_tags.size() < 2 ){
					
					Debug.out( "eh?" );
					
					return;
				}
				
				Set<Taggable> existing = current_tag.getTagged();
				
				for ( Taggable t: existing ){
				
					current_tag.removeTaggable( t );
				}
				
				Tag[] temp = untagged_tags.toArray(new Tag[untagged_tags.size()]);
				
				Tag copy_from = temp[0]==current_tag?temp[1]:temp[0];
				
				for ( Taggable t: copy_from.getTagged()){
					
					current_tag.addTaggable( t );
				}
			}
			
			return;
		}
		
		is_enabled = enabled;
		
		if ( enabled ){
			
			if ( is_initialised ){
				
				enable();
			}
		}else{
			
			disable();
		}
	}
	
	private void
	handleProperty(
		TagProperty		property )
	{
		Tag	tag = property.getTag();
				
		synchronized( taggable_counts ){
		
			Boolean val = property.getBoolean();
		
			if ( val != null && val ){
				
				untagged_tags.add( tag );
				
				setEnabled( tag, true );
				
			}else{
				
				boolean was_untagged = untagged_tags.remove( tag );
				
				if ( untagged_tags.size() == 0 ){
					
					setEnabled( tag, false );
				}
				
				if ( was_untagged ){
					
					Set<Taggable> existing = tag.getTagged();
					
					for ( Taggable t: existing ){
					
						tag.removeTaggable( t );
					}
				}
			}
		}
	}
	
	private void
	addDownloads(
		List<DownloadManager>		dms )
	{
		synchronized( taggable_counts ){
		
			if ( !is_enabled ){
				
				return;
			}
			
			for ( DownloadManager dm: dms ){
				
				if ( !dm.isPersistent()){
			
					continue;
				}
				
				if ( !taggable_counts.containsKey( dm )){
					
					for ( Tag t: untagged_tags ){
						
						t.addTaggable( dm );
					}
				}
			}		
		}
	}
}
