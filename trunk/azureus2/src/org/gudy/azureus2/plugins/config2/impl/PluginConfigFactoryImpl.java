/*
 * Created on Nov 16, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins.config2.impl;

import org.gudy.azureus2.plugins.config2.PluginConfigFactory;
import org.gudy.azureus2.plugins.config2.StringConfigProperty;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PluginConfigFactoryImpl implements PluginConfigFactory
{

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.config2.PluginConfigFactory#createStringConfigProperty(java.lang.String, java.lang.String, java.lang.String)
	 */
	public StringConfigProperty createStringConfigProperty(
		String identifier,
		String label,
		String value)
	{
		StringConfigPropertyImpl x = new StringConfigPropertyImpl();
		x.setIdentifier(identifier);
		x.setLabel(label);
		x.setValue(value);
		return x;
	}
}
