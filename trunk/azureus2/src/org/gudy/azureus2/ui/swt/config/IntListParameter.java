/*
 * Created on 10 juil. 2003
 *
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
public class IntListParameter implements IParameter {

  Combo list;

  public IntListParameter(
    Composite composite,
    final String name,
    int defaultValue,
    final String labels[],
    final int values[]) {
      if(labels.length != values.length)
        return;
      int value = COConfigurationManager.getIntParameter(name,defaultValue);
      int index = findIndex(value,values);
      list = new Combo(composite,SWT.SINGLE | SWT.READ_ONLY);
      for(int i = 0 ; i < labels.length  ;i++) {
        list.add(labels[i]);
      }
      
      list.select(index);
      
      list.addListener(SWT.Selection, new Listener() {
           public void handleEvent(Event e) {
			COConfigurationManager.setParameter(name, values[list.getSelectionIndex()]);
           }
         });
      
    }
    
  private int findIndex(int value,int values[]) {
    for(int i = 0 ; i < values.length ;i++) {
      if(values[i] == value)
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
}
