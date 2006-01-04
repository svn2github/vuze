/*
 * Created on 23-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.tracker.local;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.Monitor;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerListener;

public class 
LocalTrackerPlugin
	implements Plugin, AZInstanceManagerListener, DownloadManagerListener
{
	private static final String	PLUGIN_NAME	= "LAN Peer Finder";
	
	private static final long	ANNOUNCE_PERIOD	= 20*60*1000;
	
	private PluginInterface		plugin_interface;
	private AZInstanceManager	instance_manager;
	private boolean				active;
	private TorrentAttribute 	ta_networks;

	private Map 				downloads 	= new HashMap();
	private Map					track_times	= new HashMap();
	
	private BooleanParameter	enabled;
	
	private LoggerChannel 		log;
	private Monitor 			mon;
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );

		ta_networks 	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_NETWORKS );

		mon	= plugin_interface.getUtilities().getMonitor();
		
		log = plugin_interface.getLogger().getTimeStampedChannel(PLUGIN_NAME);
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel	config = ui_manager.createBasicPluginConfigModel( "Plugins", "Plugin.localtracker.name" );

		config.addLabelParameter2( "Plugin.localtracker.info" );
		
		enabled = config.addBooleanParameter2( "Plugin.localtracker.enable", "Plugin.localtracker.enable", true );

		final BasicPluginViewModel	view_model = 
			plugin_interface.getUIManager().createBasicPluginViewModel( "Plugin.localtracker.name" );

		
		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	content )
					{
						view_model.getLogArea().appendText( content + "\n" );
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						if ( str.length() > 0 ){
							view_model.getLogArea().appendText( str + "\n" );
						}
						
						StringWriter sw = new StringWriter();
						
						PrintWriter	pw = new PrintWriter( sw );
						
						error.printStackTrace( pw );
						
						pw.flush();
						
						view_model.getLogArea().appendText( sw.toString() + "\n" );
					}
				});
		
		instance_manager	= AzureusCoreFactory.getSingleton().getInstanceManager();
		
		instance_manager.addListener( this );
		
		plugin_interface.getDownloadManager().addListener( this );
	}
	
	public void
	instanceFound(
		AZInstance		instance )
	{
		if ( !enabled.getValue()){
		
			return;
		}
		
		log.log( "Found: " + instance.getString());
		
		try{
			mon.enter();
		
			if ( active ){
				
				return;
			}
			
			active	= true;
			
			plugin_interface.getUtilities().createThread(
				"Tracker",
				new Runnable()
				{
					public void
					run()
					{
						track();
					}
				});
			
		}finally{
			
			mon.exit();
		}
	}
	
	public void
	instanceChanged(
		AZInstance		instance )
	{
		if ( !enabled.getValue()){
			
			return;
		}
		
		log.log( "Changed: " + instance.getString());
	}
	
	public void
	instanceLost(
		AZInstance		instance )
	{
		try{
			mon.enter();
			
			track_times.remove( instance );
			
		}finally{
			
			mon.exit();
		}
		
		if ( !enabled.getValue()){
			
			return;
		}
		
		log.log( "Lost: " + instance.getString());
	}
	
	public void
	instanceTracked(
		AZInstance		instance )
	{
		if ( !enabled.getValue()){
			
			return;
		}
		
		handleTrackResult( instance );
	}
	
	protected void
	track()
	{
		while( true ){
	
			try{
				long	now = plugin_interface.getUtilities().getCurrentSystemTime();
				
				List	todo = new ArrayList();
				
				try{
					mon.enter();
					
					Iterator	it = downloads.entrySet().iterator();
					
					while( it.hasNext()){
						
						Map.Entry	entry = (Map.Entry)it.next();
						
						Download	dl 		= (Download)entry.getKey();
						long		when	= ((Long)entry.getValue()).longValue();
						
						if ( when > now || now - when > ANNOUNCE_PERIOD ){
							
							todo.add( dl );
						}
					}
					
					for (int i=0;i<todo.size();i++){
						
						downloads.put( todo.get(i), new Long( now ));
					}
				}finally{
					
					mon.exit();
				}
				
				for (int i=0;i<todo.size();i++){
				
					track((Download)todo.get(i));
				}
				
				Thread.sleep(60*1000);
				
			}catch( Throwable e ){
				
				log.log(e);
			}
		}
	}
	
	protected void
	track(
		Download	download )
	{
		if ( !enabled.getValue()){
			
			return;
		}
		
		int	state = download.getState();
		
		if ( state == Download.ST_ERROR || state == Download.ST_STOPPED ){
			
			return;
		}
		
		AZInstance[]	peers = instance_manager.track( download );
		
		for (int i=0;i<peers.length;i++){
			
			handleTrackResult( peers[i] );
		}
	}
	
	protected void
	handleTrackResult(
		AZInstance		inst )
	{
		Download	download = (Download)inst.getProperty( AZInstance.PR_DOWNLOAD );
				
		boolean	is_seed = ((Boolean)inst.getProperty(AZInstance.PR_SEED)).booleanValue();
		
		long	now		= SystemTime.getCurrentTime();
		
		boolean	skip 	= false;
		
			// this code is here to deal with multiple interface machines that receive the result multiple times
		
		try{
			mon.enter();
			
			Map	map = (Map)track_times.get( inst );
			
			if ( map == null ){
				
				map	= new HashMap();
				
				track_times.put( inst, map );
			}
			
			String	dl_key = plugin_interface.getUtilities().getFormatters().encodeBytesToString(download.getTorrent().getHash());
			
			Long	last_track = (Long)map.get( dl_key );
			
			if ( last_track != null ){
				
				long	lt = last_track.longValue();
				
				if ( now - lt < 60*1000 ){
					
					skip	= true;
				}
			}
			
			map.put( dl_key, new Long(now));
			
		}finally{
			
			mon.exit();
		}
		
		if ( skip ){
		
			return;
		}
		
		log.log( "Tracked: " + inst.getString() + ": " + download.getName() + ", seed = " + is_seed );

		if ( download.isComplete() && is_seed ){
			
			return;
		}
		
		PeerManager	peer_manager = download.getPeerManager();
		
		if ( peer_manager != null ){
			
			String	peer_ip		= inst.getInternalAddress().getHostAddress();
			int		peer_port	= inst.getTrackerClientPort();
			
			log.log( "    " + download.getName() + ": Injecting peer " + peer_ip + ":" + peer_port );
			
			peer_manager.addPeer( peer_ip, peer_port );
		}
	}
	
	public void
	downloadAdded(
		Download	download )
	{
		try{
			mon.enter();
		
			Torrent	torrent = download.getTorrent();
			
			if ( torrent == null || torrent.isPrivate()){
				
				return;
			}
			
			String[]	networks = download.getListAttribute( ta_networks );
			
			boolean	public_net = false;
			
			for (int i=0;i<networks.length;i++){
				
				if ( networks[i].equalsIgnoreCase( "Public" )){
						
					public_net	= true;
					
					break;
				}
			}
			
			if ( !public_net ){
				
				return;
			}
			
			downloads.put( download, new Long(0));
			
		}finally{
			
			mon.exit();
		}
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		try{
			mon.enter();
		
			downloads.remove( download );
			
		}finally{
			
			mon.exit();
		}
	}
}
