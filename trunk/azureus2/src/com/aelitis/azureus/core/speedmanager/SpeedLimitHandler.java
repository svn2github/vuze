/*
 * Created on Feb 3, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.speedmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.stats.transfer.LongTermStats;
import org.gudy.azureus2.core3.stats.transfer.LongTermStatsListener;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAttributeListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.network.RateLimiter;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerManagerEvent;
import org.gudy.azureus2.plugins.peers.PeerManagerListener2;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagFeature;
import com.aelitis.azureus.core.tag.TagFeatureRateLimit;
import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagPeer;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.impl.TagBase;
import com.aelitis.azureus.core.tag.impl.TagTypeWithState;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.util.average.AverageFactory;

public class 
SpeedLimitHandler 
	implements LongTermStatsListener
{
	private static SpeedLimitHandler		singleton;
	
	public static SpeedLimitHandler
	getSingleton(
		AzureusCore		core )
	{
		synchronized( SpeedLimitHandler.class ){
			
			if ( singleton == null ){
				
				singleton = new SpeedLimitHandler( core );
			}
			
			return( singleton );
		}
	}
	
	private AzureusCore			core;
	private PluginInterface 	plugin_interface;
	private TorrentAttribute	category_attribute;
	
	private LoggerChannel	logger;
	
	private TimerEventPeriodic		schedule_event;
	private List<ScheduleRule>		current_rules	= new ArrayList<ScheduleRule>();
	private ScheduleRule			active_rule;
	
	
	private Map<String,IPSet>		current_ip_sets 			= new HashMap<String,IPSet>();
	private Map<String,RateLimiter>	ip_set_rate_limiters_up 	= new HashMap<String,RateLimiter>();
	private Map<String,RateLimiter>	ip_set_rate_limiters_down 	= new HashMap<String,RateLimiter>();
	private TimerEventPeriodic		ip_set_event;

	private boolean					net_limit_listener_added;
	
	private Map<Integer,long[]>		net_limits	= new HashMap<Integer, long[]>();
	
	private List<String> predefined_profile_names = new ArrayList<String>();
	
	{
		predefined_profile_names.add( "pause_all" );
		predefined_profile_names.add( "resume_all" );
	}
	
	private boolean rule_pause_all_active;
	private boolean net_limit_pause_all_active;
	
	private IPSetTagType	ip_set_tag_type = new IPSetTagType();
	
	private
	SpeedLimitHandler(
		AzureusCore		_core )
	{
		core 	= _core;
		
		plugin_interface = core.getPluginManager().getDefaultPluginInterface();
		
		category_attribute	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_CATEGORY );

		logger = plugin_interface.getLogger().getTimeStampedChannel( "Speed Limit Handler" );
		
		UIManager	ui_manager = plugin_interface.getUIManager();

		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( "Speed Limit Handler" );

		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		logger.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						model.getLogArea().appendText( error.toString()+"\n");
					}
				});
		
		loadPauseAllActive();
		
		loadSchedule();
	}
	
	private synchronized Map
	loadConfig()
	{
		return( BEncoder.cloneMap( COConfigurationManager.getMapParameter( "speed.limit.handler.state", new HashMap())));
	}
	
	private synchronized void
	saveConfig(
		Map		map )
	{
		COConfigurationManager.setParameter( "speed.limit.handler.state", map );
		
		COConfigurationManager.save();
	}
	
	private void
	loadPauseAllActive()
	{
		setRulePauseAllActive( COConfigurationManager.getBooleanParameter( "speed.limit.handler.schedule.pa_active", false ));
		
		setNetLimitPauseAllActive( COConfigurationManager.getBooleanParameter( "speed.limit.handler.schedule.nl_pa_active", false ));
	}
	
	private void
	setRulePauseAllActive(
		boolean	active )
	{
		GlobalManager gm = core.getGlobalManager();

		if ( active ){
			
			if ( !rule_pause_all_active ){
				
				logger.logAlertRepeatable(
						LoggerChannel.LT_INFORMATION,
						"Pausing all downloads due to pause_all rule" );
			}
			
			gm.pauseDownloads();

			rule_pause_all_active = true;
			
		}else{

			if ( !net_limit_pause_all_active ){
			
				if ( COConfigurationManager.getBooleanParameter( "speed.limit.handler.schedule.pa_capable", false )){

					if ( rule_pause_all_active ){
						
						logger.logAlertRepeatable(
								LoggerChannel.LT_INFORMATION,
								"Resuming all downloads as pause_all rule no longer applies" );
					}
					
					gm.resumeDownloads( true );
				}
			}
			
			rule_pause_all_active = false;
		}
		
		COConfigurationManager.setParameter( "speed.limit.handler.schedule.pa_active", active );
	}
	
	private void
	setNetLimitPauseAllActive(
		boolean	active )
	{
		GlobalManager gm = core.getGlobalManager();

		if ( active ){
			
			if ( !net_limit_pause_all_active ){
				
				logger.logAlertRepeatable(
					LoggerChannel.LT_INFORMATION,
					"Pausing all downloads as network limit exceeded" );
			}
			
			gm.pauseDownloads();

			net_limit_pause_all_active = true;
			
		}else{

			if ( !rule_pause_all_active ){
			
				if ( COConfigurationManager.getBooleanParameter( "speed.limit.handler.schedule.pa_capable", false )){

					if ( net_limit_pause_all_active ){
						
						logger.logAlertRepeatable(
							LoggerChannel.LT_INFORMATION,
							"Resuming all downloads as network limit no longer exceeded" );
					}
					
					gm.resumeDownloads( true );
				}
			}
			
			net_limit_pause_all_active = false;
		}
		
		COConfigurationManager.setParameter( "speed.limit.handler.schedule.nl_pa_active", active );
	}
	
	public List<String>
	reset()
	{		
		if ( net_limit_pause_all_active ){
			
			setNetLimitPauseAllActive( false );
		}
		
		return( resetRules());
	}
	
	private List<String>
	resetRules()
	{
		if ( rule_pause_all_active ){
			
			setRulePauseAllActive( false );
		}
				
		LimitDetails details = new LimitDetails();
		
		details.loadForReset();
		
		details.apply();
		
		return( details.getString( true, false ));
	}
	
	public List<String>
	getCurrent()
	{
		LimitDetails details = new LimitDetails();
		
		details.loadCurrent();
		
		List<String> lines = details.getString( true, false );
		
		lines.add( "" );
		lines.add( "IP Sets" );
		if ( current_ip_sets.size() == 0 ){
			lines.add( "    None" );
		}else{
			for( Map.Entry<String,IPSet> entry: current_ip_sets.entrySet()){
				lines.add( "    " + entry.getValue().getDetailString());
			}
		}
		
		ScheduleRule rule = active_rule;
		
		lines.add( "" );
		lines.add( "Scheduler" );
		lines.add( "    Rules defined: " + current_rules.size());
		lines.add( "    Active rule: " + (rule==null?"None":rule.getString()));
		
		lines.add( "" );
		lines.add( "Network Totals" );
		
		LongTermStats lt_stats = StatsFactory.getLongTermStats();
		
		if ( lt_stats == null || !lt_stats.isEnabled()){
			
			lines.add( "    Not Available" );
			
		}else{
			
			lines.add( "    Today:\t\t" + getString( lt_stats.getTotalUsageInPeriod( LongTermStats.PT_CURRENT_DAY ), net_limits.get( LongTermStats.PT_CURRENT_DAY ), false ));
			lines.add( "    This week:\t" + getString( lt_stats.getTotalUsageInPeriod( LongTermStats.PT_CURRENT_WEEK ), net_limits.get( LongTermStats.PT_CURRENT_WEEK ), false ));
			lines.add( "    This month:\t" + getString( lt_stats.getTotalUsageInPeriod( LongTermStats.PT_CURRENT_MONTH ), net_limits.get( LongTermStats.PT_CURRENT_MONTH ), false ));
			lines.add( "" );
			lines.add( "    Rate (3 minute average):\t\t" + getString( lt_stats.getCurrentRateBytesPerSecond(), null, true));
		}
		
		return( lines );
	}
	
	private String
	getString(
		long[]		stats,
		long[]		limits,
		boolean		is_rate )
	{
		long total_up = stats[LongTermStats.ST_PROTOCOL_UPLOAD] + stats[LongTermStats.ST_DATA_UPLOAD] + stats[LongTermStats.ST_DHT_UPLOAD];
		long total_do = stats[LongTermStats.ST_PROTOCOL_DOWNLOAD] + stats[LongTermStats.ST_DATA_DOWNLOAD] + stats[LongTermStats.ST_DHT_DOWNLOAD];
		
		String	lim_str = "";
		
		if ( limits != null ){
			
			long total_lim 	= limits[0];
			long up_lim		= limits[1];
			long down_lim	= limits[2];
			
			if ( total_lim > 0 ){
				
				lim_str += "Total=" + DisplayFormatters.formatByteCountToKiBEtc( total_lim ) + " " + (100*(total_up+total_do)/total_lim) + "%";
			}
			if ( up_lim > 0 ){
				
				lim_str += (lim_str.length()==0?"":", ") + "Up=" + DisplayFormatters.formatByteCountToKiBEtc( up_lim ) + " " + (100*(total_up)/up_lim) + "%";
			}
			if ( down_lim > 0 ){
				
				lim_str += (lim_str.length()==0?"":", ") + "Down=" + DisplayFormatters.formatByteCountToKiBEtc( down_lim ) + " " + (100*(total_do)/down_lim) + "%";
			}

			if ( lim_str.length() > 0 ){
				
				lim_str = "\t[ Limits: " + lim_str + "]";
			}
		}
		
		if ( is_rate ){
		
			return( "Upload=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( total_up ) + ", Download=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( total_do ));
			
		}else{
			
			return( "Upload=" + DisplayFormatters.formatByteCountToKiBEtc( total_up ) + ", Download=" + DisplayFormatters.formatByteCountToKiBEtc( total_do ) + lim_str );
		}
	}
	
	public List<String>
	getProfileNames()
	{
		Map	map = loadConfig();
				
		List<String> profiles = new ArrayList<String>();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	name = importString( m, "n" );
				
				if ( name != null ){
					
					profiles.add( name );
				}
			}
		}

		return( profiles );
	}
	
	public List<String>
	loadProfile(
		String		name )
	{		
		Map	map = loadConfig();
				
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
					
					ld.apply();
					
					return( ld.getString( false, false ));
				}
			}
		}
		
		List<String> result = new ArrayList<String>();
		
		result.add( "Profile not found" );

		return( result );
	}
	
	private boolean
	profileExists(
		String		name )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					return( true );
				}
			}
		}
		
		return( false );
	}
	
	public List<String>
	getProfile(
		String		name )
	{
		return( getProfileSupport( name, false ));
	}
	
	public List<String>
	getProfileSupport(
		String		name,
		boolean		use_hashes )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
										
					return( ld.getString( false, use_hashes ));
				}
			}
		}
		
		List<String> result = new ArrayList<String>();
		
		result.add( "Profile not found" );

		return( result );
	}
	
	public List<String>
	getProfilesForDownload(
		byte[]		hash )
	{
		List<String> result = new ArrayList<String>();

		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			String	hash_str = Base32.encode( hash );
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null ){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
										
					if ( ld.getLimitsForDownload( hash_str ) != null ){
						
						result.add( p_name );
					}
				}
			}
		}
	
		return( result );
	}
	
	private void
	addRemoveDownloadsToProfile(
		String			name,
		List<byte[]>	hashes,
		boolean			add )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		List<String>	hash_strs = new ArrayList<String>();
		
		for ( byte[] hash: hashes ){
			
			hash_strs.add( Base32.encode( hash ));
		}
		
		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
					
					ld.addRemoveDownloads( hash_strs, add );
					
					m.put( "p", ld.export());
					
					saveConfig( map );

					return;
				}
			}
		}
	}
	
	public void
	addDownloadsToProfile(
		String			name,
		List<byte[]>	hashes )
	{
		addRemoveDownloadsToProfile( name, hashes, true );
	}
	
	public void
	removeDownloadsFromProfile(
		String			name,
		List<byte[]>	hashes )
	{
		addRemoveDownloadsToProfile( name, hashes, false );
	}
	
	public void
	deleteProfile(
		String		name )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					list.remove( m );
					
					saveConfig( map );
					
					return;
				}
			}
		}
	}
	
	public List<String>
	saveProfile(
		String		name )
	{
		LimitDetails details = new LimitDetails();
		
		details.loadCurrent();
		
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list == null ){
			
			list = new ArrayList<Map>();
			
			map.put( "profiles", list );
		}
		
		for ( Map m: list ){
			
			String	p_name = importString( m, "n" );
			
			if ( p_name != null && name.equals( p_name )){
				
				list.remove( m );
				
				break;
			}
		}
		
		Map m = new HashMap();
		
		list.add( m );
		
		m.put( "n", name );
		m.put( "p", details.export());
		
		saveConfig( map );
		
		ScheduleRule	rule;
		
		synchronized( this ){
			
			rule = active_rule;
		}
		
		if ( rule != null && rule.profile_name.equals( name )){
			
			details.apply();
		}
		
		return( details.getString( false, false ));
	}
	
	private synchronized List<String>
	loadSchedule()
	{
		List<String>	result = new ArrayList<String>();
		
		List list = COConfigurationManager.getListParameter( "speed.limit.handler.schedule.lines", new ArrayList());
		List<String> schedule_lines = BDecoder.decodeStrings( BEncoder.cloneList(list) );
	
		boolean	enabled = true;
		
		List<ScheduleRule>	rules 	= new ArrayList<ScheduleRule>();
		Map<String,IPSet>	ip_sets	= new HashMap<String, IPSet>();
		
		Map<Integer,long[]> new_net_limits = new HashMap<Integer, long[]>();

		boolean checked_lts_enabled = false;
		boolean	lts_enabled	= false;
		
		for ( String line: schedule_lines ){
			
			line = line.trim();
			
			if ( line.length() == 0 || line.startsWith( "#" )){
				
				continue;
			}
			
			String lc_line = line.toLowerCase( Locale.US );
			
			if ( lc_line.startsWith( "enable" )){
				
				String[]	bits = lc_line.split( "=" );
				
				boolean	ok = false;
				
				if ( bits.length == 2 ){
					
					String arg = bits[1];
					
					if ( arg.equals( "yes" )){
						
						enabled = true;
						ok		= true;
						
					}else if ( arg.equals( "no" )){
						
						enabled = false;
						ok		= true;
					}
				}
				
				if ( !ok ){
					
					result.add( "'" +line + "' is invalid: use enable=(yes|no)" );
				}
			}else if ( lc_line.startsWith( "ip_set" )){

				try{
						// uppercase here as category names are case sensitive..
					
					String[] args = line.substring(6).split( "," );
					
					boolean	inverse 	= false;
					int		up_lim		= -1;
					int		down_lim	= -1;
					
					List<String>	categories = new ArrayList<String>();
					
					IPSet set = null;
					
					for ( String arg: args ){
						
						String[]	bits = arg.split( "=" );
		
						if ( bits.length != 2 ){
							
							throw( new Exception( "Expected <key>=<value> for '" + arg + "'" ));
							
						}else{
							
							String lhs		= bits[0].trim();
							String lc_lhs	= lhs.toLowerCase( Locale.US );
							String rhs 		= bits[1].trim();
							
							String lc_rhs = rhs.toLowerCase( Locale.US );

							if ( lc_lhs.equals( "inverse" )){
							
								inverse = lc_rhs.equals( "yes" );
								
							}else if ( lc_lhs.equals( "up" )){
								
								up_lim = (int)parseRate( lc_rhs );
								
							}else if ( lc_lhs.equals( "down" )){
								
								down_lim = (int)parseRate( lc_rhs );
								
							}else if ( lc_lhs.equals( "cat" )){
								
								String[] cats = rhs.split( " " );
								
								for ( String cat: cats ){
									
									cat = cat.trim();
									
									if ( cat.length() > 0 ){
										
										if ( categories == null ){
											
											categories = new ArrayList<String>();
										}
										
										categories.add( cat );
									}
								}
							}else{
								
								String name = lhs;
							
								String def = rhs.replace(';', ' ');
														
								set = ip_sets.get( name );
								
								if ( set == null ){
									
									set = new IPSet( name );
									
									ip_sets.put( name, set );
								}
								
								bits = def.split( " " );
								
								for ( String bit: bits ){
									
									bit = bit.trim();
									
									if ( bit.length() > 0 ){
										
										IPSet other_set = ip_sets.get( bit );
										
										if ( other_set != null && other_set != set ){
											
											set.addSet( other_set );
											
										}else{
											
											if ( !set.addCIDRorCC( bit )){
											
												result.add( "CIDR, CC or ip_set reference '" + bit + "' isn't valid" );
											}
										}
									}
								}
							}
						}
					}
					
					if ( set == null ){
						
						throw( new Exception());
					}
					
					set.setParameters( inverse, up_lim, down_lim, categories );
					
				}catch( Throwable e ){
					
					result.add( "'" +line + "' is invalid: use ip_set <name>=<cidrs...> [,inverse=[yes|no]] [,up=<limit>] [,down=<limit>] [,cat=<categories>]: " + e.getMessage());
				}
			}else if ( lc_line.startsWith( "net_limit" )){

				if ( !checked_lts_enabled ){
				
					checked_lts_enabled = true;
					
					lts_enabled = StatsFactory.getLongTermStats().isEnabled();
					
					if ( !lts_enabled ){
						
						result.add( "Long-term stats are currently disabled, limits will NOT be applied" );
					}
				}
				
				line = lc_line.substring(9).replace( ",", " " );
				
				String[] args = line.split( " " );
				
				int		type = -1;
				
				long	total_lim	= 0;
				long	up_lim		= 0;
				long	down_lim	= 0;
								
				for ( String arg: args ){
					
					arg = arg.trim();
					
					if ( arg.length() == 0 ){
						
						continue;
					}
					
					if ( type == -1 ){
						
						if ( arg.equalsIgnoreCase( "daily" )){
							
							type = LongTermStats.PT_CURRENT_DAY;
							
						}else if ( arg.equalsIgnoreCase( "weekly" )){
							
							type = LongTermStats.PT_CURRENT_WEEK;
							
						}else if ( arg.equalsIgnoreCase( "monthly" )){
							
							type = LongTermStats.PT_CURRENT_MONTH;
							
						}else{
							
							result.add( "net_limit type of '" + arg + "' not recognised - use daily, weekly or monthly" );
							
							break;
						}
					}else{
					
						String[]	bits = arg.split( "=" );
		
						if ( bits.length != 2 ){
							
							result.add( "'" + line + "': invalid net_limit specification" );
							
						}else{
							
							String lhs = bits[0];
							String rhs = bits[1];
							
							long lim = parseRate( rhs );
							
							if ( lhs.equalsIgnoreCase( "total" )){
								
								total_lim = lim;
								
							}else if ( lhs.equalsIgnoreCase( "up" )){
									
								up_lim = lim;
								
							}else if ( lhs.equalsIgnoreCase( "down" )){
								
								down_lim = lim;
								
							}else{
								
								result.add( "'" + line + "': invalid net_limit specification" );
							}
						}
					}
				}
				
				if ( type != -1 ){
					
					new_net_limits.put( type, new long[]{ total_lim, up_lim, down_lim } );
				}				
			}else{
				
				String[]	_bits = line.split( " " );
							
				List<String>	bits = new ArrayList<String>();
				
				for ( String b: _bits ){
					
					b = b.trim();
					
					if ( b.length() > 0 ){
						
						bits.add( b );
					}
				}
				
				List<String>	errors = new ArrayList<String>();
				
				if ( bits.size() == 6 ){
					
					String	freq_str = bits.get(0).toLowerCase( Locale.US );
					
					byte	freq = 0;
					
					if ( freq_str.equals( "daily" )){
						
						freq = ScheduleRule.FR_DAILY;
						
					}else if ( freq_str.equals( "weekdays" )){
						
						freq = ScheduleRule.FR_WEEKDAY;
						
					}else if ( freq_str.equals( "weekends" )){
						
						freq = ScheduleRule.FR_WEEKEND;
						
					}else if ( freq_str.equals( "mon" )){
						
						freq = ScheduleRule.FR_MON;
						
					}else if ( freq_str.equals( "tue" )){
						
						freq = ScheduleRule.FR_TUE;
						
					}else if ( freq_str.equals( "wed" )){
						
						freq = ScheduleRule.FR_WED;
						
					}else if ( freq_str.equals( "thu" )){
						
						freq = ScheduleRule.FR_THU;
						
					}else if ( freq_str.equals( "fri" )){
						
						freq = ScheduleRule.FR_FRI;
						
					}else if ( freq_str.equals( "sat" )){
						
						freq = ScheduleRule.FR_SAT;
						
					}else if ( freq_str.equals( "sun" )){
						
						freq = ScheduleRule.FR_SUN;
						
					}else{
						
						errors.add( "frequency '" + freq_str + "' is invalid" );
					}
					
					String	profile = bits.get(1);
					
					if ( !profileExists( profile ) && !predefined_profile_names.contains( profile.toLowerCase())){
						
						errors.add( "profile '" + profile + "' not found" );
						
						profile = null;
					}
					
					int from_mins = -1;
					
					if ( bits.get(2).equalsIgnoreCase( "from" )){
						
						from_mins = getMins( bits.get(3));
					}
					
					if ( from_mins == -1 ){
						
						errors.add( "'from' is invalid" );
					}
					
					int to_mins = -1;
					
					if ( bits.get(4).equalsIgnoreCase( "to" )){
						
						to_mins = getMins( bits.get(5));
					}
					
					if ( to_mins == -1 ){
						
						errors.add( "'to' is invalid" );
					}
					
					if ( errors.size() == 0 ){
						
						rules.add( new ScheduleRule( freq, profile, from_mins, to_mins ));
						
					}else{
						
						String	err_str = "";
						
						for ( String e: errors ){
							
							err_str += (err_str.length()==0?"":", ") + e;
						}
						
						result.add( "'" + line + "' is invalid (" + err_str + ") - use <frequency> <profile> from <hh:mm> to <hh:mm>" );
					}
				}else{
					
					result.add( "'" + line + "' is invalid: use <frequency> <profile> from <hh:mm> to <hh:mm>" );
				}
			}
		}
		
			// schedule fully loaded into local variables
			// handle overall changes in pause/resume features, in particular to disable them if
			// the schedule no longer controls them
		
		boolean	schedule_has_net_limits = false;
		boolean	schedule_has_pausing 	= false;
		
		if ( enabled ){
			
			if ( new_net_limits.size() > 0 ){
				
				schedule_has_net_limits = true;
			}
			
			for ( ScheduleRule rule: rules ){
					
				String profile_name = rule.profile_name;
					
				if ( profile_name.equalsIgnoreCase( "pause_all" ) || profile_name.equalsIgnoreCase( "resume_all" )){
						
					schedule_has_pausing = true;
						
					break;
				}
			}
		}
		
		if ( !schedule_has_pausing ){
				
			setRulePauseAllActive( false );
		}
		
		if ( !schedule_has_net_limits ){
		
			setNetLimitPauseAllActive( false );
		}
		
			// this marker is used to prevent unwanted 'resumeAll' operations being performed by the
			// scheduler when it is enabled but doesn't have any features that could warrant this. This
			// allows manual pause states to be respected. Of course we should probably differeniate between
			// manually paused downloads and those auto-paused to generally support this better, but
			// that would take a bit of effort to persistently remember this....
		
		COConfigurationManager.setParameter( "speed.limit.handler.schedule.pa_capable", enabled && ( schedule_has_pausing || schedule_has_net_limits ));
		
		if ( enabled ){
			
			current_rules = rules;
			
			if ( schedule_event == null && ( rules.size() > 0 || net_limits.size() > 0 )){
				
				schedule_event = 
					SimpleTimer.addPeriodicEvent(
						"speed handler scheduler",
						30*1000,
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent event) 
							{
								checkSchedule();
							}
						});
			}
			
			if ( active_rule != null || rules.size() > 0 || net_limits.size() > 0 ){
			
				checkSchedule();
			}
			
			for( IPSet s: current_ip_sets.values()){
				
				s.destroy();
			}
			
			current_ip_sets = ip_sets;
			
			Map<IPSet,Integer>	id_map 		= new HashMap<IPSet, Integer>();
			int					id_max		= -1;
			
			for ( int i=0;i<2;i++ ){
				
				for ( IPSet s: current_ip_sets.values()){
					
					String name = s.getName();
					
					try{
						String config_key = "speed.limit.handler.ipset_n." + Base32.encode( name.getBytes( "UTF-8" ));
						
						if ( i == 0 ){
							
							int existing = COConfigurationManager.getIntParameter( config_key, -1 );
							
							if ( existing != -1 ){
								
								id_map.put( s, existing );
								
								id_max = Math.max( id_max, existing );
							}
						}else{
							
							Integer tag_id = id_map.get( s );
							
							if ( tag_id == null ){
								
								tag_id = ++id_max;
								
								COConfigurationManager.setParameter( config_key, tag_id );
							}
							
							s.initialise( tag_id );
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
			
			checkIPSets();
			
			if ( !lts_enabled ){
				
				new_net_limits.clear();
			}
			
			net_limits = new_net_limits;

			if ( net_limits.size() > 0 ){
				
				if ( !net_limit_listener_added ){
					
					net_limit_listener_added = true;
					
					StatsFactory.getLongTermStats().addListener( 1024*1024, this );
				}
				
				updated( StatsFactory.getLongTermStats());
				
			}else{
				
				if ( net_limit_listener_added ){
					
					net_limit_listener_added = false;
					
					StatsFactory.getLongTermStats().removeListener( this );
				}
			}
		}else{
	
			current_rules.clear();
			
			if ( schedule_event != null ){
				
				schedule_event.cancel();
				
				schedule_event = null;
			}
			
			if ( active_rule != null ){
				
				active_rule	= null;
				
				resetRules();
			}
			
			for( IPSet s: current_ip_sets.values()){
				
				s.destroy();
			}

			current_ip_sets.clear();
			
			checkIPSets();
			
			if ( net_limit_pause_all_active ){
				
				setNetLimitPauseAllActive( false );
			}
			
			net_limits.clear();
			
			if ( net_limit_listener_added ){
				
				net_limit_listener_added = false;
				
				StatsFactory.getLongTermStats().removeListener( this );
			}
		}
		
		return( result );
	}
	
	private long
	parseRate(
		String	str )
	{
		int	pos = str.indexOf( "/" );
		
		if ( pos != -1 ){
			
			str = str.substring( 0, pos ).trim();
		}
	
		String	 	num 	= "";
		long		mult	= 1;
		
		for ( int i=0;i<str.length();i++){
			
			char c = str.charAt(i);
			
			if ( Character.isDigit( c ) || c == '.' ){
				
				num += c;
				
			}else{
				
				if ( c == 'k' ){
					
					mult = 1024;
					
				}else if ( c == 'm' ){
					
					mult = 1024*1024;
					
				}else if ( c == 'g' ){
					
					mult = 1024*1024*1024L;
				}
				
				break;
			}
		}
		
		if ( num.contains( "." )){
			
			return((long)( Float.parseFloat( num ) * mult ));
			
		}else{
			
			return( Integer.parseInt( num ) * mult );
		}
	}
	
	private int
	getMins(
		String	str )
	{
		try{
			String[]	bits = str.split( ":" );
			
			if ( bits.length == 2 ){
				
				return( Integer.parseInt(bits[0].trim())*60 + Integer.parseInt(bits[1].trim()));
			}
		}catch( Throwable e ){
		}
		
		return( -1 );
	}
	
	private DownloadManagerListener dml;
	private static Object	ip_set_peer_key = new Object();

	private synchronized void
	checkIPSets()
	{
		final org.gudy.azureus2.plugins.download.DownloadManager download_manager = plugin_interface.getDownloadManager();
		
		Download[] downloads = download_manager.getDownloads();
		
		for ( Download dm: downloads ){
			
			PeerManager pm = dm.getPeerManager();
			
			if ( pm != null ){
				
				Peer[] peers = pm.getPeers();
				
				for ( Peer peer: peers ){
					
					RateLimiter[] lims = peer.getRateLimiters( false );
					
					for ( RateLimiter l: lims ){
						
						if ( ip_set_rate_limiters_down.containsValue( l )){
							
							peer.removeRateLimiter( l , false );
						}
					}
					
					lims = peer.getRateLimiters( true );
					
					for ( RateLimiter l: lims ){
						
						if ( ip_set_rate_limiters_up.containsValue( l )){
							
							peer.removeRateLimiter( l , true );
						}
					}
				}
			}
		}
		
		ip_set_rate_limiters_down.clear();
		ip_set_rate_limiters_up.clear();
		
		boolean	has_cats = false;
		
		for ( IPSet set: current_ip_sets.values()){
			
			ip_set_rate_limiters_down.put( set.getName(), set.getDownLimiter());
			
			ip_set_rate_limiters_up.put( set.getName(), set.getUpLimiter());
			
			if ( set.getCategories() != null ){
				
				has_cats = true;
			}
		}
		
		if ( current_ip_sets.size() == 0 ){
			
			if ( ip_set_event != null ){
				
				ip_set_event.cancel();
				
				ip_set_event = null;
				
			}
			if ( dml != null ){
				
				download_manager.removeListener( dml );
				
				dml = null;
			}
		}else{
			
			if ( ip_set_event == null ){
				
				ip_set_event = 
					SimpleTimer.addPeriodicEvent(
						"speed handler ip set scheduler",
						1000,
						new TimerEventPerformer()
						{
							private int	tick_count;
							
							public void 
							perform(
								TimerEvent event) 
							{
								tick_count++;
								
								synchronized( current_ip_sets){
									
									for ( IPSet set: current_ip_sets.values()){
										
										set.updateStats( tick_count );
									}
									
									/*
									if ( tick_count % 30 == 0 ){

										String str = "";
										
										for ( IPSet set: current_ip_sets.values()){
											
											str += (str.length()==0?"":", ") + set.getString();
										}
										
										logger.log( str );
									}
									*/
								}
							}
						});
			}
			
			if ( dml != null ){
				
				download_manager.removeListener( dml );
			}
			
			final boolean	f_has_cats = has_cats;
			
			dml = 
				new DownloadManagerListener()
				{
					final DownloadManagerListener this_dml = this;
					
					final DownloadAttributeListener attr_listener = new
						DownloadAttributeListener()
						{
							public void 
							attributeEventOccurred(
								Download 			download, 
								TorrentAttribute 	attribute, 
								int 				event_type )
							{
								if ( dml != this_dml ){
									
									download.removeAttributeListener( this, category_attribute, DownloadAttributeListener.WRITTEN );
									
									return;
								}
								
								checkIPSets();
							}
						};
						
					public void
					downloadAdded(
						final Download	download )
					{
						if ( dml != this_dml ){
							
							download_manager.removeListener( this );
							
							return;
						}
						
						if ( f_has_cats ){
							
							download.addAttributeListener(
								attr_listener,
								category_attribute,
								DownloadAttributeListener.WRITTEN );
						}
							
						download.addPeerListener(
							new DownloadPeerListener()
							{
								public void
								peerManagerAdded(
									final Download			download,
									final PeerManager		peer_manager )
								{
									if ( dml != this_dml ){
										
										download.removePeerListener( this );
										
										return;
									}
									
									peer_manager.addListener(
										new PeerManagerListener2()
										{
											public void
											eventOccurred(
												PeerManagerEvent	event )
											{
												if ( dml != this_dml ){
													
													peer_manager.removeListener( this );
													
													return;
												}
												
												if ( event.getType() == PeerManagerEvent.ET_PEER_ADDED ){
													
													peersAdded( download, new Peer[]{ event.getPeer() });
													
												}else if ( event.getType() == PeerManagerEvent.ET_PEER_REMOVED ){
													
													peerRemoved( download, event.getPeer());
												}
											}
										});
									
									Peer[] peers = peer_manager.getPeers();
																				
									peersAdded( download, peers );
								}
								
								public void
								peerManagerRemoved(
									Download		download,
									PeerManager		peer_manager )
								{						
								}
							});
					}
				
						
					public void
					downloadRemoved(
						Download	download )
					{
					}
				};
			
			download_manager.addListener( dml, true );
		}
	}
	
	private void
	peersAdded(
		Download	download,
		Peer[]		peers )
	{
		IPSet[]		sets;
		long[][][]	set_ranges;
		Set[]		set_ccs;
		
		boolean	has_ccs = false;
		
		String	category = null;
		
		synchronized( current_ip_sets ){
			
			int	len = current_ip_sets.size();
			
			sets 		= new IPSet[len];
			set_ranges	= new long[len][][];
			set_ccs		= new Set[len];
			
			int	pos = 0;
			
			for ( IPSet set: current_ip_sets.values()){
				
				sets[pos]		= set;
				set_ranges[pos]	= set.getRanges();
				set_ccs[pos]	= set.getCountryCodes();
				
				if ( set_ccs[pos].size() > 0 ){
					
					has_ccs = true;
				}
				
				pos++;
				
				if ( category == null && set.getCategories() != null ){
				
					category = download.getAttribute( category_attribute );
				}
			}
		}
		
		if ( sets.length == 0 ){
			
			return;
		}
		
		for ( Peer peer: peers ){
			
			long[] entry = (long[])peer.getUserData( ip_set_peer_key );

			long	l_address;

			if ( entry == null ){
				
				l_address = 0;
				
				String ip = peer.getIp();
				
				if ( !ip.contains( ":" )){
					
					byte[] bytes = HostNameToIPResolver.hostAddressToBytes( ip );
					
					if ( bytes != null ){
						
						l_address = ((long)((bytes[0]<<24)&0xff000000 | (bytes[1] << 16)&0x00ff0000 | (bytes[2] << 8)&0x0000ff00 | bytes[3]&0x000000ff))&0xffffffffL;

					}
				}
				
				entry = new long[]{ l_address };
				
				peer.setUserData( ip_set_peer_key, entry );
				
			}else{
				
				l_address = entry[0];
			}
			
			String	peer_cc = null;
			
			if ( has_ccs ){
				
				String[] details = PeerUtils.getCountryDetails( peer );
				
				if ( details != null && details.length > 0 ){
					
					peer_cc = details[0];
				}	
			}
			
			Set<IPSet>	added_to_sets = new HashSet<IPSet>();
			
			if ( l_address != 0 ){
								
				for ( int i=0;i<set_ranges.length;i++ ){
					
					long[][] ranges = set_ranges[i];
					
					if ( ranges.length == 0 ){
						
						continue;
					}
					
					IPSet set = sets[i];
					
					boolean is_inverse = set.isInverse();
					
					List<String> set_cats = set.getCategories();
					
					if ( set_cats == null || set_cats.contains( category )){
					
						boolean	hit = false;
						
						for ( long[] range: ranges ){
							
							if ( l_address >= range[0] && l_address <= range[1] ){
															
								hit	= true;
	
								if ( !is_inverse ){
								
									addLimiters( peer, set );
									
									added_to_sets.add( set );
								}
	
								break;
							}
						}
						
						if ( is_inverse && !hit ){
							
							addLimiters( peer, set );
							
							added_to_sets.add( set );
						}
					}
				}
			}
			
			if ( peer_cc != null ){
				
				for ( int i=0;i<set_ccs.length;i++ ){
					
					IPSet set = sets[i];

					if ( added_to_sets.contains( set )){
						
						continue;
					}
					
					Set<String>	ccs = set_ccs[i];
					
					if ( ccs.size() == 0 ){
						
						continue;
					}
										
					boolean not_inverse = !set.isInverse();
					
					List<String> set_cats = set.getCategories();
					
					if ( set_cats == null || set_cats.contains( category )){
											
						boolean	hit = ccs.contains( peer_cc );
															
						if ( hit == not_inverse ){
							
							addLimiters( peer, set );
						}
					}
				}
			}
		}
	}

	private void
	peerRemoved(
		Download	download,
		Peer		peer )
	{
		Collection<IPSet> sets;
		
		synchronized( current_ip_sets ){
			
			if ( current_ip_sets.size() == 0 ){
				
				return;
			}
			
			sets = current_ip_sets.values();
		}
		
		for ( IPSet s: sets ){
			
			s.removePeer( peer );
		}
	}
	
	private void
	addLimiters(
		Peer	peer,
		IPSet	set )
	{
		boolean	matched = false;
		
		{
			RateLimiter l = set.getUpLimiter();
			
			RateLimiter[] existing = peer.getRateLimiters( true );
			
			boolean found = false;
			
			for ( RateLimiter e: existing ){
				
				if ( e == l ){
					
					found = true;
					
					break;
				}
			}
			if ( !found ){
														
				peer.addRateLimiter( l, true );
				
				matched = true;
			}
		}
		
		{
			RateLimiter l = set.getDownLimiter();
			
			RateLimiter[] existing = peer.getRateLimiters( false );
			
			boolean found = false;
			
			for ( RateLimiter e: existing ){
				
				if ( e == l ){
					
					found = true;
					
					break;
				}
			}
			if ( !found ){
													
				peer.addRateLimiter( l, false );
				
				matched = true;
			}
		}
		
		if ( matched ){
			
			set.addPeer( peer );
		}
	}
	
	private synchronized void
	checkSchedule()
	{
		Calendar cal = new GregorianCalendar();
		
		int	day_of_week = cal.get( Calendar.DAY_OF_WEEK );
		int	hour_of_day	= cal.get( Calendar.HOUR_OF_DAY );
		int min_of_hour	= cal.get( Calendar.MINUTE );
		
		int	day = -1;
		
		switch( day_of_week ){
		case Calendar.MONDAY:
			day = ScheduleRule.FR_MON;
			break;
		case Calendar.TUESDAY:
			day = ScheduleRule.FR_TUE;
			break;
		case Calendar.WEDNESDAY:
			day = ScheduleRule.FR_WED;
			break;
		case Calendar.THURSDAY:
			day = ScheduleRule.FR_THU;
			break;
		case Calendar.FRIDAY:
			day = ScheduleRule.FR_FRI;
			break;
		case Calendar.SATURDAY:
			day = ScheduleRule.FR_SAT;
			break;
		case Calendar.SUNDAY:
			day = ScheduleRule.FR_SUN;
			break;
		}
		
		int	min_of_day = hour_of_day * 60 + min_of_hour;
		
		ScheduleRule latest_match = null;
		
		for ( ScheduleRule main_rule: current_rules ){
			
			List<ScheduleRule>	sub_rules = main_rule.splitByDay();
			
			for ( ScheduleRule rule: sub_rules ){
			
				if (( rule.frequency | day ) == 0 ){
					
					continue;
				}
				
				if (	rule.from_mins <= min_of_day &&
						rule.to_mins >= min_of_day ){
					
					latest_match = main_rule;
				}
			}
		}
		
		GlobalManager gm = core.getGlobalManager();

		if ( latest_match == null ){
			
			active_rule = null;
			
			resetRules();
			
		}else{
			
			String	profile_name = latest_match.profile_name;
				
			boolean is_rule_pause_all = false;
			
			if ( active_rule == null || !active_rule.sameAs( latest_match )){
				
				String lc_profile_name = profile_name.toLowerCase();
				
				if ( predefined_profile_names.contains( lc_profile_name)){
					
					if ( lc_profile_name.equals( "pause_all" )){
						
						active_rule = latest_match;
						
						is_rule_pause_all = true;
						
						setRulePauseAllActive( true );
						
					}else if ( lc_profile_name.equals( "resume_all" )){
						
						active_rule = latest_match;
						
						setRulePauseAllActive( false );
						
					}else{
						
						Debug.out( "Unknown pre-def name '" + profile_name + "'" );
					}
					
				}else if ( profileExists( profile_name )){

					active_rule = latest_match;
				
					loadProfile( profile_name );
					
				}else{
					
					active_rule = null;
					
					resetRules();
				}
			}else{
				
				is_rule_pause_all = rule_pause_all_active;	// same rule as before
			}
			
			if ( rule_pause_all_active ){
				
				if ( !is_rule_pause_all ){
				
					setRulePauseAllActive( false );
							
				}else{
					
					if ( gm.canPauseDownloads()){
						
						gm.pauseDownloads();
					}
				}
			}
		}
			
		if ( net_limit_pause_all_active ){
				
			if ( gm.canPauseDownloads()){
					
				gm.pauseDownloads();
			}
		}
	}
	
	public List<String>
	getSchedule()
	{
		List<String>	result = new ArrayList<String>();
		
		result.add( "# Enter rules on separate lines below this section." );
		result.add( "# Rules are of the following types:" );
		result.add( "#    enable=(yes|no)   - controls whether the entire schedule is enabled or not (default=enabled)" );
		result.add( "#    <frequency> <profile_name> from <time> to <time>" );
		result.add( "#        frequency: daily|weekdays|weekends|<day_of_week>" );
		result.add( "#            days_of_week: mon|tue|wed|thu|fri|sat|sun" );
		result.add( "#        time: hh:mm - 24 hour clock; 00:00=midnight; local time" );
		result.add( "#    ip_set <ip_set_name> [<CIDR_specs...>|CC list|<prior_set_name>] [,inverse=[yes|no]] [,up=<limit>] [,down=<limit>] [cat=<cat names>]" );
		result.add( "#    net_limit [daily|weekly|monthly] total=<limit>|[up=<limit> down=<limit>]");
		result.add( "#" );
		result.add( "# For example - assuming there are profiles called 'no_limits' and 'limited_upload' defined:" );
		result.add( "#" );
		result.add( "#     daily no_limits from 00:00 to 23:59" );
		result.add( "#     daily limited_upload from 06:00 to 22:00" );
		result.add( "#     daily pause_all from 08:00 to 17:00" );
		result.add( "#" );
		result.add( "#     net_limit monthly total=250G" );
		result.add( "#" );
		result.add( "#     ip_set external=211.34.128.0/19 211.35.128.0/17" );
		result.add( "#     ip_set Europe=EU;AD;AL;AT;BA;BE;BG;BY;CH;CS;CZ;DE;DK;EE;ES;FI;FO;FR;FX;GB;GI;GR;HR;HU;IE;IS;IT;LI;LT;LU;LV;MC;MD;MK;MT;NL;NO;PL;PT;RO;SE;SI;SJ;SK;SM;UA;VA" );
		result.add( "#     ip_set Blorp=Europe;US" );
		result.add( "#" );
		result.add( "# When multiple rules apply the one further down the list of rules take precedence" );
		result.add( "# Currently ip_set limits are not schedulable" );
		result.add( "# Comment lines are prefixed with '#'" );
		result.add( "# Pre-defined profiles: " + predefined_profile_names );

		
		List<String> profiles = getProfileNames();
		
		if ( profiles.size() == 0 ){
			
			result.add( "# No user profiles currently defined." );
			
		}else{
			
			String	str = "";
			
			for( String s: profiles ){
				str += (str.length()==0?"":", ") + s;
			}
			
			result.add( "# Current profiles details:" );
			result.add( "#     defined: " + str );
			
			ScheduleRule	current_rule;
			
			synchronized( this ){
				
				current_rule = active_rule;
			}
		
			result.add( "#     active: " + (current_rule==null?"none":current_rule.profile_name ));
		}
		
		result.add( "# ---- Do not edit this line or any text above! ----" );
		
		List lines_list = COConfigurationManager.getListParameter( "speed.limit.handler.schedule.lines", new ArrayList());
		
		List<String> schedule_lines = BDecoder.decodeStrings(BEncoder.cloneList(lines_list) );
		
		if ( schedule_lines.size() == 0 ){
			
			schedule_lines.add( "" );
			schedule_lines.add( "" );
			
		}else{
		
			for ( String l: schedule_lines ){
			
				result.add( l.trim());
			}
		}
		
		return( result );
	}
	
	public List<String>
	setSchedule(
		List<String>		lines )
	{
		int	trim_from = 0;
		
		for ( int i=0; i<lines.size(); i++ ){
			
			String	line = lines.get( i );
			
			if ( line.startsWith( "# ---- Do not edit" )){
				
				trim_from = i+1;
			}
		}
		
		if ( trim_from > 0 ){
			
			lines = lines.subList( trim_from, lines.size());
		}
		
		COConfigurationManager.setParameter( "speed.limit.handler.schedule.lines", lines );
		
		COConfigurationManager.save();
		
		return( loadSchedule());
	}
	
	private List<LimitedRateGroup>
	trim(
		LimitedRateGroup[]	groups )
	{
		List<LimitedRateGroup> result = new ArrayList<LimitedRateGroup>();
		
		for ( LimitedRateGroup group: groups ){
			
			if ( group instanceof UtilitiesImpl.PluginLimitedRateGroup ){
				
				result.add( group );
			}
		}
		
		return( result );
	}
	
	public void 
	updated(
		LongTermStats stats ) 
	{
		boolean exceeded = false;
		
		for (Map.Entry<Integer,long[]> entry: net_limits.entrySet()){
		
			int	type = entry.getKey();
			
			long[] usage = stats.getTotalUsageInPeriod( type );
						
			long total_up = usage[LongTermStats.ST_PROTOCOL_UPLOAD] + usage[LongTermStats.ST_DATA_UPLOAD] + usage[LongTermStats.ST_DHT_UPLOAD];
			long total_do = usage[LongTermStats.ST_PROTOCOL_DOWNLOAD] + usage[LongTermStats.ST_DATA_DOWNLOAD] + usage[LongTermStats.ST_DHT_DOWNLOAD];
			
			long[]	limits = entry.getValue();

			if ( limits[0] > 0 ){
				
				exceeded = total_up + total_do >= limits[0];
			}
			
			if ( limits[1] > 0 && !exceeded){
				
				exceeded = total_up >= limits[1];
			}
		
			if ( limits[2] > 0 && !exceeded){
				
				exceeded = total_do >= limits[2];
			}
			
			if ( exceeded ){
				
				break;
			}
		}
		
		if ( net_limit_pause_all_active != exceeded ){
			
			setNetLimitPauseAllActive( exceeded );
		}
	}
	
	private String
	formatUp(
		int	rate )
	{
		return( "Up=" + format( rate ));
	}
	
	private String
	formatDown(
		int	rate )
	{
		return( "Down=" + format( rate ));
	}
	
	private String
	format(
		int		rate )
	{
		if ( rate < 0 ){
			
			return( "Disabled" );
			
		}else if ( rate == 0 ){
			
			return( "Unlimited" );
			
		}else{
			
			return( DisplayFormatters.formatByteCountToKiBEtcPerSec( rate ));
		}
	}
	
	private String
	formatUp(
		List<LimitedRateGroup>	groups )
	{
		return( "Up=" + format( groups ));
	}
	
	private String
	formatDown(
		List<LimitedRateGroup>	groups )
	{
		return( "Down=" + format( groups ));
	}
	
	private String
	format(
		List<LimitedRateGroup>	groups )
	{
		String str = "";
		
		for ( LimitedRateGroup group: groups ){
			
			str += (str.length()==0?"":", ") + group.getName() + ":" + format( group.getRateLimitBytesPerSecond());
		}
		
		return( str );
	}
	
    private void
    exportBoolean(
    	Map<String,Object>	map,
    	String				key,
    	boolean				b )
    {
    	map.put( key, new Long(b?1:0));
    }
    
    private boolean
    importBoolean(
    	Map<String,Object>	map,
    	String				key )
    {
    	Long	l = (Long)map.get( key );
    	
    	if ( l != null ){
    		
    		return( l == 1 );
    	}
    	
    	return( false );
    }
    
    private void
    exportInt(
    	Map<String,Object>	map,
    	String				key,
    	int					i )
    {
    	map.put( key, new Long( i ));
    }
    
    private int
    importInt(
    	Map<String,Object>	map,
    	String				key )
    {
    	Long	l = (Long)map.get( key );
    	
    	if ( l != null ){
    		
    		return( l.intValue());
    	}
    	
    	return( 0 );
    }
    
    private void
    exportString(
    	Map<String,Object>	map,
    	String				key,
    	String				s )
    {
    	try{
    		map.put( key, s.getBytes( "UTF-8" ));
    		
    	}catch( Throwable e ){
    	}
    }
    
    private String
    importString(
    	Map<String,Object>	map,
    	String				key )
    {
       	Object obj= map.get( key );
       	
       	if ( obj instanceof String ){
       		
       		return((String)obj);
       		
       	}else if ( obj instanceof byte[] ){
       	
    		try{
    			return( new String((byte[])obj, "UTF-8" ));
    			
    		}catch( Throwable e ){
	    	}
       	}
       	
    	return( null );
    }
    
    public void
    dump(
    	IndentWriter		iw )
    {
    	iw.println( "Profiles" );
    	
    	iw.indent();
    	
    	try{
	    	List<String> profiles = getProfileNames();
	    	
	    	for (String profile: profiles ){
	    	
	    		iw.println( profile );
	    		
	    		iw.indent();
	    		
	    		try{
	    			List<String> p_lines = getProfileSupport( profile, true );
	    			
	    			for ( String line: p_lines ){
	    				
	    				iw.println( line );
	    			}
	    		}finally{
	    			
	    			iw.exdent();
	    		}
	    	}
    	}finally{
    		
    		iw.exdent();
    	}
    	
    	iw.println( "Schedule" );
    	
    	iw.indent();
		
    	try{
	    	List lines_list = COConfigurationManager.getListParameter( "speed.limit.handler.schedule.lines", new ArrayList());
			
			List<String> schedule_lines = BDecoder.decodeStrings(BEncoder.cloneList(lines_list) );
	
			for ( String line: schedule_lines ){
				
				iw.println( line );
			}
    	}finally{
		
    		iw.exdent();
    	}
    }
    
	private class
	LimitDetails
	{
	    private boolean		auto_up_enabled;
	    private boolean		auto_up_seeding_enabled;
	    private boolean		seeding_limits_enabled;
	    private int			up_limit;
	    private int			up_seeding_limit;
	    private int			down_limit;
	    
	    private boolean		lan_rates_enabled;
	    private int			lan_up_limit;
	    private int			lan_down_limit;
	    
	    private Map<String,int[]>	download_limits = new HashMap<String, int[]>();
	    private Map<String,int[]>	category_limits = new HashMap<String, int[]>();
	    private Map<String,int[]>	tag_limits 		= new HashMap<String, int[]>();
	    
	    private 
	    LimitDetails()
	    {	
	    }
	    
	    private 
	    LimitDetails(
	    	Map<String,Object>		map )
	    {
	    	auto_up_enabled 		= importBoolean( map, "aue" );
	    	auto_up_seeding_enabled	= importBoolean( map, "ause" );
	    	seeding_limits_enabled	= importBoolean( map, "sle" );
	    	
	    	up_limit			= importInt( map, "ul" );
	    	up_seeding_limit	= importInt( map, "usl" );
	    	down_limit			= importInt( map, "dl" );
	    	
	    	if ( map.containsKey( "lre" )){
	    		
	    		lan_rates_enabled 		= importBoolean( map, "lre" );
	    		
	    	}else{
	    			// migration from before LAN rates added
	    		
	    		lan_rates_enabled = COConfigurationManager.getBooleanParameter( "LAN Speed Enabled" );
	    	}
	    	
	    	lan_up_limit		= importInt( map, "lul" );
	    	lan_down_limit		= importInt( map, "ldl" );

	    	
	    	List<Map<String,Object>>	d_list = (List<Map<String,Object>>)map.get( "dms" );
	    	
	    	if ( d_list != null ){
	    		
	    		for ( Map<String,Object> m: d_list ){
	    			
	    			String	k = importString( m, "k" );
	    			
	    			if ( k != null ){
	    				
	    				int	ul = importInt( m, "u" );
	    				int	dl = importInt( m, "d" );
	    				
	    				download_limits.put( k, new int[]{ ul, dl });
	    			}
	    		}
	    	}
	    	
	    	List<Map<String,Object>>	c_list = (List<Map<String,Object>>)map.get( "cts" );
	    	
	    	if ( c_list != null ){
	    		
	    		for ( Map<String,Object> m: c_list ){
	    			
	    			String	k = importString( m, "k" );
	    			
	    			if ( k != null ){
	    				
	    				int	ul = importInt( m, "u" );
	    				int	dl = importInt( m, "d" );
	    				
	    				category_limits.put( k, new int[]{ ul, dl });
	    			}
	    		}
	    	}
	    	
	    	List<Map<String,Object>>	t_list = (List<Map<String,Object>>)map.get( "tgs" );
	    	
	    	if ( t_list != null ){
	    		
	    		for ( Map<String,Object> m: t_list ){
	    			
	    			String	t = importString( m, "k" );
	    			
	    			if ( t != null ){
	    				
	    				int	ul = importInt( m, "u" );
	    				int	dl = importInt( m, "d" );
	    				
	    				tag_limits.put( t, new int[]{ ul, dl });
	    			}
	    		}
	    	}
	    }
	    
	    private Map<String,Object>
	    export()
	    {
	    	Map<String,Object>	map = new HashMap<String, Object>();
	    	
	    	exportBoolean( map, "aue", auto_up_enabled );
	    	exportBoolean( map, "ause", auto_up_seeding_enabled );
	    	exportBoolean( map, "sle", seeding_limits_enabled );
	    	
	    	exportInt( map, "ul", up_limit );
	    	exportInt( map, "usl", up_seeding_limit );
	    	exportInt( map, "dl", down_limit );
	    	
	    	exportBoolean( map, "lre", lan_rates_enabled );
	    	exportInt( map, "lul", lan_up_limit );
	    	exportInt( map, "ldl", lan_down_limit );

	    	
	    	List<Map<String,Object>>	d_list = new ArrayList<Map<String,Object>>();
	    	
	    	map.put( "dms", d_list );
	    	
	    	for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
	    		
	    		Map<String,Object> m = new HashMap<String,Object>();
	    		
	    		d_list.add( m );
	    		
	    		exportString( m, "k", entry.getKey());
	    		exportInt( m, "u", entry.getValue()[0]);
	    		exportInt( m, "d", entry.getValue()[1]);
	    	}
	    	
	    	List<Map<String,Object>>	c_list = new ArrayList<Map<String,Object>>();
	    	
	    	map.put( "cts", c_list );
	    	
	    	for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
	    		
	    		Map<String,Object> m = new HashMap<String,Object>();
	    		
	    		c_list.add( m );
	    		
	    		exportString( m, "k", entry.getKey());
	    		exportInt( m, "u", entry.getValue()[0]);
	    		exportInt( m, "d", entry.getValue()[1]);
	    	}
	    	
	    	List<Map<String,Object>>	t_list = new ArrayList<Map<String,Object>>();
	    	
	    	map.put( "tgs", t_list );
	    	
	    	for ( Map.Entry<String,int[]> entry: tag_limits.entrySet()){
	    		
	    		Map<String,Object> m = new HashMap<String,Object>();
	    		
	    		t_list.add( m );
	    		
	    		exportString( m, "k", entry.getKey());
	    		exportInt( m, "u", entry.getValue()[0]);
	    		exportInt( m, "d", entry.getValue()[1]);
	    	}
	    	
	    	return( map );
	    }
	    
	    private void
	    loadForReset()
	    {
	    		// just maintain the auto upload setting over a reset
	    	
		    auto_up_enabled 		= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
	    }
	    
	    private void
	    loadCurrent()
	    {
		    auto_up_enabled 		= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
		    auto_up_seeding_enabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY );
		    seeding_limits_enabled 	= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.UPLOAD_SEEDING_ENABLED_CONFIGKEY );
		    up_limit 				= COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY );
		    up_seeding_limit 		= COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY );
		    down_limit				= COConfigurationManager.getIntParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY );
		
		    lan_rates_enabled 		= COConfigurationManager.getBooleanParameter( "LAN Speed Enabled" );
		    lan_up_limit 			= COConfigurationManager.getIntParameter( "Max LAN Upload Speed KBs" );
		    lan_down_limit 			= COConfigurationManager.getIntParameter( "Max LAN Download Speed KBs" );
	    
		    download_limits.clear();
		    
			GlobalManager gm = core.getGlobalManager();

			List<DownloadManager>	downloads = gm.getDownloadManagers();
			
			for ( DownloadManager download: downloads ){
				
				TOTorrent torrent = download.getTorrent();
				
				byte[]	hash = null;
				
				if ( torrent!= null ){
					
					try{
						hash = torrent.getHash();
						
					}catch( Throwable e ){
						
					}
				}
				
				if ( hash != null ){
					int	download_up_limit 	= download.getStats().getUploadRateLimitBytesPerSecond();
					int	download_down_limit = download.getStats().getDownloadRateLimitBytesPerSecond();
					
			    	if ( download_up_limit > 0 || download_down_limit > 0 ){
			    		
			    		download_limits.put( Base32.encode( hash ), new int[]{ download_up_limit, download_down_limit });
			    	}
				}
			}
		    
			Category[] categories = CategoryManager.getCategories();
		 
			category_limits.clear();
			
		    for ( Category category: categories ){
		    	
		    	int	cat_up_limit	 	= category.getUploadSpeed();
		    	int	cat_down_limit 		= category.getDownloadSpeed();
		    	
		    	if ( cat_up_limit > 0 || cat_down_limit > 0 ){
		    	
		    		category_limits.put( category.getName(), new int[]{ cat_up_limit, cat_down_limit });
		    	}
		    }
		    
			List<TagType>	tag_types = TagManagerFactory.getTagManager().getTagTypes();
			 
			tag_limits.clear();
			
		    for ( TagType tag_type: tag_types ){
		    	
		    	if ( tag_type.getTagType() == TagType.TT_DOWNLOAD_CATEGORY ){
		    		
		    		continue;
		    	}
		    	
		    	if ( tag_type.hasTagTypeFeature( TagFeature.TF_RATE_LIMIT )){
		    		
		    		List<Tag> tags = tag_type.getTags();
		    		
		    		for ( Tag tag: tags ){
		    			
			    		TagFeatureRateLimit rl = (TagFeatureRateLimit)tag;
			    		
				    	int	tag_up_limit	 	= rl.getTagUploadLimit();
				    	int	tag_down_limit 		= rl.getTagDownloadLimit();
				    	
				    	if ( tag_up_limit > 0 || tag_down_limit > 0 ){
				    	
				    		tag_limits.put( 
				    			tag_type.getTagType() + "." + tag.getTagID(),
				    			new int[]{ tag_up_limit, tag_down_limit });
				    	}
		    		}
		    	}
		    }
	    }
	    
	    private int[]
	    getLimitsForDownload(
	    	String	hash )
	    {
	    	return( download_limits.get( hash ));
	    }
	    
	    private void
	    addRemoveDownloads(
	    	List<String>		hashes,
	    	boolean				add )
	    {
			GlobalManager gm = core.getGlobalManager();

	    	for ( String hash: hashes ){
	    		
	    		if ( add ){

	   				DownloadManager download = gm.getDownloadManager( new HashWrapper( Base32.decode( hash )));
	    			
	    			if ( download != null ){
	    						
						int	download_up_limit 	= download.getStats().getUploadRateLimitBytesPerSecond();
						int	download_down_limit = download.getStats().getDownloadRateLimitBytesPerSecond();
						
				    	if ( download_up_limit > 0 || download_down_limit > 0 ){
				    		
				    		download_limits.put(hash, new int[]{ download_up_limit, download_down_limit });
				    	}
	    			}
	    		}else{
	    			
	    			download_limits.remove( hash );
	    		}
	    	}
	    }
	    
	    private void
	    apply()
	    {			    		
	    		// don't manage this properly because the speedmanager has a 'memory' of 
    			// the last upload limit in force before it became active and we're
    			// not persisting this... rare use case methinks anyway

    		COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY, auto_up_enabled );
	    	COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY, auto_up_seeding_enabled );

    		if ( !( auto_up_enabled || auto_up_seeding_enabled )){
      				
 		     	COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY, up_limit );
    		}
    		
		    COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_ENABLED_CONFIGKEY, seeding_limits_enabled );
		    COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY, up_seeding_limit );

		    COConfigurationManager.setParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY, down_limit );

		    COConfigurationManager.setParameter( "LAN Speed Enabled", lan_rates_enabled );
		    COConfigurationManager.setParameter( "Max LAN Upload Speed KBs", lan_up_limit );
		    COConfigurationManager.setParameter( "Max LAN Download Speed KBs", lan_down_limit );
		    	    
			GlobalManager gm = core.getGlobalManager();

			Set<DownloadManager>	all_managers = new HashSet<DownloadManager>( gm.getDownloadManagers());
			
			for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
				
				byte[] hash = Base32.decode( entry.getKey());
				
				DownloadManager dm = gm.getDownloadManager( new HashWrapper( hash ));
			
				if ( dm != null ){
						
					int[]	limits = entry.getValue();
					
					dm.getStats().setUploadRateLimitBytesPerSecond( limits[0] );
					dm.getStats().setDownloadRateLimitBytesPerSecond( limits[1] );
					
					all_managers.remove( dm );
				}
			}

			for ( DownloadManager dm: all_managers ){
				
				dm.getStats().setUploadRateLimitBytesPerSecond( 0 );
				dm.getStats().setDownloadRateLimitBytesPerSecond( 0 );
			}
			
				//cats
			
			Set<Category> all_categories = new HashSet<Category>( Arrays.asList(CategoryManager.getCategories()));
			 
			Map<String, Category> cat_map = new HashMap<String, Category>();
			
			for ( Category c: all_categories ){
				
				cat_map.put( c.getName(), c );
			}
								
			for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
		    	
		    	String cat_name = entry.getKey();
		    	
		    	Category category = cat_map.get( cat_name );
		    	
		    	if ( category != null ){
		    		
		    		int[]	limits = entry.getValue();
		    		
		    		category.setUploadSpeed( limits[0] );
		    		category.setDownloadSpeed( limits[1] );
		    		
		    		all_categories.remove( category );
		    	}
			}
			
			for ( Category category: all_categories ){
				
	    		category.setUploadSpeed( 0 );
	    		category.setDownloadSpeed( 0 );
			}
			
				// tags
			
			TagManager tm = TagManagerFactory.getTagManager();
			
			List<TagType> all_tts = tm.getTagTypes();
			
			Set<Tag>	all_rl_tags = new HashSet<Tag>();
			
			for ( TagType tt: all_tts ){
				
				if ( tt.getTagType() == TagType.TT_DOWNLOAD_CATEGORY ){
					continue;
				}
				
				if ( tt.hasTagTypeFeature( TagFeature.TF_RATE_LIMIT )){
					
					all_rl_tags.addAll( tt.getTags());
				}
			}
			
			for ( Map.Entry<String,int[]> entry: tag_limits.entrySet()){
		    	
		    	String tag_key = entry.getKey();
		    	
		    	String[] bits = tag_key.split( "\\." );
		    	
		    	try{
		    		int	tag_type 	= Integer.parseInt( bits[0] );
		    		int tag_id		= Integer.parseInt( bits[1] );
		    	
		    		TagType tt = tm.getTagType( tag_type );
		    		
		    		if ( tt == null || !tt.hasTagTypeFeature( TagFeature.TF_RATE_LIMIT )){
		    			
		    			continue;
		    		}
		    		
		    		Tag tag = tt.getTag( tag_id );
		    			
		    		if ( tag == null ){
		    		
		    			continue;
		    		}
		    		
		    		TagFeatureRateLimit rl = (TagFeatureRateLimit)tag;
		    		
		    		int[]	limits = entry.getValue();
		    		
		    		rl.setTagUploadLimit( limits[0] );
		    		rl.setTagDownloadLimit( limits[1] );
		    		
		    		all_rl_tags.remove( tag );
		    		
		    	}catch( Throwable e ){
		    		
		    	}
			}
			
			for ( Tag tag: all_rl_tags ){
				
				try{
					TagFeatureRateLimit rl = (TagFeatureRateLimit)tag;
					
					rl.setTagUploadLimit( 0 );
		    		rl.setTagDownloadLimit( 0 );

				}catch( Throwable e ){
		    		
		    	}	
			}
	    }
	    
	    private List<String>
	    getString(
	    	boolean	is_current,
	    	boolean	use_hashes )
	    {
			List<String> result = new ArrayList<String>();
			
			result.add( "Global Limits" );
				    	    
		    if ( auto_up_enabled ){
		    	
			    result.add( "    Auto upload limit enabled" );
			    
		    }else if ( auto_up_seeding_enabled ){
		    				    	
		    	result.add( "    Auto upload seeding limit enabled" );

		    }else{
		    	
		    	result.add( "    " + formatUp( up_limit*1024 ));

	    		if ( seeding_limits_enabled ){

	    			result.add( "    Seeding only limit enabled" );
		    		
		    		result.add( "    Seeding only: " + format( up_seeding_limit*1024 ));
		    	}
		    }
		    
		    result.add( "    " + formatDown( down_limit*1024 ));

		    if ( lan_rates_enabled ){
		    	
		    	result.add( "" );
			    result.add( "    LAN limits enabled" );
		    	result.add( "        " + formatUp( lan_up_limit*1024 ));
		    	result.add( "        " + formatDown( lan_down_limit*1024 ));
		    }
		    
		    result.add( "" );
		    
		    result.add( "Download Limits" );
		    
		    int	total_download_limits 				= 0;
		    int	total_download_limits_up 			= 0;
		    int	total_download_limits_up_disabled 	= 0;
		    int	total_download_limits_down 			= 0;
		    int	total_download_limits_down_disabled	= 0;
		    
			GlobalManager gm = core.getGlobalManager();
			
			for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
				
				String hash_str = entry.getKey();
				
				byte[] hash = Base32.decode( hash_str );
				
				DownloadManager dm = gm.getDownloadManager( new HashWrapper( hash ));
			
				if ( dm != null ){
						
					int[]	limits = entry.getValue();
					
		    		total_download_limits++;
		    		
		    		int	up 		= limits[0];
		    		int	down 	= limits[1];
		    		
		    		if ( up < 0 ){
		    			
		    			total_download_limits_up_disabled++;
		    			
		    		}else{
		    			
		    			total_download_limits_up 	+= up;
		    		}
		    		
		    		if ( down < 0 ){
		    			
		    			total_download_limits_down_disabled++;
		    			
		    		}else{
		    			
		    			total_download_limits_down 	+= down;
		    		}
		    		
		    		result.add( "    " + (use_hashes?hash_str.substring(0,16):dm.getDisplayName()) + ": " + formatUp( up ) + ", " + formatDown( down ));
		    	}
			}
			
		    if ( total_download_limits == 0 ){
		    	
		    	result.add( "    None" );
		    	
		    }else{
		    	
		    	result.add( "    ----" );
		    	
		    	result.add( 
		    		"    Total=" + total_download_limits + 
		    		" - Compounded limits: " + formatUp( total_download_limits_up ) + 
		    		(total_download_limits_up_disabled==0?"":( " [" + total_download_limits_up_disabled + " disabled]" )) +
		    		", " + formatDown( total_download_limits_down ) +
		    		(total_download_limits_down_disabled==0?"":( " [" + total_download_limits_down_disabled + " disabled]" )));
		    }
		    
			Category[] categories = CategoryManager.getCategories();
		 
			Map<String, Category> cat_map = new HashMap<String, Category>();
			
			for ( Category c: categories ){
				
				cat_map.put( c.getName(), c );
			}
			
		    result.add( "" );

			result.add( "Category Limits" );
			
			int	total_cat_limits 		= 0;
		    int	total_cat_limits_up 	= 0;
		    int	total_cat_limits_down 	= 0;

			for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
		    	
		    	String cat_name = entry.getKey();
		    	
		    	Category category = cat_map.get( cat_name );
		    	
		    	if ( category != null ){
		    		
		    		if ( category.getType() == Category.TYPE_UNCATEGORIZED ){
		    			
		    			cat_name = "Uncategorised";
		    		}
		    		
					int[]	limits = entry.getValue();
					
		    		total_cat_limits++;

		    		int	up 		= limits[0];
		    		int	down 	= limits[1];
		    		
		    		total_cat_limits_up 	+= up;
		    		total_cat_limits_down 	+= down;
		    		
		    		result.add( "    " + cat_name + ": " + formatUp( up ) + ", " + formatDown( down ));
		    	}
		    }
		    
		    if ( total_cat_limits == 0 ){
		    	
		    	result.add( "    None" );
		    	
		    }else{
		    	
		    	result.add( "    ----" );
		    	
		    	result.add( "    Total=" + total_cat_limits + " - Compounded limits: " + formatUp( total_cat_limits_up ) + ", " + formatDown( total_cat_limits_down ));

		    }
		    
		    result.add( "" );

			result.add( "Tag Limits" );
			
			int	total_tag_limits 		= 0;
		    int	total_tag_limits_up 	= 0;
		    int	total_tag_limits_down 	= 0;

		    TagManager tm = TagManagerFactory.getTagManager();
		    
			for ( Map.Entry<String,int[]> entry: tag_limits.entrySet()){
		    	
		    	String tag_key = entry.getKey();
		    	
		    	String[] bits = tag_key.split( "\\." );
		    	
		    	try{
		    		int	tag_type 	= Integer.parseInt( bits[0] );
		    		int tag_id		= Integer.parseInt( bits[1] );
		    	
		    		TagType tt = tm.getTagType( tag_type );
		    		
		    		if ( tt == null || !tt.hasTagTypeFeature( TagFeature.TF_RATE_LIMIT )){
		    			
		    			continue;
		    		}
		    		
		    		Tag tag = tt.getTag( tag_id );
		    			
		    		if ( tag == null ){
		    		
		    			continue;
		    		}
		    		
		    		String tag_name = tt.getTagTypeName( true ) + " - " + tag.getTagName( true );
		    		
					int[]	limits = entry.getValue();
					
		    		total_tag_limits++;

		    		int	up 		= limits[0];
		    		int	down 	= limits[1];
		    		
		    		total_tag_limits_up 	+= up;
		    		total_tag_limits_down 	+= down;
		    		
		    		result.add( "    " + tag_name + ": " + formatUp( up ) + ", " + formatDown( down ));

		    	}catch( Throwable e ){
		    		
		    	}
		    }
		    
		    if ( total_tag_limits == 0 ){
		    	
		    	result.add( "    None" );
		    	
		    }else{
		    	
		    	result.add( "    ----" );
		    	
		    	result.add( "    Total=" + total_tag_limits + " - Compounded limits: " + formatUp( total_tag_limits_up ) + ", " + formatDown( total_tag_limits_down ));

		    }
		    
		    
		    
		    if ( is_current ){
		    	
				Map<LimitedRateGroup,List<Object>> plugin_limiters = new HashMap<LimitedRateGroup, List<Object>>();
	
				List<DownloadManager> dms = gm.getDownloadManagers();
				
				for ( DownloadManager dm: dms ){
	    		
					for ( boolean upload: new Boolean[]{ true, false }){
						
						List<LimitedRateGroup> limiters = trim( dm.getRateLimiters( upload ));
						
						for ( LimitedRateGroup g: limiters ){
							
							List<Object> entries = plugin_limiters.get( g );
							
							if ( entries == null ){
								
								entries = new ArrayList<Object>();
								
								plugin_limiters.put( g, entries );
								
								entries.add( upload );
								entries.add( new int[]{ 0 });
							}
							
							entries.add( dm );
						}
					}
					
		    		PEPeerManager pm = dm.getPeerManager();
		    		
		    		if ( pm != null ){
		    			
		    			List<PEPeer> peers = pm.getPeers();
		    			
		    			for ( PEPeer peer: peers ){
		    				
		    				for ( boolean upload: new Boolean[]{ true, false }){
		    					
		    					List<LimitedRateGroup> limiters = trim( peer.getRateLimiters( upload ));
		    					
		    					for ( LimitedRateGroup g: limiters ){
		    						
		    						List<Object> entries = plugin_limiters.get( g );
		    						
		    						if ( entries == null ){
		    							
		    							entries = new ArrayList<Object>();
		    							
		    							plugin_limiters.put( g, entries );
		    							
		    							entries.add( upload );
		    								
		    							entries.add( new int[]{ 1 });
		    							
		    						}else{
		    								
		    							((int[])entries.get(1))[0]++;
		    						}
		    					}
		    				}
		    			}
		    		}
	    		}
	
			    result.add( "" );
	
				result.add( "Plugin Limits" );
	
			    if ( plugin_limiters.size() == 0 ){
			    	
			    	result.add( "    None" );
			    	
			    }else{
			    	
			    	List<String>	plugin_lines = new ArrayList<String>();
			    	
			    	for ( Map.Entry<LimitedRateGroup,List<Object>> entry: plugin_limiters.entrySet()){
			    		
			    		LimitedRateGroup group = entry.getKey();
			    		
			    		List<Object> list = entry.getValue();
			    		
			    		boolean is_upload 	= (Boolean)list.get(0);
			    		int		peers		= ((int[])list.get(1))[0];
			    		
			    		String line = "    " + group.getName() + ": " + (is_upload?formatUp( group.getRateLimitBytesPerSecond()):formatDown( group.getRateLimitBytesPerSecond()));
			    	
			    		if ( peers > 0 ){
			    			
			    			line += ", peers=" + peers;
			    		}
			    		
			    		if ( list.size() > 2 ){
			    			
			    			line += ", downloads=" + (list.size()-2);
			    		}
			    		
			    		plugin_lines.add( line );
			    	}
			    	
			    	Collections.sort( plugin_lines );
			    	
			    	result.addAll( plugin_lines );
			    }
		    }
		    
			return( result );
	    }
	}
	
	private class
	ScheduleRule
	{
		private static final byte	FR_MON		= 0x01;
		private static final byte	FR_TUE		= 0x02;
		private static final byte	FR_WED		= 0x04;
		private static final byte	FR_THU		= 0x08;
		private static final byte	FR_FRI		= 0x10;
		private static final byte	FR_SAT		= 0x20;
		private static final byte	FR_SUN		= 0x40;
		private static final byte	FR_OVERFLOW	= (byte)0x80;
		private static final byte	FR_WEEKDAY	= ( FR_MON | FR_TUE | FR_WED | FR_THU | FR_FRI );
		private static final byte	FR_WEEKEND	= ( FR_SAT | FR_SUN );
		private static final byte	FR_DAILY	= ( FR_WEEKDAY | FR_WEEKEND );
		
		private String	profile_name;
		private byte	frequency;
		private int		from_mins;
		private int		to_mins;
		
		private 
		ScheduleRule(
			byte			_freq,
			String			_profile,
			int				_from,
			int				_to )
		{
			frequency 		= _freq;
			profile_name	= _profile;
			from_mins		= _from;
			to_mins			= _to;
		}
		
		private List<ScheduleRule>
		splitByDay()
		{
			List<ScheduleRule>	result = new ArrayList<ScheduleRule>();
			
			if ( to_mins > from_mins ){
			
				result.add( this );
				
			}else{
				
					// handle rules that wrap across days. e.g. 23:00 to 00:00
				
				byte next_frequency = (byte)(frequency << 1 );
				
				if ((next_frequency & FR_OVERFLOW ) != 0 ){
					
					next_frequency &= ~FR_OVERFLOW;
					
					next_frequency |= FR_MON;
				}
				
				ScheduleRule	rule1 = new ScheduleRule( frequency, profile_name, from_mins, 23*60+59 );
				ScheduleRule	rule2 = new ScheduleRule( next_frequency, profile_name, 0, to_mins );

				result.add( rule1 );
				result.add( rule2 );
			}
			
			return( result );
		}
		
		private boolean
		sameAs(
			ScheduleRule	other )
		{
			if ( other == null ){
				
				return( false );
			}
			
			return( frequency == other.frequency &&
					profile_name.equals( other.profile_name ) &&
					from_mins == other.from_mins &&
					to_mins == other.to_mins );
		}
		
		public String
		getString()
		{
			String	freq_str = "";
			
			if ( frequency == FR_DAILY ){
				
				freq_str = "daily";
				
			}else if ( frequency == FR_WEEKDAY ){
				
				freq_str = "weekdays";
				
			}else if ( frequency == FR_WEEKEND ){
				
				freq_str = "weekends";
				
			}else if ( frequency == FR_MON ){
				
				freq_str = "mon";
				
			}else if ( frequency == FR_TUE ){
				
				freq_str = "tue";
				
			}else if ( frequency == FR_WED ){
				
				freq_str = "wed";
				
			}else if ( frequency == FR_THU ){
				
				freq_str = "thu";
				
			}else if ( frequency == FR_FRI ){
				
				freq_str = "fri";
				
			}else if ( frequency == FR_SAT ){
				
				freq_str = "sat";
				
			}else if ( frequency == FR_SUN ){
				
				freq_str = "sun";
			}
			
			return( "profile=" + profile_name + ", frequency=" + freq_str + ", from=" + getTime( from_mins ) + ", to=" + getTime( to_mins ));
		}
		
		private String
		getTime(
			int	mins )
		{
			String str = getTimeBit( mins/60 ) + ":" + getTimeBit( mins % 60 );
		
			return( str );
		}
		
		private String
		getTimeBit(
			int	num )
		{
			String str = String.valueOf( num );
			
			if ( str.length() < 2 ){
				
				str = "0" + str;
			}
			
			return( str );
		}
	}
	
	private class
	IPSetTagType
		extends TagTypeWithState
	{	
		private final int[] color_default		 	= { 132, 16, 57 };

		private
		IPSetTagType()
		{
			super( TagType.TT_PEER_IPSET, TagPeer.FEATURES, "tag.type.ipset" );
			
			addTagType();
		}
		
		@Override
		protected int[] 
	    getColorDefault() 
		{
			return( color_default );
		}
	}
	
	private class
	IPSet
	{
		private final String		name;
		
		private long[][]			ranges 			= new long[0][];
		private Set<String>			country_codes 	= new HashSet<String>();
		
		private boolean	inverse;
		
		private List<String>	categories;
		
		private boolean	has_explicit_up_lim;
		private boolean	has_explicit_down_lim;
		
		private long	last_send_total = -1;
		private long	last_recv_total = -1;
		
		//private Average send_rate		= Average.getInstance(1000, 10);  //average over 10s, update every 1000ms
		//private Average receive_rate	= Average.getInstance(1000, 10);  //average over 10s, update every 1000ms
		private Average send_rate		= AverageFactory.MovingImmediateAverage( 10 );
		private Average receive_rate	= AverageFactory.MovingImmediateAverage( 10 );

		private RateLimiter		up_limiter;
		private RateLimiter		down_limiter;
		
		private TagPeerImpl		tag_impl;
		
		private
		IPSet(
			String	_name )
		{			
			name	= _name;
			
			up_limiter 		= plugin_interface.getConnectionManager().createRateLimiter( "ips-" + name, 0 );
			down_limiter 	= plugin_interface.getConnectionManager().createRateLimiter( "ips-" + name, 0 );
		}
		
		private void
		initialise(
			int		tag_id )
		{
			tag_impl	= new TagPeerImpl( tag_id );
			
			if ( !has_explicit_up_lim ){
				
				up_limiter.setRateLimitBytesPerSecond( COConfigurationManager.getIntParameter( "speed.limit.handler.ipset_n." + tag_id + ".up", 0 ));
			}
			
			if ( !has_explicit_down_lim ){
				
				down_limiter.setRateLimitBytesPerSecond( COConfigurationManager.getIntParameter( "speed.limit.handler.ipset_n." + tag_id + ".down", 0 ));
			}
		}
		
		private void
		setParameters(
			boolean			_inverse,
			int				_up_lim,
			int				_down_lim,
			List<String>	_cats )
		{
			inverse	= _inverse;
			
			has_explicit_up_lim = _up_lim >= 0;
			if ( !has_explicit_up_lim ){
				_up_lim = 0;
			}
			
			has_explicit_down_lim = _down_lim >= 0;
			if ( !has_explicit_down_lim ){
				_down_lim = 0;
			}
			
			up_limiter.setRateLimitBytesPerSecond( _up_lim );
			down_limiter.setRateLimitBytesPerSecond( _down_lim );
			
			categories = _cats.size()==0?null:_cats;
		}
		
		private boolean
		addCIDRorCC(
			String		cidr_or_cc )
		{
			if ( Character.isDigit( cidr_or_cc.charAt( 0 ))){
				
				String cidr = cidr_or_cc;
				
				int	pos = cidr.indexOf( '/' );
				
				if ( pos == -1 ){
					
					return( false );
				}
				
				String	address = cidr.substring( 0, pos );
				
					// no ipv6 atm
				
				if ( address.contains( ":" )){
					
					return( false );
				}
				
				try{
					byte[] start_bytes = HostNameToIPResolver.syncResolve( address ).getAddress();
								
					int	cidr_mask = Integer.parseInt( cidr.substring( pos+1 ));
					
					int	rev_mask = 0;
					
					for (int i=0;i<32-cidr_mask;i++){
						
						rev_mask = ( rev_mask << 1 ) | 1;
					}
				
					start_bytes[0] &= ~(rev_mask>>24);
					start_bytes[1] &= ~(rev_mask>>16);
					start_bytes[2] &= ~(rev_mask>>8);
					start_bytes[3] &= ~(rev_mask);
					
					byte[] end_bytes = start_bytes.clone();
					
					end_bytes[0] |= (rev_mask>>24)&0xff;
					end_bytes[1] |= (rev_mask>>16)&0xff;
					end_bytes[2] |= (rev_mask>>8)&0xff;
					end_bytes[3] |= (rev_mask)&0xff;
	
					long	l_start = ((long)((start_bytes[0]<<24)&0xff000000 | (start_bytes[1] << 16)&0x00ff0000 | (start_bytes[2] << 8)&0x0000ff00 | start_bytes[3]&0x000000ff))&0xffffffffL;
					long	l_end	= ((long)((end_bytes[0]<<24)&0xff000000 | (end_bytes[1] << 16)&0x00ff0000 | (end_bytes[2] << 8)&0x0000ff00 | end_bytes[3]&0x000000ff))&0xffffffffL;
					
					//System.out.println( cidr + " -> " + ByteFormatter.encodeString( start_bytes ) + " - " +  ByteFormatter.encodeString( end_bytes ) + ": " + ((l_end-l_start+1)));
					
					int	len = ranges.length;
					
					long[][] new_ranges = new long[len+1][];
					
					for (int i=0;i<len;i++){
						
						new_ranges[i] = ranges[i];
					}
					
					new_ranges[len] = new long[]{ l_start, l_end };
					
					ranges = new_ranges;
					
					return( true );
					
				}catch( Throwable e ){
					
					return( false );
				}
			}else{
				
				String cc = cidr_or_cc;
				
				if ( cc.length() != 2 ){
					
					return( false );
				}
				
				country_codes.add( cc.toUpperCase( Locale.US ));
				
				return( true );
			}
		}
		
		private void
		addSet(
			IPSet	other )
		{
			long[][] new_ranges = new long[ ranges.length + other.ranges.length ][];
			
			System.arraycopy( ranges, 0, new_ranges, 0, ranges.length );
			System.arraycopy( other.ranges, 0, new_ranges, ranges.length, other.ranges.length );
			
			ranges = new_ranges;
			
			country_codes.addAll( other.country_codes );
		}
		
		private String
		getName()
		{
			return( name );
		}
		
		private long[][]
		getRanges()
		{
			return( ranges );
		}
		
		private Set<String>
		getCountryCodes()
		{
			return( country_codes );
		}
		
		private RateLimiter
		getUpLimiter()
		{
			return( up_limiter );
		}
		
		private RateLimiter
		getDownLimiter()
		{
			return( down_limiter );
		}
		
		private List<String>
		getCategories()
		{
			return( categories );
		}
		
		private void
		updateStats(
			int	tick_count )
		{
			long	send_total 	= up_limiter.getRateLimitTotalByteCount();
			long	recv_total	= down_limiter.getRateLimitTotalByteCount();
			
			if ( last_send_total != -1 ){
				
				long send_diff = send_total - last_send_total;
				long recv_diff = recv_total - last_recv_total;
				
				send_rate.update( send_diff );
				receive_rate.update( recv_diff );
			}
			
			last_send_total = send_total;
			last_recv_total = recv_total;
						
			TagPeerImpl tag = tag_impl;
				
			if ( tag != null ){
					
				tag.update(tick_count );
			}
		}
	
		private boolean
		isInverse()
		{
			return( inverse );
		}
		
		private void
		addPeer(
			Peer		peer )
		{
			TagPeerImpl tag = tag_impl;
			
			if ( tag != null ){
				
				tag.add( PluginCoreUtils.unwrap( peer ));
			}
		}
		
		private void
		removePeer(
			Peer		peer )
		{
			TagPeerImpl tag = tag_impl;
			
			if ( tag != null ){
				
				tag.remove( PluginCoreUtils.unwrap( peer ));
			}
		}
		
		private void
		destroy()
		{
			if ( tag_impl != null ){
				
				tag_impl.removeTag();
				
				tag_impl = null;
			}
		}
		
		private String
		getAddressString()
		{
			long	address_count = 0;
			
			for ( long[] range: ranges ){
				address_count += range[1] - range[0] + 1;
			}
			
			return( String.valueOf( address_count ));
		}
		
		private String
		getDetailString()
		{
			return( name + ": Up=" + format(up_limiter.getRateLimitBytesPerSecond()) + " (" + DisplayFormatters.formatByteCountToKiBEtcPerSec((long)send_rate.getAverage()) + ")" + 
					", Down=" + format( down_limiter.getRateLimitBytesPerSecond()) + " (" + DisplayFormatters.formatByteCountToKiBEtcPerSec((long)receive_rate.getAverage()) + ")" + 
					", Addresses=" + getAddressString() + 
					", Inverse=" + inverse +
					", Categories=" + categories );
					
		}
		
		private class
		TagPeerImpl
			extends TagBase
			implements TagPeer
		{
			private Object	UPLOAD_PRIORITY_ADDED_KEY = new Object();
			
			private int upload_priority;
			
			private Set<PEPeer>	added_peers 	= new HashSet<PEPeer>();
			private Set<PEPeer>	pending_peers 	= new HashSet<PEPeer>();
			
			private 
			TagPeerImpl(
				int		tag_id )
			{
				super( ip_set_tag_type, tag_id, name );
				
				addTag();
				
				upload_priority = COConfigurationManager.getIntParameter( "speed.limit.handler.ipset_n." + getTagID() + ".uppri", 0 );
			}
			
			public int 
			getTaggableTypes() 
			{
				return( Taggable.TT_PEER );
			}
			 
			private void
			update(
				int		tick_count )
			{
				List<PEPeer> to_remove 	= null;
				List<PEPeer> to_add		= null;
				
				synchronized( this ){

					if ( tick_count % 5 == 0 ){
								
						Iterator<PEPeer> it = added_peers.iterator();
						
						while( it.hasNext()){
							
							PEPeer peer = it.next();
							
							if ( peer.getPeerState() == PEPeer.DISCONNECTED ){
								
								it.remove();
								
								if ( to_remove == null ){
									
									to_remove = new ArrayList<PEPeer>();
								}
																
								to_remove.add( peer );
							}
						}
					}
							
					Iterator<PEPeer> it = pending_peers.iterator();
					
					while ( it.hasNext()){
						
						PEPeer peer = it.next();
						
						int state =  peer.getPeerState();
						
						if ( state == PEPeer.TRANSFERING ){
						
							it.remove();
							
							added_peers.add( peer );
							
							if ( to_add == null ){
								
								to_add = new ArrayList<PEPeer>();
							}

							to_add.add( peer );
							
						}else if ( state == PEPeer.DISCONNECTED ){
						
							it.remove();
							
								// no need to untag as never added
						}
					}
				}
				
				if ( to_add != null ){
	
					for ( PEPeer peer: to_add ){
						
						addTaggable( peer );
					}
				}
				
				if ( to_remove != null ){
					
					for ( PEPeer peer: to_remove ){
						
						removeTaggable( peer );
					}
				}
			}
			
			private void
			add(
				PEPeer		peer )
			{
				synchronized( this ){
										
					if ( peer.getPeerState() == PEPeer.TRANSFERING ){
						
						if ( added_peers.contains( peer )){
							
							return;
						}
						
						pending_peers.remove( peer );
						
						added_peers.add( peer );
						
					}else{
						
						pending_peers.add( peer );
						
						return;
					}
				}
								
				addTaggable( peer );
			}
			
			private void
			remove(
				PEPeer		peer )
			{
				synchronized( this ){
					
					if ( pending_peers.remove( peer )){
					
						return;
					}
					
					if ( !added_peers.remove( peer )){
						
						return;
					}
				}
								
				removeTaggable( peer );
			}
			
			public void
			addTaggable(
				Taggable	t )
			{
				if ( upload_priority > 0 ){
					
					((PEPeer)t).updateAutoUploadPriority( UPLOAD_PRIORITY_ADDED_KEY, true );
				}
				
				super.addTaggable( t );
			}
			
			public void
			removeTaggable(
				Taggable	t )
			{
				if ( upload_priority > 0 ){
					
					((PEPeer)t).updateAutoUploadPriority( UPLOAD_PRIORITY_ADDED_KEY, false );
				}

				super.removeTaggable( t );
			}
			
			public int
			getTaggedCount()
			{
				synchronized( this ){
					
					return( added_peers.size());
				}
			}
			
			public List<PEPeer>
			getTaggedPeers()
			{
				synchronized( this ){
				
					return( new ArrayList<PEPeer>( added_peers ));
				}
			}
			
			public Set<Taggable> 
			getTagged() 
			{
				synchronized( this ){
					
					return( new HashSet<Taggable>( added_peers ));
				}
			}
			
			public boolean 
			hasTaggable(
				Taggable	t )
			{
				synchronized( this ){
					
					return( added_peers.contains( t ));
				}
			}
			
			public boolean
			supportsTagRates()
			{
				return( true );
			}
			 
			public boolean
			supportsTagUploadLimit()
			{
				return( !has_explicit_up_lim );
			}

			public boolean
			supportsTagDownloadLimit()
			{
				return( !has_explicit_down_lim );
			}

			public int
			getTagUploadLimit()
			{
				return( up_limiter.getRateLimitBytesPerSecond());
			}
			
			public void
			setTagUploadLimit(
				int		bps )
			{
				if ( supportsTagUploadLimit()){
					
					up_limiter.setRateLimitBytesPerSecond( bps );
					
					COConfigurationManager.setParameter( "speed.limit.handler.ipset_n." + getTagID() + ".up", bps );
				}
			}
			
			public int
			getTagCurrentUploadRate()
			{
				return( (int)send_rate.getAverage());
			}
			
			public int
			getTagDownloadLimit()
			{
				return( down_limiter.getRateLimitBytesPerSecond());
			}
			
			public void
			setTagDownloadLimit(
				int		bps )
			{
				if ( supportsTagDownloadLimit()){
					
					down_limiter.setRateLimitBytesPerSecond( bps );
					
					COConfigurationManager.setParameter( "speed.limit.handler.ipset_n." + getTagID() + ".down", bps );
				}
			}
			
			public int
			getTagCurrentDownloadRate()
			{
				return( (int)receive_rate.getAverage());
			}
			
			public boolean
			getCanBePublicDefault()
			{
				return( false );
			}
			
			public int
			getTagUploadPriority()
			{
				return( upload_priority );
			}
			
			public void
			setTagUploadPriority(
				int		priority )
			{
				if ( priority < 0 ){
					
					priority = 0;
				}
				
				if ( priority == upload_priority ){
					
					return;
				}
				
				int	old_up = upload_priority;
				
				upload_priority	= priority;
				
				COConfigurationManager.setParameter( "speed.limit.handler.ipset_n." + getTagID() + ".uppri", priority );
				
				if ( old_up == 0 || priority == 0 ){
					
					List<PEPeer> peers = getTaggedPeers();
					
					for ( PEPeer peer: peers ){
							
						peer.updateAutoUploadPriority( UPLOAD_PRIORITY_ADDED_KEY, priority>0 );
					}
				}
			}
			
			public void
			removeTag()
			{
				if ( upload_priority > 0 ){
					
					List<PEPeer> peers = getTaggedPeers();
					
					for ( PEPeer peer: peers ){
							
						peer.updateAutoUploadPriority( UPLOAD_PRIORITY_ADDED_KEY, false );
					}
				}
				
				super.removeTag();
			}
		}
	}
}
