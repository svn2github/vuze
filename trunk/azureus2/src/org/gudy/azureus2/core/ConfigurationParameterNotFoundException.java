/*
 * ConfigurationParameterNotFoundException.java
 *
 * Created on 22. August 2003, 19:18
 */

package org.gudy.azureus2.core;

/**
 *
 * @author  Tobias Minich
 */

public class ConfigurationParameterNotFoundException extends Exception {
  
  /** Creates a new instance of ConfigurationNotFoundException */
  public ConfigurationParameterNotFoundException() {
    super();
  }
  
  public ConfigurationParameterNotFoundException(String s) {
    super(s);
  }
}
