/*
 * Created on 9 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.*;

/**
 * @author Olivier
 * 
 */
public class StringParameter extends Parameter{

  String name;
  Text inputField;

  public StringParameter(Composite composite,final String name) {
    this(composite, name, COConfigurationManager.getStringParameter(name));
  }

  public StringParameter(Composite composite,final String name, String defaultValue) {
    this.name = name;
    inputField = new Text(composite, SWT.BORDER);
    String value = COConfigurationManager.getStringParameter(name, defaultValue);
    inputField.setText(value);
    inputField.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        COConfigurationManager.setParameter(name, inputField.getText());
        
        if( change_listeners != null ) {
          for (int i=0;i<change_listeners.size();i++){
            ((ParameterChangeListener)change_listeners.get(i)).parameterChanged(StringParameter.this,false);
          }
        }
      }
    });
  }

  public void setLayoutData(Object layoutData) {
    inputField.setLayoutData(layoutData);
  }
  
  public void setValue(String value) {    
    if(inputField == null || inputField.isDisposed())
      return;
    inputField.setText(value);        
    COConfigurationManager.setParameter(name, value);
  }
  
  public String getValue() {
    return inputField.getText();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IParameter#getControl()
   */
  public Control getControl() {
    return inputField;
  }

}
