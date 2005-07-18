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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.TCPTransport;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;


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
    
    if( last_check_data == null )  last_check_data = new HashMap();
    
    return last_check_data;
  }
  
  public boolean
  isVersionCheckDataValid()
  {
  	getVersionCheckInfo();
  	
  	return( last_check_data != null && last_check_data.size() > 0 ); 
  }
  
  /**
   * Get the ip address seen by the version check server.
   * NOTE: This information may be cached, see getVersionCheckInfo().
   * @return external ip address, or empty string if no address information found
   */
  public String getExternalIpAddress() {
    Map reply = getVersionCheckInfo();
    
    byte[] address = (byte[])reply.get( "source_ip_address" );
    if( address != null ) {
      return new String( address );
    }
    
    return new String();
  }
  
  
  /**
   * Is the DHT plugin allowed to be enabled.
   * @return true if DHT can be enabled, false if it should not be enabled
   */
  public boolean DHTEnableAllowed() {
    Map reply = getVersionCheckInfo();
    
    byte[] value = (byte[])reply.get( "enable_dht" );
    if( value != null ) {
      return( new String( value ).equalsIgnoreCase( "true" ));
    }
    
    return false;
  }
  
  
  /**
   * Is the DHT allowed to be used by external plugins.
   * @return true if extended DHT use is allowed, false if not allowed
   */
  public boolean DHTExtendedUseAllowed() {
    Map reply = getVersionCheckInfo();
    
    byte[] value = (byte[])reply.get( "enable_dht_extended_use" );
    if( value != null ) {
      return( new String( value ).equalsIgnoreCase( "true" ));
    }
    
    return false;
  }
  

  
  /**
   * Perform the actual version check by connecting to the version server.
   * @param data_to_send version message
   * @return version reply
   * @throws Exception if the server check connection fails
   */
  private Map performVersionCheck( Map data_to_send ) throws Exception {
    LGLogger.log( LGLogger.INFORMATION, "VersionCheckClient retrieving version information from " +SERVER_ADDRESS+ ":" +SERVER_PORT ); 
    
    final TCPTransport transport = new TCPTransport();  //use transport for proxy capabilities
    final AESemaphore block = new AESemaphore( "versioncheck" );
    final Throwable[] errors = new Throwable[1];
    
    transport.establishOutboundConnection( new InetSocketAddress( SERVER_ADDRESS, SERVER_PORT ), new TCPTransport.ConnectListener() {  //NOTE: async operation!
      public void connectAttemptStarted() {  /*nothing*/ }
      
     public void connectSuccess() {
       block.release();       
     }
     
     public void connectFailure( Throwable failure_msg ) {
       errors[0] = failure_msg;
       block.release();  
     }
    });
    
    block.reserve();  //block while waiting for connect
    
    //connect op finished   
    
    if( errors[0] != null ) {  //connect failure
      transport.close();
      String error = errors[0].getMessage();
      throw new IOException( "version check connect operation failed: " + error == null ? "[]" : error );
    }
    
    //connect success  
    try{    
      final StreamDecoder decoder = new StreamDecoder( "AZR" );
      final ByteBuffer[] reply = new ByteBuffer[1];
         
      NetworkManager.getSingleton().getReadSelector().register( transport.getSocketChannel(), new VirtualChannelSelector.VirtualSelectorListener() {
        public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc,Object attachment ) {
          try {
            reply[0] = decoder.decode( transport );
            
            if( reply[0] != null ) {  //reading complete
              NetworkManager.getSingleton().getReadSelector().cancel( transport.getSocketChannel() );
              block.release();
            }
            else {
              NetworkManager.getSingleton().getReadSelector().resumeSelects( transport.getSocketChannel() );
            }
          }
          catch( Throwable t ) {
            errors[0] = t;
            NetworkManager.getSingleton().getReadSelector().cancel( transport.getSocketChannel() );
            block.release();
          }
          return true;
        }
        
        public void selectFailure( VirtualChannelSelector selector, SocketChannel sc,Object attachment, Throwable msg ) {
          errors[0] = msg;
          NetworkManager.getSingleton().getReadSelector().cancel( transport.getSocketChannel() );
          block.release();
        }
      }, null );

      ByteBuffer message = ByteBuffer.wrap( BEncoder.encode( data_to_send ) );
      StreamEncoder encoder = new StreamEncoder( "AZH", message );
      
      while( true ) {  //send message
        if( encoder.encode( transport ) ) {
          break;
        }
      }

      block.reserve();  //block while waiting for read completion
      
      //read op finished
      
      if( errors[0] != null ) {  //read failure
        transport.close();
        String error = errors[0].getMessage();
        throw new IOException( "version check read operation failed: " + error == null ? "[]" : error );
      }
      
      //read success
      
      Map reply_message = BDecoder.decode( reply[0].array() );
      
      LGLogger.log( LGLogger.INFORMATION, "VersionCheckClient server version check successful. Received " +reply_message.size()+ " reply keys." );

      last_check_time = SystemTime.getCurrentTime();
      
      return reply_message;
    }
    finally {
      transport.close();
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
      message.put( "appid", SystemProperties.getApplicationIdentifier());
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
      	
	      long total_bytes_downloaded 	= stats.getDownloadedBytes();
	      long total_bytes_uploaded		= stats.getUploadedBytes();
	      long total_uptime 			= stats.getTotalUpTime();
	
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
