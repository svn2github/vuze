/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * Set.java
 * 
 * Created on 23.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.ui.common.ExternalUIConst;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * command that allows manipulation of Azureus' runtime properties.
 * - when called without any parameters, it lists all of the available runtime properties.
 * - when called with 1 parameter, it shows the current value of that parameter
 * - when called with 2 or 3 parameters, it assigns a specified value to the 
 *   specified parameter name. (the third parameter forces the property to be set
 *   to a particular type, otherwise we try and guess the type by the current value)
 * @author Tobias Minich, Paul Duran
 */
public class Set extends IConsoleCommand {
	
	private static final String NULL_STRING = "__NULL__";

	public Set()
	{
		super(new String[] {"set", "+" });
	}
	
	public String getCommandDescriptions() {
		return("set [parameter] [value]\t\t+\tSet a configuration parameter. The whitespaceless notation has to be used. If value is omitted, the current setting is shown.");
	}
	
	public void execute(String commandName,ConsoleInput ci, List args) {
		if( args.isEmpty() )
		{
			displayOptions(ci.out);
			return;
		}
		String origParamName = (String) args.get(0);
		String parameter = (String) ExternalUIConst.parameterlegacy.get(origParamName);
		if( parameter == null || parameter.length() == 0 )
		{
			parameter = origParamName;
		}
//		else
//			ci.out.println("> converting " + origParamName + " to " + parameter);
		
		Parameter param;
		switch( args.size() )
		{
			case 1:
				// try to display the value of the specified parameter
				if( ! COConfigurationManager.doesParameterDefaultExist( parameter ) )					
				{
					ci.out.println("> Command 'set': Parameter '" + parameter + "' unknown.");
					return;
				}
				param = Parameter.get(parameter);
				param.name = origParamName;
				ci.out.println( param.toString() );
				break;
			case 2:
			case 3:
				String setto = (String) args.get(1);
				String type;
				if( args.size() == 2 )
				{
					// guess the parameter type by getting the current value and determining its type
					param = Parameter.get(parameter);
					type = param.getType();
				}
				else
					type = (String) args.get(2);
				
				boolean success = false;
				if( type.equalsIgnoreCase("int") ) {
					COConfigurationManager.setParameter( parameter, Integer.parseInt( setto ) );
					success = true;
				}
				else if( type.equalsIgnoreCase("bool") ) {
					COConfigurationManager.setParameter( parameter, setto.equalsIgnoreCase("true") ? true : false );
					success = true;
				}
				else if( type.equalsIgnoreCase("float") ) {
					COConfigurationManager.setParameter( parameter, Float.parseFloat( setto ) );
					success = true;
				}
				else if( type.equalsIgnoreCase("string") ) {
					COConfigurationManager.setParameter( parameter, setto );
					success = true;
				}
				else if( type.equalsIgnoreCase("password") ) {
					SHA1Hasher hasher = new SHA1Hasher();
					
					byte[] password = setto.getBytes();
					
					byte[] encoded;
					
					if(password.length > 0){
						
						encoded = hasher.calculateHash(password);
						
					}else{
						
						encoded = password;
					}
					
					COConfigurationManager.setParameter( parameter, encoded );
					
					success = true;
				}
				
				if( success ) {
					COConfigurationManager.save();
					ci.out.println("> Parameter '" + parameter + "' set to '" + setto + "'. [" + type + "]");
				}
				else ci.out.println("ERROR: invalid type given");
				
				break;
			default:
				ci.out.println("Usage: 'set \"parameter\" value type', where type = int, bool, float, string, password");
				break;
		}
	}

	private void displayOptions(PrintStream out)
	{
		Iterator I = COConfigurationManager.getAllowedParameters().iterator();
		Map backmap = new HashMap();
		for (Iterator iter = ExternalUIConst.parameterlegacy.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			backmap.put( entry.getValue(), entry.getKey() );
		}
		TreeSet srt = new TreeSet();
		while (I.hasNext()) {
			String parameter = (String) I.next();
			if( backmap.containsKey(parameter))
				parameter = (String) backmap.get(parameter);
			
			Parameter param = Parameter.get( parameter );
			srt.add( param.toString() );
		}
		I = srt.iterator();
		while (I.hasNext()) {
			out.println((String) I.next());
		}
	}

	/**
	 * class that represents a parameter. we can use one of these objects to 
	 * verify a parameter's type and value as well as whether or not a value has been set. 
	 * @author pauld
	 */
	private static class Parameter
	{
		private static final int PARAM_INT = 1;
		private static final int PARAM_BOOLEAN = 2;
		private static final int PARAM_STRING = 4;
		
		/**
		 * returns a new Parameter object reprenting the specified parameter name
		 * @param parameter
		 * @return
		 */
		public static Parameter get(String parameter)
		{
			int underscoreIndex = parameter.indexOf('_');
			int nextchar = parameter.charAt(underscoreIndex + 1);
			try {
				if( nextchar == 'i' )
				{
					int value = COConfigurationManager.getIntParameter(parameter, Integer.MIN_VALUE);
					return new Parameter(parameter, value == Integer.MIN_VALUE ? (Integer)null : Integer.valueOf(value) );
				}
				else if( nextchar == 'b' )
				{
					// firstly get it as an integer to make sure it is actually set to something
					if( COConfigurationManager.getIntParameter(parameter, Integer.MIN_VALUE) != Integer.MIN_VALUE )
					{
						boolean b = COConfigurationManager.getBooleanParameter(parameter);
						return new Parameter(parameter, Boolean.valueOf(b));
					}
					else
					{
						return new Parameter(parameter, (Boolean)null);
					}
				}
				else
				{
					String value = COConfigurationManager.getStringParameter(parameter, NULL_STRING);				
					return new Parameter( parameter, NULL_STRING.equals(value) ? null : value);
				}
			} catch (Exception e)
			{
				try {
					int value = COConfigurationManager.getIntParameter(parameter, Integer.MIN_VALUE);
					return new Parameter(parameter, value == Integer.MIN_VALUE ? (Integer)null : Integer.valueOf(value) );
				} catch (Exception e1)
				{
					String value = COConfigurationManager.getStringParameter(parameter);
					return new Parameter( parameter, NULL_STRING.equals(value) ? null : value);
				}
			}
		}
		public Parameter( String name, Boolean val )
		{
			this(name, val, PARAM_BOOLEAN);
		}
		public Parameter( String name, Integer val )
		{
			this(name, val, PARAM_INT);
		}
		public Parameter( String name, String val )
		{
			this(name, val, PARAM_STRING);
		}
		private Parameter( String name, Object val, int type )
		{
			this.type = type;
			this.name = name;
			this.value = val;
			this.isSet = (val != null);
		}
		public final int type;
		public String name;
		public final Object value;
		public final boolean isSet;
		
		public String getType()
		{
			switch( type )
			{
				case PARAM_BOOLEAN:
					return "bool";
				case PARAM_INT:
					return "int";
				case PARAM_STRING:
					return "string";
				default:
					return "unknown";
			}
		}	
		public String toString()
		{
			if( isSet )
				return "> " + name + ": " + value + " [" + getType() + "]";				
			else
				return "> " + name + " is not set. [" + getType() + "]";
		}
	}
}
