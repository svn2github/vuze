/*
 * Created on 9 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.config.generic;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.IAdditionalActionPerformer;

/**
 * @author Olivier
 * 
 */
public class 
GenericBooleanParameter
{
  GenericParameterAdapter	adapter;
	
  String 	name;
  Button 	checkBox;
  boolean	defaultValue;
  
  List	performers	= new ArrayList();
  
  public GenericBooleanParameter(GenericParameterAdapter adapter,Composite composite, final String name) {
    this(adapter,composite,name,adapter.getBooleanValue(name),null,null);
  }

  public GenericBooleanParameter(GenericParameterAdapter adapter,Composite composite, final String name, String textKey) {
    this(adapter,composite, name, adapter.getBooleanValue(name),
         textKey, null);
  }

  public GenericBooleanParameter(GenericParameterAdapter adapter,Composite composite, final String name, boolean defaultValue, String textKey) {
    this(adapter,composite,name,defaultValue,textKey,null);
  }

  public GenericBooleanParameter(GenericParameterAdapter adapter,Composite composite, final String name, boolean defaultValue) {
    this(adapter,composite,name,defaultValue,null,null);
  }
  
  public 
  GenericBooleanParameter(
		GenericParameterAdapter _adapter,
  		Composite composite, 
		final String _name, 
        boolean _defaultValue,
        String textKey,
        IAdditionalActionPerformer actionPerformer) 
  {
	adapter			= _adapter;
  	name			= _name;
  	defaultValue	= _defaultValue;
    if ( actionPerformer != null ){
    	performers.add( actionPerformer );
    }
    boolean value = adapter.getBooleanValue(name,defaultValue);
    checkBox = new Button(composite,SWT.CHECK);
    if (textKey != null)
      Messages.setLanguageText(checkBox, textKey);
    checkBox.setSelection(value);
    checkBox.addListener(SWT.Selection,new Listener() {
    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    public void 
	handleEvent(
		Event event) 
    {
		setSelected( checkBox.getSelection(), true );
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
  
  public String getName() {
  	return name;
  }
  
  public void setName(String newName) {
  	name = newName;
  }

  public boolean
  isSelected()
  {
  	return( checkBox.getSelection());
  }
  
  public void
  setSelected(
  	boolean	selected )
  {
  	setSelected( selected, false );
  }
  
  protected void
  setSelected(
  	boolean	selected,
	boolean	force )
  {
   	if ( selected != checkBox.getSelection() || force ){
 			
		adapter.setBooleanValue(name,selected);
		 			
  		checkBox.setSelection( selected );
  		
	    if ( performers.size() > 0 ){
	    	
	    	for (int i=0;i<performers.size();i++){
	    		
	    		IAdditionalActionPerformer	performer = (IAdditionalActionPerformer)performers.get(i);
	    	
	    		performer.setSelected(selected);
	    		
	    		performer.performAction();
	    	}
	    }
	    
	    adapter.informChanged( false );
	 }
  }
}
