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
import java.util.Set;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagDownload;
import com.aelitis.azureus.core.tag.TagFeatureRunState;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.Taggable;

public class 
TagDownloadWithState
	extends TagWithState
	implements TagDownload
{
	private int upload_rate_limit;
	private int download_rate_limit;
	
	private int	upload_rate		= -1;
	private int	download_rate	= -1;
	
	private long last_rate_update;
	
	private LimitedRateGroup upload_limiter = 
		new LimitedRateGroup()
		{
			public String 
			getName() 
			{
				return( "tag_up: " + getTagName( true ));
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
				return( "tag_down: " + getTagName( true ));
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
		
	private boolean	do_rates;
	private boolean	do_up;
	private boolean	do_down;
	
	private int		run_states;
	
	private static AsyncDispatcher rs_async = new AsyncDispatcher(2000);

	public
	TagDownloadWithState(
		TagTypeBase		tt,
		int				tag_id,
		String			name,
		boolean			do_rates,
		boolean			do_up,
		boolean			do_down,
		int				run_states )
	{
		super( tt, tag_id, name );
		
		init( do_rates, do_up, do_down, run_states );
	}
	
	protected
	TagDownloadWithState(
		TagTypeBase		tt,
		int				tag_id,
		Map				details,
		boolean			do_rates,
		boolean			do_up,
		boolean			do_down,
		int				run_states )
	{
		super( tt, tag_id, details );
		
		init( do_rates, do_up, do_down, run_states );
	}
	
	private void
	init(
		boolean		_do_rates,
		boolean		_do_up,
		boolean		_do_down,
		int			_run_states )
	{
		do_rates	= _do_rates;
		do_up		= _do_up;
		do_down		= _do_down;
		run_states	= _run_states;
		
		upload_rate_limit 	= (int)readLongAttribute( AT_RATELIMIT_UP, 0 );
		download_rate_limit = (int)readLongAttribute( AT_RATELIMIT_DOWN, 0 );
		
		addTagListener(
			new TagListener()
			{
				public void
				taggableAdded(
					Tag			tag,
					Taggable	tagged )
				{
					DownloadManager manager = (DownloadManager)tagged;
					
					manager.addRateLimiter( upload_limiter, true );
					manager.addRateLimiter( download_limiter, false );
				}
				
				public void 
				taggableSync(
					Tag 		tag ) 
				{
				}
				
				public void
				taggableRemoved(
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
	
	public int 
	getTaggableTypes() 
	{
		return( Taggable.TT_DOWNLOAD );
	}
	
	public Set<DownloadManager> 
	getTaggedDownloads() 
	{
		return((Set<DownloadManager>)(Object)getTagged());
	}
	
	public boolean
	supportsTagRates()
	{
		return( do_rates );
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
		updateRates();
		
		return( upload_rate );
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
	getTagCurrentDownloadRate()
	{
		updateRates();
		
		return( download_rate );
	}
	
	private void
	updateRates()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( now - last_rate_update > 2500 ){ 
			
			int	new_up		= 0;
			int new_down	= 0;
			
			Set<DownloadManager> dms = getTaggedDownloads();
			
			if ( dms.size() == 0 ){
				
				new_up		= -1;
				new_down	= -1;

			}else{
				
				new_up		= 0;
				new_down	= 0;
				
				for ( DownloadManager dm: dms ){
		
					DownloadManagerStats stats = dm.getStats();
					
					new_up 		+= stats.getDataSendRate() + stats.getProtocolSendRate();
					new_down 	+= stats.getDataReceiveRate() + stats.getProtocolReceiveRate();
				}
			}
			
			upload_rate			= new_up;
			download_rate		= new_down;
			last_rate_update 	= now;
		}
	}
	
	public int
	getRunStateCapabilities()
	{
		return( run_states );
	}
	
	public boolean
	hasRunStateCapability(
		int		capability )
	{
		return((run_states & capability ) != 0 );
	}
	
	public boolean[]
   	getPerformableOperations(
      	int[]	ops )
   	{
   		boolean[] result = new boolean[ ops.length];
   		
		Set<DownloadManager> dms = getTaggedDownloads();

		for ( DownloadManager dm: dms ){
			
			int	dm_state = dm.getState();
			
			for ( int i=0;i<ops.length;i++){
				
				if ( result[i]){
					
					continue;
				}
				
				int	op = ops[i];
				
				if (( op & TagFeatureRunState.RSC_START ) != 0 ){
					
					if ( 	dm_state == DownloadManager.STATE_STOPPED ||
							dm_state == DownloadManager.STATE_ERROR ){
						
						result[i] = true;
					}
				}
				
				if (( op & TagFeatureRunState.RSC_STOP ) != 0 ){
					
					if ( 	dm_state != DownloadManager.STATE_STOPPED &&
							dm_state != DownloadManager.STATE_STOPPING &&
							dm_state != DownloadManager.STATE_ERROR ){
						
						result[i] = true;
					}
				}
				
				if (( op & TagFeatureRunState.RSC_PAUSE ) != 0 ){
					
					if ( 	dm_state != DownloadManager.STATE_STOPPED &&
							dm_state != DownloadManager.STATE_STOPPING &&
							dm_state != DownloadManager.STATE_ERROR ){
						
						if ( !dm.isPaused()){
						
							result[i] = true;
						}
					}
				}
				
				if (( op & TagFeatureRunState.RSC_RESUME ) != 0 ){

					if ( dm.isPaused()){
						
						result[i] = true;
					}
				}
			}
		}
		
		return( result );
   	}
	
	public void
	performOperation(
		int		op )
	{
		Set<DownloadManager> dms = getTaggedDownloads();

		for ( final DownloadManager dm: dms ){
			
			int	dm_state = dm.getState();

			if ( op == TagFeatureRunState.RSC_START ){
				
				if ( 	dm_state == DownloadManager.STATE_STOPPED ||
						dm_state == DownloadManager.STATE_ERROR ){		    		
		    	
					rs_async.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								dm.setStateQueued();
							}
						});
				}
			}else if ( op == TagFeatureRunState.RSC_STOP ){
				
				if ( 	dm_state != DownloadManager.STATE_STOPPED &&
						dm_state != DownloadManager.STATE_STOPPING &&
						dm_state != DownloadManager.STATE_ERROR ){
					
					rs_async.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								dm.stopIt( DownloadManager.STATE_STOPPED, false, false );
							}
						});
				}
			}else if ( op == TagFeatureRunState.RSC_PAUSE ){
				
				if ( 	dm_state != DownloadManager.STATE_STOPPED &&
						dm_state != DownloadManager.STATE_STOPPING &&
						dm_state != DownloadManager.STATE_ERROR ){
					
					rs_async.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								dm.pause();
							}
						});
				}
			}else if ( op == TagFeatureRunState.RSC_RESUME ){

				if ( dm.isPaused()){
					
					rs_async.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								dm.resume();
							}
						});
				}
			}
		}
	}
}
