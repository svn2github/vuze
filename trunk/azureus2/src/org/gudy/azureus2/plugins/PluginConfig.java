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

public interface 
PluginConfig 
{  
  /**
   *
   * @since 2.0.8.2
   */
	public static final String CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC	= "Max Upload Speed KBs";
  /**
   *
   * @since 2.1.0.0
   */
	public static final String CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC	= "Max Download Speed KBs";
  /**
   *
   * @since 2.1.0.0
   */
	public static final String CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT		= "Max Connections Per Torrent";
  /**
   *
   * @since 2.1.0.0
   */
	public static final String CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL			= "Max Connections Global";
	

  /**
   * returns the value of a core float parameter
   * @param key the parameter name
   * @return the value of the parameter
   *
   * @since 2.1.0.0
   */
  public float getFloatParameter(String key);

  /**
   * returns the value of a core int parameter
   * @param key the parameter name
   * @return the value of the parameter
   *
   * @since 2.0.4.2
   */
  public int getIntParameter(String key);
	
  /**
   * returns the value of a core int parameter
   * @param key the parameter name
   * @param default_value the default return value
   * @return the value of the parameter
   *
   * @since 2.0.7.0
   */
  public int getIntParameter(String key, int default_value);
  
  /**
   * sets a core parameter.  
   * @param key		must be from above core constants
   * @param value the new value
   *
   * @since 2.0.8.0
   */
  
  public void
  setIntParameter(
  	String	key, 
	int		value );
  
  /**
   * returns the value of a core String parameter
   * @param key the parameter name
   * @return the value of the parameter
   *
   * @since 2.0.4.2
   */
  
  public String getStringParameter(String key);
  
  /**
   * returns the value of a core string parameter or the default value if not defined
   * @param key
   * @param _default
   * @return the value of the parameter
   *
   * @since 2.1.0.0
   */
  
  public String getStringParameter(String key, String _default );
  
  /**
   * returns the value of a core boolean parameter
   * @param key the parameter name
   * @return the value of the parameter
   *
   * @since 2.0.4.2
   */
  public boolean getBooleanParameter(String key);
  
  /**
   * returns the value of a core boolean parameter
   * @param key the parameter name
   * @param _default default value if non defined
   * @return the value of the parameter
   *
   * @since 2.0.6.0
   */
  public boolean getBooleanParameter(String key, boolean _default );
  
  /**
   * 
   * @param key
   * @param _default
   * @return
   * @since 2.1.0.2
   */
  
  public byte[] getByteParameter(String key, byte[] _default );
  
  /**
   * returns the value of a plugin int parameter
   * @param key the parameter name
   * @return the value or 0 if the parameter doesn't exist
   *
   * @since 2.0.4.2
   */
  public int getPluginIntParameter(String key);
  
  /**
   * returns the value of a plugin int parameter
   * @param key the parameter name
   * @param defaultValue the parameter default value
   * @return the value of defaultValue if the parameter doesn't exist
   *
   * @since 2.0.4.2
   */
  public int getPluginIntParameter(String key,int defaultValue);
  
  /**
   * returns the value of a plugin String parameter
   * @param key the parameter name
   * @return the value or an empty String if the parameter doesn't exist
   *
   * @since 2.0.4.2
   */
  public String getPluginStringParameter(String key);
  
  /**
   * returns the value of a plugin String parameter
   * @param key the parameter name
   * @param defaultValue the parameter default value
   * @return the value of defaultValue if the parameter doesn't exist
   *
   * @since 2.0.4.2
   */
  public String getPluginStringParameter(String key,String defaultValue);
  
  /**
   * returns the value of a plugin boolean parameter
   * @param key the parameter name
   * @return the value or false if the parameter doesn't exist
   *
   * @since 2.0.4.2
   */
  public boolean getPluginBooleanParameter(String key);
  
  /**
   * returns the value of a plugin boolean parameter
   * @param key the parameter name
   * @param defaultValue the parameter default value
   * @return the value of defaultValue if the parameter doesn't exist
   *
   * @since 2.0.4.2
   */
  public boolean getPluginBooleanParameter(String key,boolean defaultValue);
    
  /**
   * sets a plugin int parameter value
   * @param key the parameter name
   * @param value the parameter value
   *
   * @since 2.0.4.2
   */
  public void setPluginParameter(String key,int value);
  
  /**
   * sets a plugin String parameter value
   * @param key the parameter name
   * @param value the parameter value
   *
   * @since 2.0.4.2
   */
  public void setPluginParameter(String key,String value);
  
  /**
   * sets a plugin boolean parameter value
   * @param key the parameter name
   * @param value the parameter value
   *
   * @since 2.0.4.2
   */
  public void setPluginParameter(String key,boolean value);
  
  /**
   * @since 2.1.0.2
   * @param key
   * @param value
   */
  
  public void setPluginParameter(String key,byte[] value);

  /**
   * @return the prefix used when storing configuration values in the config file for
   * this plugin's config parameters
   *
   * @since 2.1.0.0
   */
  public String
  getPluginConfigKeyPrefix();

  /**
   * make sure you save it after making changes!
   *
   * @since 2.0.8.0
   */
	public void
	save()
		throws PluginException;
}
