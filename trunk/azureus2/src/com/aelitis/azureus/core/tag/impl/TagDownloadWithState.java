/*
 * Created on Mar 22, 2013
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

import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagDownload;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.Taggable;

public class 
TagDownloadWithState
	extends TagWithState
	implements TagDownload
{
	private int upload_rate_limit;
	private int download_rate_limit;
	
	private LimitedRateGroup upload_limiter = 
		new LimitedRateGroup()
		{
			public String 
			getName() 
			{
				return( "tag_up: " + getTagName());
			}
			public int 
			getRateLimitBytesPerSecond()
			{
				return( upload_rate_limit );
			}
	
			public void
			updateBytesUsed(
					int	used )
			{
	
			}
		};

	private LimitedRateGroup download_limiter = 
		new LimitedRateGroup()
		{
			public String 
			getName() 
			{
				return( "tag_down: " + getTagName());
			}
			public int 
			getRateLimitBytesPerSecond()
			{
				return( download_rate_limit );
			}
	
			public void
			updateBytesUsed(
					int	used )
			{
	
			}
		}; 
		
		
	private boolean	do_up;
	private boolean	do_down;
		
	public
	TagDownloadWithState(
		TagTypeBase		tt,
		int				tag_id,
		String			name,
		boolean			do_up,
		boolean			do_down )
	{
		super( tt, tag_id, name );
		
		init( do_up, do_down );
	}
	
	protected
	TagDownloadWithState(
		TagTypeBase		tt,
		int				tag_id,
		Map				details,
		boolean			do_up,
		boolean			do_down )
	{
		super( tt, tag_id, details );
		
		init( do_up, do_down );
	}
	
	private void
	init(
		boolean		_do_up,
		boolean		_do_down )
	{
		do_up		= _do_up;
		do_down		= _do_down;
		
		upload_rate_limit 	= (int)readLongAttribute( AT_RATELIMIT_UP, 0 );
		download_rate_limit = (int)readLongAttribute( AT_RATELIMIT_DOWN, 0 );
		
		addTagListener(
			new TagListener()
			{
				public void
				tagabbleAdded(
					Tag			tag,
					Taggable	tagged )
				{
					DownloadManager manager = (DownloadManager)tagged;
					
					manager.addRateLimiter( upload_limiter, true );
					manager.addRateLimiter( download_limiter, false );
				}
				
				public void
				tagabbleRemoved(
					Tag			tag,
					Taggable	tagged )
				{
					DownloadManager manager = (DownloadManager)tagged;
					
					manager.removeRateLimiter( upload_limiter, true );
					manager.removeRateLimiter( download_limiter, false );
				}
			},
			true );
	}
	
	@Override
	public void 
	removeTag() 
	{
		for ( DownloadManager dm: getTaggedDownloads()){
			
			dm.removeRateLimiter( upload_limiter, true );
			dm.removeRateLimiter( download_limiter, false );
		}
		
		super.removeTag();
	}
	
	public 
	List<DownloadManager> 
	getTaggedDownloads() 
	{
		return((List<DownloadManager>)(Object)getTagged());
	}
	
	public boolean
	supportsTagUploadLimit()
	{
		return( do_up );
	}

	public boolean
	supportsTagDownloadLimit()
	{
		return( do_down );
	}

	public int
	getTagUploadLimit()
	{
		return( upload_rate_limit );
	}
	
	public void
	setTagUploadLimit(
		int		bps )
	{
		upload_rate_limit	= bps;
		
		writeLongAttribute( AT_RATELIMIT_UP, upload_rate_limit );
	}
	
	public int
	getTagCurrentUploadRate()
	{
		return( -1 );
	}
	
	public int
	getTagDownloadLimit()
	{
		return( download_rate_limit );
	}
	
	public void
	setTagDownloadLimit(
		int		bps )
	{
		download_rate_limit	= bps;
		
		writeLongAttribute( AT_RATELIMIT_DOWN, download_rate_limit );
	}
	
	public int
	getTagDownloadUploadRate()
	{
		return( -1 );
	}
}
