/*
 * Created on Oct 16, 2004
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.util.ArrayList;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;



/**
 * Processes reads of read-entities and handles the read selector.
 */
public class ReadController {
  private final VirtualChannelSelector read_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_READ, true );

  private volatile ArrayList normal_priority_entities = new ArrayList();  //copied-on-write
  private volatile ArrayList high_priority_entities = new ArrayList();  //copied-on-write
  private final AEMonitor entities_mon = new AEMonitor( "ReadController:EM" );
  private int next_normal_position = 0;
  private int next_high_position = 0;
  
  private static final int IDLE_SLEEP_TIME = 25;
  private static final int SELECT_LOOP_TIME = 25;
  
  
  public ReadController() {
    //start read selector processing
    Thread read_selector_thread = new AEThread( "ReadController:ReadSelector" ) {
      public void runSupport() {
        readSelectorLoop();
      }
    };
    read_selector_thread.setDaemon( true );
    read_selector_thread.setPriority( Thread.MAX_PRIORITY - 2 );
    read_selector_thread.start();
    
    
    //start read handler processing
    Thread read_processor_thread = new AEThread( "ReadController:ReadProcessor" ) {
      public void runSupport() {
        readProcessorLoop();
      }
    };
    read_processor_thread.setDaemon( true );
    read_processor_thread.setPriority( Thread.MAX_PRIORITY - 1 );
    read_processor_thread.start();
  }
  

  
  private void readSelectorLoop() {
    while( true ) {
      try {
        read_selector.select( SELECT_LOOP_TIME );
      }
      catch( Throwable t ) {
        Debug.out( "readSelectorLoop() EXCEPTION: ", t );
      }      
    }
  }
  
  
  
  
  private void readProcessorLoop() {
    boolean check_high_first = true;
    
    while( true ) {
      try {
        if( check_high_first ) {
          check_high_first = false;
          if( !doHighPriorityRead() ) {
            if( !doNormalPriorityRead() ) {
              try {  Thread.sleep( IDLE_SLEEP_TIME );  }catch(Exception e) { Debug.printStackTrace(e); }
            }
          }
        }
        else {
          check_high_first = true;
          if( !doNormalPriorityRead() ) {
            if( !doHighPriorityRead() ) {
              try {  Thread.sleep( IDLE_SLEEP_TIME );  }catch(Exception e) { Debug.printStackTrace(e); }
            }
          }
        }
      }
      catch( Throwable t ) {
        Debug.out( "readProcessorLoop() EXCEPTION: ", t );
      }
    }
  }
  
  
  private boolean doNormalPriorityRead() {
    RateControlledEntity ready_entity = getNextReadyNormalPriorityEntity();
    if( ready_entity != null && ready_entity.doProcessing() ) {
      return true;
    }
    return false;
  }
  
  private boolean doHighPriorityRead() {
    RateControlledEntity ready_entity = getNextReadyHighPriorityEntity();
    if( ready_entity != null && ready_entity.doProcessing() ) {
      return true;
    }
    return false;
  }
  
  
  private RateControlledEntity getNextReadyNormalPriorityEntity() {
    ArrayList ref = normal_priority_entities;
    
    int size = ref.size();
    int num_checked = 0;

    while( num_checked < size ) {
      next_normal_position = next_normal_position >= size ? 0 : next_normal_position;  //make circular
      RateControlledEntity entity = (RateControlledEntity)ref.get( next_normal_position );
      next_normal_position++;
      num_checked++;
      if( entity.canProcess() ) {  //is ready
        return entity;
      }
    }

    return null;  //none found ready
  }
  
  
  private RateControlledEntity getNextReadyHighPriorityEntity() {
    ArrayList ref = high_priority_entities;
    
    int size = ref.size();
    int num_checked = 0;

    while( num_checked < size ) {
      next_high_position = next_high_position >= size ? 0 : next_high_position;  //make circular
      RateControlledEntity entity = (RateControlledEntity)ref.get( next_high_position );
      next_high_position++;
      num_checked++;
      if( entity.canProcess() ) {  //is ready
        return entity;
      }
    }

    return null;  //none found ready
  }
  
  
  
  /**
   * Add the given entity to the controller for read processing.
   * @param entity to process reads for
   */
  public void addReadEntity( RateControlledEntity entity ) {
    try {  entities_mon.enter();
      if( entity.getPriority() == RateControlledEntity.PRIORITY_HIGH ) {
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
   * @param entity to remove from read processing
   */
  public void removeReadEntity( RateControlledEntity entity ) {
    try {  entities_mon.enter();
      if( entity.getPriority() == RateControlledEntity.PRIORITY_HIGH ) {
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
   * Get the virtual selector for socket channel read readiness.
   * @return selector
   */
  public VirtualChannelSelector getReadSelector() {  return read_selector;  }
}
