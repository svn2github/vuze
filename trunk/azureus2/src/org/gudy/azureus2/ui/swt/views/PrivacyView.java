/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.views;

import java.net.URL;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateAttributeListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerController;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLGroup;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.tracker.TrackerPeerSource;
import com.aelitis.azureus.plugins.I2PHelpers;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;



public class PrivacyView
	implements UISWTViewCoreEventListener, DownloadManagerStateAttributeListener
{
	public static final String MSGID_PREFIX = "PrivacyView";

	private UISWTView swtView;

	private Composite cMainComposite;

	private ScrolledComposite sc;

	private Composite 	parent;
	
	private Composite	i2p_lookup_comp;
	private Button 		i2p_lookup_button;

	private Button[]	network_buttons;
	
	private Label		peer_info;
	
	private Label		torrent_info;
	private Label		tracker_info;
	private Label		webseed_info;
	
	private Label		vpn_info;
	private Label		socks_info;
	
	private DownloadManager	current_dm;
	private Set<String>		enabled_networks = new HashSet<String>();
	
	public 
	PrivacyView() 
	{
	}

	private String 
	getFullTitle() 
	{
		return( MessageText.getString("label.privacy"));
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				parent = (Composite) event.getData();
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				Messages.updateLanguageForControl(cMainComposite);
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				Object ds = event.getData();
				dataSourceChanged(ds);
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				initialize();
				if (current_dm == null) {
					dataSourceChanged(swtView.getDataSource());
				}
				break;
				
			case UISWTViewEvent.TYPE_FOCUSLOST:
				delete();
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
		}

		return true;
	}

	private void 
	delete() 
	{
		Utils.disposeComposite( sc );
		
		dataSourceChanged( null );
	}

	private void 
	refresh() 
	{
		updatePeersEtc( current_dm );
	}

	private void 
	dataSourceChanged(
		Object ds ) 
	{
		synchronized( this ){
			
			final DownloadManager	old_dm = current_dm;
			
			if ( ds != current_dm ){
							
				if ( ds == null ){
					
					current_dm = null;
					
				}else if ( ds instanceof DownloadManager ){
					
					current_dm = (DownloadManager)ds;
					
				}else if ( ds instanceof Object[] ){
					
					Object[] objs = (Object[])ds;
					
					if ( objs.length == 1 && objs[0] instanceof DownloadManager ){
						
						current_dm = (DownloadManager)objs[0];
					}
				}else{
					
					current_dm = null;
				}
			}
			
			if ( old_dm == current_dm ){
				
				return;
			}

			final DownloadManager new_dm = current_dm;
		
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					swt_updateFields( old_dm, new_dm );
				}
			});
		}
	}

	private void 
	initialize() 
	{
		if (cMainComposite == null || cMainComposite.isDisposed()){
			
			if ( parent == null || parent.isDisposed()){
				return;
			}
			
			sc = new ScrolledComposite(parent, SWT.V_SCROLL);
			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			sc.getVerticalBar().setIncrement(16);
			
			Layout parentLayout = parent.getLayout();
			
			if ( parentLayout instanceof GridLayout ){
				
				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				
				sc.setLayoutData(gd);
				
			}else if ( parentLayout instanceof FormLayout ){
				
				sc.setLayoutData(Utils.getFilledFormData());
			}

			cMainComposite = new Composite(sc, SWT.NONE);

			sc.setContent(cMainComposite);
			
		}else{
			
			Utils.disposeComposite(cMainComposite, false);
		}
		
		cMainComposite.setLayout(new GridLayout(1, false));

		GridData gd; 
		
			// I2P install state
		
		Composite i2p_install_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData( GridData.FILL_HORIZONTAL );
		i2p_install_comp.setLayoutData( gd );

		i2p_install_comp.setLayout( new GridLayout(2, false ));

		Label label = new Label( i2p_install_comp, SWT.NULL );
		label.setText( "I2P Network Availability" );
		
		final Button i2p_install = new Button( i2p_install_comp, SWT.PUSH );
		
		boolean i2p_installed = I2PHelpers.isI2PInstalled();
		
		i2p_install.setText( i2p_installed?"I2P is available":"Install I2P" );
		
		i2p_install.setEnabled( !i2p_installed );
		
		i2p_install.addSelectionListener(
			new SelectionAdapter() {
				
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					final boolean[] result = { false };
					
					I2PHelpers.installI2PHelper( 
						null, result, 
						new Runnable() 
						{						
							public void 
							run() 
							{								
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run() 
										{
											boolean i2p_installed = result[0];																		

											i2p_install.setText( i2p_installed?"I2P is available":"Install I2P" );
											
											i2p_install.setEnabled( !i2p_installed );
										}
									});
							}
						});
				}
			});
		
			// I2P peer lookup
		
		i2p_lookup_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData();
		gd.widthHint = 300;
		gd.heightHint = 200;
		i2p_lookup_comp.setLayoutData( gd );
		
		i2p_lookup_comp.setBackground( Colors.white );
		
		i2p_lookup_button = new Button( cMainComposite, SWT.PUSH );
		
		i2p_lookup_button.setText( "Lookup I2P Peers" );
		
		i2p_lookup_button.addSelectionListener(
			new SelectionAdapter() {
				
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					Utils.disposeComposite( i2p_lookup_comp, false );
					
					PluginInterface i2p_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "azneti2phelper", true );
					
					if ( i2p_pi != null ){
						
						IPCInterface ipc = i2p_pi.getIPC();
						
						Map<String,Object>	options = new HashMap<String, Object>();
						
						options.put( "server_id", "Scraper" );
						options.put( "server_id_transient", true );
						options.put( "ui_composite", i2p_lookup_comp );
						
						IPCInterface callback =
							new IPCInterface()
							{
								public Object 
								invoke(
									String methodName, 
									Object[] params) 
										
									throws IPCException
								{
									if ( methodName.equals( "statusUpdate" )){
										
									}else if ( methodName.equals( "peerFound")){
										
										String 	host		= (String)params[0];
										int		peer_type 	= (Integer)params[1];
										
										System.out.println( peer_type + "/" + host );
										
									}
									return( null );
								}
	
								public boolean 
								canInvoke( 
									String methodName, 
									Object[] params )
								{
									return( true );
								}
							};
							

						byte[] hash = (byte[])i2p_lookup_button.getData( "hash" );
							
						try{
							ipc.invoke(
								"lookupTorrent",
								new Object[]{
									"",
									hash,
									options,
									callback 
								});
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				}
			});
		
		i2p_lookup_button.setEnabled( false );
		
			// network selection
		
		Composite network_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData( GridData.FILL_HORIZONTAL );
		network_comp.setLayoutData( gd );

		network_buttons = new Button[AENetworkClassifier.AT_NETWORKS.length];

		network_comp.setLayout( new GridLayout( network_buttons.length, true ));
				
		for ( int i=0; i<network_buttons.length; i++){
			
			final String nn = AENetworkClassifier.AT_NETWORKS[i];

			String msg_text = "ConfigView.section.connection.networks." + nn;

			Button button = new Button(network_comp, SWT.CHECK);
			Messages.setLanguageText(button, msg_text);
			
			network_buttons[i] = button;
			
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					boolean selected = ((Button)e.widget).getSelection();
					
					if ( current_dm != null ){
						current_dm.getDownloadState().setNetworkEnabled(nn, selected);
					}
				}
			});

			GridData gridData = new GridData();
			button.setLayoutData(gridData);
		}
		
			// Torrent Info
			
		Composite torrent_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData( GridData.FILL_HORIZONTAL );
		torrent_comp.setLayoutData( gd );
		torrent_comp.setLayout( new GridLayout( 2, false ));
	
		label = new Label( torrent_comp, SWT.NULL );
		label.setText( "Torrent:" );
		
		torrent_info = new Label( torrent_comp, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		torrent_info.setLayoutData( gd );
	
			// Tracker Info
			
		Composite tracker_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData( GridData.FILL_HORIZONTAL );
		tracker_comp.setLayoutData( gd );
		tracker_comp.setLayout( new GridLayout( 2, false ));
	
		label = new Label( tracker_comp, SWT.NULL );
		label.setText( "Trackers:" );
		
		tracker_info = new Label( tracker_comp, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		tracker_info.setLayoutData( gd );
	
			// Webseed Info
			
		Composite webseed_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData( GridData.FILL_HORIZONTAL );
		webseed_comp.setLayoutData( gd );
		webseed_comp.setLayout( new GridLayout( 2, false ));
	
		label = new Label( webseed_comp, SWT.NULL );
		label.setText( "Web Seeds:" );
		
		webseed_info = new Label( webseed_comp, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		webseed_info.setLayoutData( gd );
	
			// VPN Info
		
		Composite vpn_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData( GridData.FILL_HORIZONTAL );
		vpn_comp.setLayoutData( gd );
		vpn_comp.setLayout( new GridLayout( 2, false ));

		label = new Label( vpn_comp, SWT.NULL );
		label.setText( "VPN Status:" );
		
		vpn_info = new Label( vpn_comp, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		vpn_info.setLayoutData( gd );
		
			// SOCKS Info
			
		Composite socks_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData( GridData.FILL_HORIZONTAL );
		socks_comp.setLayoutData( gd );
		socks_comp.setLayout( new GridLayout( 2, false ));
	
		label = new Label( socks_comp, SWT.NULL );
		label.setText( "SOCKS Status:" );
		
		socks_info = new Label( socks_comp, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		socks_info.setLayoutData( gd );
		
			// Peer Info
			
		Composite peer_comp = new Composite( cMainComposite, SWT.NULL );
		
		gd = new GridData( GridData.FILL_HORIZONTAL );
		peer_comp.setLayoutData( gd );
		peer_comp.setLayout( new GridLayout( 2, false ));
	
		label = new Label( peer_comp, SWT.NULL );
		label.setText( "Peer Status:" );
		
		peer_info = new Label( peer_comp, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		peer_info.setLayoutData( gd );
		
			// the rest
		
		sc.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = sc.getClientArea();
				Point size = cMainComposite.computeSize(r.width, SWT.DEFAULT);
				sc.setMinSize(size);
			}
		});

		swt_updateFields( null, current_dm );

		updatePeersEtc( current_dm );
		
		Rectangle r = sc.getClientArea();
		Point size = cMainComposite.computeSize(r.width, SWT.DEFAULT);
		sc.setMinSize(size);
	}

	private void 
	swt_updateFields(
		DownloadManager		old_dm,
		DownloadManager		new_dm )
	{
		if ( cMainComposite == null || cMainComposite.isDisposed()){
			
			return;
		}
		
		byte[] hash = null;
		
		if ( new_dm != null ){
			
			TOTorrent torrent = new_dm.getTorrent();
			
			if ( torrent != null ){
				
				try{
					hash = torrent.getHash();
					
				}catch( Throwable e ){
					
				}
			}
		}
		
		i2p_lookup_button.setData( "hash", hash );
		i2p_lookup_button.setEnabled( hash != null );
		
		Utils.disposeComposite( i2p_lookup_comp, false );
		
		if ( old_dm != null ){
			
			DownloadManagerState state = old_dm.getDownloadState();
			
			state.removeListener( this, DownloadManagerState.AT_NETWORKS, DownloadManagerStateAttributeListener.WRITTEN );
		}
		
		if ( new_dm != null ){
			
			DownloadManagerState state = new_dm.getDownloadState();
						
			state.addListener( this, DownloadManagerState.AT_NETWORKS, DownloadManagerStateAttributeListener.WRITTEN );
			
			setupNetworks( state.getNetworks());
			
			setupTorrentTracker( new_dm );
			
		}else{
			
			setupNetworks( null );
			
			setupTorrentTracker( null );
		}
	}
	
	private void
	setupNetworks(
		final String[]	enabled )
	{
		Utils.execSWTThread(new AERunnable() {
			public void runSupport(){
				
				enabled_networks.clear();
				
				if ( network_buttons == null || network_buttons[0].isDisposed()){
				
					return;
				}
								
				if ( enabled != null ){
					
					enabled_networks.addAll( Arrays.asList( enabled ));
				}
				
				for ( int i=0; i<AENetworkClassifier.AT_NETWORKS.length; i++){
					
					final String net = AENetworkClassifier.AT_NETWORKS[i];
					
					network_buttons[i].setEnabled( enabled != null );
					
					network_buttons[i].setSelection( enabled_networks.contains ( net ));
				}
				
					// update info about which trackers etc are enabled
				
				setupTorrentTracker( current_dm );
			}
		});
	}
	
	private void
	setupTorrentTracker(
		final DownloadManager	dm  )
	{
		Utils.execSWTThread(new AERunnable() {
			public void runSupport(){
				if ( torrent_info == null || torrent_info.isDisposed()){
					
					return;
				}
				
				TOTorrent torrent = dm==null?null:dm.getTorrent();
				
				if ( torrent == null ){
					
					torrent_info.setText( "" );
					tracker_info.setText( "" );
					webseed_info.setText( "" );
					
					return;
				}
				
				boolean private_torrent = torrent.getPrivate();
				
				torrent_info.setText( "Private=" + private_torrent);
								
				boolean		decentralised 	= false;
				Set<String>	tracker_nets	= new HashSet<String>();
				
				URL	announce_url = torrent.getAnnounceURL();
				
				if ( announce_url != null ){
				
					if ( TorrentUtils.isDecentralised(announce_url)){
						
						decentralised = true;
						
					}else{
						
						String net = AENetworkClassifier.categoriseAddress( announce_url.getHost());
						
						tracker_nets.add( net );
					}
				}
				
				TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
				
				TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();
				
				for ( TOTorrentAnnounceURLSet set: sets ){
																	
					URL[]	urls = set.getAnnounceURLs();
						
					for ( URL u: urls ){
						
						if ( TorrentUtils.isDecentralised( u)){
							
							decentralised = true;
							
						}else{
							
							String net = AENetworkClassifier.categoriseAddress( u.getHost());
							
							tracker_nets.add( net );
						}
					}
				}
				
				String tracker_str = "";
				
				if ( decentralised ){
					
					tracker_str = "Decentralised";
					
					String net_string = "";
					
					if ( !private_torrent ){
						
							// dht only applicable to non-private torrents
						
						for ( String net: new String[]{ AENetworkClassifier.AT_PUBLIC, AENetworkClassifier.AT_I2P }){
							
							if ( enabled_networks.contains( net )){
								
								net_string += (net_string.length()==0?"":", ") + net;
							}
						}
					}
					
					if ( net_string.length() == 0 ){
						
						tracker_str += " (disabled)";
						
					}else{
						
						tracker_str += " [" + net_string + "]";
					}
				}
				
				for ( String net: tracker_nets ){
					
					if ( !enabled_networks.contains( net )){
						
						net += " (disabled)";
					}
					
					tracker_str += (tracker_str.length()==0?"":", " ) + net;
				}
				
				tracker_info.setText( tracker_str );
				
					// web seeds
				
				Set<String>	webseed_nets	= new HashSet<String>();

				ExternalSeedPlugin esp = DownloadManagerController.getExternalSeedPlugin();
				
				if ( esp != null ){
					  	
					ExternalSeedReader[] seeds = esp.getManualWebSeeds( PluginCoreUtils.wrap( torrent ));
					
					if ( seeds != null ){
						
						for ( ExternalSeedReader seed: seeds ){
							
							URL u = seed.getURL();
							
							String net = AENetworkClassifier.categoriseAddress( u.getHost());
							
							webseed_nets.add( net );
						}
					}
				}
				
				String webseeds_str = "";
				
				if ( webseed_nets.isEmpty()){
					
					webseeds_str = "None";
					
				}else{
					
					for ( String net: webseed_nets ){
						
						if ( !enabled_networks.contains( net )){
						
							net += " (disabled)";
						}
						
						webseeds_str += (webseeds_str.length()==0?"":", " ) + net;
					}
				}
				
				webseed_info.setText( webseeds_str );
			}
		});
	}
		
	private void
	updatePeersEtc(
		DownloadManager		dm )
	{		
		final PEPeerManager pm;
		
		if ( dm != null ){
			
			pm = dm.getPeerManager();
			
		}else{
			
			pm = null;
		}
		
		Utils.execSWTThread(new AERunnable(){
			public void runSupport()
			{
				if ( peer_info == null || peer_info.isDisposed()){
					
					return;
				}
				
				if ( pm == null ){
					
					peer_info.setText( "Download is not running" );
					
				}else{
					
					List<PEPeer> peers = pm.getPeers();
					
					String[] all_nets = AENetworkClassifier.AT_NETWORKS;
					
					int[]	 counts = new int[ all_nets.length];
					
					for ( PEPeer peer: peers ){
						
						String net = PeerUtils.getNetwork( peer );
						
						for ( int i=0;i<all_nets.length;i++ ){
							
							if ( all_nets[i] == net ){
								
								counts[i]++;
								
								break;
							}
						}
					}
					
					String str = "";
					
					for ( int i=0;i<all_nets.length;i++ ){
						
						int num = counts[i];
						
						if ( num > 0 ){
							
							str += (str.length()==0?"":", ") + all_nets[i] + "=" + num;
						}
					}
					
					if ( str.length() == 0 ){
						
						str = "No peers connected";
					}
					
					peer_info.setText( str );
				}
			}
		});
	}
	
	public void 
	attributeEventOccurred(
		DownloadManager 	download,
		String 				attribute, 
		int 				event_type ) 
	{
		DownloadManagerState state = download.getDownloadState();

		setupNetworks( state.getNetworks());
	}
}
