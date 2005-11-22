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


import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.stats.transfer.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.clientmessageservice.*;



/**
 * Client for checking version information from a remote server.
 */
public class VersionCheckClient {
	
	public static final String	REASON_UPDATE_CHECK_START		= "us";
	public static final String	REASON_UPDATE_CHECK_PERIODIC	= "up";
	public static final String	REASON_CHECK_SWT				= "sw";
	public static final String	REASON_DHT_EXTENDED_ALLOWED		= "dx";
	public static final String	REASON_DHT_ENABLE_ALLOWED		= "de";
	public static final String	REASON_EXTERNAL_IP				= "ip";
	public static final String	REASON_RECOMMENDED_PLUGINS		= "rp";
	
	
  private static final String SERVER_ADDRESS = "version.aelitis.com";
  private static final int SERVER_PORT = 27001;
  private static final String MESSAGE_TYPE_ID = "AZVER";
  private static final long		CACHE_PERIOD	= 5*60*1000;
  
  
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
  public Map getVersionCheckInfo( String reason ) {
    try {  check_mon.enter();
    
      long time_diff = SystemTime.getCurrentTime() - last_check_time;
      boolean force = time_diff > CACHE_PERIOD || time_diff < 0;
      
      if( last_check_data == null || last_check_data.size() == 0 || force ) {
        try {
          last_check_data = performVersionCheck( constructVersionCheckMessage( reason ) );
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
  
  private boolean
  isVersionCheckDataValid()
  {  	
  	return( last_check_data != null && last_check_data.size() > 0 ); 
  }
  
  /**
   * Get the ip address seen by the version check server.
   * NOTE: This information may be cached, see getVersionCheckInfo().
   * @return external ip address, or empty string if no address information found
   */
  public String getExternalIpAddress() {
    Map reply = getVersionCheckInfo( REASON_EXTERNAL_IP );
    
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
    Map reply = getVersionCheckInfo( REASON_DHT_ENABLE_ALLOWED );
    
    boolean	res = false;
    
    byte[] value = (byte[])reply.get( "enable_dht" );
    
    if( value != null ) {
    	
      res = new String( value ).equalsIgnoreCase( "true" );
    }
    
	// we take the view that if the version check failed then we go ahead
	// and enable the DHT (i.e. we're being optimistic)

    if ( !res ){
    	res = !isVersionCheckDataValid();
    }
    
    return res;
  }
  
  
  /**
   * Is the DHT allowed to be used by external plugins.
   * @return true if extended DHT use is allowed, false if not allowed
   */
  public boolean DHTExtendedUseAllowed() {
    Map reply = getVersionCheckInfo( REASON_DHT_EXTENDED_ALLOWED );
    
    boolean	res = false;
    
    byte[] value = (byte[])reply.get( "enable_dht_extended_use" );
    if( value != null ) {
      res = new String( value ).equalsIgnoreCase( "true" );
    }
    
    	// be generous and enable extended use if check failed
    
    if ( !res ){
    	res = !isVersionCheckDataValid();
    }
    
    return res;
  }
  
  public String[]
  getRecommendedPlugins()
  {
	  Map reply = getVersionCheckInfo( REASON_RECOMMENDED_PLUGINS );

	  List	l = (List)reply.get( "recommended_plugins" );
	  
	  if ( l == null ){
		  
		  return( new String[0] );
	  }
	  
	  String[]	res = new String[l.size()];
	  
	  for (int i=0;i<l.size();i++){
		  
		  res[i] = new String((byte[])l.get(i));
	  }
	  
	  return( res );
  }
  
  /**
   * Perform the actual version check by connecting to the version server.
   * @param data_to_send version message
   * @return version reply
   * @throws Exception if the server check connection fails
   */
  private Map performVersionCheck( Map data_to_send ) throws Exception {
  	
    LGLogger.log( LGLogger.INFORMATION, "VersionCheckClient retrieving version information from " +SERVER_ADDRESS+ ":" +SERVER_PORT ); 
    
    ClientMessageService 	msg_service = null;
    Map 					reply		= null;	
    
    try{
	    msg_service = ClientMessageServiceClient.getServerService( SERVER_ADDRESS, SERVER_PORT, MESSAGE_TYPE_ID );
	    
	    msg_service.sendMessage( data_to_send );  //send our version message
	
	    reply = msg_service.receiveMessage();  //get the server reply
	    
    }finally{
    	
    	if ( msg_service != null ){
    		
    		msg_service.close();
    	}
    }

    LGLogger.log( LGLogger.INFORMATION, "VersionCheckClient server version check successful. Received " +reply.size()+ " reply keys." );

    last_check_time = SystemTime.getCurrentTime();
      
    return reply;
  }
  
  
  
  /**
   * Construct the default version check message.
   * @return message to send
   */
  private Map constructVersionCheckMessage( String reason ) {
    Map message = new HashMap();
    
    message.put( "appid", SystemProperties.getApplicationIdentifier());
    message.put( "version", Constants.AZUREUS_VERSION );
    
    String id = COConfigurationManager.getStringParameter( "ID", null );
    boolean send_info = COConfigurationManager.getBooleanParameter( "Send Version Info" );
    
    int last_send_time = COConfigurationManager.getIntParameter( "Send Version Info Last Time", -1 );

    int current_send_time = (int)(SystemTime.getCurrentTime()/1000);
    
    COConfigurationManager.setParameter( "Send Version Info Last Time", current_send_time );
    
    if( id != null && send_info ) {
    	
      message.put( "id", id );
      message.put( "os", Constants.OSName );
    
      if ( last_send_time != -1 && last_send_time < current_send_time ){
    	  
    	  	// tims since last
    	  
    	  message.put( "tsl", new Long(current_send_time-last_send_time));
      }
      
      message.put( "reason", reason );
      
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
	        
        	String	info = (String)plugins[i].getPluginconfig().getPluginStringParameter( "plugin.info" );
        	
	          // filter out built-in and core ones
	        if ( 	( info != null && info.length() > 0 ) ||
	        		(	!pid.startsWith( "<" ) && 
		        		!pid.startsWith( "azbp" ) &&
		        		!pid.startsWith( "azupdater" ) &&
	        			!pid.startsWith( "azplatform" ) &&
	        			!pids.contains( pid ))){
	        
	        	if ( info != null && info.length() > 0 ){
	        		
	        		if( info.length() < 256 ){
	        			
	        			pid += ":" + info;
	        			
	        		}else{
	        			
	        			Debug.out( "Plugin '" + pid + "' reported excessive info string '" + info + "'" );
	        		}
	        	}
	        	
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
    catch( NoClassDefFoundError er ) {  /* ignore */ }
    catch( Throwable t ) {  t.printStackTrace();  }
    
    return message;
  }
  
}
