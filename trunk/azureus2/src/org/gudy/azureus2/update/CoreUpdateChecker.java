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
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.html.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
CoreUpdateChecker
	implements Plugin, UpdatableComponent
{
	public static final int	RD_GET_DETAILS_RETRIES	= 3;
	public static final int	RD_GET_MIRRORS_RETRIES	= 3;
	
	public static final int	RD_SIZE_RETRIES	= 3;
	public static final int	RD_SIZE_TIMEOUT	= 10000;

	protected PluginInterface				plugin_interface;
	protected ResourceDownloaderFactory 	rdf;
	protected LoggerChannel					log;
	protected ResourceDownloaderListener	rd_logger;
	
	public void
	initialize(
		PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
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
	
	public void
	checkForUpdate(
		final UpdateChecker	checker )
	{
		try{			
			String	current_version = plugin_interface.getAzureusVersion();
			
			log.log( "Update check starts: current = " + current_version );
						
			String id = COConfigurationManager.getStringParameter("ID",null);
			
			String url_str = Constants.SF_WEB_SITE + "version.php";
			
			boolean	TESTING = true;	// !!!!TODO: REMOVE THIS
			
			if ( TESTING ){
			
				System.out.println( "CoreUpdater: ID deliberately currently removed!!!!" );
				
			}else{
				if ( id != null && COConfigurationManager.getBooleanParameter("Send Version Info")){
				
					url_str += "?id=" + id + "&version=" + Constants.AZUREUS_VERSION;
				}
			}
			
			URL url = new URL(url_str); 
			
			ResourceDownloader	rd = rdf.create( url );
			
			rd = rdf.getRetryDownloader( rd, RD_GET_DETAILS_RETRIES );
			
			rd.addListener( rd_logger );
			
			InputStream	data = rd.download();
			
			Map decoded = BDecoder.decode(new BufferedInputStream(data));
			
			String latest_version 			= null;
			String latest_file_name			= null;
			
			byte[] b_version = (byte[])decoded.get("version");
			
			if ( b_version != null ){
			
				latest_version = new String( b_version );
				
			}else{
				
				throw( new Exception( "No version found in reply" ));
			}
			
			byte[] b_filename = (byte[]) decoded.get("filename");
			
			if ( b_filename != null ){
			
				latest_file_name = new String( b_filename );
			}
			
			log.log( "Retrieved: latest_version = '" + latest_version + "', file = '" + latest_file_name + "'");
			
			boolean	latest_is_cvs	= Constants.isCVSVersion( latest_version );
			String	latest_base		= Constants.getBaseVersion( latest_version );
			
			boolean	current_is_cvs	= Constants.isCVSVersion();
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
			
			URL[]	primary_mirrors = getPrimaryURLs( latest_file_name );
			
			/*
			boolean current_az_is_cvs	= Constants.isCVSVersion();
				
			
			String	target_dll_version	= null;
			
			String sf_plugin_version	= sf_details.getVersion();
			
			String sf_comp_version	 	= sf_plugin_version;
	
			String target_download		= sf_details.getDownloadURL();
			
			if ( current_az_is_cvs ){
				
				String	sf_cvs_version = sf_details.getCVSVersion();
				
				if ( sf_cvs_version.length() > 0 ){
					
						// sf cvs version ALWAYS entry in _CVS
					
					sf_plugin_version	= sf_cvs_version;
					
					sf_comp_version = sf_plugin_version.substring(0,sf_plugin_version.length()-4);
					
					target_download	= sf_details.getCVSDownloadURL();
				}
			}
						
			if ( Constants.compareVersions( current_dll_version, sf_comp_version ) < 0 ){
				
				target_dll_version	= sf_comp_version;
			}
	
			LGLogger.log( "PlatformManager:Win32 update required = " + (target_dll_version!=null));
			
			if ( target_dll_version != null ){
							
				ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
				
				ResourceDownloader dll_rd = rdf.create( new URL( target_download ));
			
					// get size here so it is cached
				
				rdf.getTimeoutDownloader(rdf.getRetryDownloader(dll_rd,RD_SIZE_RETRIES),RD_SIZE_TIMEOUT).getSize();

				final String f_target_dll_version	= target_dll_version;
				
				dll_rd.addListener( 
						new ResourceDownloaderAdapter()
						{
							public boolean
							completed(
								final ResourceDownloader	downloader,
								InputStream					data )
							{	
								installUpdate( checker, downloader, f_target_dll_version, data );
									
								return( true );
							}
						});

				checker.addUpdate(
						"Windows native support: " + PlatformManagerImpl.DLL_NAME + ".dll",
						new String[]{"This DLL supports native operations such as file-associations" },
						target_dll_version,
						dll_rd,
						Update.RESTART_REQUIRED_YES );
			}
			*/
		}catch( Throwable e ){
			
			log.log( e );
			
			e.printStackTrace();
			
			checker.failed();
			
		}finally{
			
			checker.completed();
		}
	}
	
	protected URL[]
	getPrimaryURLs(
		String		latest_file_name )
	{
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
		
		URL[]	urls = new URL[res.size()];
		
		res.toArray( urls );
		
		for (int i=0;i<urls.length;i++){
			
			log.log( "Primary mirror:" + urls[i].toString());
		}
		
		return( urls );
	}
		
		/*

      //Grab a random mirror
      if ( mirrors.size() > 0 ) {
        int random = (int) (Math.random() * mirrors.size());
        String mirror = (String) (mirrors.get(random));

        URL mirrorUrl = new URL("http://prdownloads.sourceforge.net" + mirror);
        String mirrorHtml = readUrl(mirrorUrl);
        pattern = "<META HTTP-EQUIV=\"refresh\" content=\"5; URL=";
        position = mirrorHtml.indexOf(pattern);
        if ( position >= 0 ) {
        	int end = mirrorHtml.indexOf("\">", position);
          if ( end >= 0 ) {
        	  reqUrl = new URL(mirrorHtml.substring(position + pattern.length(), end));
            foundMirror = true;
          }
        }
      }
    }

    if (reqUrl == null || !foundMirror) {
    	reqUrl = getMirrorFromBackupList( log );
    }
    
    HttpURLConnection con = null;
    try {
    	con = (HttpURLConnection) reqUrl.openConnection();
    	con.connect();
      in = con.getInputStream();
    } catch (IOException e) {
      //probably a 404 error, try one last time
      if (con != null)  con.disconnect();
      URL backup = getMirrorFromBackupList( log );
      con = (HttpURLConnection) backup.openConnection();
      con.connect();
      in = con.getInputStream();
    }
    
    
    private URL getMirrorFromBackupList( FileWriter log ) {
        try{ log.write("Retrieving backup SF mirror list..."); } catch (Exception e) {}
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        int nbRead = 0;
        HttpURLConnection con = null;
        InputStream is = null;
        try {
          String url = "http://azureus.sourceforge.net/mirrors.php";
          URL mirrorUrl = new URL(url);
          con = (HttpURLConnection) mirrorUrl.openConnection();
          con.connect();
          is = con.getInputStream();
          byte[] data = new byte[1024];
          while (nbRead >= 0) {
            nbRead = is.read(data);
            if (nbRead >= 0) {
              message.write(data, 0, nbRead);
            }
          }
          Map decoded = BDecoder.decode(message.toByteArray());
          List mirrors = (List)decoded.get("mirrors");
          int random = (int) (Math.random() * mirrors.size());
          String mirror = new String( (byte[])mirrors.get(random) );
          return new URL( mirror + latestVersionFileName );
        }
        catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      }
      */
	/*
	protected void
	installUpdate(
		UpdateChecker		checker,
		ResourceDownloader	rd,
		String				version,
		InputStream			data )
	{
		try{
			String	temp_dll_name 	= PlatformManagerImpl.DLL_NAME + "_" + version + ".dll";
			String	target_dll_name	= PlatformManagerImpl.DLL_NAME + ".dll";
			
			
			UpdateInstaller	installer = checker.createInstaller();
			
			installer.addResource( temp_dll_name, data );
			
			installer.addMoveAction( 
					temp_dll_name,
					installer.getInstallDir() + File.separator + target_dll_name );
			
		}catch( Throwable e ){
			
			rd.reportActivity("Update install failed:" + e.getMessage());
		}
	}
	*/
}
