/*
 * File    : PluginConfig.java
 * Created : 17 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.plugins;

/**
 * @author Olivier
 *
 */
public interface PluginConfig {  

  /**
   * returns the value of a core int parameter
   * @param key the parameter name
   * @return the value of the parameter
   */
  public int getIntParameter(String key);

  /**
   * returns the value of a core int parameter
   * @param key the parameter name
   * @param default_value the default return value
   * @return the value of the parameter
   */
  public int getIntParameter(String key, int default_value);
  
  /**
   * returns the value of a core String parameter
   * @param key the parameter name
   * @return the value of the parameter
   */
  public String getStringParameter(String key);
  
  /**
   * returns the value of a core boolean parameter
   * @param key the parameter name
   * @return the value of the parameter
   */
  public boolean getBooleanParameter(String key);
  
  /**
   * returns the value of a core boolean parameter
   * @param key the parameter name
   * @param _default default value if non defined
   * @return the value of the parameter
   */
  public boolean getBooleanParameter(String key, boolean _default );
  
  /**
   * returns the value of a plugin int parameter
   * @param key the parameter name
   * @return the value or 0 if the parameter doesn't exist
   */
  public int getPluginIntParameter(String key);
  
  /**
   * returns the value of a plugin int parameter
   * @param key the parameter name
   * @param defaultValue the parameter default value
   * @return the value of defaultValue if the parameter doesn't exist
   */
  public int getPluginIntParameter(String key,int defaultValue);
  
  /**
   * returns the value of a plugin String parameter
   * @param key the parameter name
   * @return the value or an empty String if the parameter doesn't exist
   */
  public String getPluginStringParameter(String key);
  
  /**
   * returns the value of a plugin String parameter
   * @param key the parameter name
   * @param defaultValue the parameter default value
   * @return the value of defaultValue if the parameter doesn't exist
   */
  public String getPluginStringParameter(String key,String defaultValue);
  
  /**
   * returns the value of a plugin boolean parameter
   * @param key the parameter name
   * @return the value or false if the parameter doesn't exist
   */
  public boolean getPluginBooleanParameter(String key);
  
  /**
   * returns the value of a plugin boolean parameter
   * @param key the parameter name
   * @param defaultValue the parameter default value
   * @return the value of defaultValue if the parameter doesn't exist
   */
  public boolean getPluginBooleanParameter(String key,boolean defaultValue);
    
  /**
   * sets a plugin int parameter value
   * @param key the parameter name
   * @param value the parameter value
   */
  public void setPluginParameter(String key,int value);
  
  /**
   * sets a plugin String parameter value
   * @param key the parameter name
   * @param value the parameter value
   */
  public void setPluginParameter(String key,String value);
  
  /**
   * sets a plugin boolean parameter value
   * @param key the parameter name
   * @param value the parameter value
   */
  public void setPluginParameter(String key,boolean value);

  /**
   * make sure you save it after making changes!
   *
   */
  
	public void
	save()
		throws PluginException;
}
