/*
 * Created on 24.11.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.core3.config;

/**
 * A ParameterListener is called when a configuration parameter changes.
 * @author Rene Leonhardt
 */
public interface ParameterListener {
  /**
   * Called, when a parameter has changed.
   * The listener could only react if the parameter name is relevant.
   * Or the listener can just read all parameters again.
   * @param parameterName the name of the parameter that has changed
   */
  public void parameterChanged(String parameterName);
}
