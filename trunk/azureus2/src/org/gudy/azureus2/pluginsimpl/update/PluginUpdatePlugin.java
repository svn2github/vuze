/*
 * Created on 28-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.update;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;
import java.io.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.pluginsimpl.*;
import org.gudy.azureus2.pluginsimpl.update.sf.*;

public class 
PluginUpdatePlugin
	implements Plugin
{
	public static final int	RD_SIZE_RETRIES	= 3;
	public static final int	RD_SIZE_TIMEOUT	= 10000;
	
	protected static Map			outstanding_updates = new HashMap();
	
	protected PluginInterface		plugin_interface;
	protected LoggerChannel 		log;
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel("Plugin Update");

		UIManager	ui_manager = plugin_interface.getUIManager();
		
		final BasicPluginViewModel model = 
			ui_manager.getBasicPluginViewModel( 
					"Plugin Update");
		
		boolean enabled = plugin_interface.getPluginconfig().getPluginBooleanParameter( "enable.update", true );

		model.getStatus().setText( enabled?"Running":"Disabled" );
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		log.addListener(
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
		
		PluginView view = ui_manager.createPluginView( model );
		
		plugin_interface.addView( view );	
		
		BasicPluginConfigModel config = ui_manager.createBasicPluginConfigModel( "plugins", "plugins.update");
		
		config.addBooleanParameter( "enable.update", "Plugin.pluginupdate.enablecheck", true );
		
		if ( enabled ){
			
			Thread t = new Thread("PluginUpdate" )
				{
					public void
					run()
					{
						updater();
					}
				};
				
			t.setDaemon(true);
			
			t.start();
		}else{
			
			log.log( LoggerChannel.LT_INFORMATION, "Update check disabled in configuration" );
		}
	}
	
	protected void
	updater()
	{
		PluginInterface[]	plugins = plugin_interface.getPluginManager().getPlugins();
		
		log.log( LoggerChannel.LT_INFORMATION, "Currently loaded plugins:");

		List	plugins_to_check 		= new ArrayList();
		List	plugins_to_check_ids	= new ArrayList();
		Map		plugins_to_check_names	= new HashMap();
		
		for (int i=0;i<plugins.length;i++){
			
			PluginInterface	pi = plugins[i];
			
			String	id 		= pi.getPluginID();
			String	version = pi.getPluginVersion();
			String	name	= pi.getPluginName();
			
			if ( version != null && !id.startsWith("<")){
				
				if ( plugins_to_check_ids.contains( id )){
					
					String	s = (String)plugins_to_check_names.get(id);
					
					if ( !name.equals( id )){
						
						plugins_to_check_names.put( id, s+","+name);
					}
					
				}else{
					plugins_to_check_ids.add( id );
					
					plugins_to_check.add( pi );
					
					plugins_to_check_names.put( id, name.equals(id)?"":name);
				}
			}
			
			log.log( LoggerChannel.LT_INFORMATION, "    " + pi.getPluginName() + ", id = " + id + (version==null?"":(", version = " + pi.getPluginVersion())));
		}
		
		try{
			SFPluginDetailsLoader loader = SFPluginDetailsLoaderFactory.create();
			
			loader.addListener( 
				new SFPluginDetailsLoaderListener()
				{
					public void
					log(
						String	str )
					{
						log.log( LoggerChannel.LT_INFORMATION, "[" + str + "]" );
						
					}
				});
			
			String[]	names = loader.getPluginNames();
			
			String	name_list = "";
			
			for (int i=0;i<names.length;i++){
				
				name_list += (i==0?"":",") + names[i];
			}
			
			log.log( LoggerChannel.LT_INFORMATION, "Downloaded plugin ids = " + name_list );
			
			for ( int i=0;i<plugins_to_check.size();i++){
				
				final PluginInterface	pi = (PluginInterface)plugins_to_check.get(i);
				
				final String	plugin_id = pi.getPluginID();
				
				if ( outstanding_updates.get( plugin_id ) != null ){
			
					log.log( LoggerChannel.LT_INFORMATION, "Skipping " + plugin_id + " as update already outstanding");

					continue;
				}
				
				boolean	found	= false;
				
				for (int j=0;j<names.length;j++){
					
					if ( names[j].equalsIgnoreCase( plugin_id )){
						
						found	= true;
						
						break;
					}
				}
				
				if ( !found ){
					
					log.log( LoggerChannel.LT_INFORMATION, "Skipping " + plugin_id + " as not listed on web site");

					continue;
				}
				
				String	plugin_names	= (String)plugins_to_check_names.get( plugin_id );
				
				log.log( LoggerChannel.LT_INFORMATION, "Checking " + plugin_id);
				
				try{
					
					SFPluginDetails	details = loader.getPluginDetails( plugin_id );
	
					boolean az_cvs = plugin_interface.getUtilities().isCVSVersion();
					
					String az_plugin_version	= pi.getPluginVersion();
					
					String sf_plugin_version	= details.getVersion();
					String sf_plugin_download	= details.getDownloadURL();
					
					String sf_comp_version		= sf_plugin_version;
					
					if ( az_cvs ){
						
						String	sf_cvs_version = details.getCVSVersion();
						
						if ( sf_cvs_version.length() > 0 ){
							
								// sf cvs version ALWAYS entry in _CVS
							
							sf_plugin_version	= sf_cvs_version;
							sf_plugin_download	= details.getCVSDownloadURL();
							
							sf_comp_version = sf_plugin_version.substring(0,sf_plugin_version.length()-4);
						}
					}
					
					// 	System.out.println("comp version = " + sf_comp_version );
					
					int	comp = PluginUtils.comparePluginVersions( az_plugin_version, sf_comp_version );
					
					log.log( LoggerChannel.LT_INFORMATION, "    Current: " + az_plugin_version + ", Latest: " + sf_plugin_version );
					
					if ( comp < 0 ){
						
						log.log( LoggerChannel.LT_INFORMATION, "    Description:" );
						
						logMultiLine( "        ", details.getDescription());
						
						log.log( LoggerChannel.LT_INFORMATION, "    Comment:" );
						
						logMultiLine( "        ", details.getComment());
						
						String msg =   "A newer version (version " + sf_plugin_version + ") of plugin '" + 
										plugin_id + "' " +
										(plugin_names.length()==0?"":"(" + plugin_names + ") " ) +
										"is available. ";
						
						log.logAlert( LoggerChannel.LT_INFORMATION, msg +"See View->Plugins->Plugin Update");
						
						log.log( LoggerChannel.LT_INFORMATION, "" );
						
						log.log( 	LoggerChannel.LT_INFORMATION, "        " + msg + "Download from "+
									sf_plugin_download);
						
						ResourceDownloaderFactory rdf =  plugin_interface.getUtilities().getResourceDownloaderFactory();
						
						ResourceDownloader rdl = rdf.create( new URL( sf_plugin_download ));

							// get size so it is cached
						
						rdf.getTimeoutDownloader(rdf.getRetryDownloader(rdl,RD_SIZE_RETRIES),RD_SIZE_TIMEOUT).getSize();
												
						final String	f_sf_plugin_download 	= sf_plugin_download;
						final String	f_sf_plugin_version		= sf_plugin_version;
						
						rdl.addListener( 
							new ResourceDownloaderListener()
							{
								public void
								reportPercentComplete(
									ResourceDownloader	downloader,
									int					percentage )
								{								
								}
								
								public void
								reportActivity(
									ResourceDownloader	downloader,
									String				activity )
								{	
								}
									
								public boolean
								completed(
									ResourceDownloader	downloader,
									InputStream			data )
								{	
									outstanding_updates.remove( plugin_id );
									
									installUpdate( 
											pi, 
											f_sf_plugin_download, 
											f_sf_plugin_version, 
											data );
									
									return( true );
								}
								
								public void
								failed(
									ResourceDownloader			downloader,
									ResourceDownloaderException e )
								{
									outstanding_updates.remove( plugin_id );								
								}
								
							});
						
						Update update = plugin_interface.getUpdateManager().addUpdate(
								plugin_id + "/" + plugin_names,
								sf_plugin_version,
								rdl,
								Update.RESTART_REQUIRED_MAYBE );
						
						update.addListener(
							new UpdateListener()
							{
								public void
								cancelled(
									Update		update )
								{
									outstanding_updates.remove( plugin_id );
								}
								
								public void
								completed(
									Update		update )
								{
									
								}
							});
						
						outstanding_updates.put( plugin_id, rdl );
					}
				}catch( Throwable e ){
					
					log.log("    Plugin check failed", e ); 
				}
			}
		}catch( Throwable e ){
			
			log.log("Failed to load plugin details", e ); 
		}
	}
	
	protected void
	installUpdate(
		PluginInterface		plugin,
		String				download,
		String				version,
		InputStream			data )
	{
		log.log( LoggerChannel.LT_INFORMATION,
				 "Installing plugin " + plugin.getPluginID() + ", version " + version );
		
			// .jar files get copied straight in with the right version number
			// .zip files need to be unzipped. There are various possibilities for
			// target dir depending on the contents of the zip file. Basically we
			// need to remove any zip paths to ensure it ends up in the right place
			// There's also the issue of overwriting stuff like "plugin.properties"
			// and any other config files....
	}
	
	protected void
	logMultiLine(
		String		indent,
		String		text )
	{
		int		pos = 0;
		
		String	lc_text = text.toLowerCase();
		
		while( true ){
			
			String	line;
			
			int	p1 = lc_text.indexOf( "<br>", pos );
			
			if ( p1 == -1 ){
				
				line = text.substring(pos);
				
			}else{
				
				line = text.substring(pos,p1);
				
				pos = p1+4;
			}
			
			log.log( LoggerChannel.LT_INFORMATION, indent + line );
			
			if ( p1 == -1 ){
				
				break;
			}
		}
	}
}
