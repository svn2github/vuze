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
  
  private volatile ArrayList normal_priority_entities = new ArrayList();  //copied-on-write
  private volatile ArrayList high_priority_entities = new ArrayList();  //copied-on-write
  private final AEMonitor entities_mon = new AEMonitor( "WriteController:EM" );
  private int next_normal_position = 0;
  private int next_high_position = 0;
  
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
    ArrayList ref = normal_priority_entities;
    
    int size = ref.size();
    int num_checked = 0;

    while( num_checked < size ) {
      next_normal_position = next_normal_position >= size ? 0 : next_normal_position;  //make circular
      RateControlledWriteEntity entity = (RateControlledWriteEntity)ref.get( next_normal_position );
      next_normal_position++;
      num_checked++;
      if( entity.canWrite() ) {  //is ready
        return entity;
      }
    }

    return null;  //none found ready
  }
  
  
  private RateControlledWriteEntity getNextReadyHighPriorityEntity() {
    ArrayList ref = high_priority_entities;
    
    int size = ref.size();
    int num_checked = 0;

    while( num_checked < size ) {
      next_high_position = next_high_position >= size ? 0 : next_high_position;  //make circular
      RateControlledWriteEntity entity = (RateControlledWriteEntity)ref.get( next_high_position );
      next_high_position++;
      num_checked++;
      if( entity.canWrite() ) {  //is ready
        return entity;
      }
    }

    return null;  //none found ready
  }
  
  
  
  /**
   * Add the given entity to the controller for write processing.
   * @param entity to process writes for
   */
  protected void addWriteEntity( RateControlledWriteEntity entity ) {
    try {  entities_mon.enter();
      if( entity.getPriority() == RateControlledWriteEntity.PRIORITY_HIGH ) {
        //copy-on-write
        ArrayList high_new = new ArrayList( high_priority_entities.size() + 1 );
        high_new.addAll( high_priority_entities );
        high_new.add( entity );
        high_priority_entities = high_new;
      }
      else {
        //copy-on-write
        ArrayList norm_new = new ArrayList( normal_priority_entities.size() + 1 );
        norm_new.addAll( normal_priority_entities );
        norm_new.add( entity );
        normal_priority_entities = norm_new;
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
      if( entity.getPriority() == RateControlledWriteEntity.PRIORITY_HIGH ) {
        //copy-on-write
        ArrayList high_new = new ArrayList( high_priority_entities );
        high_new.remove( entity );
        high_priority_entities = high_new;
      }
      else {
        //copy-on-write
        ArrayList norm_new = new ArrayList( normal_priority_entities );
        norm_new.remove( entity );
        normal_priority_entities = norm_new;
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
