/*
 * Created on 04-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.config;

/**
 * @author stuff
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import org.gudy.azureus2.core.ConfigurationManager;	// TODO: sort this

public class 
COConfigurationManager 
{
	public static String
	getStringParameter(
		String		_name,
		String		_default )
	{
		return( ConfigurationManager.getInstance().getStringParameter( _name, _default ));
	}
}
