/*
 * File    : PluginConfigImpl.java
 * Created : 10 nov. 2003
 * By      : epall
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

package org.gudy.azureus2.pluginsimpl;

import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.plugins.PluginConfig;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PluginConfigImpl
	implements PluginConfig {

	private ConfigurationManager config;
	private String key;
  
	public PluginConfigImpl(String key) {
    this.key = key + ".";
		config = ConfigurationManager.getInstance();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getStringParameter(java.lang.String)
	 */
	public String getStringParameter(String name) {
		return config.getStringParameter(name);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getIntParameter(java.lang.String)
	 */
	public int getIntParameter(String name) {
		return config.getIntParameter(name);
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getIntParameter(java.lang.String)
	 */
	public int getIntParameter(String name, int default_value) {
		return config.getIntParameter(name, default_value);
	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getBooleanParameter(java.lang.String)
	 */
	public boolean getBooleanParameter(String name) {
		return config.getBooleanParameter(name);
	}
	
	public boolean getBooleanParameter(String name, boolean _default) {
		return config.getBooleanParameter(name, _default);
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginIntParameter(java.lang.String)
	 */
	public int getPluginIntParameter(String key)
	{
		return getIntParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginIntParameter(java.lang.String, int)
	 */
	public int getPluginIntParameter(String key, int defaultValue)
	{
		return config.getIntParameter(this.key+key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginStringParameter(java.lang.String)
	 */
	public String getPluginStringParameter(String key)
	{
		return getStringParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginStringParameter(java.lang.String, int)
	 */
	public String getPluginStringParameter(String key, String defaultValue)
	{
		return config.getStringParameter(this.key+key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginBooleanParameter(java.lang.String)
	 */
	public boolean getPluginBooleanParameter(String key)
	{
		return getBooleanParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginBooleanParameter(java.lang.String, int)
	 */
	public boolean getPluginBooleanParameter(String key, boolean defaultValue)
	{
		return config.getBooleanParameter(this.key+key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, int)
	 */
	public void setPluginParameter(String key, int value)
	{
		config.setParameter(this.key+key, value);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, java.lang.String)
	 */
	public void setPluginParameter(String key, String value)
	{
		config.setParameter(this.key+key, value);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, boolean)
	 */
	public void setPluginParameter(String key, boolean value)
	{
		config.setParameter(this.key+key, value);
	}
}
