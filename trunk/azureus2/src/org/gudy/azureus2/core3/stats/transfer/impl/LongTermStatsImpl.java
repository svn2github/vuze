/*
 * Created on Feb 1, 2013
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


package org.gudy.azureus2.core3.stats.transfer.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.stats.transfer.LongTermStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.plugins.dht.DHTPlugin;

public class 
LongTermStatsImpl 
	implements LongTermStats
{
	private static final int VERSION = 1;
	
	public static final int RT_SESSION_START	= 1;
	public static final int RT_SESSION_STATS	= 2;
	public static final int RT_SESSION_END		= 3;
	
	private AzureusCore			core;
	private GlobalManagerStats	gm_stats;
	private DHT[]				dhts;
	
		// totals at start of session
	
	private long	st_p_sent;
	private long	st_d_sent;
	private long	st_p_received;
	private long	st_d_received;
	private long	st_dht_sent;
	private long	st_dht_received;
	
		// session offsets at start of session
	
	private long	ss_p_sent;
	private long	ss_d_sent;
	private long	ss_p_received;
	private long	ss_d_received;
	private long	ss_dht_sent;
	private long	ss_dht_received;
	
	private boolean				active;
	private boolean				closing;
	
	private TimerEventPeriodic	event;
	private PrintWriter			writer;
	private String				writer_rel_file;
	
	private SimpleDateFormat	date_format = new SimpleDateFormat( "yyyy,MM,dd" );
	
	{
		date_format.setTimeZone( TimeZone.getTimeZone( "UTC" ));
	}
	
	private File stats_dir		= FileUtil.getUserFile( "stats" );
	
	public
	LongTermStatsImpl(
		AzureusCore			_core,
		GlobalManagerStats	_gm_stats )
	{
		core		= _core;
		gm_stats 	= _gm_stats;
		
		COConfigurationManager.addParameterListener(
			"long.term.stats.enable",
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name) 
				{
					boolean	enabled = COConfigurationManager.getBooleanParameter( name );
					
					synchronized( LongTermStatsImpl.this ){
												
						if ( enabled ){
							
							if ( !active ){
								
								sessionStart();
							}
						}else{
							
							if ( active ){
								
								sessionEnd();
							}
						}
					}
				}
			});
		
	    _core.addLifecycleListener(
	        	new AzureusCoreLifecycleAdapter()
	        	{
	        		public void
	        		componentCreated(
	        			AzureusCore				core,
	        			AzureusCoreComponent	component )
	        		{
	        			if ( component instanceof GlobalManager ){
	        				
	        				synchronized( LongTermStatsImpl.this ){
	        					
	        					sessionStart();
	        				}
	        			}
	        		}
	        		
	        		public void
	        		stopped(
	        			AzureusCore		core )
	        		{
	        			synchronized( LongTermStatsImpl.this ){
	        				
	        				closing	= true;
	        				
	        				if ( active ){
	        			
	        					sessionEnd();
	        				}
	        			}
	        		}
	        	});
	}
	
	private DHT[]
	getDHTs()
	{
	    if ( dhts == null ){
	    	
		    try{
		        PluginInterface dht_pi = core.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
		          
		        if ( dht_pi == null ){
		           
		        	dhts = new DHT[0];
		        	
		        }else{
		        	
		        	DHTPlugin plugin = (DHTPlugin)dht_pi.getPlugin();
		        	
		        	if ( plugin.isEnabled()){
		        	
		        		dhts = ((DHTPlugin)dht_pi.getPlugin()).getDHTs();
		        		
		        	}else{
		        		
		        		dhts = new DHT[0];
		        	}
		        }
		    }catch( Throwable e ){
		    	
		    	dhts = new DHT[0];
		    }
	    }
	    
	    return( dhts );
	}
	
	private void
	sessionStart()
	{
		OverallStatsImpl stats = (OverallStatsImpl)StatsFactory.getStats();

		synchronized( this ){
			
			if ( closing ){
				
				return;
			}
			
			boolean	enabled = COConfigurationManager.getBooleanParameter( "long.term.stats.enable" );

			if ( active || !enabled ){
				
				return;
			}
			
			active = true;
			
			long[] snap = stats.getLastSnapshot();
			
			ss_d_received 	= gm_stats.getTotalDataBytesReceived();
			ss_p_received 	= gm_stats.getTotalProtocolBytesReceived();

			ss_d_sent		= gm_stats.getTotalDataBytesSent();
			ss_p_sent		= gm_stats.getTotalProtocolBytesSent();
			    
			ss_dht_sent		= 0;
			ss_dht_received	= 0;
			
			if ( core.isStarted()){
				
				DHT[]	dhts = getDHTs();
				
				if ( dhts != null ){
					
					for ( DHT dht: dhts ){
						
				    	DHTTransportStats dht_stats = dht.getTransport().getStats();
				    	
				    	ss_dht_sent 		+= dht_stats.getBytesSent();
				    	ss_dht_received 	+= dht_stats.getBytesReceived();
					}
				}
			}
			
			st_p_sent 		= snap[0] + ( ss_p_sent - snap[6]);
			st_d_sent 		= snap[1] + ( ss_d_sent - snap[7]);
			st_p_received 	= snap[2] + ( ss_p_received - snap[8]);
			st_d_received 	= snap[3] + ( ss_d_received - snap[9]);
			st_dht_sent		= snap[4] + ( ss_dht_sent - snap[10]);
			st_dht_received = snap[5] + ( ss_dht_received - snap[11]);
			
			write( 	RT_SESSION_START,  
					new long[]{ 
						st_p_sent,
						st_d_sent,
						st_p_received,
						st_d_received,
						st_dht_sent,
						st_dht_received });
			
			if ( event == null ){	// should always be null but hey ho
				
			    event = 
			    	SimpleTimer.addPeriodicEvent(
				    	"LongTermStats", 
				    	60*1000, 
				    	new TimerEventPerformer()
				    	{
				    		public void 
				    		perform(TimerEvent event) 
				    		{
				    			updateStats();
				    		}
				    	});
			}
		}
	}
	
	private void
	sessionEnd()
	{
		synchronized( this ){
			
			if ( !active ){
				
				return;
			}
			
			updateStats( RT_SESSION_END );

			active = false;
			
			if ( event != null ){
				
				event.cancel();
			}
		}
	}
	
	private void
	updateStats()
	{
		updateStats( RT_SESSION_STATS );
	}
	
	private void
	updateStats(
		int	record_type )
	{	    
	    long	current_d_received 	= gm_stats.getTotalDataBytesReceived();
	    long	current_p_received 	= gm_stats.getTotalProtocolBytesReceived();

	    long	current_d_sent		= gm_stats.getTotalDataBytesSent();
	    long	current_p_sent		= gm_stats.getTotalProtocolBytesSent();
   
		long	current_dht_sent		= 0;
		long	current_dht_received	= 0;

		DHT[]	dhts = getDHTs();
		
		if ( dhts != null ){
			
			for ( DHT dht: dhts ){
				
		    	DHTTransportStats dht_stats = dht.getTransport().getStats();
		    	
		    	current_dht_sent 		+= dht_stats.getBytesSent();
		    	current_dht_received 	+= dht_stats.getBytesReceived();
			}
		}
		
	    write(	record_type,
	    		new long[]{
		    		( current_p_sent - ss_p_sent ),
		    		( current_d_sent - ss_d_sent ),
		    		( current_p_received - ss_p_received ),
		    		( current_d_received - ss_d_received ),
		    		( current_dht_sent - ss_dht_sent ),
		    		( current_dht_received - ss_dht_received )});
	}
	
	private long[] line_stats_prev = new long[6];
	
	private void
	write(
		int		record_type,
		long[]	line_stats )
	{
		synchronized( this ){

			try{
				long	now = SystemTime.getCurrentTime();
				
				long	when_mins = now/(1000*60);
				
				String line;
				
				String stats_str = "";
				
				if ( record_type == RT_SESSION_START ){
					
						// absolute values
					
					for ( long s: line_stats ){
						
						stats_str += "," + s;
					}
				}else{
					
						// relative values
					
					for ( int i=0;i< line_stats.length;i++){
						
						stats_str += "," + ( line_stats[i] - line_stats_prev[i] );
						
						line_stats_prev[i] = line_stats[i];
					}
				}
				
				if ( record_type != RT_SESSION_STATS ){
					
					line = (record_type==RT_SESSION_START?"s,":"e,") + VERSION + "," + when_mins + stats_str;
					
				}else{
					
					line = stats_str.substring(1);
				}
				
				String[] bits = date_format.format( new Date( now )).split( "," );
				
				String	current_rel_file = bits[0] + File.separator + bits[1] + File.separator + bits[2] + ".dat";
				
				if ( writer == null || !writer_rel_file.equals( current_rel_file )){
				
						// first open of a file or file switch
					
					if ( writer != null ){
					
							// file switch
						
						if ( record_type != RT_SESSION_START ){
							
							writer.println( line );
						}
						
						writer.close();
						
						if ( writer.checkError()){
							
							writer 			= null;
							
							throw( new IOException( "Write faled" ));
						}
						
						writer 			= null;
					}
					
						// no point in opening a new file just to record the session-end
					
					if ( record_type != RT_SESSION_END ){
						
						File file = new File( stats_dir, current_rel_file );
						
						file.getParentFile().mkdirs();
						
						writer = new PrintWriter( new FileWriter( file, true ));
						
						writer_rel_file = current_rel_file;
							
						if ( record_type == RT_SESSION_START ){
						
							writer.println( line );
							
						}else{
							
								// first entry in a new file, files always start with a session-start so they
								// can be processed in isolation so reset the session data and start a new one
							
							st_p_sent		+= line_stats[0];
							st_d_sent		+= line_stats[1];
							st_p_received	+= line_stats[2];
							st_d_received	+= line_stats[3];
							st_dht_sent		+= line_stats[4];
							st_dht_received	+= line_stats[5];
							
							ss_p_sent		+= line_stats[0];
							ss_d_sent		+= line_stats[1];
							ss_p_received	+= line_stats[2];
							ss_d_received	+= line_stats[3];
							ss_dht_sent		+= line_stats[4];
							ss_dht_received	+= line_stats[5];
														
							stats_str = "";
							
							for ( long s: new long[]{ st_p_sent,st_d_sent, st_p_received,st_d_received, st_dht_sent, st_dht_received }){
								
								stats_str += "," + s;
							}
							
							line = "s," + when_mins + stats_str;
						
							writer.println( line );
						}
					}
				}else{
					
					writer.println( line );
				}
								
			}catch( Throwable e ){
				
				Debug.out( "Failed to write long term stats", e );
				
			}finally{
				
				if ( writer != null ){
					
					if ( record_type == RT_SESSION_END ){
						
						writer.close();
					}
					
					if ( writer.checkError()){
						
						Debug.out( "Failed to write long term stats" );
						
						writer.close();
						
						writer	= null;
						
					}
				}
			}
		}
	}
}
