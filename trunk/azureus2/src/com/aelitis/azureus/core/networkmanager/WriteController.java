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
  private final LinkedHashMap normal_priority_entities = new LinkedHashMap();
  private final LinkedHashMap high_priority_entities = new LinkedHashMap();
  private final AEMonitor entities_mon = new AEMonitor( "WriteController:EM" );

  private static final int SELECT_TIME = 50;
  private static final int PROCESSOR_SLEEP_TIME = 20;
  
  
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
    write_processor_thread.setPriority( Thread.MAX_PRIORITY - 1 );
    write_processor_thread.start();
  }
  
  
  
  private void writeSelectorLoop() {
    while( true ) {
      write_selector.select( SELECT_TIME );      
    }
  }
  
  
  private void writeProcessorLoop() {
    boolean check_high_first = true;
    
    while( true ) {      
      if( check_high_first ) {
        check_high_first = false;
        if( !doHighPriorityWrite() ) {
          if( !doNormalPriorityWrite() ) {
            try {  Thread.sleep( PROCESSOR_SLEEP_TIME );  }catch(Exception e) { Debug.printStackTrace(e); }
          }
        }
      }
      else {
        check_high_first = true;
        if( !doNormalPriorityWrite() ) {
          if( !doHighPriorityWrite() ) {
            try {  Thread.sleep( PROCESSOR_SLEEP_TIME );  }catch(Exception e) { Debug.printStackTrace(e); }
          }
        }
      }
    }
  }
  
  
  private boolean doNormalPriorityWrite() {
    RateControlledWriteEntity ready_entity = getNextReadyNormalPriorityEntity();
    if( ready_entity != null && ready_entity.doWrite() ) {
      return true;
    }
    return false;
  }
  
  private boolean doHighPriorityWrite() {
    RateControlledWriteEntity ready_entity = getNextReadyHighPriorityEntity();
    if( ready_entity != null && ready_entity.doWrite() ) {
      return true;
    }
    return false;
  }
  
  
  private RateControlledWriteEntity getNextReadyNormalPriorityEntity() {
    RateControlledWriteEntity ready_entity = null;
    
    try {  entities_mon.enter();
      //find the next ready entity
      for( Iterator i = normal_priority_entities.keySet().iterator(); i.hasNext(); ) {
        ready_entity = (RateControlledWriteEntity)i.next();
        if( ready_entity.canWrite() ) {  //is ready
          i.remove();  //remove from beginning and...
          break;
        }
        ready_entity = null;  //not ready, so leave at beginning for checking next round
      }
      
      if( ready_entity != null ) {
        normal_priority_entities.put( ready_entity, null );  //...put back at the end
      }
    }
    finally {  entities_mon.exit();  }
 
    return ready_entity;
  }
  
  
  private RateControlledWriteEntity getNextReadyHighPriorityEntity() {
    RateControlledWriteEntity ready_entity = null;
    
    try {  entities_mon.enter();
      //find the next ready entity
      for( Iterator i = high_priority_entities.keySet().iterator(); i.hasNext(); ) {
        ready_entity = (RateControlledWriteEntity)i.next();
        if( ready_entity.canWrite() ) {  //is ready
          i.remove();  //remove from beginning and...
          break;
        }
        ready_entity = null;  //not ready, so leave at beginning for checking next round
      }

      if( ready_entity != null ) {
        high_priority_entities.put( ready_entity, null );  //...put back at the end
      }
    }
    finally {  entities_mon.exit();  }
 
    return ready_entity;
  }
  
  
  
  /**
   * Add the given entity to the controller for write processing.
   * @param entity to process writes for
   */
  protected void addWriteEntity( RateControlledWriteEntity entity ) {
    try {  entities_mon.enter();
      
      if( entity.getPriority() == RateControlledWriteEntity.PRIORITY_HIGH ) {
        high_priority_entities.put( entity, entity );
      }
      else {
        normal_priority_entities.put( entity, entity );
      }
    }
    finally {  entities_mon.exit();  }
  }
  
  
  /**
   * Remove the given entity from the controller.
   * @param entity to remove from write processing
   */
  protected void removeWriteEntity( RateControlledWriteEntity entity ) {
    try {  entities_mon.enter();
      
      Object removed = normal_priority_entities.remove( entity );
      if( removed == null ) {
        high_priority_entities.remove( entity );
      }
    }
    finally {  entities_mon.exit();  }
  }
  
  
  /**
   * Get the virtual selector for socket channel write readiness.
   * @return selector
   */
  protected VirtualChannelSelector getWriteSelector() {  return write_selector;  }
  
}
