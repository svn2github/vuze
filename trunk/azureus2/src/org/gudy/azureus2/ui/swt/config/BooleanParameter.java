/*
 * Created on 9 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.ui.swt.config.generic.GenericBooleanParameter;

/**
 * @author Olivier
 * 
 */
public class BooleanParameter extends Parameter{
  protected GenericBooleanParameter	delegate;
  
  public BooleanParameter(Composite composite, final String name) {
	  delegate = new GenericBooleanParameter( config_adapter, composite,name,COConfigurationManager.getBooleanParameter(name),null,null);
  }

  public BooleanParameter(Composite composite, final String name, String textKey) {
	  delegate = new GenericBooleanParameter( config_adapter, composite, name, COConfigurationManager.getBooleanParameter(name),
         textKey, null);
  }

  public BooleanParameter(Composite composite, final String name, boolean defaultValue, String textKey) {
	  delegate = new GenericBooleanParameter( config_adapter, composite,name,defaultValue,textKey,null);
  }

  public BooleanParameter(Composite composite, final String name, boolean defaultValue) {
	  delegate = new GenericBooleanParameter( config_adapter, composite,name,defaultValue,null,null);
  }
  
  public 
  BooleanParameter(
  		Composite composite, 
		final String _name, 
        boolean _defaultValue,
        String textKey,
        IAdditionalActionPerformer actionPerformer) 
  {
	  delegate = new GenericBooleanParameter( config_adapter, composite, _name, _defaultValue, textKey, actionPerformer );
  }

  public void setLayoutData(Object layoutData) {
   delegate.setLayoutData( layoutData );
  }
  
  public void setAdditionalActionPerformer(IAdditionalActionPerformer actionPerformer) {
	 delegate.setAdditionalActionPerformer( actionPerformer );
  }
 
  public Control getControl() {
    return delegate.getControl();
  }
  
  public String getName() {
  	return delegate.getName();
  }
  
  public void setName(String newName) {
  	delegate.setName( newName );
  }

  public boolean
  isSelected()
  {
  	return(delegate.isSelected());
  }
  
  public void
  setSelected(
  	boolean	selected )
  {
  	delegate.setSelected( selected );
  }
}
