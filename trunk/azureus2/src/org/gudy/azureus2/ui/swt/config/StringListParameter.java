/*
 * File    : StringListParameter.java
 * Created : 18-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.config.*;

/**
 * @author Olivier
 * 
 */
public class StringListParameter extends Parameter {

  Combo list;
  final String name;
  final String default_value;

  public StringListParameter( Composite composite, String _name, String defaultValue, final String labels[], final String values[]) {
    this.name = _name;
    this.default_value = defaultValue;
    
    if(labels.length != values.length) {
      return;
    }
    
    String value = COConfigurationManager.getStringParameter(name,defaultValue);
    int index = findIndex(value,values);
    list = new Combo(composite,SWT.SINGLE | SWT.READ_ONLY);
    
    for(int i = 0 ; i < labels.length  ;i++) {
      list.add(labels[i]);
    }
      
    list.select(index);
      
    list.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        COConfigurationManager.setParameter(name, values[list.getSelectionIndex()]);
        
        if( change_listeners != null ) {
          for (int i=0;i<change_listeners.size();i++){
            ((ParameterChangeListener)change_listeners.get(i)).parameterChanged(StringListParameter.this,false);
          }
        }
      }
    }); 
  }
    
  private int findIndex(String value,String values[]) {
    for(int i = 0 ; i < values.length ;i++) {
      if(values[i].equals( value))
        return i;
    }
    return 0;
  }
  
  
  public void setLayoutData(Object layoutData) {
    list.setLayoutData(layoutData);
   }
   
  public Control getControl() {
    return list;
  }
  
  public String getValue() {
    return COConfigurationManager.getStringParameter( name, default_value );
  }
  
}