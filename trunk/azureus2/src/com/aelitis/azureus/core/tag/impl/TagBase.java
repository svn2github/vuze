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

import java.io.File;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.SimpleTimer;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.TagFeatureFileLocation;
import com.aelitis.azureus.core.tag.TagFeatureRSSFeed;
import com.aelitis.azureus.core.tag.TagFeatureRateLimit;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.Taggable;

public abstract class 
TagBase
	implements Tag, SimpleTimer.TimerTickReceiver
{
	protected static final String	AT_RATELIMIT_UP		= "rl.up";
	protected static final String	AT_RATELIMIT_DOWN	= "rl.down";
	protected static final String	AT_VISIBLE			= "vis";
	protected static final String	AT_PUBLIC			= "pub";
	protected static final String	AT_CAN_BE_PUBLIC	= "canpub";
	protected static final String	AT_ORIGINAL_NAME	= "oname";
	protected static final String	AT_IMAGE_ID			= "img.id";
	protected static final String	AT_COLOR_ID			= "col.rgb";
	protected static final String	AT_RSS_ENABLE		= "rss.enable";
	protected static final String	AT_RATELIMIT_UP_PRI	= "rl.uppri";
	protected static final String	AT_XCODE_TARGET		= "xcode.to";
	protected static final String	AT_FL_MOVE_COMP		= "fl.comp";
	  
	private TagTypeBase	tag_type;
	
	private int			tag_id;
	private String		tag_name;
	
	private static final int TL_ADD 	= 1;
	private static final int TL_REMOVE 	= 2;
	private static final int TL_SYNC 	= 3;
	
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
						
						listener.taggableAdded(TagBase.this,(Taggable)value);
						
					}else if ( type == TL_REMOVE ){
						
						listener.taggableRemoved(TagBase.this,(Taggable)value);
						
					}else if ( type == TL_SYNC ){
						
						listener.taggableSync( TagBase.this );
					}
				}
			});	
		
	private Boolean	is_visible;
	private Boolean	is_public;
	
	private TagFeatureRateLimit		tag_rl;
	private TagFeatureRSSFeed		tag_rss;
	private TagFeatureFileLocation	tag_fl;
	
	
	protected
	TagBase(
		TagTypeBase			_tag_type,
		int					_tag_id,
		String				_tag_name )
	{
		tag_type		= _tag_type;
		tag_id			= _tag_id;
		tag_name		= _tag_name;
		
		is_visible = readBooleanAttribute( AT_VISIBLE, null );
		is_public = readBooleanAttribute( AT_PUBLIC, null );
		
		if ( this instanceof TagFeatureRateLimit ){
			
			tag_rl = (TagFeatureRateLimit)this;
		}
		
		if ( this instanceof TagFeatureRSSFeed ){
			
			tag_rss = (TagFeatureRSSFeed)this;
			
			if ( tag_rss.isTagRSSFeedEnabled()){
				
				getManager().checkRSSFeeds( this, true );
			}
		}
		
		if ( this instanceof TagFeatureFileLocation ){
			
			tag_fl = (TagFeatureFileLocation)this;
		}
	}
	
	protected void
	addTag()
	{
		tag_type.addTag( this );
	}
	
	protected TagManagerImpl
	getManager()
	{
		return( tag_type.getTagManager());
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
	
	public long 
	getTagUID() 
	{
		return((((long)getTagType().getTagType())<<32) | tag_id );
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
				
				String original_name = readStringAttribute( AT_ORIGINAL_NAME, null );

				if ( original_name != null && original_name.startsWith( "tag." )){
					
					return( original_name );
				}
				
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
		
		if ( tag_name.startsWith( "tag." )){
		
			String original_name = readStringAttribute( AT_ORIGINAL_NAME, null );
		
			if ( original_name == null ){
			
				writeStringAttribute( AT_ORIGINAL_NAME, tag_name );
			}
		}
		
		tag_name = name;
				
		tag_type.fireChanged( this );
	}
	
		// public
	
	public boolean
	isPublic()
	{
		return( is_public==null?getPublicDefault():is_public );
	}
	
	public void
	setPublic(
		boolean	v )
	{
		if ( is_public == null || v != is_public ){
			
			if ( v && !canBePublic()){
				
				Debug.out( "Invalid attempt to set public" );
				
				return;
			}
			
			is_public	= v;
			
			writeBooleanAttribute( AT_PUBLIC, v );
			
			tag_type.fireChanged( this );
		}
	}
	
	protected boolean
	getPublicDefault()
	{
		if ( !getCanBePublicDefault()){
			
			return( false );
		}
		
		return( tag_type.getTagManager().getTagPublicDefault());
	}
	
	public void
	setCanBePublic(
		boolean	can_be_public )
	{
		writeBooleanAttribute( AT_CAN_BE_PUBLIC, can_be_public );
		
		if ( !can_be_public ){
			
			if ( isPublic()){
				
				setPublic( false );
			}
		}
	}
	
	public boolean
	canBePublic()
	{
		return( readBooleanAttribute( AT_CAN_BE_PUBLIC, getCanBePublicDefault()));
	}
	
	protected boolean
	getCanBePublicDefault()
	{
		return( true );
	}
	
		// visible
	
	public boolean
	isVisible()
	{
		return( is_visible==null?getVisibleDefault():is_visible );
	}
	
	public void
	setVisible(
		boolean	v )
	{
		if ( is_visible == null || v != is_visible ){
			
			is_visible	= v;
			
			writeBooleanAttribute( AT_VISIBLE, v );
			
			tag_type.fireChanged( this );
		}
	}
	
	protected boolean
	getVisibleDefault()
	{
		return( true );
	}

	public String
	getImageID()
	{
		return( readStringAttribute( AT_IMAGE_ID, null ));
	}
	
	public void
	setImageID(
		String		id )
	{
		writeStringAttribute( AT_IMAGE_ID, id );
	}
	
	private int[]
	decodeRGB(
		String str )
	{
		if ( str == null ){
			
			return( null );
		}
		
		String[] bits = str.split( "," );
		
		if ( bits.length != 3 ){
			
			return( null );
		}
		
		int[] rgb = new int[3];
		
		for ( int i=0;i<bits.length;i++){
			
			try{
				
				rgb[i] = Integer.parseInt(bits[i]);
				
			}catch( Throwable e ){
				
				return( null );
			}
		}
		
		return( rgb );
	}
	
	private String
	encodeRGB(
		int[]	rgb )
	{
		if ( rgb == null || rgb.length != 3 ){
			
			return( null );
		}
		
		return( rgb[0]+","+rgb[1]+","+rgb[2] );
	}
	
	public int[]
	getColor()
	{
		int[] result = decodeRGB( readStringAttribute( AT_COLOR_ID, null ));
		
		if ( result == null ){
			
			result = tag_type.getColorDefault();
		}
		
		return( result );
	}
	
	public void
	setColor(
		int[]		rgb )
	{
		writeStringAttribute( AT_COLOR_ID, encodeRGB( rgb ));
	}
	
	public boolean
	isTagRSSFeedEnabled()
	{
		if ( tag_rss != null ){
		
			return( readBooleanAttribute( AT_RSS_ENABLE, false ));
		}
		
		return( false );
	}
	
	public void
	setTagRSSFeedEnabled(
		boolean		enable )
	{
		if ( tag_rss != null ){
			
			if ( isTagRSSFeedEnabled() != enable ){
			
				writeBooleanAttribute( AT_RSS_ENABLE, enable );
				
				tag_type.fireChanged( this );
				
				tag_type.getTagManager().checkRSSFeeds( this, enable );
			}
		}
	}
	
	public boolean
	supportsTagMoveOnComplete()
	{
		return( false );
	}
	
	public File
	getTagMoveOnCompleteFolder()
	{
		if ( tag_fl != null ){
			
			String str = readStringAttribute( AT_FL_MOVE_COMP, null );
			
			if ( str == null ){
				
				return( null );
				
			}else{
				
				return( new File( str ));
			}
		}
		
		return( null );
	}
	
	public void
	setTagMoveOnCompleteFolder(
		File		folder )
	{
		if ( tag_fl != null ){
			
			File	existing = getTagMoveOnCompleteFolder();
			
			if ( existing == null && folder == null ){
				
				return;
				
			}else if ( existing == null || folder == null || !existing.equals( folder )){
				
				writeStringAttribute( AT_FL_MOVE_COMP, folder==null?null:folder.getAbsolutePath());
				
				tag_type.fireChanged( this );
			}
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
	
	protected void
	sync()
	{
		t_listeners.dispatch( TL_SYNC, null );
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
		boolean was_rss = isTagRSSFeedEnabled();
		
		tag_type.removeTag( this );
		
		if ( was_rss ){
		
			tag_type.getTagManager().checkRSSFeeds( this, false );
		}
	}
	
	public void
	addTagListener(
		TagListener	listener,
		boolean		fire_for_existing )
	{
		t_listeners.addListener( listener );
		
		if ( fire_for_existing ){
			
			for ( Taggable t: getTagged()){
				
				listener.taggableAdded( this, t );
			}
		}
	}
	
	public void
	removeTagListener(
		TagListener	listener )
	{
		t_listeners.removeListener( listener );
	}
	
	protected Boolean
	readBooleanAttribute(
		String		attr,
		Boolean		def )
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
	
	protected String
	readStringAttribute(
		String	attr,
		String	def )
	{
		return( tag_type.readStringAttribute( this, attr, def ));
	}
	
	protected void
	writeStringAttribute(
		String	attr,
		String	value )
	{
		tag_type.writeStringAttribute( this, attr, value );
	}
	
	
	private static final int HISTORY_MAX_SECS = 30*60;
	private volatile boolean history_retention_required;
	private long[]	history;
	private int		history_pos;
	private boolean	history_wrapped;
	private boolean	timer_registered;
	
	public void
	setRecentHistoryRetention(
		boolean	required )
	{
		if ( tag_rl == null || !tag_rl.supportsTagRates()){
			
			return;
		}
		
		synchronized( this ){
			
			if ( required ){
				
				if ( !history_retention_required ){
					
					history 	= new long[HISTORY_MAX_SECS];
					
					history_pos	= 0;
					
					history_retention_required = true;
					
					if ( !timer_registered ){
						
						SimpleTimer.addTickReceiver( this );
						
						timer_registered = true;
					}
				}
			}else{
				
				history = null;
				
				history_retention_required = false;
				
				if ( timer_registered ){
					
					SimpleTimer.removeTickReceiver( this );
					
					timer_registered = false;
				}
			}
		}
	}
	
	public int[][]
 	getRecentHistory()
 	{
 		synchronized( this ){

 			if ( history == null ){
 		
 				return( new int[2][0] );
 				
 			}else{
 			
 				int	entries = history_wrapped?HISTORY_MAX_SECS:history_pos;
 				int	start	= history_wrapped?history_pos:0;
 				
 				int[][] result = new int[2][entries];
 				
 				int	pos = start;
 				
 				for ( int i=0;i<entries;i++){
 					
 					if ( pos == HISTORY_MAX_SECS ){
 						
 						pos = 0;
 					}
 					
 					long entry = history[pos++];
 					
 					int	send_rate 	= (int)((entry>>32)&0xffffffffL);
 					int	recv_rate 	= (int)((entry)    &0xffffffffL);
 					
 					result[0][i] = send_rate;
 					result[1][i] = recv_rate;
  				}
 				
 				return( result );
 			}
 		}
 	}
 	
 	public void
 	tick(
 		int count )
 	{
 		if ( !history_retention_required ){
 			
 			return;
 		}
 		
  		long send_rate 			= tag_rl.getTagCurrentUploadRate();
 		long receive_rate 		= tag_rl.getTagCurrentDownloadRate();
 		
 		long	entry = 
 			(((send_rate)<<32) & 0xffffffff00000000L ) |
 			(((receive_rate)   & 0x00000000ffffffffL ));
  			
 		
 		synchronized( this ){
 			
 			if ( history != null ){
 				
 				history[history_pos++] = entry;
 				
 				if ( history_pos == HISTORY_MAX_SECS ){
 					
 					history_pos 	= 0;
 					history_wrapped	= true;
 				}
 			}
 		}
 	}
}
