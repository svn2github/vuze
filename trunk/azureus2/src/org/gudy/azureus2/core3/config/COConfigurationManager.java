/*
 * File    : COConfigurationManager.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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
 
package org.gudy.azureus2.core3.config;

import java.util.Map;
import java.util.Set;
import java.io.IOException;

import org.gudy.azureus2.core3.config.impl.*;

public class 
COConfigurationManager 
{
	public static final int CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED			= 5;
	public static final int CONFIG_DEFAULT_MAX_DOWNLOAD_SPEED			= 0;
	public static final int	CONFIG_DEFAULT_MAX_CONNECTIONS_PER_TORRENT	= 100;
	public static final int	CONFIG_DEFAULT_MAX_CONNECTIONS_GLOBAL		= 1000;
	
	public static void
	setSystemProperties()
	{
		ConfigurationChecker.setSystemProperties();
	}
	
	public static void
	checkConfiguration()
	{
		ConfigurationChecker.checkConfiguration();
	}
	
	public static void
	initialiseFromMap(
		Map		data )
	{
		ConfigurationManager.getInstance(data);
	}
	
	public static String
	getStringParameter(
		String		_name )
	{
		return( ConfigurationManager.getInstance().getStringParameter( _name ));
	}
	
	public static String
	getStringParameter(
		String		_name,
		String		_default )
	{
		return( ConfigurationManager.getInstance().getStringParameter( _name, _default ));
	}
	
	public static boolean
	setParameter(String parameter, String value) 
	{
		return ConfigurationManager.getInstance().setParameter( parameter, value );
	}

	public static boolean
	getBooleanParameter(
		String		_name )
	{
		return( ConfigurationManager.getInstance().getBooleanParameter( _name ));
	}
	
	public static boolean
	getBooleanParameter(
		String		_name,
		boolean		_default )
	{
		return( ConfigurationManager.getInstance().getBooleanParameter( _name, _default ));
	}
	
	public static boolean  
	setParameter(String parameter, boolean value) 
	{
		return ConfigurationManager.getInstance().setParameter( parameter, value );
	}
	
	public static int
	getIntParameter(
		String		_name )
	{
		return( ConfigurationManager.getInstance().getIntParameter( _name ));
	}
	
	public static int
	getIntParameter(
		String		_name,
		int		_default )
	{
		return( ConfigurationManager.getInstance().getIntParameter( _name, _default ));
	}
	
	public static boolean 
	setParameter(String parameter, int value) 
	{
		return ConfigurationManager.getInstance().setParameter( parameter, value );
	}
	
	public static byte[]
	getByteParameter(
		String		_name,
		byte[]		_default )
	{
		return( ConfigurationManager.getInstance().getByteParameter( _name, _default ));
	}
	
	public static boolean
	setParameter(String parameter, byte[] value) 
	{
		return ConfigurationManager.getInstance().setParameter( parameter, value );
	}
	
	public static String
	getDirectoryParameter(
		String		_name )		
		throws IOException
	{
		return( ConfigurationManager.getInstance().getDirectoryParameter( _name ));
	}
	
	/*
	public static boolean
	setParameter(String parameter, Color value) 
	{
		return ConfigurationManager.getInstance().setParameter( parameter, value );
	}

	public static boolean
	setParameter(String parameter, RGB value) 
	{
		return ConfigurationManager.getInstance().setParameter( parameter, value );
	}
	*/
	
	public static boolean
	setRGBParameter(String parameter, int red, int green, int blue) 
	{
		return ConfigurationManager.getInstance().setRGBParameter( parameter, red, green, blue);
	}


	public static float
	getFloatParameter(
		String		_name)
	{
		return( ConfigurationManager.getInstance().getFloatParameter( _name ));
	}
	
	public static boolean 
	setParameter(String parameter, float value)
	{
		return ConfigurationManager.getInstance().setParameter( parameter, value );
	}

  public static void
	save()
	{
		ConfigurationManager.getInstance().save();
	}
	
	public static void
	addListener(
		COConfigurationListener		listener )
	{
		ConfigurationManager.getInstance().addListener( listener );
	}

  public static void
  addParameterListener(String parameter, ParameterListener listener)
  {
    ConfigurationManager.getInstance().addParameterListener(parameter, listener);
  }

  public static void
  removeParameterListener(String parameter, ParameterListener listener)
  {
    ConfigurationManager.getInstance().removeParameterListener(parameter, listener);
  }

  public static void
	removeListener(
		COConfigurationListener		listener )
	{
		ConfigurationManager.getInstance().removeListener( listener );
	}
  
  public static Set
  getAllowedParameters()
  {
  	return ConfigurationDefaults.getInstance().getAllowedParameters();
  }
        public static boolean
        doesParameterExist(
                String                          parameter)
        {
                return ConfigurationDefaults.getInstance().doesParameterExist(parameter);
        }
        
  public static void
  registerExternalDefaults(
  	Map							addmap)
  {
  	ConfigurationDefaults.getInstance().registerExternalDefaults(addmap);
  }
  
  public static void
  setBooleanDefault(
  	String	parameter,
	boolean	_default )
  {
  	ConfigurationDefaults.getInstance().addParameter( parameter, _default );
  }
  
  public static void
  setIntDefault(
  	String	parameter,
	int	_default )
  {
  	ConfigurationDefaults.getInstance().addParameter( parameter, _default );
  }
  
  public static void
  setStringDefault(
  	String	parameter,
	String	_default )
  {
  	ConfigurationDefaults.getInstance().addParameter( parameter, _default );
  }
}
