/*
 * Created on Sep 27, 2004
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

import org.gudy.azureus2.core3.util.*;

/**
 * Processes writes of write-entities and handles the write selector.
 */
public class WriteController {
  private final VirtualChannelSelector write_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_WRITE );
  private final LinkedList write_entities = new LinkedList();
  private final AEMonitor write_entities_mon = new AEMonitor( "WriteController:WE" );
  
  
  /**
   * Create a new write controller.
   */
  protected WriteController() {
    //start write selector processing
    Thread write_selector_thread = new AEThread( "WriteController:WriteSelector" ) {
      public void runSupport() {
        writeSelectorLoop();
      }
    };
    write_selector_thread.setDaemon( true );
    write_selector_thread.start();
    
    
    //start write handler processing
    Thread write_processor_thread = new AEThread( "WriteController:WriteProcessor" ) {
      public void runSupport() {
        writeProcessorLoop();
      }
    };
    write_processor_thread.setDaemon( true );
    write_processor_thread.start();
  }
  
  
  
  private void writeSelectorLoop() {
    while( true ) {
      write_selector.select( 50 );      
    }
  }
  
  
  private void writeProcessorLoop() {
    RateControlledWriteEntity current_entity = null;
    int total_entities = 0;
    int num_failed = 0;
    long sleep_time = 0;
    
    while( true ) {
      try {
        write_entities_mon.enter();
      
        total_entities = write_entities.size();
        
        if( total_entities > 0 ) {
          current_entity = (RateControlledWriteEntity)write_entities.removeFirst();
          write_entities.addLast( current_entity );
        }
      }
      finally {
        write_entities_mon.exit();
      }
      
      if( total_entities > 0 ) {
        boolean success = current_entity.doWrite();
        
        if( success ) {
          num_failed = 0;  //reset cumulative failure count
        }
        else {  //nothing written
          num_failed++;  //increment cumulative failure count
          if( num_failed >= total_entities ) {  //none of the entities wrote anything
            num_failed = 0;  //reset so we dont sleep again before checking all entries
            sleep_time = 20;  //sleep a bit
          }
        }
      }
      else {  //no entities registered....just sleep
        num_failed = 0;
        sleep_time = 50;
      }
      
      if( sleep_time > 0 ) {
        try {
          Thread.sleep( sleep_time );
        }
        catch(Exception e) { Debug.printStackTrace(e); }
        sleep_time = 0;  //reset sleep time
      }
    }
  }
  
  
  
  /**
   * Add the given entity to the controller for write processing.
   * @param entity to process writes for
   */
  protected void addWriteEntity( RateControlledWriteEntity entity ) {
    try {
      write_entities_mon.enter();
      
      write_entities.addLast( entity );
    }
    finally {
      write_entities_mon.exit();
    }
  }
  
  
  /**
   * Remove the given entity from the controller.
   * @param entity to remove from write processing
   */
  protected void removeWriteEntity( RateControlledWriteEntity entity ) {
    try {
      write_entities_mon.enter();
      
      write_entities.remove( entity );
    }
    finally {
      write_entities_mon.exit();
    }
  }
  
  
  /**
   * Get the virtual selector for socket channel write readyness.
   * @return selector
   */
  protected VirtualChannelSelector getWriteSelector() {  return write_selector;  }
  
}
