/*
 * Created on Dec 20, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.versioncheck;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;


/**
 * Client for checking version information from a remote server.
 */
public class VersionCheckClient {
  private static final String SERVER_ADDRESS = "azureus.aelitis.com";
  private static final int SERVER_PORT = 6868;
  
  private static final VersionCheckClient instance = new VersionCheckClient();
  private Map last_check_data = null;
  private final AEMonitor check_mon = new AEMonitor( "versioncheckclient" );
  private long last_check_time = 0;
  
  
  private VersionCheckClient() {
    /* blank */
  }
  
  
  /**
   * Get the singleton instance of the version check client.
   * @return version check client
   */
  public static VersionCheckClient getSingleton() {  return instance;  }
  
  
  
  /**
   * Get the version check reply info.
   * @return reply data, possibly cached, if the server was already checked within the last minute
   */
  public Map getVersionCheckInfo() {
    try {  check_mon.enter();
    
      long time_diff = SystemTime.getCurrentTime() - last_check_time;
      boolean force = time_diff > 60*1000 || time_diff < 0;
      
      if( last_check_data == null || last_check_data.size() == 0 || force ) {
        try {
          last_check_data = performVersionCheck( constructVersionCheckMessage() );
        }
        catch( Throwable t ) {
          t.printStackTrace();
          last_check_data = new HashMap();
        }
      }
      else {
        LGLogger.log( LGLogger.INFORMATION, "VersionCheckClient is using cached version check info. Using " +last_check_data.size()+ " reply keys." ); 
      }
    }
    finally {  check_mon.exit();  }
    
    return last_check_data;
  }
  
  
  
  /**
   * Get the ip address seen by the version check server.
   * NOTE: This information may be cached, see getVersionCheckInfo().
   * @return external ip address, or empty string if no address information found
   */
  public String getExternalIpAddress() {
    Map reply = getVersionCheckInfo();
    
    byte[] address = (byte[])reply.get("source_ip_address");
    if( address != null ) {
      return new String( address );
    }
    
    return new String();
  }
  
  
  
  
  /**
   * Perform the actual version check by connecting to the version server.
   * @param data_to_send version message
   * @return version reply
   * @throws Exception if the server check connection fails
   */
  private Map performVersionCheck( Map data_to_send ) throws Exception {
    LGLogger.log( LGLogger.INFORMATION, "VersionCheckClient retrieving version information from " +SERVER_ADDRESS+ ":" +SERVER_PORT ); 
    
    SocketChannel channel = null;
    
    try{
      channel = SocketChannel.open();
      channel.configureBlocking( true );
      channel.connect( new InetSocketAddress( SERVER_ADDRESS, SERVER_PORT ) );
      channel.finishConnect();
    
      ByteBuffer message = ByteBuffer.wrap( BEncoder.encode( data_to_send ) );
    
      StreamEncoder encoder = new StreamEncoder( "AZH", message );
    
      while( true ) {  //send message
        if( encoder.encode( channel ) ) {
          break;
        }
      }
    
      StreamDecoder decoder = new StreamDecoder( "AZR" );
    
      ByteBuffer reply;
      while( true ) {  //receive reply
        reply = decoder.decode( channel );
        if( reply != null ) {
          break;
        }
      }
    
      Map reply_message = BDecoder.decode( reply.array() );
      
      LGLogger.log( LGLogger.INFORMATION, "VersionCheckClient server version check successful. Received " +reply_message.size()+ " reply keys." );

      last_check_time = SystemTime.getCurrentTime();
      
      return reply_message;
    }
    finally {
      if( channel != null )  channel.close();
    }
  }
  
  
  
  /**
   * Construct the default version check message.
   * @return message to send
   */
  private Map constructVersionCheckMessage() {
    Map message = new HashMap();
    
    String id = COConfigurationManager.getStringParameter( "ID", null );
    boolean send_info = COConfigurationManager.getBooleanParameter( "Send Version Info" );
    
    if( id != null && send_info ) {
      
      message.put( "id", id );
      message.put( "version", Constants.AZUREUS_VERSION );
      message.put( "os", Constants.OSName );
      
      
      String  java_version = System.getProperty( "java.version" );
      if ( java_version == null ){  java_version = "unknown";  }
      message.put( "java", java_version );
      
      
      String  java_vendor = System.getProperty( "java.vm.vendor" );
      if ( java_vendor == null ){   java_vendor = "unknown";  }
      message.put( "javavendor", java_vendor );
      
      
      long  max_mem = Runtime.getRuntime().maxMemory()/(1024*1024);
      message.put( "javamx", new Long( max_mem ) );
      
      OverallStats	stats = StatsFactory.getStats();
      
      if ( stats != null ){
      	
	      long total_bytes_downloaded = StatsFactory.getStats().getDownloadedBytes();
	      long total_bytes_uploaded = StatsFactory.getStats().getUploadedBytes();
	      long total_uptime = StatsFactory.getStats().getTotalUpTime();
	
	      message.put( "total_bytes_downloaded", new Long( total_bytes_downloaded ) );
	      message.put( "total_bytes_uploaded", new Long( total_bytes_uploaded ) );
	      message.put( "total_uptime", new Long( total_uptime ) );
      }
      
      if ( AzureusCoreFactory.isCoreAvailable()){
      	
	      //installed plugin IDs
	      PluginInterface[] plugins = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaces();
	      List pids = new ArrayList();
	      for (int i=0;i<plugins.length;i++){
	        String  pid = plugins[i].getPluginID();
	        
	          // filter out built-in and core ones
	        if (  !pid.startsWith( "<" ) && 
	            !pid.startsWith( "azupdater" ) &&
	            !pid.startsWith( "azplatform" ) &&
	            !pids.contains( pid )){
	        
	          pids.add( pid );
	        }
	      }
	      message.put( "plugins", pids );
      }
    }
    
    
    //swt stuff
    try {
      Class c = Class.forName( "org.eclipse.swt.SWT" );
      
      String swt_platform = (String)c.getMethod( "getPlatform", new Class[]{} ).invoke( null, new Object[]{} );
      message.put( "swt_platform", swt_platform );
      
      if( send_info ) {
        Integer swt_version = (Integer)c.getMethod( "getVersion", new Class[]{} ).invoke( null, new Object[]{} );
        message.put( "swt_version", new Long( swt_version.longValue() ) );
      }
    }
    catch( ClassNotFoundException e ) {  /* ignore */ }
    catch( Throwable t ) {  t.printStackTrace();  }
    
    return message;
  }
  
}
