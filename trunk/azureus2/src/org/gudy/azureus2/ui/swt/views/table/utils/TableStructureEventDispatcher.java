/*
 * File    : TableStructureEventDispatcher.java
 * Created : 27 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.table.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.gudy.azureus2.ui.swt.views.table.ITableStructureModificationListener;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;

import org.gudy.azureus2.core3.util.AEMonitor;

/**
 * @author Olivier
 *
 */
public class TableStructureEventDispatcher implements ITableStructureModificationListener {

  private static Map instances = new HashMap();
  
  private static AEMonitor	class_mon	= new AEMonitor( "TableStructureEventDispatcher:class" );
  
  private List 		listeners;
  private AEMonitor	listeners_mon	= new AEMonitor( "TableStructureEventDispatcher:L" );

  /**
   * 
   */
  private TableStructureEventDispatcher() {
    listeners = new ArrayList();
  }
  
  public static TableStructureEventDispatcher getInstance(String sTableID) {
  	try{
  		class_mon.enter();
  	
  		TableStructureEventDispatcher instance = (TableStructureEventDispatcher)instances.get(sTableID);
  		if (instance == null) {
  			instance = new TableStructureEventDispatcher();
  			instances.put(sTableID, instance);
  		}
  		return instance;
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public void addListener(ITableStructureModificationListener listener) {
    try{
    	listeners_mon.enter();
    
      this.listeners.add(listener);
      
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void removeListener(ITableStructureModificationListener listener) {
    try{
    	listeners_mon.enter();
    	    
    	this.listeners.remove(listener);
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void tableStructureChanged() {
   try{
   		listeners_mon.enter();
   
     Iterator iter = listeners.iterator();
     while(iter.hasNext()) {
       ITableStructureModificationListener listener = (ITableStructureModificationListener) iter.next();
       listener.tableStructureChanged();
     }
   }finally{
   	
   		listeners_mon.exit();
   }
  }
  
  public void columnSizeChanged(TableColumnCore tableColumn) {
   try{
   		listeners_mon.enter();
   
     Iterator iter = listeners.iterator();
     while(iter.hasNext()) {
       ITableStructureModificationListener listener = (ITableStructureModificationListener) iter.next();
       listener.columnSizeChanged(tableColumn);
     }
   }finally{
   	
   	listeners_mon.exit();
   }
  }

  public void columnInvalidate(TableColumnCore tableColumn) {
    try{
    	listeners_mon.enter();
    
      Iterator iter = listeners.iterator();
      while (iter.hasNext()) {
        ITableStructureModificationListener listener = 
                              (ITableStructureModificationListener)iter.next();
        listener.columnInvalidate(tableColumn);
      }
    }finally{
    	
    	listeners_mon.exit();
    }
  }
}
