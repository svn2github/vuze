/*
 * Created on Nov 10, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins.impl;

import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.plugins.Plugin;
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
	
	public PluginConfigImpl() {
		config = ConfigurationManager.getInstance();
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#SetParameter(java.lang.String, java.lang.String, org.gudy.azureus2.plugins.Plugin)
	 */
	public void setPluginParameter(String name, String value, Plugin setter) {
		config.setParameter("plugin."+setter.getClass().getName()+name, value);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#SetParameter(java.lang.String, int, org.gudy.azureus2.plugins.Plugin)
	 */
	public void setPluginParameter(String name, int value, Plugin setter) {
		config.setParameter("plugin."+setter.getClass().getName()+name, value);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#SetParameter(java.lang.String, boolean, org.gudy.azureus2.plugins.Plugin)
	 */
	public void setPluginParameter(String name, boolean value, Plugin setter) {
		config.setParameter("plugin."+setter.getClass().getName()+name, value);
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
	 * @see org.gudy.azureus2.plugins.PluginConfig#getBooleanParameter(java.lang.String)
	 */
	public boolean getBooleanParameter(String name) {
		return config.getBooleanParameter(name);
	}
}
