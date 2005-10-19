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

import java.io.File;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.plugins.config.*;

public interface 
PluginConfig 
{  
	public static final String CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC			= "Max Upload Speed KBs";
	public static final String CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC = "Max Upload Speed When Only Seeding KBs";
 	public static final String CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC			= "Max Download Speed KBs";
 	public static final String CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT				= "Max Connections Per Torrent";
 	public static final String CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL					= "Max Connections Global";
 	public static final String CORE_PARAM_INT_MAX_DOWNLOADS								= "Max Downloads";
 	public static final String CORE_PARAM_INT_MAX_ACTIVE								= "Max Active Torrents";
 	public static final String CORE_PARAM_INT_MAX_ACTIVE_SEEDING				= "Max Active Torrents When Only Seeding";
 	
 	public static final String CORE_PARAM_BOOLEAN_MAX_UPLOAD_SPEED_SEEDING = "Max Upload Speed When Only Seeding Enabled";
 	public static final String CORE_PARAM_BOOLEAN_MAX_ACTIVE_SEEDING = "Max Active Torrents When Only Seeding Enabled";
	public static final String CORE_PARAM_BOOLEAN_SOCKS_PROXY_NO_INWARD_CONNECTION		= "SOCKS Proxy No Inward Connection";
	public static final String CORE_PARAM_BOOLEAN_NEW_SEEDS_START_AT_TOP				= "Newly Seeding Torrents Get First Priority";
	
	/**
	 * @since 2.3.0.5
	 */
	public static final String CORE_PARAM_STRING_LOCAL_BIND_IP							= "CORE_PARAM_STRING_LOCAL_BIND_IP";
	public static final String CORE_PARAM_BOOLEAN_FRIENDLY_HASH_CHECKING				= "CORE_PARAM_BOOLEAN_FRIENDLY_HASH_CHECKING";

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
   * Set a core boolean
   * @param key		 must be from above constants
   * @param value
   */
 
  public void
  setBooleanParameter(
  	String	key, 
	boolean		value );
  
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
   * @since 2.2.0.3
   * @param key
   * @param defaultValue
   * @return
   */
  
  public byte[] getPluginByteParameter( String key, byte[] defaultValue );
 
  /**
   * Get a list parameter List contents must conform to bencodable rules (Map,Long,byte[],List) 
   * @param key
   * @param default_value
   * @return
   * @since 2301
   */
  
  public List
  getPluginListParameter( String key, List	default_value );
 
  public void
  setPluginListParameter( String key, List	value );
 
  /**
   * Get a list parameter Map contents must conform to bencodable rules (Map,Long,byte[],List) 
   * @param key
   * @param default_value
   * @return
   * @since 2301
   */
  
  public Map
  getPluginMapParameter( String key, Map	default_value );
 
  public void
  setPluginMapParameter( String key, Map	value );
  
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

  public ConfigParameter
  getParameter(
  	String		key );
  
  public ConfigParameter
  getPluginParameter(
  	String		key );
  
  /**
   * make sure you save it after making changes!
   *
   * @since 2.0.8.0
   */
	public void
	save()
		throws PluginException;
	
		/**
		 * Returns a file that can be used by the plugin to save user-specific state
		 * This will be "azureus-user-dir"/plugins/<plugin name>/name 
		 * @param name
		 * @return
		 */
	
	public File
	getPluginUserFile(
		String	name );
	
	public void
	addListener(
		PluginConfigListener	l );
}
