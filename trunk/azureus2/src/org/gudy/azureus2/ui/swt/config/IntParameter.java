/*
 * Created on 10 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.*;

/**
 * @author Olivier
 * 
 */
public class IntParameter {

  Text inputField;
  int iMinValue = 0;
  int iMaxValue = -1;
  int iDefaultValue;
  String sParamName;
  boolean allowZero = false;

  public IntParameter(Composite composite, final String name) {
    iDefaultValue = COConfigurationManager.getIntParameter(name);
    initialize(composite,name);
  }

  public IntParameter(Composite composite, final String name, int defaultValue) {
    iDefaultValue = defaultValue;
    initialize(composite, name);
  }
  
  
  public IntParameter(Composite composite, final String name, int minValue, int maxValue, boolean allowZero) {
    iDefaultValue = COConfigurationManager.getIntParameter(name);
    initialize(composite,name);
    iMinValue = minValue;
    iMaxValue = maxValue;
    this.allowZero = allowZero;
  }
  
    
  public void initialize(Composite composite, final String name) {
    sParamName = name;

    inputField = new Text(composite, SWT.BORDER);
    int value = COConfigurationManager.getIntParameter(name, iDefaultValue);
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

    inputField.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        try {
          int val = Integer.parseInt(inputField.getText());
          if (val < iMinValue) {
            if (!(allowZero && val == 0)) {
            	val = iMinValue;
            }
          }
          if (val > iMaxValue) {
            if (iMaxValue > -1) {
              val = iMaxValue;
            }
          }
          COConfigurationManager.setParameter(name, val);
        }
        catch (Exception e) {}
      }
    });

    inputField.addListener(SWT.FocusOut, new Listener() {
      public void handleEvent(Event event) {
        try {
          int val = Integer.parseInt(inputField.getText());
          if (val < iMinValue) {
            if (!(allowZero && val == 0)) {
              inputField.setText(String.valueOf(iMinValue));
              COConfigurationManager.setParameter(name, iMinValue);
            }
          }
          if (val > iMaxValue) {
            if (iMaxValue > -1) {
            	inputField.setText(String.valueOf(iMaxValue));
            	COConfigurationManager.setParameter(name, iMaxValue);
            }
          }
        }
        catch (Exception e) {}
      }
    });
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
