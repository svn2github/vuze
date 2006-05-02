/*
 * Created on 02-May-2006
 * Created by Damokles
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.plugins.ipc;

/**
 * @author Damokles
 *
 */
public interface IPCInterface {

	/**
	 * This function will call the given method on the plugin.
	 * 
	 * This function allows direct method calls to the plugin
	 * using Java Reflection API.
	 * 
	 * Primitives like int, boolean need to be wrappen in their
	 * Objects (int -> Integer).
	 * 
	 * Results will be returned as Object and can be classcasted.
	 * 
	 * WARNING: only call Methods that use Java or Azureus Classes
	 * 			the use of custom classes may cause problems.
	 * 
	 * Example:
	 * 
	 * 1.
	 * Plugin has method
	 * int add (int x, int y);
	 * 
	 * int result = ((Integer)invoke ("add", new Object[] {Integer.valueOf(10),Integer.valueOf(5)}).intValue();
	 * //result (15)
	 * 
	 * 2.
	 * String randomize (String x);
	 * 
	 * String result = (String)invoke("randomize", new Object[]{"foobar"});
	 * //result ("bfaoro")
	 * 
	 * 
	 * @param methodName the name of the Methods to be called
	 * @param params Parameters of the Method
	 * @return returns the result of the method
	 */
	public Object invoke (String methodName, Object[] params) throws IPCException;

}
