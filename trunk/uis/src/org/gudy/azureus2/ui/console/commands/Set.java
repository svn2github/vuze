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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.common.ExternalUIConst;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author Tobias Minich
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Set implements IConsoleCommand {
	
	public static void command(ConsoleInput ci, List args) {
		if ((args != null) && (!args.isEmpty())) {
			String parameter = (String) args.get(0);
			String setto = (args.size()==1)?null:((String) args.get(1));
			if (COConfigurationManager.doesParameterExist(ExternalUIConst.parameterlegacy.get(parameter).toString())) {
				try {
					if (setto == null) {
						if (parameter.substring(parameter.indexOf('_') + 1).startsWith("s"))
							ci.out.println("> " + parameter + ": " + COConfigurationManager.getStringParameter(ExternalUIConst.parameterlegacy.get(parameter).toString()));
						else
							ci.out.println("> " + parameter + ": " + COConfigurationManager.getIntParameter(ExternalUIConst.parameterlegacy.get(parameter).toString()));
					} else {
						if (parameter.substring(parameter.indexOf('_') + 1).startsWith("s")) {
							COConfigurationManager.setParameter(ExternalUIConst.parameterlegacy.get(parameter).toString(), setto);
						} else if (parameter.substring(parameter.indexOf('_') + 1).startsWith("s")) {
							COConfigurationManager.setParameter(ExternalUIConst.parameterlegacy.get(parameter).toString(), Float.parseFloat(setto));
						} else {
							COConfigurationManager.setParameter(ExternalUIConst.parameterlegacy.get(parameter).toString(), Integer.parseInt(setto));
						}
						COConfigurationManager.save();
						ci.out.println("> Parameter '" + parameter + "' set to '" + setto + "'.");
					}
				} catch (Exception e) {
					ci.out.println("> Command 'set': Exception '" + e.getMessage() + "' on parameter '" + parameter + "'");
          e.printStackTrace();
				}
			}
      else if( COConfigurationManager.doesParameterExist( parameter ) ) {
        try {
          if (setto == null) {
            try {
              ci.out.println("> " + parameter + ": " + COConfigurationManager.getStringParameter( parameter ) );
            } 
            catch( ClassCastException cce1 ) {
              try {
                ci.out.println("> " + parameter + ": " + COConfigurationManager.getIntParameter( parameter ) );
              }
              catch( ClassCastException cce2 ) {
                try {
                  ci.out.println("> " + parameter + ": " + COConfigurationManager.getBooleanParameter( parameter ) );
                }
                catch( ClassCastException cce3 ) {
                  ci.out.println("Error: cannot determine parameter type");
                }
              }
            }
          }
        	else if( args.size() == 3 ) {
        		String type = (String)args.get( 2 );
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

        		if( success ) {
        			COConfigurationManager.save();
        			ci.out.println("> Parameter '" + parameter + "' set to '" + setto + "'.");
        		}
        		else ci.out.println("ERROR: invalid type given");
        	}
        	else {
        		ci.out.println("Usage: 'set \"parameter\" value type', where type = int, bool, float, string");
        	}
        }
        catch( Throwable t ) {
          ci.out.println("> Command 'set': Exception '" + t.getMessage() + "' on parameter '" + parameter + "'");
          t.printStackTrace();
        }
      }
      else {
				ci.out.println("> Command 'set': Parameter '" + parameter + "' unknown.");
			}
		} else {
			Iterator I = COConfigurationManager.getAllowedParameters().iterator();
			Hashtable backmap = new Hashtable();
			Enumeration enum = ExternalUIConst.parameterlegacy.keys();
			while (enum.hasMoreElements()) {
				Object o = enum.nextElement();
				backmap.put(ExternalUIConst.parameterlegacy.get(o), o);
			}
			TreeSet srt = new TreeSet();
			while (I.hasNext()) {
				String parameter = (String) I.next();
				if (ExternalUIConst.parameterlegacy.containsValue(parameter))
					parameter = (String) backmap.get(parameter);
				try {
					if (parameter.substring(parameter.indexOf('_') + 1).startsWith("s"))
						srt.add("> " + parameter + ": " + COConfigurationManager.getStringParameter(ExternalUIConst.parameterlegacy.get(parameter).toString()));
					else
						srt.add("> " + parameter + ": " + COConfigurationManager.getIntParameter(ExternalUIConst.parameterlegacy.get(parameter).toString()));
				} catch (Exception e) {
					srt.add("> " + parameter + ": Exception '" + e.getMessage() + "' (Probably the parameter type couldn't be deduced from its name).");
				}
			}
			I = srt.iterator();
			while (I.hasNext()) {
				ci.out.println((String) I.next());
			}
		}
	}
	
	public static void RegisterCommands() {
		try {
			ConsoleInput.RegisterCommand("set", Set.class.getMethod("command", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("+", Set.class.getMethod("command", ConsoleCommandParameters));
			ConsoleInput.RegisterHelp("set [parameter] [value]\t\t+\tSet a configuration parameter. The whitespaceless notation has to be used. If value is omitted, the current setting is shown.");
		} catch (Exception e) {e.printStackTrace();}
	}
}
