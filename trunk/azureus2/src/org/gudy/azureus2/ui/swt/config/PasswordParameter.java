/*
 * Created on 8 september 2003
 *
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.SHA1Hasher;

/**
 * @author Olivier
 * 
 */
public class PasswordParameter {

  String name;
  Text inputField;

  public PasswordParameter(Composite composite,final String name) {
    this.name = name;
    inputField = new Text(composite, SWT.BORDER);
    inputField.setEchoChar('*');
    byte[] value = COConfigurationManager.getByteParameter(name, "".getBytes());
    if(value.length > 0)
      inputField.setText("***");
    inputField.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        try{
          SHA1Hasher hasher = new SHA1Hasher();
          byte[] password = inputField.getText().getBytes();
          byte[] encoded;
          if(password.length > 0)
            encoded = hasher.calculateHash(password);
          else
            encoded = password;
          COConfigurationManager.setParameter(name, encoded);
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public void setLayoutData(Object layoutData) {
    inputField.setLayoutData(layoutData);
  }
  
  public void setValue(String value) {
    inputField.setText(value);
    COConfigurationManager.setParameter(name, value);         
  }
  
  public String getValue() {
    return inputField.getText();
  }

}
