/*
 * Created on Mar 14, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager;

import java.util.HashMap;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.*;

/**
 *
 */
public class PeerDownloadManager {
  
  private static final int UNLIMITED_READ_RATE = 1024 * 1024 * 100; //100 mbyte/s
  
  private int global_max_rate_bps;
  private final ByteBucket global_bucket;
  
  private final HashMap peer_connections = new HashMap();
  private final AEMonitor peer_connections_mon = new AEMonitor( "PeerDownloadManager:PC" );
  
  private final HashMap group_buckets = new HashMap();
  private final AEMonitor group_buckets_mon = new AEMonitor( "PeerDownloadManager:GB" );
  
  
  
  
  protected PeerDownloadManager() {
    int max_rateKBs = COConfigurationManager.getIntParameter( "Max Download Speed KBs" );
    global_max_rate_bps = max_rateKBs == 0 ? UNLIMITED_READ_RATE : max_rateKBs * 1024;
    COConfigurationManager.addParameterListener( "Max Download Speed KBs", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        int rateKBs = COConfigurationManager.getIntParameter( "Max Download Speed KBs" );
        global_max_rate_bps = rateKBs == 0 ? UNLIMITED_READ_RATE : rateKBs * 1024;
      }
    });
    
    global_bucket = new ByteBucket( global_max_rate_bps ); 
    
    /*
    RateHandler rate_handler = new RateHandler() {
      public int getCurrentNumBytesAllowed() {
        if( global_bucket.getRate() != global_max_rate_bps ) { //sync rate
          global_bucket.setRate( global_max_rate_bps );
        }
        return global_bucket.getAvailableByteCount();
      }
      
      public void bytesWritten( int num_bytes_written ) {
        global_bucket.setBytesUsed( num_bytes_written );
      }
    };
    */
    
    
  }
  

}
