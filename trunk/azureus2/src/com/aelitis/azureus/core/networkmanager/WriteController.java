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
  private final LinkedHashMap entities = new LinkedHashMap();
  private final AEMonitor entities_mon = new AEMonitor( "WriteController:EM" );
  
  
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
    while( true ) {
      RateControlledWriteEntity ready_entity = null;
      
      //find the next ready entity
      try {  entities_mon.enter();
        for( Iterator i = entities.keySet().iterator(); i.hasNext(); ) {
          ready_entity = (RateControlledWriteEntity)i.next();
          if( ready_entity.canWrite() ) {  //is ready
            i.remove();  //remove from beginning and...
            break;
          }
          ready_entity = null;  //not ready, so leave at beginning for checking next round
        }
        
        if( ready_entity != null ) {
          entities.put( ready_entity, null );  //...put back at the end
        }
      }
      finally {  entities_mon.exit();  }
      
      
      if( ready_entity != null ) {
        ready_entity.doWrite();  //do the write op
      }
      else { //none ready, so sleep a bit
        try {  Thread.sleep( 10 );   }catch(Exception e) { Debug.printStackTrace(e); }
      }
    }
  }
  
  
  /**
   * Add the given entity to the controller for write processing.
   * @param entity to process writes for
   */
  protected void addWriteEntity( RateControlledWriteEntity entity ) {
    try {  entities_mon.enter();
    
      entities.put( entity, null );
    }
    finally {  entities_mon.exit();  }
  }
  
  
  /**
   * Remove the given entity from the controller.
   * @param entity to remove from write processing
   */
  protected void removeWriteEntity( RateControlledWriteEntity entity ) {
    try {  entities_mon.enter();
    
      entities.remove( entity );
    }
    finally {  entities_mon.exit();  }
  }
  
  
  /**
   * Get the virtual selector for socket channel write readyness.
   * @return selector
   */
  protected VirtualChannelSelector getWriteSelector() {  return write_selector;  }
  
}
