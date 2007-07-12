/*
 * Created on Jul 6, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.speedmanager.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingMapper;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingZone;

class
SpeedManagerPingMapperImpl
	implements SpeedManagerPingMapper
{
	static final int VARIANCE_GOOD_VALUE		= 50;
	static final int VARIANCE_BAD_VALUE			= 150;
	static final int VARIANCE_MAX				= VARIANCE_BAD_VALUE*10;
	
	static final int RTT_MAX					= 30*1000;

	static final int MAX_BAD_LIMIT_HISTORY		= 32;
	
	static final int SPEED_DIVISOR = 256;
	
	private static final int SPEED_HISTORY_PERIOD	= 1*60*1000;
	private static final int SPEED_HISTORY_COUNT	= SPEED_HISTORY_PERIOD / SpeedManagerImpl.UPDATE_PERIOD_MILLIS;
		
	private SpeedManagerImpl	speed_manager;
	private String				name;
	private boolean				variance;
	private boolean				trans;
			
	private int	ping_count;
	
	private pingValue[]	pings;
	private int			max_pings;
	
	private pingValue	prev_ping;
	
	private int[]		x_speeds = new int[ SPEED_HISTORY_COUNT ];
	private int[]		y_speeds = new int[ SPEED_HISTORY_COUNT ];
	
	private int speeds_next;
	
	private LinkedList	regions;
		
	private int last_x;
	private int	last_y;
	
	private int[]	recent_metrics = new int[3];
	private int		recent_metrics_next;
	
	private limitEstimate	up_estimate;
	private limitEstimate	down_estimate;
	
	private LinkedList	last_bad_ups;
	private LinkedList	last_bad_downs;
	
	private limitEstimate	last_bad_up;
	private boolean			bad_up_in_progress;
	
	private limitEstimate	last_bad_down;
	private boolean			bad_down_in_progress;

	private limitEstimate	up_capacity		= getNullLimit();
	private limitEstimate	down_capacity	= getNullLimit();
	
	private File	history_file;
	
	protected
	SpeedManagerPingMapperImpl(
		SpeedManagerImpl		_speed_manager,
		String					_name,
		int						_entries ,
		boolean					_variance,
		boolean					_transient )
	{
		speed_manager	= _speed_manager;
		name			= _name;
		max_pings		= _entries;
		variance		= _variance;
		trans			= _transient;	
		
		init();
	}
	
	protected void
	init()
	{
		pings		= new pingValue[max_pings];
		ping_count	= 0;
		
		regions	= new LinkedList();
		
		up_estimate		= getNullLimit();
		down_estimate	= getNullLimit();

		last_bad_ups		= new LinkedList();
		last_bad_downs		= new LinkedList();
		
		last_bad_up				= null;
		bad_up_in_progress		= false;
		
		last_bad_down			= null;
		bad_down_in_progress	= false;
		
		up_capacity 	= getNullLimit();
		down_capacity 	= getNullLimit();

		prev_ping 			= null;
		recent_metrics_next	= 0;
	}
	
	protected synchronized void
	loadHistory(
		File		file )
	{
		try{
			if ( history_file != null && history_file.equals( file )){
				
				return;
			}
			
			if ( history_file != null ){
				
				saveHistory();
			}
			
			history_file = file;

			init();
			
			if ( history_file.exists()){
							
				Map map = FileUtil.readResilientFile( history_file );
				
				List	p = (List)map.get( "pings" );
				
				if ( p != null ){
					
					for (int i=0;i<p.size();i++){
						
						Map	m = (Map)p.get(i);
						
						int	x 		= ((Long)m.get( "x" )).intValue();
						int	y 		= ((Long)m.get( "y" )).intValue();
						int	metric 	= ((Long)m.get( "m" )).intValue();
						
						if ( i == 0 ){
							
							last_x	= 0;
							last_y	= 0;						
						}
						
						if ( variance ){
							
							if ( metric > VARIANCE_MAX ){
						
								metric = VARIANCE_MAX;
							}
						}else{
							
							if ( metric > RTT_MAX ){
								
								metric = RTT_MAX;
							}						
						}
						
						addPingSupport( x, y, -1, metric, false );
					}
				}
								
				last_bad_ups 	= loadLimits( map, "lbus" );
				last_bad_downs 	= loadLimits( map, "lbds" );
				
				up_capacity 	= loadLimit((Map)map.get( "upcap" ));
				down_capacity 	= loadLimit((Map)map.get( "downcap" ));
				
				log( "Loaded " + ping_count + " entries from " + history_file + ": bad_up=" + getLimitString(last_bad_ups) + ", bad_down=" + getLimitString(last_bad_downs));
			}
		
			prev_ping 			= null;
			recent_metrics_next	= 0;
			
			updateLimitEstimates();	
				
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected synchronized void
	saveHistory()
	{
		try{
			if ( history_file == null ){
				
				return;
			}
			
			Map	map = new HashMap();
			
			List p = new ArrayList(ping_count);
			
				// NOTE: add to this you will need to modify the "reset" method appropriately
			
			map.put( "pings", p );
			
			for (int i=0;i<ping_count;i++){
				
				pingValue ping = pings[i];
				
				Map	m = new HashMap();
				
				p.add( m );
				
				m.put( "x", new Long(ping.getX()));
				m.put( "y", new Long(ping.getY()));
				m.put( "m", new Long(ping.getMetric()));
			}
			
			saveLimits( map, "lbus", last_bad_ups );
			saveLimits( map, "lbds", last_bad_downs );

			map.put( "upcap", 	saveLimit( up_capacity ));
			map.put( "downcap", saveLimit( down_capacity ));

			FileUtil.writeResilientFile( history_file, map );
			
			log( "Saved " + p.size() + " entries to " + history_file );
		
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected LinkedList
	loadLimits(
		Map		map,
		String	name )
	{
		LinkedList	result = new LinkedList();
		
		List	l = (List)map.get(name);
		
		if ( l != null ){
			
			for (int i=0;i<l.size();i++){
				
				Map m = (Map)l.get(i);
							
				result. add(loadLimit( m ));
			}
		}
		
		return( result );
	}
	
	protected limitEstimate
	loadLimit(
		Map	m )
	{
		if ( m == null ){
			
			return( getNullLimit());
		}
		
		int	speed = ((Long)m.get( "s" )).intValue();
		
		double	metric = Double.parseDouble( new String((byte[])m.get("m")));
		
		int	hits = ((Long)m.get( "h" )).intValue();

		long	when = ((Long)m.get("w")).longValue();
		
		return( new limitEstimate( speed, metric, hits, when, new int[0][] ));
	}
	
	protected void
	saveLimits(
		Map				map,
		String			name,
		List			limits )
	{
		List	l = new ArrayList();
		
		for (int i=0;i<limits.size();i++){
			
			limitEstimate limit = (limitEstimate)limits.get(i);
			
			Map	m = saveLimit( limit );
			
			l.add( m );
		}
		
		map.put( name, l );
	}
	
	protected Map
	saveLimit(
		limitEstimate	limit )
	{
		if ( limit == null ){
			
			limit = getNullLimit();
		}
		
		Map	m = new HashMap();
		
		m.put( "s", new Long( limit.getBytesPerSec()));
		
		m.put( "m", String.valueOf( limit.getMetricRating()));
		
		m.put( "h", new Long( limit.getHits()));
		
		m.put( "w", new Long( limit.getWhen()));	
		
		return( m );
	}
	
	public boolean
	isActive()
	{
		return( variance );
	}
	
	protected limitEstimate
	getNullLimit()
	{
		return( new limitEstimate( 0, -1, 0, 0, new int[0][] ));
	}
	
	protected String
	getLimitString(
		List	limits )
	{
		String	str = "";
		
		for (int i=0;i<limits.size();i++){
	
			str += (i==0?"":",") + ((limitEstimate)limits.get(i)).getString();
		}
		
		return( str );
	}
	
	protected void
	log(
		String	str )
	{
		if ( speed_manager != null ){
		
			speed_manager.log( str );
		}
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	protected synchronized void
	addSpeed(
		int		x,
		int		y )
	{
		x = x/SPEED_DIVISOR;
		y = y/SPEED_DIVISOR;
		
		if ( x > 65535 )x = 65535;
		if ( y > 65535 )y = 65535;

		addSpeedSupport( x, y );
	}
	
	protected synchronized void
	addSpeedSupport(
		int		x,
		int		y )
	{
		x_speeds[speeds_next] = x;
		y_speeds[speeds_next] = y;
				
		speeds_next = (speeds_next+1)%SPEED_HISTORY_COUNT;
		
		int	min_x	= Integer.MAX_VALUE;
		int	min_y	= Integer.MAX_VALUE;
		
		for (int i=0;i<SPEED_HISTORY_COUNT;i++){
			
			min_x = Math.min( min_x, x_speeds[i] );
			min_y = Math.min( min_y, y_speeds[i] );
		}
		
		min_x *= SPEED_DIVISOR;
		min_y *= SPEED_DIVISOR;
				
		if ( up_capacity.getMetricRating() < SpeedManagerLimitEstimate.RATING_MANUAL ){
			
			if ( min_x > up_capacity.getBytesPerSec()){
				
				up_capacity.setBytesPerSec( min_x );
				
				up_capacity.setMetricRating( 0 );
				
				speed_manager.informUpCapChanged();
			}
		}
		
		if ( down_capacity.getMetricRating() < SpeedManagerLimitEstimate.RATING_MANUAL ){
			
			if ( min_y > down_capacity.getBytesPerSec()){
				
				down_capacity.setBytesPerSec( min_y );
				
				down_capacity.setMetricRating( 0 );
				
				speed_manager.informDownCapChanged();
			}
		}
	}
	
	protected synchronized void
	addPing(
		int		x,
		int		y,
		int		rtt,
		boolean	re_base )
	{
		x = x/SPEED_DIVISOR;
		y = y/SPEED_DIVISOR;
		
		if ( x > 65535 )x = 65535;
		if ( y > 65535 )y = 65535;
		if ( rtt > 65535 )rtt = variance?VARIANCE_MAX:RTT_MAX;
		if ( rtt == 0 )rtt = 1;
		
			// ping time won't refer to current x+y due to latencies, apply to average between
			// current and previous
				
		int	average_x = (x + last_x )/2;
		int	average_y = (y + last_y )/2;
		
		last_x	= x;
		last_y	= y;
		
		x	= average_x;
		y	= average_y;
		
		int	metric = rtt;
		
		if ( variance ){
			
			if ( re_base ){
				
				log( "Re-based variance" );
				
				recent_metrics_next = 0;
			}

			recent_metrics[recent_metrics_next++%recent_metrics.length] = metric;
			
			metric = 0;

			if ( recent_metrics_next > 1 ){
				
				int	entries = Math.min( recent_metrics_next, recent_metrics.length );
				
				int total = 0;
				
				for (int i=0;i<entries;i++){
					
					total += recent_metrics[i];
				}
				
				int	average = total/entries;
				
				int	total_deviation = 0;
				
				for (int i=0;i<entries;i++){

					int	deviation = recent_metrics[i] - average;
					
					total_deviation += deviation * deviation;
				}
				
				metric = (int)Math.sqrt( total_deviation );
			}
			
			if ( metric < VARIANCE_BAD_VALUE ){
				
				addSpeedSupport( x, y );
				
			}else{
				
				addSpeedSupport( 0, 0 );
			}
		}
		
		addPingSupport( x, y, rtt, metric, true );
		
		updateLimitEstimates();
	}
	
	protected void
	addPingSupport(
		int		x,
		int		y,
		int		rtt,
		int		metric,
		boolean	log )
	{
		if ( ping_count == pings.length ){

				// discard oldest pings and reset 
							
			int	to_discard = pings.length/10;
			
			if ( to_discard < 3 ){
				
				to_discard = 3;
			}
			
			ping_count = pings.length - to_discard;

			System.arraycopy(pings, to_discard, pings, 0, ping_count);
			
			for (int i=0;i<to_discard;i++ ){
				
				regions.removeFirst();
			}
		}
				
		pingValue	ping = new pingValue( x, y, metric );

		pings[ping_count++] = ping;
		
		region	new_region = null;
		
		if ( prev_ping != null ){
			
			new_region = new region(prev_ping,ping);
			
			regions.add( new_region );
		}
		
		prev_ping = ping;
		
		if ( variance && log ){
		
			log( "Ping: " + (rtt>0?("rtt="+rtt+","):"") + ping.getString() + (new_region==null?"":(", region=" + new_region.getString())));
		}
	}
	
	public synchronized int[][]
	getHistory()
	{
		int[][]	result = new int[ping_count][];

		for (int i=0;i<ping_count;i++){
			
			pingValue	ping = pings[i];
			
			result[i] = new int[]{ SPEED_DIVISOR*ping.getX(), SPEED_DIVISOR*ping.getY(), ping.getMetric()};
		}
		
		return( result );
	}
	
	public synchronized SpeedManagerPingZone[]
	getZones()
	{
		return((SpeedManagerPingZone[])regions.toArray( new SpeedManagerPingZone[regions.size()] ));
	}
	
	public synchronized SpeedManagerLimitEstimate
	getEstimatedUploadLimit(
		boolean	persistent )
	{
		return( adjustForPersistence( up_estimate, last_bad_up, persistent ));
	}
	
	public synchronized SpeedManagerLimitEstimate
	getEstimatedDownloadLimit(
		boolean	persistent )
	{
		return( adjustForPersistence( down_estimate, last_bad_down, persistent ));
	}

	public SpeedManagerLimitEstimate
	getLastBadUploadLimit()
	{
		return( last_bad_up );
	}
	
	public SpeedManagerLimitEstimate
	getLastBadDownloadLimit()
	{
		return( last_bad_down );
	}
	
	protected SpeedManagerLimitEstimate
	adjustForPersistence(
		SpeedManagerLimitEstimate	estimate,
		limitEstimate				last_bad,	
		boolean						persistent )
	{
		if ( estimate == null ){
			
			return( null );
		}
		
		if ( persistent ){
			
			if ( estimate.getMetricRating() == -1 || last_bad == null ){
				
				return( estimate );
			}
			
				// current estimate isn't bad and we have an old bad one
			
			if ( last_bad.getBytesPerSec() >= estimate.getBytesPerSec()){
				
				return( last_bad );
			}
			
				// current estimate is > last bad - use current estimate
			
			return( estimate );
			
		}else{
			
			return( estimate );
		}
	}
		
	protected void
	updateLimitEstimates()
	{
		up_estimate 	= getEstimatedLimit( true );
				
		if ( up_estimate != null ){
			
			double metric = up_estimate.getMetricRating();
			
			if ( metric == -1 ){
			
				if ( !bad_up_in_progress ){
					
					last_bad_ups.addLast( up_estimate );
					
					if ( last_bad_ups.size() > MAX_BAD_LIMIT_HISTORY ){
						
						last_bad_ups.removeFirst();
					}
				}
				
				bad_up_in_progress	= true;
				
				last_bad_up = up_estimate;
				
			}else if ( metric == 1 ){
				
				bad_up_in_progress	= false;
			}
		}
		
		down_estimate 	= getEstimatedLimit( false );
				
		if ( down_estimate != null ){
			
			double metric = down_estimate.getMetricRating();
			
			if ( metric == -1 ){
			
				if ( !bad_down_in_progress ){
					
					last_bad_downs.addLast( down_estimate );
					
					if ( last_bad_downs.size() > MAX_BAD_LIMIT_HISTORY ){
						
						last_bad_downs.removeFirst();
					}
				}
				
				bad_down_in_progress	= true;
				
				last_bad_down = down_estimate;
				
			}else if ( metric == 1 ){
				
				bad_down_in_progress	= false;
			}	
		}
	}
	
	protected synchronized limitEstimate
	getEstimatedLimit(
		boolean		up )
	{
		if ( !variance ){
			
			return( getNullLimit() );
		}
		
		int	num_samples = regions.size();
		
		if ( num_samples == 0 ){
			
			return( getNullLimit());
		}
		
		Iterator	it = regions.iterator();
		
		int	max_end = 0;
		
		while( it.hasNext()){
			
			region r = (region)it.next();
			
			int	end		= (up?r.getUploadEndBytesPerSec():r.getDownloadEndBytesPerSec())/SPEED_DIVISOR;
			
			if ( end > max_end ){
				
				max_end = end;
			}
		}
						
		int	sample_end = max_end + 1;
		
		int[]	totals 			= new int[sample_end];
		short[]	hits			= new short[sample_end];				
		short[]	worst_var_type	= new short[sample_end];
											
		ListIterator sample_it = regions.listIterator( 0 );
			
			// flatten out all observations into a single munged metric

		while( sample_it.hasNext()){
			
			region r = (region)sample_it.next();
		
			int	start 	= (up?r.getUploadStartBytesPerSec():r.getDownloadStartBytesPerSec())/SPEED_DIVISOR;
			int	end		= (up?r.getUploadEndBytesPerSec():r.getDownloadEndBytesPerSec())/SPEED_DIVISOR;
			int	metric	= r.getMetric();
		
			int	weighted_start;
			int	weighted_end;
			
			short	this_var_type;
			
			if ( metric < VARIANCE_GOOD_VALUE ){
			
					// a good variance applies to all speeds up to this one. This means
					// that previously occuring bad variance will get flattened out by
					// subsequent good variance
				
				weighted_start 	= 0;	
				weighted_end	= end;
				this_var_type 	= 0;
				
			}else if ( metric < VARIANCE_BAD_VALUE ){
				
					// medium values, treat at face value
				
				weighted_start 	= start;
				weighted_end	= end;
				this_var_type	= VARIANCE_GOOD_VALUE;

			}else{
				
					// bad ones, treat at face value
				
				weighted_start 	= start;
				weighted_end	= max_end;
				this_var_type	= VARIANCE_BAD_VALUE;
			}
			
			for (int j=weighted_start;j<=weighted_end;j++){
			
					// a bad variance resets totals as we have encountered this after (in time)
					// the existing data and this is more relevant and replaces any feel good
					// factor we might have accumulated via prior observations
				
				if ( this_var_type == VARIANCE_BAD_VALUE && worst_var_type[j] < this_var_type ){
					
					totals[j]	= 0;
					hits[j]		= 0;
					
					worst_var_type[j] = this_var_type;
				}
				
				totals[j] += metric;
				hits[j]++;
			}
		}

			// now average out values based on history computed above
						
		for (int i=0;i<sample_end;i++){
			
			int	hit = hits[i];
			
			if ( hit > 0 ){
				
				int	average = totals[i]/hit;
				
				totals[i] = average;
				
				if ( average < VARIANCE_GOOD_VALUE ){

					worst_var_type[i] = 0;
				
				}else if ( average < VARIANCE_BAD_VALUE ){
				
					worst_var_type[i] = VARIANCE_GOOD_VALUE;

				}else{
					
					worst_var_type[i] = VARIANCE_BAD_VALUE;
				}
			}
		}
			
			// break history up into segments of same speed 
		
		int	last_average 			= -1;
		int	last_average_change		= 0;
		int last_average_worst_var	= 0;
		int	last_max_hits			= 0;
		
		int	worst_var	= 0;
		
		List segments = new ArrayList(totals.length);
		
		for (int i=0;i<sample_end;i++){
			
			int var		= worst_var_type[i];
			int	hit 	= hits[i];
			
			if ( var > worst_var ){
				
				worst_var = var;
			}
			
			int average = totals[i];
			
			if ( i == 0 ){
				
				last_average = average;
				
			}else if ( last_average != average ){
				
				segments.add( new int[]{ last_average, last_average_change*SPEED_DIVISOR, (i-1)*SPEED_DIVISOR, last_average_worst_var, last_max_hits });
				
				last_average 			= average;
				last_average_change		= i;
				last_average_worst_var	= var;
				last_max_hits			= hit;
			}else{
				
				last_average_worst_var 	= Math.max( var, last_average_worst_var );
				last_max_hits			= Math.max( hit, last_max_hits );
			}
		}
		
		if ( last_average_change != sample_end - 1 ){
		
			segments.add( new int[]{ last_average, last_average_change*SPEED_DIVISOR, (sample_end-1)*SPEED_DIVISOR, last_average_worst_var, last_max_hits });
		}
		
		int[]	estimate_seg 	= null;
		
			// take smallest bad value and largest good
		
		if ( worst_var == VARIANCE_BAD_VALUE ){
			
			for (int i=segments.size()-1;i>=0;i-- ){
						
				int[]	seg = (int[])segments.get(i);
				
				int	var = seg[3];
				
				if ( var >= worst_var ){
						
					estimate_seg = seg;
				}
			}
		}else{
			for (int i=0;i<segments.size();i++){
			
				int[]	seg = (int[])segments.get(i);
			
				int	var = seg[3];
			
				if ( var >= worst_var ){
				
					estimate_seg = seg;
				}
			}
		}
		
		int	estimate_speed;
		int	estimate_hits;
		
		if ( estimate_seg == null ){
			
			estimate_speed 	= -1;
			estimate_hits	= 0;
		}else{
			
			estimate_speed 	= -1;
			
			if ( worst_var == 0 ){
				
				estimate_speed = estimate_seg[2];
				
			}else if ( worst_var == VARIANCE_GOOD_VALUE ){
				
				estimate_speed = ( estimate_seg[1] + estimate_seg[2])/2;

			}else{
				
				estimate_speed = estimate_seg[1];
			}
			
			estimate_hits = estimate_seg[4];
		}
			
		limitEstimate result = 
			new limitEstimate(
					estimate_speed,
					convertMetricToRating( worst_var ), 
					estimate_hits, 
					SystemTime.getCurrentTime(),
					(int[][])segments.toArray(new int[segments.size()][]));
		
		if ( variance ){
		
			log( "Estimate (samples=" + num_samples + ")" + (up?"up":"down") + "->" + result.getString());
		}
								
		return( result );
	}
	
	public synchronized double
	getCurrentMetricRating()
	{
		if ( ping_count == 0 ){
			
			return( 0 );
		}
		
		int	latest_metric = pings[ping_count-1].getMetric();
		
		if ( variance ){
			
			return( convertMetricToRating( latest_metric ));
			
		}else{
		
			return( 0 );
		}
	}
	
	public SpeedManagerLimitEstimate
	getEstimatedUploadCapacityBytesPerSec()
	{
		return( up_capacity );
	}
	
	public void
	setEstimatedDownloadCapacityBytesPerSec(
		int		bytes_per_sec,
		float	metric )
	{
		if ( down_capacity.getBytesPerSec() != bytes_per_sec || down_capacity.getMetricRating() != metric ){
			
			down_capacity.setBytesPerSec( bytes_per_sec );
			down_capacity.setMetricRating( metric );
			
			speed_manager.informDownCapChanged();
		}
	}
	
	public SpeedManagerLimitEstimate
	getEstimatedDownloadCapacityBytesPerSec()
	{
		return( down_capacity );
	}
	
	public void
	setEstimatedUploadCapacityBytesPerSec(
		int		bytes_per_sec,
		float	metric )
	{
		if ( up_capacity.getBytesPerSec() != bytes_per_sec || up_capacity.getMetricRating() != metric ){

			up_capacity.setBytesPerSec( bytes_per_sec );
			up_capacity.setMetricRating( metric );
			
			speed_manager.informUpCapChanged();
		}
	}
	
	protected synchronized void
	reset()
	{
		setEstimatedDownloadCapacityBytesPerSec( 0, SpeedManagerLimitEstimate.RATING_UNKNOWN );
		setEstimatedUploadCapacityBytesPerSec( 0, SpeedManagerLimitEstimate.RATING_UNKNOWN );
		
		ping_count	= 0;
		regions.clear();
		
		last_bad_down	= null;
		last_bad_downs.clear();
		
		last_bad_up		= null;
		last_bad_ups.clear();
		
		saveHistory();
	}
	
	protected double
	convertMetricToRating(
		int		metric )
	{
		if ( metric < VARIANCE_GOOD_VALUE ){
			
			return( +1 );
			
		}else if ( metric > VARIANCE_BAD_VALUE ){
			
			return( -1 );
			
		}else{
			
			return( 1 - ((double)metric - VARIANCE_GOOD_VALUE )/50 );
		}
	}
	
	protected void 
	generateEvidence(
		IndentWriter writer ) 
	{
		writer.println( "up_cap=" + up_capacity.getString());
		writer.println( "down_cap=" + down_capacity.getString());
		
		String	bad_up_str = "";
		
		Iterator	it = last_bad_ups.iterator();
		
		while( it.hasNext()){
			
			bad_up_str += (bad_up_str.length()==0?"":",") + ((limitEstimate)it.next()).getString();
		}
		
		writer.println( "bad_up=" + bad_up_str );
		
		String	bad_down_str = "";
		
		it = last_bad_downs.iterator();
		
		while( it.hasNext()){
			
			bad_down_str += (bad_down_str.length()==0?"":",") + ((limitEstimate)it.next()).getString();
		}
		
		writer.println( "bad_down=" + bad_down_str );
	}
	
	public void
	destroy()
	{
		if ( trans ){
			
			speed_manager.destroy( this );
			
		}else{
	
			Debug.out( "Attempt to destroy non-transient mapper!" );
		}
	}
	
	class
	pingValue
	{
		private short	x;
		private short	y;
		private short	metric;
		
		protected
		pingValue(
			int		_x,
			int		_y,
			int		_m )
		{
			x		= (short)_x;
			y		= (short)_y;
			metric	= (short)_m;
		}
		
		protected int
		getX()
		{
			return(((int)(x))&0xffff );
		}
		
		protected int
		getY()
		{
			return(((int)(y))&0xffff );
		}
		
		protected int
		getMetric()
		{
			return(((int)(metric))&0xffff );
		}
		
		protected String
		getString()
		{
			return("x=" + getX()+",y=" + getY() +",m=" + getMetric());
		}
	}

	class
	region
		implements SpeedManagerPingZone
	{
		private short	x1;
		private short	y1;
		private short	x2;
		private short	y2;
		private short	metric;
		
		protected
		region(
			pingValue		p1,
			pingValue		p2 )
		{
			x1 = (short)p1.getX();
			y1 = (short)p1.getY();
			x2 = (short)p2.getX();
			y2 = (short)p2.getY();
			
			if ( x2 < x1 ){
				short t = x1;
				x1 = x2;
				x2 = t;
			}
			if ( y2 < y1 ){
				short t = y1;
				y1 = y2;
				y2 = t;
			}
			metric = (short)((p1.getMetric()+p2.getMetric())/2);
		}
		
		public int
		getX1()
		{
			return( x1 & 0x0000ffff );
		}
		
		public int
		getY1()
		{
			return( y1 & 0x0000ffff );
		}
		
		public int
		getX2()
		{
			return( x2 & 0x0000ffff );
		}
		
		public int
		getY2()
		{
			return( y2 & 0x0000ffff );
		}
					
		public int
		getUploadStartBytesPerSec()
		{
			return( getX1()*SPEED_DIVISOR );
		}
		
		public int
		getUploadEndBytesPerSec()
		{
			return( getX2()*SPEED_DIVISOR + (SPEED_DIVISOR-1));
		}
		
		public int
		getDownloadStartBytesPerSec()
		{
			return( getY1()*SPEED_DIVISOR );
		}
		
		public int
		getDownloadEndBytesPerSec()
		{
			return( getY2()*SPEED_DIVISOR + (SPEED_DIVISOR-1));
		}
		
		public int
		getMetric()
		{
			return( metric & 0x0000ffff );

		}
					
		public String
		getString()
		{				
			return( "x="+getX1() + ",y="+getY1()+",w=" + (getX2()-getX1()+1) +",h=" + (getY2()-getY1()+1));
		}
	}
	
	class
	limitEstimate
		implements SpeedManagerLimitEstimate
	{
		private int		speed;
		private float	metric_rating;
		private long	when;
		private int		hits;
		
		private int[][]	segs;
		
		protected
		limitEstimate(
			int			_speed,
			double		_metric_rating,
			int			_hits,
			long		_when,
			int[][]		_segs )
		{
			speed				= _speed;
			metric_rating		= (float)_metric_rating;
			hits				= _hits;
			when				= _when;
			segs				= _segs;
		}
		
		public int
		getBytesPerSec()
		{
			return( speed );
		}
		
		protected void
		setBytesPerSec(
			int		s )
		{
			speed	= s;
		}
		
		public float
		getMetricRating()
		{
			return( metric_rating );
		}
		
		protected void
		setMetricRating(
			float	d )
		{
			metric_rating	= d;
		}
		
		public int[][]
		getSegments()
		{
			return( segs );
		}
		
		protected int
		getHits()
		{
			return( hits );
		}
		
		protected long
		getWhen()
		{
			return( when );
		}
		
		public String
		getString()
		{
			return( "speed=" + DisplayFormatters.formatByteCountToKiBEtc( speed )+
					",metric=" + metric_rating + ",segs=" + segs.length + ",hits=" + hits + ",when=" + when );
		}
	}
	
	
	public static void
	main(
		String[]	args )
	{
		SpeedManagerPingMapperImpl pm = new SpeedManagerPingMapperImpl( null, "test", 100, true, false );
		
		Random rand = new Random();
		
		int[][] phases = { 
				{ 50, 0, 100000, 50 },
				{ 50, 100000, 200000, 200 },
				{ 50, 50000, 50000, 200 },
				{ 50, 0, 100000, 50 },

		};
		
		for (int i=0;i<phases.length;i++){
			
			int[]	phase = phases[i];
			
			System.out.println( "**** phase " + i );
			
			for (int j=0;j<phase[0];j++){
			
				int	x_base 	= phase[1];
				int	x_var	= phase[2];
				int r = phase[3];
				
				pm.addPing( x_base + rand.nextInt( x_var ), x_base + rand.nextInt( x_var ), rand.nextInt( r ), false);
			
				SpeedManagerLimitEstimate up 	= pm.getEstimatedUploadLimit( false );
				SpeedManagerLimitEstimate down 	= pm.getEstimatedDownloadLimit( false );
				
				if ( up != null && down != null ){
					
					System.out.println( up.getString() + "," + down.getString());
				}
			}
		}
	}
}