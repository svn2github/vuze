/*
 * Created on 9 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core.ConfigurationManager;

/**
 * @author Olivier
 * 
 */
public class BooleanParameter {

  String name;
  Button checkBox;
 
  public BooleanParameter(Composite composite, final String name, boolean defaultValue) {
    boolean value = ConfigurationManager.getInstance().getBooleanParameter(name,defaultValue);
    checkBox = new Button(composite,SWT.CHECK);
    checkBox.setSelection(value);
    checkBox.addListener(SWT.Selection,new Listener() {
    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    public void handleEvent(Event event) {
      ConfigurationManager.getInstance().setParameter(name,checkBox.getSelection());
    }
  });
  }

  public void setLayoutData(Object layoutData) {
    checkBox.setLayoutData(layoutData);
  }
}
