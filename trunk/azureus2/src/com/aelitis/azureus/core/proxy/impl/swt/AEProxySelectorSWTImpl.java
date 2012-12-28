/*
 * Created on Aug 14, 2012
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


package com.aelitis.azureus.core.proxy.impl.swt;


import java.net.InetAddress;
import java.net.Proxy;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.proxy.impl.AEProxySelectorImpl;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class 
AEProxySelectorSWTImpl 
{
	private AzureusCore				core;
	private AEProxySelectorImpl		proxy_selector;
	
	private UISWTStatusEntry 	status;
		
	private Image	icon_grey;
	private Image	icon_green;
	private Image	icon_yellow;
	private Image	icon_red;
	
	private Image	last_icon;

	private boolean	flag_incoming;
	
	{
		COConfigurationManager.addAndFireParameterListener(
			"Proxy.SOCKS.ShowIcon.FlagIncoming",
			new ParameterListener()
			{
				public void parameterChanged(String name) {
					flag_incoming = COConfigurationManager.getBooleanParameter( name );
				}
			});
	}
	private long	last_bad_peer_update;
	
	private volatile boolean	is_visible;
	
	public 
	AEProxySelectorSWTImpl(
		AzureusCore				_core,
		AEProxySelectorImpl		_proxy_selector )
	{
		core			= _core;
		proxy_selector	= _proxy_selector;
		
		final PluginInterface default_pi = PluginInitializer.getDefaultInterface();
	
		default_pi.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						final UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							UIFunctions uif = UIFunctionsManager.getUIFunctions();
							
							if ( uif != null ){
								
								uif.getUIUpdater().addListener(
									new UIUpdater.UIUpdaterListener()
									{
										public void 
										updateComplete(
											int count )
										{											
											updateStatus();
										}
									});
							}
							
							Utils.execSWTThread(
								new AERunnable() 
								{
									public void 
									runSupport() 
									{
										ImageLoader imageLoader = ImageLoader.getInstance();
	
										icon_grey	 	= imageLoader.getImage( "grayled" );
										icon_yellow 	= imageLoader.getImage( "yellowled" );
										icon_green 		= imageLoader.getImage( "greenled" );
										icon_red		= imageLoader.getImage( "redled" );
				
										final UISWTInstance	ui_instance = (UISWTInstance)instance;
										
										status	= ui_instance.createStatusEntry();
										
										status.setText( "SOCKS" );
																				
										status.setImageEnabled( true );
										
										status.setImage( icon_grey );
																				
										final String icon_param = "Proxy.SOCKS.ShowIcon";
										
										boolean	enable_proxy 	= COConfigurationManager.getBooleanParameter("Enable.Proxy");
									    boolean enable_socks	= COConfigurationManager.getBooleanParameter("Enable.SOCKS");

										is_visible	 = 
											enable_proxy && enable_socks && 
											COConfigurationManager.getBooleanParameter( icon_param );

										status.setVisible( is_visible );
										
										if ( is_visible ){
											
											updateStatus();
										}
										
										final MenuItem mi_show =
											default_pi.getUIManager().getMenuManager().addMenuItem(
																status.getMenuContext(),
																"pairing.ui.icon.show" );
										
										mi_show.setStyle( MenuItem.STYLE_CHECK );
										mi_show.setData( new Boolean( is_visible ));
										
										mi_show.addListener(
												new MenuItemListener()
												{
													public void
													selected(
														MenuItem			menu,
														Object 				target )
													{
														COConfigurationManager.setParameter( icon_param, false );
													}
												});
										
										COConfigurationManager.addParameterListener(
											new String[]{ "Enable.Proxy", "Enable.SOCKS", icon_param },
											new ParameterListener()
											{
												public void 
												parameterChanged(
													String parameterName)
												{
													boolean	enable_proxy 	= COConfigurationManager.getBooleanParameter("Enable.Proxy");
												    boolean enable_socks	= COConfigurationManager.getBooleanParameter("Enable.SOCKS");

													is_visible	 = 
														enable_proxy && enable_socks && 
														COConfigurationManager.getBooleanParameter( icon_param );
													
													status.setVisible( is_visible );
													
													mi_show.setData( new Boolean( is_visible ));
													
													if ( is_visible ){
														
														updateStatus();
													}
												}
											});
											
										
										MenuItem mi_sep =
											default_pi.getUIManager().getMenuManager().addMenuItem(
																status.getMenuContext(),
																"" );
			
										mi_sep.setStyle( MenuItem.STYLE_SEPARATOR );
										
										MenuItem mi_options =
											default_pi.getUIManager().getMenuManager().addMenuItem(
																status.getMenuContext(),
																"MainWindow.menu.view.configuration" );
			
										mi_options.addListener(
											new MenuItemListener()
											{
												public void
												selected(
													MenuItem			menu,
													Object 				target )
												{
													UIFunctions uif = UIFunctionsManager.getUIFunctions();
			
													if ( uif != null ){
			
														uif.openView( UIFunctions.VIEW_CONFIG, "proxy" );
													}
												}
											});
										
										
										UISWTStatusEntryListener click_listener = 
											new UISWTStatusEntryListener()
										{
												public void 
												entryClicked(
													UISWTStatusEntry entry )
												{
													UIFunctions uif = UIFunctionsManager.getUIFunctions();
													
													if ( uif != null ){
			
														uif.getMDI().loadEntryByID( StatsView.VIEW_ID, true, false, "TransferStatsView" );
													}
												}
											};
											
										status.setListener( click_listener );
									}
								});
						}
								
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
						
					}
				});
	}
	
	private void
	updateStatus()
	{
		if ( !is_visible ){
			
			return;
		}
		
		Image	icon;
		String	tip_key;
		
		Proxy active_proxy = proxy_selector.getActiveProxy();
		
	    long	now = SystemTime.getMonotonousTime();

		if ( active_proxy == null ){
			
			icon 	= icon_grey;
			tip_key	= "label.inactive"; 
		}else{
			
		    long	last_con 	= proxy_selector.getLastConnectionTime();
		    long	last_fail 	= proxy_selector.getLastFailTime();
		    		    
		    long	con_ago		= now - last_con;
		    long	fail_ago 	= now - last_fail;
		   		    
		    if ( last_fail < 0 ){
		    	
		    	icon 		= icon_green;
		    	tip_key		= "PeerManager.status.ok";
		    }else{
		    	
			    if ( fail_ago > 60*1000 ){
			    	
			    	if ( con_ago < fail_ago ){
			    		
			    		icon 		= icon_green;
			    		tip_key		= "PeerManager.status.ok";
			    	}else{
			    		
			    		icon 	= icon_grey;
			    		tip_key	= "PeersView.state.pending"; 
			    	}
			    }else{
			    	
			    	icon 		= icon_yellow;
			    	tip_key		= "label.con_prob";
			    }
		    }
		}
		
		if ( flag_incoming ){
			
			boolean	bad_incoming = false;
	
			if ( now - last_bad_peer_update > 15*1000 ){
				
				last_bad_peer_update = now;
				
				List<DownloadManager> dms = core.getGlobalManager().getDownloadManagers();
							
				for ( DownloadManager dm: dms ){
					
					PEPeerManager pm = dm.getPeerManager();
					
					if ( pm != null ){
						
						if ( pm.getNbRemoteTCPConnections() + pm.getNbRemoteUDPConnections() + pm.getNbRemoteUTPConnections() > 0 ){
							
							List<PEPeer> peers = pm.getPeers();
							
							for ( PEPeer peer: peers ){
								
								if ( peer.isIncoming()){
									
									if ( !peer.isLANLocal()){
										
										try{
											if ( InetAddress.getByAddress( HostNameToIPResolver.hostAddressToBytes( peer.getIp())).isLoopbackAddress()){
												
												continue;
											}
										}catch( Throwable e ){	
										}
										
										bad_incoming = true;
										
										break;
									}
								}
							}
						}
					}
					
					if ( bad_incoming ){
						
						break;
					}
				}
			}else if ( last_icon == icon_red ){
				
				bad_incoming = true;
			}
			
			if ( bad_incoming ){
				
				icon 	= icon_red;
				tip_key	= "proxy.socks.bad.incoming";
			}
		}
		
		if ( last_icon != icon ){
			
			final Image 	f_icon 	= icon;
			final String	f_key	= tip_key;
			
			Utils.execSWTThread(
				new AERunnable() 
				{
					public void 
					runSupport() 
					{
						last_icon = f_icon;
						
						status.setImage( f_icon );
						
						status.setTooltipText( 
							MessageText.getString(
								"proxy.socks.ui.icon.tip",
								new String[]{ MessageText.getString(f_key)} ));
					}
				});
		}
	}
}
