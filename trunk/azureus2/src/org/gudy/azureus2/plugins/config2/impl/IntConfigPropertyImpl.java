/*
 * Created on Nov 16, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins.config2.impl;

import org.gudy.azureus2.plugins.config2.IntConfigProperty;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class IntConfigPropertyImpl
	extends ConfigPropertyImpl
	implements IntConfigProperty
{
	protected int value;
	
	/**
	 * @return Returns the value.
	 */
	int getValue()
	{
		return value;
	}

	/**
	 * @param value The value to set.
	 */
	void setValue(int value)
	{
		this.value = value;
	}

}
