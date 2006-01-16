/*
 * Created on 10 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.config.generic.GenericIntParameter;

/**
 * @author Olivier
 * 
 */
public class 
IntParameter 
	extends Parameter
{ 
  protected GenericIntParameter	delegate;
  
  public IntParameter(Composite composite, final String name) {
	  delegate = new GenericIntParameter( config_adapter, composite, name );
  }

  public IntParameter(Composite composite, final String name, boolean generateIntermediateEvents) {
	  delegate = new GenericIntParameter( config_adapter, composite, name, generateIntermediateEvents );
  }
  public IntParameter(Composite composite, final String name, int defaultValue) {
	  delegate = new GenericIntParameter( config_adapter, composite, name, defaultValue );
  }
  
  
  public IntParameter(Composite composite, final String name, int defaultValue,boolean generateIntermediateEvents) {
	  delegate = new GenericIntParameter( config_adapter, composite, name, defaultValue, generateIntermediateEvents );
  }
  
  public IntParameter(Composite composite,
                      final String name,
                      int minValue,
                      int maxValue,
                      boolean allowZero,
                      boolean generateIntermediateEvents ) {
	  delegate = new GenericIntParameter( config_adapter, composite, name, minValue, maxValue, allowZero, generateIntermediateEvents );
  }
  
   
  public void
  setAllowZero(
  	boolean		allow )
  {
  	delegate.setAllowZero( allow );
  }
  
  public void
  setMinimumValue(
  	int		value )
  {
	  delegate.setMinimumValue( value );
  }
  public void
  setMaximumValue(
  	int		value )
  {
  	delegate.setMaximumValue( value );
  }

  public void
  setValue(
  	int		value )
  {
	  delegate.setValue( value );
  }
  
  public int
  getValue()
  {
  	return( delegate.getValue());
  }
  
  public void setLayoutData(Object layoutData) {
   delegate.setLayoutData( layoutData );
  }
  
  public Control
  getControl()
  {
  	return( delegate.getControl());
  }
}