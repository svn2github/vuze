/*
 * Created on Nov 16, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins.config2.impl;
import org.gudy.azureus2.plugins.config2.ConfigProperty;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ConfigPropertyImpl implements ConfigProperty
{
	protected String identifier;
	protected String label;
	
	/**
	 * @return Returns the identifier.
	 */
	String getIdentifier()
	{
		return identifier;
	}

	/**
	 * @return Returns the label.
	 */
	String getLabel()
	{
		return label;
	}

	/**
	 * @param identifier The identifier to set.
	 */
	void setIdentifier(String identifier)
	{
		this.identifier = identifier;
	}

	/**
	 * @param label The label to set.
	 */
	void setLabel(String label)
	{
		this.label = label;
	}

}
