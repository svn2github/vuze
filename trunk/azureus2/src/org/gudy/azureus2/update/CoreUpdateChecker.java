/*
 * Created on 20-May-2004
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

package org.gudy.azureus2.update;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.html.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.logging.LoggerImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import com.aelitis.azureus.core.versioncheck.*;


public class 
CoreUpdateChecker
	implements Plugin, UpdatableComponent
{
	public static final String	LATEST_VERSION_PROPERTY	= "latest_version";
	public static final String	MESSAGE_PROPERTY		= "message";
	
	public static final int	RD_GET_DETAILS_RETRIES	= 3;
	public static final int	RD_GET_MIRRORS_RETRIES	= 3;
	
	public static final int	RD_SIZE_RETRIES	= 3;
	public static final int	RD_SIZE_TIMEOUT	= 10000;

	protected static CoreUpdateChecker		singleton;
	
	protected PluginInterface				plugin_interface;
	protected ResourceDownloaderFactory 	rdf;
	protected LoggerChannel					log;
	protected ResourceDownloaderListener	rd_logger;
	
	protected boolean						first_check		= true;
	
	public static void
	doUsageStats()
	{
		singleton.doUsageStatsSupport();
	}
	
	public
	CoreUpdateChecker()
	{
		singleton	= this;
	}
	
	protected void
	doUsageStatsSupport()
	{
		try{
			Map decoded = VersionCheckClient.getSingleton().getVersionCheckInfo(
			  			first_check?VersionCheckClient.REASON_UPDATE_CHECK_START:VersionCheckClient.REASON_UPDATE_CHECK_PERIODIC);
    	  
			displayUserMessage( decoded );
			
		}finally{
			
			  first_check	= false;
		}
	}

	
	public void
	initialize(
		PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Core Updater" );
		
		log	= plugin_interface.getLogger().getChannel("CoreUpdater");
		
		rd_logger =
			new ResourceDownloaderAdapter()
			{
				public void
				reportActivity(
					ResourceDownloader	downloader,
					String				activity )
				{
					log.log( activity );
				}
			};
			
		Properties	props = plugin_interface.getPluginProperties();
							
		props.setProperty( "plugin.version", plugin_interface.getAzureusVersion());
		
		rdf = plugin_interface.getUtilities().getResourceDownloaderFactory();
	
		plugin_interface.getUpdateManager().registerUpdatableComponent( this, true );
	}
	
	public String
	getName()
	{
		return( "Azureus Core" );
	}
	
	public int
	getMaximumCheckTime()
	{
		return( ( RD_SIZE_RETRIES * RD_SIZE_TIMEOUT )/1000);
	}	

	public void
	checkForUpdate(
		final UpdateChecker	checker )
	{
		try{			
			String	current_version = plugin_interface.getAzureusVersion();
			
			log.log( "Update check starts: current = " + current_version );
									
			boolean	TESTING = false;	// !!!!TODO: REMOVE THIS
			
			if ( TESTING ){
			
				System.out.println( "CoreUpdater: !!!! Testing mode !!!!" );
				
			}
				
			Map	decoded = VersionCheckClient.getSingleton().getVersionCheckInfo(
		  			first_check?VersionCheckClient.REASON_UPDATE_CHECK_START:VersionCheckClient.REASON_UPDATE_CHECK_PERIODIC);

      
			displayUserMessage( decoded );
			
			String latest_version 			= null;
			String latest_file_name			= null;
			
			byte[] b_version = (byte[])decoded.get("version");
			
			if ( b_version != null ){
			
				latest_version = new String( b_version );
				
				plugin_interface.getPluginProperties().setProperty( LATEST_VERSION_PROPERTY, latest_version );
				
			}else{
				
				throw( new Exception( "No version found in reply" ));
			}
			
			byte[] b_filename = (byte[]) decoded.get("filename");
			
			if ( b_filename != null ){
			
				latest_file_name = new String( b_filename );
			}
			
			String	msg = "Core: latest_version = '" + latest_version + "', file = '" + latest_file_name + "'";
			
			checker.reportProgress( msg );
			
			log.log( msg );
			
			boolean	latest_is_cvs	= Constants.isCVSVersion( latest_version );
			String	latest_base		= Constants.getBaseVersion( latest_version );
			
			String	current_base	= Constants.getBaseVersion();
				
				// currently we upgrade from, for example
				//  1) 2.0.8.4     -> 2.0.8.6
				//	2) 2.0.8.5_CVS -> 2.0.8.6
				// but not to a CVS version (also currently never reported as latest...)
				
			if ( latest_is_cvs && !TESTING ){
				
				return;
			}

			if ( Constants.compareVersions( current_base, latest_base ) >= 0 && !TESTING){
				
				return;
			}
			
			final String	f_latest_version	= latest_version;
			final String	f_latest_file_name	= latest_file_name;
			

			ResourceDownloader[]	primary_mirrors;
				
			primary_mirrors = getPrimaryDownloaders( latest_file_name );

				// the download hierarchy is primary mirrors first (randomised alternate)
				// then backup mirrors (randomised alternate)
			
				// we don't want to load the backup mirrors until the primary mirrors fail
			
			ResourceDownloader		random_primary_mirrors = rdf.getRandomDownloader( primary_mirrors );
			
			ResourceDownloader		backup_downloader =
				rdf.create(
					new ResourceDownloaderDelayedFactory()
					{
						public ResourceDownloader
						create()
						{
							ResourceDownloader[]	backup_mirrors = getBackupDownloaders( f_latest_file_name );
						
							return( rdf.getRandomDownloader( backup_mirrors ));
						}
					});
			
			ResourceDownloader	top_downloader = 
				rdf.getAlternateDownloader( 
						new ResourceDownloader[]
							{
								random_primary_mirrors,
								backup_downloader,
							});
			

			top_downloader.addListener( rd_logger );
			
				// get size so it is cached
			
			top_downloader.getSize();		
							
			top_downloader.addListener( 
					new ResourceDownloaderAdapter()
					{
						public boolean
						completed(
							final ResourceDownloader	downloader,
							InputStream					data )
						{	
							installUpdate( checker, downloader, f_latest_version, data );
									
							return( true );
						}
					});

			byte[]	info_b = (byte[])decoded.get( "info" );
			
			String	info = null;
			
			if ( info_b != null ){
			
				try{
					info = new String( info_b );
				
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
			
			String[]	desc;
			
			if ( info == null ){
				
				desc = new String[]{"Core Azureus Version" };
				
			}else{
				
				desc = new String[]{"Core Azureus Version", info };
			}
			
			checker.addUpdate(
						"Core Azureus Version",
						desc,
						latest_version,
						top_downloader,
						Update.RESTART_REQUIRED_YES );
			
		}catch( Throwable e ){
			
			log.log( e );
			
			Debug.printStackTrace( e );
			
			checker.failed();
			
		}finally{
			
			checker.completed();
			
			first_check = false;
		}
	}
	
      
  
  
  /**
   * Log and display a user message if contained within reply.
   * @param reply from server
   */
  private void displayUserMessage( Map reply ) {
    //  pick up any user message in the reply
    
    try{
      byte[]  message = (byte[])reply.get( "message" );
          
      if ( message != null && message.length > 0 ){
        
        String  s_message = new String(message);
        
        String  last = COConfigurationManager.getStringParameter( "CoreUpdateChecker.lastmessage", "" );
        
        if ( !s_message.equals( last )){
          
          int   alert_type    = LogAlert.AT_WARNING;
          String  alert_text    = s_message;
          
          if ( alert_text.startsWith("i:" )){
          
            alert_type = LogAlert.AT_INFORMATION;
            
            alert_text = alert_text.substring(2);
          }
          
          plugin_interface.getPluginProperties().setProperty( MESSAGE_PROPERTY, alert_text );


          Logger.log(new LogAlert(LogAlert.UNREPEATABLE, alert_type, alert_text));
          
          COConfigurationManager.setParameter( "CoreUpdateChecker.lastmessage", s_message );
          
          COConfigurationManager.save();
        }
      }
    }catch( Throwable e ){
      
      Debug.printStackTrace( e );
    }
  } 
	
	protected ResourceDownloader[]
	getPrimaryDownloaders(
		String		latest_file_name )
	{
		log.log( "Downloading primary mirrors" );
		
		List	res = new ArrayList();

		try{
			if ( latest_file_name == null ){
		
					// very old method, non-mirror based
				
				res.add( new URL( Constants.SF_WEB_SITE + "Azureus2.jar" ));
								
			}else{
		
				URL mirrors_url = new URL("http://prdownloads.sourceforge.net/azureus/" + latest_file_name + "?download");
				
				ResourceDownloader	rd = rdf.create( mirrors_url );
				
				rd = rdf.getRetryDownloader( rd, RD_GET_MIRRORS_RETRIES );
				
				rd.addListener( rd_logger );
				
				String	page = HTMLPageFactory.loadPage( rd.download()).getContent();
				
				String pattern = "/azureus/" + latest_file_name + "?use_mirror=";
	     
				int position = page.indexOf(pattern);
				
				while ( position > 0 ){
					
					int end = page.indexOf(">", position);
					
					if (end < 0) {
						
						position = -1;
						
					}else{
						
						String mirror = page.substring(position, end);
						
						if ( mirror.endsWith("\"")){
							
							mirror = mirror.substring(0,mirror.length()-1);
						}
						
						try{
							res.add( new URL( "http://prdownloads.sourceforge.net" + mirror ));
							
						}catch( Throwable e ){
							
							log.log( "Invalid URL read:" + mirror, e );
						}
	          
						position = page.indexOf(pattern, position + 1);
					}
				}
			}
		}catch( Throwable e ){
			
			log.log( "Failed to read primary mirror list", e );
		}
		
		ResourceDownloader[]	dls = new ResourceDownloader[res.size()];
				
		for (int i=0;i<res.size();i++){
			
			URL	url =(URL)res.get(i);
			
			log.log( "    Primary mirror:" +url.toString());
			
			ResourceDownloader dl = rdf.create( url );
			
			dl = rdf.getMetaRefreshDownloader( dl );
			
				// add in a layer to do torrent based downloads if url ends with .torrent
			
			dl = rdf.getSuffixBasedDownloader( dl );
			
			dls[i] = dl;
		}
		
		return( dls );
	}
	
	protected ResourceDownloader[]
	getBackupDownloaders(
		String	latest_file_name )
	{
		List	res = new ArrayList();
	
		try{
			if ( latest_file_name != null ){
							
				log.log( "Downloading backup mirrors" );
				
				URL mirrors_url = new URL("http://azureus.sourceforge.net/mirrors.php");
				
				ResourceDownloader	rd = rdf.create( mirrors_url );
				
				rd = rdf.getRetryDownloader( rd, RD_GET_MIRRORS_RETRIES );
				
				rd.addListener( rd_logger );
				
				BufferedInputStream	data = new BufferedInputStream(rd.download());
				
				Map decoded = BDecoder.decode(data);
				
				data.close();
				
				List mirrors = (List)decoded.get("mirrors");
		
				for (int i=0;i<mirrors.size();i++){
					
					String mirror = new String( (byte[])mirrors.get(i));
					
					try{
						
						res.add( new URL( mirror + latest_file_name ));
						
					}catch(Throwable e){
						
						log.log( "Invalid URL read:" + mirror, e );
					}
				}
			}
		}catch( Throwable e ){
			
			log.log( "Failed to read backup mirror list", e );
		}
		
		ResourceDownloader[]	dls = new ResourceDownloader[res.size()];
		
		for (int i=0;i<res.size();i++){
			
			URL	url =(URL)res.get(i);
			
			log.log( "    Backup mirror:" +url.toString());
			
			ResourceDownloader dl = rdf.create( url );

				// add in .torrent decoder if appropriate
			
			dl = rdf.getSuffixBasedDownloader( dl );

			dls[i] = dl;
		}
		
		return( dls );
	}       	

	protected void
	installUpdate(
		UpdateChecker		checker,
		ResourceDownloader	rd,
		String				version,
		InputStream			data )
	{
		try{
			String	temp_jar_name 	= "Azureus2_" + version + ".jar";
			String	target_jar_name	= "Azureus2.jar";
			
			UpdateInstaller	installer = checker.createInstaller();
			
			installer.addResource( temp_jar_name, data );
			
			if ( Constants.isOSX ){
				
				installer.addMoveAction( 
					temp_jar_name,
					installer.getInstallDir() + "/" + SystemProperties.getApplicationName() + ".app/Contents/Resources/Java/" + target_jar_name );        
			}else{
				
				installer.addMoveAction( 
					temp_jar_name,
					installer.getInstallDir() + File.separator + target_jar_name );
			}
		}catch( Throwable e ){
			
			rd.reportActivity("Update install failed:" + e.getMessage());
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		AEDiagnostics.startup();
		
		CoreUpdateChecker	checker = new CoreUpdateChecker();
		
		checker.log = new LoggerImpl(null).getTimeStampedChannel("");
		checker.rdf	= new ResourceDownloaderFactoryImpl();
		checker.rd_logger = 
			new ResourceDownloaderAdapter()
			{
				public void
				reportActivity(
					ResourceDownloader	downloader,
					String				activity )
				{
					System.out.println( activity );
				}
				
				public void
				reportPercentComplete(
					ResourceDownloader	downloader,
					int					percentage )
				{
					System.out.println( "    % = " + percentage );
				}
			};
			
		ResourceDownloader[]	primaries = checker.getPrimaryDownloaders( "Azureus-2.0.3.0.jar" );
		
		for (int i=0;i<primaries.length;i++){
			
			System.out.println( "primary: " + primaries[i].getName());
		}
		
		try{
			ResourceDownloader	rd = primaries[0];
			
			rd.addListener( checker.rd_logger );
			
			rd.download();
			
			System.out.println( "done" );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
