/*
 * Created on Dec 20, 2004
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.versioncheck;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;


/**
 *
 */
public class VersionCheckClient {

  private static final VersionCheckClient instance = new VersionCheckClient();
  
  
  
  private VersionCheckClient() {
    
  }
  
  
  public static VersionCheckClient getSingleton() {  return instance;  }
  
  
  
  
  private Map performVersionCheck( Map data_to_send ) throws Exception {
    SocketChannel channel = null;
    
    try{
      channel = SocketChannel.open();
      channel.configureBlocking( true );
      channel.connect( new InetSocketAddress( "azureus.aelitis.com", 6868 ) );
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

      return reply_message;
    }
    finally {
      if( channel != null )  channel.close();
    }
  }
  
  
  
  
  
  
  private Map constructVersionCheckMessage() {
    Map message = new HashMap();
    
    String id = COConfigurationManager.getStringParameter("ID",null);
    
    if ( id != null && COConfigurationManager.getBooleanParameter("Send Version Info")){
      
      message.put( "id", id );
      message.put( "version", Constants.AZUREUS_VERSION );
      message.put( "os", Constants.OSName );
      
      
      String  java_version = System.getProperty("java.version");
      if ( java_version == null ){  java_version = "unknown";  }
      message.put( "java", java_version );
      
      
      String  java_vendor = System.getProperty( "java.vm.vendor" );
      if ( java_vendor == null ){   java_vendor = "unknown";  }
      message.put( "javavendor", java_vendor );
      
      
      long  max_mem = Runtime.getRuntime().maxMemory()/(1024*1024);
      message.put( "javamx", new Long( max_mem ) );
      
      
      //TODO total uploaded/download numbers
      
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
    
    
    //TODO swt version check
    
    
    return message;
  }
  
}
