/*
 * File    : ParameterRepository.java
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
import java.util.HashMap;
import org.gudy.azureus2.plugins.ui.config.*;

/**
 * @author epall
 *
 */
public class ParameterRepository
{
	private static ParameterRepository instance;
	private HashMap params;
	
	private ParameterRepository()
	{
		params = new HashMap();
	}
	
	public static synchronized ParameterRepository getInstance()
	{
		if(instance == null)
			instance = new ParameterRepository();
		return instance;
	}
	
	public void addPlugin(Parameter[] parameters, String displayName)
	{
		params.put(displayName, parameters);
	}
	
	public int getSize()
	{
		return 2;
//		return params.size();
	}
	
	public String[] getNames()
	{
		String[] x = {"Hello, World!", "plugin 2"};
		return x;
//		String[] temp = new String[1];
//		return (String[])(params.keySet().toArray(temp));
	}
	
	public Parameter[] getParameterBlock(String key)
	{
		Parameter[] x = new Parameter[2];
		PluginConfigUIFactoryImpl factory = new PluginConfigUIFactoryImpl();
		x[0] = factory.createStringParameter("test.1", "first parameter", "none");
		x[1] = factory.createStringParameter("test.2", "second parameter", "none");		
		return x;
		
//		return (Parameter[])params.get(key);
	}
}
