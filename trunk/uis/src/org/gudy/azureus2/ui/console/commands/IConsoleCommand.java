/*
 * Written and copyright 2001-2004 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 */

package org.gudy.azureus2.ui.console.commands;

import java.util.List;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author Tobias Minich
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface IConsoleCommand {
	public static final Class ConsoleCommandParameters[] = {ConsoleInput.class,  List.class};
	
	/*
	 * This Interface serves only as a tag. The following method(s) need to be implemented
	 * by command classes
	 */
	
	//public static void RegisterCommands();
}
