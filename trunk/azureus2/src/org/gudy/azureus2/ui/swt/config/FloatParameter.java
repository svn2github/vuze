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

public class FloatParameter {

  Text inputField;
  float fMinValue = 0;
  float fMaxValue = -1;
  float fDefaultValue;
  int iDigitsAfterDecimal = 1;
  String sParamName;
  boolean allowZero = false;

  public FloatParameter(Composite composite, final String name) {
    fDefaultValue = COConfigurationManager.getFloatParameter(name);
    initialize(composite,name);
  }

  public FloatParameter(Composite composite, final String name, 
                        float minValue, float maxValue, boolean allowZero,
                        int digitsAfterDecimal) {
    fDefaultValue = COConfigurationManager.getFloatParameter(name);
    initialize(composite,name);
    fMinValue = minValue;
    fMaxValue = maxValue;
    this.allowZero = allowZero;
    iDigitsAfterDecimal = digitsAfterDecimal;
  }
  
    
  public void initialize(Composite composite, final String name) {
    sParamName = name;

    inputField = new Text(composite, SWT.BORDER);
    float value = COConfigurationManager.getFloatParameter(name);
    inputField.setText(String.valueOf(value));
    inputField.addListener(SWT.Verify, new Listener() {
      public void handleEvent(Event e) {
        String text = e.text;
        if (!text.matches("^[0-9]*\\.?[0-9]{0," + iDigitsAfterDecimal + "}$")) {
          e.doit = false;
          return;
        }
      }
    });

    inputField.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        try {
          float val = Float.parseFloat(inputField.getText());
          if (val < fMinValue) {
            if (!(allowZero && val == 0)) {
            	val = fMinValue;
            }
          }
          if (val > fMaxValue) {
            if (fMaxValue > -1) {
              val = fMaxValue;
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
          float val = Float.parseFloat(inputField.getText());
          if (val < fMinValue) {
            if (!(allowZero && val == 0)) {
              inputField.setText(String.valueOf(fMinValue));
              COConfigurationManager.setParameter(name, fMinValue);
            }
          }
          if (val > fMaxValue) {
            if (fMaxValue > -1) {
            	inputField.setText(String.valueOf(fMaxValue));
            	COConfigurationManager.setParameter(name, fMaxValue);
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
