/*
 * Created on Nov 16, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins.config2;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface PluginConfigFactory
{
	StringConfigProperty createStringConfigProperty(String identifier, String label, String value);
	BooleanConfigProperty createBooleanConfigProperty(String identifier, String label, boolean value);
	IntConfigProperty createIntConfigProperty(String identifier, String label, int value);
}
