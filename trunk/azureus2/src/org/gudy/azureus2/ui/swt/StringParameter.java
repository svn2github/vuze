/*
 * Created on 9 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core.ConfigurationManager;

/**
 * @author Olivier
 * 
 */
public class StringParameter {

  String name;
  Text inputField;

  public StringParameter(Composite composite,final String name, String defaultValue) {
    this.name = name;
    inputField = new Text(composite, SWT.BORDER);
    String value = ConfigurationManager.getInstance().getStringParameter(name, defaultValue);
    inputField.setText(value);
    inputField.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        ConfigurationManager.getInstance().setParameter(name, inputField.getText());
      }
    });
  }

  public void setLayoutData(Object layoutData) {
    inputField.setLayoutData(layoutData);
  }
  
  public void setValue(String value) {
    inputField.setText(value);
    ConfigurationManager.getInstance().setParameter(name, value);         
  }
  
  public String getValue() {
    return inputField.getText();
  }

}
