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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.FileUtil;
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
	
	private SpeedManagerImpl	speed_manager;
	private String				name;
	private boolean				variance;
			
	private int	ping_count;
	
	private pingValue[]	pings;
	
	private pingValue	prev_ping;
	
	private LinkedList	regions;
		
	private int last_x;
	private int	last_y;
	
	private int[]	recent_metrics = new int[3];
	private int		recent_metrics_next;
	
	private limitEstimate[]	up_estimate		= new limitEstimate[0];
	private limitEstimate[]	down_estimate	= new limitEstimate[0];
	
	private LinkedList	last_bad_ups	= new LinkedList();
	private LinkedList	last_bad_downs	= new LinkedList();
	
	private limitEstimate	last_bad_up;
	private boolean			bad_up_in_progress;
	
	private limitEstimate	last_bad_down;
	private boolean			bad_down_in_progress;

	private limitEstimate	up_capacity;
	private limitEstimate	down_capacity;
	
	private File	history_file;
	
	protected
	SpeedManagerPingMapperImpl(
		SpeedManagerImpl		_speed_manager,
		String					_name,
		int						_entries ,
		boolean					_variance )
	{
		speed_manager	= _speed_manager;
		name			= _name;
		variance		= _variance;
		
		pings	= new pingValue[_entries];
		
		regions	= new LinkedList();
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
						
			pings		= new pingValue[pings.length];
			ping_count	= 0;
			
			regions	= new LinkedList();
	
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
				
				prev_ping = null;
				
				last_bad_ups 	= loadLimits( map, "lbus" );
				last_bad_downs 	= loadLimits( map, "lbds" );
				
				up_capacity 	= loadLimit((Map)map.get( "upcap" ));
				down_capacity 	= loadLimit((Map)map.get( "downcap" ));
				
				log( "Loaded " + p.size() + " entries from " + history_file + ": bad_up=" + getLimitString(last_bad_ups) + ", bad_down=" + getLimitString(last_bad_downs));
				
				updateLimitEstimates();	
			}
				
			/*
			if ( variance ){
				
				Iterator	it = regions.iterator();
				
				while( it.hasNext()){
					
					region r = (region)it.next();
					
					if ( r.getMetric() >= VARIANCE_BAD_VALUE ){
					
						for (int i=0;i<r.getX1();i++){
						
							System.out.print( " " );
						}
					
						for (int i=r.getX1();i<=r.getX2();i++){
							
							System.out.print( "*" );
						}
						
						System.out.println( 
								": " + r.getMetric() + "/" + 
								DisplayFormatters.formatByteCountToKiBEtc(r.getX1()*SPEED_DIVISOR ) + "," +
								DisplayFormatters.formatByteCountToKiBEtc(r.getX2()*SPEED_DIVISOR ));
					}
				}
			}
			*/
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
		return( adjustForPersistence( getEstimatedLimit( up_estimate ), last_bad_up, persistent ));
	}
	
	public synchronized SpeedManagerLimitEstimate
	getEstimatedDownloadLimit(
		boolean	persistent )
	{
		return( adjustForPersistence( getEstimatedLimit( down_estimate ), last_bad_down, persistent ));
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
		
	protected synchronized limitEstimate
	getEstimatedLimit(
		limitEstimate[]	estimates )
	{
		if ( estimates.length == 0 ){
			
			return( null );
		}
		
		for (int i=0;i<estimates.length;i++){
			
			limitEstimate e = estimates[i];
			
			if ( e.getMetricRating() == -1 ){
				
				return( e );
			}
		}
		
		return( estimates[estimates.length-1] );
	}

	protected void
	updateLimitEstimates()
	{
		up_estimate 	= getEstimatedLimit( true );
		
		limitEstimate up = getEstimatedLimit( up_estimate );
		
		if ( up != null ){
			
			double metric = up.getMetricRating();
			
			if ( metric == -1 ){
			
				if ( !bad_up_in_progress ){
					
					last_bad_ups.addLast( up );
					
					if ( last_bad_ups.size() > MAX_BAD_LIMIT_HISTORY ){
						
						last_bad_ups.removeFirst();
					}
				}
				
				bad_up_in_progress	= true;
				
				last_bad_up = up;
				
			}else if ( metric == 1 ){
				
				bad_up_in_progress	= false;
			}
		}
		
		down_estimate 	= getEstimatedLimit( false );
		
		limitEstimate down = getEstimatedLimit( down_estimate );
		
		if ( down != null ){
			
			double metric = down.getMetricRating();
			
			if ( metric == -1 ){
			
				if ( !bad_down_in_progress ){
					
					last_bad_downs.addLast( down );
					
					if ( last_bad_downs.size() > MAX_BAD_LIMIT_HISTORY ){
						
						last_bad_downs.removeFirst();
					}
				}
				
				bad_down_in_progress	= true;
				
				last_bad_down = up;
				
			}else if ( metric == 1 ){
				
				bad_down_in_progress	= false;
			}	
		}
	}
	
	protected synchronized limitEstimate[]
	getEstimatedLimit(
		boolean		up )
	{
		if ( !variance ){
			
			return( new limitEstimate[0] );
		}
		
		int	num_samples = regions.size();
		
		if ( num_samples == 0 ){
			
			return( new limitEstimate[0] );
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
		
		int[]	samples;
		
		if ( num_samples >= SpeedManagerImpl.MEDIUM_ESTIMATE_SAMPLES ){
			
			samples = new int[]{ SpeedManagerImpl.SHORT_ESTIMATE_SAMPLES, SpeedManagerImpl.MEDIUM_ESTIMATE_SAMPLES, num_samples };
			
		}else if ( num_samples >= SpeedManagerImpl.SHORT_ESTIMATE_SAMPLES ){
			
			samples = new int[]{ SpeedManagerImpl.SHORT_ESTIMATE_SAMPLES, num_samples };

		}else{
			
			samples = new int[]{ num_samples };
		}
		
		limitEstimate[]	results = new limitEstimate[samples.length];
		
		for (int sample=0;sample<samples.length;sample++){
			
			int	sample_end = max_end + 1;
			
			int[]	totals 			= new int[sample_end];
			short[]	hits			= new short[sample_end];				
			short[] when			= new short[sample_end];
			short[]	worst_var_type	= new short[sample_end];
			
				// take the last 'n' samples (at end of list)
			
			int	sample_count = samples[sample];
			
			int	pos = num_samples - sample_count;
			
			ListIterator sample_it = regions.listIterator( pos );
				
				// flatten out all observations into a single munged metric

			for (int i=pos;i<num_samples;i++){

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
						when[j]		= 0;
						
						worst_var_type[j] = this_var_type;
					}
					
					totals[j] += metric;
					hits[j]++;
						
						// keep track of most recent observation pertaining to this value
					
					if ( i > when[j] ){
						
						when[j] = (short)i;
					}
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
			
				// now we look for the most recent worst area of contiguous badness
			
			int	estimate		= -1;
			int	estimate_when	= 0;
			int	estimate_hits	= -1;
								
			int	zone_start		= -1;
			int	zone_max_hit	= 0;
			int	zone_max_time	= 0;
			
			int	worst_var		= 0;
			
			int	last_average 		= -1;
			int	last_average_change	= 0;
			
			List segments = new ArrayList(totals.length);
			
			for (int i=0;i<sample_end;i++){
				
				int var		= worst_var_type[i];
				int	hit 	= hits[i];
				
				int average = totals[i];
				
				if ( i == 0 ){
					
					last_average = average;
					
				}else if ( last_average != average ){
					
					segments.add( new int[]{ last_average, last_average_change*SPEED_DIVISOR, (i-1)*SPEED_DIVISOR });
					
					last_average 		= average;
					last_average_change	= i;
				}
				
				if ( var >= worst_var ){
					
					if ( var > worst_var || zone_start == -1 ){
				
						// start a new zone and discard any previous results as things have got worse
						
						worst_var		= var;
						
						zone_start 		= i;
						zone_max_hit	= hit;
						zone_max_time	= 0;
						
						estimate_when	= 0;	// forget any previous zone stats
						
					}else{
						
							// continuation of zone
					
						zone_max_hit = Math.max( zone_max_hit, hit );
					}
					
						// keep track of most recent contribution to this zone
					
					int	w = when[i];
					
					if ( w > zone_max_time ){
						
						zone_max_time = w;
					}
				}else{
					
						// zone ended - capture details if this is more recent
					
					if ( zone_start != -1 ){
						
						if ( zone_max_time > estimate_when ){
						
								// if zone has contiguous time region at start then take middle of this
								// when bad variance as we want to err on the side of caution
							
							if ( worst_var == VARIANCE_BAD_VALUE ){
								
								estimate 		= zone_start;
								
							}else if ( worst_var == VARIANCE_GOOD_VALUE ){
								
								estimate 		= zone_start + (i-1-zone_start)/2;
								
							}else{
								
								estimate		= i-1;
							}
							
							estimate_when	= zone_max_time;
							estimate_hits	= zone_max_hit;
						}
						
						zone_start		= -1;
						zone_max_hit	= 0;
						zone_max_time	= 0;
					}
				}
			}
			
			if ( zone_start != -1 ){
				
					// capture any trailing zone
				
				if ( zone_max_time > estimate_when ){
				
					if ( worst_var == VARIANCE_BAD_VALUE ){
						
						estimate 		= zone_start;
										
					}else if ( worst_var == VARIANCE_GOOD_VALUE ){
						
						estimate 		= zone_start + (sample_end-1-zone_start)/2;

					}else{
						
						estimate		= sample_end-1;
					}
					
					estimate_when	= zone_max_time;
					estimate_hits	= zone_max_hit;
				}
			}
			
			if ( last_average_change != sample_end - 1 ){
			
				segments.add( new int[]{ last_average, last_average_change*SPEED_DIVISOR, (sample_end-1)*SPEED_DIVISOR });
			}
			
			results[sample] = 
				new limitEstimate(
						estimate==-1?-1:(estimate*SPEED_DIVISOR),
						convertMetricToRating( worst_var ), 
						estimate_hits, 
						SystemTime.getCurrentTime(),
						(int[][])segments.toArray(new int[segments.size()][]));
		}
		
		String	str = "";
					
		for (int i=0;i<results.length;i++){
			
			limitEstimate	r = results[i];
			
			str += (i==0?"":",") + r.getString();
		}
		
		if ( variance ){
		
			log( "Estimate (samples=" + num_samples + ")" + (up?"up":"down") + "->" + str );
		}
		
		return( results );
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
	
	public SpeedManagerLimitEstimate
	getEstimatedDownloadCapacityBytesPerSec()
	{
		return( down_capacity );
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
		
		public double
		getMetricRating()
		{
			return( metric_rating );
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
		SpeedManagerPingMapperImpl pm = new SpeedManagerPingMapperImpl( null, "test", 100, true );
		
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