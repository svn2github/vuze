/*
 * Created on Nov 16, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins.config2.impl;

import org.gudy.azureus2.plugins.config2.*;

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

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.config2.PluginConfigFactory#createBooleanConfigProperty(java.lang.String, java.lang.String, boolean)
	 */
	public BooleanConfigProperty createBooleanConfigProperty(String identifier, String label, boolean value)
	{
		BooleanConfigPropertyImpl x = new BooleanConfigPropertyImpl();
		x.setIdentifier(identifier);
		x.setLabel(label);
		x.setValue(value);
		return x;
	}

	public IntConfigProperty createIntConfigProperty(String identifier, String label, int value)
	{
		IntConfigPropertyImpl x = new IntConfigPropertyImpl();
		x.setIdentifier(identifier);
		x.setLabel(label);
		x.setValue(value);
		return x;
	}
}
