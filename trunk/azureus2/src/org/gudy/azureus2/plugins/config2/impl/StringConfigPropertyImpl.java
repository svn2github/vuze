/*
 * Created on Nov 16, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins.config2.impl;

import org.gudy.azureus2.plugins.config2.StringConfigProperty;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class StringConfigPropertyImpl
	extends ConfigPropertyImpl
	implements StringConfigProperty
{
	protected String value;
	
	String getValue()
	{
		return value;
	}

	/**
	 * @param value The value to set.
	 */
	void setValue(String value)
	{
		this.value = value;
	}

}
