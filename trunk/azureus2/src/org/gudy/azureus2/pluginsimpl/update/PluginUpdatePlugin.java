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
import java.util.zip.*;
import java.net.URL;
import java.io.*;

import org.gudy.azureus2.core3.util.FileUtil;

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
		
	protected PluginInterface		plugin_interface;
	protected SFPluginDetailsLoader	loader;
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
		
		loader = SFPluginDetailsLoaderFactory.getSingleton();
		
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
		
		PluginView view = ui_manager.createPluginView( model );
		
		plugin_interface.addView( view );	
		
		BasicPluginConfigModel config = ui_manager.createBasicPluginConfigModel( "plugins", "plugins.update");
		
		config.addBooleanParameter( "enable.update", "Plugin.pluginupdate.enablecheck", true );
		
		UpdateManager	update_manager = plugin_interface.getUpdateManager();
		
		update_manager.addListener(
			new UpdateManagerListener()
			{
				public void
				checkInstanceCreated(
					UpdateCheckInstance	inst )
				{
					SFPluginDetailsLoaderFactory.getSingleton().reset();
				}
				
			});
		
		update_manager.registerUpdatableComponent( 
			new UpdatableComponent()
			{
				public void
				checkForUpdate(
					UpdateChecker	checker )
				{
					checkForUpdateSupport( checker, false );
				}
				
			}, false );
		
		update_manager.registerUpdatableComponent( 
				new UpdatableComponent()
				{
					public void
					checkForUpdate(
						UpdateChecker	checker )
					{
						checkForUpdateSupport( checker, true );
					}			
				}, true );	
		}
	
	protected synchronized void
	checkForUpdateSupport(
		UpdateChecker	checker,
		boolean			mandatory )
	{
		try{
			if ( 	(!mandatory) &&
					(!plugin_interface.getPluginconfig().getPluginBooleanParameter( "enable.update", true ))){
								
				return;
			}
			
			PluginInterface[]	plugins = plugin_interface.getPluginManager().getPlugins();
			
			log.log( LoggerChannel.LT_INFORMATION, "Currently loaded " + (mandatory?"mandatory ":"non-mandatory" ) + " plugins:");
	
			List	plugins_to_check 			= new ArrayList();
			List	plugins_to_check_ids		= new ArrayList();
			Map		plugins_to_check_unloadable = new HashMap();
			Map		plugins_to_check_names		= new HashMap();
			
			for (int i=0;i<plugins.length;i++){
				
				PluginInterface	pi = plugins[i];
				
				String	mand = pi.getPluginProperties().getProperty( "plugin.mandatory");
				
				boolean	pi_mandatory = mand != null && mand.trim().toLowerCase().equals("true");
				
				if ( pi_mandatory != mandatory ){
					
					continue;
				}
				
				String	id 		= pi.getPluginID();
				String	version = pi.getPluginVersion();
				String	name	= pi.getPluginName();
				
				if ( version != null && !id.startsWith("<")){
					
					if ( plugins_to_check_ids.contains( id )){
						
						String	s = (String)plugins_to_check_names.get(id);
						
						if ( !name.equals( id )){
							
							plugins_to_check_names.put( id, s+","+name);
						}
						
						Boolean	old_unloadable = (Boolean)plugins_to_check_unloadable.get(id);
						
						plugins_to_check_unloadable.put(id,new Boolean(pi.isUnloadable() && old_unloadable.booleanValue()));
						
					}else{
						plugins_to_check_ids.add( id );
						
						plugins_to_check.add( pi );
						
						plugins_to_check_names.put( id, name.equals(id)?"":name);
						
						plugins_to_check_unloadable.put( id, new Boolean( pi.isUnloadable()));
					}
				}
				
				log.log( LoggerChannel.LT_INFORMATION, "    " + pi.getPluginName() + ", id = " + id + (version==null?"":(", version = " + pi.getPluginVersion())));
			}
			
			String[]	names = loader.getPluginNames();
			
			String	name_list = "";
			
			for (int i=0;i<names.length;i++){
				
				name_list += (i==0?"":",") + names[i];
			}
			
			log.log( LoggerChannel.LT_INFORMATION, "Downloaded plugin ids = " + name_list );
			
			for ( int i=0;i<plugins_to_check.size();i++){
				
				final PluginInterface	pi_being_checked 	= (PluginInterface)plugins_to_check.get(i);
				final String			plugin_id 			= pi_being_checked.getPluginID();
								
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
				
				String			plugin_names		= (String)plugins_to_check_names.get( plugin_id );
				final boolean	plugin_unloadable 	= ((Boolean)plugins_to_check_unloadable.get( plugin_id )).booleanValue();
				
				log.log( LoggerChannel.LT_INFORMATION, "Checking " + plugin_id);
				
				try{
					
					SFPluginDetails	details = loader.getPluginDetails( plugin_id );
	
					boolean az_cvs = plugin_interface.getUtilities().isCVSVersion();
					
					String az_plugin_version	= pi_being_checked.getPluginVersion();
					
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
					
					if ( comp < 0 && ! ( pi_being_checked.getPlugin() instanceof UpdatableComponent)){
													
							// only update if newer verison + plugin itself doesn't handle
							// the update
						
						log.log( LoggerChannel.LT_INFORMATION, "    Description:" );
						
						List	update_desc = new ArrayList();
						
						List	desc_lines = splitMultiLine( "", details.getDescription());
						
						logMultiLine( "        ", desc_lines );
						
						update_desc.addAll( desc_lines );
						
						log.log( LoggerChannel.LT_INFORMATION, "    Comment:" );
						
						List	comment_lines = splitMultiLine( "    ", details.getComment());

						logMultiLine( "    ", comment_lines );
						
						update_desc.addAll( comment_lines );
						
						String msg =   "A newer version (version " + sf_plugin_version + ") of plugin '" + 
										plugin_id + "' " +
										(plugin_names.length()==0?"":"(" + plugin_names + ") " ) +
										"is available. ";
						
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
									final ResourceDownloader	downloader,
									InputStream					data )
								{	
										// during the update phase report any messages
										// to the downloader
									
									LoggerChannelListener	list = 
										new LoggerChannelListener()
										{
										public void
											messageLogged(
												int		type,
												String	content )
											{
												downloader.reportActivity( content );
											}
											
											public void
											messageLogged(
												String		str,
												Throwable	error )
											{
												downloader.reportActivity( str );
											}
										};
							
									try{
										
										log.addListener(list);
											
										installUpdate( 
												pi_being_checked,
												plugin_unloadable,
												f_sf_plugin_download, 
												f_sf_plugin_version, 
												data );
										
										return( true );
									}finally{
										
										log.removeListener( list );
									}
								}
								
								public void
								failed(
									ResourceDownloader			downloader,
									ResourceDownloaderException e )
								{
								}
								
							});
						
						String[]	update_d = new String[update_desc.size()];
						
						update_desc.toArray( update_d );
						
						Update update = checker.addUpdate(
								plugin_id + "/" + plugin_names,
								update_d,
								sf_plugin_version,
								rdl,
								plugin_unloadable?Update.RESTART_REQUIRED_NO:Update.RESTART_REQUIRED_YES );			
					}
				}catch( Throwable e ){
					
					log.log("    Plugin check failed", e ); 
				}
			}
						
		}catch( Throwable e ){
			
			log.log("Failed to load plugin details", e );
			
			checker.failed();
			
		}finally{
			
				// any prior failure will take precedence
			
			checker.completed();
		}
	}
	
	protected void
	installUpdate(
		PluginInterface		plugin,	// note this will be first one if > 1 defined
		boolean				unloadable,
		String				download,
		String				version,
		InputStream			data )
	{
		log.log( LoggerChannel.LT_INFORMATION,
				 "Installing plugin " + plugin.getPluginID() + ", version " + version );

		String	target_version = version.endsWith("_CVS")?version.substring(0,version.length()-4):version;

		try{				

				// .jar files get copied straight in with the right version number
				// .zip files need to be unzipped. There are various possibilities for
				// target dir depending on the contents of the zip file. Basically we
				// need to remove any zip paths to ensure it ends up in the right place
				// There's also the issue of overwriting stuff like "plugin.properties"
				// and any other config files....
			
			boolean jar = download.toLowerCase().endsWith(".jar");
				
			String	target = plugin.getPluginDirectoryName() + File.separator + 
								plugin.getPluginID() + "_" + target_version + (jar?".jar":".zip");
			
			FileUtil.copyFile( data, new FileOutputStream(target));
		
			if ( !jar ){
				
				ZipInputStream	zis = 
					new ZipInputStream( 
							new BufferedInputStream( new FileInputStream( target ) ));
				
				
					// first look for a common dir prefix 
				
				String	common_prefix = null;
				
				try{
					while( true ){
						
						ZipEntry	entry = zis.getNextEntry();
							
						if ( entry == null ){
							
							break;
						}
						
						String	name = entry.getName();
						
						if ( !name.endsWith("/")){
							
							if ( common_prefix == null ){
								
								common_prefix = name;
								
							}else{
								int	len = 0;
								
								for (int i=0;i<Math.min(common_prefix.length(), name.length());i++){
									
									if ( common_prefix.charAt(i) == name.charAt(i)){
										
										len++;
										
									}else{
										
										break;
									}
								}
								
								common_prefix = common_prefix.substring(0,len);
							}
						}
						
						long	rem = entry.getSize();
											
						byte[]	buffer = new byte[65536];
						
						while( rem > 0 ){
						
							int	len = zis.read( buffer, 0, buffer.length>rem?(int)rem:buffer.length);
							
							if ( len <= 0 ){
								
								break;
							}
							
							rem -= len;
						}
						
						if ( rem != 0 ){
							
							throw( new Exception( "Invalid ZIP file" ));
						}
					}
				}finally{
					
					zis.close();
				}
				

				if ( common_prefix != null ){
					
					int	pos = common_prefix.lastIndexOf("/");
					
					if ( pos == -1 ){
						
						common_prefix = "";
					}else{
						
						common_prefix = common_prefix.substring(0,pos+1);
					}
				
					
					zis = new ZipInputStream( 
								new BufferedInputStream( new FileInputStream( target ) ));
										
					try{
						while( true ){
								
							ZipEntry	entry = zis.getNextEntry();
									
							if ( entry == null ){
									
								break;
							}
							
							String	name = entry.getName();
							
							OutputStream	entry_os = null;
							
							File			initial_target			= null;
							File			final_target			= null;
							boolean			is_plugin_properties 	= false;
							
							try{
								if ( 	name.length() >= common_prefix.length() &&
										!name.endsWith("/")){
									
									String	file_name = entry.getName().substring( common_prefix.length());
									
									initial_target 	= new File( plugin.getPluginDirectoryName() + File.separator + file_name );
									
									final_target	= initial_target;
									
									if ( initial_target.exists()){
										
										if ( 	file_name.toLowerCase().endsWith(".properties") ||
												file_name.toLowerCase().endsWith(".config" )){
											
											is_plugin_properties	= file_name.toLowerCase().equals("plugin.properties");
											
											String	old_file_name = file_name;
											
											file_name = file_name + "_" + target_version;
											
											final_target = new File( plugin.getPluginDirectoryName() + File.separator + file_name );
											
											log.log( LoggerChannel.LT_INFORMATION,
														"saving new file '" + old_file_name + "'as '" + file_name +"'" );
										}else{
											
											log.log( LoggerChannel.LT_INFORMATION,
													"overwriting '" + file_name +"'" );
										}
									}
									
									final_target.getParentFile().mkdirs();
									
									entry_os = new FileOutputStream( final_target );
								}
								
								long	rem = entry.getSize();
								
								byte[]	buffer = new byte[65536];
								
								while( rem > 0 ){
								
									int	len = zis.read( buffer, 0, buffer.length>rem?(int)rem:buffer.length);
									
									if ( len <= 0 ){
										
										break;
									}
									
									rem -= len;
									
									if ( entry_os != null ){
										
										entry_os.write( buffer, 0, len );
									}
								}
							}finally{
								
								if ( entry_os != null ){
									
									entry_os.close();
								}
							}
							
							if ( is_plugin_properties ){
								
									// we've got to modify the plugin.version in the existing
									// file (if it exists) otherwise we keep downloading the new 
									// version! Or, if the key doesn't exist, add it!
								
									// if we were smarter we could merge values from the
									// old one into the new one, however this is too much like
									// hard work
																
								File	tmp_file 	= new File(initial_target.toString() + ".tmp");
								File	bak_file	= new File(initial_target.toString() + ".bak");
								
								LineNumberReader	lnr = null;
								
								PrintWriter			tmp = null;
								
								try{
									lnr = new LineNumberReader(new FileReader(initial_target));
								
									tmp = new PrintWriter(new FileWriter( tmp_file ));			
								
									tmp.println( "plugin.version=" + target_version );
									
								
									while(true){
										
										String	line = lnr.readLine();
										
										if ( line == null ){
											
											break;
										}
										
										if ( line.indexOf( "plugin.version" ) != -1 ){
										
												// skip existing entry
										}else{
										
											tmp.println( line );
										}
									}
								}finally{
									
									lnr.close();
									
									if ( tmp != null ){
										
										tmp.close();
									}
								}
								
								if ( bak_file.exists()){
									
									bak_file.delete();
								}
								
								if ( !initial_target.renameTo( bak_file)){
									
									throw( new IOException( "Failed to rename '" + initial_target.toString() + "' to '" + bak_file.toString() + "'" ));
								}
								
								if ( !tmp_file.renameTo( initial_target )){
									
									bak_file.renameTo( initial_target );
									
									throw( new IOException( "Failed to rename '" + tmp_file.toString() + "' to '" + initial_target.toString() + "'" ));
								}
								
								bak_file.delete();
							}
						}
					}finally{
							
						zis.close();
					}
				}
			}
			
			
			if ( unloadable ){
				
				plugin.reload();	// this will reload all if > 1 defined
			}	
			
			String msg =   "Version " + version + " of plugin '" + 
							plugin.getPluginID() + "' " +
							"installed successfully";

			log.logAlert( LoggerChannel.LT_INFORMATION, msg );			

		}catch( Throwable e ){
					
			String msg =   "Version " + version + " of plugin '" + 
							plugin.getPluginID() + "' " +
							"failed to install - " + (e.getMessage());

			log.log( msg, e );
		
			log.logAlert( LoggerChannel.LT_ERROR, msg );
		}	
	}
	
	protected void
	logMultiLine(
		String		indent,
		List		lines )
	{
		for (int i=0;i<lines.size();i++){
			
			log.log( LoggerChannel.LT_INFORMATION, indent + (String)lines.get(i) );
		}
	}
	
	protected List
	splitMultiLine(
		String		indent,
		String		text )
	{
		int		pos = 0;
		
		String	lc_text = text.toLowerCase();
		
		List	lines = new ArrayList();
		
		while( true ){
			
			String	line;
			
			int	p1 = lc_text.indexOf( "<br>", pos );
			
			if ( p1 == -1 ){
				
				line = text.substring(pos);
				
			}else{
				
				line = text.substring(pos,p1);
				
				pos = p1+4;
			}
			
			lines.add( indent + line );
			
			if ( p1 == -1 ){
				
				break;
			}
		}
		
		return( lines );
	}
}
