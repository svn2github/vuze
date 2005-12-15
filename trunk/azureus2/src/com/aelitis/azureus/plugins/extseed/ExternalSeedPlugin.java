/*
 * Created on 15-Dec-2005
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

package com.aelitis.azureus.plugins.extseed;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.Monitor;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.plugins.extseed.getright.ExternalSeedReaderFactoryGetRight;

public class 
ExternalSeedPlugin
	implements Plugin, DownloadManagerListener
{
	private static ExternalSeedReaderFactory[]	factories = {
		new ExternalSeedReaderFactoryGetRight(),
	};
	
	private PluginInterface		plugin_interface;
	private LoggerChannel		log;
	
	private Map		download_map	= new HashMap();
	private Monitor	download_mon;
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"External Seed" );
		
		log	= plugin_interface.getLogger().getChannel( "External Seeds" );
		
		final BasicPluginViewModel	view_model = plugin_interface.getUIManager().createBasicPluginViewModel( "External Seed" );

		
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
		
		download_mon	= plugin_interface.getUtilities().getMonitor();
		
		plugin_interface.getDownloadManager().addListener( this );
		
		plugin_interface.getUtilities().createTimer( "ExternalPeerScheduler", true ).addPeriodicEvent(
				30000,
				new UTTimerEventPerformer()
				{
					public void
					perform(
						UTTimerEvent		event )
					{
						try{
							download_mon.enter();
							
							Iterator	it = download_map.values().iterator();
							
							while( it.hasNext()){
								
								List	peers = (List)it.next();
								
								for (int i=0;i<peers.size();i++){
									
									((ExternalSeedPeer)peers.get(i)).checkConnection();
								}
							}
							
						}finally{
							
							download_mon.exit();
						}	
					}
				});
		
	}
	
	public void
	downloadAdded(
		Download	download )
	{
		Torrent	torrent = download.getTorrent();
		
		if ( torrent == null ){
			
			return;
		}
		
		Random	random = new Random();
		
		final List	peers = new ArrayList();
		
		for (int i=0;i<factories.length;i++){
			
			ExternalSeedReader[]	x = factories[i].getSeedReaders( this, torrent );
			
			for (int j=0;j<x.length;j++){
				
				ExternalSeedReader	reader = x[j];
				
				ExternalSeedPeer	peer = new ExternalSeedPeer( this, reader );
				
				peers.add( random.nextInt( peers.size()+1 ), peer );
			}
		}
		
		if ( peers.size() > 0 ){
			
			download.addPeerListener(
				new DownloadPeerListener()
				{
					public void
					peerManagerAdded(
						Download		download,
						PeerManager		peer_manager )
					{
						for (int i=0;i<peers.size();i++){
							
							ExternalSeedPeer	peer = (ExternalSeedPeer)peers.get(i);
							
							peer.setManager( peer_manager );
						}
					}
					
					public void
					peerManagerRemoved(
						Download		download,
						PeerManager		peer_manager )
					{
						for (int i=0;i<peers.size();i++){

							ExternalSeedPeer	peer = (ExternalSeedPeer)peers.get(i);
						
							peer.setManager( null );
						}
					}
				});
			
			try{
				download_mon.enter();
				
				download_map.put( download, peers );
				
			}finally{
				
				download_mon.exit();
			}
		}
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		try{
			download_mon.enter();
			
			download_map.remove( download );
			
		}finally{
			
			download_mon.exit();
		}	
	}
	
	public void
	log(
		String		str )
	{
		log.log( str );
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
}