/*
 * Created on 9 juil. 2003
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
public class StringParameter implements IParameter{

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
