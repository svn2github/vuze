/*
 * Created on Mar 20, 2013
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

import java.io.File;
import java.util.Set;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.SimpleTimer;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.TagFeatureExecOnAssign;
import com.aelitis.azureus.core.tag.TagFeatureFileLocation;
import com.aelitis.azureus.core.tag.TagFeatureProperties;
import com.aelitis.azureus.core.tag.TagFeatureRSSFeed;
import com.aelitis.azureus.core.tag.TagFeatureRateLimit;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.TagFeatureProperties.TagProperty;
import com.aelitis.azureus.core.tag.TagFeatureProperties.TagPropertyListener;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public abstract class 
TagBase
	implements Tag, SimpleTimer.TimerTickReceiver
{
	protected static final String	AT_RATELIMIT_UP		= "rl.up";
	protected static final String	AT_RATELIMIT_DOWN	= "rl.down";
	protected static final String	AT_VISIBLE			= "vis";
	protected static final String	AT_PUBLIC			= "pub";
	protected static final String	AT_GROUP			= "gr";
	protected static final String	AT_CAN_BE_PUBLIC	= "canpub";
	protected static final String	AT_ORIGINAL_NAME	= "oname";
	protected static final String	AT_IMAGE_ID			= "img.id";
	protected static final String	AT_COLOR_ID			= "col.rgb";
	protected static final String	AT_RSS_ENABLE		= "rss.enable";
	protected static final String	AT_RATELIMIT_UP_PRI	= "rl.uppri";
	protected static final String	AT_XCODE_TARGET		= "xcode.to";
	protected static final String	AT_FL_MOVE_COMP		= "fl.comp";
	protected static final String	AT_FL_COPY_COMP		= "fl.copy";
	protected static final String	AT_FL_INIT_LOC		= "fl.init";
	protected static final String	AT_RATELIMIT_MIN_SR	= "rl.minsr";
	protected static final String	AT_RATELIMIT_MAX_SR	= "rl.maxsr";
	protected static final String	AT_PROPERTY_PREFIX	= "pp.";
	protected static final String	AT_EOA_PREFIX		= "eoa.";

	private static final String[] EMPTY_STRING_LIST = {};
	
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
	private String	group;

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
		
		if ( getManager().isEnabled()){
		
			is_visible 	= readBooleanAttribute( AT_VISIBLE, null );
			is_public 	= readBooleanAttribute( AT_PUBLIC, null );
			group		= readStringAttribute( AT_GROUP, null );
			
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
	}
	
	protected void
	addTag()
	{
		if ( getManager().isEnabled()){
		
			tag_type.addTag( this );
		}
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
		boolean pub = is_public==null?getPublicDefault():is_public;
		
		if ( pub ){
			
			if ( isTagAuto()){
				
				pub = false;
			}
		}
		
		return( pub );
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
	
	public boolean 
	isTagAuto() 
	{
		return( false );
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
	
	public String
	getGroup()
	{
		return( group );
	}
	
	public void
	setGroup(
		String		new_group )
	{
		if ( group == null && new_group == null ){
			
			return;
		}
		
		if ( group == null || new_group == null || !group.equals(new_group)){
			
			group	= new_group;
			
			writeStringAttribute( AT_GROUP, new_group );
			
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
		
		tag_type.fireChanged( this );
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
	
		// initial save location
	
	public boolean
	supportsTagInitialSaveFolder()
	{
		return( false );
	}
	
	public File
	getTagInitialSaveFolder()
	{
		if ( tag_fl != null ){
			
			String str = readStringAttribute( AT_FL_INIT_LOC, null );
			
			if ( str == null ){
				
				return( null );
				
			}else{
				
				return( new File( str ));
			}
		}
		
		return( null );
	}
	
	public void
	setTagInitialSaveFolder(
		File		folder )
	{
		if ( tag_fl != null ){
			
			File	existing = getTagInitialSaveFolder();
			
			if ( existing == null && folder == null ){
				
				return;
				
			}else if ( existing == null || folder == null || !existing.equals( folder )){
				
				writeStringAttribute( AT_FL_INIT_LOC, folder==null?null:folder.getAbsolutePath());
				
				tag_type.fireChanged( this );
			}
		}
	}
	
		// move on complete
	
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
	
		// copy on complete
		
	public boolean
	supportsTagCopyOnComplete()
	{
		return( false );
	}
	
	public File
	getTagCopyOnCompleteFolder()
	{
		if ( tag_fl != null ){
			
			String str = readStringAttribute( AT_FL_COPY_COMP, null );
			
			if ( str == null ){
				
				return( null );
				
			}else{
				
				return( new File( str ));
			}
		}
		
		return( null );
	}
	
	public void
	setTagCopyOnCompleteFolder(
		File		folder )
	{
		if ( tag_fl != null ){
			
			File	existing = getTagCopyOnCompleteFolder();
			
			if ( existing == null && folder == null ){
				
				return;
				
			}else if ( existing == null || folder == null || !existing.equals( folder )){
				
				writeStringAttribute( AT_FL_COPY_COMP, folder==null?null:folder.getAbsolutePath());
				
				tag_type.fireChanged( this );
			}
		}
	}

	
		// min ratio
	
	public int
	getTagMinShareRatio()
	{
		return( -1 );
	}
	
	public void
	setTagMinShareRatio(
		int		sr )
	{
		Debug.out( "not supported" );
	}
	
		// max ratio
	
	public int
	getTagMaxShareRatio()
	{
		return( -1 );
	}
	
	public void
	setTagMaxShareRatio(
		int		sr )
	{
		Debug.out( "not supported" );
	}

	public TagProperty[]
	getSupportedProperties()
	{
		return( new TagProperty[0] );
	}
	
	public TagProperty
	getProperty(
		String		name )
	{
		TagProperty[] props = getSupportedProperties();
		
		for ( TagProperty prop: props ){
			
			if ( prop.getName( false ) == name ){
				
				return( prop );
			}
		}
		
		return( null );
	}
	
	protected TagProperty
	createTagProperty(
		String		name,
		int			type )
	{
		return( new TagPropertyImpl( name, type ));
	}
	
		// exec on assign
	
	public int
	getSupportedActions()
	{
		return( TagFeatureExecOnAssign.ACTION_NONE );
	}
	
	public boolean
	supportsAction(
		int		action )
	{
		return((getSupportedActions() & action ) != 0 );
	}
	
	public boolean
	isActionEnabled(
		int		action )
	{
		if ( !supportsAction( action )){
			
			Debug.out( "not supported" );
			
			return( false );
		}
		
		return( readBooleanAttribute( AT_PROPERTY_PREFIX + action, false ));
	}
	
	public void
	setActionEnabled(
		int			action,
		boolean		enabled )
	{
		if ( !supportsAction( action )){
			
			Debug.out( "not supported" );
			
			return;
		}
		
		writeBooleanAttribute( AT_PROPERTY_PREFIX + action, enabled );
	}
	
		// others
	
	public void
	addTaggable(
		Taggable	t )
	{
		t_listeners.dispatch( TL_ADD, t );
		
		tag_type.taggableAdded( this, t );
		
		tag_type.fireChanged( this );
	}
	
	public void
	removeTaggable(
		Taggable	t )
	{
		t_listeners.dispatch( TL_REMOVE, t );
		
		tag_type.taggableRemoved( this, t );

		tag_type.fireChanged( this );
	}
	
	protected void
	sync()
	{
		t_listeners.dispatch( TL_SYNC, null );
		
		tag_type.taggableSync( this );
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
	
	public String
	getDescription()
	{
		return( null );
	}
	
	public void
	addTagListener(
		TagListener	listener,
		boolean		fire_for_existing )
	{
		if (!t_listeners.hasListener(listener)) {
			t_listeners.addListener( listener );
		}
		
		if ( fire_for_existing ){
			
			for ( Taggable t: getTagged()){
				
				listener.taggableAdded( this, t );
			}
		}
	}
	
	protected void
	destroy()
	{
		Set<Taggable>	taggables = getTagged();
		
		for( Taggable t: taggables ){
			
			t_listeners.dispatch( TL_REMOVE, t );
			
			tag_type.taggableRemoved( this, t );
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
	
	protected boolean
	writeBooleanAttribute(
		String	attr,
		boolean	value )
	{
		return( tag_type.writeBooleanAttribute( this, attr, value ));
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
	
	protected String[]
	readStringListAttribute(
		String		attr,
		String[]	def )
	{
		return( tag_type.readStringListAttribute( this, attr, def ));
	}
	
	protected boolean
	writeStringListAttribute(
		String		attr,
		String[]	value )
	{
		return( tag_type.writeStringListAttribute( this, attr, value ));
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
 		long	mono_now,
 		int 	count )
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
 	
 	private class
 	TagPropertyImpl
 		implements TagProperty
 	{
 		private String		name;
 		private int			type;
 		
 		private CopyOnWriteList<TagPropertyListener>	listeners = new CopyOnWriteList<TagPropertyListener>();
 		
 		private
 		TagPropertyImpl(
 			String		_name,
 			int			_type )
 		{
 			name		= _name;
 			type		= _type;
 		}
 		
 		public Tag 
 		getTag() 
 		{
 			return( TagBase.this );
 		}
 		
		public int
		getType()
		{
			return( type );
		}
		
		public String
		getName(
			boolean	localize )
		{
			if ( localize ){
				
				return( MessageText.getString( "tag.property." + name ));
				
			}else{
			
				return( name );
			}
		}
		
		public void
		setStringList(
			String[]	value )
		{
			if ( writeStringListAttribute( AT_PROPERTY_PREFIX + name, value )){
				
				for ( TagPropertyListener l: listeners ){
					
					try{
						l.propertyChanged( this );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
				
				tag_type.fireChanged( TagBase.this );
			}
		}
		
		public String[]
		getStringList()
		{
			return( readStringListAttribute( AT_PROPERTY_PREFIX + name, EMPTY_STRING_LIST ));
		}
		
		public void
		setBoolean(
			boolean	value )
		{
			if ( writeBooleanAttribute( AT_PROPERTY_PREFIX + name, value )){
				
				for ( TagPropertyListener l: listeners ){
					
					try{
						l.propertyChanged( this );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
				
				tag_type.fireChanged( TagBase.this );
			}
		}
		
		public Boolean
		getBoolean()
		{
			return( readBooleanAttribute( AT_PROPERTY_PREFIX + name, null ));
		}
		
		public String
		getString()
		{
			String	value = null;
			
			switch( getType()){
				case TagFeatureProperties.PT_STRING_LIST:{
					String[] vals = getStringList();
					
					if ( vals != null && vals.length > 0 ){
						value = "";
						
						if ( getName( false ).equals( TagFeatureProperties.PR_TRACKER_TEMPLATES )){
							
							String str_merge 	= MessageText.getString("label.merge" );
							String str_replace 	= MessageText.getString("label.replace" );
							String str_remove 	= MessageText.getString("Button.remove" );

							for ( String val: vals ){
								String[] bits = val.split( ":" );
								String type = bits[0];
								String str 	= bits[1];
								
								if ( type.equals("m")){
									str += ": " + str_merge;
								}else if ( type.equals( "r" )){
									str += ": " + str_replace;
								}else{
									str += ": " + str_remove;
								}
								value += (value.length()==0?"":"," ) + str;
							}
						}else{
							for ( String val: vals ){
								value += (value.length()==0?"":"," ) + val;
							}
						}
					}
					break;
				}
				case TagFeatureProperties.PT_BOOLEAN:{
					Boolean val = getBoolean();
					if ( val != null ){
						value = String.valueOf( val );
					}
					break;
				}
				default:{
					value = "Unknown type";
				}
			}
			
			if ( value == null ){
				
				return( "" );
				
			}else{
			
				return( getName( true ) + "=" + value );
			}
		}
		
		public void
		addListener(
			TagPropertyListener		listener )
		{
			listeners.add( listener );	
		}
		
		public void
		removeListener(
			TagPropertyListener		listener )
		{
			listeners.remove( listener );
		}
		
		public void
		syncListeners()
		{
			for ( TagPropertyListener l: listeners ){
				
				try{
					l.propertySync( this );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
 	}
}
