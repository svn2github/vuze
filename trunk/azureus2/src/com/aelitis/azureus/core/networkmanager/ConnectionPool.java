/*
 * Created on Jul 28, 2004
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

package com.aelitis.azureus.core.networkmanager;


import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SystemTime;


/**
 *
 */
public class ConnectionPool {
  private static final int FLUSH_WAIT_TIME = 3*1000;  //3sec no-new-data wait before forcing write flush
  
  private final ConnectionPool parent_pool;
  private final LinkedList children_pools = new LinkedList();
  
  private final LinkedList connections = new LinkedList();
  private final ArrayList added_connections = new ArrayList();
  private final ArrayList removed_connections = new ArrayList();

  private final ByteBucket write_bytebucket;
  private float write_percent_of_max;
  
  private static final VirtualChannelSelector.VirtualSelectorListener write_select_listener = new VirtualChannelSelector.VirtualSelectorListener() {
    public void channelSuccessfullySelected( Object attachment ) {
      ((Connection)attachment).setTransportReadyForWrite( true );
    }
  };
  
  
  /**
   * Constructor for creating root pool.
   */
  protected ConnectionPool( int max_write_rate_bytes_per_sec ) {
    parent_pool = null;
    write_percent_of_max = 1.0F;
    write_bytebucket = new ByteBucket( max_write_rate_bytes_per_sec );
    write_bytebucket.setRate( max_write_rate_bytes_per_sec, max_write_rate_bytes_per_sec );
    
    int mss = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
    if( write_bytebucket.getBurstRate() < mss ) { //make sure the bucket will allow at least one full packet
      write_bytebucket.setRate( write_bytebucket.getRate(), mss );
    }
  }
  
  
  /**
   * Constructor for creating child pools.
   */
  private ConnectionPool( ConnectionPool parent, float write_percent_of_max ) {
    this.parent_pool = parent;
    this.write_percent_of_max = write_percent_of_max;
    write_bytebucket = new ByteBucket( new Float( NetworkManager.getSingleton().getMaxWriteRateBytesPerSec() * write_percent_of_max ).intValue() );
    
    int mss = NetworkManager.getSingleton().getTcpMssSize();
    if( write_bytebucket.getBurstRate() < mss ) { //make sure the bucket will allow at least one full packet
      write_bytebucket.setRate( write_bytebucket.getRate(), mss );
    }
  }
  

  /**
   * Add the given connection to the pool to be managed.
   * @param connection
   */
  public void addConnection( Connection connection ) {    
    synchronized( added_connections ) {
      added_connections.add( connection );
    }
  }
  
  
  /**
   * Remove the given connection from the pool's management.
   * @param connection
   */
  public void removeConnection( Connection connection ) {
    synchronized( removed_connections ) {
      removed_connections.add( connection );
    }
  }
  
  
  /**
   * Create a new child connection pool.
   * The newly created child pool is allocated 1/Nth of the parent pool's bandwidth
   * (where N is the new total number of children pools controlled by the parent),
   * and existing children pools individual bandwidth percentages are adjusted down
   * equally.
   * @return the newly created child pool
   */
  public ConnectionPool createChildConnectionPool() {
    synchronized( children_pools ) {
      int curr_num_children = children_pools.size();
      float given_write_percentage = write_percent_of_max / (curr_num_children + 1);
      
      if( curr_num_children > 0 ) {
        float indiv_write_percentage_change = given_write_percentage / curr_num_children;
        for( Iterator i = children_pools.iterator(); i.hasNext(); ) {
          ConnectionPool child = (ConnectionPool)i.next();
          float old_write_percentage = child.getWritePercentOfMax();
          child.setWritePercentOfMax( old_write_percentage - indiv_write_percentage_change, false );
        }
      }
      
      ConnectionPool new_child = new ConnectionPool( this, given_write_percentage );
      children_pools.add( new_child );
      return new_child;
    }
  }
  
  
  /**
   * Destroy/terminate this connection pool and all children pools,
   * releasing all attached Connections, and returning its allocated
   * bandwidth to the parent.
   */
  public void destroy() {
    destroy( true );
  }
  
  
  /**
   * Used by the parent pool to destroy children.
   * @param inform_parent tell the parent pool of the destroy
   */
  protected void destroy( boolean inform_parent ) {
    synchronized( children_pools ) {
      for( Iterator i = children_pools.iterator(); i.hasNext(); ) {
        ConnectionPool child = (ConnectionPool)i.next();
        child.destroy( false );
        i.remove();
      }
    }
    if( parent_pool != null ) {  //root pool parent is null, and we don't want to destroy the root
      connections.clear();
      added_connections.clear();
      removed_connections.clear();
      if( inform_parent )  parent_pool.informChildDestroyed( this );
    }
  }
  
  
  /**
   * Inform that the given child has been destroyed, so that it may be removed
   * from this parent's list and that its bandwidth may be equally re-allocated
   * amongst any remaining children.
   * @param destroyed_child
   */
  protected void informChildDestroyed( ConnectionPool destroyed_child ) {
    synchronized( children_pools ) {
      children_pools.remove( destroyed_child );
      int num_children = children_pools.size();
      if( num_children > 0 ) {
        float indiv_write_percentage_change = destroyed_child.getWritePercentOfMax() / num_children;
        for( Iterator i = children_pools.iterator(); i.hasNext(); ) {
          ConnectionPool child = (ConnectionPool)i.next();
          float old_write_percentage = child.getWritePercentOfMax();
          child.setWritePercentOfMax( old_write_percentage + indiv_write_percentage_change, false );
        }
      }
    }
  }
  
  
  /**
   * Get the pool's write rate percentage, i.e. the percentage of the 
   * global maximum upload rate that this pool is allowed to use.
   * NOTE: Pool bandwidth rates are calculated as percentages of the global
   * max rates, so the real byte rate of a pool = percentage * global max rate
   * @return write percentage
   */
  public float getWritePercentOfMax() {  return write_percent_of_max;  }
  

  /**
   * Set the pool's write rate percentage, i.e. the percentage of the 
   * global maximum upload rate that this pool is allowed to use.
   * Bandwidth percentage rates for fellow and children pools are
   * dynamically adjusted up/down to compensate for the given change.
   * NOTE: Pool bandwidth rates are calculated as percentages of the global
   * max rates, so the real byte rate of a pool = percentage * global max rate
   * @param new_percentage
   */
  public void setWritePercentOfMax( float new_percentage ) {
    setWritePercentOfMax( new_percentage, true );
  }
  
  
  /**
   * Used by the parent pool to set write percentages of children.
   * @param new_percentage
   * @param inform_parent tell the parent pool of the change
   */
  protected void setWritePercentOfMax( float new_percentage, boolean inform_parent ) {
    System.out.println("old=" + write_percent_of_max + ", new=" + new_percentage);
    float percent_change = new_percentage - write_percent_of_max;
    if( parent_pool == null ) {
      write_percent_of_max = 1.0F;  //make sure the root pool is always 100%
    }
    else {
      write_percent_of_max = new_percentage;
    }
    synchronized( children_pools ) {
      int num_children = children_pools.size();
      if( num_children > 0 ) {
        float indiv_percentage_change = percent_change / num_children;
        for( Iterator i = children_pools.iterator(); i.hasNext(); ) {
          ConnectionPool child = (ConnectionPool)i.next();
          float old_percentage = child.getWritePercentOfMax();
          child.setWritePercentOfMax( old_percentage + indiv_percentage_change, false );
        }
      }
    }
    updateBucketRates();
    if( parent_pool != null && inform_parent ) { //root pool does not have a parent
      parent_pool.informOfChildWritePercentageChange( this, percent_change );
    }
  }
  
  
  /**
   * Inform that the given child pool has had its write rate percentage changed by the given
   * amount, so that the parent may adjust the fellow child pool rates accordingly.
   * @param changed_child
   * @param percent_change
   */
  protected void informOfChildWritePercentageChange( ConnectionPool changed_child, float percent_change ) {
    synchronized( children_pools ) {
      int num_children = children_pools.size();
      if( num_children > 1 ) {  //if there are other children that need adjustment
        float indiv_percentage_change = percent_change / (num_children - 1);
        for( Iterator i = children_pools.iterator(); i.hasNext(); ) {
          ConnectionPool child = (ConnectionPool)i.next();
          if( !child.equals( changed_child ) ) { //we don't want to adjust the already-changed child
            float old_percentage = child.getWritePercentOfMax();
            child.setWritePercentOfMax( old_percentage - indiv_percentage_change, false );
          }
        }
      }
    }
  }
  
  
  /**
   * Update the read/write token bucket byte rates to reflect the current
   * global max and local percentage-of rates.
   */
  protected void updateBucketRates() {
    synchronized( children_pools ) {
      for( Iterator i = children_pools.iterator(); i.hasNext(); ) {
        ConnectionPool child = (ConnectionPool)i.next();
        child.updateBucketRates();
      }
    }
    int new_rate = new Float( NetworkManager.getSingleton().getMaxWriteRateBytesPerSec() * write_percent_of_max ).intValue();
    if( parent_pool == null ) { //root pool doesn't burst
      write_bytebucket.setRate( new_rate, new_rate );
    }
    else {
      write_bytebucket.setRate( new_rate );
    }
    
    int mss = NetworkManager.getSingleton().getTcpMssSize();
    if( write_bytebucket.getBurstRate() < mss ) { //make sure the bucket will allow at least one full packet
      write_bytebucket.setRate( write_bytebucket.getRate(), mss );
    }
  }
  
  
  /**
   * Write (up to) as many bytes as allowed by the pool's max rate,
   * i.e. do limited guaranteed-rate writes first, and then allow
   * connections to use any remaining unclaimed bandwidth (burst).
   */
  protected void doWrites( VirtualChannelSelector selector ) {
    selector.select( 50 );
    doLimitedRateWrites();  //do guaranteed-rate writes
    //optimization: if the configured max rate is unlimited, doLimitedRateWrites() will do all the writes possible at the moment
    boolean is_unlimited_rate = NetworkManager.getSingleton().getMaxWriteRateBytesPerSec() == NetworkManager.UNLIMITED_WRITE_RATE ? true : false;
    int remaining = write_bytebucket.getAvailableByteCount();
    if( remaining >= NetworkManager.getSingleton().getTcpMssSize() && !is_unlimited_rate ) {
      int used = doUncountedRateWrites( remaining );  //use any remaining unclaimed bandwidth
      //System.out.println("remaining=" +remaining+ ", used=" +used);
      if( used > 0 ) {
        write_bytebucket.setBytesUsed( used );
      }
    }
  }
  
  
  private int doLimitedRateWrites() {
    int total_bytes_used = 0;
    
    final int mss_size = NetworkManager.getSingleton().getTcpMssSize();
    
    synchronized( children_pools ) {
      int num_children = children_pools.size();
      int num_checked = 0;
      while( num_checked < num_children && write_bytebucket.getAvailableByteCount() >= mss_size ) {
        ConnectionPool child = (ConnectionPool)children_pools.removeFirst();
        int used = child.doLimitedRateWrites();
        //System.out.println("  used=" +used);
        if( used > 0 ) {
          write_bytebucket.setBytesUsed( used );
          total_bytes_used += used;
        }
        children_pools.addLast( child );
        num_checked++;
      }
    }
    
    int remaining = write_bytebucket.getAvailableByteCount();
    if( remaining >= mss_size ) {
      int used = doConnectionWrites( remaining );
      //System.out.println("written="+used);
      if( used > 0 ) {
        write_bytebucket.setBytesUsed( used );
        total_bytes_used += used;
      }
    }
    
    //if( parent_pool == null ) System.out.println("total_bytes_used="+total_bytes_used);
    return total_bytes_used;
  }
  

  
  private int doUncountedRateWrites( int max_bytes_allowed ) {
    int total_bytes_used = 0;
    int mss_size = NetworkManager.getSingleton().getTcpMssSize();
    
    synchronized( children_pools ) {
      int num_children = children_pools.size();
      int num_checked = 0;
      while( num_checked < num_children && max_bytes_allowed - total_bytes_used >= mss_size ) {
        ConnectionPool child = (ConnectionPool)children_pools.removeFirst();
        total_bytes_used += child.doUncountedRateWrites( max_bytes_allowed - total_bytes_used );
        children_pools.addLast( child );
        num_checked++;
      }
    }
    
    int remaining = max_bytes_allowed - total_bytes_used;
    if( remaining >= mss_size ) {
      total_bytes_used += doConnectionWrites( remaining );
    }

    //System.out.println("max_bytes_allowed="+max_bytes_allowed+", total_bytes_used="+total_bytes_used  );
    return total_bytes_used;
  }
  
  
  private int doConnectionWrites( int max_bytes_allowed ) {    
    //add new connections
    synchronized( added_connections ) {
      for( int i=0; i < added_connections.size(); i++ ) {
        Connection conn = (Connection)added_connections.get( i );
        connections.addLast( conn );
      }
      added_connections.clear();
    }
    
    //remove removed connections
    synchronized( removed_connections ) {
      for( int i=0; i < removed_connections.size(); i++ ) {
        Connection conn = (Connection)removed_connections.get( i );
        connections.remove( conn );
      }
      removed_connections.clear();
    }
    
    int total_bytes_used = 0;
    int mss_size = NetworkManager.getSingleton().getTcpMssSize();
    int num_connections = connections.size();
    int num_seen = 0;
    int num_empty = 0;
    
    while( max_bytes_allowed - total_bytes_used >= mss_size && num_empty < num_connections ) {
      if( num_seen == num_connections ) {
        num_empty = 0;
        num_seen = 0;
      }
      
      Connection conn = (Connection)connections.removeFirst();
      
      if( conn.isTransportReadyForWrite() ) {
        OutgoingMessageQueue omq = conn.getOutgoingMessageQueue();
        int size = omq.getTotalSize();
        boolean forced_flush = size > 0 && SystemTime.getCurrentTime() - conn.getLastNewWriteDataAddedTime() > FLUSH_WAIT_TIME ? true : false;
        if( size >= mss_size || forced_flush || omq.hasUrgentMessage() ) {
          int num_bytes_to_write;
          
          if( size < mss_size ) {
            num_bytes_to_write = size;
          }
          else {
            int remaining = max_bytes_allowed - total_bytes_used;
            int avail = size;
            if( avail > remaining )  avail = remaining;
            int num_full_packets = avail / mss_size;
            num_bytes_to_write = num_full_packets * mss_size;
          }
          
          int written = 0;
          try {
            written = omq.deliverToTransport( num_bytes_to_write );
            if( written < num_bytes_to_write ) {  //unable to deliver all data....add to selector for readiness notification
              conn.setTransportReadyForWrite( false );
              NetworkManager.getSingleton().getWriteSelector().register( conn.getTransport().getSocketChannel(), write_select_listener, conn );
            }
            else if( omq.getTotalSize() >= mss_size ) { //there's still data left to send
              num_empty--; //make sure it's not counted as empty
            }
          } 
          catch( Throwable t ) {
            conn.setTransportReadyForWrite( false );
            conn.notifyOfException( t );
          }
          total_bytes_used += written;
        }
      }
      
      connections.addLast( conn );
      num_seen++;
      num_empty++;
    }
    
    return total_bytes_used;
  }
  
  
}
