/*
 * Created on 9 juil. 2003
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
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 * 
 */
public class BooleanParameter extends Parameter{

  String name;
  Button checkBox;
  
  List	performers	= new ArrayList();
  
  public BooleanParameter(Composite composite, final String name) {
    this(composite,name,COConfigurationManager.getBooleanParameter(name),null,null);
  }

  public BooleanParameter(Composite composite, final String name, String textKey) {
    this(composite, name, COConfigurationManager.getBooleanParameter(name),
         textKey, null);
  }

  public BooleanParameter(Composite composite, final String name, boolean defaultValue, String textKey) {
    this(composite,name,defaultValue,textKey,null);
  }

  public BooleanParameter(Composite composite, final String name, boolean defaultValue) {
    this(composite,name,defaultValue,null,null);
  }
  
  public BooleanParameter(Composite composite, final String name, 
                          boolean defaultValue,
                          String textKey,
                          IAdditionalActionPerformer actionPerformer) {
    if ( actionPerformer != null ){
    	performers.add( actionPerformer );
    }
    boolean value = COConfigurationManager.getBooleanParameter(name,defaultValue);
    checkBox = new Button(composite,SWT.CHECK);
    if (textKey != null)
      Messages.setLanguageText(checkBox, textKey);
    checkBox.setSelection(value);
    checkBox.addListener(SWT.Selection,new Listener() {
    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    public void handleEvent(Event event) {
		boolean selected  = checkBox.getSelection();
    COConfigurationManager.setParameter(name,selected);
    if(performers.size() > 0 ) {
    	for (int i=0;i<performers.size();i++){
    		IAdditionalActionPerformer	performer = (IAdditionalActionPerformer)performers.get(i);
    	
    		performer.setSelected(selected);
    		performer.performAction();
    	}
    }    
    }
  });
  }

  public void setLayoutData(Object layoutData) {
    checkBox.setLayoutData(layoutData);
  }
  
  public void setAdditionalActionPerformer(IAdditionalActionPerformer actionPerformer) {
    performers.add(actionPerformer);
    boolean selected  = checkBox.getSelection();
    actionPerformer.setSelected(selected);
    actionPerformer.performAction();
  }
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IParameter#getControl()
   */
  public Control getControl() {
    return checkBox;
  }

  public boolean
  isSelected()
  {
  	return( checkBox.getSelection());
  }
}
