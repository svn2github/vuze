/*
 * Created on Nov 10, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins;

/**
 * @author Eric Allen
 * Read-Only (and confined write) access for plugins to Azureus configuration
 * 
 */
public interface PluginConfig {
	
	/**
	 * A plugin may want to get a String configuration parameter.
	 * @param name identifier of the property
	 * @return the value of the specified property
	 */
	public String getStringParameter(String name);
	
	/**
	 * A plugin may want to get an int configuration parameter.
	 * @param name identifier of the property
	 * @return the value of the specified property
	 */
	public int getIntParameter(String name);

	/**
	 * A plugin may want to get a boolean configuration parameter.
	 * @param name identifier of the property
	 * @return the value of the specified property
	 */
	public boolean getBooleanParameter(String name);

	/**
	 * A plugin may want to get a byte[] configuration parameter.
	 * @param name identifier of the property
	 * @return the value of the specified property
	 */
//	public byte[] GetByteParameter(String name);
	
	// write-access spec

	/**
	 * A plugin may want to set a parameter of it's own.
	 * All names will be prefixed with plugin.pluginname.
	 * @param name The identifier of the property
	 * @param value The value of the property to store
	 * @param setter The Plugin that is setting the parameter
	 */
	public void setPluginParameter(String name, String value, Plugin setter);

	/**
	 * A plugin may want to set a parameter of it's own.
	 * All names will be prefixed with plugin.pluginname.
	 * @param name The identifier of the property
	 * @param value The value of the property to store
	 * @param setter The Plugin that is setting the parameter
	 */
	public void setPluginParameter(String name, int value, Plugin setter);

	/**
	 * A plugin may want to set a parameter of it's own.
	 * All names will be prefixed with plugin.pluginname.
	 * @param name The identifier of the property
	 * @param value The value of the property to store
	 * @param setter The Plugin that is setting the parameter
	 */
	public void setPluginParameter(String name, boolean value, Plugin setter);
}
