/*
 * Created on 2004/02/15
 *
 */
package org.gudy.azureus2.ui.swt.config;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.config.*;

/**
 * @author TuxPaper
 *
 */
public class RadioParameter extends Parameter{

  Button radioButton;

  List  performers  = new ArrayList();

  public RadioParameter(Composite composite, String sConfigName, int iButtonValue) {
    this(composite, sConfigName, iButtonValue, null);
  }

  public RadioParameter(Composite composite, final String sConfigName, final int iButtonValue,
                        IAdditionalActionPerformer actionPerformer) {
    if ( actionPerformer != null ){
      performers.add( actionPerformer );
    }
    int iDefaultValue = COConfigurationManager.getIntParameter(sConfigName);

    radioButton = new Button(composite, SWT.RADIO);
    radioButton.setSelection(iDefaultValue == iButtonValue);
    radioButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        boolean selected = radioButton.getSelection();
        if (selected)
          COConfigurationManager.setParameter(sConfigName, iButtonValue);

        if (performers.size() > 0 ) {
          for (int i = 0;i < performers.size(); i++) {
            IAdditionalActionPerformer  performer = (IAdditionalActionPerformer)performers.get(i);

            performer.setSelected(selected);
            performer.performAction();
          }
        }
      }
    });
  }

  public void setLayoutData(Object layoutData) {
    radioButton.setLayoutData(layoutData);
  }

  public void setAdditionalActionPerformer(IAdditionalActionPerformer actionPerformer) {
    performers.add(actionPerformer);
    boolean selected  = radioButton.getSelection();
    actionPerformer.setSelected(selected);
    actionPerformer.performAction();
  }
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IParameter#getControl()
   */
  public Control getControl() {
    return radioButton;
  }

  public boolean
  isSelected()
  {
    return( radioButton.getSelection());
  }
}
