/*
 * Created on 10 juil. 2003
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
public class IntParameter {

  Text inputField;

  public IntParameter(Composite composite, final String name, int defaultValue) {
    inputField = new Text(composite, SWT.BORDER);
    int value = ConfigurationManager.getInstance().getIntParameter(name, defaultValue);
    inputField.setText("" + value);
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
          int value = Integer.parseInt(inputField.getText());
          ConfigurationManager.getInstance().setParameter(name, value);
        }
        catch (Exception e) {}
      }
    });
  }
   

  public void setLayoutData(Object layoutData) {
    inputField.setLayoutData(layoutData);
  }
}
