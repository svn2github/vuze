/*
 * File    : PluginConfigUIFactoryImpl.java
 * Created : Nov 21, 2003
 * By      : epall
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
 
package org.gudy.azureus2.plugins.ui.config.impl2;

import org.gudy.azureus2.plugins.ui.config.EnablerParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;

/**
 * @author epall
 *
 */
public class PluginConfigUIFactoryImpl implements PluginConfigUIFactory
{

  /* (non-Javadoc)
   * @see org.gudy.azureus2.plugins.ui.PluginConfigUIFactory#createDirectoryParameter(java.lang.String, java.lang.String, java.lang.String)
   */ 
  public Parameter createDirectoryParameter(
    String key,
    String label,
    String defaultValue) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.plugins.ui.PluginConfigUIFactory#createIntParameter(java.lang.String, java.lang.String, int, int[], java.lang.String[])
   */
  public Parameter createIntParameter(
    String key,
    String label,
    int defaultValue,
    int[] values,
    String[] labels) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.plugins.ui.PluginConfigUIFactory#createIntParameter(java.lang.String, java.lang.String, int, java.lang.String[], java.lang.String[])
   */
  public Parameter createIntParameter(
    String key,
    String label,
    int defaultValue,
    String[] values,
    String[] labels) {
    // TODO Auto-generated method stub
    return null;
  }

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.PluginConfigUIFactory#createBooleanParameter(java.lang.String, java.lang.String, boolean)
	 */
	public EnablerParameter createBooleanParameter(
		String key,
		String label,
		boolean defaultValue)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.PluginConfigUIFactory#createIntParameter(java.lang.String, java.lang.String, boolean)
	 */
	public Parameter createIntParameter(
		String key,
		String label,
		int defaultValue)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.PluginConfigUIFactory#createStringParameter(java.lang.String, java.lang.String, boolean)
	 */
	public Parameter createStringParameter(
		String key,
		String label,
		String defaultValue)
	{
		return new StringParameter(key, label, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.PluginConfigUIFactory#createFileParameter(java.lang.String, java.lang.String, boolean)
	 */
	public Parameter createFileParameter(
		String key,
		String label,
		String defaultValue)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.PluginConfigUIFactory#createColorParameter(java.lang.String, java.lang.String, boolean)
	 */
	public Parameter createColorParameter(
		String key,
		String label,
		int defaultValueRed,
    int defaultValueGreen,
    int defaultValueBlue)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
