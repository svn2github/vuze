/*
 * Created on 16-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.config.generic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class 
GenericIntParameter 
{
  GenericParameterAdapter	adapter;
	
  Text inputField;
  int iMinValue = 0;
  int iMaxValue = -1;
  int iDefaultValue;
  String sParamName;
  boolean allowZero = false;
  boolean generateIntermediateEvents = true;

  boolean value_is_changing_internally;
    
  public GenericIntParameter(GenericParameterAdapter adapter, Composite composite, final String name) {
    iDefaultValue = adapter.getIntValue(name);
    initialize(adapter,composite,name);
  }

  public GenericIntParameter(GenericParameterAdapter adapter, Composite composite, final String name, boolean generateIntermediateEvents ){
	    iDefaultValue = adapter.getIntValue(name);
	    this.generateIntermediateEvents = generateIntermediateEvents;
	    initialize(adapter,composite,name);
  }
  public GenericIntParameter(GenericParameterAdapter adapter, Composite composite, final String name, int defaultValue) {
    iDefaultValue = defaultValue;
    initialize(adapter,composite, name);
  }
  
  
  public GenericIntParameter(GenericParameterAdapter adapter, Composite composite, final String name, int defaultValue,boolean generateIntermediateEvents) {
    iDefaultValue = defaultValue;
    this.generateIntermediateEvents = generateIntermediateEvents;
    initialize(adapter,composite, name);
  }
  
  
  public GenericIntParameter(
		  GenericParameterAdapter adapter, 
		  Composite composite,
		  String name,
          int minValue,
          int maxValue,
          boolean allowZero,
          boolean generateIntermediateEvents ) {
    iDefaultValue = adapter.getIntValue(name);
    iMinValue = minValue;
    iMaxValue = maxValue;
    this.allowZero = allowZero;
    this.generateIntermediateEvents = generateIntermediateEvents;
    initialize(adapter,composite,name);
  }
  
    
  public void initialize(GenericParameterAdapter _adapter,Composite composite, String name) {
	adapter = _adapter;
    sParamName = name;

    inputField = new Text(composite, SWT.BORDER);
    int value = adapter.getIntValue(name, iDefaultValue);
    inputField.setText(String.valueOf(value));
    inputField.addListener(SWT.Verify, new Listener() {
      public void handleEvent(Event e) {
        String text = e.text;
        char[] chars = new char[text.length()];
        text.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
          if (!('0' <= chars[i] && chars[i] <= '9')) {
            e.doit = false;
            return;
          }
        }
      }
    });

    if(generateIntermediateEvents) {
      inputField.addListener(SWT.Modify, new Listener() {
        public void handleEvent(Event event) {
        	checkValue();
        }
      });
    }

    
    inputField.addListener(SWT.FocusOut, new Listener() {
      public void handleEvent(Event event) {
      	checkValue();
      }
    });
  }
  
  public void
  setAllowZero(
  	boolean		allow )
  {
  	allowZero	= allow;
  }
  
  public void
  setMinimumValue(
  	int		value )
  {
  	iMinValue	= value;
  }
  public void
  setMaximumValue(
  	int		value )
  {
  	iMaxValue	= value;
  }
  
  protected void
  checkValue()
 {
    try{
    	int	old_val = adapter.getIntValue( sParamName, -1 );
    	
        int new_val = Integer.parseInt(inputField.getText());
        
        int	original_new_val	= new_val;
               
        if (new_val < iMinValue) {
          if (!(allowZero && new_val == 0)) {
          	new_val = iMinValue;
          }
        }
        
        if (new_val > iMaxValue) {
          if (iMaxValue > -1) {
            new_val = iMaxValue;
          }
        }
        
        if ( new_val == old_val ){
        	
        	if ( new_val != original_new_val ){
        		
        		inputField.setText(String.valueOf(new_val));
        	}
        	
        }else{
        	adapter.setIntValue(sParamName, new_val);
        	
        	if ( new_val != original_new_val ){
        		
        		inputField.setText(String.valueOf(new_val));
        	}
        	
        	adapter.informChanged( value_is_changing_internally );
        }
      }
      catch (Exception e) {
        inputField.setText( String.valueOf( iMinValue ) );
        adapter.setIntValue( sParamName, iMinValue );
      }
  }

  public void
  setValue(
  	int		value )
  {
	String	str_val = String.valueOf( value );
	  
  	if ( getValue() != value || !str_val.equals( inputField.getText())){
  		
	  	try{
	  		value_is_changing_internally	= true;
	  		
	  		inputField.setText( str_val );
	  		
	  	}finally{
	  		
	 		value_is_changing_internally	= false;
	  	}
  	}
  }
  
  public int
  getValue()
  {
  	return(adapter.getIntValue(sParamName, iDefaultValue));
  }
  
  public void setLayoutData(Object layoutData) {
    inputField.setLayoutData(layoutData);
  }
  
  public Control
  getControl()
  {
  	return( inputField );
  }
}